alter table public.camera_snapshots
    alter column expires_at set default (now() + interval '30 minutes');

drop policy if exists "beneficiaries can upload incident snapshots" on storage.objects;
create policy "beneficiaries can upload incident snapshots"
    on storage.objects for insert to authenticated
    with check (
        bucket_id = 'camera-snapshots'
        and (storage.foldername(name))[1] = auth.uid()::text
        and exists (
            select 1
            from public.camera_snapshots s
            where s.beneficiary_id = auth.uid()
              and s.status = 'requested'
              and (s.expires_at is null or s.expires_at > now())
              and s.storage_path = name
        )
    );

drop policy if exists "connected caregivers can upload incident snapshots" on storage.objects;
create policy "connected caregivers can upload incident snapshots"
    on storage.objects for insert to authenticated
    with check (
        bucket_id = 'camera-snapshots'
        and exists (
            select 1
            from public.camera_snapshots s
            where s.storage_path = name
              and s.status = 'requested'
              and (s.expires_at is null or s.expires_at > now())
              and (
                  s.beneficiary_id = auth.uid()
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
