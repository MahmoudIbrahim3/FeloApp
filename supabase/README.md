# ClipKid backend (Supabase)

Everything the backend needs lives here and is versioned with the app.

```
supabase/
  config.toml                         local CLI config (API exposes only `public`; function JWT check off)
  migrations/                         schema, RLS, refresh SQL, pg_cron schedule — applied in order
  seed.sql                            the 6 launch collections
  tests/database/*.test.sql           pgTAP: RLS, the refresh SQL and the playlist scan SQL
  functions/refresh-youtube-data/     Edge Function (Deno/TypeScript) + helper tests
  functions/scan-youtube-playlist/    Edge Function: checks a playlist before you curate it (read-only)
  functions/.env.example              secrets for running the function locally
```

## What is where

| Table | Who writes | Who reads |
|---|---|---|
| `collections` | you (Dashboard) | app: only `is_published` |
| `videos` | you (curated fields) + the function (`yt_*`, `status` → `unavailable`) | app: only `approved` + `yt_embeddable` + refreshed within 30 days + collection published |
| `video_reports` | app (insert only: `youtube_id`, `reason`) | you (Dashboard) |
| `refresh_runs` | the function (one row per run) | you (Dashboard) |

The app uses the **publishable key** (`sb_publishable_…`, role `anon`). It can read the visible
catalog and insert reports — nothing else (see `migrations/20261106000002_rls.sql`).

YouTube data (`yt_*`) is only ever written by `refresh-youtube-data` and is **cleared after 30 days
without a refresh** (YouTube API policy). Approved videos are re-checked every 7 days; a daily job
runs the function at 03:17 UTC and a SQL-only sweep at 04:47 UTC.

---

## One-time setup (hosted project)

