-- 036_push_config_table.sql
--
-- Supabase no longer permits `alter database postgres set app.<custom>` from
-- the SQL editor (the postgres role is not a true superuser), so the push
-- endpoint/secret configured in migration 035 via database settings cannot be
-- set that way. This migration moves the configuration into an app_config
-- table and re-creates enqueue_incident_notification() to read from it.
--
-- The table is locked down: RLS enabled, no policies, all grants revoked from
-- anon/authenticated. Only the security-definer trigger (owner: postgres) and
-- dashboard/SQL-editor sessions can read or write it.

create table if not exists public.app_config (
    key text primary key,
    value text not null
);

alter table public.app_config enable row level security;
revoke all on public.app_config from anon, authenticated;

-- Seed/update the push dispatch config. Edit these values via the SQL editor.
insert into public.app_config (key, value) values
    ('push_endpoint', 'https://jyqtflafibjoacwlgexj.supabase.co/functions/v1/send-push'),
    ('push_secret',   'sg-push-9fK2mXq7Lw4RtZ8p')
on conflict (key) do update set value = excluded.value;

-- Re-create the incident notification trigger to source its config from
-- app_config instead of current_setting('app.push_endpoint' / 'app.push_secret').
create or replace function public.enqueue_incident_notification()
returns trigger
language plpgsql
security definer set search_path = public
as $$
declare
    notified_count integer;
    v_push_endpoint text;
    v_push_secret text;
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

    -- Push dispatch config lives in app_config (see header note).
    select value into v_push_endpoint from public.app_config where key = 'push_endpoint';
    select value into v_push_secret   from public.app_config where key = 'push_secret';

    -- Count existing notifications so the in-app escalation ladder advances
    -- one caregiver at a time (original behaviour from migration 027).
    select count(*)::integer
    into notified_count
    from public.notifications n
    where n.incident_id = new.id;

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
    insert into public.notifications (incident_id, caregiver_id, channel, status)
    select new.id, c.caregiver_id, 'push', 'queued'
    from public.care_connections c
    where c.beneficiary_id = new.beneficiary_id
      and c.status = 'active'
    on conflict (incident_id, caregiver_id, channel) do nothing;

    -- Ping the send-push edge function to drain immediately. Wrapped so push
    -- delivery never blocks incident insert.
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
