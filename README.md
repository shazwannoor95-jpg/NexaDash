# TizenTube · NexaDash Next — ARM32 experiment

[Download APK (~81MB)](TizenTube-NexaDash-Next-arm32.apk?raw=true) · [Dual native install/status](https://github.com/shazwannoor95-jpg/NexaDash/blob/main/docs/DUAL_NATIVE_MS.md) · [Patch/build source](https://github.com/shazwannoor95-jpg/NexaDash/tree/main/tools/tizentube-next)

Use with NexaDash Next1.4 or newer, selecting **TizenTube · native Next companion (test)** in one panel and Maps Next in the other. Start each independently. This app is a new, co-installable identity; the original TizenTube/old Native Lab remains unchanged. Both host and companion must use the same Next signing key. A normal, unpatched installed app cannot use this bridge automatically.

The reviewed input is the earlier TizenTube/Cobalt Native Lab2.0.3 ARM export, identical to the owner's installed-app export. Four DEX and two ARM32 native libraries are unchanged; package/provider/custom-permission identities, main task placement, label/version are adapted for Next. See [artifact.json](artifact.json) for input/output hashes and static checks.

**ARM/head-unit rendering, YouTube playback, touch/IME, background lifecycle and performance remain unverified.** Renaming and re-signing can affect app behavior. The host's separate two-test-app API27 result is not a Maps/TizenTube playback certification. No authentication/certificate checks were bypassed.

[Original project/source](https://github.com/reisxd/TizenTubeCobalt/tree/v2.0.3) · [Upstream copyright/license](UPSTREAM_LICENSE.txt). Original third-party notices/assets remain in the APK. This is an unofficial experiment, not endorsed by Google or upstream authors. Private signing keys are not included.
