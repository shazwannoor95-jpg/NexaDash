# Saga Dash personal edition

Landscape dashboard inspired by the supplied reference: English UI, rotatable sedan preview,
GPS speed/trip, navigation shortcuts, a media panel and a JAKIM prayer strip below the clock.
The original MIT notice and third-party licenses remain applicable.

## Owner's target hardware

Recorded from the owner's head-unit screenshots and vehicle photos:

| Item | Reported or observed value |
| --- | --- |
| Head unit | TEST9278B |
| Firmware | TEST9278B_00020_V001; android-trunk-p0 |
| Android version shown in Settings | 12.0 (actual SDK/API level not yet queried) |
| Security patch shown | February 1, 2020 |
| Kernel shown | 4.14.116; build dated October 21, 2022 |
| CPU shown | A7, 1.5 GHz × 4 (confirm ABI before installing native APKs) |
| Memory/storage shown | 4 GB DDR / 32 GB flash |
| Display | 1024 × 600, LVDS, landscape — primary UI validation target |
| Car audio / MCU | F7.3.7 (X2) / 4.2 (X2) |
| CAN | 1.017, disconnected / no protocol at time of screenshot |
| Bluetooth AVRCP shown | 1.4 (default); does not establish Android MediaSession support |
| Vehicle | Red Proton Saga VVT, third-generation pre-facelift body shown in owner's photos |

Do not identify this unit as NWD K2401 or assume its package names, radio API or local ADB
behavior match the upstream author's hardware. Query the actual SDK level and CPU ABI on the
owner's selected device before choosing a TizenTube APK. The Android version label alone does
not verify these properties. Use the supplied car photos for red paint, body shape, silver
wheels, black window visors and exterior trim; exact year/trim has not been confirmed.

No live door, light or other CAN state is available from these screenshots. Keep vehicle
visualization independent of real telemetry until a supported connected provider is verified.
Bluetooth AVRCP can carry track metadata, but the firmware must also expose that data to the
dashboard through an Android session or a documented vendor interface. Lyrics still need a
separate source. The existing preview has not yet been validated on this physical head unit
or at its exact 1024 × 600 display configuration.

## Build

For Windows and Android Studio, follow the [beginner guide](WINDOWS_ANDROID_STUDIO_MS.md).
It uses the included Gradle wrapper, SDK 35 and JDK 17 or 21. No cloud-local paths or signing
keys need to be copied. Debug output: `app/build/outputs/apk/debug/app-debug.apk`.

In the prepared cloud machine:

```sh
source /workspace/.cloud-setup/env.sh
/workspace/.cloud-setup/build-klikdash
```

Output: `build/klikdash.apk`, labelled **Saga Dash**, application/package ID
**`com.shazwan.sagadash`**. Java sources and generated resources use the same namespace under
`src/com/shazwan/sagadash/`. The separate ID allows installation beside original KlikDash
(`com.ashafizullah.klikdash`) with independent Android UIDs, private app data, permissions and
launcher components. Installing Saga Dash does not migrate or replace the original app's data.
The existing local development signing key was preserved; a user-owned release key and its
backup will be explained separately. The upstream update action remains disabled in this edition.

The shell build defaults to this new ID. An explicit `PKG=<custom.id>` override is still supported;
install/compile/grant/HOME commands use the effective application ID and canonical Java activity.
This does not alter the signing key or upstream copyright/source links.

## What is implemented

- New dashboard as the Android HOME and launcher activity. Quran and dhikr activities and
  Quran playback service are not registered; their AI tools are removed from the advertised tool list.
  Legacy source files remain for shared-code compatibility; no Quran/dhikr UI is exposed.
- UI language fixed to English. AI speech settings remain independently configurable.
- Hand-modelled Saga VVT mesh based on the owner's red pre-facelift Saga photos and brochure
  proportions: curved body/cabin, real wheel-arch openings, split-spoke wheels, mirrors, visors,
  door seams/handles, shaped front/rear lamps and grille. No photo billboard is used.
  It remains a custom approximation, not manufacturer CAD or a photogrammetry scan.
- Studio lighting, contact shadow and precomputed vertex/normal buffers. Drag rotates/tilts,
  pinch zooms, and double-tap resets. Rendering is on-demand and paused when hidden.
  The Android plate label is now owner-requested NEXA.
