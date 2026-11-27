# Feelo — Project Guide

## Product
Feelo is a curated video app for children aged 3–12, Arabic-first (Egyptian colloquial UI copy, RTL), on Android and iOS.
- Every video is hand-picked and reviewed by the team. Videos play through the official YouTube embedded player (IFrame API). We never host, download or cache video.
- Parents set a daily limit (N videos or N minutes). No infinite feed, no autoplay, no countdowns.
- One app, two modes: Child mode (default) and Parent mode behind a PIN gate.
- v1 has no accounts and no login. All settings and history live on the device.
- Revenue: subscription via RevenueCat (entitlement `full_access`, offering `default` with `$rc_monthly` + `$rc_annual`). **Every video and collection is free for everyone** (YouTube Developer Policies III.F.3.a: never charge to watch embedded YouTube content). The subscription sells parent features only: free = 1 child profile and today's activity; full = up to 4 child profiles and the 7-day activity report. No ads, ever.

## Non-negotiable rules (YouTube, Google Families, Apple Kids)
1. Never download, cache, store or play YouTube audio/video outside the official embedded player. No background playback, no offline playback, no audio-only.
2. Never modify, hide, cover or block the player, its controls, YouTube branding, links or ads. Player viewport is never smaller than 200x200.
3. Always send an HTTP Referer: load the player HTML with a base URL (Android `loadDataWithBaseURL`, iOS `loadHTMLString(_:baseURL:)`) set to `https://com.mahmoud.feelo` (`PlayerConfig.BASE_URL`). YouTube's Required Minimum Functionality asks native apps to identify as `https://<app ID>`, stable across versions. Missing Referer = error 153.
4. Player params: `host` = `https://www.youtube-nocookie.com` (privacy-enhanced mode), `rel=0`, `playsinline=1`, `enablejsapi=1`, `origin` = the base URL. Never autoplay the next video.
5. YouTube API data (title, thumbnail URL, duration, madeForKids, embeddable) is fetched by the backend only, stored max 30 days and refreshed by a scheduled job. The app caches images only through Coil/HTTP cache.
6. Wherever a YouTube thumbnail or player is shown, show YouTube attribution using official brand assets.
7. The YouTube Data API key never ships in the app. It lives only in Supabase Edge Function secrets. The app only has the Supabase URL and **publishable** key, from `local.properties` / env at build time (BuildKonfig), never committed.
8. The app is child-directed: no third-party analytics, ads, attribution or tracking SDKs, no personalized anything, no YouTube write actions. Ask before adding ANY SDK that sends data off-device.
9. Every external link, purchase, settings screen and “Watch on YouTube” escape route sits behind the parental gate.
10. PIN is stored hashed (salt + SHA-256 at minimum) in secure storage (Android Keystore-backed / iOS Keychain).

## Tech stack
- Kotlin Multiplatform + Compose Multiplatform. Targets: Android through the `android {}` target of `com.android.kotlin.multiplatform.library` (AGP 9 setup already in the project; minSdk 28, compile/target SDK from `libs.versions.toml`), `iosArm64`, `iosSimulatorArm64`.
- Shared UI and logic in commonMain. Platform code only through expect/actual or interfaces injected by Koin.
- Koin (DI) · androidx lifecycle-viewmodel (multiplatform) · Navigation Compose (multiplatform) · Room KMP with bundled SQLite driver · DataStore Preferences (KMP) · Ktor client (OkHttp / Darwin) · kotlinx.serialization · kotlinx.datetime · Coil 3 · supabase-kt (postgrest + functions) · RevenueCat purchases-kmp · Compose Multiplatform resources for strings, fonts and images.
- Use the latest STABLE version of every library. Check the current version before adding it and keep all versions in `gradle/libs.versions.toml`.
- Tests: kotlin.test + Turbine + hand-written fakes in commonTest.

## Modules
- `shared` — app shell: `App()` composable, root navigation, Koin start, iOS framework (`Shared`). Depends on the feature modules.
- `androidApp` — Android application module (MainActivity) that hosts `App()` from `shared`.
- `iosApp` — thin Xcode/SwiftUI host.
- `core:model` — pure Kotlin data classes.
- `core:domain` — repository interfaces + use cases (quota, up-next, filtering).
- `core:data` — Room, DataStore, Supabase client, repository implementations, fake repositories.
- `core:designsystem` — tokens, typography, components (VideoCard, CollectionCover, Shelf, PinPad, QuotaDots…).
- `core:player` — expect/actual YouTube player.
- `core:billing` — RevenueCat wrapper behind an interface.
- `feature:onboarding`, `feature:child`, `feature:parent`.
- `/supabase` — the backend, versioned with the app: SQL migrations (schema, RLS, pg_cron schedule), pgTAP tests, the `refresh-youtube-data` Edge Function, and `README.md` (setup and how to add a video).
Rules: features depend only on core modules, never on each other. Navigation lives in `shared`.

## Architecture
- MVVM + unidirectional data flow. Each screen: `XxxViewModel` exposes `StateFlow<XxxUiState>`, receives `XxxEvent` (sealed interface), emits one-off effects through a Channel.
- Business rules live in use cases, not ViewModels or composables.
- Catalog (collections + videos) is cached in Room and synced with Supabase on every app start: only changed rows are downloaded (manifest of `youtube_id` + `updated_at`), and videos the backend no longer lists are deleted. Only a first launch with an empty cache waits for it. Cached YouTube data older than 30 days is never shown. The app works from cache when offline (lists only; playback needs network, and the player shows an offline state).

