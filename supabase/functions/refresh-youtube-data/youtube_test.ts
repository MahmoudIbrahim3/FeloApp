// deno test supabase/functions/refresh-youtube-data/youtube_test.ts
import { assertEquals } from "jsr:@std/assert@1";
import {
  bestThumbnail,
  chunk,
  DEFAULT_TARGET_REGIONS,
  parseDuration,
  parseRegions,
  safeEqual,
  toRefreshItem,
  type YouTubeVideo,
} from "./youtube.ts";

const REGIONS = ["EG", "KW"];

function video(overrides: Partial<YouTubeVideo> = {}): YouTubeVideo {
  return {
    id: "abcdefghijk",
    snippet: {
      thumbnails: {
        default: { url: "https://i.ytimg.com/vi/abcdefghijk/default.jpg" },
        high: { url: "https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg" },
      },
    },
    contentDetails: { duration: "PT3M5S" },
    status: { uploadStatus: "processed", privacyStatus: "public", embeddable: true, madeForKids: true },
    ...overrides,
  };
}

Deno.test("parseDuration", () => {
  assertEquals(parseDuration("PT3M5S"), 185);
  assertEquals(parseDuration("PT1H2M3S"), 3723);
  assertEquals(parseDuration("PT45S"), 45);
  assertEquals(parseDuration("PT10M"), 600);
  assertEquals(parseDuration("P1DT2H"), 93600);
  assertEquals(parseDuration("P0D"), 0);
  assertEquals(parseDuration("PT1.5S"), 1);
  assertEquals(parseDuration(undefined), null);
  assertEquals(parseDuration("P"), null);
  assertEquals(parseDuration("PT"), null);
  assertEquals(parseDuration("3 minutes"), null);
});

Deno.test("bestThumbnail picks maxres > standard > high > medium > default", () => {
  assertEquals(
    bestThumbnail({ default: { url: "d" }, medium: { url: "m" }, high: { url: "h" }, standard: { url: "s" }, maxres: { url: "x" } }),
    "x",
  );
  assertEquals(bestThumbnail({ default: { url: "d" }, high: { url: "h" } }), "h");
  assertEquals(bestThumbnail({ default: { url: "d" } }), "d");
  assertEquals(bestThumbnail({}), null);
  assertEquals(bestThumbnail(undefined), null);
});

Deno.test("parseRegions", () => {
  assertEquals(parseRegions("eg, kw ,SA"), ["EG", "KW", "SA"]);
  assertEquals(parseRegions(""), DEFAULT_TARGET_REGIONS);
  assertEquals(parseRegions(undefined), DEFAULT_TARGET_REGIONS);
  assertEquals(parseRegions("Egypt"), DEFAULT_TARGET_REGIONS);
});

Deno.test("a playable kids video is available with its data", () => {
  assertEquals(toRefreshItem("abcdefghijk", video(), REGIONS), {
    youtube_id: "abcdefghijk",
    found: true,
    available: true,
    duration_sec: 185,
    thumbnail_url: "https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg",
    made_for_kids: true,
    embeddable: true,
    reasons: [],
  });
});

Deno.test("not returned by the API = not found", () => {
  assertEquals(toRefreshItem("gone0000001", undefined, REGIONS), {
    youtube_id: "gone0000001",
    found: false,
    available: false,
    reasons: ["not_found"],
  });
});

Deno.test("each rule makes a video unavailable", () => {
  const cases: Array<[string, YouTubeVideo, string]> = [
    ["embed", video({ status: { uploadStatus: "processed", privacyStatus: "public", embeddable: false, madeForKids: true } }), "not_embeddable"],
    ["kids", video({ status: { uploadStatus: "processed", privacyStatus: "public", embeddable: true, madeForKids: false } }), "not_made_for_kids"],
    ["private", video({ status: { uploadStatus: "processed", privacyStatus: "private", embeddable: true, madeForKids: true } }), "not_public"],
    ["upload", video({ status: { uploadStatus: "rejected", privacyStatus: "public", embeddable: true, madeForKids: true } }), "not_processed"],
    ["age", video({ contentDetails: { duration: "PT1M", contentRating: { ytRating: "ytAgeRestricted" } } }), "age_restricted"],
    ["blocked", video({ contentDetails: { duration: "PT1M", regionRestriction: { blocked: ["kw", "US"] } } }), "blocked_in:KW"],
    ["allowed", video({ contentDetails: { duration: "PT1M", regionRestriction: { allowed: ["EG"] } } }), "not_allowed_in:KW"],
  ];
  for (const [name, v, reason] of cases) {
    const item = toRefreshItem("abcdefghijk", v, REGIONS);
    assertEquals(item.found, true, name);
    assertEquals(item.available, false, name);
    assertEquals(item.reasons, [reason], name);
  }
});

Deno.test("unlisted videos and unrelated region blocks stay available", () => {
  const unlisted = video({ status: { uploadStatus: "processed", privacyStatus: "unlisted", embeddable: true, madeForKids: true } });
  assertEquals(toRefreshItem("abcdefghijk", unlisted, REGIONS).available, true);
  const blockedElsewhere = video({ contentDetails: { duration: "PT1M", regionRestriction: { blocked: ["US"] } } });
  assertEquals(toRefreshItem("abcdefghijk", blockedElsewhere, REGIONS).available, true);
});

Deno.test("chunk", () => {
  assertEquals(chunk([1, 2, 3, 4, 5], 2), [[1, 2], [3, 4], [5]]);
  assertEquals(chunk([], 50), []);
  assertEquals(chunk(Array.from({ length: 120 }, (_, i) => i), 50).map((c) => c.length), [50, 50, 20]);
});

Deno.test("safeEqual", () => {
  assertEquals(safeEqual("secret", "secret"), true);
  assertEquals(safeEqual("secret", "secreT"), false);
  assertEquals(safeEqual("secret", "secret2"), false);
  assertEquals(safeEqual("", "secret"), false);
});
