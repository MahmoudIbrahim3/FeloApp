-- Scan a YouTube playlist BEFORE curating it. The scan-youtube-playlist Edge Function checks every
-- video with the same rules as refresh-youtube-data (made for kids, embeddable, public or unlisted,
-- processed, not age-restricted, playable in TARGET_REGIONS), so only videos that would pass are
-- worth watching. Nothing is written to public.*: the answer only lives in net._http_response,
-- which pg_net clears after a few hours.
--
--   select internal.scan_youtube_playlist('https://www.youtube.com/playlist?list=PL...');  -- request id
--   select * from internal.playlist_scan_result(<request id>);                              -- a few seconds later
--
-- Uses the same Vault secrets as internal.run_youtube_refresh: project_url and refresh_secret.
-- Everything lives in `internal`, which the API doesn't expose, and anon/authenticated can't execute.

-- Starts a scan. Returns the pg_net request id to pass to internal.playlist_scan_result.
create or replace function internal.scan_youtube_playlist(playlist text)
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
  if playlist is null or length(btrim(playlist)) = 0 then
    raise exception 'Pass a playlist link or id, e.g. https://www.youtube.com/playlist?list=PL...';
  end if;

  select decrypted_secret into base_url from vault.decrypted_secrets where name = 'project_url';
  select decrypted_secret into secret from vault.decrypted_secrets where name = 'refresh_secret';
  if base_url is null or secret is null then
    raise exception 'Vault secrets project_url and refresh_secret are required (see supabase/README.md)';
  end if;

  select net.http_post(
    url := rtrim(base_url, '/') || '/functions/v1/scan-youtube-playlist',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'x-refresh-secret', secret
    ),
    body := jsonb_build_object('playlist', btrim(playlist)),
    timeout_milliseconds := 60000
  ) into request_id;
  return request_id;
end;
$$;

-- Turns the function's answer into one row per playlist entry, in playlist order.
-- `reasons` is empty when the video is available; `in_catalog` is its status in public.videos
-- (null = not added yet). Raises the function's error for any answer other than 200.
-- Separate from playlist_scan_result so the parsing can be tested without pg_net.
create or replace function internal.parse_playlist_scan(status_code integer, content text)
returns table (
  "position" integer,
  youtube_id text,
  title text,
  duration_sec integer,
  made_for_kids boolean,
  embeddable boolean,
  available boolean,
  reasons text,
  in_catalog text
)
language plpgsql
stable
set search_path = ''
as $$
#variable_conflict use_column
declare
  body jsonb;
begin
  begin
    body := content::jsonb;
  exception when others then
    raise exception 'scan-youtube-playlist answered % with a non-JSON body: %',
      status_code, left(coalesce(content, ''), 300);
  end;

  if status_code is distinct from 200 then
    raise exception 'scan-youtube-playlist answered %: %',
      status_code, coalesce(body ->> 'error', left(coalesce(content, ''), 300));
  end if;

  if coalesce((body ->> 'truncated')::boolean, false) then
    raise notice 'Only the first % videos were scanned (the playlist has %).',
      body ->> 'scanned', body ->> 'total_in_playlist';
  end if;

  return query
  select v."position",
         v.youtube_id,
         v.title,
         v.duration_sec,
         v.made_for_kids,
         v.embeddable,
         v.available,
         nullif(array_to_string(array(
           select jsonb_array_elements_text(coalesce(v.reasons, '[]'::jsonb))
         ), ', '), ''),
         c.status::text
  from jsonb_to_recordset(coalesce(body -> 'videos', '[]'::jsonb)) as v(
    "position" integer,
    youtube_id text,
    title text,
    duration_sec integer,
    made_for_kids boolean,
    embeddable boolean,
    available boolean,
    reasons jsonb
  )
  left join public.videos c on c.youtube_id = v.youtube_id
  order by v."position";
end;
$$;

-- Reads the answer of a scan started with internal.scan_youtube_playlist.
create or replace function internal.playlist_scan_result(request_id bigint)
returns table (
  "position" integer,
  youtube_id text,
  title text,
  duration_sec integer,
  made_for_kids boolean,
  embeddable boolean,
  available boolean,
  reasons text,
  in_catalog text
)
language plpgsql
stable
set search_path = ''
as $$
#variable_conflict use_column
declare
  answer record;
begin
  select r.status_code, r.content, r.timed_out, r.error_msg
  into answer
  from net._http_response r
  where r.id = request_id;

  if not found then
    raise exception 'No answer yet for request % (wait a few seconds and run this again; pg_net keeps answers for a few hours)',
      request_id;
  end if;
  if coalesce(answer.timed_out, false) or answer.error_msg is not null then
    raise exception 'Request % failed: %', request_id, coalesce(answer.error_msg, 'timed out');
  end if;

  return query select * from internal.parse_playlist_scan(answer.status_code, answer.content);
end;
$$;

revoke execute on function internal.scan_youtube_playlist(text) from public, anon, authenticated;
revoke execute on function internal.parse_playlist_scan(integer, text) from public, anon, authenticated;
revoke execute on function internal.playlist_scan_result(bigint) from public, anon, authenticated;
