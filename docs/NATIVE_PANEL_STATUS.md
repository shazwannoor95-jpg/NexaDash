# Native panel status — native5

The host follows actual panel surface proportions instead of stretching a fixed640x400 display. Longest raster edge capped800 pixels, density160, event-driven display resize and matching touch coordinates. Existing one-inline-player limit and explicit Start remain. App UI may reconfigure/recreate on resize; responsive layout is controlled by each app. No DiAuto integration or execution.

| Companion | Evidence | Limitations |
|---|---|---|
| Demo | Native5 API29 focused integration PASS16: cross-UID rendering, aspect/budget, touch, text and cleanup on both panels | Native5 physical head-unit behavior not verified |
| Patched V58 | UI previously rendered inside actual left dashboard panel | minAPI29; not installable on API27; lifecycle error observed; live playback unverified |
| Patched ARM TizenTube | Static/signature/ARM32 packaging checks | Rendering, touch, text and playback on ARM head unit unverified |
| Original Maps/Waze | No cooperative native companion delivered | Original apps not hosted by this Lab |

No third-party companion is certified perfect on TEST9278B.

## New uploaded navigation files

- Maps26.42.02.997557324: archive extracted successfully, APK304176297 bytes. Manifest minSDK29/target37; ARM32 included but Android8.1/API27 installation remains blocked. Maps car-projection opt-in is not a general embedding opt-in for normal MapsActivity. No APK minimum was lowered or dependencies bypassed.
- Waze5.9.5.4: supplied parts1–4 are each20MiB; archive advertises97380592-byte APKM and extraction fails before completion. Not analysed as a complete bundle, not patched/executed. Owner asked to skip pending Waze.

## Validation

Native5 shell release and Gradle debug/release assembly passed; test sources compile. Production fittedSize method executed on JVM for438x228,280x450,1024x600,3840x2160,300x300,120x800: aspect tolerance and max800 budget passed. After cold boot, Native5 focused native integration passed16 assertions on API29 x86_64. Actual display638x320 observed, replacing old fixed640x400. Full regression initially found two stale layout-test coordinates from the prior unequal panel geometry; these were updated. Full suite then failed after341 assertions on compatibility-report exact clipboard copy; this update is not a full-suite pass. Physical head-unit behavior and third-party app playback/navigation remain unverified.

Final native5 rebuilt APK also passed the focused16 assertions after adding the failed-resize launch guard.
