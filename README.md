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

## Build and run

- Android app: `./gradlew :androidApp:installDebug`, or run `androidApp` from Android Studio.
- iOS app: open [`iosApp/iosApp.xcodeproj`](iosApp) in Xcode and run on a simulator.
  Xcode builds the `Shared` framework through `:shared:embedAndSignAppleFrameworkForXcode`.
- All checks (unit tests on Android host and iOS simulator, Android lint):
  `./gradlew check`
