create or replace function public.reset_my_account_data()
returns void
language plpgsql
security definer set search_path = public, storage
as $$
begin
    delete from storage.objects object_row
    using public.camera_snapshots snapshot_row
    where object_row.bucket_id = 'camera-snapshots'
      and object_row.name = snapshot_row.storage_path
      and (snapshot_row.beneficiary_id = auth.uid() or snapshot_row.requested_by = auth.uid());

    delete from public.incidents where beneficiary_id = auth.uid();
    delete from public.notifications where caregiver_id = auth.uid();
    delete from public.camera_snapshots where beneficiary_id = auth.uid() or requested_by = auth.uid();
    delete from public.device_push_tokens where user_id = auth.uid();
    delete from public.beneficiary_settings where user_id = auth.uid();
    delete from public.caregiver_settings where user_id = auth.uid();
    delete from public.care_connections where beneficiary_id = auth.uid() or caregiver_id = auth.uid();

    update public.profiles
    set role = null,
        setup_completed_at = null,
        full_name = null,
        phone = null,
        updated_at = now()
    where id = auth.uid();
end;
$$;