## Domain model
- `AgeBand`: A3_5, A6_8, A9_12 (Google Play Families groups; wire values "3-5", "6-8", "9-12")
- `Collection(id, slug, titleAr, cover: CoverStyle(bg, fg, glyph | icon), ageBands, sortOrder)`
- `Video(youtubeId, collectionId, titleAr, durationSec, thumbnailUrl, tags, ageBands, sortOrder, madeForKids, embeddable, refreshedAt)`
- `ChildProfile(id, nickname, avatarColor, ageBand, allowedCollectionIds, dailyLimit: Videos(n) | Minutes(m))`
- `WatchEvent(id, childId, youtubeId, startedAt, watchedSec, completed)`
- `Favorite(childId, youtubeId)` · `HiddenVideo(childId, youtubeId)`

## Product rules
- A video counts toward the daily limit when playback starts. Replaying the same video from the Up Next screen the same day does not count again. Videos opened from Favorites count.
- The day resets at local midnight.
- When the limit is reached, show DoneToday instead of Up Next. A parent can grant +1 / +3 videos for today through the gate.
- Up Next = 1 primary choice (next video in the same collection) + up to 2 by shared tag. Exclude videos watched today and hidden videos. Never more than 3 choices.
- A child only sees collections allowed for their profile AND matching their age band. Hidden videos never appear for that child.
- When a parent hides a video, send `{youtubeId, reason: "parent_hidden"}` to the backend with no device, user or child identifiers.
- Subscriptions: nothing a child can watch ever depends on the subscription; child mode never shows locks, prices or the paywall. The paywall is only reachable in parent mode (after the PIN): from a locked extra child, "+" at the free limit, the dashboard's subscription link, and the Activity week card. Prices, periods and trials come only from the store.
- Expiry never changes settings or history: only the first (oldest) child stays usable (the others keep everything and come back with the subscription), and Activity shows today only (history keeps being recorded).
- Consent: onboarding asks the parent to agree, right after the PIN, that videos play through YouTube's player and subscriptions go through Apple/Google and RevenueCat. Older installs without it get the same consent card on the paywall. Before consent the app never contacts RevenueCat.
- RevenueCat is configured only through `ConnectBillingUseCase`: in parent mode (dashboard, paywall), or at launch / back to the foreground to re-check a cached subscription whose period ended. Child mode otherwise reads the cached status (`EntitlementCache`).
- RevenueCat runs with an anonymous app user id only: never `logIn`, attributes, device identifiers, integrations or store in-app messages.

## UI and design
- RTL everywhere. Display numbers with Western digits (0–9), never Arabic-Indic.
- Fonts (bundled): Baloo Bhaijaan 2 (display, 700/800) and Cairo (body, 400/600/700).
- Colors: ink #17243A · muted #56657A · background #F4F7FB · surface #FFFFFF · primary #2563D9 · primaryDark #1A4FB5 · accent #FFB627 · border #C9D3E0 · divider #E3E9F1 · success #1F7A4A · danger #B23A26 · celebrate #FFF4D6 · childBackground #FFF8EC (Home and Favorites) · dark (player / gate) #0F1724, #17243A, surfaces #1E2B40, #24364F.
- Collection covers (bg / fg): letters #FFE6B0/#8A5A00 · numbers #D6E8FF/#1A4FB5 · Quran & duas #D9F2E4/#1F7A4A · manners #FFDCD6/#B23A26 · science #E6DEFF/#5B3DC4 · stories #FFE3C7/#A3530E.
- Shapes: cards 20–24dp radius, buttons 18dp, chips fully rounded. 4dp spacing grid.
- Touch targets ≥ 48dp everywhere, ≥ 56dp for primary child actions.
- Video cards = real YouTube thumbnail + YouTube attribution + duration badge. Collection covers = unified style (color + glyph/icon), never a thumbnail.
- Layout: compact width → bottom bar with 2 tabs (Home, Favorites). Expanded width (tablet) → navigation rail on the start side (right in RTL), larger cards, more items per shelf. Use window size classes.
- Reference screens: `design/*.png`, exported at 2x (780×1688 px = 390×844 dp phone; `HomeTablet.png` 2560×1600 px = 1280×800 dp tablet landscape). Names: Onboard, SetPin, (consent step: no image yet), ChildSetup, SetupCollections, HomeFeed, HomeTablet, Collection, Clips (= the video player screen), UpNext, DoneToday, Favorites, Gate, Parent, Activity, Paywall. Match them; when the code and an image disagree, ask.
- Strings: Egyptian colloquial Arabic in `composeResources/values/strings.xml`. No hard-coded strings in composables.

## How to work
- Start every phase in plan mode and wait for approval.
- Small commits with clear messages. but never commit before ask me.
- Never compile, build, run or test the project (no `./gradlew`, no Xcode builds, no SDK or Gradle downloads). Only implement the code. Mahmoud runs `./gradlew check` and the iOS build himself and reports any errors back.
- Never add a dependency without saying why. Never add tracking, analytics or crash SDKs without asking.
- When unsure about a product rule, ask instead of guessing.
