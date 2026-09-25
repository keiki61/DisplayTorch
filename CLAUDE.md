# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew assembleDebug
./gradlew assembleRelease          # minified + resource-shrunk (R8)

# Install to connected device
./gradlew installDebug

# Unit tests (JVM, no device needed)
./gradlew test
```

Unit tests cover `TorchState` only (`app/src/test`). There are no instrumented tests.

## Architecture

DisplayTorch is a single-activity Android app that uses the screen as a torch/flashlight. XML layout with ViewBinding, no navigation, no ViewModel, no dependency injection. Five classes:

- **`MainActivity.kt`** — glue only: gesture detection, window brightness, view updates, and wiring of the classes below. Holds a `TorchState`, a `BillingManager`, and an `AdsController` that stays `null` when the remove-ads entitlement is cached.
- **`TorchState.kt`** — pure Kotlin state: the 5 brightness steps (`DEFAULT_STEPS`), the selected step index, and the light colour (`LightColor`). Fine-tuning in edit mode (`EDIT_STEP` = 0.01 per key press) and reset persist through the `BrightnessStore` interface. `PreferencesBrightnessStore.kt` backs it with `SharedPreferences` (`brightness_prefs`, keys `brightness_<index>`). This is the class with unit tests; keep Android imports out of it.
- **`AdsController.kt`** — UMP consent flow, the privacy-options form, and the adaptive banner lifecycle inside the `adContainer`. `remove()` tears the banner down and blocks any later load, so a consent callback arriving after a restored purchase cannot bring it back. `isPrivacyOptionsRequired` drives the "Privacy options" menu entry.
- **`BillingManager.kt`** — Play Billing (Library 9) for the non-consumable `remove_ads` product. Entitlement is cached in `billing_prefs` so it is known synchronously at launch; Play re-verifies it on every connection. Reports `PurchaseEvent`s (`AdsRemoved`, `Pending`, `Cancelled`, `Unavailable`, `Failed`) that the activity turns into toasts.
- **`TorchTileService.kt`** — Quick Settings tile that launches the activity. The activity is `showWhenLocked`, so the tile works from the lock screen.

**Interaction model:**
| Gesture / Key | Normal mode | Edit mode |
|---|---|---|
| Single tap | Cycle to next brightness step | Exit edit mode |
| Two-finger tap | Toggle color (white ↔ red) | — |
| Long press | Enter edit mode | Exit edit mode |
| Volume up/down | Cycle brightness steps | Fine-adjust current step by ±`EDIT_STEP` |

Edit mode reveals a screen frame, a hint line, and a `⋮` menu (top-end) with: "How it works" (reopens the tutorial), "Reset to defaults" (confirmation dialog), "Remove ads" (hidden once bought), and "Privacy options" (only when UMP reports it required).

**Launch flow:** on first run the tutorial overlay is shown alone; consent and ads start only after it is dismissed (`startAdsIfEligible()`), on later runs directly from `onCreate`. Ads are never started when the entitlement is cached. While any dialog-like overlay is open (tutorial, ⋮ menu, reset dialog, consent or privacy form) the window brightness override is released so it is readable at step 1 (2%): `overlayOpened()` / `overlayClosed()` keep a depth counter and `applyCurrentStep()` only applies the step brightness at depth zero.

**Color modes:** White mode uses per-step shades (`whiteShades` in `MainActivity`: grey → greyWhite → white). Red mode always uses `R.color.red` (`#8B0000`), intended for preserving night vision.

**Debug overlay:** Gated on `BuildConfig.DEBUG` — debug builds show the current brightness % and background color hex in the center `TextView`. Release builds suppress it automatically.

## Key constraints

- Package / applicationId: `com.github.keiki.displaytorch`.
- `minSdk = 26` (Android 8.0), `compileSdk = targetSdk = 36`. Edge-to-edge is enforced by the target SDK; insets are applied manually in `MainActivity.setupInsets()` (including the ad container's bottom margin, which also keeps the edit-mode frame above the navigation bar when no banner exists).
- Accessibility: the root view's click / long-click listeners are the single source of the tap and hold actions (the gesture detector calls `performClick` / `performLongClick`), and the two-finger colour toggle is exposed as a custom accessibility action. Keep it that way so TalkBack can drive the app.
- AGP `9.3.1` with built-in Kotlin `2.3.20`, JVM target 11.
- Ad IDs: debug always uses Google's test IDs; release reads `DISPLAYTORCH_ADMOB_APP_ID` / `DISPLAYTORCH_BANNER_AD_UNIT_ID` Gradle properties. Signing reads `DISPLAYTORCH_STORE_FILE` etc.
- Release builds enable `isMinifyEnabled` and `isShrinkResources` — no ProGuard rules are needed today; add keep rules to `proguard-rules.pro` if reflection is ever introduced.
- Backup rules include only `brightness_prefs`; the entitlement and ad consent are deliberately not backed up.
- Keys in `SharedPreferences` (`brightness_<index>`, `seenOnboarding`, `ads_removed`) are used by existing installs; do not rename them.
- License: 0BSD (`LICENSE` at repo root).
- Planned work lives in `ROADMAP.md`.
