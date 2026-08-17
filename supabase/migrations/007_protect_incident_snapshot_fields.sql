create or replace function public.protect_incident_fields()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
    if new.id is distinct from old.id
        or new.beneficiary_id is distinct from old.beneficiary_id
        or new.device_id is distinct from old.device_id
        or new.sound_label is distinct from old.sound_label
        or new.severity is distinct from old.severity
        or new.confidence is distinct from old.confidence
        or new.started_at is distinct from old.started_at
        or new.created_at is distinct from old.created_at then
        raise exception 'Incident identity and detection fields are immutable';
    end if;
    return new;
end;
$$;

drop trigger if exists protect_incident_fields on public.incidents;
create trigger protect_incident_fields
    before update on public.incidents
    for each row execute procedure public.protect_incident_fields();

create or replace function public.protect_snapshot_fields()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
    if new.id is distinct from old.id
        or new.incident_id is distinct from old.incident_id
        or new.beneficiary_id is distinct from old.beneficiary_id
        or new.requested_by is distinct from old.requested_by
        or new.camera_facing is distinct from old.camera_facing
        or new.storage_path is distinct from old.storage_path
        or new.requested_at is distinct from old.requested_at
        or new.expires_at > old.expires_at then
        raise exception 'Snapshot identity and expiry fields are immutable';
    end if;
    return new;
end;
$$;

drop trigger if exists protect_snapshot_fields on public.camera_snapshots;
create trigger protect_snapshot_fields
    before update on public.camera_snapshots
    for each row execute procedure public.protect_snapshot_fields();
