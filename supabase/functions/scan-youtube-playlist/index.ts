// scan-youtube-playlist: checks every video of a YouTube playlist BEFORE you curate it, with the
// same rules as refresh-youtube-data (made for kids, embeddable, public or unlisted, processed,
// not age-restricted, playable in TARGET_REGIONS). Only the videos that pass are worth watching.
//
// Read-only: it writes nothing to the database, so no YouTube data is stored here. The answer is
// returned to the caller (pg_net keeps it in net._http_response for a few hours).
//
// Called by internal.scan_youtube_playlist(playlist) through pg_net; read the answer as a table
// with internal.playlist_scan_result(request_id). Body: {"playlist": "<playlist URL or id>"}.
// Auth: header `x-refresh-secret` must equal REFRESH_SECRET (verify_jwt is off, so the app's
// public key can never call it or spend quota).
//
// Quota: playlistItems.list and videos.list cost 1 unit per call of up to 50 videos, so a
// 100-video playlist costs about 4 units. A scan stops after MAX_VIDEOS.
//
// Secrets: YOUTUBE_API_KEY, REFRESH_SECRET, TARGET_REGIONS (optional, default EG,KW,SA,AE).

import { BATCH_SIZE, chunk, parseRegions, safeEqual } from "../refresh-youtube-data/youtube.ts";
import {
  extractPlaylistId,
  PLAYLIST_ITEM_FIELDS,
  SCAN_FIELDS,
  type ScanRow,
  type ScanVideo,
  toScanRows,
  uniqueIds,
} from "./playlist.ts";

const MAX_VIDEOS = 500;
const API_URL = "https://www.googleapis.com/youtube/v3";

interface ScanResult {
  playlist_id: string;
  /** What YouTube reports for the whole playlist (may include deleted or private entries). */
  total_in_playlist: number | null;
  scanned: number;
  available: number;
  /** true when the playlist has more than MAX_VIDEOS entries and the rest was not scanned. */
  truncated: boolean;
  regions: string[];
  api_calls: number;
  quota_units: number;
  videos: ScanRow[];
}

/** An error with the HTTP status this function should answer with. */
class ScanError extends Error {
  constructor(readonly status: number, message: string) {
    super(message);
  }
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

/** One YouTube Data API v3 GET; counts the call (each one used here costs 1 quota unit). */
async function youtube(
  endpoint: "playlistItems" | "videos",
  params: Record<string, string>,
  apiKey: string,
  usage: { calls: number },
): Promise<unknown> {
  const url = new URL(`${API_URL}/${endpoint}`);
  for (const [name, value] of Object.entries(params)) url.searchParams.set(name, value);
  url.searchParams.set("key", apiKey);

  usage.calls += 1;
  const response = await fetch(url);
  if (response.ok) return await response.json();

  const text = await response.text();
  if (endpoint === "playlistItems" && (response.status === 404 || /playlistNotFound/.test(text))) {
    throw new ScanError(404, "playlist not found: it is private, deleted, or the id is wrong");
  }
  if (response.status === 403 && /quotaExceeded|dailyLimitExceeded|rateLimitExceeded/.test(text)) {
    throw new ScanError(429, "YouTube quota exceeded, try again tomorrow");
  }
  throw new ScanError(502, `YouTube ${endpoint}.list ${response.status}: ${text.slice(0, 300)}`);
}

/** The playlist's video ids in playlist order, up to MAX_VIDEOS. */
async function playlistVideoIds(
  playlistId: string,
  apiKey: string,
  usage: { calls: number },
): Promise<{ ids: string[]; total: number | null; truncated: boolean }> {
  const ids: string[] = [];
  let total: number | null = null;
  let pageToken: string | undefined;

  do {
    const params: Record<string, string> = {
      part: "contentDetails",
      playlistId,
      maxResults: String(BATCH_SIZE),
      fields: PLAYLIST_ITEM_FIELDS,
    };
    if (pageToken) params.pageToken = pageToken;

    const page = await youtube("playlistItems", params, apiKey, usage) as {
      nextPageToken?: string;
      pageInfo?: { totalResults?: number };
      items?: { contentDetails?: { videoId?: string } }[];
    };
    total ??= page.pageInfo?.totalResults ?? null;
    for (const item of page.items ?? []) {
      const id = item.contentDetails?.videoId;
      if (id) ids.push(id);
    }
    pageToken = page.nextPageToken;
  } while (pageToken && ids.length < MAX_VIDEOS);

  return { ids: ids.slice(0, MAX_VIDEOS), total, truncated: Boolean(pageToken) || ids.length > MAX_VIDEOS };
}

async function scan(playlistId: string, apiKey: string, regions: string[]): Promise<ScanResult> {
  const usage = { calls: 0 };
  const { ids, total, truncated } = await playlistVideoIds(playlistId, apiKey, usage);

  const videos = new Map<string, ScanVideo>();
  for (const batch of chunk(uniqueIds(ids), BATCH_SIZE)) {
    const body = await youtube("videos", {
      part: "snippet,contentDetails,status",
      id: batch.join(","),
      maxResults: String(BATCH_SIZE),
      fields: SCAN_FIELDS,
    }, apiKey, usage) as { items?: ScanVideo[] };
    for (const item of body.items ?? []) videos.set(item.id, item);
  }

  const rows = toScanRows(ids, videos, regions);
  return {
    playlist_id: playlistId,
    total_in_playlist: total,
    scanned: rows.length,
    available: rows.filter((row) => row.available).length,
    truncated,
    regions,
    api_calls: usage.calls,
    quota_units: usage.calls,
    videos: rows,
  };
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method not allowed" }, 405);

  const expected = Deno.env.get("REFRESH_SECRET");
  const given = req.headers.get("x-refresh-secret") ?? "";
  if (!expected || !safeEqual(given, expected)) return json({ error: "unauthorized" }, 401);

  const apiKey = Deno.env.get("YOUTUBE_API_KEY");
  if (!apiKey) return json({ error: "YOUTUBE_API_KEY secret is not set" }, 500);

  let input: string | null = null;
  try {
    const body = await req.json() as { playlist?: unknown };
    if (typeof body?.playlist === "string") input = body.playlist;
  } catch {
    // handled below: no playlist
  }
  const playlistId = extractPlaylistId(input);
  if (!playlistId) {
    return json({ error: 'send {"playlist": "<YouTube playlist URL or id>"}' }, 400);
  }

  try {
    const result = await scan(playlistId, apiKey, parseRegions(Deno.env.get("TARGET_REGIONS")));
    // One line per scan in the function logs (without the rows).
    const { videos: _videos, ...summary } = result;
    console.log(JSON.stringify({ fn: "scan-youtube-playlist", ...summary }));
    return json(result);
  } catch (e) {
    const status = e instanceof ScanError ? e.status : 500;
    const message = String(e instanceof Error ? e.message : e);
    console.error(JSON.stringify({ fn: "scan-youtube-playlist", playlist_id: playlistId, error: message }));
    return json({ error: message, playlist_id: playlistId }, status);
  }
});
