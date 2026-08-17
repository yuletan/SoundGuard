create extension if not exists pgcrypto;

create table public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    email text,
    full_name text,
    phone text,
    role text check (role in ('beneficiary', 'caregiver')),
    setup_completed_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table public.beneficiary_settings (
    user_id uuid primary key references public.profiles(id) on delete cascade,
    consent_monitoring boolean not null default false,
    consent_share_with_caregiver boolean not null default false,
    consent_camera_requests boolean not null default false,
    emergency_phone text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table public.caregiver_settings (
    user_id uuid primary key references public.profiles(id) on delete cascade,
    notify_in_app boolean not null default true,
    notify_push boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table public.care_connections (
    id uuid primary key default gen_random_uuid(),
    beneficiary_id uuid not null references public.profiles(id) on delete cascade,
    caregiver_id uuid not null references public.profiles(id) on delete cascade,
    status text not null default 'invited' check (
        status in ('invited', 'active', 'declined', 'revoked')
    ),
    is_primary boolean not null default false,
    escalation_order integer not null default 1,
    invited_by uuid not null references public.profiles(id),
    invited_at timestamptz not null default now(),
    accepted_at timestamptz,
    created_at timestamptz not null default now(),
    unique (beneficiary_id, caregiver_id)
);

create table public.devices (
    id uuid primary key default gen_random_uuid(),
    beneficiary_id uuid not null references public.profiles(id) on delete cascade,
    name text not null,
    location text,
    status text not null default 'inactive' check (status in ('inactive', 'ready', 'monitoring', 'paused')),
    last_seen_at timestamptz,
    created_at timestamptz not null default now()
);

create table public.incidents (
    id uuid primary key default gen_random_uuid(),
    beneficiary_id uuid not null references public.profiles(id) on delete cascade,
    device_id uuid references public.devices(id) on delete set null,
    sound_label text not null,
    severity text not null check (severity in ('low', 'high')),
    confidence numeric(5, 4) not null check (confidence >= 0 and confidence <= 1),
    status text not null default 'detected' check (
        status in ('detected', 'waiting_user', 'caregiver_notified', 'caregiver_acknowledged', 'resolved', 'false_alarm', 'escalated')
    ),
    user_response text,
    caregiver_response text,
    started_at timestamptz not null default now(),
    resolved_at timestamptz,
    created_at timestamptz not null default now()
);

create table public.notifications (
    id uuid primary key default gen_random_uuid(),
    incident_id uuid not null references public.incidents(id) on delete cascade,
    caregiver_id uuid not null references public.profiles(id) on delete cascade,
    channel text not null check (channel in ('in_app', 'push')),
    status text not null default 'sent' check (status in ('queued', 'sent', 'acknowledged', 'failed')),
    sent_at timestamptz,
    acknowledged_at timestamptz,
    created_at timestamptz not null default now()
);

create table public.camera_snapshots (
    id uuid primary key default gen_random_uuid(),
    incident_id uuid not null references public.incidents(id) on delete cascade,
    beneficiary_id uuid not null references public.profiles(id) on delete cascade,
    requested_by uuid not null references public.profiles(id),
    camera_facing text not null check (camera_facing in ('front', 'rear')),
    storage_path text,
    status text not null default 'requested' check (status in ('requested', 'uploaded', 'viewed', 'expired', 'failed')),
    requested_at timestamptz not null default now(),
    expires_at timestamptz not null default (now() + interval '10 minutes'),
    viewed_at timestamptz,
    created_at timestamptz not null default now()
);

create index care_connections_beneficiary_idx on public.care_connections (beneficiary_id, status, escalation_order);
create index care_connections_caregiver_idx on public.care_connections (caregiver_id, status);
create index incidents_beneficiary_idx on public.incidents (beneficiary_id, created_at desc);
create index notifications_caregiver_idx on public.notifications (caregiver_id, created_at desc);
create index camera_snapshots_expiry_idx on public.camera_snapshots (expires_at);

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
    insert into public.profiles (id, email)
    values (new.id, new.email)
    on conflict (id) do nothing;
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute procedure public.handle_new_user();

alter table public.profiles enable row level security;
alter table public.beneficiary_settings enable row level security;
alter table public.caregiver_settings enable row level security;
alter table public.care_connections enable row level security;
alter table public.devices enable row level security;
alter table public.incidents enable row level security;
alter table public.notifications enable row level security;
alter table public.camera_snapshots enable row level security;

create policy "users can view their profile"
    on public.profiles for select
    using (id = auth.uid());

create policy "users can update their profile"
    on public.profiles for update
    using (id = auth.uid())
    with check (id = auth.uid());

create policy "users can view their settings"
    on public.beneficiary_settings for select
    using (user_id = auth.uid());

create policy "beneficiaries can update their settings"
    on public.beneficiary_settings for all
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

create policy "users can view caregiver settings"
    on public.caregiver_settings for select
    using (user_id = auth.uid());

create policy "caregivers can update their settings"
    on public.caregiver_settings for all
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

create policy "connected users can view care connections"
    on public.care_connections for select
    using (beneficiary_id = auth.uid() or caregiver_id = auth.uid());

create policy "participants can update care connections"
    on public.care_connections for update
    using (beneficiary_id = auth.uid() or caregiver_id = auth.uid())
    with check (beneficiary_id = auth.uid() or caregiver_id = auth.uid());

create policy "users can view beneficiary devices"
    on public.devices for select
    using (
        beneficiary_id = auth.uid()
        or exists (
            select 1 from public.care_connections c
            where c.beneficiary_id = devices.beneficiary_id
              and c.caregiver_id = auth.uid()
              and c.status = 'active'
        )
    );

create policy "beneficiaries can manage devices"
    on public.devices for all
    using (beneficiary_id = auth.uid())
    with check (beneficiary_id = auth.uid());

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

create policy "beneficiaries can create incidents"
    on public.incidents for insert
    with check (beneficiary_id = auth.uid());

create policy "connected users can update incidents"
    on public.incidents for update
    using (
        beneficiary_id = auth.uid()
        or exists (
            select 1 from public.care_connections c
            where c.beneficiary_id = incidents.beneficiary_id
              and c.caregiver_id = auth.uid()
              and c.status = 'active'
        )
    );

create policy "caregivers can view their notifications"
    on public.notifications for select
    using (caregiver_id = auth.uid());

create policy "caregivers can acknowledge notifications"
    on public.notifications for update
    using (caregiver_id = auth.uid())
    with check (caregiver_id = auth.uid());

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

create policy "connected caregivers can request snapshots"
    on public.camera_snapshots for insert
    with check (
        requested_by = auth.uid()
        and exists (
            select 1 from public.care_connections c
            where c.beneficiary_id = camera_snapshots.beneficiary_id
              and c.caregiver_id = auth.uid()
              and c.status = 'active'
        )
    );

create policy "participants can update snapshots"
    on public.camera_snapshots for update
    using (requested_by = auth.uid() or beneficiary_id = auth.uid())
    with check (requested_by = auth.uid() or beneficiary_id = auth.uid());
