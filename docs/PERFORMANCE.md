# Saga Dash resource measurements

The current APK is `dist/SagaDash-1.6.7-sagadash.3.apk` (package `com.shazwan.sagadash`, code 27).
The car preview is eight image angles. Original video and live mesh classes/assets are removed.
Default Static + Reduce motion has no background animation; Ambient/Cinematic are opt-in, capped at
10fps and stopped while inactive. The wallpaper uses a bounded bitmap, without a custom GL renderer, blur or WebView.
Optional video/map work starts only when selected.

## Current validation (.3)

- Android Studio **debug and release assembly passed** with SDK 35/JDK 17, Gradle 8.9 and AGP 8.7.3.
- The distributable APK signature is verified; package/version are `com.shazwan.sagadash`, code **27**.
- **547 integration assertions passed** on the Android 10/API 29 software emulator. Added checks cover
  in-place Original/Synthwave changes, preserved geometry/car angle/zoom/media/prayer state, bounded
  RGB565 wallpaper, Static/Reduce motion, animation cancellation, compact controls, dock sizing,
  repeated Apps taps and decoded-frame notification gating.
- The frame-flow regression uses a decoded **64×64 synthetic fixture**, temporarily looping only
  inside the test and restoring the production no-loop setting. The bundled 1280×360 car clip still
  receives metadata/opening/prepare/start/pause/release checks. In this software emulator its decoder
  took roughly 0.8–1.0s per frame and dropped nearly all late frames; full-resolution smooth playback
  cannot be validated here. This is not a claim about the head unit's hardware decoder.
- No new Android permissions, UI framework, runtime blur or custom GL renderer was added for the theme.
- Physical TEST9278B behavior and total-device resource use remain unmeasured.

## Current runtime (.3)

Measured on **2026-10-27** using the code27 APK: fresh normal process after instrumentation, 45-second
warm-up, **Synthwave / Static / Reduce motion**, image angles, idle media, no map/GPS/media playback,
confirmed device clock and real cached manual SGR02 countdown. No forced GC was used.

| Process PSS | CPU, one virtual core | Guest interval | Custom GL threads | WebViews |
| ---: | ---: | ---: | ---: | ---: |
| **40.09 MiB** | **1.69%** | **20.69s** | **0** | **0** |

[Counter evidence](performance/synthwave-v3.json) · [Actual UI screenshot](img/saga-dash-synthwave.png).
CPU was sampled before the memory dump (35ticks at100Hz). This is one short software-emulator sample,
not a physical-head-unit measurement or total device load. Optional Ambient/Cinematic and external
YouTube/Maps/TizenTube have additional costs not covered by this Static sample. No Saga Dash
crash/ANR markers appeared in the reviewed retained logs. Startup and retained allocator pages vary;
comparisons with the older post-test process below do not establish a memory or CPU speedup.

## Validation checkpoint (.2)

- Android Studio/Gradle 8.9 + AGP 8.7.3: debug and release assembly passed with SDK 35/JDK 17.
- Release lint retains the intentional sideload-only target 29. Only GooglePlay's expired-target policy
  check is disabled; release lint otherwise runs. This APK is not a GooglePlay submission.
- The signed shell APK passed signature verification and 403 integration assertions on Android 10/API 29,
  x86_64 software emulation at 1024×600. Tests cover real MediaSession control, prayer/time/cache handling,
  image gestures/lifecycle, removed old resources, panel choices, layout geometry/persistence and video import.
- TizenTube helper syntax and API <24 rejection passed; the owner's head unit was not connected.
- No SagaDash FATAL/ANR marker was found in the reviewed final-run logcat. The cold software emulator
  had an unrelated SystemUI startup ANR; this is not counted as successful physical-device validation.

## Method and limits

Memory is process PSS from `dumpsys meminfo`, not Java heap, RSS or the `top` percent-memory column.
CPU is `(delta utime + delta stime) / 100 / delta guest uptime ×100` over a 20-second interval,
expressed as percent of **one virtual core**. The emulator has 2 virtual cores and 2GiB RAM; it uses
software CPU/GPU emulation. These numbers do not predict Cortex-A7 timing or the head unit's total load.

Measurements use the default image/native-media panels, static neon, no GPS, no map tiles, no actual
media playback and lyrics off. The active-countdown condition has confirmed-correct device time and
manual SGR02 with the real bundled 2026 JAKIM timetable. All app downloads/cache files remain separate
from the synthetic instrumentation fixtures. Startup and optional video/map/source-app work can cost more.

## Runtime checkpoint (.2)

Measured on 2026-10-26. Both samples have the real SGR02 countdown active and an idle media panel.
The .2 sample followed the integration suite and a normal warm launch with 45 seconds to settle.
No forced GC or allocator trimming was used.

| Active countdown sample | PSS | CPU, one virtual core | Guest interval |
| --- | ---: | ---: | ---: |
| Before countdown date-format optimization | 34.59 MiB | 2.64% | 20.87s |
| **1.6.7-sagadash.2 checkpoint** | **48.15 MiB** | **2.13%** | **20.69s** |

Counter evidence: [final sample](performance/final-countdown.json),
[before optimization](performance/before-countdown-optimization.json).
The CPU reading decreased in these short samples; they do not establish a guaranteed speedup.
The final PSS is higher: its native allocator reported about 17.2MiB free but retained pages.
Heap/GC/cache state differs after tests, so no memory improvement or memory-leak diagnosis is inferred.
The final result is reported as observed rather than substituting an earlier lower snapshot.

48.15MiB is approximately 1.18% of a **4GiB reference total**, by arithmetic only; the measured emulator
has 2GiB, and the physical head unit's available RAM was not measured. Additional source apps use more.
Both countdown samples had **zero custom GL threads and zero WebViews**. This covers the default image
mode; optional video playback deliberately creates its decoder/GLES resources.

Earlier image-mode startup/no-zone and countdown snapshots ranged from 33.81 to 36.48MiB; startup CPU
was temporarily higher. Those are earlier checkpoints, not the final APK's memory claim.
[Earlier .2 dashboard screenshot](img/saga-dash-final.png).

Upstream KlikDash's approximate 30MB / 0–1% CPU README statement is the author's result for that app/device,
not a SagaDash guarantee. A percentage of the advertised 4GB cannot substitute for a physical PSS sample.
Apps such as YouTube, Maps, Spotify and TizenTube consume additional memory/CPU in their own processes.

## Check on the actual head unit later

Use the native image mode first; verify installed apps, GPS/zone/countdown, Bluetooth/radio metadata,
reverse camera, steering keys and sleep/wake. Capture PSS/CPU after startup settles, then separately while
playing media and navigating. Keep the existing launcher available until these checks pass.
No firmware/MCU/CAN change is required to install SagaDash. Physical TEST9278B validation remains pending.
