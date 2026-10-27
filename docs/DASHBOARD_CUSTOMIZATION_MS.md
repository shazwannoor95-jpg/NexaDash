# Panel, susunan dan penggunaan sumber — Saga Dash

Pilihan native yang ringan digunakan: tiada WebView dan tiada skrin app pihak ketiga yang dipaksa
embed. Susunan asal ialah default. Tema Synthwave bermula dengan latar statik dan Reduce motion hidup.

## Tukar kandungan panel

Ketuk nama panel dengan simbol **▾** di bahagian atas panel kiri atau kanan.

- Kiri: Car preview, Navigation preview, YouTube controls, TizenTube controls, Media player, Radio controls.
- Kanan: Media player, Navigation preview, Radio controls, YouTube controls, TizenTube controls.
- Kedua-dua panel media berkongsi satu pemerhati sesi Android. Mainkan media dalam app asal dahulu.
- YouTube/TizenTube/Radio menunjukkan metadata dan kawalan apabila sumber itu menerbitkan sesi yang
  sepadan. **Open** membuka app asal; **Choose app** menetapkan app yang dipasang.
- Navigation ialah peta GPS native, bukan enjin turn-by-turn. Jubin OpenStreetMap dimuatkan apabila
  panel dipilih, GPS tersedia dan Internet berfungsi. **Open navigation** membuka Waze/Maps/app pilihan.
- Tiada lokasi direka tanpa GPS. Cache peta terhad dan bukan peta offline penuh; attribution kekal.

Apabila panel sumber dipendekkan, tajuk/panduan tambahan diringkaskan dan metadata menggunakan satu
baris. Butang Open, Choose app serta Previous/Play/Next dikekalkan supaya masih boleh dicapai.

Launcher Android biasa tidak boleh embed skrin penuh Maps/Waze/TizenTube sesuka hati. Panel ini
menggunakan preview/kawalan native; app sebenar tetap menggunakan sumbernya sendiri apabila dibuka.

Untuk permintaan **app asal terus dalam panel**, versi .4 menyediakan Settings >
**App panel compatibility**. [Salin laporan head unit](APP_PANELS_MS.md) dahulu; fungsi ini
belum menambah embedding dan tidak mengubah panel kawalan di atas.

## Paparan kereta ringan

Default ialah **Saga image angles (lightweight)**: lapan imej sudut daripada rujukan Saga merah,
dengan plat **NEXA**. Drag menukar sudut secara langkah dan berulang; pinch zoom; double-tap reset.
Ia bukan mesh 3D, kamera bebas atau animasi yang bergerak sendiri. Imej render mungkin berbeza
sedikit daripada bentuk sebenar kereta. Mod ini tidak memerlukan decoder/GLES video.

**Transparent Saga video** dan import video masih pilihan berasingan. Mod lama Original Saga video
serta Interactive 3D model telah dibuang. Tiada mesh lama dimuatkan dalam paparan awal.

## Gerak dan ubah saiz panel

1. Klik **Edit layout** atau Settings > Dashboard layout.
2. Pilih **Move / resize panels**.
3. Drag badan panel untuk bergerak; drag handle penjuru kanan bawah untuk resize.
4. **Done ✓** menyimpan. Tap footer Cancel atau tekan Back untuk membatalkan sesi edit.
5. **Reset default layout** mengembalikan susunan asal.

Kedudukan, saiz dan stacking panel disimpan secara normalized supaya berada dalam skrin apabila
resolusi berubah. Header/jam/waktu solat/footer kekal; panel kandungan, shortcut dan dock boleh disusun.
Gesture sudut kereta dan butang biasa berfungsi di luar mod Edit.

## Susun dan ubah saiz ikon dock

1. Edit layout > **Reorder dock icons**.
2. Tekan lama ikon dan drag ke ikon sasaran untuk ubah urutan.
3. Tap ikon dalam mod ini untuk pilih saiz **70–130%**.
4. **Done ✓** menyimpan; footer Cancel / Back membatalkan perubahan sesi itu.

70–130% ialah saiz yang diminta. Ikon dikecilkan secara automatik mengikut ruang sebenar setiap slot
dock, termasuk ruang label. Dock yang kecil boleh memaparkan ikon lebih kecil daripada peratus pilihan
supaya ikon dan label tidak melimpah keluar.

Di luar mod Edit, tekan lama ikon membuka Choose app. Jika label app OEM seperti BT, CarSettings,
CarSet, Music, Video, VLC atau ZLINK tidak dapat dipastikan secara unik, pilih app sendiri.
Tiada andaian bahawa CarSet ialah DSP.

## Rekaan dan sumber

Settings > **Appearance** menyimpan pilihan berikut:

- Theme: Original / Synthwave, tanpa mengubah kedudukan panel atau tindakan butang.
- Background brightness, Panel transparency dan Neon intensity.
- Animation mode: Static / Ambient / Cinematic, serta Reduce motion.

Lihat [panduan Synthwave](SYNTHWAVE_MS.md) untuk langkah dan had penggunaan sumber.

Synthwave menggunakan imej latar yang diisi mengikut nisbah asal, lapisan gelap dan panel kaca gelap
dengan outline cyan/violet. Peta dan permukaan video mengekalkan kandungan sendiri. Original
mengembalikan latar grid/litar terdahulu. Edit layout > **Neon outline** masih tersedia.

Default **Static + Reduce motion** tidak menjadualkan animasi latar. Ambient/Cinematic hanya bergerak
apabila dipilih, Reduce motion dimatikan dan dashboard kelihatan; hadnya 10 frame sesaat. Kesan hanya
cahaya/garisan, bukan pergerakan kereta. Tiada blur masa nyata, WebView atau renderer GL untuk latar.

- Imej sudut menggunakan atlas yang disampel dan dibatasi; tiada animasi idle, mesh atau decoder video.
- Bitmap latar disampel/dihadkan kepada 2MiB, dimuatkan di worker dan dilepaskan apabila tidak diperlukan.
- Video pilihan hanya menggunakan decoder/GLES apabila diperlukan; sumber dilepaskan apabila tersembunyi.
- Formatter jam/tarikh diguna semula dan teks sama tidak ditulis semula setiap saat.
- Cover album dihadkan kepada 128px. Semua panel berkongsi satu pengawal media.
- Peta berkongsi cache RAM maksimum 3MiB, cache cakera 12MiB dan satu worker. Ia tidak memuat turun
  jubin ketika tersembunyi atau sebelum GPS diperoleh.

Lihat [ukuran prestasi dan semakan build ini](PERFORMANCE.md).

Angka KlikDash upstream sekitar 30MB ialah laporan untuk app/perkakasan upstream, bukan ukuran
Saga Dash. RAM mesti dinilai dengan ukuran proses seperti PSS; peratus bergantung pada jumlah RAM.
App YouTube/Spotify/Maps/TizenTube mempunyai penggunaan berasingan. Ujian emulator tidak membuktikan
prestasi Cortex-A7 atau menjamin tiada lag/stutter/crash pada firmware TEST9278B.

## Update

Buka **ZIP projek terkini dalam folder baharu**, bukan copy `src`/`res` ke projek lama. Fail yang
sudah dibuang boleh tertinggal melalui copy/replace. Ikut [panduan Windows](WINDOWS_ANDROID_STUDIO_MS.md)
dan [panduan APK/head unit](HEAD_UNIT_INSTALL_MS.md).
