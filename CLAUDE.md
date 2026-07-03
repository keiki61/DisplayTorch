# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew assembleDebug
./gradlew assembleRelease          # minified + resource-shrunk (R8)

# Install to connected device
./gradlew installDebug
```

No test suites exist — the original stubs were removed. `./gradlew test` and `./gradlew connectedAndroidTest` will run but find nothing until tests are added.

## Architecture

DisplayTorch is a minimal single-activity Android app that uses the screen as a torch/flashlight. There is no navigation, no ViewModel, no dependency injection — just `MainActivity.kt` with XML layout.

**Core concept:** `brightnessLevels` is a `mutableListOf<BrightnessStep>` with 5 preset steps. Each step holds a `brightness` float (passed to `window.attributes.screenBrightness`) and color resources for white and red modes. The current index cycles on interaction; the individual step brightness values can be fine-tuned in edit mode and are persisted to `SharedPreferences`.

**Interaction model:**
| Gesture / Key | Normal mode | Edit mode |
|---|---|---|
| Single tap | Cycle to next brightness step | Exit edit mode |
| Two-finger tap | Toggle color (white ↔ red) | — |
| Long press | Enter edit mode | Exit edit mode |
| Volume up/down | Cycle brightness steps | Fine-adjust current step's brightness by ±`EDIT_BRIGHTNESS_STEP` (0.05) |

Edit mode also reveals a small `⋮` overflow-menu button (top-end corner) with a "Reset to defaults" action, guarded by a confirmation dialog, that restores all 5 brightness steps.

**Color modes:** White mode uses per-step shades (grey → greyWhite → white). Red mode always uses `R.color.red` (`#8B0000`), intended for preserving night vision.

**Debug overlay:** Gated on `BuildConfig.DEBUG` — debug builds show the current brightness % and background color hex in the center `TextView`. Release builds suppress it automatically.

## Key constraints

- Package / applicationId: `com.github.keiki.displaytorch`.
- `minSdk = 24` (Android 7.0), `compileSdk = targetSdk = 36`.
- AGP `9.2.0`, Kotlin `2.2.10`, JVM target 11.
- Release builds enable `isMinifyEnabled` and `isShrinkResources` — keep reflection-using code reachable via `proguard-rules.pro` if any is added.
- License: 0BSD (`LICENSE` at repo root).
- Planned: signing config for release, onboarding tutorial after first install, F-Droid and Google Play release, fastlane metadata.
