# Privacy Policy for Display Torch

**Effective date:** 2026-08-21

Display Torch is a simple flashlight app that uses your device's screen as a
light source. This policy explains, plainly, what the app does and does not
do with your data.

## Summary

Display Torch itself does **not** collect, store, or transmit any personal
data, and has no analytics or crash reporting. The app does show an
advertising banner supplied by Google AdMob, which requires internet access
and involves Google collecting some data to serve and measure ads. The app
also offers an optional one-time "Remove ads" purchase processed entirely by
Google Play Billing — details below.

## Permissions

Display Torch requests the **`INTERNET`** permission, used solely to request
and display ads through Google AdMob. It does not request access to your
camera, contacts, location, storage, or microphone. You can verify this
yourself in the app's [`AndroidManifest.xml`](https://github.com/keiki61/DisplayTorch/blob/main/app/src/main/AndroidManifest.xml).

## Data stored on your device

The app saves your 5 brightness-step settings (the values you fine-tune in
edit mode), your ad-consent choice recorded by Google's consent SDK, and
whether you've purchased "Remove ads" (checked against Google Play on each
launch, then cached), to a local `SharedPreferences` file on your device,
using Android's standard app-private storage. This data:

- Never leaves your device directly from the app.
- Is never transmitted to Display Torch's developer.
- Is deleted automatically if you uninstall the app.

You can review the exact code that reads and writes this data in
[`MainActivity.kt`](https://github.com/keiki61/DisplayTorch/blob/main/app/src/main/java/com/github/keiki/displaytorch/MainActivity.kt).

## Third-party services

Display Torch shows a banner ad using the **Google Mobile Ads SDK
(AdMob)**. To serve and measure ads, Google may collect data such as your
advertising ID, IP address, general device information, and ad interaction
data. This processing is done by Google, not by Display Torch's developer,
and is governed by:

- [Google's Privacy Policy](https://policies.google.com/privacy)
- [How Google uses information from sites or apps that use its services](https://support.google.com/admob/answer/6128543)

### Google Play Billing

The optional "Remove ads" purchase is processed entirely by **Google Play
Billing**. Display Torch never sees your payment details (card number, etc.);
Google Play handles the transaction and reports back a purchase token, which
the app uses only to confirm ("acknowledge") the purchase and unlock the
ad-free entitlement. This processing is governed by
[Google Play's Privacy Policy](https://policies.google.com/privacy) and
[Google Play's Terms of Service](https://play.google.com/about/play-terms/).

Display Torch does not integrate any other third-party SDK — no analytics
and no crash-reporting service.

### Consent (EEA, UK, and similar regions)

On first launch, the app uses Google's **User Messaging Platform (UMP)** SDK
to check whether a consent message is required for your region (for example
under GDPR) and shows one if so. Ads are only requested once this consent
flow has resolved. Your choice is stored locally via the UMP SDK, as noted
above.

## Children's privacy

Display Torch is a general-purpose utility app and is not directed at or
marketed to children. It does not knowingly collect personal information
from children. If you believe a child has provided personal information
through this app, please contact us using the details below so it can be
addressed.

## Open source

Display Torch is open source under the [0BSD license](https://github.com/keiki61/DisplayTorch/blob/main/LICENSE).
The entire source code is publicly available at
[github.com/keiki61/DisplayTorch](https://github.com/keiki61/DisplayTorch),
so these claims can be independently verified by anyone.

## Changes to this policy

If this policy ever changes — for example, if a future version of the app
adds a feature that requires a permission or network access — this page will
be updated and the "Effective date" above will change accordingly.

## Contact

Questions about this policy can be sent to displaytorch.app@gmail.com or raised as
an issue on the [GitHub repository](https://github.com/keiki61/DisplayTorch/issues).