- The bundled mesh is ready to use without Blender. To edit geometry, regenerate it using
  `python3 tools/build_saga_model.py`; optional Blender reviews use
  `blender --background --python tools/render_saga_preview.py -- assets/vehicle/saga-vvt.mesh <output>`.
- GPS speed with stale-signal indication; accuracy-filtered trip saved locally and reset by holding it.
- Media source selection, title, artist, embedded album art, play/pause/previous/next through Android
  MediaSession. Enable notification access in Dashboard Settings; no permission is silently granted
  in production. Unsupported vendor radio/Bluetooth sessions show an unavailable state.
- Optional LRCLIB lyrics (explicitly enabled in Settings). Sends only title and artist to `lrclib.net`;
  supports timed lyrics when available and when playback position is published. No Bluetooth lyrics
  protocol or OEM CAN/radio bridge is claimed. Plain lyrics can be opened by tapping the lyric panel.
- Auto prayer zone from fresh GPS, resolved entirely on-device against bundled Selangor,
  Kuala Lumpur and Putrajaya district polygons. Two consistent fixes at least one second apart
  confirm a change; duplicate, stale (>10 seconds), poor-accuracy (>50 metres) and ambiguous
  boundary fixes cannot silently switch zones. No nearest-city guessing or network geocoding.
- Official JAKIM annual schedules for SGR01, SGR02, SGR03 and WLY01 are included in the APK,
  covering **2026-01-01 through 2026-12-31**, so first-run travel across these zones works offline
  once GPS confirms location. GPS reception itself does not require Internet.
- Online refresh downloads annual data per zone and preserves the cache if a request fails.
  Refresh also prepares the other travel zones; a separate worker keeps media independent of
  slow prayer downloads. Automatic retries are throttled, with explicit refresh in Settings.
- Date parsing accepts actual JAKIM Malay month abbreviations, including Mac/Mei/Ogos/Okt/Dis,
  as well as English. Timetables and countdown use Asia/Kuala_Lumpur, regardless of device timezone.
- Sunrise is shown but excluded from prayer countdown/alerts. After Isha, the countdown uses the
  next cached day's Fajr. Missing/expired dates never reuse yesterday's times or Kemenag calculations.
- A manual override remains available for travel outside the bundled map, uncertain boundaries
  or GPS loss. The displayed status identifies a manual/last-confirmed zone; the map covers this
  user's travel region, not all Malaysia. District geometry is community sourced, not an official
  JAKIM boundary survey; provenance is included in `assets/prayer-zones-source.txt`.
- Prayer alerts and the AI prayer-times answer use the same active-zone JAKIM cache.
- Existing PIN mechanism, AI profiles and power saver remain accessible. App shortcuts use installed
  package lookup. Vendor-specific packages vary; the Apps list is the fallback.

## TizenTube Cobalt

Official source: https://github.com/reisxd/TizenTubeCobalt/releases/tag/v2.0.3

The ARM and ARM64 APKs were downloaded outside the checkout under `/workspace/apks/` and signatures
verified. To install on an explicitly selected connected ARM device:

```sh
source /workspace/.cloud-setup/env.sh
DEVICE=<head-unit-serial> tools/install_tizentube.sh
```

The helper checks ABI, pinned SHA-256 and APK signature before installation. This is a separate app;
its source and assets are not bundled in Saga Dash. Package: `io.gh.reisxd.tizentube.cobalt`.
It targets TV devices; touch support, playback and MediaSession interoperability need head-unit testing.
No physical head unit was connected during development, so it has not been installed on yours.

## Validation

Build first, then use a selected emulator/test device:

```sh
DEVICE=<test-device-serial> tests/run-android-checks.sh
```

The integration runner tests real Android startup, media metadata and play/pause with a synthetic
MediaSession, JAKIM response validation and timezone handling, and synced-lyric parsing. It grants
notification access on that selected test device; disable it afterwards if not wanted. Synthetic
prayer data is only parsed in memory, never written into the production cache.

## Remaining device and network checks

The saved network configuration includes `www.e-solat.gov.my` and `lrclib.net`. Official e-Solat
weekly and annual requests now work from the cloud. Four full annual responses (1,460 zone-days)
were validated and bundled. Live lyric lookup remains outside this prayer-zone change. Device
Internet is configured separately; GPS zoning and the bundled schedules do not depend on it.

Use the real head unit to verify radio/Bluetooth metadata and controls, GPS, audio focus, reverse
camera, steering buttons, sleep/wake and performance. This iteration does not invent weather,
vehicle telemetry, radio frequencies or song metadata when no provider is available.

