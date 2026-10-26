> **Update transparent:** ikut [panduan ini](TRANSPARENT_VIDEO.md#windows-update), copy folder `src` dan `res`,
> kemudian pilih **Transparent Saga video**. Video asal dan pilihan 3D masih ada.

# Video kereta dan shortcut — panduan Windows

S/D dahulu ialah singkatan Saga Dash. Sidebar kini memaparkan Saga Dash dengan jelas.
Apps / Installed apps menunjukkan app yang benar-benar dipasang pada peranti itu.
Emulator telefon tidak mempunyai radio FM, DSP atau Bluetooth audio vendor head unit.

## Update projek sedia ada

1. Stop app dalam Android Studio.
2. Buka https://github.com/shazwannoor95-jpg/klikdash/tree/saga-dash-android-studio dan klik Code > Download ZIP.
3. Extract ZIP ke folder **lain**, bukan ke dalam folder projek yang sedang dibuka.
4. Jika belum mengubah coding sendiri, copy folder `src` daripada ZIP baharu ke folder root projek lama
   (folder yang ada `settings.gradle`). Pilih Replace untuk fail yang sama. Ini menambah
   `DashShortcuts.java` dan `SagaShowroom.java`, serta menggantikan `DashboardActivity.java`.
5. Copy **`res/raw/saga_showroom.mp4`** daripada ZIP baharu ke lokasi sama dalam projek lama.
   Fail ini diperlukan kerana video kini disertakan terus dalam APK.
6. Untuk model 3D versi terkini juga, copy folder `assets/vehicle` ke lokasi sama dalam projek lama.
7. Klik Run dalam Android Studio. Tidak perlu uninstall app, upgrade Gradle, ganti `local.properties`
   atau ulang setup SDK/JDK. Jangan ganti signing key atau folder `.idea`.

Jika sudah membuat perubahan coding sendiri, semak perbezaan tiga fail Java itu sebelum menggantikannya.

## Video yang sudah ada dalam APK

Video asal kini dimasukkan terus sebagai `res/raw/saga_showroom.mp4`: MP4 H.264, 1280×720,
24fps, 10 saat, kira-kira 3.8 MB. Semua 240 frame gambar sama dengan video asal; hanya audio dibuang.
Plat **NEXA** dikekalkan atas permintaan pemilik, termasuk pada label model 3D.

Install baru menggunakan video transparent sebagai paparan awal. Jika projek lama masih menunjukkan model,
klik **3D ▾ > Transparent Saga video**. Pilih **Original Saga video** jika mahu latar studio asal. Tak perlu pindah MP4 ke emulator atau import fail untuk video ini.
Pilihan 3D masih tersedia melalui **Video ▾ > Interactive 3D model**.

Fail video/plat ini memang disertakan dalam source dan APK edisi peribadi yang diminta pemilik.
Video sendiri yang diimport kemudian kekal dalam storan peribadi peranti dan menggantikan
preview terbina dalam semasa playback.

## Guna video kereta lain

Video yang dihantar dalam chat ialah rakaman 10 saat. Ia boleh dimainkan sebagai preview,
dengan gerakan/sudut yang sudah dirakam. Ia bukan model yang boleh diputar bebas. Latar studio,
plat nombor dan tulisan/grafik dalam rakaman juga akan kelihatan. Dua upload tersebut ialah fail sama.
Preview tidak auto-loop kerana hujung dan permulaan rakaman mempunyai framing berlainan.

Untuk menggantikan preview dengan video lain, import fail sendiri:

1. Pastikan MP4 asal ada pada komputer Windows kau.
2. Hidupkan emulator. Cuba drag fail MP4 dari File Explorer ke skrin emulator; fail biasanya
   disalin ke Download. Jika drag-and-drop tidak tersedia, gunakan langkah alternatif di bawah.
3. Dalam Saga Dash, klik **3D ▾ / Video ▾ / Cutout ▾** di atas panel kereta, sebelah PROTON SAGA.
4. Klik **Choose car video…**.
5. Dalam pemilih fail Android, buka menu ☰ > Downloads dan pilih MP4 tersebut.
6. Tunggu import. Panel bertukar ke **Video ▾**. Ketuk video untuk Play/Pause.
7. Untuk kembali kepada model yang boleh diputar, klik **Video ▾ > Interactive 3D model**.
   Pilihan **Imported video preview** membolehkan kau kembali kepada video yang disimpan.

Alternatif untuk menghantar MP4 ke emulator:

1. Android Studio > View > Tool Windows > Device Explorer.
2. Pilih emulator Pixel 5 yang sedang hidup.
3. Buka folder `/sdcard/Download` (atau `/storage/emulated/0/Download`).
4. Klik kanan folder Download > Upload, kemudian pilih MP4 Windows kau.
5. Kembali ke Saga Dash dan ikut langkah Choose car video di atas.

App menyalin video ke storan peribadinya, jadi fail asal Downloads tidak perlu kekal selepas import
berjaya. Import yang gagal mengekalkan video sebelumnya. Had: 32 MB, 2 minit dan sehingga 4K;
MP4 H.264 720p disyorkan untuk head unit ini. Keserasian codec sebenar masih bergantung pada firmware.

Video preview bermula dalam keadaan pause, tanpa bunyi, tidak meminta audio focus atau membuat
MediaSession. Keluar dari app atau tukar ke 3D akan melepaskan decoder; video tidak berjalan di
background. Sesi Spotify/YouTube/radio untuk mini player tetap merupakan sumber berasingan.

## Tetapkan Music, Radio, DSP dan shortcut lain

- **Tekan lama** ikon Music, Bluetooth, Radio, Video, Files, Equalizer, Car Link atau DSP.
- Pilih app yang sesuai daripada senarai installed apps. Pilihan disimpan untuk shortcut itu sahaja.
- Pada emulator, radio/DSP head unit memang tidak dipasang. Dialog akan menerangkan keadaan ini;
  butang Choose app hanya boleh memilih app yang memang ada, bukan memasang perkakasan radio/DSP.
- Music boleh membuka fail audio melalui Open audio file. Video boleh membuka fail video melalui
  Open video file. Peranti masih memerlukan player yang menyokong fail tersebut.
- YouTube/TizenTube ada shortcut sendiri di bahagian atas. Video tidak lagi membuka YouTube secara
  automatik. TizenTube ialah app berasingan dan belum dipasang/diuji pada head unit kau.
- Tekan lama shortcut semula > Use automatic untuk membuang assignment sendiri.
- Butang Navigation dan Vehicle settings juga boleh ditetapkan dengan tekan lama.

Assignment radio/Bluetooth/DSP perlu dibuat pada head unit menggunakan app vendor yang sebenar.
Membuka app itu sahaja tidak menjamin firmware akan membekalkan metadata kepada mini player.


## Format yang diperlukan

| Tujuan | Format |
| --- | --- |
| Video preview dalam APK | MP4 dengan video H.264; 720p / 24–30fps sesuai untuk head unit ini. Video asal sudah sesuai. |
| Model 3D untuk integrasi akan datang | GLB (glTF 2.0), dengan mesh, normals, UV dan tekstur/material sekali. |

MP4 tidak menyimpan mesh atau tekstur 3D. Untuk model yang boleh diputar bebas, model Saga perlu
dibina atau diperoleh secara berasingan. Fail GLB kemudian perlu diintegrasikan dengan renderer;
app semasa tidak menyediakan import GLB melalui pemilih fail. Jangan sekadar rename MP4 kepada GLB.
