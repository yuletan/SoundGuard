-- Definitive fix for "new row violates RLS for table camera_snapshots" + storage.
-- Root cause: policies queried care_connections / camera_snapshots directly, but those
-- tables are themselves RLS-protected, so the EXISTS check returned 0 rows for the
-- inserting user and the WITH CHECK failed even with a valid active link.
-- Solution: use SECURITY DEFINER helpers that bypass RLS.

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

create or replace function public.can_upload_camera_snapshot(p_name text)
returns boolean
language plpgsql
security definer set search_path = public, storage
as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then return false; end if;
  return exists (
    select 1 from public.camera_snapshots s
    where s.storage_path = p_name
      and s.status = 'requested'
      and (s.expires_at is null or s.expires_at > now() - interval '5 minutes')
      and (
        s.beneficiary_id = v_uid
        or s.requested_by = v_uid
        or public.has_active_care_link(s.beneficiary_id, v_uid)
      )
  );
end;
$$;

grant execute on function public.can_upload_camera_snapshot(text) to authenticated;

create or replace function public.can_read_camera_snapshot(p_name text)
returns boolean
language plpgsql
security definer set search_path = public, storage
as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then return false; end if;
  return exists (
    select 1 from public.camera_snapshots s
    where s.storage_path = p_name
      and (s.expires_at is null or s.expires_at > now())
      and (
        s.beneficiary_id = v_uid
        or s.requested_by = v_uid
        or public.has_active_care_link(s.beneficiary_id, v_uid)
      )
  );
end;
$$;

grant execute on function public.can_read_camera_snapshot(text) to authenticated;

-- 1) camera_snapshots: replace INSERT/SELECT/UPDATE with helper-based versions
drop policy if exists "connected caregivers can request snapshots" on public.camera_snapshots;
drop policy if exists "connected users can view snapshots" on public.camera_snapshots;
drop policy if exists "participants can update snapshots" on public.camera_snapshots;
drop policy if exists "connected users can upload incident snapshots" on storage.objects;
drop policy if exists "connected users can read incident snapshots" on storage.objects;
drop policy if exists "beneficiaries can remove incident snapshots" on storage.objects;
drop policy if exists "beneficiaries can upload incident snapshots" on storage.objects;
drop policy if exists "connected caregivers can upload incident snapshots" on storage.objects;

create policy "connected caregivers can request snapshots"
  on public.camera_snapshots for insert to authenticated
  with check (
    requested_by = auth.uid()
    and (
      beneficiary_id = auth.uid()
      or public.has_active_care_link(beneficiary_id, auth.uid())
    )
  );

create policy "connected users can view snapshots"
  on public.camera_snapshots for select to authenticated
  using (
    requested_by = auth.uid()
    or beneficiary_id = auth.uid()
    or public.has_active_care_link(beneficiary_id, auth.uid())
  );

create policy "participants can update snapshots"
  on public.camera_snapshots for update to authenticated
  using (requested_by = auth.uid() or beneficiary_id = auth.uid() or public.has_active_care_link(beneficiary_id, auth.uid()))
  with check (requested_by = auth.uid() or beneficiary_id = auth.uid() or public.has_active_care_link(beneficiary_id, auth.uid()));

-- 2) storage.objects: single permissive policy per action using helpers
create policy "connected users can upload incident snapshots"
  on storage.objects for insert to authenticated
  with check (
    bucket_id = 'camera-snapshots'
    and public.can_upload_camera_snapshot(name)
  );

create policy "connected users can read incident snapshots"
  on storage.objects for select to authenticated
  using (
    bucket_id = 'camera-snapshots'
    and public.can_read_camera_snapshot(name)
  );

create policy "beneficiaries can remove incident snapshots"
  on storage.objects for delete to authenticated
  using (
    bucket_id = 'camera-snapshots'
    and (
      (storage.foldername(name))[1] = auth.uid()::text
      or public.can_read_camera_snapshot(name)
      or public.can_upload_camera_snapshot(name)
    )
  );

notify pgrst, 'reload schema';
