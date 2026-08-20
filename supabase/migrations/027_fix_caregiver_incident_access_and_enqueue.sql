-- Fix 1: caregiver reads were blocked for historical data.
-- Your beneficiary chat (left screenshot) shows incidents directly;
-- the caregiver chat tried to read via `notifications -> incidents` join.
-- Old `connected users can view incidents` required the *caregiver* to have
-- already had notifications, and the old enqueue trigger only fired for
-- waiting_user/caregiver_notified — so if RLS/trigger ever mis-fired,
-- caregiver saw "No incidents yet" (right screenshot) forever.

-- Widen incident visibility: any active care_connection is enough,
-- regardless of notification state.
drop policy if exists "connected users can view incidents" on public.incidents;
create policy "connected users can view incidents"
    on public.incidents for select
    using (
        beneficiary_id = auth.uid()
        or exists (
            select 1 from public.care_connections c
            where c.beneficiary_id = incidents.beneficiary_id
              and c.caregiver_id = auth.uid()
              and c.status = 'active'
        )
    );

-- Fix 2: also allow caregiver to see camera_snapshots rows for the chat thumbnails
-- even when the old policy's join failed. Public is already covered by the
-- storage policy; this just fixes the table RLS.
drop policy if exists "connected users can view snapshots" on public.camera_snapshots;
create policy "connected users can view snapshots"
    on public.camera_snapshots for select
    using (
        requested_by = auth.uid()
        or beneficiary_id = auth.uid()
        or exists (
            select 1 from public.care_connections c
            where c.beneficiary_id = camera_snapshots.beneficiary_id
              and c.caregiver_id = auth.uid()
              and c.status = 'active'
        )
    );

-- Fix 3: make enqueue actually create a notification for every high-severity
-- incident (the thing that populates caregiver chat). Keep medium=visible but
-- no escalation, low=never. Also backfill any missed high incidents.
create or replace function public.enqueue_incident_notification()
returns trigger
language plpgsql
security definer set search_path = public
as $$
declare
    notified_count integer;
begin
    if new.status = 'detected' then
        return new;
    end if;

    -- Collapse _all_ high/medium waiting into one path: queue if HIGH,
    -- otherwise just return (medium is chat-visible locally, no queue).
    if new.severity = 'high'
        and new.status in ('waiting_user', 'caregiver_notified', 'escalated')
        and (tg_op = 'INSERT' or old.status is distinct from new.status) then
        select count(*)::integer
        into notified_count
        from public.notifications n
        where n.incident_id = new.id;
    else
        return new;
    end if;

    insert into public.notifications (incident_id, caregiver_id, channel, status)
    select new.id, c.caregiver_id, 'in_app', 'queued'
    from public.care_connections c
    where c.beneficiary_id = new.beneficiary_id
      and c.status = 'active'
    order by c.escalation_order, c.created_at
    offset notified_count
    limit 1
    on conflict (incident_id, caregiver_id, channel) do nothing;

    return new;
end;
$$;

drop trigger if exists incidents_enqueue_notification on public.incidents;
create trigger incidents_enqueue_notification
    after insert or update of status on public.incidents
    for each row execute procedure public.enqueue_incident_notification();

-- Backfill: for every existing high incident that never got a notification
-- (which is why caregiver Chat was empty), create the missing row now.
insert into public.notifications (incident_id, caregiver_id, channel, status)
select i.id, c.caregiver_id, 'in_app', 'queued'
from public.incidents i
join public.care_connections c
  on c.beneficiary_id = i.beneficiary_id and c.status = 'active'
where i.severity = 'high'
  and i.status in ('waiting_user', 'caregiver_notified', 'escalated', 'acknowledged', 'detected')
  and not exists (
      select 1 from public.notifications n
      where n.incident_id = i.id and n.caregiver_id = c.caregiver_id and n.channel = 'in_app'
  )
on conflict (incident_id, caregiver_id, channel) do nothing;

notify pgrst, 'reload schema';
