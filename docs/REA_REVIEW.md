# REA reference assessment

User reference: https://github.com/morluto/rea
Inspected revision: 1173c0a9dc1b352a60943a491462294db8f353ae (2026-10-26).
Package metadata at that revision: rea-agents6.8.0; this is not an independent npm-publication check.

REA is a reverse-engineering investigation tool. Its Android provider can inspect a supplied
standalone APK's manifest, components, Java/smali methods and cross-references with JADX.
That may help identify the original TEST9278B Radio/Bluetooth apps' broadcast actions, metadata
extras, services and control interfaces. It is not itself a Bluetooth/radio driver or3D modeller.
Static evidence still needs runtime/device verification; native libraries and split APK/AAB
need different handling. No original head-unit APK or connected head unit was supplied.

No REA setup, MCP registration, analysis engine or client configuration was installed for this
change. KlikDash source is available, so direct source adaptation is sufficient for the clock,
prayer and notice fixes. Future OEM APK analysis can use REA once the actual files are available.

Pinned documentation:
- https://github.com/morluto/rea/blob/1173c0a9dc1b352a60943a491462294db8f353ae/docs/android-analysis.md
- https://github.com/morluto/rea/blob/1173c0a9dc1b352a60943a491462294db8f353ae/docs/adb-device-analysis.md
- https://github.com/morluto/rea/blob/1173c0a9dc1b352a60943a491462294db8f353ae/THIRD_PARTY_NOTICES.md