### Current validation results

The signed personal APK built successfully and passed v2/v3 signature verification and zip alignment.
On the software Android 10 emulator, all **286 integration assertions passed**, including mesh geometry/drag/pinch/reset, application/Java/resource namespace migration,
offline GPS zoning, annual cache/date handling, real MediaSession metadata/control and confirmation
that Quran/dhikr activities are absent from the manifest. The earlier GPS suite also passed with
Wi-Fi/mobile data disabled and `Active default network: none`.

Both application IDs are installed on the test emulator with different UIDs and private data
folders. Installing the new ID left the original package's code path, version, installation
timestamps and data directory unchanged. The new application passed signature/alignment checks.
Native emulator display is 1024 × 600. Startup review found cache-lock contention in the
original-ID app's stored ANR traces and matching main-thread lock waits in the new app. Timetable
UI now reads published snapshots; refresh checks and alarm scheduling stay on the prayer worker.
Mesh decoding also runs off the GL thread, allowing an initial frame before the asset finishes.
A regression holds the cache monitor while requiring UI refresh to return within750ms.
The corrected suite passes163 assertions. An external force-stop/relaunch completed with
Status:ok in6,294ms and no subsequent app ANR event; Activity Manager labelled it WARM, so this
is not represented as a measured fully cold/head-unit startup time. The software emulator has
also exhibited separate System UI ANRs; physical device responsiveness remains unverified.

The first launch exposed a date-format error; that was fixed before the passing run. The emulator
also exhibited System UI ANRs independent of the app, so no smoothness/performance claim is made.
A clean screenshot was captured after dismissing the system dialog. Physical head-unit checks remain deferred at the owner’s request. Live official JAKIM requests
were subsequently verified during the GPS/offline implementation; lyric lookup remains unverified.

Current separate-ID APK: `/workspace/artifacts/saga-dash-final/saga-dash-independent.apk`.
Model review assets: `/workspace/artifacts/saga-model/` (five actual-mesh renders, GLB and Blender scene).
Generated mesh: 46,216 triangles in 17 opaque batches; ≈3.3MB (decimal). It is already included in the APK.
The original shared-ID preview is retained as an older artifact, not the current installation target.
Latest app screenshot: `/workspace/artifacts/saga-dash-final/dashboard-1024x600.png`.


### Updating the offline prayer data

Run `python3 tools/update_jakim_cache.py` to re-fetch all four official annual schedules, validate
zone/status, complete-year coverage, dates and time ordering, and update asset provenance/hashes.
The app also refreshes its own on-device cache online. An updated timetable is required when the
bundled/cached date range ends; offline support does not imply an unlimited future schedule.

GPS/offline checks use isolated test preferences and actual official bundled data. They cover the
user's Kuala Selangor → Shah Alam → Kuala Lumpur route, Klang, Petaling Jaya, Putrajaya, loss of GPS,
manual override, map holes/boundaries, all Malay months, cache expiry and midnight/Isha rollover.


### Verified GPS time and selective upstream fixes

Saga Dash now uses an app-local verified GPS clock for dashboard date/time, JAKIM selection,
countdown and prayer alerts. It keeps progressing with elapsed time through sleep/background;
it does not set the Android system clock. Open the app with GPS permission to acquire a fresh
satellite fix. Same-boot restoration requires an available boot counter, and verification expires
after24h without another accepted fix. The system/status bar and other apps can still show their
own device clock. No privileged local-ADB/system-time feature is assumed on TEST9278B.

Until time is verified, the clock is labelled unverified and prayer countdown/alerts wait.
Tap the clock (or Settings > Clock / GPS time) to explicitly confirm a correct device clock
if GPS is unavailable. A later device-time change revokes that manual confirmation; GPS-derived
time remains independent of wall-clock changes. A fresh GPS fix takes priority again.

Prayer alarms now use elapsed-time deadlines and guarded late delivery; notices expire using
elapsed time and are cleaned up after resume/wake. Source comparison and limitations are recorded
in UPSTREAM_REVIEW.md. All286 checks passed, including simulated wrong wall clocks, reboot,
deep-sleep time progression, expiry, outlier fixes and alert policy. Real head-unit behavior
remains deferred, and no emulator test has modified the actual Android system clock.

