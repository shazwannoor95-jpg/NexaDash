# Saga Dash installable APK

[Download SagaDash-1.6.7-sagadash.3.apk](SagaDash-1.6.7-sagadash.3.apk?raw=true)

- App: **Saga Dash**, `com.shazwan.sagadash`, version **1.6.7-sagadash.3**, code **27**.
- Android minimum **7.0 / API 24**; screen layout designed for **1024×600 landscape**.
- Signed using the existing cloud development certificate. Private signing keys are not included.
- Android Studio debug/release builds, APK signature verification and **547 integration assertions** pass.
  See [validation evidence](../docs/PERFORMANCE.md), including software-video and physical-unit limits.
- TEST9278B hardware, vendor Bluetooth/FM/DSP, reverse camera and steering controls have not been tested on the physical unit.

Follow the [head unit / Windows signing guide](../docs/HEAD_UNIT_INSTALL_MS.md).
A Windows-built APK generally uses a different signing key; keep one key for future updates.
The current Static Synthwave active-countdown sample measured **40.09MiB PSS / 1.69% of one virtual
CPU core**. See [method and limits](../docs/PERFORMANCE.md). Physical-unit performance remains unmeasured.
Checksums and public certificate identity are in [SHA256SUMS](SHA256SUMS) and [build-info.json](build-info.json).

Default vehicle preview uses eight rendered image angles with **NEXA**, no idle animation or 3D renderer.
Some angles were generated from the supplied car references; this is an image viewer, not a reconstructed 3D model.
Optional transparent/imported video is prepared only on demand and released when hidden.
Panel choices use native controls / GPS map preview; routing and full video playback open their original apps.
Synthwave/Original appearance preserves layout and actions. Static + Reduce motion is the default;
optional Ambient/Cinematic light effects are capped at 10fps and stop offscreen.

The older .2 APK is retained for existing download links; the current build is .3.
