# ClipKid: compliance audit and release checklist

Phase 7 audit. Branch `phase-6-billing` at `40b04ce`, audited 2026-11-06. Every policy item below was checked against the official page **as published today**. The sources are listed at the end with the "last updated" date each page showed.

**Statuses**
- **OK**: complies today. The evidence shows where.
- **FIX**: a code or config change I propose. Nothing marked FIX has been changed yet; I'll apply only the ones you approve.
- **NEEDS DECISION**: a product or legal call that is yours. Each one is written as a question in the "Decisions" section.

Things you do in the store and Google consoles are not statuses; they are in "Your console steps" at the end.

## Summary

| | OK | FIX | NEEDS DECISION |
|---|---|---|---|
| A. YouTube API Services | 16 | 4 | 2 |
| B. Google Play Families | 6 | 2 | 1 |
| C. Apple Kids Category (1.3, 5.1.4) | 6 | 1 | 2 |
| D. iOS privacy manifest | 1 | 1 | 0 |
| E. COPPA / GDPR-K | 4 | 0 | 1 |
| F. Release build | 1 | 7 | 2 |
| G. Accessibility | 6 | 1 | 1 |
| **Total** | **40** | **16** | **9** |

The 9 NEEDS DECISION rows map to 9 questions, D1–D9 (A18 and A21 share D1; C7 shares D2). The 16 FIX items are: A10, A13, A14, A19, B3, B8, C6 (same work as A14), D-1, F1–F7, G6.

The biggest finding comes first: **D1, the paywall over YouTube videos, likely breaks YouTube's Developer Policies (III.F.3.a) and touches Apple 5.2.2.** I recommend settling it before anything else, because it changes the paywall, the store listing and the privacy policy.

---

## A. YouTube API Services (Terms, Developer Policies, Required Minimum Functionality)

