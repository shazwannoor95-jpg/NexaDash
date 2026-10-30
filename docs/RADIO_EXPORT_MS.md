# Radio FM — export fail sistem tanpa ADB

**Next1.5 menambah alat export, bukan player/kawalan Radio FM.** Radio asal masih belum boleh dijanjikan berjalan dalam panel. Tiada arahan tuner, bind servis OEM, root, penggantian app sistem atau upload automatik dibuat oleh alat ini.

## Apa yang ditemui dalam upload FM?

Kedua-dua `fm.part1.rar` dan `fm.part2.rar` lengkap dan lulus extraction/CRC. Lima APK di dalamnya **identik byte demi byte** dengan export35-part terdahulu. Backup App Manager Radio mempunyai APK dan direktori `oat` kosong; fail `.odex/.vdex` tidak berada dalam backup itu. Dua library FM juga tidak disertakan. `am.jar`/`main.jar` ialah alat sokongan App Manager, bukan implementation Radio.

Jadi tidak perlu ulang **Select all → APK Extractor/backup**. [Rekod semakan](qa/radio-fm-archive-review.json) · [Analisis kod Radio terdahulu](RADIO_FULL_ARCHIVE_35_REVIEW.md).

Laporan runtime daripada TEST9278B menunjukkan lokasi APK, dua fail preoptimized dalam `oat/arm`, serta `/system/lib/libRadioInterface.so` dan `/system/lib/libfmjni.so` yang ditanda boleh dibaca. Itu membolehkan percubaan copy secara app biasa; ia belum membuktikan NexaDash boleh load library atau mengawal tuner.

## Langkah pada head unit

1. Install **[NexaDash Next1.5](https://raw.githubusercontent.com/shazwannoor95-jpg/NexaDash/main/dist/next/NexaDash-Next-1.5.apk)** atas Next1.4. Package dan key sama; tidak perlu uninstall atau pasang semula companion Maps/TizenTube.
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

Analisis seterusnya ialah memulihkan kod Radio daripada ODEX/VDEX jika formatnya disokong, mengenal pasti API/dependency JNI dan keperluan identity/permission sebenar. Hanya selepas itu boleh ditentukan sama ada sesuai membuat companion UI asal atau controller FM dalam panel. **Lima fail lengkap sekalipun belum menjamin app biasa mendapat akses hardware.**

## Semakan update

Semakan JVM menguji allowlist, integriti byte/SHA256, fail hilang/ditolak akses, bacaan separuh, had saiz, pembatalan dan cleanup. Semakan Android API27 menguji tile Settings, handoff pemilih simpan dan penulisan ZIP ke content provider. Tiada library OEM atau perkakasan FM dijalankan dalam ujian ini. [Rekod QA](qa/radio-export-api27.json).
