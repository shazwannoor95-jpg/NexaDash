# Player dalam panel tanpa ADB — .7

Versi **1.6.7-sagadash.7 / code31** menambah pemain sebenar dalam panel. Ini berbeza daripada
mod lama YouTube/TizenTube/Radio app controls yang hanya membaca sesi media app asal.

## Video fail — native

1. Ketuk selector **▾** pada panel kiri atau kanan.
2. Pilih **Video file · in panel**.
3. Tekan **Choose video**, pilih video melalui Android File Picker, kemudian tekan **Play**.
4. Play/Pause dan seek berada dalam panel; tiada app player lain dibuka.

Fail dimainkan melalui content URI, tanpa disalin ke APK atau cache video launcher. App cuba
mengekalkan grant baca untuk fail terakhir apabila provider menyokongnya. Picker alternatif vendor
mungkin hanya memberikan akses sementara; pilih fail semula jika akses sudah tamat. Apabila fail
berubah, grant lama yang disimpan oleh player dilepaskan. Fail asal tidak dipadam atau diubah.

Gunakan **MP4 H.264 720p** untuk permulaan pada TEST9278B. Had video native ialah1080p/2MP;
codec dan kelancaran sebenar tetap bergantung pada decoder head unit. Video yang tidak boleh dibaca
atau tidak disokong menunjukkan ralat dan pilihan memilih semula, tanpa membuka player luar.
Native player berhenti apabila fokus audio hilang; tekan Play untuk sambung apabila sesuai.
Gunakan kawalan dalam panel Video file itu sendiri. Panel Media controls berasingan masih membaca
sesi media app asal, bukan sesi pemain fail ini.

[Preview player native dalam panel](img/saga-inline-video.png). Preview menggunakan klip720p
yang pengguna bekalkan, dipilih melalui File Picker; fail demo itu tidak dibundel dalam APK.

## YouTube web — beta

- Tekan shortcut **YouTube web** di dashboard untuk panel kiri; atau selector panel kiri/kanan >
  **YouTube web · in panel (beta)**.
- Masukkan kata carian atau link YouTube pada kotak atas dan tekan **Go**. Gunakan kawalan laman
  YouTube untuk memilih/mainkan video. Ia laman YouTube, bukan app Google YouTube atau TizenTube.
- **Search** menunjukkan/menyembunyikan kotak carian. Bar carian/status disembunyikan pada halaman
  watch untuk memberi ruang video. **Back** mengundur sejarah, **Home** membuka halaman utama,
  **Stop** melepaskan WebView.
- Gunakan **Edit layout** jika perlu membesarkan kawasan paparan. Butang fullscreen laman tidak
  membuka activity di luar panel; gunakan saiz panel yang sesuai.
- Tekan lama shortcut YouTube web untuk membuka atau memilih app YouTube/TizenTube asal.

Internet, tarikh/masa sistem yang betul dan **Android System WebView** yang serasi diperlukan.
Kelayakan akaun/region/video, sekatan sign-in, DRM dan sokongan browser ditentukan oleh YouTube.
Pilih kualiti rendah seperti360p/480p pada laman jika perlu mengurangkan beban. Launcher tidak
memintas sekatan laman, tidak memasang JavaScript bridge native, dan tidak memberikan kamera,
mikrofon, lokasi, file/content URI atau akses muat turun kepada laman. Link app/intent dan skema
bukan HTTPS tidak dilancarkan secara automatik. Ralat SSL tidak diabaikan.

Mesin ujian menyekat sambungan ke YouTube (`CONNECT tunnel403`). Ujian benar memainkan video
HTML5 lokal dalam WebView; itu **bukan** pengesahan streaming YouTube sebenar. Mod YouTube web
kekal beta sehingga dicuba pada WebView/rangkaian head unit. Ia tidak menggunakan API key YouTube.

## Had sumber dan lifecycle

Hanya **satu panel video/web** dipilih pada satu masa. Memilih pemain di panel satu lagi akan
mengembalikan panel video terdahulu ke Car/Media controls. Peta native boleh berada di panel lain.
Car preview menggunakan imej apabila panel video/web dipilih; decoder car preview tidak berjalan
serentak. Susunan dan saiz panel tidak diubah secara automatik.

WebView dimuatkan hanya apabila diminta. Keluar app, masuk Edit layout, Stop atau menukar panel
akan menutup WebView/melepaskan decoder native, audio focus dan progress callback. Apabila kembali,
tekan Start/Play; tiada autoplay latar. Provider Android mungkin mengekalkan proses renderer/cache
selepas destroy; ini bukan jaminan semua RAM dikembalikan serta-merta.

Native player mempunyai progress UI1Hz hanya ketika bermain. I/O content-provider dijalankan pada
worker sedia ada. Tiada servis playback latar, salinan fail video, permission storage baharu atau
renderer GL custom. YouTube web mempunyai beban tambahan milik website/provider; ukuran idle .3
sekitar40MiB tidak mewakili playback .7.

## Peta dan Radio FM

Peta .7 masih **GPS map preview** dengan butang membuka app navigasi. Carian destinasi, pengiraan
route dan navigasi turn-by-turn bersuara **belum ditambah dalam build ini**. Untuk membina navigasi
dalam panel, carian/route service, pengesanan kemajuan GPS/rerouting dan TTS perlu dipilih/dibina
serta diuji; versi online asas tidak sama dengan traffic live atau navigasi offline lengkap.

Radio FM masih memerlukan pemeriksaan integrasi vendor. Dapatkan APK asal
**com.acloud.stub.localradio**, bukan link Play Store. APK extractor/backup boleh mengeksportnya.
Jika menggunakan Termux pada head unit, `/system/bin/pm path com.acloud.stub.localradio` boleh
menguji sama ada firmware membenarkan lokasi APK dibaca tanpa ADB/root. Output Permission denied
bermakna gunakan extractor biasa; jangan jalankan arahan root secara tekaan. APK mungkin hanya
stub dan masih memerlukan library/permission vendor, jadi mendapatkan APK belum menjamin integrasi.
