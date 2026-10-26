# Saga Dash — panduan Android Studio di Windows

Panduan ini untuk beginner. Sasaran pertama ialah app berjalan dalam emulator. Ujian head unit
serta signing key release dibuat kemudian. Nama app Saga Dash; Package ID com.shazwan.sagadash.

## 1. Install Android Studio

1. Buka https://developer.android.com/studio pada komputer Windows.
2. Download pemasang Android Studio untuk Windows, buka fail .exe dan ikut langkah pemasangan.
3. Buka Android Studio. Jika ditanya sama ada hendak import setting lama, pilih Do not import.
4. Dalam Setup Wizard, pilih Standard dan terima lesen SDK yang dipaparkan.
5. Biarkan SDK dan Android Emulator siap dimuat turun. Sambungan Internet diperlukan untuk setup ini.

Android Studio sudah menyediakan Java sendiri. Tidak perlu install Java secara berasingan.

## 2. Sediakan folder projek lengkap

Gunakan branch [saga-dash-android-studio](https://github.com/shazwannoor95-jpg/klikdash/tree/saga-dash-android-studio).
Pada halaman branch itu, klik butang Code > Download ZIP. Branch main lama tidak mengandungi
semua perubahan Saga Dash ini. Selepas download:

1. Download ZIP projek/branch tersebut melalui GitHub.
2. Dalam File Explorer, klik kanan ZIP > Extract All.
3. Letakkan projek di folder ringkas, contohnya C:\Projects\SagaDash.
4. Pastikan folder itu mengandungi settings.gradle, build.gradle, gradlew.bat dan folder app.
   Jika ZIP menghasilkan folder tambahan di dalamnya, buka folder dalaman yang ada settings.gradle.
5. Buka Android Studio > Open > pilih folder projek tersebut. Jangan pilih folder app sahaja.
6. Trust Project jika Android Studio meminta pengesahan untuk projek yang baru dibuka.
7. Tunggu Gradle Sync. Kali pertama mengambil masa kerana plugin/dependency perlu dimuat turun.

## 3. Tetapkan SDK dan Java

1. Buka Tools > SDK Manager (atau More Actions > SDK Manager pada welcome screen).
2. Di SDK Platforms, pilih Android 15 / API 35.
3. Di SDK Tools, tandakan Android SDK Build-Tools, Android SDK Platform-Tools dan Android Emulator.
4. Jika perlu, tandakan Show Package Details dan pastikan Build-Tools 35.0.0 tersedia.
5. Klik Apply dan terima lesen.
6. Buka File > Settings > Build, Execution, Deployment > Build Tools > Gradle.
7. Di Gradle JDK, pilih JDK bawaan Android Studio / Embedded JDK / GRADLE_LOCAL_JAVA_HOME.
   Pilih JDK 17 atau 21. Jika JDK bawaan versi lain dan Gradle menolaknya, pilih Download JDK
   dalam senarai Gradle JDK dan download versi 17; Android Studio akan menguruskannya.
8. Klik OK, kemudian Sync Project with Gradle Files.

Fail local.properties akan diwujudkan oleh Android Studio mengikut lokasi SDK Windows kau.
Jangan salin sdk.dir=/workspace/android-sdk daripada mesin cloud.

## 4. Buat emulator

1. Buka Tools > Device Manager > Create Virtual Device.
2. Untuk permulaan mudah, pilih sebuah telefon Pixel yang tersedia.
3. Pilih system image x86_64 yang sesuai dengan komputer Intel/AMD, Android 10/API 29 atau lebih baru.
4. Download image jika belum ada, klik Next kemudian Finish.
5. Klik butang Play pada emulator dan tunggu Android selesai boot.

Untuk semak saiz head unit kemudian, buat New Hardware Profile dengan skrin landscape 1024×600,
7 inci, RAM 2048 MB dan image yang sama. Ini masih simulasi, bukan pengesahan firmware TEST9278B.
Jika Windows/Android Studio menyatakan virtualisasi belum aktif, ikut diagnostik emulator yang
dipaparkan. Sesetengah komputer memerlukan VT-x/AMD-V di BIOS dan Windows Hypervisor Platform.

## 5. Jalankan Saga Dash

1. Pada bar atas Android Studio, pilih konfigurasi app.
2. Di sebelahnya, pilih emulator yang telah hidup.
3. Klik Run (segitiga hijau). Tunggu proses build dan pemasangan.
4. Saga Dash akan dibuka dalam landscape.
5. Semak Home, Apps, Settings, putaran model kereta dan dialog masa dengan mengetuk jam.

Klik-drag kereta untuk memutarnya. Double-click pada panel kereta untuk reset.
Pinch memerlukan gesture multitouch emulator; gunakan kawalan multitouch emulator jika tersedia.
Untuk GPS, gunakan Extended Controls (...) > Location dan hantar koordinat lokasi.
Data mock emulator mungkin ditolak untuk pengesahan masa GPS; gunakan pilihan pengesahan jam
peranti dalam dialog Clock untuk semakan UI sahaja. Ini tidak mengesahkan GPS fizikal.

## 6. Permission dan paparan yang dijangka

- Settings > Location permission: beri lokasi untuk kelajuan dan pemilihan zon GPS.
- Settings > Enable media / notification access: hidupkan Saga Dash jika mahu membaca sesi media.
- Mainkan media dalam app lain yang menyokong Android MediaSession untuk menguji mini player.
- Emulator biasa tidak membekalkan radio kereta, Bluetooth audio vendor, DSP atau CAN.
  Mesej sumber tiada/unsupported untuk komponen itu bukan bukti bahawa fungsi fizikal rosak.
- Jam/countdown solat menunggu masa yang sah. Ketuk jam untuk melihat sumber masa dan, jika
  jam peranti memang betul, pilih Device time is correct bagi semakan tanpa satelit.
- Jadual rasmi SGR01, SGR02, SGR03 dan WLY01 tahun 2026 sudah disertakan. Data baharu diperlukan
  selepas tempoh cache tamat. Pilihan zon manual juga ada di Prayer location & offline data.

## 7. Keluarkan APK debug dahulu

1. Pilih Build > Build Bundle(s) / APK(s) > Build APK(s), atau menu Build APKs yang setara.
2. Bila selesai, klik Locate pada notifikasi build.
3. APK debug berada di app\build\outputs\apk\debug\app-debug.apk.

APK debug ditandatangani secara automatik menggunakan debug key komputer itu. Ia sesuai untuk
ujian emulator. Signing key release sendiri dan backup-nya akan diterangkan sebelum pemasangan
jangka panjang. Projek tidak menyertakan private signing key atau kata laluan mesin cloud.

## 8. Lokasi fail untuk belajar/edit

Pilih paparan Project dalam panel kiri jika struktur Android mengelirukan:

- app\build.gradle: versi SDK, app ID dan tetapan build.
- AndroidManifest.xml di root: senarai komponen dan permission; ini manifest sumber utama.
- src\com\shazwan\sagadash\DashboardActivity.java: susun atur dashboard.
- src\com\shazwan\sagadash\SagaView.java: viewer model 3D dan gesture.
- assets\vehicle\saga-vvt.mesh: model yang sudah siap dimasukkan dalam APK.
- TrustedClock.java, PrayerLocation.java, JakimSchedule.java: masa, zon GPS dan jadual.
- res: teks dan resource Android.

Gradle menghasilkan manifest tanpa atribut package di app\build\generated\studio.
Edit manifest root, bukan fail generated. Layout folder root dikekalkan supaya skrip build sedia
ada masih boleh digunakan; Android Studio membaca folder itu melalui sourceSets.
Tidak perlu install Blender atau Python untuk membina APK daripada asset yang sudah tersedia.

## 9. Jika build gagal

- SDK location not found: buka SDK Manager, pastikan SDK sudah dipasang dan sync semula.
- Failed to find Platform SDK35/Build-Tools: install versi yang diminta melalui SDK Manager.
- Gradle JDK: pilih JDK bawaan Android Studio yang sesuai, bukan Java8.
- Dependency download failed: semak Internet/proxy komputer dan cuba Sync semula selepas punca dibaiki.
  Jangan matikan semakan TLS atau checksum.
- INSTALL_FAILED_UPDATE_INCOMPATIBLE: APK dengan ID yang sama pernah dipasang menggunakan key lain.
  Jangan uninstall app berdata penting secara membuta tuli. Pada emulator ujian kosong, salinan lama
  boleh dibuang sebelum mencuba APK dari komputer sendiri; data salinan itu akan hilang.

Untuk bantuan, berikan teks ralat pertama daripada panel Build. Signing key, password dan API key
jangan dihantar dalam chat atau dimasukkan ke GitHub.

## 10. Video kereta dan shortcut sendiri

Ikut [panduan video dan shortcut](VIDEO_AND_SHORTCUTS_MS.md) untuk import MP4 daripada Windows,
bertukar antara video/3D dan menetapkan Music, Radio atau DSP dengan tekan lama.
