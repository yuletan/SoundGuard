-- Severity tiers: high (red), medium (orange), low (hidden/noise). Low should
-- never appear in caregiver chats and should not trigger escalation.
-- Backfill uses the real YAMNet labels to separate medium from low noise.

alter table public.incidents
    drop constraint if exists incidents_severity_check;
alter table public.incidents
    add constraint incidents_severity_check
        check (severity in ('low', 'medium', 'high'));

-- Medium is a real alert (shown orange), but not an escalated safety event.
-- Only high triggers caregiver notifications / escalation queue.
update public.incidents
set severity = case
    when lower(sound_label) in (
        'crying detected', 'cough / sneeze / snore', 'thunder', 'door / doorbell / knock'
    ) then 'medium'
    when severity = 'low' then 'low'
    else severity
end
where severity in ('low', 'high');

-- Keep the trigger from creating notifications for low (and now medium/detected as well if needed).
-- Medium 'detected' rows are visible in chat but don't go through the escalation queue.
-- Adjust enqueue to only trigger for waiting_user / caregiver_notified / escalated.
create or replace function public.enqueue_incident_notification()
returns trigger
language plpgsql
security definer set search_path = public
as $$
declare
    notified_count integer;
begin
    -- Low and medium 'detected' are local history only — do not queue caregivers.
    if new.status = 'detected' then
        return new;
    end if;

    if new.status in ('waiting_user', 'caregiver_notified', 'escalated')
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

notify pgrst, 'reload schema';
