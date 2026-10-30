# Radio FM — export fail sistem tanpa ADB

**Next1.5 menambah alat export fail. Next1.8 kini menambah [kawalan FM dalam panel](RADIO_PANEL_MS.md); bunyi/tuner fizikal belum disahkan.** Tiada arahan tuner, bind servis OEM, root, penggantian app sistem atau upload automatik dibuat oleh alat ini.

## ZIP export pemilik sudah diterima — 5/5

**NexaDash-Radio-files.zip lengkap, CRC/SHA256 sepadan dan semua lima fail berjaya disalin pada head unit.** VDEX sudah dipulihkan kepada DEX sah dan dua library ARM32 sudah dianalisis. Radio asal ialah dispatcher kepada dua implementation FM sebenar. [Penemuan lengkap](RADIO_RUNTIME_FILES_REVIEW.md).

Untuk sambungan semakan ini, **tidak perlu export ZIP atau APK sekali lagi**:

1. Update kepada **[NexaDash Next1.8](https://raw.githubusercontent.com/shazwannoor95-jpg/NexaDash/main/dist/next/NexaDash-Next-1.8.apk)** atas Next sedia ada; package/key sama.
2. Buka **Settings → Radio runtime paths → Copy report**.
3. Hantar teks report. Ia kini merekod property pemilih FM, komponen yang dipilih firmware, UID app dan metadata/flag akses bagi tiga driver yang dikenal pasti.

Semakan itu tidak membuka driver, menukar frekuensi, menjalankan library OEM atau menulis setting. Property/metadata tidak boleh mengesahkan kawalan hardware atau hosting. Jika property ditolak, backend dilaporkan belum diketahui. Tiada app tambahan atau ADB diperlukan. Companion Maps/TizenTube tidak perlu dipasang semula untuk update laporan ini.

## Apa yang ditemui dalam upload FM terdahulu?

Kedua-dua `fm.part1.rar` dan `fm.part2.rar` lengkap dan lulus extraction/CRC. Lima APK di dalamnya **identik byte demi byte** dengan export35-part terdahulu. Backup App Manager Radio mempunyai APK dan direktori `oat` kosong; fail `.odex/.vdex` tidak berada dalam backup itu. Dua library FM juga tidak disertakan. `am.jar`/`main.jar` ialah alat sokongan App Manager, bukan implementation Radio.

Jadi tidak perlu ulang **Select all → APK Extractor/backup**. [Rekod semakan](qa/radio-fm-archive-review.json) · [Analisis kod Radio terdahulu](RADIO_FULL_ARCHIVE_35_REVIEW.md).

Laporan runtime daripada TEST9278B menunjukkan lokasi APK, dua fail preoptimized dalam `oat/arm`, serta `/system/lib/libRadioInterface.so` dan `/system/lib/libfmjni.so` yang ditanda boleh dibaca. Itu membolehkan percubaan copy secara app biasa; ia belum membuktikan NexaDash boleh load library atau mengawal tuner.

## Langkah pada head unit

1. Install **[NexaDash Next1.8](https://raw.githubusercontent.com/shazwannoor95-jpg/NexaDash/main/dist/next/NexaDash-Next-1.8.apk)** atas Next sedia ada. Package dan key sama; tidak perlu uninstall atau pasang semula companion Maps/TizenTube.
2. Buka **Settings → Export Radio files**.
3. Dalam pemilih simpan Android, pilih folder **Download** pada storan tempatan dan tekan **Save**. Nama fail: **`NexaDash-Radio-files.zip`**.
4. Tunggu mesej **Radio files saved**. Ia menunjukkan berapa daripada lima fail berjaya disalin.
5. Hantar ZIP itu untuk analisis seterusnya. Jika `0 of 5` atau sebahagian gagal, hantar juga ZIP: laporan di dalamnya merekodkan kegagalan. Jangan ulang export semua APK.

Jika pemilih simpan tidak tersedia atau penyimpanan gagal, hantar gambar ralat. Tiada app tambahan, permission storan umum, ADB atau root diperlukan untuk mencuba alat ini. Sekatan firmware masih boleh menghalang bacaan walaupun fail kelihatan dalam laporan.

## Kandungan ZIP

Hanya lima path tetap ini dicuba; ia khusus untuk susunan firmware yang sudah dikenal pasti:

```text
/system/app/GalaRadio_integrate/GalaRadio_integrate.apk
/system/app/GalaRadio_integrate/oat/arm/GalaRadio_integrate.odex
/system/app/GalaRadio_integrate/oat/arm/GalaRadio_integrate.vdex
/system/lib/libRadioInterface.so
/system/lib/libfmjni.so
```

Binary yang berjaya dibaca diletakkan di bawah `files/system/...`. **`RADIO_EXPORT_REPORT.txt`** mengandungi status setiap fail, saiz/SHA256, dan laporan Radio runtime paths sedia ada. Tiada folder app data, akaun, pangkalan data, GPS atau koleksi fail rekursif dibaca. Fail yang gagal dibaca tidak diganti dengan binary kosong/separuh. Salinan sementara dibersihkan selepas operasi biasa.

Had bacaan64MiB setiap fail dan128MiB keseluruhan. Fail besar/ditolak akses direkodkan dan diabaikan. Tiada file asal dipindah, dipadam, dijalankan atau diubah. Pengguna memilih lokasi ZIP dan berkongsi sendiri selepas selesai.

## Selepas fail diterima

Bagi ZIP pemilik, recovery VDEX dan analisis dependency JNI telah selesai. Semakan seterusnya mengenal pasti cabang FM sebenar pada peranti dan akses yang kelihatan kepada UID app biasa. Controller servis dengan UI native NexaDash kini tersedia dalamNext1.8; companion Radio tambahan tidak diperlukan oleh laluan ini. Hardware FM masih belum disahkan. **Lima fail lengkap sekalipun belum menjamin app biasa mendapat akses hardware.**

## Semakan update

Semakan JVM menguji allowlist, integriti byte/SHA256, fail hilang/ditolak akses, bacaan separuh, had saiz, pembatalan dan cleanup. Semakan Android API27 menguji tile Settings, handoff pemilih simpan dan penulisan ZIP ke content provider. Tiada library OEM atau perkakasan FM dijalankan dalam ujian ini. [Rekod QA exporter1.5](qa/radio-export-api27.json) · [Laporan tambahan Next1.6](qa/radio-runtime-next16-api27.json).
