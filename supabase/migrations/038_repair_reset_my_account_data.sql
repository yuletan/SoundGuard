-- Repair for "Could not delete account data: Account reset failed — RPC
-- (HTTP 404 / PGRST202)" on the live project: the function exists in the DB
-- (migration 035 recreated it) but PostgREST hides it from callers when grants
-- or the schema cache are stale. Drop + recreate with fresh EXECUTE grants and
-- force a schema reload — same fix pattern as migrations 014 and 037.
--
-- Run this once in Supabase Dashboard → SQL Editor, then retry
-- "Delete all account data" from the app's Settings screen.

alter table public.profiles add column if not exists deactivated_at timestamptz;

drop function if exists public.reset_my_account_data();

create function public.reset_my_account_data()
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

    set local row_security = off;

    delete from public.device_push_tokens where user_id = current_user_id;
    delete from public.beneficiary_settings where user_id = current_user_id;
    delete from public.caregiver_settings where user_id = current_user_id;

    -- Kept intentionally so the connected party still sees the chat with a
    -- "Deactivated" badge until they remove the connection (034 trigger):
    --   - public.care_connections
    --   - public.incidents / public.notifications / public.camera_snapshots

    update public.profiles
    set role = null,
        setup_completed_at = null,
        full_name = null,
        phone = null,
        deactivated_at = now(),
        updated_at = now()
    where id = current_user_id;
end;
$$;

grant usage on schema public to anon, authenticated;
grant execute on function public.reset_my_account_data() to anon, authenticated;

notify pgrst, 'reload schema';
