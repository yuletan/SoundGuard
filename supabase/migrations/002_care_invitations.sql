-- Table for temporary pairing invite codes
create table if not exists public.care_invitations (
    id uuid primary key default gen_random_uuid(),
    beneficiary_id uuid not null references public.profiles(id) on delete cascade,
    code text not null unique,
    expires_at timestamptz not null default (now() + interval '24 hours'),
    used_by uuid references public.profiles(id) on delete set null,
    used_at timestamptz,
    created_at timestamptz not null default now()
);

create index if not exists care_invitations_code_idx on public.care_invitations (code) where used_at is null;
create index if not exists care_invitations_beneficiary_idx on public.care_invitations (beneficiary_id);

alter table public.care_invitations enable row level security;

create policy "beneficiaries can create invitations"
    on public.care_invitations for insert
    with check (beneficiary_id = auth.uid());

create policy "beneficiaries can view their own invitations"
    on public.care_invitations for select
    using (beneficiary_id = auth.uid());

-- Function for a beneficiary to generate an unexpired 6-digit alphanumeric pairing code
create or replace function public.create_pairing_code()
returns text
language plpgsql
security definer set search_path = public
as $$
declare
    v_code text;
    v_beneficiary_id uuid;
begin
    v_beneficiary_id := auth.uid();
    if v_beneficiary_id is null then
        raise exception 'Not authenticated';
    end if;

    -- Generate random 6-character uppercase alphanumeric code
    loop
        v_code := upper(substr(md5(random()::text || clock_timestamp()::text), 1, 6));
        exit when not exists (select 1 from public.care_invitations where code = v_code and used_at is null and expires_at > now());
    end loop;

    insert into public.care_invitations (beneficiary_id, code, expires_at)
    values (v_beneficiary_id, v_code, now() + interval '24 hours');

    return v_code;
end;
$$;

-- Function for a caregiver to accept/redeem a pairing code
create or replace function public.accept_pairing_code(p_code text)
returns json
language plpgsql
security definer set search_path = public
as $$
declare
    v_caregiver_id uuid;
    v_invitation record;
    v_existing_connection record;
    v_order int;
    v_is_primary boolean;
    v_beneficiary_name text;
begin
    v_caregiver_id := auth.uid();
    if v_caregiver_id is null then
        raise exception 'Not authenticated';
    end if;

    -- Lookup valid invite code
    select * into v_invitation
    from public.care_invitations
    where code = upper(trim(p_code))
      and used_at is null
      and expires_at > now()
    limit 1;

    if not found then
        raise exception 'Invalid or expired pairing code';
    end if;

    if v_invitation.beneficiary_id = v_caregiver_id then
        raise exception 'You cannot pair with yourself';
    end if;

    -- Check if already connected
    select * into v_existing_connection
    from public.care_connections
    where beneficiary_id = v_invitation.beneficiary_id
      and caregiver_id = v_caregiver_id;

    if found then
        if v_existing_connection.status = 'active' then
            raise exception 'Already connected to this beneficiary';
        else
            update public.care_connections
            set status = 'active', accepted_at = now()
            where id = v_existing_connection.id;
        end if;
    else
        -- Determine if this is the first caregiver (primary) and determine escalation order
        select count(*) into v_order
        from public.care_connections
        where beneficiary_id = v_invitation.beneficiary_id
          and status = 'active';

        v_is_primary := (v_order = 0);
        v_order := v_order + 1;

        insert into public.care_connections (
            beneficiary_id,
            caregiver_id,
            status,
            is_primary,
            escalation_order,
            invited_by,
            accepted_at
        ) values (
            v_invitation.beneficiary_id,
            v_caregiver_id,
            'active',
            v_is_primary,
            v_order,
            v_invitation.beneficiary_id,
            now()
        );
    end if;

    -- Mark invitation as used
    update public.care_invitations
    set used_by = v_caregiver_id,
        used_at = now()
    where id = v_invitation.id;

    select coalesce(full_name, email, 'Beneficiary') into v_beneficiary_name
    from public.profiles
    where id = v_invitation.beneficiary_id;

    return json_build_object(
        'success', true,
        'beneficiary_id', v_invitation.beneficiary_id,
        'beneficiary_name', v_beneficiary_name
    );
end;
$$;
