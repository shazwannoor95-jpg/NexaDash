> Privacy update: old screenshots, video exports and APK downloads withdrawn. Use only the sanitised Next1.3 build. Old feature/build instructions below may refer to withdrawn media.

# NexaDash Next + Maps companion — install/status

## APKs

1. [Maps · NexaDash Next ARM32 (~63MB)](https://raw.githubusercontent.com/shazwannoor95-jpg/NexaDash/nexadash-maps-next-companion/Maps-NexaDash-Next-arm32.apk) — install **first**.
2. [NexaDash Next (~5.5MB)](https://raw.githubusercontent.com/shazwannoor95-jpg/NexaDash/main/dist/next/NexaDash-Next-1.3.apk) — install after the companion, then open from Apps.

Separate packages: `com.shazwan.nexadash.maps.next` and `com.shazwan.nexadash.next`. Both use the same new signing key and a dedicated signature permission, different from old Native Lab. The original Google Maps and old NexaDash are not replaced or uninstalled; their data remains. Next starts with its own settings.

Next is a HOME candidate, not an in-place signed upgrade of the old cloud APK. If the Android Home settings are available, choose **NexaDash Next** manually via Settings → Default launcher. It remains usable from Apps if this firmware hides Home settings. Private Web Test signing backup also signs Next; a Next-labelled private backup is provided in the support session, never in GitHub.

## Maps mana yang digunakan? — semakan semula arkib

**Companion ini memang berasal daripada Google Maps dalam arkib head unit yang dibekalkan**, versi **25.32.00.790560726**, minimum API26, ARM32. Ia bukan Maps26.42/26.43 minimum API29 yang dihantar sebelum eksport sistem.

Semakan semula membandingkan keempat-empat fail `Maps_base.apk`, `Maps_split_config.armeabi_v7a.apk`, `Maps_split_config.en.apk` dan `Maps_split_config.mdpi.apk` dalam eksport 35 bahagian dengan hash input companion yang telah diterbitkan: semuanya sepadan. Kesemua 9 DEX asal dan 10 library ARM32 dibandingkan byte demi byte dengan companion; semuanya dikekalkan. Semakan manifest, resource IDs, bridge, tandatangan API27, alignment dan CRC juga lulus semula. [Metadata/hash companion](https://github.com/shazwannoor95-jpg/NexaDash/blob/nexadash-maps-next-companion/artifact.json).

| Pemasangan | Untuk apa |
|---|---|
| **Maps asal** — `com.google.android.apps.maps` | Maps sedia ada dalam head unit. NexaDash tidak mengubah pemasangan ini. Activity utamanya `singleTask`, resizeable/PiP tetapi tiada `allowEmbedded` atau service bridge NexaDash. |
| **Maps · NexaDash Next** — `com.shazwan.nexadash.maps.next` | Salinan berasingan daripada Maps arkib tadi, dengan bridge input/paparan dan konfigurasi panel. Inilah sasaran yang disambungkan kepada host Next. |

Memilih app asal dalam senarai Apps atau navigation shortcut tidak menambah bridge kepada app itu. NexaDash tidak mempunyai signing key Google untuk memasang APK yang diubah sebagai update kepada Maps asal. Oleh itu laluan panel yang tersedia sekarang memerlukan pemasangan companion berasingan. Jika **Maps · NexaDash Next** sudah terpasang, tidak perlu pasang companion semula kerana semakan ini tidak menghasilkan APK baharu. App dan data Maps asal kekal. Fungsi Maps selepas perubahan identiti/tandatangan masih memerlukan ujian pada peranti; kesepadanan arkib bukan pengesahan runtime.

## Maps native panel

In **Next**, open the right-panel selector → **Google Maps · native Next companion (test)** → **Start**. Host raster follows the surface aspect ratio (longest edge<=800), with corresponding touch coordinates. Only one inline web/file/native player is selected at once. The other panel can remain car or ordinary media controls.

This companion contains the actual Maps25.32 base/ARM32/en/mdpi payload merged into one APK, with an opt-in/input bridge. It is not a Maps website. Original nine DEX/ten ARM32 libraries retained; shared UID and conflicting provider identities removed; MainActivity uses standard launch mode and embedding/resize opt-ins.

**Experimental, not validated navigation:** no ARM/head-unit execution was performed here. Renaming/re-signing can break Google identity/GMS/backend access or hardcoded provider lookups. Static checks do not establish map rendering, location, search, route/voice, touch, permission dialogs or continuous hosting. No authentication/certificate checks were bypassed. For setup/permission prompts that cannot work in the panel, the explicit Open outside panel action can be used for that separate setup step; it does not count as successful native hosting. Capture the actual panel result before using it.

After a failed Start, Settings → App panel compatibility → Copy report includes requested/proxy/reported display and last startup stage. A matched startup display is not proof of working search/navigation. Send screenshots of the actual error or blank panel and the report.

## Radio: not delivered as a working native FM player

The 35-part export was fully scanned (141 APKs/187 regular DEX). Required Radio QtActivity and tuner libraries remain absent from the supplied APK payloads. Local Binder services are not a recovered remote controller API. No opaque tuner commands were guessed or sent.

Next adds **Settings → Radio runtime paths**. In the Radio external-controls panel, the explanation can also be tapped to open this check. It reads installed package source/native/shared-library paths, bounded OAT/VDEX filenames, known system/vendor library visibility and bootclasspath paths. No shell, root, activity launch, service binding, native library load, broadcast, tuner command or file-content upload is performed. **Copy report** shares only the text chosen by the owner.

This check gathers missing firmware locations; it is not a substitute FM controller or hosted original Radio UI. The implementation is still blocked until a usable runtime/control path is established on the head unit. A nonvisible file may be hidden by permissions/SELinux rather than absent.

## Performance and validation

Settings → Performance check provides the existing opt-in own-process30s sample. It excludes Maps/WebView companion processes; it is not a total memory/CPU measurement for the whole system. See [measurement guide](PREVIEW_PERFORMANCE_MS.md).

Next resources/Java/DEX, signature/alignment/CRC, unique identity/HOME filter/task/control permission checks, Gradle compile and test-source compilation passed. Maps companion validation includes split/shared-UID cleanup, provider namespace, matching certificate, all original DEX/native libraries and resource IDs (only obsolete xml/splits0 removed). Rebuild from pinned APKEditor source succeeded. No head-unit or full runtime pass is claimed. Radio hardware control is explicitly not implemented.

## Next 1.1 update

Install Next1.1 over Next1.0: same package/key, versionCode2, existing Next settings retained. Maps companion is unchanged; no need to install it again if the matching Next companion is already present. The old cloud Native Lab is still a different app.

Queued Back/text actions now check their original session before dispatch. Native touches wait for companion display confirmation. An active gesture is cancelled on raster resize or queue overflow; remaining fingers are ignored until a new Down. Text tools support keyboard Send, hide the host keyboard afterwards, and release host input focus on Stop. Next removes the duplicate generic-native menu entry. Browser cleanup always attempts destroy after an OEM renderer stop/pause exception.

Twelve JVM geometry cases passed, covering bounds/aspect/mapped extents including tiny, portrait and extreme sizes. They do not exercise Android MotionEvent/Binder or Maps UI. Signed APK, API27 signature/alignment/CRC, Android compilation and existing test-source compilation checked. No emulator/head-unit runtime test ran; synthetic fullscreen instrumentation exists but was not executed in this environment. Physical Maps search/navigation and Radio hardware control remain unverified/not delivered respectively.

## Next1.3 — pembersihan fungsi lama

Package/signing key Next kekal; versionCode4 membolehkan update daripada Next1.2 tanpa uninstall. Pembaca Quran, pemain/service, data loader, notch dan pautan AI/media lama telah dibuang daripada source dan APK. `tests/check_no_quran.py` memeriksa semua DEX APK dan menolak kelas/fail data lama jika terbawa semula. Pemeriksaan itu gagal pada APK1.2 dan lulus pada1.3. Build Android debug/release, kompilasi sumber ujian, tandatangan API27, alignment dan CRC diperiksa; tiada runtime emulator/head-unit yang dijalankan untuk update ini. Permission/komponen Quran memang tidak berdaftar dalam manifest dashboard sebelumnya. Masa solat/dashboard tidak dibuang.
