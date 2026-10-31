# Peta dan Maps dalam panel — Next1.9

Update ini mempunyai dua laluan berasingan. **Map · search / route** ialah peta native NexaDash dengan carian dan laluan dalam panel. **Google Maps · native Next companion** kekal app Google Maps yang dipasang berasingan, kini dengan layout kanvas lebih luas dan fallback compact. Tiada APK companion baharu diperlukan untuk perubahan host ini.

## Peta NexaDash

1. Update APK host kepada Next1.9. Dalam menu panel kiri atau kanan pilih **Map · search / route**.
2. Beri NexaDash izin lokasi melalui Settings → Location permission. Untuk laluan dari kedudukan semasa, tunggu GPS yang masih segar. Medan carian boleh digunakan tanpa GPS, tetapi laluan menunggu kedudukan sebenar.
3. Tekan **Search / route**, taip destinasi dan tekan Search. Pilih hasil untuk meminta laluan pemanduan. Carian, pemilihan hasil dan peta kekal di panel.
4. Seret peta untuk pan, gunakan −/+ untuk zoom, dan **GPS** untuk kembali mengikuti kedudukan.
5. **Search / route → Route options** menyediakan Recalculate, Clear route, Voice guidance dan Show entire route. Suara asas menggunakan enjin/data suara English Android yang mesti tersedia pada unit. Ia dimatikan ketika panel berhenti; tiada muat turun suara automatik.

Carian menghantar teks dan, jika tersedia, lokasi bias kepada Photon. Pengiraan laluan menghantar koordinat mula/destinasi kepada OSRM. Tile datang daripada OpenStreetMap. Ini memerlukan Internet; cache tile tidak menyediakan carian/routing offline. OSRM demo/Photon ialah servis awam dengan ketersediaan/had penggunaan sendiri, bukan backend pengeluaran yang dijamin. Tiada API key Google diperlukan.

Paparan belokan menggunakan kedudukan sepanjang geometri laluan. ETA ialah anggaran berdasarkan tempoh laluan, tanpa trafik langsung. Apabila lebih100m di luar laluan, panel meminta Recalculate; tiada pengiraan semula automatik. Panduan ini belum diuji dalam perjalanan sebenar dan bukan pengesahan kesetaraan Google Maps/Waze. Nama jalan/arahan asas dipaparkan daripada respons laluan; suara pada head unit masih memerlukan ujian sebenar.

Request carian/routing dihadkan kepada2MiB,10,000 titik dan2,000 arahan. Request dibatalkan secara logik apabila panel pause/ditutup/diganti; respons lama tidak membuka semula dialog. Worker dihentikan ketika view ditutup. Zoom/render tidak melancarkan app luar atau menghidupkan Maps companion.

## Google Maps companion yang kosong

Rakaman pemilik terdahulu menunjukkan UI Explore/Latest in the area tetapi peta/carian kosong; companion berfungsi di luar panel. Punca tepat belum dibuktikan pada TEST9278B.

Next1.9 memberi sasaran Maps kanvas dengan sisi pendek480px apabila muat, berbanding320px sebelum ini. Sisi panjang kekal maksimum800px, DPI kekal160, nisbah aspek panel dan pemetaan sentuhan dikekalkan. Ini menyediakan lebih ruang layout untuk Maps; ia tidak membaiki identiti Google, jaringan atau renderer melalui andaian.

Cuba **Google Maps · native Next companion → Start**. Jika helaian Explore menutup peta, tarik pemegang helaian ke bawah. **⋯ → Maps layout / permissions** menyediakan:

- **Spacious map layout:** lalai baharu.
- **Original compact layout:** fallback kepada saiz asal. Pilihan disimpan mengikut panel.
- **Maps location permission settings:** membuka info app companion yang tepat; semak izin lokasi, kembali dan Start semula.

Menukar layout sesi aktif menghentikan dan memulakan semula panel itu dengan display baharu. Panel sebelah dikekalkan. Menukar layout yang sudah Stop tidak auto-start. Kanvas lebih luas boleh menambah kerja rendering; ini bukan mod penjimatan RAM.

**Maps sebenar belum disahkan pulih pada head unit.** Jika masih kosong, gunakan peta NexaDash di atas dan hantar App panel compatibility report selepas Start. Bukti display/input app ujian tidak membuktikan Google Maps melukis tile atau carian sebenar.

## Semakan developer

API27 lulus30 semakan peta native menggunakan respons HTTPS simulasi,57 semakan layout/input dengan dua app ujian berasingan UID/display,27 semakan JVM route/layout dan12 kes geometri lama. Build debug/release Android Studio serta integriti/tandatangan APK lulus. App ujian tidak menjalankan Google Maps ARM atau menghasilkan peta kawasan pemilik. Cubaan semakan Photon/OSRM sebenar ditolak403 oleh proxy environment, jadi respons servis sebenar dan perjalanan/suara pada head unit belum disahkan. [Rekod QA](qa/maps-next19-api27.json).
