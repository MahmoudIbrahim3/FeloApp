-- ClipKid catalog: curated collections and videos, plus anonymous video reports.
-- YouTube API fields (yt_*) are written only by the refresh-youtube-data Edge Function and are
-- never kept longer than 30 days (see 20261106000003_refresh.sql).

create type public.video_status as enum ('draft', 'approved', 'unavailable');

-- Keeps updated_at current; the app syncs incrementally by comparing it.
create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

create table public.collections (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique check (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
  title_ar text not null check (length(btrim(title_ar)) > 0),
  cover_bg text not null check (cover_bg ~ '^#[0-9A-Fa-f]{6}$'),
  cover_fg text not null check (cover_fg ~ '^#[0-9A-Fa-f]{6}$'),
  -- Exactly one mark: a short glyph ("أ ب") or a bundled icon key ("book", "heart", "leaf", "moon").
  cover_glyph text,
  cover_icon text,
  age_bands text[] not null check (
    cardinality(age_bands) > 0 and age_bands <@ array['2-3', '4-6', '7-9', '10-12']
  ),
  sort_order integer not null default 0,
  is_premium boolean not null default false,
  is_published boolean not null default false,
  updated_at timestamptz not null default now(),
  constraint collections_one_cover_mark check (
    (cover_glyph is not null and length(btrim(cover_glyph)) > 0 and cover_icon is null)
    or (cover_icon is not null and length(btrim(cover_icon)) > 0 and cover_glyph is null)
  )
);

comment on table public.collections is 'Curated collections. Only published ones reach the app.';

create table public.videos (
  youtube_id text primary key check (youtube_id ~ '^[A-Za-z0-9_-]{11}$'),
  collection_id uuid not null references public.collections (id) on update cascade on delete restrict,
  -- OUR curated Arabic title, never YouTube's.
  title_ar text not null check (length(btrim(title_ar)) > 0),
  tags text[] not null default '{}',
  age_bands text[] not null check (
    cardinality(age_bands) > 0 and age_bands <@ array['2-3', '4-6', '7-9', '10-12']
  ),
  sort_order integer not null default 0,
  status public.video_status not null default 'draft',
  -- YouTube Data API fields. Written by refresh-youtube-data only; cleared after 30 days.
  yt_duration_sec integer check (yt_duration_sec is null or yt_duration_sec >= 0),
  yt_thumbnail_url text,
  yt_made_for_kids boolean,
  yt_embeddable boolean,
  yt_refreshed_at timestamptz,
  updated_at timestamptz not null default now()
);

comment on table public.videos is
  'Curated videos. Add as draft, run refresh-youtube-data, review, then set approved.';
comment on column public.videos.title_ar is 'Our curated Arabic title (not the YouTube title).';
comment on column public.videos.yt_refreshed_at is 'Last YouTube API refresh. Older than 30 days = hidden and cleared.';

create index videos_collection_idx on public.videos (collection_id, sort_order);
create index videos_refresh_idx on public.videos (status, yt_refreshed_at);

create trigger collections_set_updated_at
  before insert or update on public.collections
  for each row execute function public.set_updated_at();

create trigger videos_set_updated_at
  before insert or update on public.videos
  for each row execute function public.set_updated_at();

-- Anonymous reports from the app: a parent hid a video, or the player found it removed or not
-- embeddable. Deliberately no device, user or child identifiers.
create table public.video_reports (
  id uuid primary key default gen_random_uuid(),
  youtube_id text not null check (youtube_id ~ '^[A-Za-z0-9_-]{11}$'),
  reason text not null check (reason in ('parent_hidden', 'video_removed', 'embed_disabled')),
  created_at timestamptz not null default now()
);

create index video_reports_youtube_idx on public.video_reports (youtube_id, created_at desc);

comment on table public.video_reports is 'Insert-only anonymous reports from the app. No identifiers.';
