-- WhatsApp-style ephemeral photo: both caregiver and beneficiary see the
-- same preview from one row in camera_snapshots, and it self-destructs
-- after 10 minutes for privacy. Supabase Storage holds the bytes;
-- the row is intentionally kept (as "expired") so the chat can show
-- a "Photo expired" placeholder instead of silently vanishing.

alter table public.camera_snapshots
    alter column expires_at set default (now() + interval '10 minutes');

-- Normalize any rows that were widened to 30 minutes by an earlier
-- migration: clamp future expiries back to 10 minutes from requested_at
-- (or from now if requested_at is somehow null). Already-expired rows
-- are left alone.
update public.camera_snapshots
set expires_at = least(
    expires_at,
    coalesce(requested_at, now()) + interval '10 minutes'
)
where expires_at > coalesce(requested_at, now()) + interval '10 minutes'
  and status <> 'expired';

-- Defensive: storage objects must not outlive expires_at. Clean them up
-- every minute even if the row was already marked expired.
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

drop function if exists public.expire_camera_snapshots();

create or replace function public.expire_camera_snapshots()
returns integer
language plpgsql
security definer set search_path = public, storage
as $$
begin
    return public.cleanup_expired_camera_snapshots();
end;
$$;

create extension if not exists pg_cron with schema extensions;

select cron.schedule(
    'soundguard-cleanup-expired-camera-storage',
    '* * * * *',
    $$select public.cleanup_expired_camera_snapshots();$$
)
where not exists (
    select 1 from cron.job where jobname = 'soundguard-cleanup-expired-camera-storage'
);

select cron.schedule(
    'soundguard-expire-camera-snapshots',
    '* * * * *',
    $$select public.expire_camera_snapshots();$$
)
where not exists (
    select 1 from cron.job where jobname = 'soundguard-expire-camera-snapshots'
);
