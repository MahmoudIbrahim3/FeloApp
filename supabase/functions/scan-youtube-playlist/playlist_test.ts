// deno test supabase/functions/scan-youtube-playlist/playlist_test.ts
import { assertEquals } from "jsr:@std/assert@1";
import { extractPlaylistId, type ScanVideo, toScanRows, uniqueIds } from "./playlist.ts";

const ID = "PLabcdefghijklmnop1234_-XYZ";
const REGIONS = ["EG", "KW"];

Deno.test("extractPlaylistId: playlist links", () => {
  assertEquals(extractPlaylistId(`https://www.youtube.com/playlist?list=${ID}`), ID);
  assertEquals(extractPlaylistId(`https://youtube.com/playlist?list=${ID}&si=abc123`), ID);
  assertEquals(extractPlaylistId(`https://m.youtube.com/playlist?list=${ID}`), ID);
  assertEquals(extractPlaylistId(`https://music.youtube.com/playlist?list=${ID}`), ID);
  assertEquals(extractPlaylistId(`youtube.com/playlist?list=${ID}`), ID);
  assertEquals(extractPlaylistId(`  https://www.youtube.com/playlist?list=${ID}  `), ID);
});

Deno.test("extractPlaylistId: video links that carry a playlist", () => {
  assertEquals(extractPlaylistId(`https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=${ID}&index=3`), ID);
  assertEquals(extractPlaylistId(`https://youtu.be/dQw4w9WgXcQ?list=${ID}`), ID);
  assertEquals(extractPlaylistId(`https://www.youtube-nocookie.com/embed/videoseries?list=${ID}`), ID);
});

Deno.test("extractPlaylistId: a bare id", () => {
  assertEquals(extractPlaylistId(ID), ID);
  assertEquals(extractPlaylistId(`  ${ID}\n`), ID);
});

Deno.test("extractPlaylistId: rejects everything else", () => {
  assertEquals(extractPlaylistId(""), null);
  assertEquals(extractPlaylistId("   "), null);
  assertEquals(extractPlaylistId(null), null);
  assertEquals(extractPlaylistId(undefined), null);
  assertEquals(extractPlaylistId("short"), null);
  assertEquals(extractPlaylistId("has spaces in it"), null);
  assertEquals(extractPlaylistId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"), null); // no list=
  assertEquals(extractPlaylistId(`https://example.com/playlist?list=${ID}`), null); // not YouTube
  assertEquals(extractPlaylistId(`https://notyoutube.com/playlist?list=${ID}`), null);
  assertEquals(extractPlaylistId("https://www.youtube.com/playlist?list=bad id!"), null);
  assertEquals(extractPlaylistId("https://www.youtube.com/@zaghaleel/playlists"), null); // a channel
});

Deno.test("uniqueIds keeps the first-seen order", () => {
  assertEquals(uniqueIds(["b", "a", "b", "c", "a"]), ["b", "a", "c"]);
  assertEquals(uniqueIds([]), []);
});

function video(id: string, overrides: Partial<ScanVideo> = {}): ScanVideo {
  return {
    id,
    snippet: { title: `Title ${id}` },
    contentDetails: { duration: "PT3M5S" },
    status: { uploadStatus: "processed", privacyStatus: "public", embeddable: true, madeForKids: true },
    ...overrides,
  };
}

Deno.test("toScanRows: playlist order, positions from 1, the refresh's rules", () => {
  const videos = new Map<string, ScanVideo>([
    ["aaaaaaaaaaa", video("aaaaaaaaaaa")],
    [
      "bbbbbbbbbbb",
      video("bbbbbbbbbbb", {
        status: { uploadStatus: "processed", privacyStatus: "public", embeddable: true, madeForKids: false },
      }),
    ],
    [
      "ccccccccccc",
      video("ccccccccccc", { contentDetails: { duration: "PT12M", regionRestriction: { blocked: ["kw"] } } }),
    ],
  ]);

  const rows = toScanRows(["aaaaaaaaaaa", "bbbbbbbbbbb", "ccccccccccc"], videos, REGIONS);

  assertEquals(rows.map((r) => r.position), [1, 2, 3]);
  assertEquals(rows[0], {
    position: 1,
    youtube_id: "aaaaaaaaaaa",
    title: "Title aaaaaaaaaaa",
    duration_sec: 185,
    made_for_kids: true,
    embeddable: true,
    available: true,
    reasons: [],
  });
  assertEquals(rows[1].available, false);
  assertEquals(rows[1].made_for_kids, false);
  assertEquals(rows[1].reasons, ["not_made_for_kids"]);
  assertEquals(rows[2].available, false);
  assertEquals(rows[2].duration_sec, 720);
  assertEquals(rows[2].reasons, ["blocked_in:KW"]);
});

Deno.test("toScanRows: a deleted or private entry is not_found", () => {
  const rows = toScanRows(["ddddddddddd"], new Map(), REGIONS);
  assertEquals(rows, [{
    position: 1,
    youtube_id: "ddddddddddd",
    title: null,
    duration_sec: null,
    made_for_kids: null,
    embeddable: null,
    available: false,
    reasons: ["not_found"],
  }]);
});

Deno.test("toScanRows: a video listed twice gets a row per entry", () => {
  const videos = new Map<string, ScanVideo>([["aaaaaaaaaaa", video("aaaaaaaaaaa")]]);
  const rows = toScanRows(["aaaaaaaaaaa", "aaaaaaaaaaa"], videos, REGIONS);
  assertEquals(rows.map((r) => [r.position, r.youtube_id, r.available]), [
    [1, "aaaaaaaaaaa", true],
    [2, "aaaaaaaaaaa", true],
  ]);
});
