# Saga Dash personal edition

English landscape launcher for the owner's red Proton Saga VVT. It keeps the supplied dashboard's
default positions, a JAKIM prayer strip below the clock, two configurable native panels, app shortcuts
and a configurable Synthwave appearance. Original MIT and third-party notices remain applicable.

## Current car display

The default **Saga image angles (lightweight)** uses eight rendered images derived from the supplied
Saga references, with **NEXA**. Drag selects the next/previous angle and wraps around; pinch zooms;
double-tap resets. These are eight image angles, not true 3D or a free camera. Generated details can
differ from the physical car. The atlas `res/drawable-nodpi/saga_angles.png` is sampled on decode.

There is no automatic image animation, mesh, GLES renderer or video decoder in the default mode.
The old **Original Saga video**, **Interactive 3D model**, `SagaView`/`SagaMesh` and bundled mesh are
removed from the app. Optional **Transparent Saga video** and local video imports remain available;
video follows a recording, starts paused and is muted. No video MediaSession or audio-focus request
is added, and the decoder is released when the panel/app is left.

## Glass panels and Apps grid (.5)

Existing panel positions, wallpaper, prayer strip, car view and source choices remain unchanged.
Glass panels add static vertical tint/reflection gradients and a fine rim. Settings > Appearance >
**Glass panels** restores the preceding finish when switched off. No live blur, extra wallpaper
copy, render loop or new Android permission is used. Original retains its opaque fill.

The sidebar **Apps** opens a scrollable native grid of actual installed app icons and names.
Columns adapt to width/density; cells are reused. Icon rasters are at most96×96pixels and the
LRU cache is capped at1MiB (visible cell references are additional). Icons load on the existing
worker when requested and are released when the drawer closes. Missing icons use a placeholder.
The same safe exported/enabled launcher-activity discovery and duplicate-label descriptions remain.

## App-panel compatibility (.4)

Settings > **App panel compatibility** collects a read-only report on demand: Android API/ABI,
display/features, existing host permissions and relevant app activity flags. Copy or screenshot it
for device-specific review. No root commands, app launches, settings changes or automatic uploads
are performed. This does **not** add embedding; declared PiP/freeform or opt-in flags alone do not
establish a working host. See [the beginner guide](APP_PANELS_MS.md).

## Panels and editing

- Left panel: Car preview, Navigation preview, YouTube controls, TizenTube controls, Media player or Radio controls.
- Right panel: Media player, Navigation preview, Radio controls, YouTube controls or TizenTube controls.
- Native media panels share one Android media-session observer. Actual playback stays in the source app.
  Metadata, cover, previous/play-pause/next and optional lyrics depend on what the source publishes.
- YouTube/TizenTube/Radio panels offer source controls and an Open action. They do not embed arbitrary
  third-party activities or WebViews. A normal launcher cannot force Maps/Waze/TizenTube into its panel.
- Navigation is a native GPS map preview with OpenStreetMap attribution and an action opening the
  selected navigation app. It is not a turn-by-turn engine or complete offline map.
- Edit layout moves/resizes panels; a separate dock mode reorders icons and changes icon size.
  Save, cancel and reset-to-default are available. Header/prayer/footer remain in their normal positions.
  Requested icon sizes are capped to their available dock cells. Short source panels compact their
  headings/metadata while keeping launch, chooser and playback buttons accessible.
- Settings > Appearance offers Original/Synthwave, brightness, transparency, neon intensity,
  Static/Ambient/Cinematic and Reduce motion. Static with Reduce motion is the default and has no
  background animation timer. Optional light effects are capped at 10fps and stop when inactive.
  Wallpaper uses one bounded bitmap and native draws, without blur, a custom GL renderer or WebView; the car is not animated.

See [customization instructions](DASHBOARD_CUSTOMIZATION_MS.md),
[Synthwave appearance](SYNTHWAVE_MS.md),
[car images/video and shortcuts](VIDEO_AND_SHORTCUTS_MS.md), and
[transparent-video format notes](TRANSPARENT_VIDEO.md).

## JAKIM and trusted time

Official annual 2026 schedules are bundled for SGR01, SGR02, SGR03 and WLY01: 1,460 zone-days,
2026-01-01 through 2026-12-31. GPS zone selection uses offline Selangor/Kuala Lumpur/Putrajaya
district polygons, fresh accurate fixes and boundary uncertainty checks. It does not guess the nearest
city. Manual zone selection is available, including when outside mapped coverage or GPS is absent.
Community geometry provenance is retained; it is not represented as an official surveyed JAKIM boundary.

The app uses Asia/Kuala_Lumpur time. Countdown/prayer alerts wait for verified GPS time or explicit
confirmation of a correct device clock. The app-local GPS clock progresses through elapsed time and
sleep; it does not set the Android system clock. Verification expires without a new accepted fix,
and device-time edits revoke manual confirmation. Other apps/status bar/TLS still use system time.

