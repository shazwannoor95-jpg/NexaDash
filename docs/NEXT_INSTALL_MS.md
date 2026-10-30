> **Semasa: Next1.6.** [Export fail Radio](RADIO_EXPORT_MS.md). Untuk dua native panel dan TizenTube companion, ikut [panduan dual native](DUAL_NATIVE_MS.md). Nota lama tentang satu inline player di bawah digantikan oleh polisi: dua companion native berbeza dibenarkan; web/file kekal eksklusif.

> Privacy update: old screenshots, video exports and APK downloads withdrawn. Use the current sanitised Next1.6 build. Old feature/build instructions below may refer to withdrawn media.

# NexaDash Next + Maps companion — install/status

## APKs

1. [NexaDash Next1.6 (~3MB)](https://raw.githubusercontent.com/shazwannoor95-jpg/NexaDash/main/dist/next/NexaDash-Next-1.6.apk) — install **first**, then open from Apps.
2. [Maps · NexaDash Next ARM32 (~63MB)](https://raw.githubusercontent.com/shazwannoor95-jpg/NexaDash/nexadash-maps-next-companion/Maps-NexaDash-Next-arm32.apk) — install the matching companion if not already installed.

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

Current owner evidence: TizenTube video advances in the left panel. Maps companion works outside the panel according to the owner, but map/search content stays blank inside, including when Maps is the only active panel. [Current findings and next layout check](MAPS_PANEL_MS.md).

## Maps native panel

In **Next**, open the right-panel selector → **Google Maps · native Next companion (test)** → **Start**. Host raster follows the surface aspect ratio (longest edge<=800), with corresponding touch coordinates. Two distinct native companions can run in separate panels. Web/file playback remains exclusive with other inline panels.

This companion contains the actual Maps25.32 base/ARM32/en/mdpi payload merged into one APK, with an opt-in/input bridge. It is not a Maps website. Original nine DEX/ten ARM32 libraries retained; shared UID and conflicting provider identities removed; MainActivity uses standard launch mode and embedding/resize opt-ins.

**Experimental, not validated navigation:** no ARM/head-unit execution was performed here. Renaming/re-signing can break Google identity/GMS/backend access or hardcoded provider lookups. Static checks do not establish map rendering, location, search, route/voice, touch, permission dialogs or continuous hosting. No authentication/certificate checks were bypassed. For setup/permission prompts that cannot work in the panel, the explicit Open outside panel action can be used for that separate setup step; it does not count as successful native hosting. Capture the actual panel result before using it.

After a failed Start, Settings → App panel compatibility → Copy report includes requested/proxy/reported display and last startup stage. A matched startup display is not proof of working search/navigation. Send screenshots of the actual error or blank panel and the report.

## Radio: not delivered as a working native FM player

The later **NexaDash-Radio-files.zip** contains all five expected firmware binaries. The VDEX has now been dequickened successfully: the original Radio icon is a dispatcher selecting either the external Radio Activity or Android FM Activity using one firmware property. The missing tuner libraries are also present in this new export. These findings supersede the earlier APK-only missing-code/library status. [Static review and checksums](RADIO_RUNTIME_FILES_REVIEW.md).

**Next1.6 → Settings → Radio runtime paths → Copy report** adds the exact FM selector property, the component selected by the recovered branch, app/host UIDs, and metadata/access flags for three fixed driver paths. It does not launch Radio, open/read/write a driver, send ioctl/broadcast commands, bind an OEM service or load OEM code. An unknown property read stays unresolved; no backend is guessed.

Next1.5's **Export Radio files** remains available; the owner's five-file export succeeded. **No need to repeat that export** for this investigation. [Guide and current next step](RADIO_EXPORT_MS.md).

Radio hosting/control is still not delivered. The selected real FM app, system identity/native-library namespace and hardware access need to be established. Readable firmware files and access flags alone do not prove a normal app can control the tuner.

## Performance and validation

Settings → Performance check provides the existing opt-in own-process30s sample. It excludes Maps/WebView companion processes; it is not a total memory/CPU measurement for the whole system. See [measurement guide](PREVIEW_PERFORMANCE_MS.md).

Next resources/Java/DEX, signature/alignment/CRC, unique identity/HOME filter/task/control permission checks, Gradle compile and test-source compilation passed. Maps companion validation includes split/shared-UID cleanup, provider namespace, matching certificate, all original DEX/native libraries and resource IDs (only obsolete xml/splits0 removed). Rebuild from pinned APKEditor source succeeded. No head-unit or full runtime pass is claimed. Radio hardware control is explicitly not implemented.

## Next 1.1 update

Install Next1.1 over Next1.0: same package/key, versionCode2, existing Next settings retained. Maps companion is unchanged; no need to install it again if the matching Next companion is already present. The old cloud Native Lab is still a different app.

Queued Back/text actions now check their original session before dispatch. Native touches wait for companion display confirmation. An active gesture is cancelled on raster resize or queue overflow; remaining fingers are ignored until a new Down. Text tools support keyboard Send, hide the host keyboard afterwards, and release host input focus on Stop. Next removes the duplicate generic-native menu entry. Browser cleanup always attempts destroy after an OEM renderer stop/pause exception.

Twelve JVM geometry cases passed, covering bounds/aspect/mapped extents including tiny, portrait and extreme sizes. They do not exercise Android MotionEvent/Binder or Maps UI. Signed APK, API27 signature/alignment/CRC, Android compilation and existing test-source compilation checked. No emulator/head-unit runtime test ran; synthetic fullscreen instrumentation exists but was not executed in this environment. Physical Maps search/navigation and Radio hardware control remain unverified/not delivered respectively.

## Next1.3 — pembersihan fungsi lama

Package/signing key Next kekal; versionCode4 membolehkan update daripada Next1.2 tanpa uninstall. Pembaca Quran, pemain/service, data loader, notch dan pautan AI/media lama telah dibuang daripada source dan APK. `tests/check_no_quran.py` memeriksa semua DEX APK dan menolak kelas/fail data lama jika terbawa semula. Pemeriksaan itu gagal pada APK1.2 dan lulus pada1.3. Build Android debug/release, kompilasi sumber ujian, tandatangan API27, alignment dan CRC diperiksa; tiada runtime emulator/head-unit yang dijalankan untuk update ini. Permission/komponen Quran memang tidak berdaftar dalam manifest dashboard sebelumnya. Masa solat/dashboard tidak dibuang.
