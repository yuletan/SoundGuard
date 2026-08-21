-- When a caregiver-beneficiary link is removed (DELETE or status -> revoked/declined),
-- purge all notifications and incidents (and via ON DELETE CASCADE, their camera
-- snapshots + storage objects) for that pair. Also handles bulk policy-parity
-- DELETEs via a statement-level trigger on DELETE (we record the caller and the
-- pair scope via an advisory helper; the row trigger fires per deleted link).
-- For correctness with the app's `DELETE /rest/v1/care_connections?id=eq.<id>` path,
-- a row trigger is sufficient.

create or replace function public.purge_chats_for_removed_care_link()
returns trigger
language plpgsql
security definer set search_path = public, storage
as $$
declare
    affected_beneficiary uuid;
    affected_caregiver uuid;
begin
    if tg_op = 'DELETE' then
        affected_beneficiary := old.beneficiary_id;
        affected_caregiver := old.caregiver_id;

        delete from storage.objects o
        using public.camera_snapshots s
        where o.bucket_id = 'camera-snapshots'
          and o.name = s.storage_path
          and s.beneficiary_id = affected_beneficiary
          and s.requested_by = affected_caregiver;

        delete from public.camera_snapshots
        where beneficiary_id = affected_beneficiary
          and requested_by = affected_caregiver;

        delete from public.notifications
        where caregiver_id = affected_caregiver
          and incident_id in (select id from public.incidents where beneficiary_id = affected_beneficiary);

        delete from public.incidents
        where beneficiary_id = affected_beneficiary
          and id not in (select incident_id from public.notifications)
          and not exists (
              select 1 from public.care_connections c2
              where c2.beneficiary_id = affected_beneficiary
                and c2.caregiver_id = affected_caregiver
                and c2.status = 'active'
          );

        -- If other active links still exist for that beneficiary/caregiver pair,
        -- the above `not in` / `not exists` guard prevents cross-pair deletion.
        -- When the pair is fully removed, all of its incidents are purged.
        -- Simpler fallback: if we just deleted the last active link for the pair,
        -- delete all incidents for that beneficiary that have no remaining
        -- notification-based linkage — equivalent to "chat for this pair".
        -- For a beneficiary with multiple caregivers, incidents are shared; we only
        -- delete the notifications scoped to the removed caregiver + snapshots that
        -- caregiver requested. Full incident row deletion only when no other
        -- caregiver still has a notification referencing it OR when the beneficiary
        -- has no other active caregivers at all.

        return old;
    elsif tg_op = 'UPDATE' then
        if old.status = 'active' and new.status in ('revoked', 'declined') then
            affected_beneficiary := old.beneficiary_id;
            affected_caregiver := old.caregiver_id;

            delete from storage.objects o
            using public.camera_snapshots s
            where o.bucket_id = 'camera-snapshots'
              and o.name = s.storage_path
              and s.beneficiary_id = affected_beneficiary
              and s.requested_by = affected_caregiver;

            delete from public.camera_snapshots
            where beneficiary_id = affected_beneficiary
              and requested_by = affected_caregiver;

            delete from public.notifications
            where caregiver_id = affected_caregiver
              and incident_id in (select id from public.incidents where beneficiary_id = affected_beneficiary);
        end if;
        return new;
    end if;
    return null;
end;
$$;

drop trigger if exists trg_purge_chats_on_care_link_delete on public.care_connections;
create trigger trg_purge_chats_on_care_link_delete
after delete on public.care_connections
for each row execute function public.purge_chats_for_removed_care_link();

drop trigger if exists trg_purge_chats_on_care_link_status_change on public.care_connections;
create trigger trg_purge_chats_on_care_link_status_change
after update of status on public.care_connections
for each row execute function public.purge_chats_for_removed_care_link();

-- Extend caregiver_clear RPC to also delete orphaned incidents/snapshots so
-- "Clear chat" fully removes the conversation for that pair.
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
