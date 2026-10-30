# Cobalt ARM cooperative native companion

Reviewed base: official TizenTubeCobalt2.0.3 `cobalt-arm.apk` from https://github.com/reisxd/TizenTubeCobalt/releases/tag/v2.0.3 . This keeps minSDK24/native ARM32 payload, changes package/provider/custom-permission names for co-installation, sets activity embedding/resize opt-ins and adds the shared signature-protected display-scoped input bridge. Original third-party code/assets/notices remain.

With JDK17, SDK35/build-tools36, Python3 and apktool-cli2.11.1, set `ANDROID_HOME`, `APKTOOL_JAR`, `PROBE_KEYSTORE`, `KLIKDASH_KS_PASS`, then run:

```sh
bash tools/native-companion/build-cobalt.sh /absolute/path/to/cobalt-arm.apk
```

Ignored output: `out/TizenTube-NativeLab-arm.apk`. Use a disposable lab key shared with the Lab host. No third-party APK or private signing key is committed in this source branch. Downloaded cloud-signed companions are on a separate artifact branch, with hashes and status.

ARM runtime/playback/touch/IME/lifecycle has not been tested on the x86-only emulator. Static signature verification does not establish APK safety or working UI. The [Native Lab guide](../../docs/NATIVE_LAB_MS.md) contains install order, restrictions and debug re-sign instructions.
