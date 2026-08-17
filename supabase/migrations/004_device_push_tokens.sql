create table if not exists public.device_push_tokens (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    token text not null unique,
    platform text not null default 'android' check (platform = 'android'),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists device_push_tokens_user_idx on public.device_push_tokens (user_id);

alter table public.device_push_tokens enable row level security;

drop policy if exists "users can manage their push tokens" on public.device_push_tokens;
create policy "users can manage their push tokens"
    on public.device_push_tokens for all
    using (user_id = auth.uid())
    with check (user_id = auth.uid());
