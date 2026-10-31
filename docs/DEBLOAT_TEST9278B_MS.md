# Debloat TEST9278B: profil semakan

[Profil calon dan komponen dilindungi](../config/debloat/test9278b-review.json) berdasarkan arkib APK pemilik. Profil ini tidak menjalankan arahan disable. Ia bukan senarai debloat TV generik atau bukti penjimatan RAM.

Calon awal ialah Calendar Sync jika tidak digunakan dan VLC jika bukan pemain pilihan. Identiti package, keadaan semasa dan kegunaan sebenar perlu disahkan pada peranti. Gallery, Theme, LogoSelector, LockScreen dan utiliti OEM belum dimasukkan dalam senarai automatik kerana kebergantungannya belum lengkap.

Kekalkan Radio/FM, Bluetooth, audio/DSP/MCU, CanBus/CarSet, servis GPS, Play Services untuk Maps, WebView beserta donor aktif, komponen Android dan launcher OEM sebagai fallback. Nama fail atau saiz APK tidak menentukan penggunaan RAM latar.

Alat pelaksanaan mesti merekod keadaan asal, menggunakan disable-user, menyediakan Restore dan mengesahkan keadaan selepas setiap kelompok kecil. Jangan enable semua package secara pukal ketika Restore kerana sebahagiannya mungkin sudah disabled sebelum alat digunakan.

CleanYourTV menggunakan ADB melalui rangkaian. Membungkusnya sebagai APK tidak memberikan akses shell. Akses ADB/root pada TEST9278B belum disahkan; tiada arahan terhadap head unit telah dilaksanakan.

## Peta dalam panel

> Susulan Next1.9: peta native dengan carian/laluan kini dihantar; lihat [panduan semasa](PANEL_MAP_NEXT19_MS.md). Perancangan terdahulu di bawah bukan status release terkini.

TerraDash menunjukkan pendekatan carian geocoding dan laluan OSRM dalam peta sendiri. NexaDash semasa mempunyai Map preview native dan cache tile terhad; butang directions masih membuka app luar. Integrasi carian/laluan dalam panel, belokan, suara dan pengesahan perjalanan belum dihantar oleh perubahan profil ini. Maps companion yang kosong juga belum dibaiki.

Pelaksanaan peta perlu mengurus pembatalan request apabila panel ditutup, had respons/cache, lokasi yang lapuk, kegagalan Internet, atribusi penyedia dan had penggunaan servis awam. Cache tile tidak menjamin carian atau routing offline. Lesen/kredit mesti dikekalkan jika kod TerraDash digunakan.
