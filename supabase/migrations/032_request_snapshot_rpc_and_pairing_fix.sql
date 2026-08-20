-- Definitive fix for "Request photo" failing with RLS errors, plus robust
-- any-direction pairing:
--
-- 1) request_camera_snapshot RPC: the caregiver/beneficiary calls this
--    SECURITY DEFINER function instead of POSTing to /rest/v1/camera_snapshots.
--    Row-level policies no longer apply to the insert at all; the function
--    performs its own explicit authorization checks. This removes the whole
--    class of "new row violates RLS for table camera_snapshots" errors no
--    matter which policy combination exists on the table.
--
-- 2) accept_pairing_code: direction is now inferred from the CODE CREATOR's
--    stored profile role first (not just the caller's role). Either side can
--    generate a code and the other side can redeem it — one match links the
--    pair; the two codes never need to be equal.

-- ---------------------------------------------------------------------------
-- 1) request_camera_snapshot RPC
-- ---------------------------------------------------------------------------

-- Keep the helper available even if 031 was never applied.
create or replace function public.has_active_care_link(p_beneficiary uuid, p_caregiver uuid)
returns boolean
language sql
security definer set search_path = public
as $$
  select exists (
    select 1 from public.care_connections c
    where c.beneficiary_id = p_beneficiary
      and c.caregiver_id = p_caregiver
      and c.status = 'active'
  );
$$;

grant execute on function public.has_active_care_link(uuid, uuid) to authenticated;

create or replace function public.request_camera_snapshot(
    p_incident_id uuid,
    p_beneficiary_id uuid,
    p_camera_facing text default 'rear',
    p_storage_path text default null
)
returns json
language plpgsql
security definer set search_path = public
as $$
declare
    v_caller uuid := auth.uid();
    v_path text;
    v_row public.camera_snapshots;
begin
    if v_caller is null then
        raise exception 'Not authenticated';
    end if;

    if p_camera_facing is null or p_camera_facing not in ('front', 'rear') then
        raise exception 'Unsupported camera direction';
    end if;

    -- The incident must belong to the beneficiary being photographed.
    if not exists (
        select 1 from public.incidents i
        where i.id = p_incident_id
          and i.beneficiary_id = p_beneficiary_id
    ) then
        raise exception 'Incident not found for this beneficiary';
    end if;

    -- Caller must be the beneficiary themselves or an actively linked caregiver.
    if p_beneficiary_id <> v_caller
       and not public.has_active_care_link(p_beneficiary_id, v_caller) then
        raise exception 'No active care connection to this beneficiary';
    end if;

    v_path := nullif(trim(p_storage_path), '');
    if v_path is null then
        v_path := p_beneficiary_id || '/' || p_incident_id || '/' || gen_random_uuid() || '.jpg';
    end if;

    insert into public.camera_snapshots (
        incident_id, beneficiary_id, requested_by, camera_facing, storage_path, status
    ) values (
        p_incident_id, p_beneficiary_id, v_caller, p_camera_facing, v_path, 'requested'
    )
    returning * into v_row;

    return json_build_object(
        'id', v_row.id,
        'storage_path', v_row.storage_path,
        'expires_at', v_row.expires_at,
        'approval_status', v_row.approval_status,
        'status', v_row.status
    );
end;
$$;

revoke all on function public.request_camera_snapshot(uuid, uuid, text, text) from public;
grant execute on function public.request_camera_snapshot(uuid, uuid, text, text) to authenticated;

-- ---------------------------------------------------------------------------
-- 2) accept_pairing_code: creator-role-first direction inference
-- ---------------------------------------------------------------------------

create or replace function public.accept_pairing_code(p_code text)
returns json
language plpgsql
security definer set search_path = public
as $$
declare
    v_caller_id uuid;
    v_invitation record;
    v_creator_role text;
    v_caller_role text;
    v_beneficiary_id uuid;
    v_caregiver_id uuid;
    v_existing record;
    v_order int;
    v_is_primary boolean;
    v_display_name text;
    v_other_id uuid;