Sunrise is displayed but excluded from the prayer countdown/alerts. Missing or expired dates do not
reuse yesterday's timetable. Online refresh keeps the previous cache on failure. Updated data is
required when coverage ends; offline support is not an unlimited future calendar.

Refresh bundled schedules with `tools/update_jakim_cache.py`. Details and provenance are in
`assets/jakim` and `assets/prayer-zones-source.txt`. Selective upstream clock/notice changes are
recorded in [UPSTREAM_REVIEW.md](UPSTREAM_REVIEW.md).

## Media and native head-unit apps

Saga Dash reads public Android MediaSessions after the owner grants notification access. Vendor
radio/Bluetooth metadata may not be exposed that way; AVRCP alone does not establish support.
Optional LRCLIB lyrics send track title/artist only after explicit enabling. Synced lyrics require
available timestamps and playback position. Missing provider data is shown as unavailable.

Shortcuts preserve an explicitly selected installed activity. Automatic discovery uses known packages
and matching labels such as BT, CarSettings/CarSet, Music, Video/VLC and ZLINK, with a chooser when
ambiguous. Screenshot labels do not establish package IDs. CarSet is not inferred to be DSP.
Radio/BT/DSP/CAN behavior still needs testing with the actual unit and vendor apps.

TizenTube is separate from Saga Dash. The optional helper targets
[TizenTube Cobalt v2.0.3](https://github.com/reisxd/TizenTubeCobalt/releases/tag/v2.0.3), checks the chosen
device's ABI, pinned APK hash and signature, and requires an explicitly selected device. It has not
been installed or verified on the owner's head unit. Touch/playback/session interoperability is unverified.

## Target hardware

These values were read from the owner's screenshots, including the .4 on-device compatibility report; no remote hardware access:

| Item | Reported/observed |
| --- | --- |
| Head unit / firmware | TEST9278B / TEST9278B_00020_V001; android-trunk-p0 |
| Android / ABI | Earlier menu label12.0; on-device .4 report: version string9.1, SDK API27 (Android8.1), armeabi-v7a/armeabi |
| CPU | Cortex-A7, 1.5GHz × 4 |
| RAM / flash | 4GB / 32GB |
| Display | 1024×600 LVDS landscape |
| MCU / car audio | 4.2(X2) / F7.3.7(X2) |
| CAN | 1.017, disconnected/no protocol in screenshot |
| AVRCP label | 1.4(default) |
| Vehicle | Red third-generation pre-facelift Proton Saga VVT |

Do not assume NWD K2401 behavior, local ADB access, radio APIs or live CAN state on this unit.
The physical car's exact year/trim is not established by the supplied references.

## Resource policy and practical limits

Default car images and wallpaper are static and sampled; optional decoder/GPU resources are demand-created and
released. Clock/date formatters are reused and unchanged text is not replaced each second. Media
cover images are capped at 128px. The native map shares a 3MiB memory cache, 12MiB disk cache and one
worker; hidden maps do not download tiles and no tile download starts without GPS.

The upstream KlikDash README's approximate 30MB statement belongs to that author's app/device.
It is not a Saga Dash measurement or guaranteed percentage. PSS and CPU measurements need their
mode, device, duration and separately running apps identified. Emulator timing cannot establish
Cortex-A7 performance. No guarantee of zero lag/stutter/crash or total head-unit RAM use is made.

## Build and delivery

Application ID/Java namespace: **com.shazwan.sagadash**; label: **Saga Dash**. It installs beside
upstream `com.ashafizullah.klikdash` with separate data and permissions. Quran/dhikr activities and
playback service are not registered; English UI is fixed. Upstream auto-update is disabled.

Use a **fresh project ZIP/folder** for this update so obsolete classes/assets are not retained.
See [Android Studio Windows](WINDOWS_ANDROID_STUDIO_MS.md). SDK 35, JDK 17/21, Gradle 8.9 and AGP 8.7.3
are the configured tools; the SDK used for compiling is separate from the head unit's Android version.
No Blender, image-generation service or ONNX runtime is required to build the already packaged assets.

For a ready signed APK or a locally signed Windows build, follow
[HEAD_UNIT_INSTALL_MS.md](HEAD_UNIT_INSTALL_MS.md). Cloud signing keys are not published. A Windows
key cannot update a cloud-key APK with the same package ID unless the certificates match.

In the prepared cloud workspace, the shell build remains available:

```sh
source /workspace/.cloud-setup/env.sh
/workspace/.cloud-setup/build-klikdash
```

Emulator checks use an explicitly selected test device via `tests/run-android-checks.sh`. Its synthetic
fixtures are separate from production data. The current **1.6.7-sagadash.5/code29** build passed
**626 integration assertions**, including the native app grid, bounded icon raster/cache release, glass finish state preservation,
the read-only compatibility report, actual clipboard copy,
theme state/lifecycle and compact layout regressions.
See [performance and current validation status](PERFORMANCE.md).
Physical head-unit checks—GPS, BT/radio metadata, steering keys, reverse camera, audio focus, sleep/wake
and resource behavior—remain outstanding until that unit is tested.
