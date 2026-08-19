-- Remove expired camera files even when the snapshot row was already marked
-- expired. The storage object is the privacy-sensitive data that must be
-- deleted; updating the row alone does not remove it from the bucket.
create or replace function public.cleanup_expired_camera_snapshots()
returns integer
language plpgsql
security definer set search_path = public, storage
as $$
declare
    deleted_count integer;
begin
    delete from storage.objects object_row
    using public.camera_snapshots snapshot_row
    where object_row.bucket_id = 'camera-snapshots'
      and object_row.name = snapshot_row.storage_path
      and snapshot_row.expires_at <= now();

    get diagnostics deleted_count = row_count;

    update public.camera_snapshots
    set status = 'expired'
    where expires_at <= now()
      and status <> 'expired';

    return deleted_count;
end;
$$;

create extension if not exists pg_cron with schema extensions;

select cron.schedule(
    'soundguard-cleanup-expired-camera-storage',
    '* * * * *',
    $$select public.cleanup_expired_camera_snapshots();$$
)
where not exists (
    select 1
    from cron.job
    where jobname = 'soundguard-cleanup-expired-camera-storage'
);
