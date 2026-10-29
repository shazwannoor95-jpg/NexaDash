# Maps native companion candidate

This is an **unverified ARM32 native-display candidate**, not a working-navigation certification. It consumes the owner's four Maps25.32 exports (base, armeabi_v7a, English, mdpi); no original/system Maps app is replaced. Runtime behavior after package/signature changes remains unknown: Google identity/GMS, hardcoded provider lookup, map rendering, search, routes/voice, permissions, touch/keyboard and lifecycle all need real-device validation.

Output identity: `com.shazwan.nexadash.maps.next`, **Maps · NexaDash Next**, version `25.32.00.790560726-next1`, minAPI26/target36. Main Activity remains `com.google.android.maps.MapsActivity`. The only change to normal main task placement is standard launch mode + dedicated native task affinity + allowEmbedded/resizeable. The original app's nine DEX files and ten ARM32 libraries are retained byte-for-byte; bridge classes enter a new classes10.dex.

The package/shared UID, owned custom permissions and provider authorities are isolated for co-installation. Signature-protected bridge permission is `com.shazwan.nexadash.next.permission.CONTROL`, separate from the older cloud Native Lab namespace. Install the companion first, then the matching-signed NexaDash Next host. Original Google notices/assets remain; only obsolete signing/stamp files are removed before signing with the test key. Packaged API credentials are neither printed nor changed by these tools.

## Tool provenance / rebuild

Use JDK17, SDK35 and build-tools36. APKEditor is obtained from its official source, not an APK download site:

```
git clone https://github.com/REAndroid/APKEditor.git APKEditor
git -C APKEditor checkout 5d94bc08565e30e3e3bb7accbd83138e4d6a8e45
```

Source declares version1.4.9 and bundles ARSCLib/JCommand/smali dependencies. The build script compiles that pinned source and uses its resource-aware split merge; it does not concatenate APKs or discard the ARM/density payloads. No tool JAR, signing key/password or raw original APK is committed with this source.

Set private `MAPS_NEXT_KEYSTORE` and `MAPS_NEXT_KS_PASS` (alias `webtest`), plus `APKEDITOR_SOURCE` and `ANDROID_HOME`, then:

```
bash tools/maps-native/build.sh /private/path/to/Maps_com.google.android.apps.maps
```

Do not publish the signing key/password or put a real password in shared shell history. The delivered candidate uses the matching Next/Web Test/Preview test key, not Google's or the old cloud Native Lab key.

## Static validation and limits

Single APK ~62.7MB. No sharedUserId, required split markers or uses-split nodes remain. Main opt-ins, standard launch mode, isolated providers, bridge service and signature-level control permission checked. All original DEX/native bytes and source APK hashes compared. Resource IDs23795→23794: the only removed entry is `xml/splits0` (`0x7f170044`), split-install metadata intentionally removed during merge. No unexpected ID missing. ZIP CRC, alignment and matching test certificate checked. API27-specific signature verification succeeds (verifier can warn it does not understand newer v3 scheme references, while v2 is valid).

No Maps APK was executed during this build. Manifest/static checks do not prove the app is embeddable on the OEM firmware or that any map/search/navigation function works. The Next host must check the companion's reported display; a first frame alone is not full navigation validation. This build does not implement Radio FM.
