# NexaDash: complete continuation guide for Claude Code

Handoff date:31 October2026 (Malaysia). Repository: https://github.com/shazwannoor95-jpg/NexaDash . This guide describes facts and remaining work, not a completed hardware certification.

## Start here

- Public source: `main`. Latest APK/code release: commit `835cf68`, **Next1.9 /1.9-next10 /versionCode10**. Android Studio identity: **1.6.7-nexadash.8-native20 /code52**.
- Public APK: `dist/next/NexaDash-Next-1.9.apk`; size3066694 bytes; SHA256 `ed5692933b4a6371584e5015724cf2e96b2019bbe7ccb07ce8e2ecfe3fbb3d00`.
- Check `dist/next/artifact.json` and `dist/build-info.json` for the current downloadable artifact, signer and feature status. Handoff/document-only commits do not imply a new APK.
- Source aliases `saga-dash-android-studio` (default) and `nexadash-native-panel-experiment` were synchronized with main at1.9. Use main for continued development.
- `nexadash-maps-ui-wip` contains earlier failed sizing/focus experiments; do not merge it blindly. `nexadash-navigation-next19` contains the delivered1.9 work. Other companion branches are separate packages, not newer launcher releases.

## Owner and physical device

Owner communicates in informal Malay. Wants an Android Auto-like dashboard: two apps usable in left/right panels, navigation in one and media in another. No routing/hosting through DiAuto. ADB on the physical unit has been difficult/refused; do not require it without checking available access. Root and Shizuku integration have **not** been established.

Device: **alps TEST9278B**, actual **API27**, **armeabi-v7a/armeabi ARM32**, display **1024x600 @160dpi**, around **1GB RAM**. Firmware reports Android9.1, but actual SDK27 determines compatibility. Advertises secondary-display activities and PiP; freeform false. NexaDash is not a system app and has no privileged task/input/trusted-display permissions.

Google Maps from the owner's archive is **25.32.00.790560726**, minAPI26, with base+ARM32+en+mdpi splits. Later supplied Maps26.42/26.43 minimumAPI29 are unsuitable for this device. Waze5.9.5.4 multipart archive was incomplete; owner said skip Waze for now.

## Latest physical findings: do not lose these

1. TizenTube Next video advances in the left panel in owner recordings. This does not certify all controls, keyboard, audio focus or long sessions.
2. Maps companion works outside the panel per owner, with some lag. Inside, map/search remains unusable/blank even when Maps is the only active app.
3. Next1.9 larger canvas did **not** resolve the reported Maps problem: latest owner response is "masih sama tk berhasil".
4. Owner photos show Next1.9 on API27 and confirmed requested/proxy/companion display IDs: left8/8/8 and right6/6/6, raster707x480@160dpi. These are historical attempts per panel, not proof of concurrent duplicate Maps sessions. A search screen is visible behind the report; no successful geocoding/tile display was demonstrated.
5. Active WebView provider in the latest report: `com.android.chrome`138.0.7204.179. Do not infer the active engine from an old exported WebView stub63.
6. Missing INJECT_EVENTS/ACTIVITY_EMBEDDING/etc is not alone a diagnosis: cooperative companion input uses its own signed bridge. A positive display ID is not proof of useful rendered content.
7. Owner has not yet confirmed FM sound/tuning on hardware. Do not change that to verified based on emulator tests.
8. The separate native NexaDash map's live search/routing/voice results on this head unit are not established by these photos or the latest Maps-companion failure.

Do not keep asking for the same compatibility report. The supplied report establishes startup IDs/size, not a root cause. Further observations should distinguish native Maps renderer/content/lifecycle, focus/input, permission/network and viewport layout using concrete evidence. Do not guess that RAM, Google identity, WebView or missing privileged permissions alone caused it.

## What is implemented

### Native NexaDash map (Next1.9)

Panel selector **Map · search / route** uses native Canvas/widgets, not Google Maps and not WebView. Photon destination search, OSRM driving routes, OSM tiles, native next-turn/estimated-time display, pan/zoom, GPS-follow, overview, manual recalculation and opt-in Android English TTS. Requires Internet for search/routes, fresh GPS for departure and installed Android voice data for speech. No live traffic, automatic rerouting or offline routing.

