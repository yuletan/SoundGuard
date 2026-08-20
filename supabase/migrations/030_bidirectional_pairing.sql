-- Bidirectional pairing: a caregiver's 6-char code can be redeemed by a
-- beneficiary and vice-versa. Previously only beneficiary codes worked.
-- accept_pairing_code now links whichever direction the code was created in.

create or replace function public.accept_pairing_code(p_code text)
returns json
language plpgsql
security definer set search_path = public
as $$
declare
    v_caller_id uuid;
    v_invitation record;
    v_caller_role text;
    v_other_id uuid;
    v_beneficiary_id uuid;
    v_caregiver_id uuid;
    v_existing record;
    v_order int;
    v_is_primary boolean;
    v_display_name text;
begin
    v_caller_id := auth.uid();
    if v_caller_id is null then
        raise exception 'Not authenticated';
    end if;

    select role into v_caller_role from public.profiles where id = v_caller_id;

    select * into v_invitation
    from public.care_invitations
    where code = upper(trim(p_code))
      and used_at is null
      and expires_at > now()
    limit 1;

    if not found then
        raise exception 'Invalid or expired pairing code';
    end if;

    if v_invitation.beneficiary_id = v_caller_id then
        raise exception 'You cannot pair with yourself';
    end if;

    -- The invitation owner vs the caller: figure out beneficiary/caregiver.
    -- Original flow: invitation.beneficiary_id is always the code creator.
    -- Bidirectional: creator can be either role; accept infers direction.
    -- If caller role is known, use it; otherwise fall back to original (caller = caregiver).
    if v_caller_role = 'beneficiary' then
        -- Caller is beneficiary, so creator must be caregiver; accept makes caller the beneficiary.
        v_beneficiary_id := v_caller_id;
        v_caregiver_id := v_invitation.beneficiary_id;
    elsif v_caller_role = 'caregiver' then
        v_beneficiary_id := v_invitation.beneficiary_id;
        v_caregiver_id := v_caller_id;
    else
        -- No role yet (during setup) — assume original direction: creator=beneficiary, caller=caregiver
        v_beneficiary_id := v_invitation.beneficiary_id;
        v_caregiver_id := v_caller_id;
    end if;

    -- Safety: roles must be consistent if both profiles have roles.
    -- Allow if at least one side would match after linking (don't block on role mismatch during setup).
    select * into v_existing
    from public.care_connections
    where beneficiary_id = v_beneficiary_id and caregiver_id = v_caregiver_id;

    if found then
        if v_existing.status = 'active' then
            raise exception 'Already connected';
        else
            update public.care_connections set status = 'active', accepted_at = now() where id = v_existing.id;
        end if;
    else
        select count(*) into v_order from public.care_connections where beneficiary_id = v_beneficiary_id and status = 'active';
        v_is_primary := (v_order = 0);
        v_order := v_order + 1;
        insert into public.care_connections (beneficiary_id, caregiver_id, status, is_primary, escalation_order, invited_by, accepted_at)
        values (v_beneficiary_id, v_caregiver_id, 'active', v_is_primary, v_order, v_invitation.beneficiary_id, now());
    end if;

    update public.care_invitations set used_by = v_caller_id, used_at = now() where id = v_invitation.id;

    -- Return the other side's name for the toast.
    v_other_id := case when v_caller_id = v_beneficiary_id then v_caregiver_id else v_beneficiary_id end;
    select coalesce(full_name, email, 'Beneficiary') into v_display_name from public.profiles where id = v_other_id;
    if v_display_name is null then v_display_name := 'Beneficiary'; end if;

    -- Also return beneficiary_id for clients that expect it (keeps existing CareClient happy).
    return json_build_object('success', true, 'beneficiary_id', v_beneficiary_id, 'beneficiary_name', v_display_name, 'connected_to', v_display_name);
end;
$$;

-- Codes are now usable by either role; keep create_pairing_code as-is (any authenticated user
-- can create a code from their beneficiary_id or just their user id). For a caregiver creator,
-- beneficiary_id column stores the caregiver's id — accept_pairing_code handles the flip.
-- Allow caregivers to create codes too by loosening the check to just auth.uid().
drop policy if exists "beneficiaries can create invitations" on public.care_invitations;
create policy "beneficiaries can create invitations"
    on public.care_invitations for insert to authenticated
    with check (beneficiary_id = auth.uid());

-- Invitation visibility for the creator.
drop policy if exists "beneficiaries can view their own invitations" on public.care_invitations;
create policy "beneficiaries can view their own invitations"
    on public.care_invitations for select to authenticated
    using (beneficiary_id = auth.uid());

notify pgrst, 'reload schema';
