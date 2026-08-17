drop policy if exists "participants can delete snapshot objects" on storage.objects;
create policy "participants can delete snapshot objects"
    on storage.objects for delete
    using (
        bucket_id = 'camera-snapshots'
        and exists (
            select 1
            from public.camera_snapshots s
            where s.storage_path = name
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

drop policy if exists "participants can delete their incidents" on public.incidents;
create policy "participants can delete their incidents"
    on public.incidents for delete
    using (beneficiary_id = auth.uid());

drop policy if exists "caregivers can delete their notifications" on public.notifications;
create policy "caregivers can delete their notifications"
    on public.notifications for delete
    using (caregiver_id = auth.uid());

drop policy if exists "participants can delete snapshots" on public.camera_snapshots;
create policy "participants can delete snapshots"
    on public.camera_snapshots for delete
    using (beneficiary_id = auth.uid() or requested_by = auth.uid());

drop policy if exists "participants can delete care connections" on public.care_connections;
create policy "participants can delete care connections"
    on public.care_connections for delete
    using (beneficiary_id = auth.uid() or caregiver_id = auth.uid());

notify pgrst, 'reload schema';
