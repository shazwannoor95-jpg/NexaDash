# TEST9278B: DiAuto / uploaded navigation APK review

Static review, 2026-10-28. No physical TEST9278B or phone connected; **USB/wireless Android Auto, audio, microphone, touch and reconnect are not verified**. This is not a working-device certification.

Reported device: alps TEST9278B, actual API27, armeabi-v7a/armeabi, 1024x600. OEM reported Android9.1 is inconsistent with actual API27 (Android8.1).

## DiAuto

Source: https://github.com/shihabal3amri/DiAuto at `f3046721814bff289a78e58f597b45d895efbb95`, advertised version0.3.12.

- GitHub flavor minSdk16 in app/build.gradle.kts: API27 passes the source minimum.
- Native CMake uses per-ABI libusb; checked-in armeabi-v7a/libusb1.0.so exists. Published release APK ABI contents have not been inspected.
- USB host, Wi-Fi, Bluetooth and other hardware features are optional in manifest. This permits installation without proving transport support.
- H.264 decoder path exists; bundled FFmpeg HEVC libraries are checked in for arm64 only. Prefer hardware H.264 for a first ARM32 test; actual OEM decoding unknown.
- Maintainer support focuses on BYD; tested DiLink5.1 Android13. Other head units unsupported/unverified by maintainer. BYD HUD/cluster functions do not imply support on TEST9278B.
- Practical first trial: latest official release on head unit, Android Auto-capable phone, USB data cable, close ZLink to avoid competing USB access, approve USB/phone prompts. If available choose modest resolution/frame rate and H.264. Verify video/touch/audio/mic/reconnect before calling compatible. No ADB is needed by the documented normal install flow.
- Android Auto runs phone navigation; it does not embed original installed Maps/Waze into NexaDash's left/right panels.

## Uploaded APKs (actual manifests)

| File | Minimum | ABI | TEST9278B installation |
|---|---|---|---|
| Google Maps26.43.00.1002020710 | API29 Android10 | ARM32/ARM64/x86/x86_64 | Blocked by minimum API |
| Waze5.26.0.0 XAPK | API29 Android10 | Supplied native split ARM64 only | Blocked by minimum API and missing ARM32 split |

Waze requires a complete compatible split set; renaming XAPK to APK is insufficient. Neither lowering a manifest minimum nor adding allowEmbedded ports native code/framework dependencies to Android8.1.

Maps' allowEmbedded=true applies to its car-projection Activity guarded by com.google.android.projection.gearhead.permission.START_PROJECTED_ACTIVITY, not a universal opt-in for the normal MapsActivity. Waze has resizeableActivity=true but no allowEmbedded found. No companion patch or execution of these APKs was performed.

## Input hashes

- `Waze+Navigation+&+Live+Traffic_5.26.0.0_APKPure.xapk`: SHA256 `ccb2ca58a29dbca3e6e92acc4222bbdfa7e756a886fd822a3457009906033506`, 192421687 bytes.
- `Google+Maps_26.43.00.1002020710_APKPure.apk`: SHA256 `9ad0d0d3fbb34e1bc9e99c26d454a702c511dab6cb74fc98e2917d4c553f95ec`, 304397539 bytes.
