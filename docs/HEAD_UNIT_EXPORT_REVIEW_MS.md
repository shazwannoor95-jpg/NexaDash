> Follow-up: the later35-part set extracted completely (141 APKs/117 folders) and the CarSet/CanBus class-definition search is now completed. [Updated full-archive results](NATIVE10_WEB_FULLSCREEN_MS.md) · [Radio details](RADIO_FULL_ARCHIVE_35_REVIEW.md). The39-APK review below is the earlier subset, not the size of the new archive.

# Semakan eksport head unit — 29 Oktober 2026

Semakan statik terhadap 16 bahagian RAR yang dibekalkan: **39 fail APK dalam 31 folder**, 476,512,273 bait selepas extraction. Semua 39 struktur ZIP/CRC lulus; 38 APK lulus `apksigner verify`. `video_GalaVideo.apk` gagal semakan kerana metadata v1 menyatakan signature v2 tetapi blok v2 tiada. Punca boleh berkaitan export/repackaging; ini bukan penentuan keselamatan atau keadaan APK yang sedang berjalan pada unit.

Manifest, metadata, tandatangan, DEX dan library telah diperiksa untuk semua APK. Analisis kod lebih mendalam ditumpukan pada Maps/Net Navi, media/radio/Bluetooth, Launcher, Car Settings, framework resources, Theme, CarSet/CanBus2 dan ZLink. APK tidak dijalankan, dipasang atau diubah dalam semakan arkib. Fail mentah dan kandungan akaun tidak diterbitkan. Environment analisis asal terputus; ringkasan ini merekod penemuan yang telah disahkan sebelum itu.

**Had penting:** ini eksport APK, bukan imej firmware lengkap. Tiada `services.jar`, `framework.jar`, kod OAT/VDEX atau log masa jalan. Beberapa APK sistem tiada DEX. Pengisytiharan dalam APK tidak membuktikan kebenaran runtime, sokongan hosting yang efektif, penggunaan RAM/CPU, atau playback/navigation.

## Penemuan untuk panel native

- Maps yang memang dieksport dari unit ialah **25.32.00.790560726**, code1068185295, minAPI26, target36, base + ARM32/en/mdpi splits sepadan. Ini melepasi syarat API27/ARM32, berbeza daripada Maps26.42/26.43 minAPI29 yang dihantar sebelum ini.
- MapsActivity biasa ialah `singleTask`, resizeable dan PiP, tetapi tiada `allowEmbedded`. Opt-in hanya pada GhostActivity Android Auto yang dilindungi permission `START_PROJECTED_ACTIVITY`; ia bukan pintu masuk umum untuk NexaDash.
- Maps menggunakan shared UID pada API<=32 dan Google signing lineage. Membina companion berasingan memerlukan identiti/package/provider/split/signature yang konsisten. GMS, sign-in, carian, tiles, routing dan suara selepas perubahan identiti belum diuji. Belum ada Maps companion siap.
- YouTube asal ialah **20.26.40**, minAPI26, target35, lengkap split ARM32/en/mdpi. Tiada lagi halangan minAPI29 seperti V58, tetapi ia tetap belum menjadi companion native secara automatik.
- Penjelasan pengguna terakhir: memilih TizenTube dan menekan Open/Choose app membuka app biasa. Kod mod external controls memang berbuat demikian. Ini belum membuktikan laluan `Native app → TizenTube → Start` gagal pada firmware.
- Companion TizenTube masih `singleTask`; reuse task sedia ada ialah hipotesis yang perlu diuji jika native Start benar-benar keluar panel. Launch mode tidak diubah berdasarkan andaian.
- Host lama hanya meminta display, tanpa mengesahkan actual display. Source native9 menambah semakan display yang dilaporkan companion, deadline60s dan laporan persisten. Ini mengesahkan laporan display pada startup sahaja, bukan playback, input lengkap atau kestabilan berterusan.

## Radio, media dan OEM

