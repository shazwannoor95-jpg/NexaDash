# Native10 source — YouTube web fullscreen dan arkib35-part

Source version **1.6.7-nexadash.8-native10 / code42**. **Tiada APK signed native10 untuk update terus APK GitHub lama:** private signing key asal belum dipulihkan. APK unsigned hasil build bukan APK untuk dipasang. Signed APK lama tidak diubah. [Signing/source guide](NATIVE9_SOURCE_MS.md).

## Arkib baharu cukup

35 nama part01–35 diterima dan diproses sebagai satu set berasingan daripada arkib16-part lama. Extraction tamat tanpa error: **258 entries = 117 folders + 141 fail APK**, berjumlah **1,144,954,118 bytes**. Semua141 fail APK lulus ZIP CRC. Ini mengesahkan set arkib baharu lengkap dan konsisten; ia tidak bermaksud semua runtime/framework/native-library firmware telah dieksport.

APK NexaDash dalam export sepadan **byte-for-byte** dengan file GitHub bernama native8, SHA256 `ec3f1ca6abd0b56b91580e5ed04baf707454125e86636f5c97cf48557379d1b2`. Oleh itu screenshot versionName native1 disebabkan metadata lama, bukan bukti pengguna memasang file GitHub yang lain. Fix override versi telah dimasukkan dalam source native9.

Chrome138.0.7204.179 minAPI26 ARM32 terdapat dalam set ini, bersama Google WebView donor stub63 dan MTK WebView58. Fail terpasang tidak menentukan provider yang aktif; native9+ compatibility report boleh membaca provider runtime tanpa membuka browser baharu.

## Rakaman dan fullscreen

Dua rakaman diproses secara tempatan untuk pemeriksaan UI (10.85s dan21.50s); rakaman/foto asal tidak diterbitkan. Rakaman Radio menunjukkan menekan Open Radio dalam external controls lalu Activity FM memenuhi skrin. Itu bukan percubaan native hosting. Rakaman YouTube menunjukkan carian/halaman video di panel; playback berterusan, audio, kelancaran dan fullscreen pada hardware belum diukur oleh semakan source ini.

Punca kod fullscreen disahkan: handler lama memanggil `onCustomViewHidden()` sebaik sahaja WebView meminta fullscreen. Source native10 menggantikannya dengan:

- View video custom milik WebView yang sama diletakkan di atas Activity, memenuhi skrin; tiada WebView/player kedua.
- **Exit fullscreen** atau **Back** mengembalikan paparan ke panel/browser yang sama.
- Flags window/system UI dipulihkan selepas keluar. Tiada orientation lock tambahan.
- Permintaan berganda/reentrant ditangani; pause, Stop, mode switch, detach, renderer termination dan destroy membersihkan overlay/browser.
- Polisi HTTPS, gesture untuk playback, sekatan file/content/download dan permission denial dikekalkan.

Ini ialah perubahan source; YouTube/provider/head unit masih perlu diuji. Playback web ialah berasingan daripada native companion TizenTube/YouTube.

## Radio selepas semakan penuh

Carian **187 regular DEX / 571,003 class-definition entries** tidak menemui definisi `com.acloud.stub.localradio.QtActivity`, termasuk dalam CarSet/CanBus. Ia juga tidak menemui `libRadioInterface.so`/`libfmjni.so` di dalam mana-mana APK. Local Binder yang ditemui bukan interface AIDL cross-process untuk NexaDash. Semakan ini tidak membuktikan fail tersebut tiada pada firmware; lokasinya masih perlu dikenal pasti secara runtime. [Laporan Radio penuh](RADIO_FULL_ARCHIVE_35_REVIEW.md).

## Validation

Resource/Java/DEX unsigned build, zip alignment dan compilation semua test source lulus. Gradle unsigned release, debug app Java dan Demo Java compile lulus. Manifest output native10/code42 disahkan. Ujian fullscreen custom-view sintetik telah ditambah tetapi belum dijalankan pada emulator selepas environment reset. Tiada ujian YouTube sebenar/head unit atau radio hardware dilakukan oleh build ini.