Route responses bounded2MiB/10000 points/2000 instructions. Cancel/generation checks prevent pending results returning after Clear/pause/replacement. Existing tile cache is shared/bounded. Geo progress uses projection onto segments rather than nearest maneuver. Physical route quality and audio remain unverified. Cloud attempted Photon/OSRM once for public KL landmarks; network proxy returned403, so only fixture response flows were tested here.

### Google Maps companion

Separate package `com.shazwan.nexadash.maps.next`, version25.32.00.790560726-next1. Original system Maps remains `com.google.android.apps.maps`. Original nine DEX/ten ARM32 libraries retained; bridge adds classes10.dex. Renaming/shared-UID/provider/permission isolation is documented in `tools/maps-native/README.md`.

Next1.9 adds Maps-only Spacious/Compact, up to800px longest edge,480px short edge when aspect/cap allow, fixed160dpi, preserved aspect/touch mapping, isolated stop/restart on layout selection, saved choice per panel and companion location-settings link. No new Maps APK was built for1.9. Actual Google Maps still fails on the head unit.

### Two native panels

Cooperative signed companions only. Separate display/token/service/input worker per panel, explicit Start, duplicate package rejected, display confirmation before input. Two distinct companions allowed; web/file decoder modes remain exclusive with native/web/file. Stop/background releases displays/input/bindings but does not force-stop the app or promise immediate RAM reclamation.

### FM antenna radio (Next1.8)

Native NexaDash UI sends explicit Intents to existing exported OEM services. Original Radio Activity is not embedded. No Radio companion, root or ADB required by this client, but matching installed services/hardware remain necessary.

- Internal `com.android.fmradio/com.android.fmradio.FmtestService`, version3.1: flag0 opens/factory-initializes; wait complete `android.intent.action.fmradioseekok` with `freq:int[]` in10kHz units; then flag1 with `freq:float` MHz. Partial `freqone` is not ready. flag2 closes. Initialization may block/delay queued Stop.
- External `com.acloud.stub.extradio/com.radio.service.RadioService`, version2.3.24: action `com.android.xygala.ACTION_CONTROL_RADIO`; method_open, method_setFreq with param_freq:int kHz, method_close. Service-reported on does not prove JNI/audio works.
- Auto follows `persist.sys.xy.proinfo.exfm` through `DashRadioBackend`; manual source available. Never call stopService: OEM onDestroy may kill its own process. Keep existing band/area/sensitivity; no invented MCU commands.

### Debloat

`config/debloat/test9278b-review.json` is **review-only**, not an autodebloat APK. Candidates Calendar Sync/VLC only if unused and exact installed identities confirmed. Preserve OEM radio/audio/BT/MCU/CanBus/CarSet, location, core Android, active WebView/donor, GMS for Maps, current TTS engine and original launcher fallback. APK archives do not establish running memory consumption. Execution requires working authorized shell/root; a normal APK does not acquire privileges by wrapping CleanYourTV scripts. Restore must restore prior states, not blindly enable everything.

## Source map

| Path | Purpose |
|---|---|
| `src/com/shazwan/sagadash/DashboardActivity.java` | Panel modes, dock, lifecycle, permission/settings UI |
| `DashNavigationView.java` in same directory | Native map, tiles, search/route dialogs/worker/TTS |
| `DashRouteProgress.java` | Pure route projection, distances and next-step geometry |
| `DashNativeFrame.java` | Cooperative native display/session/input/size/keyboard tools |
| `DashNativeGeometry.java` | Bounded aspect-preserving raster sizing |
| `DashNativeProxy.java` | Display-placed proxy launching companion |
| `DashPanelCompatibility.java` | Read-only report, latest startup metadata |
| `DashFmController.java`, `DashFmPanel.java`, `DashFmBand.java` | OEM FM client/UI/units |
| `DashRadioBackend.java`, `DashRadioRuntime.java`, `DashRadioFiles.java` | FM selection/report/bounded fixed-file export |
| `tools/v58-panel-lab/bridge-src/com/shazwan/nexapanel/` | Shared companion Init/Bridge source; per-display input and text |
| `tools/maps-native/` | Pinned APKEditor split merge/manifest patch/verifier |
| `tools/tizentube-next/` | Next-compatible TizenTube build |
| `tools/next/` | Public Next manifest/build identity |
| `tools/native-test/`, `tools/radio-fixture/` | Test apps; not real Maps/tuner implementations |
| `res/drawable-nodpi/saga_angles.png` | Owner-restored original8-view plated car atlas |

