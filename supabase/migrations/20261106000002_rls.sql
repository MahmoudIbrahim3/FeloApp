-- Row level security. The app uses the publishable key (role anon). It may:
--   * read published collections,
--   * read approved, embeddable videos refreshed within 30 days, in published collections,
--   * insert into video_reports.
-- Nothing else. v1 has no login, so `authenticated` gets nothing either.

alter table public.collections enable row level security;
alter table public.videos enable row level security;
alter table public.video_reports enable row level security;

-- Supabase grants everything on new public tables to anon/authenticated by default:
-- start from nothing, then grant only what the app needs.
revoke all on table public.collections, public.videos, public.video_reports from anon, authenticated;

grant select on table public.collections to anon;
grant select on table public.videos to anon;
grant insert on table public.video_reports to anon;

create policy "anon reads published collections"
  on public.collections
  for select
  to anon
  using (is_published);

create policy "anon reads approved fresh embeddable videos"
  on public.videos
  for select
  to anon
  using (
    status = 'approved'
    and yt_embeddable is true
    and yt_refreshed_at > now() - interval '30 days'
    and exists (
      select 1 from public.collections c
      where c.id = videos.collection_id and c.is_published
    )
  );

-- The table's check constraints limit what can be written (id format, known reasons).
create policy "anon inserts reports"
  on public.video_reports
  for insert
  to anon
  with check (true);

-- Functions are executable by PUBLIC by default; only the trigger function exists so far.
revoke execute on function public.set_updated_at() from public, anon, authenticated;
