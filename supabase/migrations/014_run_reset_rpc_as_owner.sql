create or replace function public.reset_my_account_data()
returns void
language plpgsql
security definer
set search_path = public, storage
as $$
declare
    current_user_id uuid := auth.uid();
begin
    if current_user_id is null then
        raise exception 'An authenticated session is required to delete account data';
    end if;

    -- The function is security-definer and performs only rows owned by the
    -- authenticated user. Disable RLS so the reset cannot be partially applied.
    set local row_security = off;

    delete from storage.objects object_row
    using public.camera_snapshots snapshot_row
    where object_row.bucket_id = 'camera-snapshots'
      and object_row.name = snapshot_row.storage_path
      and (snapshot_row.beneficiary_id = current_user_id or snapshot_row.requested_by = current_user_id);

    delete from public.incidents where beneficiary_id = current_user_id;
    delete from public.notifications where caregiver_id = current_user_id;
    delete from public.camera_snapshots where beneficiary_id = current_user_id or requested_by = current_user_id;
    delete from public.device_push_tokens where user_id = current_user_id;
    delete from public.beneficiary_settings where user_id = current_user_id;
    delete from public.caregiver_settings where user_id = current_user_id;
    delete from public.care_connections where beneficiary_id = current_user_id or caregiver_id = current_user_id;

    update public.profiles
    set role = null,
        setup_completed_at = null,
        full_name = null,
        phone = null,
        updated_at = now()
    where id = current_user_id;
end;
$$;

grant execute on function public.reset_my_account_data() to authenticated;
notify pgrst, 'reload schema';