- `Radio_GalaRadio_integrate.apk` hanya21,049 bait, tiada DEX/library. Punca pelaksanaan luarnya belum dipastikan: integrasi OEM atau kod preoptimized yang tidak dieksport masih kemungkinan, bukan fakta.
- `com.android.fmradio` ialah implementasi FM sebenar, tetapi memerlukan `libfmjni` sistem yang tidak dibawa dalam APK. Service Binder ialah local Binder walaupun action bernama IFmRadioService. Arahan `xy.setfm.freq` boleh membuka Activity penuh skrin dan bergantung pada radio chip tertentu.
- `com.acloud.stub.extradio` menggunakan system shared UID, local Binder dan RadioInterface yang tidak dibundel. Tidak ada bukti ia API AIDL umum untuk app biasa.
- LocalMusic mempunyai `IPlayService` AIDL/callback dan library ARM32 VLC. Ini calon untuk kawalan player menggunakan UI NexaDash sendiri; bukan bukti UI app asal boleh diembed.
- Nama `QtActivity` sahaja tidak membuktikan runtime Qt. Music menggunakan VLC; Video membawa Vitamio/FFmpeg. Video/BT mempunyai launch mode dan kebergantungan OEM yang perlu diuji.
- Framework resources API27 mengisytiharkan multiDisplay/multiWindow/splitScreen=true dan freeform=false. Nilai packaged default bukan ujian runtime.
- Launcher/framework/SystemUI/banyak utiliti OEM berkongsi sijil platform. Permission pengurusan stack/input/embedding dilindungi platform; menyalin manifest atau kod tidak memberikan kebenaran tersebut kepada NexaDash.
- CarSet dan CanBus2 mempunyai38 DEX dan VirtualAPK Activity stubs. Itu plugin loading dalam app OEM, bukan bukti host umum untuk app terpasang. Carian silang implementasi Radio dalam host ini belum selesai.
- APK WebView versi63 yang dieksport ialah **Chrome-donor stub** tanpa enjin native sendiri. Chrome tidak disertakan; versi provider yang aktif tidak boleh dipastikan daripada stub itu. Source native9 boleh melaporkan provider melalui compatibility report tanpa mencipta browser panel.
- ZLink3.9.27 ialah ARM32, min17/target22 dengan protected native wrapper. Keadaan activation, USB, pairing dan punca sambungan gagal tidak boleh disahkan dari APK sahaja. Tiada percubaan sambungan/DiAuto dibuat.

## Inventori satu per satu

`DEX=0` bermaksud kod tidak ada dalam eksport itu, bukan app tidak pernah berjalan pada firmware. Minimum bagi split config ikut base. Saiz fail bukan ukuran RAM.

