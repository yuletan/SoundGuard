create or replace function public.link_demo_james()
returns void
language plpgsql
security definer set search_path = public
as $$
begin
    if not exists (
        select 1 from public.profiles
        where id = auth.uid() and role = 'caregiver'
    ) then
        raise exception 'Only a caregiver can link the James test profile';
    end if;

    if not exists (
        select 1 from public.profiles
        where id = '7f3c2a91-6b84-4d1e-9f52-8a7c6b3d1042'::uuid
          and role = 'beneficiary'
    ) then
        raise exception 'James test profile does not exist';
    end if;

    insert into public.care_connections (
        beneficiary_id, caregiver_id, status, is_primary, escalation_order, invited_by, accepted_at
    ) values (
        '7f3c2a91-6b84-4d1e-9f52-8a7c6b3d1042'::uuid,
        auth.uid(), 'active', false,
        coalesce((select max(escalation_order) + 1 from public.care_connections
                  where beneficiary_id = '7f3c2a91-6b84-4d1e-9f52-8a7c6b3d1042'::uuid), 1),
        auth.uid(), now()
    )
    on conflict (beneficiary_id, caregiver_id) do update
        set status = 'active', accepted_at = coalesce(care_connections.accepted_at, now());
end;
$$;
