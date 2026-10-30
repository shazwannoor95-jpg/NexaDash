# NexaDash Next launcher build

Package `com.shazwan.nexadash.next`, label **NexaDash Next**, version1.3-next4. Separate identity/new test signing key; co-installs with old Native Lab. It is a HOME candidate so the owner can explicitly choose it in Android Home settings. No automatic default change, old-app uninstall or settings/data migration is performed by the build.

Generated manifest isolates task affinities, removes update auto-launch receiver and extra AI launcher icon, and uses the dedicated `com.shazwan.nexadash.next.permission.CONTROL` companion permission. This avoids the legacy global permission's different cloud signing owner. Maps Next companion must be installed first and share the new Next key. Existing cloud TizenTube/V58/Demo companions are not Next-compatible.

With JDK17, SDK35/build-tools36, Python3/zip, set private NEXADASH_NEXT_KEYSTORE and NEXADASH_NEXT_KS_PASS, alias webtest, then `bash tools/next/build.sh`. Output build/next/NexaDash-Next-1.3.apk. No keys/passwords are published. See ../../docs/NEXT_INSTALL_MS.md for runtime caveats and Radio status.

Next1.3 removes the leftover offline Quran subsystem. The build ends with `python3 tests/check_no_quran.py <apk>` to reject removed classes/data. Font names and third-party license notices are not evidence of a bundled Quran reader.
