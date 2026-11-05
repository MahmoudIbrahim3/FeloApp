# ClipKid

Curated, Arabic-first (RTL) video app for children aged 2–12, built with Kotlin Multiplatform and
Compose Multiplatform for Android and iOS. Product rules, stack and architecture live in
[CLAUDE.md](CLAUDE.md). Read it before changing anything.

## Design references

The reference screens are in [`design/`](design). They are exported at 2x:

- Phone: 780×1688 px = 390×844 dp (Onboard, SetPin, ChildSetup, SetupCollections, HomeFeed,
  Collection, Clips (player), UpNext, DoneToday, Favorites, Gate, Parent, Activity, Paywall).
- Tablet: `HomeTablet.png`, 2560×1600 px = 1280×800 dp, landscape.

Divide pixel sizes by 2 to get dp. When code and an image disagree, ask before changing either.

## Modules

| Module | Purpose |
| --- | --- |
| `shared` | App shell: `App()`, root navigation, Koin start, iOS framework `Shared` |
| `androidApp` | Android application (`ClipKidApplication`, `MainActivity`) hosting `App()` |
| `iosApp` | Thin Xcode / SwiftUI host |
| `core:model` | Pure Kotlin data classes |
| `core:domain` | Repository interfaces and use cases |
| `core:data` | Room, DataStore, Supabase, repository implementations, fakes |
| `core:designsystem` | Tokens, fonts, icons and components (`ClipKidTheme`, `VideoCard`, `PinPad`, …) |
| `core:player` | expect/actual YouTube embedded player |
| `core:billing` | RevenueCat wrapper behind an interface |
| `feature:onboarding`, `feature:child`, `feature:parent` | Screens; depend only on core modules |

Gradle conventions live in [`build-logic`](build-logic):

- `clipkid.kmp.library`: Android (`com.android.kotlin.multiplatform.library`) + `iosArm64` +
  `iosSimulatorArm64`, SDK levels from `gradle/libs.versions.toml`, commonTest stack
  (kotlin.test, coroutines-test, Turbine).
- `clipkid.kmp.compose`: the above + Compose Multiplatform, Compose resources
  (module-private `Res` in `<module package>.resources`) and `@Preview`.

All versions are in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Design system

`core:designsystem` holds the ClipKid look:

- `ClipKidTheme` provides colors, typography (Baloo Bhaijaan 2 + Cairo, bundled), shapes,
  spacing and sizes. Touch targets: ≥ 48dp everywhere, ≥ 56dp for primary child actions.
- `ProvideRtl` forces right-to-left. `App()` applies it once at the root.
- `withArabicDigits()`, `toArabicDigits()` and `formatDuration()` format every number with
  Arabic-Indic digits.
- Every component file ends with a public `XxxShowcase()` used by its `@Preview` and by the
  temporary `ComponentGallery` (the current start destination).

Font licenses (OFL) and the Lucide icon license (ISC) are in `core/designsystem/licenses`.

## Domain and data

- `core:domain` holds the product rules as pure use cases (`GetHomeFeedUseCase`,
  `GetDailyQuotaUseCase`, `StartWatchUseCase`, `GetUpNextUseCase`, …). Every one takes a
  `kotlin.time.Clock` and a `TimeZone`; "today" is the local calendar day and Flows re-emit at
  local midnight. `VisibleCatalog` is the single place for age band / allowed / hidden filtering.
- `core:data` implements the repositories with Room 3 (bundled SQLite, schema in
  `core/data/schemas`), DataStore Preferences and a `SecureStore` (Android Keystore / iOS
  Keychain) holding only a PBKDF2 hash of the parent PIN.
- The catalog currently comes from `FakeCatalogDataSource` (`DataConfig.useFakeCatalog = true`).
  Its YouTube ids are placeholders: search for `REPLACE_` to swap in real, reviewed videos.
- Tests: `core/domain/src/commonTest` (use cases with hand-written fakes),
  `core/data/src/commonTest` and `core/data/src/iosTest` (Room repositories on a real in-memory
  database; they need the iOS simulator).

## Subscriptions (RevenueCat)

`core:billing` implements `BillingRepository` with RevenueCat (`purchases-kmp-core`). Free =
the collections with `is_premium = false` in Supabase + 1 child; the `full_access` entitlement
opens premium collections and up to 4 children. Expiry keeps every setting and all history; it
only locks content again (and children after the first).

The app only knows these names, so product ids can change freely in the stores:

| RevenueCat | Value |
| --- | --- |
| Entitlement | `full_access` |
| Offering (Current) | `default` |
| Packages | `$rc_monthly`, `$rc_annual` |

Products used in the consoles: App Store `clipkid_full_monthly` / `clipkid_full_annual` (one
subscription group, annual = level 1, intro offer: free 1 week); Google Play subscription
`clipkid_full` with base plans `monthly` / `annual`, each with a 7-day free-trial offer
(new customers only).

Keys go in `local.properties` (gitignored) or CI env, never in git:

```properties
revenuecat.androidApiKey=goog_...     # CLIPKID_RC_ANDROID_KEY
revenuecat.iosApiKey=appl_...         # CLIPKID_RC_IOS_KEY
revenuecat.testStoreApiKey=test_...   # CLIPKID_RC_TEST_KEY, optional, debug builds only
```

- No key: billing is off (`NoStoreBillingRepository`): everything premium stays locked and the
  paywall says the subscription is not available. Tests and previews always run this way.
- The Test Store key wins in debug builds. Remove it to test against the real sandboxes.
- Privacy: anonymous RevenueCat user only; no attributes, device ids or integrations; store
  in-app messages and App Store promoted purchases are off (every purchase starts from the
  paywall, behind the PIN).

Sandbox testing:

- Android: upload a build to the Internal testing track, add your Gmail as a license tester,
  install from the testing link. Renewals are accelerated (a month ≈ 5 minutes).
- iOS: real device, Settings → Developer → Sandbox Apple Account. Manage or expire sandbox
  subscriptions there.
- Check: buy annual with the trial (content unlocks at once), restore after a reinstall,
  cancel and wait for expiry (content locks again; settings and history stay).

## Build and run

- Android app: `./gradlew :androidApp:installDebug`, or run `androidApp` from Android Studio.
- iOS app: open [`iosApp/iosApp.xcodeproj`](iosApp) in Xcode and run on a simulator.
  Xcode builds the `Shared` framework through `:shared:embedAndSignAppleFrameworkForXcode`.
- All checks (unit tests on Android host and iOS simulator, Android lint):
  `./gradlew check`
- iOS simulator tests only: `./gradlew iosSimulatorArm64Test` (macOS only).
