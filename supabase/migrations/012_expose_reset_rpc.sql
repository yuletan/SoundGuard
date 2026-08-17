grant execute on function public.reset_my_account_data() to authenticated;

-- Make the new RPC visible to PostgREST immediately after migration.
notify pgrst, 'reload schema';
