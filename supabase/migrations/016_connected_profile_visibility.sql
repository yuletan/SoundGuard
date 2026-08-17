drop policy if exists "connected users can view participant profiles" on public.profiles;
create policy "connected users can view participant profiles"
    on public.profiles for select
    using (
        id = auth.uid()
        or exists (
            select 1
            from public.care_connections c
            where c.status = 'active'
              and (
                  (c.beneficiary_id = auth.uid() and c.caregiver_id = profiles.id)
                  or (c.caregiver_id = auth.uid() and c.beneficiary_id = profiles.id)
              )
        )
    );