| ID | Requirement (source) | Status | Evidence | Notes / proposed change |
|---|---|---|---|---|
| A1 | The player identifies the app through the HTTP Referer: `https://<app ID>`, stable across versions (RMF) | OK | `core/player/.../PlayerConfig.kt:12`; `PlayerPlatform.android.kt:129` (`loadDataWithBaseURL`); `PlayerPlatform.ios.kt:114` (`loadHTMLString(baseURL:)`) | The value must equal the store app ID. Android `com.mahmoud.clipkid` matches. **The iOS bundle id does not match (F3).** |
| A2 | Player params; no autoplay of the next video | OK | `youtube_player.html:53-58` (`rel:0, playsinline:1, enablejsapi:1, origin`); `PlayerScreen.kt:168` (the player is removed on end, then Up Next waits for the child's tap) | |
| A3 | Autoplay only once the player is visible, one autoplaying player per screen (RMF) | OK | `PlayerPlatform.android.kt:111` (the first play only, for the video the child just tapped); the player is full width, one per screen | |
| A4 | Viewport at least 200×200 (RMF) | OK | `PlayerScreen.kt:62` (`MIN_PLAYER_SIZE = 200.dp`), `:191-200` | Needs a visual check in your release screenshots. |
| A5 | Nothing drawn over or obscuring the player (RMF) | OK | `youtube_player.html:9-11`; `PlayerScreen.kt` `PlayerFrame` (content only) | |
| A6 | Don't modify, block or replace the player or its ads (III.I.05, III.I.06) | OK | No CSS/JS touches the iframe; links are caught, not removed (`PlayerPlatform.android.kt:159`, `PlayerPlatform.ios.kt:156`) | |
| A7 | No background playback (III.I.09) | OK | `YouTubePlayer.kt:51` (pause on `ON_STOP`); `PlayerPlatform.android.kt:77` (`webView.onPause`); `PlayerPlatform.ios.kt:51-56` (pause on `didEnterBackground`) | |
| A8 | No download, cache or offline playback of audio/video (III.E.1.a-b) | OK | Video only ever plays in the iframe; offline shows "مفيش نت" (`PlayerViewModel.kt` prepare) | |
| A9 | API data kept ≤ 30 days, then refreshed or deleted (III.E.4 items 03-04) | OK | Backend: `supabase/migrations/20261106000003_refresh.sql:23-40` (sweep), `20261106000002_rls.sql:32`; app: `OfflineFirstCatalogRepository.kt:53,87` | |
| A10 | Thumbnails cached on the device also ≤ 30 days | **FIX** | `shared/.../App.kt:24-27`: Coil's default disk cache has no age limit, so a thumbnail can stay for months | **Fix A10:** clear Coil's disk cache when it is older than 7 days (timestamp in DataStore, checked at start). Memory cache unchanged. |
| A11 | Look up Made for Kids for every embedded video (III.E.4 item 10) | OK | `supabase/functions/refresh-youtube-data/youtube.ts:41,77-87` (`madeForKids` false → unavailable) | |
| A12 | No tracking on Made for Kids players; no personalized ads (III.E.4 item 10, III.J.1.c) | OK | `gradle/libs.versions.toml`: no analytics, ads or attribution SDK | |
| A13 | Attribution with **official** YouTube brand assets on every thumbnail and video (III.F.2, Branding Guidelines) | **FIX** | Every screen still uses the stand-in: `VideoCard.kt:201` `YouTubeBadgePlaceholder()`, used at `HomeScreen.kt:374,408`, `CollectionScreen.kt:193`, `FavoritesScreen.kt:136`, `UpNextScreen.kt:149,180,233`, `ActivityScreen.kt:221` | **Fix A13:** a real `YouTubeAttribution` component that draws the official **YouTube Icon** (Branding Guidelines: the icon is for "label[ling] individual items" where a logo doesn't fit). It stays part of the card's tap target, which opens the video in our player (the guidelines say the logo must link to YouTube content "within your app"). **I need the official icon files from you** (from YouTube's brand downloads page); I won't redraw the mark. |
| A14 | Users must agree to a privacy policy before using the client; the client shows a link to YouTube's Terms of Service, and the policy says it uses YouTube API Services and links to the Google Privacy Policy (III.A.1, III.A.2.a-d) | **FIX** | Today the only legal links are on the paywall (`feature/parent/.../paywall/LegalLinks.kt`, placeholder URLs). Onboarding has none (`WelcomeScreen.kt:52`). | **Fix A14:** (1) Onboarding PIN step: a line "بالمتابعة إنت موافق على سياسة الخصوصية وشروط خدمة YouTube" plus a "اقرأها" button that opens the policy **inside the app** (bundled text, no link out before the PIN exists). (2) A new parent-mode "عن التطبيق والخصوصية" screen (behind the PIN) with the full policy, plus links to YouTube ToS, Google Privacy Policy, our terms and contact. (3) The bundled text is the policy in section "Privacy policy drafts". |
| A15 | Child-directed client: notify Google with its tools (III.J.1.b, ToS §9.1(iii)) | OK (code) | No write actions, no OAuth, no personalized ads | The designation itself is a console step: **M1 and M2**. |
| A16 | No YouTube write actions from a child-directed client (III.J.2) | OK | The app never calls the Data API; the backend calls only `videos.list` (`index.ts:94`) | |
| A17 | Quota extension / API Compliance Audit | OK (not needed) | One `videos.list` call per 50 videos per day at 1 unit each (Phase 5) vs the 10,000-unit default ([quota page](https://developers.google.com/youtube/v3/guides/quota_and_compliance_audits): an audit is needed "If you would like to request additional quota") | YouTube can still audit at any time (III.H). D1 option B needs YouTube's written approval, which goes through the same audit form. |
| A18 | Don't charge to watch content in an embedded player; don't gate a video behind any action other than "play" (III.F.3.a-b; policy guide: "cannot charge people a fee for services that are offered free of charge on YouTube") | **NEEDS DECISION** | `VisibleCatalog.kt:41` (premium collections need `full_access`); paywall copy `strings.xml:107-108` ("افتح كل الكوليكشنز") | See **D1**. |
| A19 | Privacy-enhanced mode for a child-directed embed ([YouTube Help 171780](https://support.google.com/youtube/answer/171780): views don't personalize YouTube or ads; still requires self-designation) | **FIX** | `youtube_player.html:85` loads `www.youtube.com/iframe_api`; the player uses the default host | **Fix A19:** create the player with `host: 'https://www.youtube-nocookie.com'`. Test that the Referer still works (no error 153) on both platforms before release. |
| A20 | Brand rules for the listing: no "YouTube" in the app name; no implied endorsement | OK | App name "ClipKid"; the listing drafts below don't use the YouTube name or logo | |
| A21 | Gating by parental controls (daily limit / PIN / locks) | **NEEDS DECISION** | `DoneTodayScreen`, `AskParentScreen` | Part of **D1** (question b). |
| A22 | Data deletion when access ends (ToS §24.3) | OK | All API data lives in Supabase (one SQL `delete`) and in each device's cache, which is wiped on the next sync. | |

## B. Google Play: Families policy, target audience, Data safety

| ID | Requirement | Status | Evidence | Notes / proposed change |
|---|---|---|---|---|
| B1 | Every SDK must be appropriate for a child-directed app; ads only from Families self-certified ad SDKs ([Families policy](https://support.google.com/googleplay/android-developer/answer/9893335)) | OK | `gradle/libs.versions.toml`. SDKs: Compose/AndroidX, Koin, Room 3 + bundled SQLite, DataStore, Ktor (OkHttp), Coil 3, supabase-kt (PostgREST only), kotlinx.*, BuildKonfig (build time), RevenueCat `purchases-kmp-core` (Play Billing). **No ads, analytics, crash, attribution or messaging SDK.** | RevenueCat isn't an ads SDK, so the Families self-certified ads list doesn't apply; with an anonymous user it sends no AAID ([RevenueCat Data safety](https://www.revenuecat.com/docs/platform-resources/google-platform-resources/google-plays-data-safety): device identifiers only with ad-ID integrations, which we don't use). |
| B2 | No AAID, persistent device ids, location or phone number from children | OK | `AndroidManifest.xml:5-7`: only `INTERNET` and `ACCESS_NETWORK_STATE` | |
| B3 | No `AD_ID` permission (child-directed, API 33+) | **FIX** | Not declared by us; a library could merge it in. Can't be confirmed without building. | **Fix B3:** `<uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="remove"/>` in the app manifest. Then confirm in the merged release manifest. |
| B4 | In-app purchases: no manipulative tactics, nothing aimed at the child | OK | The paywall exists only in parent mode (`ParentNavigation.kt:130-145`, `ParentScreen` renders nothing while locked, `:196`). Children see "اسأل بابا أو ماما" with no price (`ClipKidNavHost.kt:236-241`). | |
| B5 | Ads shown to children / "Contains ads" declaration | **NEEDS DECISION** | We show no ads ourselves. YouTube can run its own (non-personalized) ads inside the embedded player on Made for Kids videos, and we may not block them (A6). | See **D3**. |
| B6 | Not merely a webview of a website | OK | Native Compose app; the webview hosts only the player | |
| B7 | Target API level | OK | `libs.versions.toml`: targetSdk 37 | |
| B8 | Backups don't copy the children's data off the device | **FIX** | `AndroidManifest.xml:11`: `allowBackup="true"`. Android backup would copy nicknames, history and the PIN-related prefs to the parent's Google Drive. A restored PIN hash can't be decrypted by the new device's Keystore (`KeystoreSecureStore`), which would break the gate. | **Fix B8:** `allowBackup="false"` and `android:dataExtractionRules` excluding everything (API 31+). |
| B9 | Data safety form answers derived from the code | OK | See "Data flows" and "Google Play Data safety answers" below | The answers change if D1, D3 or D4 changes the flows. |
| B10 | Target audience & content declared | — | Console step **M2** | The age groups are decision **D3**. |

## C. Apple: Kids Category (1.3, 5.1.4), subscriptions, privacy label

| ID | Requirement | Status | Evidence | Notes / proposed change |
|---|---|---|---|---|
| C1 | Purchases only behind a parental gate (1.3; [Kids apps](https://developer.apple.com/app-store/kids-apps/): an "adult-level task") | OK | `ClipKidNavHost.kt:94` (`GatePurpose.Paywall` → unlock), `AskParentScreen` → `GateDest(Paywall)` (`:240`); purchases run only in `PaywallViewModel` | The PIN is a parental gate. The "forgot PIN" reset is weaker: see **D8**. |
| C2 | Links out only behind the gate (1.3) | OK | Player links → `GateDest(GatePurpose.ExternalLink)` (`ClipKidNavHost.kt:208-209`); webview navigation blocked (`PlayerPlatform.android.kt:178`, `PlayerPlatform.ios.kt:135-156`); paywall/terms links only inside parent mode (`ParentNavigation.kt:141`) | A14 adds links only inside parent mode. |
| C3 | No third-party analytics or advertising SDKs (1.3, 5.1.4) | OK (SDKs) | Same SDK list as B1 | YouTube's in-player ads are **D2**. |
| C4 | No personal or device information sent to third parties, "even in sections intended for adults", unless the parent explicitly consents ([Kids apps](https://developer.apple.com/app-store/kids-apps/)) | **NEEDS DECISION** | `AppStartup.kt:42` starts RevenueCat at every launch (`RevenueCatBillingRepository.kt:72,88`); its requests carry an anonymous app user id, OS/app version and locale. The YouTube player (Google) receives standard browser/device data. | See **D4**. Is supabase-kt OK? **Yes:** it talks only to our own backend (a processor), sends no ids, and the only write is `{youtube_id, reason}` (`SupabaseVideoReporter.kt:14`). |
| C5 | Auto-renewable subscription disclosure (3.1.2(c), Schedule 2) | OK | Paywall: store price per period, trial length, the "after the trial" line, auto-renew text, restore, terms, privacy (`PaywallScreen.kt` `LegalText`, `BottomActions`) | The URLs are placeholders (D7). The content changes with D1. |
| C6 | Privacy policy link in App Store Connect and inside the app (5.1.1(i)) | **FIX** | Only on the paywall today (placeholders) | Same fix as **A14**. URLs: D7. |
| C7 | "For Kids" wording in metadata only inside the Kids Category (2.3.8, 5.1.4) | **NEEDS DECISION** | Store listing drafts below | Depends on **D2**. Both variants are drafted. |
| C8 | Export compliance | — | | Tracked as **F7**. |
| C9 | Monetizing access to third-party content needs the service's permission (5.2.2) | — | | Same question as **D1**. |
| C10 | Privacy nutrition label answers | OK | See "Apple App Privacy answers" below | Depends on D1/D4. |
| C11 | Restore purchases is available | OK | Paywall "استرجاع الاشتراك" | |

## D. iOS privacy manifest

| ID | Requirement | Status | Evidence | Notes / proposed change |
|---|---|---|---|---|
| D-1 | The app bundle has a `PrivacyInfo.xcprivacy` declaring every required-reason API used by the app's executable ([Apple](https://developer.apple.com/documentation/bundleresources/describing-use-of-required-reason-api): no manifest means no upload since May 1, 2024) | **FIX** | None in `iosApp/` | The Kotlin framework is **static** (`shared/build.gradle.kts`, `isStatic = true`), so Compose MP, Kotlin/Native, bundled SQLite, Okio/DataStore, Coil, Ktor and RevenueCat's iOS core are all **inside the app executable**. Their uses must be declared in **the app's** manifest; their own manifests aren't in the bundle. **Fix D-1:** add the manifest below to `iosApp/iosApp/` (the target uses a synchronized folder, `project.pbxproj:23-37`, so no project edit is needed). Then you run Xcode → Archive → *Generate Privacy Report* and JetBrains' `required_reason_finder.py` to confirm the list. |
| D-2 | No tracking, no tracking domains | OK | No tracking SDKs; RevenueCat declares `NSPrivacyTracking=false` | |

Draft `iosApp/iosApp/PrivacyInfo.xcprivacy` content:

- `NSPrivacyTracking` = false; `NSPrivacyTrackingDomains` = empty
- `NSPrivacyCollectedDataTypes`:
  - `PurchaseHistory`: not linked, not tracking, purposes App Functionality + Analytics (RevenueCat's own manifest and docs)
  - `ProductInteraction`: not linked, not tracking, App Functionality (anonymous "parent hid this video" / "video failed" reports)
- `NSPrivacyAccessedAPITypes`:

| Category | Reason | Why (source) |
|---|---|---|
| `NSPrivacyAccessedAPICategoryFileTimestamp` | `0A2A.1` | Compose Multiplatform `stat`/`fstat` ([Kotlin docs](https://kotlinlang.org/docs/multiplatform/multiplatform-privacy-manifest.html)) |
| `NSPrivacyAccessedAPICategoryFileTimestamp` | `C617.1` | Bundled SQLite, Okio/DataStore and Coil read file metadata inside the app container |
| `NSPrivacyAccessedAPICategorySystemBootTime` | `35F9.1` | Compose MP `mach_absolute_time` (Kotlin docs); coroutine timers measure elapsed time |
| `NSPrivacyAccessedAPICategoryUserDefaults` | `CA92.1` | RevenueCat (its own manifest: `CA92.1`) |
| `NSPrivacyAccessedAPICategoryDiskSpace` | `E174.1` | Only if the Xcode privacy report shows it (Coil's disk cache sizing). Drop it if it isn't reported. |

Apple's reason-code page didn't render as text for me, so the code meanings above come from the Kotlin page and RevenueCat's manifest. Treat **Generate Privacy Report** as the source of truth.

## E. COPPA / GDPR-K

| ID | Requirement | Status | Evidence | Notes |
|---|---|---|---|---|
| E1 | No personal information from children leaves the device | OK | Children's data is only in local Room/DataStore: `Entities.kt:11` (`child_profiles`: nickname, avatar color, age band, settings), `:25` watch events, `:40` favorites, `:47` hidden, `:54` extra grants; PIN as a PBKDF2 hash in Keystore/Keychain (`Crypto.ios.kt:16`). Network flows are below: none carries a nickname, age, history or a device id. | |
| E2 | Persistent identifiers count as personal information (FTC COPPA FAQ A.3, C.5) | **NEEDS DECISION** | RevenueCat's anonymous app user id; YouTube player cookies (the operator is responsible for third parties on its service, FAQ D.9 / D.11) | RevenueCat's id is used only to validate and restore the purchase. That fits COPPA's "support for internal operations" exception (FAQ C.5, F.2) **if** we collect no other personal information and never use it to contact anyone or build a profile. YouTube: A19 (privacy-enhanced mode) plus self-designation M1/M2. The open part is the same question as **D4**. |
| E3 | Written retention and security policy (amended COPPA Rule, April 22, 2025) | OK | Included in the policy drafts below (retention section) | It becomes real once you publish it (D7). |
| E4 | Parents can review or delete | OK | Everything about a child is on the device: the dashboard edits it, uninstall deletes it. Reports are anonymous and can't be matched to anyone. RevenueCat records can be deleted on request (dashboard / REST API). The policy explains how. | |
| E5 | GDPR Art. 8: consent from the parent for under-16s where consent is the legal basis | OK | No consent-based processing of a child's data. Purchase processing is the parent's contract. | Only matters if you publish in the EU. Your `TARGET_REGIONS` are EG, KW, SA, AE. |

## F. Release build

| ID | Item | Status | Evidence | Notes / proposed change |
|---|---|---|---|---|
| F1 | R8 shrinking and obfuscation | **FIX** | `androidApp/build.gradle.kts:37` `isMinifyEnabled = false`; `androidApp/proguard-rules.pro` is the template | **Fix F1:** `isMinifyEnabled = true`, `isShrinkResources = true`, plus the rules in "R8 rules" below. Then you smoke-test the release build (onboarding, player, paywall, sync). |
| F2 | Android release signing | **FIX** | No `signingConfigs` in `androidApp/build.gradle.kts` | **Fix F2:** a `release` signing config read from a gitignored `keystore.properties` (or env `CLIPKID_KEYSTORE_*`). You create the upload key and enroll in Play App Signing. |
| F3 | iOS bundle id = store app ID = player Referer | **FIX** | `iosApp/Configuration/Config.xcconfig:4` `com.mahmoud.clipkid.ClipKid$(TEAM_ID)` (wizard default); `TEAM_ID` is empty (`:1`) | **Fix F3:** `PRODUCT_BUNDLE_IDENTIFIER=com.mahmoud.clipkid`. You set `TEAM_ID`. It must match App Store Connect, RevenueCat and `PlayerConfig.BASE_URL` (A1). |
| F4 | A release must never ship without backend or billing keys | **FIX** | `DataModule.kt:74` and `BillingModule.kt:53` quietly fall back to the fake catalog / no billing when a key is missing | **Fix F4:** a Gradle check that fails release builds (`assembleRelease`/`bundleRelease`, and the iOS Release framework) when `supabase.*` or this platform's RevenueCat key is empty, or when the Test Store key is the only one set. |
| F5 | Versioning scheme | **FIX** | `androidApp/build.gradle.kts:27-28` (`1` / `"1.0"`), `Config.xcconfig:6-7` (`1` / `1.0`), set separately | **Fix F5:** one source, `iosApp/Configuration/Version.xcconfig` (`MARKETING_VERSION = 1.0.0`, `CURRENT_PROJECT_VERSION = 10000`); Android Gradle reads the same file. SemVer `MAJOR.MINOR.PATCH`; build number = `MAJOR*10000 + MINOR*100 + PATCH` (1.0.0 → 10000, 1.2.3 → 10203), same on both stores. A re-upload of the same version bumps PATCH. |
| F6 | Launch screens | **FIX** | iOS: `project.pbxproj:309,337` generated blank launch screen (white); Android: `AndroidManifest.xml:16` plain framework theme, so the API 31+ system splash uses defaults | **Fix F6:** iOS `UILaunchScreen` with background color asset `LaunchBackground` (#F4F7FB) and the app icon image; Android `values/themes.xml` + `values-v31/themes.xml` with `windowSplashScreenBackground` #F4F7FB and the launcher icon. No new dependency. |
| F7 | Export compliance (HTTPS only) | **FIX** | `iosApp/iosApp/Info.plist`: key missing, so App Store Connect asks on every upload | **Fix F7:** `ITSAppUsesNonExemptEncryption = false`. Every connection is standard HTTPS/TLS from the OS. |
| F8 | App icons | **NEEDS DECISION** | iOS `AppIcon.appiconset/app-icon-1024.png` is the Kotlin Multiplatform template (blue hexagon); Android `mipmap-*` / `drawable*/ic_launcher_*` is the default green Android robot | See **D6**. |
| F9 | Signed release builds for both stores | **NEEDS DECISION** | CLAUDE.md: "Never compile, build, run or test the project … Mahmoud runs `./gradlew check` and the iOS build himself." The signing keys and Xcode are on your machine. | See **D5**. |
| F10 | No debug logging of user data in release | OK | Only player error codes are logged (`PlayerPlatform.android.kt:42`, `PlayerPlatform.ios.kt:37`); webview debugging only in debug builds (`PlayerPlatform.android.kt:123`) | |

### R8 rules (Fix F1)

Most of our libraries ship consumer rules inside their AARs (AndroidX/Room, Compose and Compose resources, Coil 3, Ktor, kotlinx.serialization, RevenueCat). What we add ourselves:

```proguard
# Player bridge: page → app messages. The class is private, so keep the annotated method by annotation.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# kotlinx.serialization: navigation routes, Supabase rows and the player bridge are @Serializable.
# The library's embedded rules cover @Serializable classes; this keeps their companions when they
# are only reached through generic code (Navigation's typed routes).
-keepclassmembers @kotlinx.serialization.Serializable class com.mahmoud.clipkid.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor / supabase-kt: optional JVM-only classes that are never present on Android.
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn java.lang.management.**

# Keep line numbers for Play Console crash reports (no third-party crash SDK), hide source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
```

Whatever the first R8 run still reports under `missing_rules.txt` (`androidApp/build/outputs/mapping/release/`) gets added after you send it to me.

## G. Accessibility

| ID | Item | Status | Evidence | Notes / proposed change |
|---|---|---|---|---|
| G1 | Content descriptions are Arabic and come from resources | OK | No hard-coded descriptions in app screens; the only literal is `""` in a preview (`WeekBarChart.kt:117`) | |
| G2 | Every icon-only control has a label | OK | `IconButtonLarge` requires one (`Buttons.kt:147`); e.g. `ChildTabs.kt:122`, `PlayerScreen.kt:109,343`, `PinPad.kt:130`; covers and cards merge their title (`CollectionCover`, `VideoCard.kt:186`) | |
| G3 | Text contrast at least 4.5:1 (WCAG 1.4.3) | OK | Computed from `Color.kt` tokens: ink/background 14.5; muted/background 5.5; muted/surface 5.9; white/primary 5.4; primary links/background 5.1; ink/accent 8.9; danger/background 5.5; success/surface 5.3; onDarkMuted/dark 7.8; badge text 9.7. Cover glyphs (large text, 3:1 needed) 4.5 to 6.0 | |
| G4 | Non-text contrast at least 3:1 for control outlines and meaningful icons (WCAG 1.4.11) | **NEEDS DECISION** | Unselected outline `border` #C9D3E0 on white = **1.51:1** (text field `SetupStepsScreens.kt:297-299`, age options `SetupControls.kt:191-195`, plan cards); premium star `accent` #FFB627 on white = **1.75:1** (`DashboardScreen.kt` `CollectionRow`, it has a text label) | See **D9**: these are CLAUDE.md design tokens. |
| G5 | Text scales with the system font size | OK | All sizes in `sp` (`Type.kt:63-65`); Compose follows Android font scale and iOS Dynamic Type | |
| G6 | No clipped text at 130% font size | **FIX** | Fixed heights around text: `SetupControls.kt:191` (52 dp age option), `:234` (72 dp collection card), `SegmentedControl.kt:52` (56 dp), `SetupStepsScreens.kt:297` (56 dp field), `GateScreen.kt:322` (64 dp PIN field) | At 130%, the largest label in them is ≈ 34 dp tall, so nothing clips today; at 150–200% the 52/56 dp boxes would. **Fix G6:** `height(x)` → `heightIn(min = x)` in those five places. Buttons, chips, plan cards and pickers already use minimum sizes. Check on device: Android font size at the largest step and iOS "Larger Text" at 130%: onboarding, dashboard, paywall, PIN gate. |
| G7 | Headings, roles and states | OK | `Shelf.kt:107` headings; `Role.Button` / `Role.Switch` / `Role.RadioButton` (`PlanCard`) / `Role.Checkbox`; quota dots and PIN dots announce a summary (`QuotaDots.kt:52`, `PinPad.kt:152`) | |
| G8 | Touch targets at least 48 dp, child actions at least 56 dp | OK | `Dimens.kt:41-45`; `ButtonFrame` minimum height 56 dp (`Buttons.kt:220`) | |

---

## Decisions (your answers needed)

**D1. The subscription unlocks YouTube videos. YouTube's policies forbid charging to watch embedded YouTube content. What should the subscription sell?** (A18, A21, C9)

- **Evidence:**
  - Developer Policies III.F.3.a: API clients "must not charge users to watch content in an embedded YouTube player".
  - III.F.3.b: they must not "gate access to a video by requiring a user to take an action other than clicking the play button".
  - The policy guide: "You also cannot charge people a fee for services that are offered free of charge on YouTube."
  - Apple 5.2.2: an app that "monetizes access to … content from a third-party service" must be "specifically permitted to do so under the service's terms".
  - Today the premium collections are YouTube embeds behind `full_access` (`VisibleCatalog.kt:41`).
- **Options:**
  - **A (recommended):** every collection is free. The subscription sells our own parent features: more than one child profile, minutes mode, the weekly activity report, extra grants from the dashboard (pick the set). No YouTube video is ever behind a payment.
  - **B:** keep premium collections only with YouTube's prior written approval, through the API audit form. Slow and uncertain; release waits for it.
  - **C:** no subscription in v1.
- **Sub-question (b):** the daily limit, the PIN and the locks are parental controls, not a monetization gate. My reading is that they're fine, but III.F.3.b is broad. Should I describe them in your YouTube API audit form answers if YouTube asks?
- **Impact:** with A, I change `VisibleCatalog`/`Access` (premium no longer locks videos), the paywall benefits and copy, `is_premium` (unused or removed), the AskParent trigger (profiles only) and the docs.

**D2. Submit to Apple's Kids Category or not?** (C3, C7)

- **For:** the app is for 2–12-year-olds, and outside the Kids Category, 2.3.8 / 5.1.4 forbid anything in the name, subtitle, icon, screenshots or description that "impl[ies] the main audience for the app is children".
- **Against:** in the Kids Category, 1.3 says no "third-party advertising" unless it is human-reviewed contextual advertising. The YouTube embed can show YouTube's own ads, and YouTube's rules say we must not block them (A6).
- **Options:**
  - **A:** Kids Category. Pick **one** age band (5 and under / 6–8 / 9–11; Apple allows only one). Accept the review risk on embedded YouTube ads; I'll prepare a review note explaining that the videos are Made for Kids embeds with no ad SDK of ours.
  - **B:** no Kids Category: general listing, 4+ rating, and metadata that speaks to parents ("بإشراف الأهل") without saying "for kids". Both listing variants are drafted below.
- **Which age band, if A?**

**D3. Google Play target audience, and the "Contains ads" answer.** (B5, B10)

- **Age groups:** I'd pick 5 and under, 6–8 and 9–12 (the app is 2–12). That makes the app "solely child-directed", so the full Families policy applies; the code already meets it (B1–B4).
- **"Contains ads":** we serve none. YouTube may show non-personalized ads inside the player. Do you want to answer "No" (we have no ad SDK and no ad inventory), or "Yes" to be conservative? "Yes" shows a badge and invites Families ads review of an SDK we don't have. My recommendation is "No" plus a note in the review comments, but this is a judgment call.

**D4. RevenueCat (and the YouTube player) send device-level data before any parent consent. Is that acceptable?** (C4, E2)

- **Evidence:**
  - Apple's Kids page: "Kids apps should not transmit personally identifiable information or device information to third parties — even in sections intended for adults — unless the parent explicitly consents".
  - RevenueCat is configured at every launch (`AppStartup.kt:42`). Its requests carry an anonymous app user id plus OS, app version and locale.
  - The YouTube player is Google, our content provider, and is unavoidable.
- **Options:**
  - **A (recommended):** in onboarding, right after the PIN, a parent consent step: "ClipKid uses Apple/Google and RevenueCat to handle subscriptions, and YouTube's player to play videos", with Agree. RevenueCat is configured **only after consent, and lazily the first time the parent opens the paywall**. The last known entitlement is cached on the device, so content unlocks without contacting RevenueCat at every launch; a refresh happens only in parent mode, or once a day after consent. This also keeps RevenueCat's anonymous id out of child sessions (COPPA internal-operations framing).
  - **B:** keep it as it is (RevenueCat at launch). Simpler; higher Apple review risk.
- If D1 = C (no subscription), RevenueCat is removed and this question goes away for RevenueCat.

**D5. Who produces the signed release builds?** (F9)

- CLAUDE.md forbids me from building, and the signing keys and Xcode are on your Mac.
- I propose: I make every code and config change, and you run:
  - Android: `./gradlew :androidApp:bundleRelease` with your `keystore.properties`.
  - iOS: Xcode → Product → Archive with your team.
  - Then you send me any errors and the R8 `missing_rules.txt`.
- Or, if you want me to run them this once, say so explicitly and I'll use the folder's shell. It's a Linux VM, so **iOS can't be built there either way.**
- The acceptance item "signed release builds produced" can only be ticked by you.

**D6. App icon.** (F8)

- Both platforms still ship the template icons.
- Do you have a ClipKid icon? If yes, send a 1024×1024 PNG plus a foreground/background split for the Android adaptive icon.
- If not, I can draw an original vector icon from the design tokens (play-button shape in a rounded square, primary #2563D9 with accent #FFB627) and generate every size. You approve it before it ships.

**D7. Legal details for the privacy policy and the stores.** (A14, C5, C6, E3)

Fill these in and I'll finalize the bundled policy text and the URLs:

1. Publisher name as shown in the stores (your name or a company).
2. Country.
3. Support/privacy email.
4. Where the policy and terms pages live. They must be public URLs; a free GitHub Pages or Notion page works.
5. Your Supabase project region.
6. How long to keep anonymous video reports. I suggest 12 months.
7. Terms of use: Apple's standard EULA for iOS plus your own short terms, or your own for both?

**D8. The "forgot PIN" reset is a multiplication (6–9 × 11–19). Is that hard enough for a 9–12-year-old?** (C1)

- Evidence: `AdultChallenge.kt` (`CreateAdultChallengeUseCase`). A child who resets the PIN gets parent mode: settings, paywall and links.
- **Options:**
  - **A (recommended):** a harder challenge: two 2-digit numbers (e.g. 37 × 48), typed answer, a 2-minute lockout after 3 wrong answers.
  - **B:** keep it as it is.
  - **C:** no reset; reinstalling clears the PIN and the data.

**D9. Non-text contrast: change two tokens?** (G4)

- The unselected `border` #C9D3E0 (1.51:1 on white) and the gold star (1.75:1) are below WCAG's 3:1 for control outlines and icons.
- **Options:**
  - **A:** darken `border` to about #8A97AA (3.0:1) for interactive outlines only; dividers stay #E3E9F1. The star gets a 1 dp ink outline or uses `primaryDark`.
  - **B:** keep the CLAUDE.md tokens. Every affected control also has a text label or a fill change, so the information isn't lost.

---

## Data flows (from the code)

Every network call the app makes, and what it sends. All of them use HTTPS.

| # | When | Host | Sends | Receives | Code |
|---|---|---|---|---|---|
| 1 | Every app start (catalog sync) | `<project>.supabase.co` (PostgREST) | `GET collections` (fixed columns), `GET videos?select=youtube_id,updated_at` paged, `GET videos?youtube_id=in.(…)`. Headers: publishable `apikey`, `X-Client-Info: supabase-kt/<ver>`, standard User-Agent. **No user, child or device identifier.** | Catalog rows | `SupabaseCatalogDataSource.kt:20-49`, `SupabaseClientFactory.kt:15-21` |
| 2 | A parent hides a video, or YouTube reports error 100/101/150 | same | `POST video_reports {youtube_id, reason}`, reason ∈ `parent_hidden`, `video_removed`, `embed_disabled`. Nothing else (anon can't read the table). | — | `SupabaseVideoReporter.kt:14`, `PlayerViewModel.kt:311-312` |
| 3 | Thumbnails on screen | `i.ytimg.com` (URL from the catalog) | Image GET (IP, User-Agent) | JPEG | `App.kt:24-27` (Coil + Ktor) |
| 4 | The player screen | `www.youtube.com` (→ `youtube-nocookie.com` after A19), `*.googlevideo.com`, `*.ytimg.com` | YouTube's own iframe traffic. Referer `https://com.mahmoud.clipkid`; YouTube's cookies/storage in the webview | Player, stream, YouTube's ads | `youtube_player.html:85`, platform players |
| 5 | Launch, foreground, paywall, purchase, restore | `api.revenuecat.com` | Anonymous app user id (`$RCAnonymousID:…`), store purchase token/receipt, product ids, platform/OS/app version, locale, storefront. **No attributes, no ad ids** (`RevenueCatBillingRepository.kt:72-75`) | Entitlement `full_access`, offering | `core/billing/...` |
| 6 | Purchase / restore | Google Play Billing / Apple StoreKit | Handled by the OS store with the parent's store account | Transactions | — |
| — | Never from the app | YouTube Data API | Only our backend's scheduled `videos.list` (server-to-server, no user data) | | `supabase/functions/refresh-youtube-data/index.ts:94` |

Stored only on the device: child profiles (nickname, avatar color, age band, allowed collections, daily limit), watch events, favorites, hidden videos, extra grants, cached catalog, settings, the PIN hash (Keystore/Keychain), Coil image cache, RevenueCat's own cache (anonymous id, last entitlement).

## Google Play Data safety answers

Based on the current code; they change if D1, D3 or D4 changes the flows.

- **Does your app collect or share any of the required user data types?** Yes.
- **Is all user data encrypted in transit?** Yes.
- **Do you provide a way for users to request deletion?** Account creation: none. Explain in the policy: data lives on the device; uninstall deletes it; RevenueCat records can be deleted on request by email.

| Category → type | Collected | Shared | Ephemeral | Required / optional | Purposes | Source |
|---|---|---|---|---|---|---|
| Financial info → Purchase history | Yes | No (RevenueCat is a service provider) | No | Required for subscribers | App functionality, Analytics (RevenueCat dashboard) | Flow 5 |
| Device or other IDs | Yes: RevenueCat's app-scoped anonymous id (an identifier "that relate[s] to an … app") | No | No | Required | App functionality | Flow 5 |
| App activity → App interactions | Yes: anonymous video reports (which video a parent hid or that failed). Also what YouTube's player collects about plays inside our webview (Play counts "data from a webview your app controls") | No | No | Optional (parent action) / required (playback) | App functionality | Flows 2, 4 |
| Location, Personal info, Messages, Photos/videos, Audio, Files, Calendar, Contacts, Health, Web browsing, App info & performance | No | — | — | — | — | — |

## Apple App Privacy answers

- **Tracking:** No.
- **Data Linked to You:** none. Everything collected is anonymous: the RevenueCat id is random and there are no accounts.
- **Data Not Linked to You:**
  - **Purchases → Purchase History.** Purposes: App Functionality, Analytics. RevenueCat's docs recommend both.
  - **Usage Data → Product Interaction.** Purpose: App Functionality. Covers the anonymous reports and YouTube player plays.

Identifiers: none. RevenueCat says to declare "User ID" only for a custom app user id and "Device ID" only with ad-id integrations; we use neither.

---

## Privacy policy drafts

Placeholders in `[brackets]` come from D7. The text describes the app **as it is today**; the subscription paragraph changes with D1 and the consent paragraph with D4. Not legal advice: have it reviewed if you can.

### English

> **ClipKid Privacy Policy**
> Last updated: [date]
>
> ClipKid is made by [publisher], [country] ("we"). Contact: [email].
>
> **The short version.** ClipKid has no accounts, no ads from us, and no analytics or tracking. Your children's names, ages, settings and viewing history stay on your device. We never sell data.
>
> **What stays on your device.** The nickname, avatar color and age group you enter for each child; their daily limit and allowed collections; which videos they watched, liked or that you hid; and your parent PIN, stored only as a salted, one-way hash in the device's secure storage. None of this is sent to us or anyone else. Uninstalling the app deletes it. On Android it is excluded from device backups.
>
> **What leaves your device, and why**
> 1. **Catalog updates.** The app downloads the list of reviewed videos from our server (hosted by Supabase in [region]). These requests contain no information about you or your children.
> 2. **Anonymous video reports.** When you hide a video, or a video stops working, the app sends us only the video's YouTube ID and the reason (for example "hidden by a parent"). No device, user or child identifier is included, so a report can't be linked to you. We keep reports for [12 months] to improve the catalog, then delete them.
> 3. **Videos.** Videos play in YouTube's official embedded player, in [privacy-enhanced mode]. ClipKid uses YouTube API Services. When a video plays, YouTube (Google) receives the information any web player receives, such as your IP address and device/browser details, and may show its own non-personalized ads on Made for Kids videos. We have told Google that ClipKid is directed to children. YouTube's use of this information is covered by the Google Privacy Policy (https://policies.google.com/privacy), and by using ClipKid you agree to the YouTube Terms of Service (https://www.youtube.com/t/terms).
> 4. **Subscriptions (optional).** Purchases are handled by Apple (App Store) or Google (Google Play) with your store account; we never see your payment details. To check whether a subscription is active we use RevenueCat (https://www.revenuecat.com/privacy), which receives a random app-generated ID, your purchase receipt, the product bought and basic app/OS information. We don't send RevenueCat any names, emails or other attributes.
>
> **Children.** ClipKid is designed for children aged 2–12 to use under a parent's supervision. We do not knowingly collect personal information from children. Anything a parent enters about a child stays on the device. Purchases, settings and links that leave the app are behind the parent PIN.
>
> **Retention and deletion.** On-device data: until you delete it in the app or uninstall. Video reports: [12 months]. RevenueCat purchase records: as long as needed to provide the subscription, or until you ask us to delete them. Email [email] with your purchase receipt ID and we'll delete them within 30 days. Catalog data from YouTube is refreshed or deleted within 30 days, as YouTube requires.
>
> **Security.** All network traffic uses HTTPS. The PIN is never stored in plain text. Access to our server is restricted to the catalog and anonymous reports.
>
> **Your rights.** Depending on where you live, you may have rights to access or delete data. Since we hold nothing that identifies you, most requests are handled on your device. For anything else, contact [email].
>
> **Changes.** We'll update this page and the date above, and show a notice in the app's parent section for important changes.

### العربي (مصري)

> **سياسة الخصوصية لتطبيق ClipKid**
> آخر تحديث: [التاريخ]
>
> ClipKid من [الناشر]، [البلد] ("إحنا"). للتواصل: [الإيميل].
>
> **باختصار.** ClipKid مفيهوش حسابات، ومفيهوش إعلانات مننا، ولا أي أدوات تحليل أو تتبّع. أسامي ولادك وأعمارهم وإعداداتهم واللي اتفرجوا عليه بيفضلوا على جهازك بس. وعمرنا ما بنبيع أي بيانات.
>
> **اللي بيفضل على جهازك.** اسم الدلع واللون والسن اللي بتكتبهم لكل طفل، والحد اليومي والكوليكشنز المسموحة، والفيديوهات اللي اتفرجوا عليها أو حبّوها أو اللي إنت خبّيتها، والرقم السري بتاعك، ودا بيتخزن بس كبصمة مشفّرة ما ينفعش ترجع رقم، في التخزين الآمن بتاع الجهاز. مفيش حاجة من دي بتتبعت لنا ولا لأي حد. لو مسحت التطبيق بتتمسح كلها، وعلى أندرويد مش بتدخل في الباك أب.
>
> **اللي بيخرج من جهازك وليه**
> ١. **تحديث الكتالوج.** التطبيق بينزّل قايمة الفيديوهات اللي راجعناها من السيرفر بتاعنا (على Supabase في [المنطقة]). الطلبات دي مفيهاش أي معلومة عنك أو عن ولادك.
> ٢. **بلاغات من غير أسماء.** لما تخبّي فيديو أو فيديو يبطّل يشتغل، التطبيق بيبعت لنا رقم الفيديو على YouTube والسبب بس (زي "الأهل خبّوه"). مفيش أي رقم للجهاز أو للمستخدم أو للطفل، فمستحيل نعرف البلاغ جاي من مين. بنحتفظ بالبلاغات [١٢ شهر] علشان نحسّن الكتالوج، وبعدين بنمسحها.
> ٣. **الفيديوهات.** الفيديوهات بتشتغل في المشغّل الرسمي المدمج بتاع YouTube، [في وضع الخصوصية المحسّن]. ClipKid بيستخدم خدمات YouTube API. وقت تشغيل الفيديو، YouTube (جوجل) بيوصله اللي بيوصل لأي مشغّل ويب، زي عنوان الـIP ونوع الجهاز والمتصفح، وممكن يعرض إعلاناته غير المخصّصة على فيديوهات الأطفال. إحنا بلّغنا جوجل إن ClipKid موجّه للأطفال. استخدام YouTube للمعلومات دي خاضع لسياسة خصوصية جوجل (https://policies.google.com/privacy)، وباستخدامك لـ ClipKid إنت موافق على شروط خدمة YouTube (https://www.youtube.com/t/terms).
> ٤. **الاشتراك (اختياري).** الشراء بيتم عن طريق أبل (App Store) أو جوجل (Google Play) بحسابك في المتجر، وإحنا عمرنا ما بنشوف بيانات الدفع. علشان نعرف الاشتراك شغال ولا لأ بنستخدم RevenueCat (https://www.revenuecat.com/privacy)، وبيوصله رقم عشوائي بيعمله التطبيق، وإيصال الشراء، والباقة اللي اشتريتها، ومعلومات بسيطة عن التطبيق ونظام التشغيل. مش بنبعت لـ RevenueCat أي أسامي أو إيميلات أو بيانات تانية.
>
> **الأطفال.** ClipKid معمول لأطفال من سنتين لـ١٢ سنة يستخدموه تحت إشراف الأهل. إحنا مش بنجمع عن قصد أي بيانات شخصية من الأطفال، وأي حاجة الأهل بيكتبوها عن الطفل بتفضل على الجهاز. الشراء والإعدادات وأي لينك بيخرج برّه التطبيق كلهم ورا الرقم السري بتاع الأهل.
>
> **مدة الحفظ والمسح.** البيانات اللي على الجهاز: لحد ما تمسحها من التطبيق أو تمسح التطبيق. البلاغات: [١٢ شهر]. سجلات الشراء عند RevenueCat: طول ما الاشتراك محتاجها، أو لحد ما تطلب مسحها: ابعت لنا على [الإيميل] رقم إيصال الشراء وهنمسحها خلال ٣٠ يوم. بيانات الكتالوج اللي جاية من YouTube بتتحدّث أو تتمسح كل ٣٠ يوم زي ما YouTube بيطلب.
>
> **الأمان.** كل الاتصالات بـ HTTPS. الرقم السري عمره ما بيتخزن زي ما هو. والوصول للسيرفر بتاعنا مقفول على الكتالوج والبلاغات اللي من غير أسماء.
>
> **حقوقك.** حسب البلد اللي إنت فيها، ممكن يكون ليك حق تشوف أو تمسح بياناتك. وبما إن مفيش عندنا حاجة بتعرّف عليك، أغلب الطلبات بتتعمل من الجهاز نفسه. لأي حاجة تانية كلّمنا على [الإيميل].
>
> **التغييرات.** هنحدّث الصفحة دي والتاريخ اللي فوق، ولو فيه تغيير مهم هنعرض تنبيه في قسم الأهل جوه التطبيق.

---

## Store listing drafts (Egyptian Arabic)

Counts are characters as the stores count them (Unicode code points), measured with a script. **Kids** = use if D2 = A (Apple Kids Category) / Play audience under 13. **General** = use if D2 = B: no wording that implies children are the main audience (Apple 2.3.8, 5.1.4). No "YouTube" in names or subtitles (Branding Guidelines), no "best/#1/free" wording (Play metadata policy), and no "no ads" claim (YouTube may show ads in the player).

| Field | Text | Count / limit |
|---|---|---|
| Play title (Kids) | ClipKid: فيديوهات للأطفال | 25 / 30 |
| Play title (General) | ClipKid: فيديوهات مختارة | 24 / 30 |
| App Store name | ClipKid: فيديوهات مختارة | 24 / 30 |
| App Store subtitle (Kids) | فيديوهات هادية لولادك بحد يومي | 30 / 30 |
| App Store subtitle (General) | فيديوهات مختارة بإشراف الأهل | 28 / 30 |
| Play short description (Kids) | فيديوهات بنختارها بإيدينا لولادك من ٢ لـ١٢ سنة، بحد يومي ورقم سري للأهل. | 72 / 80 |
| Play short description (General) | كل فيديو بيتراجع قبل ما يتعرض. حد يومي، ومن غير تشغيل تلقائي، ورقم سري للأهل. | 77 / 80 |
| App Store promotional text | فيديوهات بنختارها ونراجعها واحد واحد، وإنت اللي بتحدد كام فيديو في اليوم. | 73 / 170 |
| App Store keywords (Kids) | حروف,أرقام,قرآن,أدعية,أخلاق,حواديت,علوم,تعليم,أطفال,حضانة,رقابة أبوية,حد يومي | 77 / 100 |
| App Store keywords (General) | حروف,أرقام,قرآن,أدعية,أخلاق,حواديت,علوم,تعليم,رقابة أبوية,حد يومي,عيلة | 70 / 100 |

**Full description (Kids variant, 863 / 4000 characters).** For the General variant, drop the age line and say "للعيلة" instead of "لولادك". The subscription paragraph waits for D1.

```
ClipKid بيقدّم لولادك فيديوهات بنختارها ونراجعها واحد واحد، علشان تبقى مطمّن على اللي بيتفرجوا عليه.

• كل فيديو بيعدّي على مراجعة من فريقنا قبل ما يظهر في التطبيق.
• كوليكشنز مرتّبة: الحروف، الأرقام، قرآن وأدعية، أخلاق، حواديت، علوم.
• محتوى على قد سن كل طفل: من سنتين لحد ١٢ سنة.
• إنت اللي بتحدد الحد اليومي: عدد فيديوهات أو عدد دقايق. لما الحد يخلص، التطبيق بيقول "برافو يا بطل" ويستنى لبكرة.
• مفيش فيد لا نهائي، ومفيش تشغيل تلقائي للفيديو اللي بعده. ابنك هو اللي بيختار، من ٣ اختيارات بس.
• لوحة للأهل برقم سري: تغيّر الإعدادات، تشوف نشاط الأسبوع، تخبّي أي فيديو، وتدّي فيديو زيادة النهارده.
• المفضلة لكل طفل.
• من غير حسابات ومن غير تسجيل دخول: إعدادات ولادك وسجلّهم متخزنين على جهازك بس.

لازم يكون فيه نت علشان الفيديوهات تشتغل.

الاشتراك (اختياري): [يتحدد بعد قرار D1]

سياسة الخصوصية: [الرابط]
شروط الاستخدام: [الرابط]
```

### Screenshot list

Same 6 screens on every device, in this order, with real reviewed videos. Show only videos you have the right to show (your own channel, or creators who agree); thumbnails keep their YouTube attribution (A13). Captions go above each frame, ≤ 6 words, in Arabic:

| # | Screen | Caption |
|---|---|---|
| 1 | HomeFeed (continue watching + covers) | فيديوهات مختارة بإيدينا |
| 2 | Collection (الحروف) | كوليكشنز على قد سنّه |
| 3 | Clips (player, portrait) | تشغيل آمن من غير تشغيل تلقائي |
| 4 | UpNext (3 choices) | هو اللي يختار، من ٣ بس |
| 5 | DoneToday | حد يومي إنت اللي بتحدده |
| 6 | Parent dashboard (after PIN) | لوحة للأهل برقم سري |

| Store | Device class | Size (px, portrait) | Count | Notes |
|---|---|---|---|---|
| App Store | iPhone 6.9" | 1320 × 2868 | 6 (1–10 allowed) | Smaller iPhones scale from this set |
| App Store | iPad 13" | 2064 × 2752 | 6 | Required: the target supports iPad (`project.pbxproj:318` `TARGETED_DEVICE_FAMILY = "1,2"`); use the tablet layout (HomeTablet) |
| Play | Phone | 1080 × 1920 (9:16) | 6 (min 2, max 8) | |
| Play | 7" tablet | 1200 × 1920 | 4–6 | Min 4 for large-screen listing |
| Play | 10" tablet | 1600 × 2560 (or landscape 2560 × 1600) | 4–6 | |
| Play | Feature graphic | 1024 × 500 | 1 | No "best/#1/free" wording, no YouTube logo |
| Play | Icon | 512 × 512, 32-bit PNG | 1 | After D6 |

---

## Your console steps (not code)

- **M1. iOS child-directed self-declaration** for Google: https://coppa.developers.google.com/selfdeclaration/ios/home. Required by YouTube Developer Policies III.J.1.b through [Google's designation tools](https://support.google.com/policies/answer/9664901).
- **M2. Play Console → App content → Target audience and content:** the age groups from D3. This is also Google's child-directed designation for the Play app. Then: Ads declaration (D3), Data safety (answers above), Content rating questionnaire, Families policy declaration, privacy policy URL (D7).
- **M3. App Store Connect:**
  - Privacy policy URL; App Privacy answers (above); age rating questionnaire.
  - Kids Category and age band (D2).
  - Review notes: the PIN for the reviewer, plus a sentence that the paywall and links sit behind it.
- **M4. Supabase:** confirm the project region (D7), and that API logs don't keep request bodies longer than you state in the policy.
- **M5. RevenueCat:** no integrations enabled (already in Phase 6); keep the anonymous ids.
- **M6. Release builds and smoke test:** after the approved fixes (D5): Android `bundleRelease` signed with your upload key, then an internal testing track; iOS Archive → TestFlight. On both:
  - onboarding → video plays (no error 153) → Up Next → DoneToday
  - gate → dashboard → paywall (sandbox purchase/restore)
  - large font size check (G6)
  - Xcode "Generate Privacy Report" (D-1)

---

## Sources (opened 2026-11-06)

- YouTube Required Minimum Functionality: https://developers.google.com/youtube/terms/required-minimum-functionality (last updated 2026-11-01)
- YouTube API Services Developer Policies: https://developers.google.com/youtube/terms/developer-policies (2026-11-01)
- Developer Policies guide: https://developers.google.com/youtube/terms/developer-policies-guide (2026-09-14)
- YouTube API Services Terms of Service: https://developers.google.com/youtube/terms/api-services-terms-of-service (2026-11-01)
- YouTube Branding Guidelines: https://developers.google.com/youtube/terms/branding-guidelines (2025-08-20)
- Quota and compliance audits: https://developers.google.com/youtube/v3/guides/quota_and_compliance_audits (2026-11-03)
- Child-directed designation tools: https://support.google.com/policies/answer/9664901
- YouTube privacy-enhanced mode: https://support.google.com/youtube/answer/171780
- Google Play Families policy: https://support.google.com/googleplay/android-developer/answer/9893335
- Google Play Data safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Google Play store listing assets: https://support.google.com/googleplay/android-developer/answer/9866151
- App Store Review Guidelines (1.3, 2.3.8, 3.1.2, 5.1.1, 5.1.4, 5.2.2, 5.2.3): https://developer.apple.com/app-store/review/guidelines/ (last updated October 16, 2026)
- Apple Kids apps / parental gates: https://developer.apple.com/app-store/kids-apps/
- Apple App privacy details: https://developer.apple.com/app-store/app-privacy-details/
- Apple required reason API: https://developer.apple.com/documentation/bundleresources/describing-use-of-required-reason-api
- Apple third-party SDK requirements: https://developer.apple.com/support/third-party-SDK-requirements/ (RevenueCat, Ktor, Kotlin, SQLite and Supabase are not on the list)
- Apple screenshot specifications: https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications
- Kotlin Multiplatform privacy manifest: https://kotlinlang.org/docs/multiplatform/multiplatform-privacy-manifest.html (16 May 2025)
- RevenueCat iOS privacy manifest: https://github.com/RevenueCat/purchases-ios/blob/main/Sources/PrivacyInfo.xcprivacy
- RevenueCat Play Data safety: https://www.revenuecat.com/docs/platform-resources/google-platform-resources/google-plays-data-safety
- RevenueCat Apple App Privacy: https://www.revenuecat.com/docs/platform-resources/apple-platform-resources/apple-app-privacy
- FTC COPPA FAQ: https://www.ftc.gov/business-guidance/resources/complying-coppa-frequently-asked-questions (July 10, 2026)
- GDPR Art. 8: https://gdpr-info.eu/art-8-gdpr/
