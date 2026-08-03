# Roadmap

Plan for monetizing DisplayTorch with a bottom ad banner plus a one-time
"remove ads" in-app purchase (~€1.99). Positioning for the store listing:
reading light / camping light / night light — the long-session use cases
that make banner impressions worthwhile — rather than the saturated
"flashlight" keyword space.

**Status:** Google's *test* banner (sample App ID + adaptive banner test
unit) is wired into the app for on-device evaluation. All real-money steps
below are open.

## 0. Evaluate the test banner (in progress)

- [ ] Use the app with the test banner for a few days of real scenarios
      (reading at night, camping, low brightness, red mode) and decide:
      ads yes/no. This is the cheap exit point.

## 1. Product decisions (me, ~an evening)

- [ ] Banner visibility rules — recommendation: hide in red mode.
- [ ] Remove-ads price (€1.99 suggested).
- [ ] Placement of the "Remove ads" entry point (edit-mode ⋮ menu suggested).

## 2. Accounts & paperwork (me, days of calendar time — start early)

- [ ] Google Play Console account ($25 one-time, identity verification).
      Personal accounts need **20 closed testers for 14 days** before a
      production release — this is the longest pole.
- [ ] AdMob account: payment + tax info, register app, create one real
      banner ad unit. App verification can take days before real ads serve.
- [ ] Privacy policy covering AdMob data collection, hosted at a public URL
      (GitHub Pages is fine). Required by both AdMob and Play.

## 3. Production-ready ads (code, ~half a day)

- [ ] Real App ID / ad unit via build config; debug builds keep using test
      IDs (clicking real ads during development risks an AdMob ban).
- [ ] UMP consent flow (required for EEA): request consent info on launch,
      show form if required, only then load ads.
- [ ] Implement the visibility rules from step 1.

## 4. Play Billing: remove-ads IAP (code, ~a day)

- [ ] Billing Library, non-consumable `remove_ads` product, purchase +
      acknowledge flow, restore-on-launch, cached entitlement flag.
- [ ] Guard: entitled users get no AdView, no MobileAds init, full-screen
      tap target — the app exactly as it is today.
- [ ] Mid-session purchase removes the banner immediately.
- [ ] Create the product in Play Console; test with license-tester accounts
      (requires step 2).

## 5. Release plumbing (~half a day)

- [ ] Create release keystore; wire the existing `DISPLAYTORCH_*` signing
      properties in `app/build.gradle.kts`.
- [ ] Smoke-test the minified release build (R8 with ads + billing SDKs).
- [ ] Play data-safety form matching AdMob's collection, ads declaration,
      store listing with the reading/camping-light positioning.

## 6. Closed testing → production (calendar time)

- [ ] 14-day closed test — doubles as end-to-end testing of consent, ads,
      and purchases on real devices.
- [ ] Promote to production.

**Estimated effort:** 2–3 days of actual work, 3–5 weeks of calendar time
(dominated by Play's tester requirement and account verifications).

## Previously planned, unrelated to monetization

- Onboarding tutorial after first install.
- Fastlane metadata.
