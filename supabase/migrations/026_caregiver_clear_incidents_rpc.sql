-- Caregiver-sided "Clear chat" — deletes incidents (and cascaded notifications/snapshots)
-- for the paired beneficiary, but only when an active care_connection exists.
-- This is what the caregiver's "Clear chat" button calls.

create or replace function public.caregiver_clear_incidents_for_beneficiary(p_beneficiary_id uuid)
returns integer
language plpgsql
security definer set search_path = public, storage
as $$
declare
    deleted_count integer := 0;
    caller uuid := auth.uid();
begin
    if caller is null then
        raise exception 'Not authenticated';
    end if;

    if not exists (
        select 1 from public.care_connections c
        where c.beneficiary_id = p_beneficiary_id
          and c.caregiver_id = caller
          and c.status = 'active'
    ) then
        raise exception 'No active care connection to this beneficiary';
    end if;

    -- Delete storage objects for those incidents first (bucket is private).
    delete from storage.objects o
    using public.camera_snapshots s
    where o.bucket_id = 'camera-snapshots'
      and o.name = s.storage_path
      and s.incident_id in (
          select id from public.incidents where beneficiary_id = p_beneficiary_id
      );

    delete from public.camera_snapshots
    where incident_id in (select id from public.incidents where beneficiary_id = p_beneficiary_id);

    delete from public.notifications
    where incident_id in (select id from public.incidents where beneficiary_id = p_beneficiary_id)
      and caregiver_id = caller;

    delete from public.incidents where beneficiary_id = p_beneficiary_id;
    get diagnostics deleted_count = row_count;
    return deleted_count;
end;
$$;

revoke all on function public.caregiver_clear_incidents_for_beneficiary(uuid) from public;
grant execute on function public.caregiver_clear_incidents_for_beneficiary(uuid) to authenticated;

notify pgrst, 'reload schema';
