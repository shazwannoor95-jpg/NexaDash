# NexaDash Preview — UI terkini dan Performance check

[Download APK Preview (~5.5MB)](https://raw.githubusercontent.com/shazwannoor95-jpg/klikdash/nexadash-native-panel-experiment/dist/preview/NexaDash-Preview-1.0.apk).

Install sebagai **app baharu**, buka ikon **NexaDash Preview**. Package `com.shazwan.nexadash.preview`, version1.0-preview1, source native11. App/data Native Lab lama kekal. Preview tiada HOME filter dan tidak menjadi launcher default; ia boleh dibuka dari Apps/Recents. Setting/layout lama tidak dimigrasikan.

## Uji prestasi tanpa ADB

1. Buka Preview dan tunggu30–60s untuk startup/cache reda. Biarkan wallpaper statik dan car/media controls untuk baseline.
2. **Settings → Performance check → Start 30 seconds**. Dialog tutup supaya dashboard boleh digunakan. Kekalkan Preview di depan; sample dibatalkan jika app ditinggalkan.
3. Selepas mesej selesai, buka **Settings → Performance check → Copy report**, paste dan hantar laporan. Labelkan sebagai `idle`.
4. Pilih YouTube web di panel, main video dahulu, kemudian ulang sample. Labelkan sebagai `web video`. Exit fullscreen jika perlu untuk membuka Settings; selepas Start boleh masuk fullscreen semula.

Tiada monitor yang berjalan kekal. Hanya dua bacaan memori dan satu callback tamat ketika sampel aktif; worker dilepaskan selepas selesai/cancel. Membuka menu sahaja tidak memulakan sampling. Hasil disimpan private sehingga pengguna menyalinnya.

## Maksud angka

- **CPU:** masa CPU proses Preview dibahagi masa sebenar, dalam unit satu core. Boleh melebihi100% jika banyak core digunakan. Ia bukan peratus keseluruhan head unit.
- **PSS:** RAM proses Preview sahaja. Renderer WebView, TizenTube/Maps dan app lain tidak termasuk. Angka ini tidak membuktikan penggunaan RAM keseluruhan video web rendah.
- **Java/native heap:** pecahan/anggaran allocator yang bertindih dengan PSS; jangan jumlahkan dengan PSS.
- **Device RAM available:** keadaan seluruh peranti, termasuk memori reclaimable; perubahan tidak semestinya disebabkan Preview.
- Dua snapshot tidak menangkap peak RAM, FPS/jank atau menjamin bebas crash. Kos sampling termasuk dalam ukuran. Tiada GC dipaksa.

## Fungsi dan had Preview

UI utama, fullscreen web dan diagnostics source terkini tersedia untuk dicuba. Kunci Preview baharu sama dengan kunci Web Test, bukan kunci cloud Native Lab lama. Companion TizenTube cloud lama tidak sepadan untuk native binding dengan Preview; packaging ini tidak membaiki hosting TizenTube, Maps atau Radio. Kekalkan ujian companion lama pada Native Lab lama. Radio/Maps dalam panel masih kerja berasingan.

Private backup signing Web Test yang telah disediakan juga boleh digunakan untuk update Preview; ia tidak memulihkan kunci Native Lab. Jangan publish private key/password. Tiada uninstall/reset data diperlukan oleh Preview.

## Validation

Signed Preview resource/Java/DEX build, signature/alignment, CRC, unique package/label/version, satu app-drawer entry, tiada HOME/update auto-launch dan task isolation disemak. Tujuh ujian formula CPU JVM lulus; source ujian Android dikompilasi dan Gradle release/debug compile lulus. Pengambilan sampel Android30s dan tingkah laku hardware belum diuji oleh proses build. Laporan head unit pengguna masih diperlukan sebelum menetapkan sasaran/optimasi prestasi.