| Fail | Min API | Kandungan / penilaian |
|---|---:|---|
| Android_System_framework-res.apk |27|Resources framework; tiada DEX; defaults paparan disemak|
| Android_System_WebView_WebViewGoogle.apk |24|Chrome-donor stub; tiada engine .so|
| Bluetooth_MtkBluetooth.apk |27|Android Bluetooth stack; DEX; sijil OEM|
| Bluetooth_BTSuite.apk |19|UI Bluetooth OEM; shared UID/privilege dan lifecycle khusus|
| BT_Tool_BtTool.apk |27|Utiliti MTK; tiada DEX dalam eksport|
| CAN_Settings_PvCanset.apk |17|Resources/settings CAN; tiada DEX dalam eksport|
| CanBus2_CanBus2.apk |26|38 DEX; plugin stubs; tiada host umum dibuktikan|
| Car_Settings_XyautoSettings.apk |27|Settings OEM; sijil platform; bukan grant privilege NexaDash|
| CarSet_CanBus1.apk |26|38 DEX; plugin stubs; kaitan pelaksanaan Radio belum dipastikan|
| CopyCanbus_CopyCanbus.apk |24|Utiliti konfigurasi OEM; tiada arahan dihantar|
| Gallery_MtkGallery2.apk |16|Tiada DEX dalam eksport|
| GoogleCalendarSyncAdapter.apk |15|Google sync adapter; bukan player/peta|
| Google_Play_services_base.apk |24|14 DEX;39 library ARM32; infrastruktur GMS|
| Google_Play_services_split_config.en.apk |base|Resource split; tandatangan sepadan|
| Google_Play_services_split_config.mdpi.apk |base|Density split; tandatangan sepadan|
| Launcher_LauncherXYAll.apk |19|Launcher OEM bertandatangan platform; tiada generic embedding API dibuktikan|
| localmusic_GalaMusic.apk |19|DEX +12 library ARM32; VLC dan service AIDL|
| localradio_GalaRadio.apk |19|RadioInterface/system shared UID/local Binder|
| localradio_FMRadio.apk |21|FM sebenar; JNI sistem tidak dibundel; service local Binder|
| LockScreen1.0.0_LockScreen.apk |10|Tiada DEX dalam eksport|
| LogoSelector_LogoSelector.apk |26|Pemilihan logo OEM; tiada kaitan terbukti dengan host panel|
| Maps_base.apk |26|Maps25.32,9 DEX; singleTask; shared UID/Google signature|
| Maps_split_config.armeabi_v7a.apk |base|10 library ARM32 termasuk libgmm-jni.so|
| Maps_split_config.en.apk |base|Resource split sepadan|
| Maps_split_config.mdpi.apk |base|Density split sepadan|
| Net_Navi_PveNaviGuide.apk |17|Pemilih/launcher navigasi; bukan enjin routing atau host panel|
| Radio_GalaRadio_integrate.apk |27|21KB; tiada DEX/library; pelaksanaan luar belum dikenal pasti|
| Sound_Effects_SoundEffects.apk |26|Kawalan audio OEM; tiada DSP diperintah|
| System_UI_MtkSystemUI.apk |27|Resources/sijil disemak; tiada DEX dalam eksport|
| Theme_PveThemeSetting.apk |17|Tema OEM; tiada grant hosting umum dibuktikan|
| video_GalaVideo.apk |19|3 DEX +15 library ARM32; Vitamio/FFmpeg; signature export bermasalah|
| VLC_base.apk |17|6 DEX +4 library ARM32; bukan companion NexaDash sedia ada|
| YGPS_YGPS.apk |8|Utiliti GPS MTK; tiada DEX dalam eksport|
| YouTube_base.apk |26|YouTube20.26.40,7 DEX; belum ada bridge NexaDash|
| YouTube_split_config.armeabi_v7a.apk |base|27 library ARM32; tandatangan sepadan|
| YouTube_split_config.en.apk |base|Resource split sepadan|
| YouTube_split_config.mdpi.apk |base|Density split sepadan|
| ZLINK_zlink_wireless.apk |17|ZLink3.9.27;14 library ARM32/protected wrapper; runtime connection unknown|
| ZlinkActivationCode_ZlinkActivationCode.apk |27|Helper activation OEM; tidak dijalankan atau diubah|

## Langkah seterusnya yang boleh menghasilkan bukti

1. Pada native8: pilih **Native app · experimental**, pilih **TizenTube** pada butang companion, kemudian **Start**. Open/Choose app dalam external controls bukan percubaan hosting.
2. Source native9 menyediakan pilihan terus **TizenTube · native panel** dan merekod requested/proxy/companion display. Ambil compatibility report selepas percubaan jika menggunakan build native9 dengan signing key yang sepadan.
3. Hanya selepas laluan native diuji, tentukan sama ada perlu ubah task behavior Cobalt atau firmware menolak display placement. Jangan dakwa companion fixed berdasarkan manifest sahaja.
4. Eksperimen Maps boleh menggunakan input API26/ARM32 ini, tetapi mesti berasingan daripada Maps sistem dan disahkan untuk rendering, carian, route/suara, input, resize dan lifecycle sebelum dianggap siap.
