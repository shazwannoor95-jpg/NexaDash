# Maps · NexaDash Next — experimental ARM32 companion

[Download APK (~63MB)](Maps-NexaDash-Next-arm32.apk?raw=true) · [Next launcher and install guide](https://github.com/shazwannoor95-jpg/klikdash/blob/nexadash-native-panel-experiment/docs/NEXT_INSTALL_MS.md) · [Rebuild tools and validation](https://github.com/shazwannoor95-jpg/klikdash/tree/nexadash-native-panel-experiment/tools/maps-native).

Install this companion **first**, then the matching NexaDash Next launcher. In Next select **Google Maps · native Next companion (test)** in the right panel and tap **Start**. The companion uses a separate package, `com.shazwan.nexadash.maps.next`; it does not replace original Maps. It is signed with the new Next test key and requires the matching host, not the old cloud-signed Native Lab. Only one inline player runs at once.

This candidate contains the actual Maps25.32 ARM32 payload with an opt-in native-display/input bridge. It is not a web map. Nine original DEX and ten ARM32 libraries are preserved; resource-aware split merging, package/provider isolation, signature, alignment and CRC checks passed. Minimum API26.

**No ARM/head-unit execution was performed. Rendering, location, search, routes/voice, Google services/login, panel touch/keyboard, resizing and lifecycle are unverified.** Repackaging/re-signing can break Google identity/backend access or hardcoded provider lookups. Static checks do not establish working navigation. No authentication/certificate checks were bypassed. Do not treat this as a certified or working navigation release.

Radio FM native control is not included. See the main guide for current blockers and the read-only runtime report.

Artifact details and original-input hashes: [artifact.json](artifact.json). Original Google notices/assets remain in the APK. Signing keys/passwords and raw user archives are not in this branch.
