# Panel, susunan dan penggunaan sumber — Saga Dash

Pilihan native yang ringan digunakan: tiada WebView dan tiada skrin app pihak ketiga yang dipaksa
embed. Susunan asal ialah default. Neon/latar futuristik bersifat statik.

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

Launcher Android biasa tidak boleh embed skrin penuh Maps/Waze/TizenTube sesuka hati. Panel ini
menggunakan preview/kawalan native; app sebenar tetap menggunakan sumbernya sendiri apabila dibuka.

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

Di luar mod Edit, tekan lama ikon membuka Choose app. Jika label app OEM seperti BT, CarSettings,
CarSet, Music, Video, VLC atau ZLINK tidak dapat dipastikan secara unik, pilih app sendiri.
Tiada andaian bahawa CarSet ialah DSP.

## Rekaan dan sumber

Neon cyan/violet dan grid/litar gelap dilukis dengan garisan statik. Edit layout > **Neon outline**
menghidupkan/mematikannya. Tiada blur berat, wallpaper video atau timer animasi ditambah.

- Imej sudut menggunakan atlas yang disampel dan dibatasi; tiada animasi idle, mesh atau decoder video.
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
