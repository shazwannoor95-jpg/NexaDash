# Radio FM — hasil export runtime 30 Oktober 2026

**Lima fail lengkap. Kod dispatcher Radio dan dua library tuner yang hilang daripada export APK dahulu kini sudah diterima dan dianalisis. Kawalan native Radio dalam panel kini tersedia dalam Next1.8; bunyi/tuner fizikal belum disahkan.** Tiada keperluan mengulang APK Extractor atau ZIP export untuk fail-fail ini.

## Apa yang berjaya dipulihkan?

ZIP `NexaDash-Radio-files.zip` berukuran71,388 bytes, lulus CRC dan padanan SHA256 terhadap laporan export. Lima binary berjumlah127,711 bytes. Binary/ZIP dan rakaman pemilik tidak diterbitkan dalam repo; hanya fakta/hash semakan disertakan. [Rekod analisis](qa/radio-runtime-files-review.json).

| Fail | Bytes | Hasil |
|---|---:|---|
| `GalaRadio_integrate.apk` |21,049|Sama dengan resource/manifest stub terdahulu; tiada regular DEX di dalam APK |
| `GalaRadio_integrate.odex` |16,864|ARM ELF preoptimized companion |
| `GalaRadio_integrate.vdex` |2,478|VDEX010; satu DEX2,296-byte berjaya didequicken, checksum dan SHA1 DEX sah |
| `libRadioInterface.so` |37,244|ELF32 ARM; JNI `Interface.RadioInterface` dan fungsi tuner |
| `libfmjni.so` |50,076|ELF32 ARM; MediaTek/FM JNI termasuk `com/android/fmradio/FmNative` |

VDEX dipulihkan dengan [vdexExtractor](https://github.com/anestisb/vdexExtractor), revision yang direkod dalam QA. Ia bukan sekadar menukar checksum pada bytecode quickened. DEX asal dan semua input dikekalkan setempat; tiada kod OEM dijalankan.

> **Susulan Next1.8:** [UI Radio FM dalam panel](RADIO_PANEL_MS.md) sudah dibina melalui servis asal, tanpa memuat library OEM dalam NexaDash. Bahagian semakan1.6 di bawah ialah rekod langkah terdahulu; pengesahan bunyi FM sebenar masih menunggu ujian pemilik.

## Ikon Radio asal sebenarnya pembuka dua app

`com.acloud.stub.localradio.QtActivity` yang dipulihkan mempunyai constructor, `onCreate`, `onStop` dan `onDestroy`. `onCreate` membaca **`persist.sys.xy.proinfo.exfm`** dengan default`0`, menukarnya kepada integer dan memilih:

| Nilai property | Komponen yang dibuka |
|---|---|
|1 hingga254, kecuali43|`com.acloud.stub.extradio/com.acloud.stub.extradio.QtActivity`|
|0,43,255 atau nilai di luar julat itu|`com.android.fmradio/com.android.fmradio.FmMainActivity`|
|Tidak boleh ditukar kepada integer|Laluan exception menamatkan dispatcher; bukan fallback backend yang disahkan|

Ia memanggil `startActivity` dengan `FLAG_ACTIVITY_NEW_TASK` dan kategori LAUNCHER, kemudian `finishAndRemoveTask`. **Ia tidak melukis UI FM sendiri.** Memasukkan stub itu ke senarai companion tidak menyediakan UI atau kawalan radio dalam panel. Sasaran companion perlu berdasarkan implementation sebenar yang dipilih firmware. Nilai property pada peranti pemilik belum berada dalam ZIP ini.

## Runtime yang masih perlu dibuktikan

`libRadioInterface.so` mengeksport JNI open/close, frequency/band, seek/scan, audio-routing, volume dan RDS. String binarinya merujuk `/dev/tef6606_i2c` dan `/dev/keypad`. `DT_NEEDED` termasuk `libui`, `libutils`, `libcutils`, `liblog`, `libc++`, `libc`, `libm` dan `libdl`.

`libfmjni.so` mempunyai `/dev/fm` dan FM JNI; `DT_NEEDED` termasuk `libandroid_runtime`, `libcutils`, `libmedia`, `liblog`, `libc++`, `libc`, `libm` dan `libdl`. Path dalam string bukan bukti driver itu wujud atau digunakan oleh cabang aktif.

Sebahagian dependency ialah library platform persendirian. Ketersediaan namespace linker, identity sistem asal, permission driver/SELinux dan laluan audio belum diuji daripada app biasa. Salin library sahaja tidak memberikan izin tersebut. Binder yang disemak dahulu masih local-only. Semakan lanjut menemui laluan berasingan melalui Intent `onStartCommand`; client Next1.8 menggunakan laluan itu, bukan cast kepada Binder tempatan. [Protokol servis](RADIO_SERVICE_PROTOCOL.md). [Semakan APK35-part](RADIO_FULL_ARCHIVE_35_REVIEW.md).

## Langkah pemilik sekarang — tanpa ADB

1. Install **[NexaDash Next1.8](https://raw.githubusercontent.com/shazwannoor95-jpg/NexaDash/main/dist/next/NexaDash-Next-1.8.apk)** atas Next sedia ada. Package/key sama; tidak perlu uninstall atau reinstall Maps/TizenTube.
2. Buka **Settings → Radio runtime paths → Copy report**.
3. Hantar teks report. Bahagian baharu menunjukkan nilai selector/komponen terpilih, UID launcher/app Radio, serta mode/UID/GID dan flag read/write bagi tiga path driver tetap.

Laporan membaca satu property firmware melalui framework, metadata `stat` dan flag `access`. **Ia tidak membuka, membaca, menulis atau menghantar ioctl kepada driver**, tidak load library OEM, tidak membuka Radio atau menukar frekuensi. Jika bacaan property disekat, hasilnya **backend unresolved**. Flag akses host juga tidak membuktikan companion dengan UID lain boleh membuka driver atau mengawal FM. Tiada upload automatik.

Report ini diperlukan untuk memilih calon companion dengan betul dan menilai akses yang kelihatan kepada app biasa. Ia belum mengesahkan app Radio asal dalam right panel atau controller FM baharu. Tiada arahan tuner/MCU yang diteka atau dihantar dalam analisis ini.

## Status Maps/TizenTube

Update1.6 ini hanya susulan laporan Radio. Native host dan companion Maps/TizenTube tidak berubah. Video TizenTube bergerak dalam rakaman pemilik; peta/carian Maps dalam panel masih kosong. Percubaan kawalan saiz Maps belum lulus ujian sentuhan dan tidak dimasukkan ke APK ini. [Pemerhatian Maps dan langkah layout yang masih boleh dicuba](MAPS_PANEL_MS.md).

## Semakan release

Next1.6 lulus30 semakan JVM fail +18 boundary selector serta23 assertions integrasi Android API27. Build Android Studio debug/release, kompilasi semua sumber ujian, tandatangan API27, alignment/CRC dan guard pembersihan Quran lulus. Ini tidak menjalankan hardware FM atau companion ARM. [Rekod QA release](qa/radio-runtime-next16-api27.json).
