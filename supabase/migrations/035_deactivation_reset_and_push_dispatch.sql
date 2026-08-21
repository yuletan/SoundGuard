-- 035_deactivation_reset_and_push_dispatch.sql
--
-- Consolidated backend fixes for:
--   (1) "Delete all account data" HTTP 403 — the reset_my_account_data() RPC
--       was returning 403 because grants/schema-cache were not effective on the
--       live DB. Drop + recreate cleanly and re-grant EXECUTE + schema USAGE,
--       then reload PostgREST.
--   (2) "Can't remove caregiver/beneficiary" — re-assert the DELETE RLS policy
--       on care_connections (and related tables) so the app's
--       DELETE /rest/v1/care_connections?id=eq.<id> path succeeds.
--   (3) Account deactivation indicator — "Delete all account data" now
--       DEACTIVATES the account (clears the profile) but KEEPS the care
--       connection + chat history so the OTHER party still sees the chat with
--       a "Deactivated" badge and a recommendation to remove it. Removing the
--       connection still purges the chat via migration 034's trigger.
--   (4) Server-side FCM push dispatch — the incident enqueue trigger now also
--       queues channel='push' notifications for every active caregiver, and
--       (when configured) pings the send-push edge function to deliver them
--       even when the caregiver's app/phone is closed.

-- ---------------------------------------------------------------------------
-- (3a) profiles.deactivated_at — the deactivation signal the app reads.
-- ---------------------------------------------------------------------------
alter table public.profiles
    add column if not exists deactivated_at timestamptz;

-- A deactivated user may still be referenced by an active care_connection so
-- the other party can see the chat. The existing "connected users can view
-- participant profiles" SELECT policy (016) already covers the new column.

-- ---------------------------------------------------------------------------
-- (3b)+(1) reset_my_account_data() — DEACTIVATE (keep chat for the other side)
-- ---------------------------------------------------------------------------
-- Dropped + recreated (not CREATE OR REPLACE) so a fresh, clean grant state is
-- guaranteed — this is the reliable fix for the recurring HTTP 403.
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

    -- Security-definer + RLS off so the reset applies atomically.
    set local row_security = off;

    -- Clean the caller's own device + settings (no chat-visibility impact).
    delete from public.device_push_tokens where user_id = current_user_id;
    delete from public.beneficiary_settings where user_id = current_user_id;
    delete from public.caregiver_settings where user_id = current_user_id;

    -- INTENTIONALLY KEPT so the connected party still sees the chat with a
    -- "Deactivated" badge until they remove the connection (which purges via
    -- trg_purge_chats_on_care_link_delete from migration 034):
    --   - public.care_connections
    --   - public.incidents / public.notifications / public.camera_snapshots
    --   - storage.objects (camera-snapshots bucket)

    -- Deactivate the profile. full_name/phone/role/setup are cleared; the
    -- deactivated_at timestamp is the explicit signal the app reads.
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

-- ---------------------------------------------------------------------------
-- (2) Re-assert DELETE RLS policies so removing a connection always works.
--     (Idempotent — mirrors migration 015; safe if already applied.)
-- ---------------------------------------------------------------------------
drop policy if exists "participants can delete care connections" on public.care_connections;
create policy "participants can delete care connections"
    on public.care_connections for delete
    using (beneficiary_id = auth.uid() or caregiver_id = auth.uid());

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

-- Allow a connected party to remove a deactivated partner's care_connection
-- even though the partner's profile is now deactivated. The existing policy
-- keys on beneficiary_id/caregiver_id = auth.uid(), which already covers the
-- active side removing the row — no extra policy needed. Kept here as a note.

notify pgrst, 'reload schema';

-- ---------------------------------------------------------------------------
-- (4) Server-side FCM push: enqueue channel='push' rows on high-severity
--     incidents and (when configured) ping the send-push edge function.
-- ---------------------------------------------------------------------------
create or replace function public.enqueue_incident_notification()
returns trigger
language plpgsql
security definer set search_path = public
as $$
declare
    notified_count integer;
    v_push_endpoint text := current_setting('app.push_endpoint', true);
    v_push_secret text := current_setting('app.push_secret', true);
begin
    if new.status = 'detected' then
        return new;
    end if;

    -- Only high-severity, actively-escalating incidents enqueue notifications.
    if not (
        new.severity = 'high'
        and new.status in ('waiting_user', 'caregiver_notified', 'escalated')
        and (tg_op = 'INSERT' or old.status is distinct from new.status)
    ) then
        return new;
    end if;

    -- Count existing IN-APP notifications so the in-app escalation ladder advances
    -- one caregiver at a time (push rows must not inflate this offset).
    select count(*)::integer
    into notified_count
    from public.notifications n
    where n.incident_id = new.id
      and n.channel = 'in_app';

    insert into public.notifications (incident_id, caregiver_id, channel, status)
    select new.id, c.caregiver_id, 'in_app', 'queued'
    from public.care_connections c
    where c.beneficiary_id = new.beneficiary_id
      and c.status = 'active'
    order by c.escalation_order, c.created_at
    offset notified_count
    limit 1
    on conflict (incident_id, caregiver_id, channel) do nothing;

    -- Broadcast a push row to EVERY active caregiver so the edge function can
    -- deliver an FCM push even if the caregiver's app/phone is closed.
    -- on-conflict guarantees one push row per (incident, caregiver).
    insert into public.notifications (incident_id, caregiver_id, channel, status)
    select new.id, c.caregiver_id, 'push', 'queued'
    from public.care_connections c
    where c.beneficiary_id = new.beneficiary_id
      and c.status = 'active'
    on conflict (incident_id, caregiver_id, channel) do nothing;

    -- If a push endpoint is configured, ask the send-push edge function to
    -- drain immediately. Wrapped so push delivery never blocks incident insert.
    if v_push_endpoint is not null and v_push_endpoint <> '' then
        begin
            perform net.http_post(
                url := v_push_endpoint,
                headers := jsonb_build_object(
                    'Content-Type', 'application/json',
                    'Authorization', 'Bearer ' || coalesce(v_push_secret, '')
                ),
                body := jsonb_build_object('incident_id', new.id)
            );
        exception when others then
            null;  -- pg_net missing or endpoint down — cron/webhook backstop drains later
        end;
    end if;

    return new;
end;
$$;

drop trigger if exists incidents_enqueue_notification on public.incidents;
create trigger incidents_enqueue_notification
    after insert or update of status on public.incidents
    for each row execute function public.enqueue_incident_notification();

notify pgrst, 'reload schema';

-- ---------------------------------------------------------------------------
-- (4b) Optional backstop: drain queued pushes every 30s via pg_cron + the edge
--      function. Enable ONLY after deploying the edge function and setting:
--        alter database <db> set app.push_endpoint =
--          'https://<project-ref>.functions.supabase.co/send-push';
--        alter database <db> set app.push_secret = '<shared-secret>';
--      Then uncomment the cron line below. pg_cron must be enabled on the DB.
-- ---------------------------------------------------------------------------
-- select cron.schedule('soundguard-push-drain', '*/30 * * * * *',
--   $$ select net.http_post(
--        url := current_setting('app.push_endpoint', true),
--        headers := jsonb_build_object('Content-Type','application/json',
--               'Authorization','Bearer '||coalesce(current_setting('app.push_secret',true),'')),
--        body := jsonb_build_object('drain', true)
--      ); $$);
