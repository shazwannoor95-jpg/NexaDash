# NexaDash Next launcher build

Package `com.shazwan.nexadash.next`, label **NexaDash Next**, version1.4-next5. Separate identity/new test signing key; co-installs with old Native Lab. It is a HOME candidate so the owner can explicitly choose it in Android Home settings. No automatic default change, old-app uninstall or settings/data migration is performed by the build.

Generated manifest isolates task affinities, removes update auto-launch receiver and extra AI launcher icon, and uses the dedicated `com.shazwan.nexadash.next.permission.CONTROL` companion permission. This avoids the legacy global permission's different cloud signing owner. Next1.4 declares its own signature permission; install the host first for new deployments. Maps/TizenTube Next companions must share that key. Next1.4 adds the separately signed TizenTube Next target and optional native test A/B targets. Existing cloud TizenTube/V58/Demo companions are not Next-compatible.

With JDK17, SDK35/build-tools36, Python3/zip, set private NEXADASH_NEXT_KEYSTORE and NEXADASH_NEXT_KS_PASS, alias webtest, then `bash tools/next/build.sh`. Output build/next/NexaDash-Next-1.4.apk. No keys/passwords are published. See ../../docs/NEXT_INSTALL_MS.md for runtime caveats and Radio status.

Next1.3 removes the leftover offline Quran subsystem. The build ends with `python3 tests/check_no_quran.py <apk>` to reject removed classes/data. Font names and third-party license notices are not evidence of a bundled Quran reader.
