# Radio follow-up: full35-part export (29 October2026)

> Follow-up30 October: both later `fm.part1/2.rar` volumes extracted successfully. All five APKs are identical to this export, and the App Manager Radio backup's `oat` directory is empty. Next1.5 adds a fixed-path local firmware-file exporter to obtain the omitted binaries. [Results and steps](RADIO_EXPORT_MS.md).

Read-only static analysis. No APK or OEM library executed, no head-unit command/broadcast sent, no original modified. This analysis uses only `/workspace/headunit35/extracted`, the new35-part set; the older16-part set was kept separate.

## Completed scope

- Scanned **141 APKs** in the extracted export; ZIP CRC checks passed for every APK.
- Parsed **187 regular `classes*.dex` payloads**, totaling571,003 class-definition entries (not unique classes). No DEX parse errors.
- Resolved DEX `class_defs` through type/string tables to distinguish real class definitions from string references. Searched exact Radio package/class descriptors plus RadioInterface/RadioService/FmService/CanbusService/OEM tuner markers.
- Checked archive entries for embedded APK/JAR/DEX/ODEX/VDEX payloads and library names relevant to Radio. Embedded GMS modules and Chrome webapk DEX are unrelated findings; no Radio payload established there.
- Decoded selected new-export Radio, FM, Launcher, CarSet and Video bytecode using APKTool2.11.1; manifests read with SDK36 aapt2.

## Main result

**The full app export still does not contain a regular DEX definition of `com.acloud.stub.localradio.QtActivity`.** The Radio APK declares that launcher Activity but contains no classes.dex or native library. Launcher contains its name as a reference, not its implementation.

The follow-up search of CarSet/CanBus is now complete at the regular APK DEX level:

| APK | DEX | Relevant finding |
|---|---:|---|
| CarSet_CanBus1.apk |38|Nine `CanbusService`/inner-class definitions; no `com.acloud.stub.localradio.QtActivity` or RadioInterface implementation |
| CanBus2_CanBus2.apk |38|References `android.os.ICanbusService`; no Radio UI/tuner implementation found |
| Radio_GalaRadio_integrate.apk |0|Declares missing Radio QtActivity;21,049-byte resource/manifest export |
| localradio_GalaRadio.apk |1|Actual `Interface.RadioInterface`, `com.radio.service.RadioService` and local Binder classes |
| localradio_FMRadio.apk |1|Actual `com.android.fmradio` Activity/service/native wrappers plus `Interface.RadioInterface` |

This closes the specific hypothesis that the supplied CarSet/CanBus regular DEX sets contain the missing Radio Activity. It does **not** establish that the implementation is absent from the installed firmware: framework/bootclasspath code, OEM integration and omitted preoptimized code remain possibilities whose locations are not determined by this export.

## Native tuner dependencies

`localradio_com.acloud.stub.extradio` explicitly calls `System.loadLibrary("RadioInterface")` in `RadioApplication.onCreate`; `Interface.RadioInterface` declares native openDevice/closeDevice, seekUp/seekDown, scanAll/scanStop, setBand/setFreq, volume and RDS methods.

The FM app likewise loads `RadioInterface`, and its `FmNative` class loads `fmjni`.

**No `libRadioInterface.so` or `libfmjni.so` archive entry was found in any of the141 supplied APKs.** The loader's effective library path and tuner permissions must be established on the actual head unit. No APK-only patch has yet supplied these runtime components or demonstrated access to them from NexaDash.

## Why exported service does not establish a controller API

- Extradio manifest exports `com.radio.service.RadioService`, but its returned `RadioService$LocalBinder` is a normal Binder subclass whose `getService()` returns the local Service object. It has no custom transaction protocol in that Binder class.
- FM manifest exports `.FmService` with action `com.android.fmradio.IFmRadioService`. Its `FmService$ServiceBinder` is also local-only with `getService()`, not an AIDL Stub/Proxy control protocol.
- Cross-process clients receive BinderProxy; they cannot cast that to the service's local Java object. The action name alone does not make it a usable remote radio API.
- This finding applies to these particular exported services. It does not rule out other firmware-side APIs that are not included here.

## Broadcasts can reopen the full app

In FM bytecode, dynamic previous/next/scan actions are registered by **FmMainActivity**, not a demonstrated independent controller service. Its receiver includes `xy.android.fm_scan_prev`, `xy.android.fm_scan_next`, `xy.android.previousmedia`, `xy.android.nextmedia`, `xy.setfm.freq`, band and channel actions.

The manifest `FmBroadcastReceiver` checks a tuner-chip value through `AtcMetazone`, and the `xy.setfm.freq` path explicitly builds an Intent for `FmMainActivity` and calls startActivity. Therefore sending that action is not a verified method of changing frequency while keeping only NexaDash visible. No such action was sent during this review.

## CarSet Binder findings

CarSet's `CanbusService` looks up system `canbus_service` via `ServiceManager`, and its own onBind returns `MiraVisionJniAidlInterface$Stub`. The `ICanbusService` interface packaged with Video has eight methods: callback registration/removal, `setBCS`, opaque byte-array `setValue`, and navigation-package queries. This is not a recovered, documented FM tuning protocol. No opaque command bytes were guessed or sent.

## Consequence for the right panel

- Original Radio app UI is not a ready NexaDash native companion from this archive alone.
- A custom NexaDash tuner UI still requires a verified status/control API and compatible system native runtime. No working hardware controller was delivered or claimed.
- Repacking/signing the tiny Radio manifest or copying CarSet code is insufficient. CarSet/Extradio also rely on system identity and firmware behavior.
- The useful missing evidence is runtime code/library location and accessible API behavior, not another export of the same APK files. Read-only inspection of package paths/shared libraries/bootclasspath or system library listings could establish locations if a future device-side diagnostic is available. Full firmware extraction/root is not asserted as required or authorized.

## Local evidence

- `scan.py`, `scan-results.json`, `scan.log`: full DEX definition/reference and container inventory.
- `Radio_com.acloud.stub.localradio.manifest.txt`: Radio manifest and declared Activity.
- `decoded/extradio/smali/com/acloud/stub/extradio/RadioApplication.smali:22–25`: loads RadioInterface.
- `decoded/extradio/smali/Interface/RadioInterface.smali:115–215`: native control declarations.
- `decoded/extradio/smali/com/radio/service/RadioService$LocalBinder.smali`: local getService implementation.
- `decoded/fmradio/smali/com/android/fmradio/FmService$ServiceBinder.smali`: local Binder.
- `decoded/fmradio/smali/com/android/fmradio/FmNative.smali:16–18`: loads fmjni.
- `decoded/fmradio/smali/com/android/fmradio/FmBroadcastReceiver.smali:217–276`: frequency action opens Activity.
- `decoded/fmradio/smali/com/android/fmradio/FmMainActivity.smali:7475–7552`: Activity-scoped dynamic receiver actions/registration.
- `decoded/carset/smali_classes26/com/kyhero/car/myhost/Canset/CanbusService.smali:651–655,813`: system service lookup and own Binder return.
- `decoded/video/smali_classes2/android/os/ICanbusService.smali`: eight-method interface.
