-- The bucket can also be created in the dashboard. Keep this idempotent so
-- applying migrations does not overwrite an existing size or MIME-type limit.
insert into storage.buckets (id, name, public)
values ('camera-snapshots', 'camera-snapshots', false)
on conflict (id) do update set public = false;

drop policy if exists "beneficiaries can upload incident snapshots" on storage.objects;
create policy "beneficiaries can upload incident snapshots"
    on storage.objects for insert
    with check (
        bucket_id = 'camera-snapshots'
        and (storage.foldername(name))[1] = auth.uid()::text
        and exists (
            select 1
            from public.camera_snapshots s
            where s.beneficiary_id = auth.uid()
              and s.status = 'requested'
              and s.expires_at > now()
              and s.storage_path = name
        )
    );

drop policy if exists "connected users can read incident snapshots" on storage.objects;
create policy "connected users can read incident snapshots"
    on storage.objects for select
    using (
        bucket_id = 'camera-snapshots'
        and exists (
            select 1
            from public.camera_snapshots s
            where s.storage_path = name
              and s.expires_at > now()
              and (
                  s.beneficiary_id = auth.uid()
                  or s.requested_by = auth.uid()
                  or exists (
                      select 1
                      from public.care_connections c
                      where c.beneficiary_id = s.beneficiary_id
                        and c.caregiver_id = auth.uid()
                        and c.status = 'active'
                  )
              )
        )
    );

drop policy if exists "beneficiaries can remove incident snapshots" on storage.objects;
create policy "beneficiaries can remove incident snapshots"
    on storage.objects for delete
    using (
        bucket_id = 'camera-snapshots'
        and (storage.foldername(name))[1] = auth.uid()::text
    );
