# Roadmap

Plan for monetizing DisplayTorch with a bottom ad banner plus a one-time
"remove ads" in-app purchase (€2.50). Positioning for the store listing:
reading light / camping light / night light — the long-session use cases
that make banner impressions worthwhile — rather than the saturated
"flashlight" keyword space.

**Status:** Test-banner evaluation is done — decision is to ship ads. All
real-money steps below (accounts, real AdMob IDs, billing, release) are
open.

## 0. Evaluate the test banner (done)

- [x] Use the app with the test banner for a few days of real scenarios
      (reading at night, camping, low brightness, red mode) and decide:
      ads yes/no. This is the cheap exit point. **Decision: ads yes — keep
      the banner.**

## 1. Product decisions (me, ~an evening)

- [x] Banner visibility rules — decided: always visible, including red
      mode (no `updateAdVisibility()`/hide-in-red-mode logic).
- [x] Remove-ads price: €2.50.
- [x] Placement of the "Remove ads" entry point — decided: edit-mode ⋮
      menu, alongside "Reset to defaults".

## 2. Accounts & paperwork (me, days of calendar time — start early)

- [x] Google Play Console account ($25 one-time, identity verification).
      Personal accounts need **20 closed testers for 14 days** before a
      production release — this is the longest pole; line up testers next
      so the 14-day clock can start as soon as a build is ready.
- [x] AdMob account: payment + tax info, register app, create one real
      banner ad unit. App verification can take days before real ads serve.
- [x] Privacy policy covering AdMob data collection, hosted at a public URL
      (GitHub Pages is fine). Required by both AdMob and Play.

## 3. Production-ready ads (code, ~half a day)

- [x] Real App ID / ad unit via build config; debug always uses test IDs,
      release reads `DISPLAYTORCH_ADMOB_APP_ID` /
      `DISPLAYTORCH_BANNER_AD_UNIT_ID` gradle properties (falls back to
      test IDs until the AdMob account exists — set both before release!).
- [x] UMP consent flow: consent info requested on launch, form shown if
      required, ads only load once `canRequestAds()`. End-to-end test with
      the real AdMob account's GDPR message is still open (needs step 2).
- [x] Banner always visible, including red mode (see step 1).

## 4. Play Billing: remove-ads IAP (code, ~a day)

- [x] Billing Library, non-consumable `remove_ads` product, purchase +
      acknowledge flow, restore-on-launch, cached entitlement flag. See
      `BillingManager.kt` (Billing Library 9, entitlement cached in
      `SharedPreferences`).
- [x] Guard: entitled users get no AdView, no MobileAds init, full-screen
      tap target — the app exactly as it is today. `setupAdBanner()` is
      skipped entirely when `BillingManager.adsRemoved` is true on launch.
- [x] Mid-session purchase removes the banner immediately, via the
      `onAdsRemoved` callback tearing down the `AdView`/`adContainer`.
- [x] Create the `remove_ads` product in Play Console and upload a build to
      a testing track.
- [ ] Test the purchase flow with a license-tester account (add tester under
      Setup → License testing, opt in via the track's testing link, install
      from the Play Store listing — not sideloaded — then buy via edit-mode
      ⋮ → "Remove ads").

## 5. Release plumbing (~half a day)

- [x] Create release keystore; wire the existing `DISPLAYTORCH_*` signing
      properties in `app/build.gradle.kts`.
- [x] Smoke-test the minified release build — R8 completed cleanly, all AdMob
      (`AdActivity`, `MobileAdsInitProvider`, `AdService`) and Billing
      (`ProxyBillingActivity`, `ProxyBillingActivityV2`) components present.
      Real AdMob App ID and banner unit ID from `gradle.properties` correctly
      substituted. APK 3.9 MB, AAB 6.5 MB. No ProGuard rules needed.
- [ ] Play data-safety form matching AdMob's and Play Billing's data
      collection, ads declaration.
- [x] Store listing text (fastlane metadata, en-US + de-DE) updated to
      reflect the ad-supported model and the "Remove ads" IAP — dropped the
      "no ads / no tracking" claims, added a free-with-ads section, and
      clarified that internet access is used only for the AdMob banner and
      the Play Billing purchase flow (the app itself does no tracking).
- [ ] Update Play Store screenshots — the existing screenshots show a
      full-screen display with no ad banner. The store listing must reflect
      the actual release experience: the bottom ad banner is visible in all
      paid builds, and the "Remove ads" option appears in the edit-mode ⋮
      menu. Screenshots taken with the test banner (test ID
      `ca-app-pub-3940256099942544/9214589741`) are fine for the listing;
      just don't use ad-free captures.

## 6. Closed testing → production (calendar time)

- [ ] 14-day closed test — doubles as end-to-end testing of consent, ads,
      and purchases on real devices.
- [ ] Promote to production.

**Estimated effort:** 2–3 days of actual work, 3–5 weeks of calendar time
(dominated by Play's tester requirement and account verifications).

## Previously planned, unrelated to monetization

- Onboarding tutorial after first install.