begin
    v_caller_id := auth.uid();
    if v_caller_id is null then
        raise exception 'Not authenticated';
    end if;

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

    select role into v_creator_role from public.profiles where id = v_invitation.beneficiary_id;
    select role into v_caller_role from public.profiles where id = v_caller_id;

    -- Direction: the code creator's stored role wins. Either role can create a
    -- code; the accepting side is always the opposite role. One match links.
    if v_creator_role = 'caregiver' then
        v_beneficiary_id := v_caller_id;
        v_caregiver_id := v_invitation.beneficiary_id;
    elsif v_creator_role = 'beneficiary' then
        v_beneficiary_id := v_invitation.beneficiary_id;
        v_caregiver_id := v_caller_id;
    elsif v_caller_role = 'beneficiary' then
        v_beneficiary_id := v_caller_id;
        v_caregiver_id := v_invitation.beneficiary_id;
    elsif v_caller_role = 'caregiver' then
        v_beneficiary_id := v_invitation.beneficiary_id;
        v_caregiver_id := v_caller_id;
    else
        -- Neither profile has a role yet (fresh setup): assume the original
        -- direction — creator is the beneficiary, caller is the caregiver.
        v_beneficiary_id := v_invitation.beneficiary_id;
        v_caregiver_id := v_caller_id;
    end if;

    select * into v_existing
    from public.care_connections
    where beneficiary_id = v_beneficiary_id and caregiver_id = v_caregiver_id;

    if found then
        if v_existing.status = 'active' then
            raise exception 'Already connected';
        else
            update public.care_connections
            set status = 'active', accepted_at = now()
            where id = v_existing.id;
        end if;
    else
        select count(*) into v_order
        from public.care_connections
        where beneficiary_id = v_beneficiary_id and status = 'active';
        v_is_primary := (v_order = 0);
        v_order := v_order + 1;
        insert into public.care_connections (
            beneficiary_id, caregiver_id, status, is_primary, escalation_order,
            invited_by, accepted_at
        ) values (
            v_beneficiary_id, v_caregiver_id, 'active', v_is_primary, v_order,
            v_invitation.beneficiary_id, now()
        );
    end if;

    update public.care_invitations
    set used_by = v_caller_id, used_at = now()
    where id = v_invitation.id;

    v_other_id := case when v_caller_id = v_beneficiary_id then v_caregiver_id else v_beneficiary_id end;
    select coalesce(full_name, email) into v_display_name from public.profiles where id = v_other_id;
    if v_display_name is null or v_display_name = '' then
        v_display_name := case when v_caller_id = v_beneficiary_id then 'Caregiver' else 'Beneficiary' end;
    end if;

    return json_build_object(
        'success', true,
        'beneficiary_id', v_beneficiary_id,
        'beneficiary_name', v_display_name,
        'connected_to', v_display_name
    );
end;
$$;

-- Any authenticated user (beneficiary or caregiver) can mint a pairing code;
-- their own id is stored in care_invitations.beneficiary_id.
create or replace function public.create_pairing_code()
returns text
language plpgsql
security definer set search_path = public
as $$
declare
    v_code text;
    v_creator uuid;
begin
    v_creator := auth.uid();
    if v_creator is null then
        raise exception 'Not authenticated';
    end if;

    loop
        v_code := upper(substr(md5(random()::text || clock_timestamp()::text), 1, 6));
        exit when not exists (
            select 1 from public.care_invitations
            where code = v_code and used_at is null and expires_at > now()
        );
    end loop;

    insert into public.care_invitations (beneficiary_id, code, expires_at)
    values (v_creator, v_code, now() + interval '24 hours');

    return v_code;
end;
$$;

revoke all on function public.create_pairing_code() from public;
grant execute on function public.create_pairing_code() to authenticated;

notify pgrst, 'reload schema';
