-- MOTO HUB: expired location coordinates are hidden immediately by RLS;
-- physically purge them on a short, automated schedule as well.
-- Supabase PostgreSQL pg_cron runs the command using database privileges, not the mobile client.
create extension if not exists pg_cron with schema pg_catalog;
select cron.schedule('moto-hub-expire-gps', '*/5 * * * *', $$
  delete from public.mh_positions where expires_at < clock_timestamp();
  delete from public.mh_shares where active = false
    and not exists(select 1 from public.mh_positions p where p.session_id = mh_shares.id);
$$);
