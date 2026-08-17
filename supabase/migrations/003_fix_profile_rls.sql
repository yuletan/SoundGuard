-- Allow users to insert their own profile during upsert / signup
create policy "users can insert their profile"
    on public.profiles for insert
    with check (id = auth.uid());