## Build from a fresh clone

Do not depend on a prior chat workspace existing. Required: Bash, Python3, zip/unzip, **JDK17**, Android **platform35** and **build-tools36.0.0**; platform-tools/emulator for Android tests. Standard Gradle build also needs its documented dependencies available through repositories or cache.

```bash
git clone --single-branch --branch main https://github.com/shazwannoor95-jpg/NexaDash.git
cd NexaDash
git switch main
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
# Create local.properties with sdk.dir if Android Studio/Gradle needs it.
./gradlew :app:assembleDebug :app:assembleRelease
```

Android Studio: open the folder containing settings.gradle; select module **app**, not panelprobe. Studio defaults to **com.shazwan.nexadash.lab** and a local debug signer. That is not the same identity/key as the GitHub Next APK.

Public Next build, with private keystore alias **webtest**:

```bash
export NEXADASH_NEXT_KEYSTORE=/secure/path/webtest.jks
read -r -s -p 'Keystore password: ' NEXADASH_NEXT_KS_PASS; echo
export NEXADASH_NEXT_KS_PASS
bash tools/next/build.sh
# build/next/NexaDash-Next-1.9.apk at this revision
unset NEXADASH_NEXT_KS_PASS
```

Never echo or commit the password/key. Current public signer SHA256: `fa2d67d42fd40612e025c3f87b6a2f7ca9fc48bf613fca9f7a6cdcfcbfc5c752`.

The signing key is deliberately **not in GitHub**. A new Claude Code clone needs the owner's private backup via a secure local transfer. Without that key, a new signer cannot update the installed GitHub Next or communicate with its signature-protected companions. For development, use the Lab/debug identity and re-sign matching patched companions with the same local key (`:app:resignNativeCompanion` task in app/build.gradle). Do not uninstall the owner's installed Next/data or replace its signer without an explicit migration plan. A new key is not an in-place compatible update.

### Companion inputs not included in the clone

- Maps build needs the owner's original `Maps_base.apk`, `Maps_split_config.armeabi_v7a.apk`, `Maps_split_config.en.apk`, `Maps_split_config.mdpi.apk` in one private directory, plus APKEditor source pinned to `5d94bc08565e30e3e3bb7accbd83138e4d6a8e45`. Commands/verification are in tools/maps-native/README.md. Map APK version/splits must match; do not substitute later incompatible versions.
- Existing Maps companion branch: `nexadash-maps-next-companion`. Existing TizenTube Next branch: `nexadash-tizentube-next`. Public download links in docs/DUAL_NATIVE_MS.md are canonical.
- Owner requested the related originals as well. `docs/handoff-inputs-manifest.json` inventories66 staged input files (~1.014GB), including all35 original system RAR volumes, both FM RAR parts, Radio runtime ZIP/files, original Maps splits, selected OEM APKs, TizenTube Lab input and static reports. It also indexes two already-published Next companion APKs. The35 system volumes reconstruct all141 exported APKs; the convenience APK folder is a relevant subset.
- **Current blocker:** the raw-input branch is staged locally as `handoff-inputs-20261031` but has NOT been uploaded. Automatic approval review rejected public exposure of the full proprietary system payload without more explicit informed confirmation. Do not bypass that rejection or pretend binary URLs exist. Obtain the owner's explicit public/private destination decision first. Metadata/hashes and existing published companions are available independently.
- `tools/handoff/fetch-inputs.py --list` shows the inventory. `--group published-companions` can fetch existing public Next companions. Original-input downloads are deliberately refused while the manifest says unpublished. Once publication is authorized and verified, update its published state/commit and use groups `maps`, `radio`, `oem`, `tizentube`, `reports`, `system-archives`, `fm-archives` or `--all`.
- Clone main with `--single-branch` to avoid automatically fetching the future large binary branch. Downloaded inputs default to ignored `.handoff-inputs/`. All downloads must match recorded size and SHA256; multipart extraction needs every volume together.
- Signing secrets, owner photos/videos, account databases and unrelated/unavailable V58/V62/Waze parts are excluded. Raw diagnostic images are summarized in the owner-feedback JSON instead. A fresh clone does not contain staged files or the private signing key.

