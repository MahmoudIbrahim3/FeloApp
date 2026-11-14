-- internal.parse_playlist_scan / playlist_scan_result: turning the scan function's answer into rows.
begin;
create extension if not exists pgtap with schema extensions;
set search_path = public, extensions;

select plan(10);

insert into public.collections (id, slug, title_ar, cover_bg, cover_fg, cover_glyph, age_bands, is_published)
values ('00000000-0000-0000-0000-0000000000b1', 'test-scan', 'تجربة', '#FFFFFF', '#000000', 'أ', array['3-5'], true);

insert into public.videos (youtube_id, collection_id, title_ar, age_bands, status)
values ('ccccccccccc', '00000000-0000-0000-0000-0000000000b1', 'موجود', array['3-5'], 'draft');

-- A 200 answer with three entries, as scan-youtube-playlist returns it (entries and keys out of order on purpose).
-- n = the order the function returned the rows in.
create temp table scan as
select s.*, row_number() over ()::integer as n from internal.parse_playlist_scan(200, $json$
{
  "playlist_id": "PLabcdefghijklmnop",
  "total_in_playlist": 3,
  "scanned": 3,
  "available": 1,
  "truncated": false,
  "regions": ["EG", "KW"],
  "api_calls": 2,
  "quota_units": 2,
  "videos": [
    {"position": 2, "youtube_id": "bbbbbbbbbbb", "title": "B", "duration_sec": 720,
     "made_for_kids": false, "embeddable": true, "available": false,
     "reasons": ["not_made_for_kids", "blocked_in:KW"]},
    {"youtube_id": "aaaaaaaaaaa", "position": 1, "title": "A", "duration_sec": 185,
     "made_for_kids": true, "embeddable": true, "available": true, "reasons": []},
    {"position": 3, "youtube_id": "ccccccccccc", "title": null, "duration_sec": null,
     "made_for_kids": null, "embeddable": null, "available": false, "reasons": ["not_found"]}
  ]
}
$json$) as s;

select is((select count(*)::integer from scan), 3, 'one row per playlist entry');
select is(
  (select array_agg("position" order by n) from scan),
  array[1, 2, 3],
  'rows come in playlist order'
);
select is(
  (select row("position", youtube_id, title, duration_sec, made_for_kids, embeddable, available, reasons, in_catalog)::text
   from scan where "position" = 1),
  row(1, 'aaaaaaaaaaa', 'A', 185, true, true, true, null::text, null::text)::text,
  'an available video has no reasons and is not in the catalog yet'
);
select is(
  (select reasons from scan where "position" = 2),
  'not_made_for_kids, blocked_in:KW',
  'reasons are joined in the order the function gave them'
);
select is(
  (select in_catalog from scan where "position" = 3),
  'draft',
  'in_catalog shows the status of a video already in public.videos'
);
select is(
  (select count(*)::integer from scan where in_catalog is null),
  2,
  'videos not in public.videos have no in_catalog'
);
select is(
  (select count(*)::integer from internal.parse_playlist_scan(200, '{"videos": []}')),
  0,
  'an empty playlist gives no rows'
);

select throws_ok(
  $$ select * from internal.parse_playlist_scan(404, '{"error": "playlist not found"}') $$,
  'P0001',
  'scan-youtube-playlist answered 404: playlist not found',
  'an error answer raises the function''s message'
);
select throws_ok(
  $$ select * from internal.parse_playlist_scan(502, 'Bad Gateway') $$,
  'P0001',
  'scan-youtube-playlist answered 502 with a non-JSON body: Bad Gateway',
  'a non-JSON answer raises with its status and body'
);
select throws_ok(
  $$ select * from internal.playlist_scan_result(-1) $$,
  'P0001',
  null,
  'an unknown request id asks to wait and try again'
);

select * from finish();
rollback;
