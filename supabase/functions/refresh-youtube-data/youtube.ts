// Pure helpers for refresh-youtube-data: no network, no database. Tested in youtube_test.ts.

export type Thumbnails = Record<string, { url?: string; width?: number; height?: number } | undefined>;

/** The parts of a YouTube Data API v3 `videos.list` item we request (see FIELDS). */
export interface YouTubeVideo {
  id: string;
  snippet?: { thumbnails?: Thumbnails };
  contentDetails?: {
    duration?: string;
    regionRestriction?: { allowed?: string[]; blocked?: string[] };
    contentRating?: { ytRating?: string };
  };
  status?: {
    uploadStatus?: string;
    privacyStatus?: string;
    embeddable?: boolean;
    madeForKids?: boolean;
  };
}

/** One row for public.apply_youtube_refresh(payload). */
export interface RefreshItem {
  youtube_id: string;
  found: boolean;
  available: boolean;
  duration_sec?: number | null;
  thumbnail_url?: string | null;
  made_for_kids?: boolean | null;
  embeddable?: boolean | null;
  /** Why it is unavailable (logged only, not stored). */
  reasons?: string[];
}

export const BATCH_SIZE = 50;
export const DEFAULT_TARGET_REGIONS = ["EG", "KW", "SA", "AE"];

/** Only what we store or decide on: keeps responses small. Quota is 1 unit per call either way. */
export const FIELDS =
  "items(id,snippet(thumbnails),contentDetails(duration,regionRestriction,contentRating(ytRating))," +
  "status(uploadStatus,privacyStatus,embeddable,madeForKids))";

/** ISO 8601 duration ("PT1H2M3S", "P1DT2H", "PT45S", "P0D") to whole seconds; null if unreadable. */
export function parseDuration(iso: string | undefined): number | null {
  if (!iso) return null;
  const match = /^P(?:(\d+)W)?(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?)?$/.exec(iso);
  if (!match || iso === "P" || iso === "PT") return null;
  const [, w, d, h, m, s] = match;
  const seconds = Number(w ?? 0) * 604800 + Number(d ?? 0) * 86400 + Number(h ?? 0) * 3600 +
    Number(m ?? 0) * 60 + Number(s ?? 0);
  return Number.isFinite(seconds) ? Math.floor(seconds) : null;
}

const THUMBNAIL_ORDER = ["maxres", "standard", "high", "medium", "default"];

/** The best available thumbnail: maxres > standard > high > medium > default. */
export function bestThumbnail(thumbnails: Thumbnails | undefined): string | null {
  if (!thumbnails) return null;
  for (const key of THUMBNAIL_ORDER) {
    const url = thumbnails[key]?.url;
    if (url) return url;
  }
  return null;
}

/** "eg, kw ,SA" → ["EG","KW","SA"]. Empty or unset → DEFAULT_TARGET_REGIONS. */
export function parseRegions(value: string | undefined): string[] {
  const regions = (value ?? "")
    .split(",")
    .map((r) => r.trim().toUpperCase())
    .filter((r) => /^[A-Z]{2}$/.test(r));
  return regions.length > 0 ? regions : DEFAULT_TARGET_REGIONS;
}

/**
 * Turns one requested id and the API's answer into a refresh row.
 * Unavailable when: not returned (removed / private), not embeddable, not made for kids,
 * not public or unlisted, upload not processed, age-restricted, or blocked in any target region.
 */
export function toRefreshItem(youtubeId: string, video: YouTubeVideo | undefined, regions: string[]): RefreshItem {
  if (!video) return { youtube_id: youtubeId, found: false, available: false, reasons: ["not_found"] };

  const reasons: string[] = [];
  const status = video.status ?? {};
  const details = video.contentDetails ?? {};

  if (status.embeddable !== true) reasons.push("not_embeddable");
  if (status.madeForKids !== true) reasons.push("not_made_for_kids");
  if (status.privacyStatus !== "public" && status.privacyStatus !== "unlisted") reasons.push("not_public");
  if (status.uploadStatus !== "processed") reasons.push("not_processed");
  if (details.contentRating?.ytRating === "ytAgeRestricted") reasons.push("age_restricted");

  const restriction = details.regionRestriction;
  if (restriction?.allowed) {
    const allowed = new Set(restriction.allowed.map((r) => r.toUpperCase()));
    const missing = regions.filter((r) => !allowed.has(r));
    if (missing.length > 0) reasons.push(`not_allowed_in:${missing.join(",")}`);
  }
  if (restriction?.blocked) {
    const blocked = new Set(restriction.blocked.map((r) => r.toUpperCase()));
    const hit = regions.filter((r) => blocked.has(r));
    if (hit.length > 0) reasons.push(`blocked_in:${hit.join(",")}`);
  }

  return {
    youtube_id: youtubeId,
    found: true,
    available: reasons.length === 0,
    duration_sec: parseDuration(details.duration),
    thumbnail_url: bestThumbnail(video.snippet?.thumbnails),
    made_for_kids: status.madeForKids ?? null,
    embeddable: status.embeddable ?? null,
    reasons,
  };
}

export function chunk<T>(items: T[], size: number): T[][] {
  const out: T[][] = [];
  for (let i = 0; i < items.length; i += size) out.push(items.slice(i, i + size));
  return out;
}

/** Constant-time string comparison for the shared secret. */
export function safeEqual(a: string, b: string): boolean {
  const enc = new TextEncoder();
  const x = enc.encode(a);
  const y = enc.encode(b);
  let diff = x.length ^ y.length;
  for (let i = 0; i < Math.max(x.length, y.length); i++) diff |= (x[i] ?? 0) ^ (y[i] ?? 0);
  return diff === 0;
}