## Verification and its limits

Current evidence:

- `docs/qa/maps-next19-api27.json`:30 native-map fixture assertions,57 Maps-layout host assertions using normal distinct-UID test apps,27 route/layout JVM checks and12 existing raster cases. No Google Maps ARM/TTS-audio/physical journey pass.
- `docs/qa/radio-panel-next18-api27.json`:133 FM client/UI assertions using firmware doubles,25 band/unit JVM checks; not real tuner/audio.
- `docs/qa/dual-native-api27.json`: earlier47 native host assertions; not universal installed-app hosting.
- `docs/qa/radio-runtime-files-review.json`: complete owner5-file runtime export and recovery hashes.
- `docs/qa/head-unit-native-owner-20261030.json`: earlier owner TizenTube/Maps observations, superseded for Maps latest status by the31 October finding above.
- Android Studio debug/release builds passed; lint0 errors/4 existing PendingIntent warnings. API27 signature/alignment/CRC and no-Quran guard passed. All Android test sources compiled.

Reproduce on an explicitly selected disposable API27 emulator:

```bash
# Build signed Next first; use the same private env vars as above.
export DEVICE=emulator-5554
bash tests/run-panel-map.sh
bash tests/run-maps-layout.sh
# Optional when FM code changes:
bash tests/run-radio-panel.sh
```

Run instrumentation serially: two tests targeting the same package cannot run concurrently. The emulator must advertise secondary-display activities to match the head unit; record any test-only capability configuration. Normal IME stays enabled. Native test app avoids automatic EditText focus on startup; host tools focus a non-editor container. Transparent keyboard touch regions caused early input-test failures even when the app's pixels were visible. Do not remove input guards or grant system privileges merely to obtain a pass.

`tools/radio-fixture` uses real OEM package names but dummy implementations: **never install these fixture APKs on a head unit**. The runner enforces emulator-only and refuses replacing a differently signed package. Native test A/B use their own test identities. A screenshot/pixel/counter/confirmed display in these apps is not real Maps or tuner verification.

```bash
mkdir -p build/route-jvm
javac --release 8 -d build/route-jvm \
 src/com/shazwan/sagadash/DashRouteProgress.java \
 src/com/shazwan/sagadash/DashNativeGeometry.java \
 tests/jvm/RouteProgressChecks.java tests/jvm/NativeGeometryChecks.java
java -cp build/route-jvm com.shazwan.sagadash.RouteProgressChecks
java -cp build/route-jvm com.shazwan.sagadash.NativeGeometryChecks
```

## Further investigation priorities

1. Treat Maps1.9 physical failure as unresolved. Seek decisive evidence for renderer/lifecycle/input/location/network rather than another unverified sizing preset. Do not repackage/sign Google Maps or modify every Activity speculatively.
2. Test the separate NexaDash map on the head unit: real geocoder response, fresh location, route drawing and explicit voice toggle. Cloud fixture passes do not establish external service connectivity. Keep public service limits and production backend requirements visible.
3. Confirm FM antenna sound and actual station changes with the current client; if failing, use Radio report, selected backend and exact command/result. Do not ask for the same exported firmware files again when already reviewed.
4. Measure actual whole-system/companion load on1GB; own-process Performance check excludes companions/WebView renderer.
5. Debloat only after establishing usable privileges and reviewing exact installed dependencies/current package states; no general TV list or blanket system disable.

## Release discipline

Keep source/manifest/Next versionCode progression consistent. Never overwrite an APK under the same release filename with different bytes. Update dist metadata, hashes, QA and current docs, preserve historical test facts and publish completed source/APK updates to main/default aliases without force-push. Verify the public raw download against the tested APK hash. Documentation-only updates keep1.9 APK/version untouched.

Keep the original8-view car atlas restored at the owner's explicit request. Public README screenshot remains edited. Offline Quran reader/player was removed; preserve `tests/check_no_quran.py` and do not advertise an offline Quran feature. Preserve MIT/fork credit to KlikDash and third-party notices.

## Ready-to-paste continuation prompt

Open the repository root and paste the prompt in `docs/CLAUDE_START_PROMPT.md`. Read `CLAUDE.md`, this handoff, the latest owner-feedback JSON and the artifact metadata before proposing another fix.
