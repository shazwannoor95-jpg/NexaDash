# TizenTube Native Lab ARM companion — unverified hardware experiment

[Download APK](TizenTube-NativeLab-arm.apk?raw=true). Based on [official Cobalt2.0.3 ARM](https://github.com/reisxd/TizenTubeCobalt/releases/tag/v2.0.3). Original code/assets/notices are retained; source and patch instructions are linked below.

Package changed to **com.shazwan.nexadash.tizentube.lab** (label TizenTube Native Lab), so it can install beside the original app. Activities opt into private-display hosting and a signature-protected input bridge has been added. MinSDK24 and ARM32 ABI are retained. Manifest, APK integrity and signature were checked; **runtime, touch, typing, playback and lifecycle have not been verified on ARM hardware**. This cannot execute in the x86-only test emulator.

Install this companion first, choose **Done** rather than Open, then install/open matching NexaDash Native Lab. In either panel select **Native app · experimental**, choose **TizenTube**, then Start. Use this only as a parked hardware test; do not replace the working default launcher with the Lab. Android8.1/API27 meets the manifest SDK floor, which is not proof of working rendering/playback.

Only the matching cloud-signed Lab can bind this cloud-signed companion. Android Studio debug builds need the same local debug key on BOTH apps; use the project's `resignNativeCompanion` task described in [Native Lab guide](https://github.com/shazwannoor95-jpg/klikdash/blob/nexadash-native-panel-experiment/docs/NATIVE_LAB_MS.md).

[Patch source](https://github.com/shazwannoor95-jpg/klikdash/tree/nexadash-native-panel-experiment/tools/native-companion). No root/system embedding permission is added. This package does not embed arbitrary original apps and does not establish Maps/Waze/WhatsApp hosting.
