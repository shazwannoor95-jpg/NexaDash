# Buka source Saga Dash terkini dalam Android Studio — Windows

Panduan ini untuk **1.6.7-sagadash.7 / version code 31**. Versi .6 menambah grid ikon
**Dashboard settings**, mengekalkan grid ikon **Apps**, dan memaparkan versi pada footer/menu
serta **About Saga Dash** supaya build yang sedang berjalan mudah dikenal pasti.

## Kenapa rakaman masih menunjukkan senarai Apps

APK dan source ZIP .5 yang diterbitkan telah disemak: ia mengandungi `DashAppGrid` dan tajuk
**Apps · bilangan app**. Tajuk **Installed apps** dalam rakaman sepadan dengan implementasi
senarai teks sebelum .5. Gambar itu sahaja tidak menentukan nombor versi lama yang dipasang.

Jika kau tekan Run daripada folder projek Android Studio yang lama, Studio akan membina kod
folder itu. Memuat turun ZIP baharu tidak menukar projek yang sedang dibuka secara automatik.

## Muat turun dan buka folder baharu

1. Dalam Android Studio, pilih **File > Close Project**.
2. Download [source ZIP branch Saga Dash terkini](https://codeload.github.com/shazwannoor95-jpg/klikdash/zip/refs/heads/saga-dash-android-studio).
3. Extract ke folder baharu, contohnya `C:\SagaDash-v7`. Jangan salin atau extract bertindih
   ke folder projek lama.
4. Dalam Android Studio, pilih **Open**. Buka folder di dalam hasil extract yang mengandungi
   **settings.gradle**, **build.gradle** dan folder **app**. Kadangkala ZIP mempunyai satu
   lapisan folder tambahan; pilih folder projek tersebut, bukan pembungkus luarnya.
5. Sebelum Run, semak **app > build.gradle**:

   ```groovy
   versionCode 31
   versionName '1.6.7-sagadash.7'
   ```

6. Pastikan dua fail ini ada di bawah `src/com/shazwan/sagadash`:
   **DashAppGrid.java** dan **DashSettingsGrid.java**. Jika versi atau fail tidak sepadan,
   semak semula folder yang kau buka.
7. Tunggu **Gradle Sync** selesai. Kekalkan konfigurasi projek: **AGP 8.7.3**, **Gradle 8.9**
   dan Gradle JDK **17 atau 21**. Tidak perlu menerima tawaran upgrade automatik.
   SDK compile ialah **Android 15 / API35**; ia berbeza daripada Android emulator/head unit.

## Run dan sahkan versi di emulator

1. Guna emulator **API31 atau API32** yang sudah tersedia; tidak perlu mencipta emulator lain.
2. Pada bar atas Studio, pilih konfigurasi **app** dan emulator yang kau mahu gunakan.
3. Tekan **Run ▶** dan tunggu build serta pemasangan selesai.
4. Periksa nombor versi **1.6.7-sagadash.7** pada footer/menu, atau buka **About Saga Dash**.
5. Tekan **Apps**: ia patut membuka grid ikon sebenar bersama nama app.
6. Tekan **Settings**: **Dashboard settings** juga patut menggunakan grid ikon.

Ikon pada emulator mengikut app yang dipasang dalam emulator. Head unit mempunyai koleksi
app vendor berbeza seperti Radio/BT; source tidak membekalkan atau memasang app vendor itu.

Jika footer/menu masih menunjukkan versi lain, semak projek dan emulator yang dipilih sebelum
membuat perubahan lain. Android Studio boleh membuka beberapa projek atau menjalankan beberapa
emulator pada masa yang sama.

## ZIP source dan APK ialah dua laluan berbeza

- **Source ZIP** di atas ialah projek untuk dibuka dan dibina dalam Android Studio.
- [APK siap .7](https://raw.githubusercontent.com/shazwannoor95-jpg/klikdash/saga-dash-android-studio/dist/SagaDash-1.6.7-sagadash.7.apk)
  ialah fail pemasangan terus; ia tidak dibuka sebagai projek Studio.

Untuk update pemasangan sedia ada, gunakan signing key yang sama seperti sebelumnya. Jangan
buat key baharu atau uninstall app semata-mata untuk menyelesaikan ralat signature; laporkan
ralat dahulu supaya tetapan sedia ada tidak terpadam. Ikut
[panduan pemasangan dan signing Windows](HEAD_UNIT_INSTALL_MS.md) untuk laluan yang sesuai.

## Status app asal dalam panel kiri/kanan

**.7 belum melaksanakan hosting app asal YouTube/TizenTube/Radio/Maps dalam panel.** Grid Apps
membuka app asal menggunakan tetingkapnya sendiri. Grid Settings pula hanya menukar paparan
pilihan tetapan; kedua-duanya tidak memberikan permission hosting sistem.

Laluan hosting sistem khusus firmware belum diuji kerana akses istimewa belum tersedia.
Tanpa ADB, PiP Android boleh dicuba sebagai pilihan app/sistem: ia bergantung pada sokongan app, versi, akaun jika
berkaitan, serta firmware OEM. Ia lazimnya menggunakan satu tetingkap terapung dan tidak bermakna
app boleh dipasang sebagai kandungan tetap pada panel kiri/kanan.

Versi .7 menambah [video fail native dan YouTube web beta dalam panel](INLINE_PLAYBACK_MS.md).
Ia pendekatan berbeza daripada hosting app asal; Radio FM dan navigasi lengkap belum diintegrasikan.

[Preview Settings .6](img/saga-settings-grid.png) · [Preview Apps .6](img/saga-versioned-apps-grid.png).
