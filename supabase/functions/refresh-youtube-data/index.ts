// refresh-youtube-data: refreshes the YouTube Data API fields of curated videos.
//
// Picks videos that are `draft`, or `approved` and not refreshed for 7 days (or never), asks the
// YouTube Data API v3 `videos.list` about them in batches of 50 (1 quota unit per call), writes
// duration / best thumbnail / madeForKids / embeddable / refreshed_at, and marks videos that are
// gone, not embeddable, not made for kids, age-restricted or blocked in TARGET_REGIONS as
// `unavailable`. YouTube data older than 30 days is cleared at the end of every run.
//
// Called daily by pg_cron (internal.run_youtube_refresh) and by hand after adding videos.
// Auth: header `x-refresh-secret` must equal the REFRESH_SECRET function secret (verify_jwt is off,
// so the app's public key can never trigger a run or spend quota).
//
// Secrets: YOUTUBE_API_KEY, REFRESH_SECRET, TARGET_REGIONS (optional, default EG,KW,SA,AE).
// Injected by Supabase: SUPABASE_URL, SUPABASE_SECRET_KEYS.

import { createClient, type SupabaseClient } from "npm:@supabase/supabase-js@2.119.0";
import {
  BATCH_SIZE,
  chunk,
  FIELDS,
  parseRegions,
  type RefreshItem,
  safeEqual,
  toRefreshItem,
  type YouTubeVideo,
} from "./youtube.ts";

const REFRESH_AFTER_DAYS = 7;
const PAGE_SIZE = 1000; // PostgREST max_rows
const YOUTUBE_URL = "https://www.googleapis.com/youtube/v3/videos";

