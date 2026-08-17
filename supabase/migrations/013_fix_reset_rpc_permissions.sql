grant usage on schema public to anon, authenticated;
grant execute on function public.reset_my_account_data() to anon, authenticated;

notify pgrst, 'reload schema';