You need: the [Supabase CLI](https://supabase.com/docs/guides/local-development/cli/getting-started),
a Supabase project, and a **YouTube Data API v3 key** (Google Cloud Console → APIs & Services →
enable "YouTube Data API v3" → Credentials → API key → restrict it to YouTube Data API v3).

1. **Link and migrate** (from the repo root):
   ```sh
   supabase login
   supabase link --project-ref <project-ref>
   supabase db push --remote
   ```
   (Newer CLI versions push to a local stack by default; `--remote` targets the linked project.)
2. **Seed the collections**: Dashboard → SQL Editor → paste `supabase/seed.sql` → Run.
   (Safe to run again; existing slugs are skipped.)
3. **Function secrets** (pick a long random value for `REFRESH_SECRET`, e.g. `openssl rand -hex 32`):
   ```sh
   supabase secrets set YOUTUBE_API_KEY=<your-key> REFRESH_SECRET=<random> TARGET_REGIONS=EG,SA,AE,KW,QA,BH,OM,JO,LB,SY,IQ,PS,YE,SD,LY,TN,DZ,MA,MR,SO,DJ,KM
   ```
   `TARGET_REGIONS` = countries where a video must be playable; blocked in any of them → `unavailable`.
   The list above is all 22 Arab League countries (also the default when the secret is unset).
4. **Deploy the functions**:
   ```sh
   supabase functions deploy refresh-youtube-data --no-verify-jwt
   supabase functions deploy scan-youtube-playlist --no-verify-jwt
   ```
   (`--no-verify-jwt` matches `config.toml`: both functions check `x-refresh-secret` themselves, so
   the app's public key can never trigger them. They share the secrets from step 3.)
5. **Vault secrets for the schedule**: SQL Editor → Run (same `REFRESH_SECRET` as step 3):
   ```sql
   select vault.create_secret('https://<project-ref>.supabase.co', 'project_url');
   select vault.create_secret('<same value as REFRESH_SECRET>', 'refresh_secret');
   ```
   To change one later: `select vault.update_secret((select id from vault.secrets where name = 'refresh_secret'), '<new value>');`
6. **App config**: Dashboard → Project Settings → API Keys → copy the **publishable** key. In the
   app repo's `local.properties` (gitignored, never commit it):
   ```properties
   supabase.url=https://<project-ref>.supabase.co
   supabase.publishableKey=sb_publishable_...
   ```
   CI can use env vars `CLIPKID_SUPABASE_URL` and `CLIPKID_SUPABASE_PUBLISHABLE_KEY` instead.
   Without them the app builds and uses the fake catalog (it logs a warning).

---

## Scanning a playlist (before you curate)

Most family channels are not set as "made for kids", and the refresh marks those videos
`unavailable`. Scan a playlist first and only watch the videos that pass. The scan uses exactly the
same rules as the refresh and writes nothing to the database.

1. SQL Editor → Run:
   ```sql
   select internal.scan_youtube_playlist('https://www.youtube.com/playlist?list=PL...');
   ```
   Any YouTube link with `list=` works (a playlist page, or a video opened from a playlist), or the
   bare id. It returns a request id, e.g. `42`.
2. A few seconds later:
   ```sql
   select * from internal.playlist_scan_result(42);
   ```
   One row per video, in playlist order:

   | Column | Meaning |
   |---|---|
   | `position` | order in the playlist (use it for `sort_order`, e.g. × 10) |
   | `youtube_id` | the id to insert into `videos` |
   | `title` | YouTube's title, only to recognise the video; write your own `title_ar` |
   | `duration_sec`, `made_for_kids`, `embeddable` | from the YouTube Data API |
   | `available` | `true` = it would stay `draft` after a refresh, so it's worth reviewing |
   | `reasons` | why not: `not_made_for_kids`, `not_embeddable`, `blocked_in:KW`, `age_restricted`, `not_found`, … |
   | `in_catalog` | its `status` if it's already in `videos`, empty if not |

   Only the available ones: `select * from internal.playlist_scan_result(42) where available;`

- "No answer yet" → wait a few seconds and run step 2 again. pg_net keeps answers for a few hours.
- "playlist not found" → the playlist is private, deleted, or the link is wrong.
- A scan stops after 500 videos (a notice says so).
- Errors and one summary line per scan: Dashboard → Edge Functions → `scan-youtube-playlist` → Logs.

## Adding a video

1. Find the video on YouTube and copy its id (the 11 characters after `watch?v=`, e.g. `dQw4w9WgXcQ`).
2. Dashboard → **Table Editor** → `videos` → **Insert row**:

   | Column | Value |
   |---|---|
   | `youtube_id` | the 11-character id |
   | `collection_id` | pick the collection (the picker shows `collections`) |
   | `title_ar` | **our** Arabic title, Egyptian colloquial — not YouTube's title |
   | `tags` | e.g. `{الحيوانات,أغاني}` — Arabic labels shared across collections (Up Next uses them) |
   | `age_bands` | any of `{3-5,6-8,9-12}`, e.g. `{3-5,6-8}` |
   | `sort_order` | position in the collection (1, 2, 3 …) |
   | `status` | `draft` (the default) |

   Leave every `yt_*` column empty, and `updated_at` as is.
3. **Run the refresh**: SQL Editor →
   ```sql
   select internal.run_youtube_refresh();
   ```
   It returns a request id and runs in the background (a few seconds for a handful of videos).
4. **Check the result**: Table Editor → `videos` → your row now has `yt_duration_sec`,
   `yt_thumbnail_url`, `yt_made_for_kids`, `yt_embeddable`, `yt_refreshed_at`.
   - `status` is still `draft` → YouTube says it's playable for kids in your regions.
   - `status` became `unavailable` → see why in Dashboard → Edge Functions →
     `refresh-youtube-data` → Logs (`unavailableIds` lists the reasons: `not_embeddable`,
     `not_made_for_kids`, `blocked_in:KW`, `not_found`, …). Pick another video.
   - Run details: `select * from public.refresh_runs order by finished_at desc limit 5;`
5. **Review** the video yourself (watch it, check the title, tags and ages).
6. **Approve**: set `status` = `approved`. Its collection must have `is_published` = true.
7. **Reopen the app**: it syncs on every start and the video shows up (only changed rows are downloaded).

### Other changes
- **Edit a title, tags, ages or order**: just edit the row. `updated_at` changes and the app picks it up on its next start.
- **Hide a video from everyone**: set `status` = `draft` (or delete the row). The app removes it on its next start.
- **Re-check an `unavailable` video** (e.g. the owner turned embedding back on): set `status` = `draft`, run the refresh again.
- **New collection**: insert into `collections` (`slug`, `title_ar`, `cover_bg`/`cover_fg` as `#RRGGBB`,
  exactly one of `cover_glyph` (e.g. `أ ب`) or `cover_icon` (`book`, `heart`, `leaf`, `moon`, `star`),
  `age_bands`, `sort_order`), set `is_published` when it has approved videos.
- **Reports from parents and the player**: `select youtube_id, reason, count(*) from public.video_reports group by 1, 2 order by 3 desc;`
  (`parent_hidden`, `video_removed`, `embed_disabled` — no device, user or child data is ever sent).

### Quota
`videos.list` costs **1 unit per call** of up to 50 videos. The default daily quota is 10,000 units,
so even a daily refresh of thousands of videos uses well under 1 %. Every run logs
`apiCalls` / `quotaUnits` (function logs and `refresh_runs`). If the quota runs out the run stops
early; videos not refreshed for 30 days are cleared by the sweep and hidden from the app until the
next successful run.

A playlist scan costs 1 unit per 50 videos for `playlistItems.list` plus 1 per 50 for `videos.list`
(about 4 units for 100 videos). It doesn't touch `refresh_runs`; its usage is in the function logs.

---

## Local development

Needs Docker and the Supabase CLI. From the repo root:

```sh
supabase start                 # local stack (prints the local API URL and keys)
supabase db reset              # fresh DB from zero: all migrations + seed.sql
supabase test db               # pgTAP: RLS (anon can't read drafts, can't write videos, …) and refresh SQL
deno test supabase/functions/refresh-youtube-data/youtube_test.ts   # duration, thumbnail, availability rules
deno test supabase/functions/scan-youtube-playlist/playlist_test.ts # playlist links, scan rows
```

Run the function locally:

```sh
cp supabase/functions/.env.example supabase/functions/.env   # fill in YOUTUBE_API_KEY and REFRESH_SECRET
supabase functions serve refresh-youtube-data --env-file supabase/functions/.env

# in another terminal
curl -X POST http://127.0.0.1:54321/functions/v1/refresh-youtube-data \
  -H "x-refresh-secret: <REFRESH_SECRET>" -H "Content-Type: application/json" -d '{"trigger":"manual"}'
```

To let the local pg_cron call the local function, create the Vault secrets in the local Studio
(http://127.0.0.1:54323 → SQL Editor) with `project_url` = `http://host.docker.internal:54321`.

Point a debug build at the local stack with `supabase.url=http://10.0.2.2:54321` (Android emulator)
or `http://127.0.0.1:54321` (iOS simulator) and the local publishable key from `supabase status`.

## Changing the schema
Add a new file in `migrations/` (`supabase migration new <name>`), never edit an applied one. If the
app's mapping changes, bump `CATALOG_SYNC_VERSION` in `OfflineFirstCatalogRepository` so every app
does a full resync once.
