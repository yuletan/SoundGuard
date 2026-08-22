-- 037_remove_connection_rpc.sql
--
-- Fixes:
--   (1) "Remove failed (HTTP 403) code 42501" when removing a caregiver/
--       beneficiary connection. The app's direct DELETE on care_connections
--       depends on RLS DELETE policies; any policy drift breaks removal.
--       Provide a SECURITY DEFINER RPC that verifies the caller is a
--       participant of the connection and deletes it, bypassing RLS.
--   (2) Defensive re-assertion of the DELETE policy + deactivated_at column
--       (in case 035 was only partially applied), plus fresh grants and a
--       PostgREST schema reload so both RPCs resolve immediately.

alter table public.profiles
    add column if not exists deactivated_at timestamptz;

-- ---------------------------------------------------------------------------
-- (1) remove_care_connection(p_connection_id)
--     Caller must be the beneficiary or the caregiver of the row. Security
--     definer so the delete cannot be blocked by RLS policy drift.
-- ---------------------------------------------------------------------------
create or replace function public.remove_care_connection(p_connection_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    caller uuid := auth.uid();
begin
    if caller is null then
        raise exception 'Not authenticated';
    end if;

    if not exists (
        select 1 from public.care_connections c
        where c.id = p_connection_id
          and (c.beneficiary_id = caller or c.caregiver_id = caller)
    ) then
        raise exception 'Connection not found for this account';
    end if;

    delete from public.care_connections where id = p_connection_id;
end;
$$;

revoke all on function public.remove_care_connection(uuid) from public, anon;
grant execute on function public.remove_care_connection(uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- (2) Defensive parity with 015/035: keep the direct-DELETE path working too.
-- ---------------------------------------------------------------------------
drop policy if exists "participants can delete care connections" on public.care_connections;
create policy "participants can delete care connections"
    on public.care_connections for delete
    using (beneficiary_id = auth.uid() or caregiver_id = auth.uid());

-- ---------------------------------------------------------------------------
-- (3) reset_my_account_data() — DROP + RECREATE cleanly.
--     The app still received HTTP 404 for this RPC even though it exists:
--     a stale grant state / PostgREST schema cache makes PostgREST hide the
--     function from authenticated callers (PGRST202 → 404). Dropping and
--     recreating with fresh EXECUTE grants + schema reload is the reliable fix.
--     Body identical to migration 035: deactivate the caller, keep care
--     connections + chat so the other side sees "Deactivated" until removed.
-- ---------------------------------------------------------------------------
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
