# Paparan kereta dan shortcut — Saga Dash

Paparan awal sekarang ialah **Saga image angles (lightweight)**: lapan imej sudut kereta yang
dihasilkan daripada rujukan Saga merah yang diberi, dengan plat **NEXA**. Ia bukan model 3D
atau kamera bebas. Tiada animasi berjalan apabila kereta tidak disentuh.

- Drag kiri/kanan untuk bertukar antara lapan sudut; urutan berulang selepas sudut terakhir.
- Pinch untuk zoom dan double-tap untuk kembali ke paparan asal.
- Imej menggunakan atlas `res/drawable-nodpi/saga_angles.png`, disampel semasa decode untuk
  mengehadkan penggunaan bitmap. Tiada mesh 3D, decoder video atau GLES diperlukan untuk mod ini.
- **Original Saga video** dan **Interactive 3D model** versi lama telah dibuang daripada pilihan app.
  Imej render boleh berbeza sedikit daripada kereta sebenar; ia bukan CAD pengeluar.

## Pilih paparan kereta

Di panel **Car preview**, ketuk butang View/Angles/Cutout dengan simbol **▾** di atas kereta.
Pilih salah satu:

1. **Saga image angles (lightweight)** — pilihan utama untuk penggunaan sumber yang rendah.
2. **Transparent Saga video** — rakaman kereta tanpa latar; ketuk untuk Play/Pause.
3. **Choose car video…** — import video lain dari peranti.
4. **Imported video preview** — muncul jika video sendiri telah disimpan.

Video transparent dan import tidak auto-play. Decoder hanya diperlukan semasa mod video digunakan;
meninggalkan panel/app melepaskan sumber video. Gerakan video mengikut rakaman, bukan putaran bebas.
Bunyi preview dimatikan dan preview tidak membuat MediaSession atau meminta audio focus.

Untuk menukar panel kiri kepada Navigation/YouTube/TizenTube/Media player, atau panel kanan kepada
Navigation/Radio, ikut [panduan panel dan susunan](DASHBOARD_CUSTOMIZATION_MS.md).

## Import MP4 daripada Windows (pilihan)

Tidak perlu import video untuk menggunakan lapan imej sudut atau video transparent yang disertakan.
Untuk video lain:

1. Hidupkan emulator. Drag MP4 dari File Explorer ke skrin emulator; fail biasanya masuk ke Download.
2. Jika drag-and-drop tidak tersedia, buka Android Studio > View > Tool Windows > Device Explorer,
   pilih emulator, buka `/sdcard/Download` atau `/storage/emulated/0/Download`, klik kanan > Upload.
3. Dalam pilihan paparan kereta, klik **Choose car video…**.
4. Dalam pemilih fail Android, buka ☰ > Downloads dan pilih MP4.
5. Tunggu import, kemudian ketuk preview untuk Play/Pause.

MP4 H.264 720p disyorkan. Had import: 32 MB, dua minit dan sehingga 4K; sokongan codec sebenar
bergantung pada firmware. App menyimpan salinan dalam storan peribadinya. Import gagal mengekalkan
fail sebelumnya. Memilih video biasa tidak membuang latarnya secara automatik.

## Apps, Music, Radio, Bluetooth dan DSP

**Apps / Installed apps** ialah senarai app yang benar-benar dipasang. Emulator telefon tidak
mempunyai perkakasan atau app radio/DSP/Bluetooth audio vendor head unit.

Di luar mod Edit, **tekan lama shortcut > Choose app** untuk memilih app yang sesuai. Pilihan itu
disimpan. **Use automatic** membuang pilihan manual dan mencuba pengesanan semula.

| Shortcut | Contoh label app pada head unit |
| --- | --- |
| Bluetooth | BT / Bluetooth |
| Vehicle settings | CarSettings / CarSet |
| Music | Music, atau player yang dipasang |
| Video | Video / VLC |
| Car Link | ZLINK |
| Radio | Radio / FM, jika app itu ada |
| DSP / Equalizer | Pilih app DSP/EQ vendor yang sebenar |

Label membantu pengesanan; screenshot tidak menetapkan package ID sebenar. Jika beberapa app
sepadan atau pengesanan tidak pasti, pilih app sendiri. **CarSet tidak dianggap sebagai DSP.**
Music/Video juga boleh membuka fail melalui player yang menyokongnya. Video tidak dialihkan ke
YouTube secara automatik; YouTube dan TizenTube ialah sumber berasingan.

TizenTube masih app berasingan. Membuka Radio/BT sahaja tidak menjamin metadata boleh dibaca:
firmware perlu menerbitkan sesi Android atau menyediakan integrasi vendor. Lyrics memerlukan
sumber berasingan dan boleh dihidupkan dalam Settings.

## Update projek

Gunakan [ZIP projek terkini dalam folder baharu](WINDOWS_ANDROID_STUDIO_MS.md#2-buka-projek-terkini).
Jangan sekadar copy folder `src`/`res` ke projek lama: fail model lama boleh tertinggal dan masuk
semula dalam build. Panduan [pemasangan head unit](HEAD_UNIT_INSTALL_MS.md) menerangkan APK signed.
