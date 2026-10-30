# NexaDash — Android Studio di Windows

Nama app **NexaDash**, Package ID **com.shazwan.sagadash**. Panduan ini bermula dengan emulator.
Untuk APK signed dan pemasangan head unit kemudian, ikut [panduan pemasangan](HEAD_UNIT_INSTALL_MS.md).

## 1. Install Android Studio

1. Buka https://developer.android.com/studio dan download installer Windows.
2. Jalankan installer, kemudian buka Android Studio.
3. Pilih Do not import jika tiada tetapan lama. Dalam Setup Wizard, pilih Standard.
4. Terima lesen SDK dan tunggu semua download selesai.

Java boleh diuruskan dalam Android Studio; tidak perlu memasang Java secara berasingan dahulu.

## 2. Buka projek terkini

Jika Apps masih menunjukkan senarai “Installed apps”, ikut [langkah folder baharu dan semak versi](CORRECT_BUILD_MS.md).
Versi .7 menunjukkan nombor build pada footer/tajuk menu; Apps serta Dashboard settings ialah grid ikon.
[Pemain dalam panel](INLINE_PLAYBACK_MS.md) ialah pilihan baharu, bukan hosting app asal.

Untuk update ini, gunakan folder projek **baharu**. Copy/replace fail ke folder lama boleh meninggalkan
kelas/asset model yang sudah dibuang. Folder lama boleh disimpan sebagai backup.

