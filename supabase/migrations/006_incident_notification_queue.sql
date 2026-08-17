create unique index if not exists notifications_incident_caregiver_channel_idx
    on public.notifications (incident_id, caregiver_id, channel);

create or replace function public.enqueue_incident_notification()
returns trigger
language plpgsql
security definer set search_path = public
as $$
declare
    notified_count integer;
begin
    if new.status = 'detected' and new.severity = 'low' then
        notified_count := 0;
    elsif new.status in ('caregiver_notified', 'escalated')
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
