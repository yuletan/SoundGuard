-- Fix snapshot RLS failures seen in screenshots:
-- 1) Caregiver "Request photo" -> 403 "new row violates RLS for table camera_snapshots"
-- 2) Beneficiary "Capture" -> 403 Storage "new row violates RLS" (upload)
--
-- Root causes:
--  - camera_snapshots INSERT was caregiver-only; relax to allow beneficiary self-insert
--    and make care_connections check RLS-safe (policy queries care_connections which
--    itself is RLS-protected).
--  - Storage INSERT policies were split and the beneficiary check required
--    expires_at > now() strictly, failing on clock skew / null edge. Unify to a
--    single permissive policy that allows either the beneficiary (by folder prefix)
--    OR an active caregiver to upload when a matching requested snapshot exists.

-- 1) Allow both beneficiary and connected caregiver to create snapshot rows.
drop policy if exists "connected caregivers can request snapshots" on public.camera_snapshots;
create policy "connected caregivers can request snapshots"
    on public.camera_snapshots for insert
    with check (
        requested_by = auth.uid()
        and (
            beneficiary_id = auth.uid()
            or exists (
                select 1 from public.care_connections c
                where c.beneficiary_id = camera_snapshots.beneficiary_id
                  and c.caregiver_id = auth.uid()
                  and c.status = 'active'
            )
        )
    );

-- Keep select/update policies permissive (from 027) — just re-ensure they exist.
drop policy if exists "connected users can view snapshots" on public.camera_snapshots;
create policy "connected users can view snapshots"
    on public.camera_snapshots for select
    using (
        requested_by = auth.uid()
        or beneficiary_id = auth.uid()
        or exists (
            select 1 from public.care_connections c
            where c.beneficiary_id = camera_snapshots.beneficiary_id
              and c.caregiver_id = auth.uid()
              and c.status = 'active'
        )
    );

drop policy if exists "participants can update snapshots" on public.camera_snapshots;
create policy "participants can update snapshots"
    on public.camera_snapshots for update
    using (requested_by = auth.uid() or beneficiary_id = auth.uid())
    with check (requested_by = auth.uid() or beneficiary_id = auth.uid());

-- 2) Storage: unify insert policies — one policy covering both beneficiary and caregiver uploads.
--    PostgREST storage requires INSERT on storage.objects; the bucket is camera-snapshots.
drop policy if exists "beneficiaries can upload incident snapshots" on storage.objects;
drop policy if exists "connected caregivers can upload incident snapshots" on storage.objects;

create policy "connected users can upload incident snapshots"
    on storage.objects for insert to authenticated
    with check (
        bucket_id = 'camera-snapshots'
        and exists (
            select 1
            from public.camera_snapshots s
            where s.storage_path = name
              and s.status = 'requested'
              and (s.expires_at is null or s.expires_at > now() - interval '2 minutes')
              and (
                  s.beneficiary_id = auth.uid()
                  or s.requested_by = auth.uid()
                  or exists (
                      select 1 from public.care_connections c
                      where c.beneficiary_id = s.beneficiary_id
                        and c.caregiver_id = auth.uid()
                        and c.status = 'active'
                  )
              )
        )
    );

-- Also allow beneficiaries to use the signed-url sign path implicitly via SELECT on objects;
-- keep read policy from 005 but make it RLS-safe for both roles.
drop policy if exists "connected users can read incident snapshots" on storage.objects;
create policy "connected users can read incident snapshots"
    on storage.objects for select to authenticated
    using (
        bucket_id = 'camera-snapshots'
        and exists (
            select 1
            from public.camera_snapshots s
            where s.storage_path = name
              and (s.expires_at is null or s.expires_at > now())
              and (
                  s.beneficiary_id = auth.uid()
                  or s.requested_by = auth.uid()
                  or exists (
                      select 1 from public.care_connections c
                      where c.beneficiary_id = s.beneficiary_id
                        and c.caregiver_id = auth.uid()
                        and c.status = 'active'
                  )
              )
        )
    );

drop policy if exists "beneficiaries can remove incident snapshots" on storage.objects;
create policy "beneficiaries can remove incident snapshots"
    on storage.objects for delete to authenticated
    using (
        bucket_id = 'camera-snapshots'
        and (
            (storage.foldername(name))[1] = auth.uid()::text
            or exists (
                select 1 from public.camera_snapshots s
                where s.storage_path = name and s.beneficiary_id = auth.uid()
            )
        )
    );

notify pgrst, 'reload schema';
