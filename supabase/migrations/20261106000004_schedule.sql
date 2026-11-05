-- Daily schedule for refresh-youtube-data (pg_cron + pg_net), plus a daily SQL-only sweep so
-- YouTube data never outlives 30 days even if the function fails.
--
-- Needs two Vault secrets, created once by hand (see supabase/README.md), never in a migration:
--   project_url     https://<project-ref>.supabase.co
--   refresh_secret  the same value as the function secret REFRESH_SECRET

create extension if not exists pg_cron;
create extension if not exists pg_net;

-- Not exposed through the API (only public is), so the app can never call what lives here.
create schema if not exists internal;
revoke all on schema internal from public, anon, authenticated;

-- Calls the Edge Function. Run it by hand after adding videos:
--   select internal.run_youtube_refresh();
-- The function runs asynchronously; results land in public.refresh_runs.
create or replace function internal.run_youtube_refresh(trigger_name text default 'manual')
returns bigint
language plpgsql
security definer
set search_path = ''
as $$
declare
  base_url text;
  secret text;
  request_id bigint;
begin
  select decrypted_secret into base_url from vault.decrypted_secrets where name = 'project_url';
  select decrypted_secret into secret from vault.decrypted_secrets where name = 'refresh_secret';
  if base_url is null or secret is null then
    raise exception 'Vault secrets project_url and refresh_secret are required (see supabase/README.md)';
  end if;

  select net.http_post(
    url := rtrim(base_url, '/') || '/functions/v1/refresh-youtube-data',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'x-refresh-secret', secret
    ),
    body := jsonb_build_object('trigger', trigger_name),
    timeout_milliseconds := 300000
  ) into request_id;
  return request_id;
end;
$$;

revoke execute on function internal.run_youtube_refresh(text) from public, anon, authenticated;

-- 03:17 UTC daily (off the hour to avoid the busy :00 slot).
select cron.schedule(
  'refresh-youtube-data',
  '17 3 * * *',
  $$ select internal.run_youtube_refresh('cron'); $$
);

-- 04:47 UTC daily: clear anything that still passed 30 days without a refresh.
select cron.schedule(
  'sweep-stale-youtube-data',
  '47 4 * * *',
  $$ select public.sweep_stale_youtube_data(); $$
);
