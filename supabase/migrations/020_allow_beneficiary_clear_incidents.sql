create policy "beneficiaries can delete their incidents"
    on public.incidents for delete
    using (beneficiary_id = auth.uid());
