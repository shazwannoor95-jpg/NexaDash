# Full-dashboard Preview build

Separate `com.shazwan.nexadash.preview` / **NexaDash Preview**, version1.0-preview1. Same current dashboard source/resources with isolated app data. Generated manifest removes HOME/default category, secondary AI launcher entry and update auto-launch receiver, and isolates task affinities. It cannot replace the old Native Lab or silently become the default Home app.

JDK17, Android SDK35/build-tools36, Python3, zip required. Provide private `NEXADASH_PREVIEW_KEYSTORE` and `NEXADASH_PREVIEW_KS_PASS`, keystore alias `webtest`, then run `bash tools/preview/build.sh`. No key/password is in Git. The published APK uses the same test key as Web Test1.0; its private signing backup also covers Preview updates.

Performance check is opt-in, two own-process snapshots separated by30seconds with no permanent polling/framehook or forced GC. Pause/stop cancels a pending sample. CPU is one-core equivalent, not whole-device utilization; PSS excludes WebView renderer and companion/other app processes. System RAM availability is a separate device-wide observation. Do not add heap values to PSS or attribute changes in device memory to Preview alone.

This is a test/dashboard app, not an in-place signed upgrade or data migration of the old cloud APK. Original cloud-signed native companions do not share this key, so original TizenTube Native Lab binding is not fixed here. Native Maps/Radio are not implemented by packaging Preview.
