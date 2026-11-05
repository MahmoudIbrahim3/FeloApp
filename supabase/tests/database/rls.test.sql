-- RLS and grants for the app's role (anon). Run with: supabase test db
begin;
create extension if not exists pgtap with schema extensions;
set search_path = public, extensions;

select plan(24);

-- Fixtures (as the test owner, which bypasses RLS) ------------------------------------------
insert into public.collections (id, slug, title_ar, cover_bg, cover_fg, cover_glyph, age_bands, sort_order, is_published)
values
  ('00000000-0000-0000-0000-000000000001', 'test-published', 'منشورة', '#FFFFFF', '#000000', 'أ', array['2-3'], 1, true),
  ('00000000-0000-0000-0000-000000000002', 'test-hidden', 'مخفية', '#FFFFFF', '#000000', 'ب', array['2-3'], 2, false);

insert into public.videos
  (youtube_id, collection_id, title_ar, age_bands, status, yt_duration_sec, yt_thumbnail_url, yt_made_for_kids, yt_embeddable, yt_refreshed_at)
values
  -- visible
  ('visible0001', '00000000-0000-0000-0000-000000000001', 'ظاهر', array['2-3'], 'approved', 60, 'https://i.ytimg.com/vi/visible0001/hqdefault.jpg', true, true, now() - interval '1 day'),
  -- hidden: draft
  ('draft000001', '00000000-0000-0000-0000-000000000001', 'مسودة', array['2-3'], 'draft', 60, 'https://x', true, true, now()),
  -- hidden: unavailable
  ('unavail0001', '00000000-0000-0000-0000-000000000001', 'مش متاح', array['2-3'], 'unavailable', 60, 'https://x', true, false, now()),
  -- hidden: approved but not embeddable
  ('noembed0001', '00000000-0000-0000-0000-000000000001', 'مش embed', array['2-3'], 'approved', 60, 'https://x', true, false, now()),
  -- hidden: approved but YouTube data older than 30 days
  ('stale000001', '00000000-0000-0000-0000-000000000001', 'قديم', array['2-3'], 'approved', 60, 'https://x', true, true, now() - interval '31 days'),
  -- hidden: approved but never refreshed
  ('never000001', '00000000-0000-0000-0000-000000000001', 'جديد', array['2-3'], 'approved', null, null, null, null, null),
  -- hidden: approved, fresh, but its collection is not published
  ('unpubcol001', '00000000-0000-0000-0000-000000000002', 'مجموعة مخفية', array['2-3'], 'approved', 60, 'https://x', true, true, now());

-- As the app (publishable key = anon) --------------------------------------------------------
set local role anon;

select results_eq(
  $$ select slug from public.collections where slug like 'test-%' order by slug $$,
  $$ values ('test-published') $$,
  'anon reads published collections only'
);

select results_eq(
  $$ select youtube_id from public.videos order by youtube_id $$,
  $$ values ('visible0001') $$,
  'anon reads only approved, embeddable, fresh videos in published collections'
);

select is_empty($$ select 1 from public.videos where status = 'draft' $$, 'anon cannot read drafts');
select is_empty($$ select 1 from public.videos where status = 'unavailable' $$, 'anon cannot read unavailable videos');
select is_empty($$ select 1 from public.videos where youtube_id = 'stale000001' $$, 'anon cannot read videos with YouTube data older than 30 days');

select throws_ok(
  $$ insert into public.videos (youtube_id, collection_id, title_ar, age_bands)
     values ('evil0000001', '00000000-0000-0000-0000-000000000001', 'x', array['2-3']) $$,
  '42501', null, 'anon cannot insert videos'
);
select throws_ok(
  $$ update public.videos set title_ar = 'x' where youtube_id = 'visible0001' $$,
  '42501', null, 'anon cannot update videos'
);
select throws_ok(
  $$ delete from public.videos where youtube_id = 'visible0001' $$,
  '42501', null, 'anon cannot delete videos'
);
select throws_ok(
  $$ update public.videos set status = 'approved' where youtube_id = 'draft000001' $$,
  '42501', null, 'anon cannot approve a draft'
);
select throws_ok(
  $$ insert into public.collections (slug, title_ar, cover_bg, cover_fg, cover_glyph, age_bands)
     values ('evil', 'x', '#000000', '#000000', 'x', array['2-3']) $$,
  '42501', null, 'anon cannot insert collections'
);
select throws_ok(
  $$ update public.collections set is_published = true where slug = 'test-hidden' $$,
  '42501', null, 'anon cannot update collections'
);
select throws_ok(
  $$ delete from public.collections where slug = 'test-published' $$,
  '42501', null, 'anon cannot delete collections'
);

select lives_ok(
  $$ insert into public.video_reports (youtube_id, reason) values ('visible0001', 'parent_hidden') $$,
  'anon can insert a report'
);
select lives_ok(
  $$ insert into public.video_reports (youtube_id, reason) values ('visible0001', 'embed_disabled') $$,
  'anon can report embed_disabled'
);
select throws_ok(
  $$ insert into public.video_reports (youtube_id, reason) values ('visible0001', 'i_dont_like_it') $$,
  '23514', null, 'an unknown reason is rejected'
);
select throws_ok(
  $$ insert into public.video_reports (youtube_id, reason) values ('<script>', 'parent_hidden') $$,
  '23514', null, 'a malformed youtube_id is rejected'
);
select throws_ok(
  $$ select * from public.video_reports $$,
  '42501', null, 'anon cannot read reports'
);
select throws_ok(
  $$ update public.video_reports set reason = 'video_removed' $$,
  '42501', null, 'anon cannot update reports'
);
select throws_ok(
  $$ delete from public.video_reports $$,
  '42501', null, 'anon cannot delete reports'
);
select throws_ok(
  $$ select * from public.refresh_runs $$,
  '42501', null, 'anon cannot read refresh runs'
);
select throws_ok(
  $$ select public.apply_youtube_refresh('[]'::jsonb) $$,
  '42501', null, 'anon cannot call apply_youtube_refresh'
);
select throws_ok(
  $$ select public.sweep_stale_youtube_data() $$,
  '42501', null, 'anon cannot call sweep_stale_youtube_data'
);
select throws_ok(
  $$ select internal.run_youtube_refresh() $$,
  '42501', null, 'anon cannot trigger the refresh function'
);

-- Back to the owner: the 30-day sweep -------------------------------------------------------
reset role;

select public.sweep_stale_youtube_data();
select is(
  (select row(yt_duration_sec, yt_thumbnail_url, yt_embeddable, yt_refreshed_at)::text
   from public.videos where youtube_id = 'stale000001'),
  row(null::integer, null::text, null::boolean, null::timestamptz)::text,
  'the sweep clears YouTube data older than 30 days'
);

select * from finish();
rollback;
