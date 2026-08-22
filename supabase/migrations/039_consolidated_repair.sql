-- Consolidated repair for live projects that skipped some migrations.
-- Fixes the two failures reported by "Delete all account data":
--
--   1) RPC failed with 42P01 "relation public.device_push_tokens does not
--      exist"  ->  migration 004 was never applied. We create any missing
--      tables the RPC clears, AND make the RPC treat those deletes as
--      best-effort so a missing table can never abort the whole function.
--
--   2) Direct REST fallback failed with 42501 "new row violates row-level
--      security policy for table profiles"  ->  the live DB's profiles RLS
--      policies are stale (migration 029 not fully applied). Re-apply the
--      canonical owner-only policies.
--
-- Run once in Supabase Dashboard → SQL Editor, then retry
-- "Delete all account data" from the app's Settings screen.

alter table public.profiles add column if not exists deactivated_at timestamptz;

-- ---------------------------------------------------------------------------
-- 1) Tables the reset RPC clears — create only if missing (idempotent).
-- ---------------------------------------------------------------------------
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

create table if not exists public.beneficiary_settings (
    user_id uuid primary key references public.profiles(id) on delete cascade,
    consent_monitoring boolean not null default false,
    consent_share_with_caregiver boolean not null default false,
    consent_camera_requests boolean not null default false,
    emergency_phone text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
alter table public.beneficiary_settings enable row level security;

create table if not exists public.caregiver_settings (
    user_id uuid primary key references public.profiles(id) on delete cascade,
    notify_in_app boolean not null default true,
    notify_push boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
alter table public.caregiver_settings enable row level security;

-- ---------------------------------------------------------------------------
-- 2) Canonical profiles RLS (identical to migration 029). Re-applying fixes
--    the 42501 RLS violation on direct profile updates.
-- ---------------------------------------------------------------------------
drop policy if exists "users can view their profile" on public.profiles;
drop policy if exists "users can insert their profile" on public.profiles;
drop policy if exists "users can update their profile" on public.profiles;
drop policy if exists "connected users can view participant profiles" on public.profiles;

create policy "users can view their profile"
    on public.profiles for select to authenticated
    using (id = auth.uid());

create policy "users can insert their profile"
    on public.profiles for insert to authenticated
    with check (id = auth.uid());

create policy "users can update their profile"
    on public.profiles for update to authenticated
    using (id = auth.uid())
    with check (id = auth.uid());

create policy "connected users can view participant profiles"
    on public.profiles for select to authenticated
    using (
        id = auth.uid()
        or exists (
            select 1 from public.care_connections c
            where c.status = 'active'
              and (
                  (c.beneficiary_id = auth.uid() and c.caregiver_id = profiles.id)
                  or (c.caregiver_id = auth.uid() and c.beneficiary_id = profiles.id)
              )
        )
    );

alter table public.profiles enable row level security;

-- ---------------------------------------------------------------------------
-- 3) Resilient reset_my_account_data — optional-table deletes are wrapped so
--    a missing/locked table can never abort the deactivation itself.
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

    -- Best-effort deletes: skip silently if a table is missing/blocked.
    begin
        delete from public.device_push_tokens where user_id = current_user_id;
    exception when others then null; end;
    begin
        delete from public.beneficiary_settings where user_id = current_user_id;
    exception when others then null; end;
    begin
        delete from public.caregiver_settings where user_id = current_user_id;
    exception when others then null; end;

    -- Kept intentionally so the connected party still sees the chat with a
    -- "Deactivated" badge until they remove the connection (034 trigger):
    --   - public.care_connections
    --   - public.incidents / public.notifications / public.camera_snapshots

    -- The step that must succeed — errors here surface to the caller.
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
