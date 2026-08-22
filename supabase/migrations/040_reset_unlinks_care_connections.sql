-- Account reset now ALSO removes care connections.
-- Previously (migrations 035/037/039) care_connections were intentionally kept
-- so the other party would see a "Deactivated" badge and remove the link
-- themselves. In practice this meant a re-signed-up user found their previous
-- caregiver still linked (the app lists connections with status = 'active').
--
-- New behavior: reset deletes every care_connection where the caller is the
-- beneficiary OR the caregiver. Removing a connection fires the migration-034
-- trigger, which purges the shared chat messages for BOTH sides.
--
-- Run once in Supabase Dashboard → SQL Editor.

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

    -- Best-effort deletes: skip silently if a table is missing/blocked.
    begin
        delete from public.device_push_tokens where user_id = current_user_id;
    exception when others then null; end;
    begin
        delete from public.beneficiary_settings where user_id = current_user_id;
    exception when others then null; end;
    begin
        delete from public.caregiver_settings where user_id = current_user_id;
    exception when others then null; end;

    -- Unlink everyone: remove connections in BOTH directions. This fires the
    -- migration-034 trigger that purges shared chat messages for both sides.
    delete from public.care_connections
    where beneficiary_id = current_user_id
       or caregiver_id = current_user_id;

    -- The step that must succeed — errors here surface to the caller.
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
