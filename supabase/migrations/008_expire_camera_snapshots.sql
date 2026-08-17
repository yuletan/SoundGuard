create or replace function public.expire_camera_snapshots()
returns integer
language plpgsql
security definer set search_path = public, storage
as $$
declare
    expired_count integer;
begin
    delete from storage.objects object_row
    using public.camera_snapshots snapshot_row
    where object_row.bucket_id = 'camera-snapshots'
      and object_row.name = snapshot_row.storage_path
      and snapshot_row.expires_at <= now()
      and snapshot_row.status <> 'expired';

    update public.camera_snapshots
    set status = 'expired'
    where expires_at <= now()
      and status <> 'expired';

    get diagnostics expired_count = row_count;
    return expired_count;
end;
$$;

-- Supabase exposes pg_cron as an optional extension. When enabled, this runs
-- cleanup every minute without requiring an Android process to stay alive.
create extension if not exists pg_cron with schema extensions;
select cron.schedule(
    'soundguard-expire-camera-snapshots',
    '* * * * *',
    $$select public.expire_camera_snapshots();$$
)
where not exists (
    select 1 from cron.job where jobname = 'soundguard-expire-camera-snapshots'
);
