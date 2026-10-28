# Pasang Saga Dash pada head unit — panduan beginner

Untuk **TEST9278B, 1024×600, RAM 4GB/flash 32GB** yang ditunjukkan dalam screenshot. Menu awal dilabel 12.0; laporan app pada unit
kemudian menunjukkan API27 (Android8.1), versi string9.1 dan armeabi-v7a/armeabi. Sambungan ADB
dan hosting app belum diuji pada perkakasan itu. Ikut langkah ini apabila sudah
bersedia untuk ujian head unit. Nama app **Saga Dash**, Package ID **com.shazwan.sagadash**.

## Pilih satu laluan APK

| Laluan | Cara mendapat APK | Key untuk update seterusnya |
| --- | --- | --- |
| APK signed cloud yang disediakan | Download fail siap di bawah; tidak perlu Android Studio untuk install. | Update mesti ditandatangani dengan key cloud yang sama. |
| Build sendiri pada Windows | Android Studio, kemudian hasilkan APK signed menggunakan key sendiri. | Simpan dan guna key sendiri yang sama setiap kali. |

Jangan berselang-seli key untuk pemasangan dengan Package ID sama. Cloud key, Windows debug key dan
Windows release key boleh berbeza. Ini bukan ralat pada model head unit.

## A. Download APK signed yang disediakan

