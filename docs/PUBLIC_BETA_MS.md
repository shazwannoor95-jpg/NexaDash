# NexaDash Next — public beta

NexaDash ialah launcher Android yang dibangunkan daripada KlikDash. Versi untuk percubaan komuniti sekarang: **Next 1.1 (`1.1-next2`)**. Build dan semakan statik lulus; versi ini belum disahkan stabil pada head unit fizikal. Gunakan ketika kereta berhenti dan kekalkan launcher asal untuk kembali jika perlu.

## Install

[Download NexaDash Next 1.1](https://raw.githubusercontent.com/shazwannoor95-jpg/klikdash/nexadash-native-panel-experiment/dist/next/NexaDash-Next-1.1.apk).

Android minimum API24 (Android7.0). Native-display experiment memerlukan API26+. Syarat minimum bukan jaminan keserasian firmware OEM. Install APK melalui file manager, buka **NexaDash Next** dari Apps dahulu. Selepas menguji, pengguna boleh memilihnya dalam Android Default launcher jika firmware menyediakan pilihan itu. Ia tidak perlu dijadikan Home untuk dicuba. Kekalkan app launcher asal.

Next1.1 mengemas kini Next1.0 dengan package/signing key sama. Ia dipasang berasingan daripada NexaDash lama/Native Lab; data lama tidak dipindahkan. APK GitHub dan build Android Studio dengan signing key sendiri tidak semestinya boleh update antara satu sama lain.

## Status ciri

| Bahagian | Status beta |
|---|---|
| Dashboard, glass panels, Apps/Settings, susunan ikon | Tersedia; keserasian/performance peranti perlu diuji |
| Local video / YouTube web | Tersedia; playback dan fullscreen bergantung WebView/peranti/rangkaian |
| Navigation shortcut | Membuka Maps/Waze sebagai app berasingan |
| Maps native dalam panel | Eksperimen companion berasingan; rendering, carian, route dan suara pada head unit belum disahkan |
| Radio FM native dalam panel | Belum siap; bukan ciri public beta yang berfungsi |
| Waze / WhatsApp / sebarang app asal dalam panel | Belum disediakan sebagai hosting umum |
| CPU/RAM check | Ukuran proses launcher sendiri, bukan keseluruhan sistem/companion |

Launcher asas tidak memerlukan companion Maps. Pengguna yang mahu mencuba eksperimen Maps boleh membaca [panduan berasingan](NEXT_INSTALL_MS.md). Jangan menganggap kejayaan build sebagai pengesahan navigasi atau sokongan semua app.

## Apa yang perlu diuji sebelum stable

Buka/tutup launcher, tekan Home dan kembali ke launcher asal; uji Apps/Settings dan simpan layout; uji jam selepas restart/sleep/wake; cuba video/web fullscreen dan Back; ambil Performance check. Rekod model, **API sebenar**, resolusi, versi app dan langkah menghasilkan masalah. Uji eksperimen Maps secara berasingan. Hardware yang belum diuji tidak disenaraikan sebagai disokong/disahkan.

[Laporkan masalah](https://github.com/shazwannoor95-jpg/klikdash/issues/new?template=nexadash-beta.md). Jangan sertakan akaun, kata laluan, API key, signing key atau lokasi rumah dalam rakaman/log.

## Source dan lesen

[Source branch](https://github.com/shazwannoor95-jpg/klikdash/tree/nexadash-native-panel-experiment) · [Source ZIP](https://codeload.github.com/shazwannoor95-jpg/klikdash/zip/refs/heads/nexadash-native-panel-experiment) · [Lesen MIT dan kredit KlikDash](../LICENSE).

Lesen MIT projek tidak memberikan lesen kepada APK Google Maps/YouTube atau aset pihak ketiga. Eksperimen companion tidak patut dianggap sebahagian daripada pelepasan open-source berlesen MIT; hak pengedaran pihak ketiga perlu disemak sebelum pengedaran awam lanjut. Private signing backup mesti disimpan di luar repo. Source Android Studio menggunakan identiti Lab secara lalai; [build Next](../tools/next/README.md) menghasilkan identiti Next dengan signing key sendiri.
