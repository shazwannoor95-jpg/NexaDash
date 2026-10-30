# Dual-native test companions A/B

Two small, separate Android packages with the same Java UI source: `com.shazwan.nexadash.test.a` and `.b`. Each runs under its own UID and has an independent native counter/editor. These are test apps, not Spotify, navigation, YouTube or screenshots of those apps. No network/media/system privileges are requested.

Set the same private `NEXADASH_NEXT_KEYSTORE`, `NEXADASH_NEXT_KS_PASS`, and `ANDROID_HOME` as the Next build, then run `bash tools/native-test/build.sh`. Install the Next host first, followed by the A/B APKs. Their chooser entries appear only when installed. Use explicit Start independently in the two panel hosts.

`tests/run-dual-native.sh` runs on an explicitly selected disposable emulator/test device (`DEVICE`), installs these apps and the current Next APK and checks real independent display IDs/UIDs, touch/text isolation, resize, duplicate-package protection, failure isolation, Stop/mode switch and leaving/returning to the dashboard. It changes test-host panel preferences; do not run it on a user's daily launcher without backing up their settings. It does not test Maps/TizenTube playback or physical hardware.
