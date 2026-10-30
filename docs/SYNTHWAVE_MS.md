# Tema Synthwave — Saga Dash

Tema ini menambah latar Saga merah/bulan cyan dan panel kaca gelap pada dashboard sedia ada.
Kedudukan Home, Apps, Settings, jam, waktu solat, panel dan dock tidak ditukar oleh tema. Edit layout
dan semua pilihan app kekal. Viewer lapan sudut, drag, pinch, double-tap, GPS dan trip juga kekal.

## Tukar penampilan

1. Buka **Settings > Appearance**.
2. Pilih **Synthwave** atau **Original**.
3. Laraskan **Background brightness**, **Panel transparency** dan **Neon intensity**.
4. Pilih **Animation mode**; klik **Save**. **Defaults** mengembalikan nilai asal.

| Pilihan | Kesan |
| --- | --- |
| Static | Latar imej tanpa animasi; pilihan default. |
| Ambient | Cahaya bulan berdenyut perlahan jika Reduce motion dimatikan. |
| Cinematic | Cahaya dan beberapa garisan bergerak, maksimum 10fps jika Reduce motion dimatikan. |
| Reduce motion | Hidup secara default; menghentikan semua animasi latar walaupun Ambient/Cinematic dipilih. |

Kekalkan **Static + Reduce motion** untuk ujian awal head unit. Kesan berhenti apabila dashboard
tidak aktif. Kereta pada latar tidak dianimasikan; ia berasingan daripada viewer lapan sudut.

Latar mengisi skrin mengikut nisbah asal dengan crop minimum dan lapisan gelap untuk kebolehbacaan.
Ia berada di belakang UI; peta dan permukaan playback video tidak menerima imej latar ini.
Original mengembalikan gaya grid/litar sebelumnya tanpa reset susunan atau tetapan app.

## Penggunaan sumber

Asset WebP **1672×941, sekitar 285KB** dibaca pada worker dan disampel kepada bitmap RGB565 maksimum
**2MiB**. Fail termampat dan saiz bitmap RAM ialah dua ukuran berbeza. Latar menggunakan drawable
native, cached shader dan garisan, tanpa blur masa nyata, renderer GL khusus, WebView, dependency atau permission
Android baharu. Static tidak menjadualkan frame animasi.

Tiada keputusan emulator yang menjamin kelancaran TEST9278B; lihat [ukuran dan status ujian](PERFORMANCE.md).
Untuk install atau build Windows, ikut [panduan head unit](HEAD_UNIT_INSTALL_MS.md).

## Kemasan kaca (.5)

Appearance > **Glass panels** menambah gradient/pantulan rim statik pada panel sedia ada.
Boleh dimatikan untuk kemasan sebelumnya. Susunan, wallpaper dan fungsi panel tidak berubah.
Tiada live blur, animasi baharu atau salinan bitmap wallpaper bagi setiap panel.
