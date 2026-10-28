# NexaDash .8

Nama paparan kini **NexaDash** dengan ikon orbit cyan, anak panah/jalan putih dan titik violet berdasarkan konsep logo yang dipilih. Ikon ialah VectorDrawable; tiada gambar logo besar atau animasi baharu dalam proses launcher.

Package kekal `com.shazwan.sagadash`, storage dan setting kekal. APK cloud menggunakan sijil sama seperti build cloud terdahulu. Android Studio menggunakan signing configuration tempatan; sijil berbeza boleh menyebabkan update ditolak. Jangan uninstall sebelum backup jika setting perlu disimpan.

## Cari destinasi dan directions

Tekan shortcut **Navigation**, atau tukar panel ke **Navigation preview** lalu tekan **Search / directions**. Masukkan nama tempat, alamat atau koordinat. Pilih Google Maps/Waze, kemudian **Search** atau **Directions**. Destinasi terakhir diisi semula dan boleh dipadam/diganti.

Directions dibuka dalam app Maps/Waze. Bunyi, bahasa arahan, traffic dan routing dikendalikan app itu. Jika Maps tiada, pilihan Maps web boleh dibuka dalam browser. Waze hanya boleh dipilih jika terpasang. Tiada Maps SDK, WebView peta kedua, routing engine atau speech service sentiasa berjalan dalam NexaDash.

**Ini belum memaparkan UI Google Maps/Waze di dalam panel.** Panel kekal peta GPS preview. Navigasi suara sebenar memerlukan app/GPS/rangkaian serta tetapan suara yang sesuai. Semakan emulator mengesahkan pembinaan intent, encoding destinasi dan UI, bukan perjalanan/voice guidance atas jalan.

## Dua app asli dalam panel

Sasaran: kiri Maps/Waze, kanan YouTube/TizenTube/WhatsApp dengan sentuhan penuh. Belum tersedia dalam APK utama. PiP, resizeableActivity dan feature secondary display sahaja tidak membuktikan hosting boleh digunakan.

Eksperimen app yang opt-in menggunakan private VirtualDisplay dan input bridge tersendiri pernah berjaya pada emulator API29. App tanpa opt-in/bridge memerlukan integrasi berasingan; ini bukan hosting universal untuk semua installed apps.

APK YouTube Pro V58 milik pengguna sedang diuji berasingan, bukan dibundel/disiarkan dalam repo. Ia memerlukan API29 sedangkan head unit melaporkan API27. Apa-apa keputusan emulator tidak membuktikan ia boleh dipasang/digunakan pada head unit.

## Cuba dari Android Studio

Download source branch `saga-dash-android-studio`, extract ke folder baharu, Open folder dengan `settings.gradle`, kemudian Run. Footer mesti menunjukkan **NexaDash v1.6.7-nexadash.8**. Compile SDK35 / target29 / min24 dikekalkan. Emulator API29 atau lebih baharu boleh digunakan; tiada perlu downgrade emulator sedia ada.

Upstream KlikDash dan lesen/kreditnya dikekalkan. Nama repo GitHub kekal `klikdash`; nama produk kini NexaDash.

Untuk uji laluan hosting pada firmware tanpa ADB: [demo dua APK yang opt-in](OPTIN_PANEL_PROBE_MS.md). Demo berasingan ini bukan UI YouTube/Maps.
