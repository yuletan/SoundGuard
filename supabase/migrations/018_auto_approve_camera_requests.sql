alter table public.beneficiary_settings
    add column if not exists auto_approve_camera_requests boolean not null default true;

alter table public.camera_snapshots
    add column if not exists approval_status text not null default 'pending'
        check (approval_status in ('pending', 'approved', 'declined'));

create or replace function public.apply_camera_request_preference()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
    if exists (
        select 1 from public.beneficiary_settings
        where user_id = new.beneficiary_id
          and auto_approve_camera_requests = true
    ) then
        new.approval_status := 'approved';
    end if;
    return new;
end;
$$;

drop trigger if exists camera_request_preference on public.camera_snapshots;
create trigger camera_request_preference
    before insert on public.camera_snapshots
    for each row execute procedure public.apply_camera_request_preference();
