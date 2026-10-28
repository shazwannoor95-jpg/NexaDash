# App asal dalam panel — semak head unit dahulu

Versi **1.6.7-sagadash.4 (version code 28)** menambah **App panel compatibility** untuk menyemak
kemampuan sebenar head unit. Ia **belum menambah paparan app asal dalam panel**. YouTube, TizenTube
dan Radio pada panel sekarang masih menunjukkan metadata/kawalan daripada app asal apabila tersedia;
butang Open membuka app itu dalam tetingkapnya sendiri.

## Dapatkan laporan tanpa coding

1. Download [APK .4 dengan semakan keserasian](https://raw.githubusercontent.com/shazwannoor95-jpg/klikdash/saga-dash-android-studio/dist/SagaDash-1.6.7-sagadash.4.apk).
2. Pasang sebagai update melalui File Manager, kemudian buka **Saga Dash**. Ikut
   [panduan pemasangan](HEAD_UNIT_INSTALL_MS.md) jika perlu. Gunakan laluan signing key yang sama
   seperti pemasangan sedia ada; jika Android menolak signature, jangan uninstall terus kerana
   tetapan app boleh hilang.
3. Buka **Settings**, scroll senarai ke bawah, pilih **App panel compatibility** dan tunggu laporan selesai.
4. Ketuk **Copy report**, kemudian paste laporan dalam chat ini. Jika susah memindahkan teks dari
   head unit, ambil gambar bahagian laporan tersebut; scroll untuk menunjukkan sambungannya.

Tidak perlu Android Studio, ADB, root, flash firmware atau menukar MCU/CAN/factory settings untuk
semakan ini. Laporan hanya dibaca pada peranti; ia tidak dihantar secara automatik. Tiada app lain
dilancarkan dan tiada percubaan mendapatkan akses root dibuat.

[APK .3 terdahulu](https://raw.githubusercontent.com/shazwannoor95-jpg/klikdash/saga-dash-android-studio/dist/SagaDash-1.6.7-sagadash.3.apk)
masih tersedia sebagai rujukan versi sebelum penambahan semakan ini. Android mungkin menolak
pemasangan version code lebih rendah di atas .4; pautan lama bukan arahan untuk downgrade.

[Contoh skrin laporan pada emulator](img/saga-app-panel-compatibility.png) — angka emulator
bukan spesifikasi head unit kau.

## Apa yang laporan semak

- Android/API sebenar, model dan ABI CPU, supaya kita tidak bergantung pada label Android dalam
  menu vendor sahaja.
- Saiz paparan dan ciri sistem yang dilaporkan, termasuk PiP/freeform jika tersedia.
- Permission berkaitan yang sudah dimiliki Saga Dash; semakan tidak meminta atau memberikan
  permission istimewa baharu.
- Komponen app berkaitan dan flag manifest yang menunjukkan kebenaran untuk activity embedding
  pada versi Android yang menyokongnya.

Laporan tidak mengumpul serial peranti, SSID Wi-Fi, akaun atau koordinat GPS. Nama/pakej app yang
berkaitan dimasukkan untuk mengenal pasti app OEM sebenar; screenshot ikon sahaja tidak membuktikan
package ID atau sokongan embedding.

Flag atau ciri yang dilaporkan **bukan bukti bahawa app boleh dimainkan dalam panel**. App sasaran,
host, versi Android dan firmware perlu menyokong laluan yang sama. Status “Root access: not probed”
bermaksud root tidak diuji, bukan pengesahan bahawa unit ada atau tiada root.

## Kenapa PiP belum menyelesaikannya

PiP Android ialah tetingkap terapung yang dikawal sistem dan app pemain. Ia bukan permukaan yang
launcher boleh terus masukkan ke dalam panel kiri/kanan. Split-screen juga menggunakan tetingkap
sistem; ia tidak semestinya membolehkan dua app ditempatkan dalam panel dashboard.

Untuk **paparan app asal** YouTube, TizenTube, Maps/Waze atau Radio FM OEM, kita perlu tentukan
sama ada firmware menyediakan integrasi yang sesuai. Sokongan OEM, akses sistem, atau bantuan
seperti Shizuku/root ialah kemungkinan untuk disiasat, bukan jaminan keserasian. Semakan ini tidak
memasang bantuan tersebut atau meminta pengguna melakukan root.

“Player terbina dalam Saga Dash” pula bermaksud player baharu yang dibina sebagai sebahagian
daripada launcher, contohnya video fail dengan player native atau YouTube melalui player web.
Itu berbeza daripada memasukkan app asal TizenTube/YouTube. Radio Internet juga berbeza daripada
radio FM perkakasan head unit. Laluan tersebut belum ditambah dalam build diagnostik ini.

## RAM dan prestasi

Semakan berjalan apabila diminta; ia bukan pemantauan yang berjalan berterusan. Membuka atau
menempatkan app asal tetap memerlukan proses dan sumber app tersebut, walaupun paparannya kecil.
Angka emulator sekitar **40MiB** sebelum ini ialah dashboard tanpa playback video, bukan jumlah
RAM untuk dashboard bersama YouTube/TizenTube/Maps atau dua app aktif. Lihat
[ukuran dan batas ujian](PERFORMANCE.md).

Selepas laporan head unit diperoleh, barulah laluan app asal boleh dipilih dan diuji. Jangan anggap
APK .4 sudah melaksanakan embedding atau menjamin kelancaran pada head unit sebenar.

## Alternatif tanpa ADB

PiP boleh dicuba pada app yang menyokongnya: Android Settings > Apps > Special app access >
Picture-in-picture > YouTube/Maps > Allow. Untuk YouTube, mainkan video kemudian tekan Home dan
buka Saga Dash. Kelayakan akaun/region/video dan firmware masih menentukan sama ada tingkap muncul.
Maps memerlukan navigasi aktif untuk mod PiP. Laporan unit mengisytiharkan PiP, dan activity YouTube
serta Maps mempunyai flag PiP, tetapi laluan ini belum diuji pada unit fizikal oleh pembangun.

Android8.1 biasanya menyediakan satu tingkap PiP sistem yang boleh dialihkan, bukan dua app yang
dikunci ke panel. Radio FM OEM dalam laporan tidak mengisytiharkan PiP. [Player video native/YouTube web beta dalam .7](INLINE_PLAYBACK_MS.md) ialah pilihan berasingan,
bukan paparan app asal. Radio FM dan navigasi lengkap belum ditambah.
