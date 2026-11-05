-- apply_youtube_refresh(): what one batch from the YouTube API does to the rows.
begin;
create extension if not exists pgtap with schema extensions;
set search_path = public, extensions;

select plan(7);

insert into public.collections (id, slug, title_ar, cover_bg, cover_fg, cover_glyph, age_bands, is_published)
values ('00000000-0000-0000-0000-0000000000a1', 'test-refresh', 'تجربة', '#FFFFFF', '#000000', 'أ', array['2-3'], true);

insert into public.videos (youtube_id, collection_id, title_ar, age_bands, status, yt_duration_sec, yt_refreshed_at)
values
  ('okdraft0001', '00000000-0000-0000-0000-0000000000a1', 'مسودة', array['2-3'], 'draft', null, null),
  ('okapprov001', '00000000-0000-0000-0000-0000000000a1', 'موافق', array['2-3'], 'approved', 10, now() - interval '8 days'),
  ('noembed0001', '00000000-0000-0000-0000-0000000000a1', 'مش embed', array['2-3'], 'approved', 10, now() - interval '8 days'),
  ('gone0000001', '00000000-0000-0000-0000-0000000000a1', 'اتمسح', array['2-3'], 'approved', 10, now() - interval '8 days');

select is(
  public.apply_youtube_refresh($$[
    {"youtube_id":"okdraft0001","found":true,"available":true,"duration_sec":125,"thumbnail_url":"https://i.ytimg.com/vi/okdraft0001/maxresdefault.jpg","made_for_kids":true,"embeddable":true},
    {"youtube_id":"okapprov001","found":true,"available":true,"duration_sec":200,"thumbnail_url":"https://i.ytimg.com/vi/okapprov001/hqdefault.jpg","made_for_kids":true,"embeddable":true},
    {"youtube_id":"noembed0001","found":true,"available":false,"duration_sec":90,"thumbnail_url":"https://x","made_for_kids":true,"embeddable":false},
    {"youtube_id":"gone0000001","found":false,"available":false}
  ]$$::jsonb),
  '{"missing": 1, "refreshed": 3, "unavailable": 2}'::jsonb,
  'returns the counts'
);

select is((select status::text from public.videos where youtube_id = 'okdraft0001'), 'draft', 'a refreshed draft stays a draft');
select is((select yt_duration_sec from public.videos where youtube_id = 'okdraft0001'), 125, 'the duration is written');
select ok((select yt_refreshed_at > now() - interval '1 minute' from public.videos where youtube_id = 'okapprov001'), 'yt_refreshed_at is set');
select is((select status::text from public.videos where youtube_id = 'okapprov001'), 'approved', 'an available approved video stays approved');
select is((select status::text from public.videos where youtube_id = 'noembed0001'), 'unavailable', 'not embeddable becomes unavailable');
select is(
  (select row(status::text, yt_duration_sec, yt_refreshed_at)::text from public.videos where youtube_id = 'gone0000001'),
  row('unavailable', null::integer, null::timestamptz)::text,
  'a video YouTube no longer returns becomes unavailable and loses its YouTube data'
);

select * from finish();
rollback;
