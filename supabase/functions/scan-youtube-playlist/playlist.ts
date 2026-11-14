// Pure helpers for scan-youtube-playlist: no network, no database. Tested in playlist_test.ts.
// The availability rules come from refresh-youtube-data, so a scan and a refresh always agree.

import { type Thumbnails, toRefreshItem, type YouTubeVideo } from "../refresh-youtube-data/youtube.ts";

/** A `videos.list` item as the scan requests it: same parts as the refresh, plus the title. */
export type ScanVideo = YouTubeVideo & { snippet?: { title?: string; thumbnails?: Thumbnails } };

/** One video of the playlist, in playlist order. */
export interface ScanRow {
  /** 1-based position in the playlist. */
  position: number;
  youtube_id: string;
  /** YouTube's title, only to recognise the video. Never stored; write our own title_ar. */
  title: string | null;
  duration_sec: number | null;
  made_for_kids: boolean | null;
  embeddable: boolean | null;
  /** Same meaning as for refresh-youtube-data: true = it would stay `draft` after a refresh. */
  available: boolean;
  /** Why it is unavailable (`not_made_for_kids`, `not_embeddable`, `blocked_in:KW`, `not_found`, …). */
  reasons: string[];
}

/** The fields the scan asks `videos.list` for: the refresh's FIELDS with the title instead of thumbnails. */
export const SCAN_FIELDS =
  "items(id,snippet(title),contentDetails(duration,regionRestriction,contentRating(ytRating))," +
  "status(uploadStatus,privacyStatus,embeddable,madeForKids))";

/** `playlistItems.list` fields: only the video ids and paging. */
export const PLAYLIST_ITEM_FIELDS = "nextPageToken,pageInfo(totalResults),items(contentDetails(videoId))";

const PLAYLIST_ID = /^[A-Za-z0-9_-]{10,64}$/;
const YOUTUBE_HOSTS = new Set(["youtube.com", "youtu.be", "youtube-nocookie.com"]);

/**
 * The playlist id from what a person pastes: a bare id ("PL…"), or any YouTube link with `list=`
 * ("youtube.com/playlist?list=…", "watch?v=…&list=…", "youtu.be/…?list=…", m. / music. hosts,
 * with or without https://). null for anything else.
 */
export function extractPlaylistId(input: string | null | undefined): string | null {
  const text = (input ?? "").trim();
  if (text.length === 0) return null;
  if (!/[/?=.]/.test(text)) return PLAYLIST_ID.test(text) ? text : null;

  let url: URL;
  try {
    url = new URL(/^[a-z][a-z0-9+.-]*:\/\//i.test(text) ? text : `https://${text}`);
  } catch {
    return null;
  }
  const host = url.hostname.toLowerCase().replace(/^(www|m|music)\./, "");
  if (!YOUTUBE_HOSTS.has(host)) return null;
  const list = url.searchParams.get("list");
  return list && PLAYLIST_ID.test(list) ? list : null;
}

/** Every id once, in first-seen order (a playlist may hold the same video twice). */
export function uniqueIds(ids: string[]): string[] {
  return [...new Set(ids)];
}

/**
 * One row per playlist entry, in playlist order. `videos` holds what `videos.list` returned;
 * an id it didn't return (deleted, private) becomes unavailable with `not_found`.
 */
export function toScanRows(playlistIds: string[], videos: Map<string, ScanVideo>, regions: string[]): ScanRow[] {
  return playlistIds.map((id, index) => {
    const video = videos.get(id);
    const item = toRefreshItem(id, video, regions);
    return {
      position: index + 1,
      youtube_id: id,
      title: video?.snippet?.title ?? null,
      duration_sec: item.duration_sec ?? null,
      made_for_kids: item.made_for_kids ?? null,
      embeddable: item.embeddable ?? null,
      available: item.available,
      reasons: item.reasons ?? [],
    };
  });
}
