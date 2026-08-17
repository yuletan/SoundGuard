-- Keep this migration safe to rerun. The policy is already created by the
-- initial schema; replace it here so older databases receive the same rule.
drop policy if exists "users can insert their profile" on public.profiles;
create policy "users can insert their profile"
    on public.profiles for insert
    with check (id = auth.uid());
