# NexaDash Next launcher build

Package `com.shazwan.nexadash.next`, label **NexaDash Next**, version1.9-next10. Separate identity/new test signing key; co-installs with old Native Lab. It is a HOME candidate so the owner can explicitly choose it in Android Home settings. No automatic default change, old-app uninstall or settings/data migration is performed by the build.

Generated manifest isolates task affinities, removes update auto-launch receiver and extra AI launcher icon, and uses the dedicated `com.shazwan.nexadash.next.permission.CONTROL` companion permission. This avoids the legacy global permission's different cloud signing owner. Next1.4 declares its own signature permission; install the host first for new deployments. Maps/TizenTube Next companions must share that key. Next1.4 adds the separately signed TizenTube Next target and optional native test A/B targets. Existing cloud TizenTube/V58/Demo companions are not Next-compatible.

With JDK17, SDK35/build-tools36, Python3/zip, set private NEXADASH_NEXT_KEYSTORE and NEXADASH_NEXT_KS_PASS, alias webtest, then `bash tools/next/build.sh`. Output build/next/NexaDash-Next-1.9.apk. No keys/passwords are published. See ../../docs/NEXT_INSTALL_MS.md for runtime caveats and Radio status.

Next1.3 removes the leftover offline Quran subsystem. The build ends with `python3 tests/check_no_quran.py <apk>` to reject removed classes/data. Font names and third-party license notices are not evidence of a bundled Quran reader.

Next1.5 adds Settings → Export Radio files, using Android ACTION_CREATE_DOCUMENT for a user-chosen ZIP. Exactly five known firmware binaries are attempted, with bounded reads, SHA256/status records and a read-only runtime path report. No storage permission, root/ADB, OEM code execution, tuner control or automatic upload is added. See ../../docs/RADIO_EXPORT_MS.md.

Next1.6 adds read-only Radio backend selection and device-node metadata to the existing report. The resolver follows the recovered OEM VDEX branch, including its43/255 boundaries. It does not load OEM code or open a driver. Native hosting and Maps/TizenTube companions are unchanged; unreleased sizing experiments are not included. See ../../docs/RADIO_RUNTIME_FILES_REVIEW.md.

Next1.7 restores the original eight-view vehicle atlas, including the visible plate, at the owner's request. Native host/companions and Radio diagnostics are unchanged.

Next1.8 adds native in-panel FM controls through the installed OEM services. It supports the recovered internal3.1 and external2.3.24 protocols with explicit Start/Stop, bounded frequency input and presets. No Radio companion binary is shipped. See ../../docs/RADIO_PANEL_MS.md and ../../docs/RADIO_SERVICE_PROTOCOL.md. Emulator service doubles validate software IPC, not head-unit audio.

Next1.9 adds the native map search/routing panel and Maps-specific spacious/compact layout controls. No new Maps companion binary is required. See ../../docs/PANEL_MAP_NEXT19_MS.md for service, GPS, speech and physical-test limits.
