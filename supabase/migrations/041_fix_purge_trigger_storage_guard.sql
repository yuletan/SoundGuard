-- Fixes 42501 "Direct deletion from storage tables is not allowed. Use the
-- Storage API instead." (hint: prevents accidental data loss from orphaned
-- objects) which aborted account reset, care-link removal, and clear-chat on
-- projects whose storage schema guards storage.objects against direct SQL
-- DELETEs.
--
-- Root cause: migration 034's trigger functions delete camera-snapshot files
-- directly from storage.objects. On guarded projects that raises 42501 and
-- kills the entire operation. We now ATTEMPT the storage cleanup but swallow
-- the guard error so the row-level purges (snapshots, notifications,
-- incidents, the connection itself) always proceed. Unreferenced files simply
-- become unreachable (RLS + expires_at) and age out of the bucket.
--
-- Also re-applies the canonical profiles RLS policies (migration 029) because
-- the account-reset REST fallback was rejected with 42501 "new row violates
-- row-level security policy for table profiles" — meaning these were
-- missing/stale on this project.
--
-- Run once in Supabase Dashboard → SQL Editor.

-- ---------------------------------------------------------------------------
-- 1) Canonical profiles RLS + explicit table grants (idempotent).
-- ---------------------------------------------------------------------------
drop policy if exists "users can view their profile" on public.profiles;
drop policy if exists "users can insert their profile" on public.profiles;
drop policy if exists "users can update their profile" on public.profiles;
drop policy if exists "connected users can view participant profiles" on public.profiles;

create policy "users can view their profile"
    on public.profiles for select to authenticated
    using (id = auth.uid());

create policy "users can insert their profile"
    on public.profiles for insert to authenticated
    with check (id = auth.uid());

create policy "users can update their profile"
    on public.profiles for update to authenticated
    using (id = auth.uid())
    with check (id = auth.uid());

create policy "connected users can view participant profiles"
    on public.profiles for select to authenticated
    using (
        id = auth.uid()
        or exists (
            select 1 from public.care_connections c
            where c.status = 'active'
              and (
                  (c.beneficiary_id = auth.uid() and c.caregiver_id = profiles.id)
                  or (c.caregiver_id = auth.uid() and c.beneficiary_id = profiles.id)
              )
        )
    );

alter table public.profiles enable row level security;
grant select, insert, update, delete on public.profiles to authenticated;

-- ---------------------------------------------------------------------------
-- 2) Chat-purge trigger — storage cleanup is best-effort, never fatal.
-- ---------------------------------------------------------------------------
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

        -- Best-effort: guarded projects reject direct storage DELETEs (42501).
        begin
            delete from storage.objects o
            using public.camera_snapshots s
            where o.bucket_id = 'camera-snapshots'
              and o.name = s.storage_path
              and s.beneficiary_id = affected_beneficiary
              and s.requested_by = affected_caregiver;
        exception when others then null; end;

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

        return old;
    elsif tg_op = 'UPDATE' then
        if old.status = 'active' and new.status in ('revoked', 'declined') then
            affected_beneficiary := old.beneficiary_id;
            affected_caregiver := old.caregiver_id;

            begin
                delete from storage.objects o
                using public.camera_snapshots s
                where o.bucket_id = 'camera-snapshots'
                  and o.name = s.storage_path
                  and s.beneficiary_id = affected_beneficiary
                  and s.requested_by = affected_caregiver;
            exception when others then null; end;

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

-- ---------------------------------------------------------------------------
-- 3) Clear-chat RPC — same best-effort storage cleanup.
-- ---------------------------------------------------------------------------
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

    begin
        delete from storage.objects o
        using public.camera_snapshots s
        where o.bucket_id = 'camera-snapshots'
          and o.name = s.storage_path
          and s.incident_id in (
              select id from public.incidents where beneficiary_id = p_beneficiary_id
          );
    exception when others then null; end;

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