Download [SagaDash-1.6.7-sagadash.6.apk](https://raw.githubusercontent.com/shazwannoor95-jpg/klikdash/saga-dash-android-studio/dist/SagaDash-1.6.7-sagadash.6.apk).
Fail ini berada dalam branch edisi Saga Dash, bukan release APK KlikDash upstream.
Versi .6 menambah grid ikon Dashboard settings dan nombor versi pada footer/menu. Versi .5 menambah panel kaca statik dan grid ikon Apps. Versi .4 menambah [semakan app asal dalam panel](APP_PANELS_MS.md); embedding app belum ditambah.

Simpan fail `.apk` itu pada komputer. Jika pautan raw disekat rangkaian, buka
[halaman fail GitHub](https://github.com/shazwannoor95-jpg/klikdash/blob/saga-dash-android-studio/dist/SagaDash-1.6.7-sagadash.6.apk)
dan klik **Download raw file**. Jangan rename ZIP kepada APK. Private signing key tidak disertakan
dalam download/projek. [Checksum dan maklumat build](../dist/build-info.json) disediakan untuk semakan.

## B. Hasilkan APK signed sendiri pada Windows

1. Ikut [panduan Android Studio](WINDOWS_ANDROID_STUDIO_MS.md), buka ZIP terkini dalam folder baharu
   dan pastikan app boleh berjalan pada emulator dahulu.
2. Pilih **Build > Generate Signed App Bundle or APK**.
3. Pilih **APK > Next**. Module ialah **app**.
4. Di Key store path, pilih **Create new…** jika belum mempunyai key sendiri.
5. Simpan ke tempat di luar projek, contoh `C:\SagaDash-Keys\sagadash-release.jks`.
6. Tetapkan password yang kuat. Alias boleh `sagadash`; tetapkan key password dan validity sekurang-kurangnya 25 tahun.
7. Isi maklumat sijil yang diminta. Klik OK, pilih keystore/alias itu dan masukkan password pada wizard.
8. Klik Next, pilih build variant **release**, kemudian **Create/Finish**. Jika pilihan signature
   versions ditunjukkan, V1 dan V2 boleh ditandakan untuk julat Android yang disokong.
9. Bila build selesai, klik **Locate**. Bergantung pada versi IDE, fail mungkin berada di
   `app\release\app-release.apk` atau `app\build\outputs\apk\release\app-release.apk`.
   Gunakan APK signed yang ditunjukkan oleh wizard, bukan fail `unsigned`.

Backup fail `.jks`, alias dan password dalam simpanan sendiri yang selamat. Kehilangan key boleh
menghalang update pemasangan sedia ada. Jangan upload key/password ke GitHub atau hantar dalam chat.
Untuk update akan datang, pilih keystore sedia ada dalam wizard—jangan Create new lagi.

## C. Pindah dan install

1. Copy APK signed ke USB yang boleh dibaca head unit. Letakkan di folder yang mudah dicari.
2. Pada head unit, buka File Manager, cari fail APK dan ketuk.
3. Jika diminta **Install unknown apps**, benarkan untuk File Manager yang sedang memasang itu,
   kemudian kembali ke installer. Pada sesetengah firmware, nama menu mungkin Unknown sources.
4. Klik **Install > Open**. Untuk ujian pertama, buka Saga Dash sebagai app dahulu; belum perlu
   menjadikannya default launcher.
5. Semak paparan landscape, lapan sudut imej kereta, panel dan ikon. Default kereta ialah imej statik,
   bukan Original Saga video atau Interactive 3D model lama.
   Settings > Appearance membolehkan pertukaran Original/Synthwave. Kekalkan Static dan Reduce motion
   untuk permulaan ringan; ini tidak mengubah susunan panel atau pilihan app.
6. Setelah ujian asas memuaskan, **Settings Saga Dash > Set as default launcher** membuka tetapan
   Home Android. Pilih Saga Dash jika mahu menggunakannya sebagai launcher.

Untuk kembali ke launcher asal, buka Android Settings > Apps > Default apps > Home app dan pilih
launcher bawaan. Susunan menu vendor boleh berbeza; jangan tukar MCU/CAN/factory settings untuk
memasang app ini. Jika firmware tidak menyediakan pilihan Home yang berfungsi, catat keadaan itu
untuk semakan khusus unit tersebut.

## D. Tetapkan app native yang betul

Di luar mod Edit, tekan lama shortcut > **Choose app**. Pilih label yang benar-benar ada:

| Fungsi | Label yang mungkin muncul |
| --- | --- |
| Bluetooth audio/phone | BT / Bluetooth |
| Tetapan kenderaan | CarSettings / CarSet |
| Muzik | Music / player pilihan |
| Video fail | Video / VLC |
| CarPlay/Android Auto vendor | ZLINK |
| Radio | Radio / FM |
| DSP/EQ | App DSP/EQ vendor yang diketahui |

Pengesanan label hanya memilih app apabila padanan tidak kabur; package ID tidak diteka daripada
screenshot. Jika ada beberapa padanan, pilih sendiri. **CarSet tidak dipilih sebagai DSP secara automatik.**
Membuka app vendor tidak menjamin metadata radio/Bluetooth boleh dibaca oleh Saga Dash.

Untuk mini player, hidupkan Settings > Enable media / notification access, kemudian mainkan lagu
melalui app asal. Maklumat dan Play/Pause bergantung pada sesi yang firmware/app itu dedahkan.
TizenTube ialah app berasingan; kesesuaian ABI, sentuhan dan playback perlu disemak sebelum install.

### TizenTube pilihan: Windows / install manual

[TizenTube Cobalt v2.0.3 rasmi](https://github.com/reisxd/TizenTubeCobalt/releases/tag/v2.0.3)
memerlukan **Android 7.0/API24 ke atas dan OpenGL ES2.0**. Release itu menyediakan APK ARMv7
32-bit serta ARM64; jangan pilih berdasarkan label Android12 sahaja. API/ABI sebenar head unit
ini belum disahkan dan TizenTube **belum dipasang atau diuji pada head unit ini**.

1. Apabila bersedia menyemak unit sebenar, sambungkan melalui ADB dan pilih serial yang betul.
   Dari folder Android SDK `platform-tools` di Windows, jalankan
   `adb -s SERIAL shell getprop ro.build.version.sdk` dan
   `adb -s SERIAL shell getprop ro.product.cpu.abilist`; gantikan `SERIAL` dengan serial unit sendiri.
2. Jika API sekurang-kurangnya24 dan ABI ialah `armeabi-v7a` tanpa `arm64-v8a`, download
   **cobalt-arm.apk** dari Assets release rasmi itu. Jika ABI menyokong `arm64-v8a`, pilih
   **cobalt-arm64.apk**. APK ARM64 tidak boleh digunakan pada Android yang hanya menyokong ARMv7.
3. Copy APK yang sepadan ke USB dan pasang melalui File Manager seperti langkah C. Kejayaan
   install masih belum membuktikan keserasian sentuhan, video decoder atau sesi media.

Helper Bash `tools/install_tizentube.sh` menyemak API24, memilih ABI dan mengesahkan hash APK
yang dipin serta tandatangan sebelum memasang pada serial yang ditetapkan. Ia tidak dijalankan
secara automatik oleh Saga Dash dan tidak menyemak semua keupayaan GPU/perkakasan.

Jika tidak disokong atau kurang sesuai, gunakan **YouTube yang sudah kelihatan dalam screenshot**:
tekan lama shortcut YouTube > Choose app > YouTube. Untuk panel, pilih **YouTube controls**,
bukan TizenTube controls. TizenTube kekal pilihan berasingan dan tidak dibundel dalam APK Saga Dash.

## E. Waktu solat dan ujian awal

1. Beri Location permission dan hidupkan lokasi/GPS. Tunggu fix di tempat dengan penerimaan satelit.
2. Pastikan Android date/time betul. Saga Dash boleh mengesahkan masa GPS untuk dirinya, tetapi
   tidak menukar jam sistem Android/app lain.
3. Jika belum ada GPS dan jam peranti memang betul, ketuk jam > Device time is correct.
4. Settings > Prayer zone & timetable: guna GPS atau pilih zon manual yang betul.

| Kawasan umum | Kod manual contoh |
| --- | --- |
| Kuala Selangor | SGR02 |
| Shah Alam / Petaling | SGR01 |
| Klang / Kuala Langat | SGR03 |
| Kuala Lumpur / Putrajaya | WLY01 |

Lembah Klang merentasi beberapa zon; jangan menggunakan satu kod untuk semua tempat. Dekat
sempadan, rujuk lokasi sebenar dan jadual rasmi. Cache yang disertakan ialah tahun 2026 dan perlu
jadual baharu selepas tempohnya tamat. GPS dan jadual tersimpan tidak memerlukan Internet;
peta Navigation preview, refresh jadual serta lyrics mempunyai keperluan Internet berasingan.

Sahkan juga GPS, radio/BT audio, reverse camera, steering buttons dan sleep/wake pada unit sebenar.
Keputusan emulator tidak menjamin semua fungsi firmware, penggunaan RAM/CPU atau kelancaran head unit.

## Jika install gagal

- **App not installed / conflicting package / UPDATE_INCOMPATIBLE:** key APK lama dan baharu mungkin
  berbeza. Gunakan key asal untuk update. Uninstall salinan lama akan membuang data/settings app itu;
  buat hanya jika memang mahu reset dan sudah menerima kehilangan data tersebut.
- **Parse error / incompatible Android:** semak bahawa fail download benar-benar `.apk` lengkap,
  bukan ZIP/halaman web, kemudian catat versi API Android sebenar dan teks ralat.
- **Shortcut tidak membuka app:** tekan lama > Choose app. Emulator tidak mempunyai semua app OEM.
- **Lagu dimainkan tetapi metadata kosong:** aktifkan media access; jika masih kosong, vendor mungkin
  tidak menerbitkan Android MediaSession dan memerlukan integrasi khusus.

Untuk bantuan, berikan teks ralat dan screenshot tanpa key/password. Tidak perlu root, memasang
firmware baharu atau mengubah konfigurasi CAN untuk mencuba APK ini.