interface Summary {
  trigger: string;
  checked: number;
  refreshed: number;
  unavailable: number;
  cleared: number;
  apiCalls: number;
  quotaUnits: number;
  errors: string[];
  unavailableIds: Record<string, string[]>;
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

/** The new secret key (sb_secret_…); the legacy service_role key only as a fallback for old setups. */
function secretKey(): string {
  const keys = Deno.env.get("SUPABASE_SECRET_KEYS");
  if (keys) {
    try {
      const parsed = JSON.parse(keys) as Record<string, string>;
      const key = parsed["default"] ?? Object.values(parsed)[0];
      if (key) return key;
    } catch {
      // fall through
    }
  }
  const legacy = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (legacy) {
    console.warn("refresh-youtube-data: SUPABASE_SECRET_KEYS missing, using the legacy service_role key");
    return legacy;
  }
  throw new Error("No Supabase secret key available");
}

async function dueVideoIds(db: SupabaseClient): Promise<string[]> {
  const cutoff = new Date(Date.now() - REFRESH_AFTER_DAYS * 86_400_000).toISOString();
  const ids: string[] = [];
  for (let from = 0; ; from += PAGE_SIZE) {
    const { data, error } = await db
      .from("videos")
      .select("youtube_id")
      .or(
        // The timestamp is quoted: it contains PostgREST's reserved "." and ":".
        `status.eq.draft,and(status.eq.approved,yt_refreshed_at.is.null),and(status.eq.approved,yt_refreshed_at.lt."${cutoff}")`,
      )
      .order("youtube_id")
      .range(from, from + PAGE_SIZE - 1);
    if (error) throw new Error(`select due videos: ${error.message}`);
    ids.push(...data.map((row) => row.youtube_id as string));
    if (data.length < PAGE_SIZE) return ids;
  }
}

class QuotaExceeded extends Error {}

async function fetchVideos(ids: string[], apiKey: string): Promise<Map<string, YouTubeVideo>> {
  const url = new URL(YOUTUBE_URL);
  url.searchParams.set("part", "contentDetails,status,snippet");
  url.searchParams.set("id", ids.join(","));
  url.searchParams.set("maxResults", String(BATCH_SIZE));
  url.searchParams.set("fields", FIELDS);
  url.searchParams.set("key", apiKey);

  const response = await fetch(url);
  if (!response.ok) {
    const text = await response.text();
    if (response.status === 403 && /quotaExceeded|dailyLimitExceeded|rateLimitExceeded/.test(text)) {
      throw new QuotaExceeded(`YouTube quota exceeded (${response.status})`);
    }
    throw new Error(`YouTube videos.list ${response.status}: ${text.slice(0, 300)}`);
  }
  const body = await response.json() as { items?: YouTubeVideo[] };
  return new Map((body.items ?? []).map((item) => [item.id, item]));
}

async function refresh(db: SupabaseClient, trigger: string): Promise<Summary> {
  const apiKey = Deno.env.get("YOUTUBE_API_KEY");
  if (!apiKey) throw new Error("YOUTUBE_API_KEY secret is not set");
  const regions = parseRegions(Deno.env.get("TARGET_REGIONS"));

  const summary: Summary = {
    trigger,
    checked: 0,
    refreshed: 0,
    unavailable: 0,
    cleared: 0,
    apiCalls: 0,
    quotaUnits: 0,
    errors: [],
    unavailableIds: {},
  };

  const ids = await dueVideoIds(db);
  for (const batch of chunk(ids, BATCH_SIZE)) {
    let videos: Map<string, YouTubeVideo>;
    try {
      summary.apiCalls += 1;
      summary.quotaUnits += 1; // videos.list costs 1 unit per call, whatever the parts.
      videos = await fetchVideos(batch, apiKey);
    } catch (e) {
      summary.errors.push(String(e instanceof Error ? e.message : e));
      if (e instanceof QuotaExceeded) break; // every further call would fail too
      continue; // this batch stays as it was; the 30-day sweep still applies
    }

    const items: RefreshItem[] = batch.map((id) => toRefreshItem(id, videos.get(id), regions));
    for (const item of items) {
      if (!item.available) summary.unavailableIds[item.youtube_id] = item.reasons ?? [];
    }
    const payload = items.map(({ reasons: _reasons, ...row }) => row);

    const { data, error } = await db.rpc("apply_youtube_refresh", { payload });
    if (error) {
      summary.errors.push(`apply_youtube_refresh: ${error.message}`);
      continue;
    }
    summary.checked += batch.length;
    summary.refreshed += (data?.refreshed as number) ?? 0;
    summary.unavailable += (data?.unavailable as number) ?? 0;
  }

  const { data: cleared, error: sweepError } = await db.rpc("sweep_stale_youtube_data");
  if (sweepError) summary.errors.push(`sweep_stale_youtube_data: ${sweepError.message}`);
  else summary.cleared = (cleared as number) ?? 0;

  return summary;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method not allowed" }, 405);

  const expected = Deno.env.get("REFRESH_SECRET");
  const given = req.headers.get("x-refresh-secret") ?? "";
  if (!expected || !safeEqual(given, expected)) return json({ error: "unauthorized" }, 401);

  let trigger = "manual";
  try {
    const body = await req.json() as { trigger?: unknown };
    if (typeof body?.trigger === "string") trigger = body.trigger.slice(0, 32);
  } catch {
    // empty body is fine
  }

  const startedAt = new Date().toISOString();
  const db = createClient(Deno.env.get("SUPABASE_URL") ?? "", secretKey(), {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  let summary: Summary;
  try {
    summary = await refresh(db, trigger);
  } catch (e) {
    const message = String(e instanceof Error ? e.message : e);
    console.error(JSON.stringify({ fn: "refresh-youtube-data", trigger, error: message }));
    await db.from("refresh_runs").insert({ started_at: startedAt, trigger, errors: [message] });
    return json({ error: message }, 500);
  }

  // One line per run in the function logs: quota used and what changed.
  console.log(JSON.stringify({ fn: "refresh-youtube-data", ...summary }));

  const { error: logError } = await db.from("refresh_runs").insert({
    started_at: startedAt,
    trigger: summary.trigger,
    checked: summary.checked,
    refreshed: summary.refreshed,
    unavailable: summary.unavailable,
    cleared: summary.cleared,
    api_calls: summary.apiCalls,
    quota_units: summary.quotaUnits,
    errors: summary.errors,
  });
  if (logError) console.error(`refresh_runs insert failed: ${logError.message}`);

  return json(summary, summary.errors.length > 0 ? 207 : 200);
});
