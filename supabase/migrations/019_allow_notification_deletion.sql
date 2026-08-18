create policy "caregivers can delete their notifications"
    on public.notifications for delete
    using (caregiver_id = auth.uid());
