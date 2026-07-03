# Privacy Policy for Display Torch

**Effective date:** 2026-07-03

Display Torch is a simple flashlight app that uses your device's screen as a
light source. This policy explains, plainly, what the app does and does not
do with your data.

## Summary

Display Torch does **not** collect, store, or transmit any personal data.
It has no analytics, no advertising, no crash reporting, and no network
access of any kind.

## Permissions

Display Torch requests **zero Android permissions**. It cannot access your
camera, contacts, location, storage, microphone, or the internet, because it
never asks for that access in the first place. You can verify this yourself
in the app's [`AndroidManifest.xml`](https://github.com/keiki61/DisplayTorch/blob/main/app/src/main/AndroidManifest.xml).

## Data stored on your device

The app saves your 5 brightness-step settings (the values you fine-tune in
edit mode) to a local `SharedPreferences` file on your device, using
Android's standard app-private storage. This data:

- Never leaves your device.
- Is never transmitted to Display Torch's developer or to any third party.
- Is deleted automatically if you uninstall the app.

You can review the exact code that reads and writes this data in
[`MainActivity.kt`](https://github.com/keiki61/DisplayTorch/blob/main/app/src/main/java/com/github/keiki/displaytorch/MainActivity.kt).

## Third-party services

Display Torch does not integrate with any third-party SDKs, analytics
platforms, advertising networks, or crash-reporting services.

## Children's privacy

Since the app collects no data from anyone, it does not knowingly collect
data from children, and none of the concerns Children's privacy laws (such
as COPPA) are designed to address apply here.

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

Questions about this policy can be sent to otakun85@gmail.com or raised as
an issue on the [GitHub repository](https://github.com/keiki61/DisplayTorch/issues).
