# NexaDash — panduan tukar launcher head unit

![Contoh reka bentuk dashboard NexaDash](img/nexadash-dashboard-public.png)

*Contoh reka bentuk daripada screenshot versi terdahulu yang telah disunting untuk mengosongkan plat. Susunan versi Next semasa mungkin berbeza. Ini menunjukkan dashboard launcher; bukan bukti Maps/Radio native berfungsi.*

NexaDash ialah launcher Android dengan dashboard panel kaca, diselenggara oleh [shazwannoor95-jpg](https://github.com/shazwannoor95-jpg). Versi untuk percubaan komuniti sekarang: **Next 1.7 (`1.7-next8`)**. Build dan semakan statik lulus; versi ini belum disahkan stabil pada head unit fizikal. Gunakan ketika kereta berhenti dan kekalkan launcher asal untuk kembali jika perlu.

## Install

[Download NexaDash Next 1.7](https://raw.githubusercontent.com/shazwannoor95-jpg/NexaDash/main/dist/next/NexaDash-Next-1.7.apk).

Android minimum API24 (Android7.0). Native-display experiment memerlukan API26+. Syarat minimum bukan jaminan keserasian firmware OEM. Install APK melalui file manager, buka **NexaDash Next** dari Apps dahulu. Selepas menguji, pengguna boleh memilihnya dalam Android Default launcher jika firmware menyediakan pilihan itu. Ia tidak perlu dijadikan Home untuk dicuba. Kekalkan app launcher asal.

Next1.7 mengemas kini Next1.0–1.6 dengan package/signing key sama. Ia dipasang berasingan daripada NexaDash lama/Native Lab; data lama tidak dipindahkan. APK GitHub dan build Android Studio dengan signing key sendiri tidak semestinya boleh update antara satu sama lain.

## Dua panel native

Next1.4 membenarkan dua companion native berbeza tanpa menutup panel sebelah. Host Next1.4 lulus47 assertions dengan dua app ujian pada emulator API27 yang diiklankan secondary-display-capable; Rakaman pemilik kini menunjukkan video TizenTube bergerak dalam panel; fungsi lain belum disahkan. Peta/carian Maps masih kosong dalam panel. [Panduan, APK companion dan ujian tanpa ADB](DUAL_NATIVE_MS.md). App asal WhatsApp/Spotify tidak menjadi embeddable secara automatik.

## Status ciri

| Bahagian | Status beta |
|---|---|
| Dashboard, glass panels, Apps/Settings, susunan ikon | Tersedia; keserasian/performance peranti perlu diuji |
| Local video / YouTube web | Tersedia; playback dan fullscreen bergantung WebView/peranti/rangkaian |
| Navigation shortcut | Membuka Maps/Waze sebagai app berasingan |
| Maps native dalam panel | UI app muncul tetapi peta/carian masih kosong dalam ujian pemilik; route/suara belum disahkan |
| Radio FM native dalam panel | Belum siap; bukan ciri public beta yang berfungsi |
| Waze / WhatsApp / sebarang app asal dalam panel | Belum disediakan sebagai hosting umum |
| CPU/RAM check | Ukuran proses launcher sendiri, bukan keseluruhan sistem/companion |

Launcher asas tidak memerlukan companion Maps. Pengguna yang mahu mencuba eksperimen Maps boleh membaca [panduan berasingan](NEXT_INSTALL_MS.md). Jangan menganggap kejayaan build sebagai pengesahan navigasi atau sokongan semua app.

## Preview kereta sendiri

Nak guna kereta sendiri seperti preview Saga? Ikut **[panduan preview kereta](CAR_PREVIEW_MS.md)**. Video sendiri boleh diimport terus; preview8 sudut memerlukan PNG transparent, atlas dan build APK sendiri. Panduan merangkumi contoh prompt, susunan8 sudut, alat packing dan lokasi menukar nama model. Menu import foto belum ada dalam Next1.3.

## Apa yang perlu diuji sebelum stable

Buka/tutup launcher, tekan Home dan kembali ke launcher asal; uji Apps/Settings dan simpan layout; uji jam selepas restart/sleep/wake; cuba video/web fullscreen dan Back; ambil Performance check. Rekod model, **API sebenar**, resolusi, versi app dan langkah menghasilkan masalah. Uji eksperimen Maps secara berasingan. Hardware yang belum diuji tidak disenaraikan sebagai disokong/disahkan.

[Laporkan masalah](https://github.com/shazwannoor95-jpg/NexaDash/issues/new?template=nexadash-beta.md). Jangan sertakan akaun, kata laluan, API key, signing key atau lokasi rumah dalam rakaman/log.

## Source dan lesen

[Source branch](https://github.com/shazwannoor95-jpg/NexaDash/tree/main) · [Source ZIP](https://codeload.github.com/shazwannoor95-jpg/NexaDash/zip/refs/heads/main) · [Lesen projek](../LICENSE) · [Kredit dan lesen komponen](../ATTRIBUTIONS.md).

Lesen MIT projek tidak memberikan lesen kepada APK Google Maps/YouTube atau aset pihak ketiga. Eksperimen companion tidak patut dianggap sebahagian daripada pelepasan open-source berlesen MIT; hak pengedaran pihak ketiga perlu disemak sebelum pengedaran awam lanjut. Private signing backup mesti disimpan di luar repo. Source Android Studio menggunakan identiti Lab secara lalai; [build Next](../tools/next/README.md) menghasilkan identiti Next dengan signing key sendiri.

## Next1.3

Pembaca/pemain Quran offline tidak disertakan. Menu dashboard memang tidak menawarkannya sebelum ini; update ini turut membuang baki kelas pembaca, service, notch, data dan resource lama daripada APK. Jadual solat JAKIM dan fungsi dashboard kekal. Ciri panel native masih berstatus eksperimen seperti jadual di atas; ini bukan update untuk dua app native serentak.

## Radio follow-up Next1.5

**Settings → Export Radio files** menyimpan lima binary firmware yang sudah dikenal pasti dan laporan hasil bacaan ke ZIP pilihan pengguna. Update ini membantu mendapatkan kod/library yang tidak dibawa oleh backup APK; ia belum menambah kawalan atau hosting Radio FM. [Panduan export](RADIO_EXPORT_MS.md).

## Radio follow-up Next1.6

Lima fail export pemilik lengkap dan kod dispatcher Radio berjaya dipulihkan. **Settings → Radio runtime paths → Copy report** kini membaca satu property pemilih FM dan metadata tiga driver tanpa membuka driver atau mengawal tuner. Report itu menentukan app FM sebenar yang dipilih firmware dan izin akses yang kelihatan kepada UID launcher. [Penemuan dan langkah berikutnya](RADIO_RUNTIME_FILES_REVIEW.md). Companion/native host, saiz panel dan fungsi Maps tidak berubah dalam update ini.

## Preview kereta Next1.7

Set asal8 sudut dengan plat dipulihkan atas permintaan pemilik. Saiz atlas, urutan sudut, drag/pinch/reset dan had bitmap kekal. Update ini tidak membaiki Maps atau menambah Radio dalam panel.