When GPS app time and Android device time differ, other apps and the Android TLS certificate
clock still depend on the device setting. Correct that device time through Android Settings
or a separately verified device-specific system route; this integration does not bypass TLS
verification or silently issue a privileged clock-setting command.

## Curved vehicle model update — 26 October 2026

The bonnet/deck shoulders now roll into the side panels, bumper corners curve in plan view,
and the cabin uses a continuous cubic roof profile. Tires and mirror housings have finer
curved surfaces; side glass has a slight bow. Front lamps sweep outward and the rear-quarter
lens follows the body rather than a floating flat patch. Paint highlights are broader, ambient
light is brighter and neutral, and the starting camera is lower and closer with more side visible. The asset remains
an original handmade approximation; it is not a manufacturer-accurate Saga model.
Rendering stays on-demand. Physical TEST9278B graphics performance remains untested.

To update a Windows ZIP checkout without replacing local IDE settings:

1. Stop the running app in Android Studio.
2. Download/extract the current `saga-dash-android-studio` branch ZIP to a **separate** folder.
3. Copy `assets/vehicle/saga-vvt.mesh` and `src/com/shazwan/sagadash/SagaView.java` from that
   folder into the same paths in your existing project, replacing those two files.
4. Click Run again. No uninstall, signing-key change or Gradle upgrade is needed.

For model source editing, also take `tools/build_saga_model.py`, the vehicle README and stats.
Do not replace `local.properties`, `.idea` or local keystores with files from another computer.

Actual-mesh studio previews (Blender lighting; Android uses its own simpler lighting):

![Curved model front](img/saga-curved-front.png)

![Curved model rear](img/saga-curved-rear.png)

Validation of this update: Gradle `:app:assembleDebug` passed; the final asset and viewer passed
286 assertions on the cloud Android 10 emulator, including geometry, rotate/pinch/reset and media
session controls. Five actual-mesh studio views were reviewed. This does not establish visual
accuracy to manufacturer CAD or frame-rate performance on the physical head unit.

Actual Android screenshot at 1024×600 (OpenGL lighting, not the studio render):

![Updated model in the Android dashboard](img/saga-curved-android.png)


## Local car-video preview and assigned shortcuts — 26 October 2026

See the [Windows video/shortcut guide](VIDEO_AND_SHORTCUTS_MS.md). Vehicle View can import a
local document-provider video into private device-protected storage (32MB, up to two minutes
and 4K; H.264 MP4 recommended). Metadata and a decoded frame are checked before atomic replacement.
The owner-requested video is now bundled; additional local imports stay private. This is a recorded clip with baked-in background,
not a reconstructed 3D model or free orbit. The interactive 3D option remains available.

Preview playback starts paused, is muted, does not loop, creates no MediaSession and requests no
audio focus. Its decoder is released when hidden/paused. Public media playback stays separate.
Dock shortcuts support persistent installed-activity assignments with long press and reset.
Video's automatic candidates are separate from YouTube/TizenTube. Missing OEM apps show an
explanatory chooser rather than silently redirecting to the generic Apps list. Apps lists installed
launchable apps; the sidebar spells out Saga Dash. No head-unit install or physical test performed.


Validation: Gradle debug build and 325 emulator assertions passed (including content-provider
imports, corrupted-file preservation, decoder prepare/play/pause/release/resume, shortcut assignment
and the existing prayer/model/media checks). The owner's original 1280×720 H.264 clip was also
selected through Android's real document picker and rendered successfully as a paused preview.
Only synthetic media is embedded in test fixtures; the separately bundled owner video is now included as requested.


## Bundled original video and NEXA — 26 October 2026

At the owner's request, `res/raw/saga_showroom.mp4` includes the uploaded video directly in the APK.
It is H.264 1280×720 at 24fps, ten seconds, 3,801,076 bytes. The audio track was removed by stream copy;
all 240 decoded video frames match the upload exactly. The baked-in NEXA plate and generated
background/annotations are retained. The 3D renderer also labels its front/rear plates NEXA.

New installs default to the paused video preview. Existing saved mode choices remain; select
Vehicle View > Video preview to use it. Imported private videos still take priority. MediaPlayer reads
the packaged resource FD directly, without first extracting it to private storage. No audio focus,
MediaSession or background playback is added. This video is not a reconstructed 3D mesh.

Validation of the bundled version: Gradle debug build passed; 336 emulator assertions passed,
including actual decode/play/pause/release from the APK resource with no imported file present,
and private-import override behavior. All 240 decoded frames match the original uploaded video.
