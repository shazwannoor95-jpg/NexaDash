# TizenTube · NexaDash Next ARM32 candidate

This companion is a separate package, `com.shazwan.nexadash.tizentube.next`, version2.0.3-next1/code204. It uses the current Next signing key and `com.shazwan.nexadash.next.permission.CONTROL`; old cloud Lab companions have a different certificate and permission. The original installed TizenTube/Native Lab app is retained.

The reviewed input is the earlier published Cobalt/TizenTube Native Lab ARM APK, SHA256 `097530c5403f1ebba0db24e2cfcc4b3b33d3ca55ea0724364ed57e976185ba40`. The owner's complete head-unit export contains exactly the same bytes. Input download: https://raw.githubusercontent.com/shazwannoor95-jpg/NexaDash/nexadash-native-companions/TizenTube-NativeLab-arm.apk . Original source/release: https://github.com/reisxd/TizenTubeCobalt/tree/v2.0.3 .

PatchTizen changes only manifest/resource package identity, authorities/custom permissions, main launch mode (`standard`), task affinity and the displayed label/version. All four existing DEX files (including the previously added cooperative bridge) and two ARM32 libraries are preserved byte-for-byte. The `com.shazwan.embedprobe.CONTROL` Binder interface token is a protocol identifier, not the old signature-permission owner.

With JDK17/SDK35/build-tools36, obtain the official APKEditor source pinned at `5d94bc08565e30e3e3bb7accbd83138e4d6a8e45`, as documented in ../maps-native/README.md. Set `APKEDITOR_SOURCE`, `ANDROID_HOME`, private `NEXADASH_NEXT_KEYSTORE` and `NEXADASH_NEXT_KS_PASS` (alias webtest), then:

```
bash tools/tizentube-next/build.sh /private/path/TizenTube-NativeLab-arm.apk
```

Output: build/tizentube-next/TizenTube-NexaDash-Next-arm32.apk. No key/password is included. Static checks verify API27 signing, ZIP alignment, CRC and unchanged DEX/native payload. ARM rendering, YouTube playback, touch/IME, background lifecycle and physical head-unit behavior remain unverified. A passing two-test-app host check does not certify TizenTube or Maps.

Upstream copyright/license is reproduced in UPSTREAM_LICENSE.txt. Original third-party notices/assets remain in the APK. This build is unofficial and not endorsed by Google or the upstream authors.
