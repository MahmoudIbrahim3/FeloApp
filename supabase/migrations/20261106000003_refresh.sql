-- Server side of refresh-youtube-data: apply one batch of YouTube API results, clear YouTube
-- data older than 30 days, and keep a log of every run. Callable by service_role only.

create table public.refresh_runs (
  id uuid primary key default gen_random_uuid(),
  started_at timestamptz not null,
  finished_at timestamptz not null default now(),
  trigger text not null default 'manual',
  checked integer not null default 0,
  refreshed integer not null default 0,
  unavailable integer not null default 0,
  cleared integer not null default 0,
  api_calls integer not null default 0,
  quota_units integer not null default 0,
  errors jsonb not null default '[]'::jsonb
);

comment on table public.refresh_runs is 'One row per refresh-youtube-data run (counts and YouTube quota used).';

alter table public.refresh_runs enable row level security;
revoke all on table public.refresh_runs from anon, authenticated;

-- Clears YouTube data that could not be refreshed for 30 days (YouTube API policy). The row
-- itself and its status stay; RLS already hides it from the app.
create or replace function public.sweep_stale_youtube_data()
returns integer
language plpgsql
set search_path = ''
as $$
declare
  cleared integer;
begin
  update public.videos
  set yt_duration_sec = null,
      yt_thumbnail_url = null,
      yt_made_for_kids = null,
      yt_embeddable = null,
      yt_refreshed_at = null
  where yt_refreshed_at is not null
    and yt_refreshed_at <= now() - interval '30 days';
  get diagnostics cleared = row_count;
  return cleared;
end;
$$;

-- Applies one batch from the YouTube API. payload = array of
--   { "youtube_id": "...", "found": bool, "available": bool,
--     "duration_sec": int, "thumbnail_url": text, "made_for_kids": bool, "embeddable": bool }
-- * found = false  → status 'unavailable', every yt_* field cleared.
-- * found = true   → yt_* fields written, yt_refreshed_at = now();
--                    available = false → status 'unavailable' (draft and approved alike).
-- Drafts never become approved here: a person approves after review.
create or replace function public.apply_youtube_refresh(payload jsonb)
returns jsonb
language plpgsql
set search_path = ''
as $$
declare
  refreshed integer;
  missing integer;
  unavailable integer;
begin
  with item as (
    select * from jsonb_to_recordset(payload) as x(
      youtube_id text,
      found boolean,
      available boolean,
      duration_sec integer,
      thumbnail_url text,
      made_for_kids boolean,
      embeddable boolean
    )
  )
  update public.videos v
  set yt_duration_sec = item.duration_sec,
      yt_thumbnail_url = item.thumbnail_url,
      yt_made_for_kids = item.made_for_kids,
      yt_embeddable = item.embeddable,
      yt_refreshed_at = now(),
      status = case when item.available then v.status else 'unavailable'::public.video_status end
  from item
  where v.youtube_id = item.youtube_id and item.found;
  get diagnostics refreshed = row_count;

  with item as (
    select x.youtube_id from jsonb_to_recordset(payload) as x(youtube_id text, found boolean)
    where not x.found
  )
  update public.videos v
  set status = 'unavailable',
      yt_duration_sec = null,
      yt_thumbnail_url = null,
      yt_made_for_kids = null,
      yt_embeddable = null,
      yt_refreshed_at = null
  from item
  where v.youtube_id = item.youtube_id;
  get diagnostics missing = row_count;

  select count(*) into unavailable
  from jsonb_to_recordset(payload) as x(found boolean, available boolean)
  where not x.found or not x.available;

  return jsonb_build_object('refreshed', refreshed, 'missing', missing, 'unavailable', unavailable);
end;
$$;

revoke execute on function public.sweep_stale_youtube_data() from public, anon, authenticated;
revoke execute on function public.apply_youtube_refresh(jsonb) from public, anon, authenticated;
grant execute on function public.sweep_stale_youtube_data() to service_role;
grant execute on function public.apply_youtube_refresh(jsonb) to service_role;