1. Buka [branch saga-dash-android-studio](https://github.com/shazwannoor95-jpg/NexaDash/tree/saga-dash-android-studio).
2. Klik **Code > Download ZIP**, atau [download ZIP branch ini terus](https://codeload.github.com/shazwannoor95-jpg/NexaDash/zip/refs/heads/saga-dash-android-studio).
3. File Explorer > klik kanan ZIP > **Extract All** ke folder baharu, contoh `C:\Projects\NexaDash-v8`.
4. Cari folder yang terus mengandungi `settings.gradle`, `build.gradle`, `gradlew.bat` dan `app`.
   ZIP kadang-kadang mempunyai satu folder tambahan di dalamnya; pilih folder dalaman itu.
5. Android Studio > **File > Open** > pilih folder tadi, kemudian **This Window** jika diminta.
6. Pilih Trust Project dan tunggu Gradle Sync.

Jangan copy `.idea`, `.gradle`, `build`, atau `local.properties` daripada projek/mesin lain.
Jika sudah mengubah coding sendiri, simpan perubahan itu dan bandingkan dengan ZIP baharu sebelum
memindahkannya. Signing key sendiri perlu dibackup berasingan, bukan diganti dengan key orang lain.

## 3. Tetapkan SDK dan Java

**SDK binaan** ialah Android 15/API 35. Ini tidak menaik taraf Android head unit; app masih menetapkan
Android minimum API 24. Keserasian firmware sebenar tetap perlu diuji.

1. **File > Settings > Languages & Frameworks > Android SDK**.
2. Tab SDK Platforms: tandakan **Android 15.0 — API 35 biasa**, tanpa suffix `ext`.
3. Tab SDK Tools: pastikan Build-Tools, Platform-Tools dan Android Emulator tersedia.
4. Tandakan Show Package Details, kembangkan Build-Tools dan pilih **35.0.0**.
5. Klik Apply, terima lesen dan tunggu Finish. Versi SDK lain yang sudah installed boleh dibiarkan.
6. Di panel kiri Settings, kembangkan **Build, Execution, Deployment > Build Tools > Gradle**.
   Pilih **Gradle**, bukan Gradle-Android Compiler.
7. **Distribution: Wrapper**. Di Gradle JDK pilih **JDK 17 atau 21**; `jbr-21` juga sesuai.
8. Jika tiada, pilih Download JDK > Version 17 > Download. Kemudian Apply > OK.
9. **File > Sync Project with Gradle Files** dan tunggu selesai.

Project menggunakan AGP 8.7.3 dan Gradle 8.9. Cadangan Upgrade Assistant bukan langkah wajib;
jangan upgrade/migrate secara automatik semasa mengikuti panduan ini.
`local.properties` diwujudkan mengikut SDK Windows sendiri. Jangan salin path `/workspace/android-sdk`.

## 4. Buat emulator

Kalau emulator API31 atau API32 kau sudah berfungsi, terus guna emulator itu; tak perlu buat semula.

1. **Tools > Device Manager > Create Virtual Device**.
2. Pilih **Pixel 5 > Next**. Model Pixel baharu mungkin mengehadkan pilihan Android lama.
3. Pilih **API 31 “S” — Android 12.0**, Services **Google APIs**, image **x86_64** untuk Intel/AMD.
4. Download image jika diminta, kemudian Finish.
5. Klik ▶ di sebelah emulator dan tunggu skrin utama Android.

Untuk semak ruang head unit kemudian, buat New Hardware Profile landscape **1024×600**, 7 inci,
RAM 2048MB dan image yang sama. Itu masih simulasi, bukan perkakasan TEST9278B.
Jika emulator meminta virtualisasi, ikut diagnostik Android Studio; VT-x/AMD-V atau Windows
Hypervisor Platform mungkin perlu diaktifkan pada komputer itu.

## 5. Run NexaDash

1. Pada bar atas, pilih konfigurasi **app** dan emulator yang hidup.
2. Klik ▶ hijau. Tunggu build dan install selesai.
3. Jika paparan mengiring, guna ikon Rotate pada toolbar emulator untuk menjadikannya landscape.
4. Sahkan footer menunjukkan **1.6.7-nexadash.8**. Apps dan Dashboard settings patut menunjukkan grid ikon.
   Semak Home, pilihan panel dan Edit layout.
5. Settings > Appearance: cuba Original/Synthwave. Default Static + Reduce motion tidak menganimasikan
   latar; susunan dan tindakan butang kekal. Lihat [panduan tema](SYNTHWAVE_MS.md).

Default kereta ialah **Saga image angles (lightweight)**: drag menukar lapan sudut, pinch zoom dan
double-tap reset. Ia bukan model 3D/kamera bebas. Tiada animasi idle atau decoder video untuk mod ini.
**Original Saga video** dan **Interactive 3D model** lama telah dibuang.

Ketuk pilihan paparan kereta ▾ untuk video transparent atau import pilihan sendiri.
Lihat [panduan kereta/shortcut](VIDEO_AND_SHORTCUTS_MS.md) dan [panel/Edit layout](DASHBOARD_CUSTOMIZATION_MS.md).

## 6. Permission, masa dan media

- Settings > Location permission: beri lokasi untuk GPS dan pemilihan zon solat.
- Settings > Enable media / notification access: hidupkan NexaDash jika mahu membaca sesi media.
- Mainkan media dalam app yang menyokong Android MediaSession untuk menguji mini player.
- Radio, DSP dan Bluetooth audio vendor head unit tidak dibekalkan oleh emulator biasa.
- Jam/countdown menunggu masa yang sah. Ketuk jam; hanya pilih Device time is correct jika masa
  peranti memang betul. Data lokasi mock emulator boleh ditolak untuk pengesahan masa GPS.
- Extended Controls (…) > Location boleh menghantar koordinat untuk semakan peta/GPS UI.
- Jadual rasmi SGR01/SGR02/SGR03/WLY01 tahun 2026 disertakan. Cache tamat memerlukan data baharu;
  mod offline tidak bermakna jadual tersedia untuk semua tahun.

Peta Navigation preview menggunakan tile Internet apabila dipilih dan GPS tersedia; ia bukan
navigasi turn-by-turn atau peta offline penuh. Open navigation membuka app navigasi berasingan.

## 7. APK debug untuk emulator

Pilih **Build > Build APK(s)** atau menu Build Bundle(s)/APK(s) > Build APK(s), kemudian Locate.
Output: `app\build\outputs\apk\debug\app-debug.apk`.

Debug key Windows dijana oleh Android Studio. APK ini sesuai untuk ujian. Untuk pemasangan jangka
panjang, ikut [panduan signing dan head unit](HEAD_UNIT_INSTALL_MS.md). APK cloud dan APK Windows
boleh mempunyai key berbeza; app dengan Package ID sama hanya boleh di-update dengan key yang sama.

## 8. Lokasi fail

Untuk menukar preview Saga kepada kereta sendiri, ikut [panduan8 sudut dan atlas PNG](CAR_PREVIEW_MS.md). Menggantikan aset memerlukan build semula; menu import video tidak mengimport gambar.

- `app\build.gradle`: SDK, application ID dan tetapan build.
- `AndroidManifest.xml` di root: manifest sumber utama.
- `src\com\shazwan\sagadash\DashboardActivity.java`: dashboard/panel.
- `res\drawable-nodpi\saga_angles.png`: atlas lapan sudut kereta.
- `res\drawable-nodpi\saga_synthwave.webp`: latar Synthwave; `DashAppearance.java` mengurus tetapan tema.
- `res\raw\saga_showroom_alpha.mp4`: transport video transparent pilihan.
- `TrustedClock.java`, `PrayerLocation.java`, `JakimSchedule.java`: masa, zon dan jadual solat.

Jangan edit manifest generated dalam `app\build\generated\studio`. Folder sumber root dikongsi
dengan shell build melalui sourceSets. Membina APK tidak memerlukan Blender, Python, ONNX atau GPU AI.

## 9. Jika gagal

| Ralat | Langkah |
| --- | --- |
| Add Configuration sahaja | Pastikan membuka folder dalaman yang ada settings.gradle; tunggu Sync. |
| SDK location not found | Semak SDK Manager dan sync semula. |
| Platform 35 / Build-Tools tidak ditemui | Install versi yang diminta melalui SDK Manager. |
| Gradle JDK tidak sesuai | Pilih JDK 17/21 di Build Tools > Gradle. |
| Dependency download failed | Semak Internet/proxy dan cuba sync selepas punca dibaiki; kekalkan TLS/checksum. |
| INSTALL_FAILED_UPDATE_INCOMPATIBLE | Key APK lama dan baharu berbeza. Ikut panduan signing; jangan terus uninstall data penting. |

Untuk bantuan, berikan teks ralat pertama daripada panel Build. Jangan hantar signing key,
password atau API key ke chat/GitHub.
