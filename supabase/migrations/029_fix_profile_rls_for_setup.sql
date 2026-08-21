-- Fix: "Profile save failed with HTTP 403: new row violates RLS for table profiles"
-- Setup calls POST /rest/v1/profiles with Prefer: resolution=merge-duplicates (upsert)
-- which needs BOTH INSERT (WITH CHECK) and UPDATE (USING + WITH CHECK) to pass.
-- Previous policies were ambiguous about role and TO target; make them explicit for
-- authenticated users and keep the connected-profile visibility.

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

-- The handle_new_user trigger is security definer so it bypasses RLS, but ensure
-- the table is still RLS-enabled.
alter table public.profiles enable row level security;

notify pgrst, 'reload schema';
