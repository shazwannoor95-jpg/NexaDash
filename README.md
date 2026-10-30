# NexaDash

Launcher head unit Android oleh **[shazwannoor95-jpg](https://github.com/shazwannoor95-jpg)**.

![Dashboard NexaDash](https://raw.githubusercontent.com/shazwannoor95-jpg/NexaDash/main/docs/img/nexadash-dashboard-public.png)

*Contoh reka bentuk versi terdahulu, disunting untuk mengosongkan plat. Susunan Next semasa mungkin berbeza.*

NexaDash menyediakan dashboard dengan panel kaca, jam, maklumat solat, preview kereta, shortcut app dan susunan yang boleh diubah.

**[Download NexaDash Next 1.6](https://raw.githubusercontent.com/shazwannoor95-jpg/NexaDash/main/dist/next/NexaDash-Next-1.6.apk)** · **[Panduan pemasangan head unit](https://github.com/shazwannoor95-jpg/NexaDash/blob/main/docs/PUBLIC_BETA_MS.md)** · [Lapor masalah](https://github.com/shazwannoor95-jpg/NexaDash/issues/new?template=nexadash-beta.md)

Versi semasa ialah **public beta**, belum disahkan stabil pada head unit fizikal. Android minimum 7.0/API24; eksperimen native display memerlukan API26+. Sokongan firmware perlu diuji pada peranti sendiri.

## Mula guna

1. Download APK Next 1.6 dan install melalui file manager head unit.
2. Buka **NexaDash Next** dari Apps dan cuba dashboard dahulu.
3. Selepas menguji, pilih **NexaDash Next** dalam Android Default launcher jika firmware menyediakan pilihan itu. Kekalkan launcher asal untuk kembali jika perlu.

Next dipasang berasingan daripada NexaDash lama/Native Lab. Update daripada Next1.0–1.5 menggunakan package dan signing key sama. Pemasangan launcher asas tidak memerlukan companion Maps.

## Ciri dan status

| Bahagian | Status semasa |
| --- | --- |
| Dashboard kaca, Apps/Settings dan susunan panel | Tersedia dalam beta |
| Preview kereta | 8 imej sudut; import video sendiri tersedia |
| Local video / YouTube web | Tersedia; playback dan fullscreen perlu disahkan pada peranti |
| Navigation shortcut | Membuka app Maps/Waze berasingan |
| Dua panel native | Host dua companion diuji dengan47 assertions pada emulator API27 (Next1.4); [panduan dan batasan](docs/DUAL_NATIVE_MS.md) |
| Maps + TizenTube dalam panel | Video TizenTube bergerak dalam rakaman pemilik; Maps UI muncul tetapi peta/carian dalam panel masih kosong — [status](docs/MAPS_PANEL_MS.md) |
| Radio FM native / Waze native dalam panel | Belum siap |
| Export fail Radio untuk analisis | Settings → Export Radio files; [panduan](docs/RADIO_EXPORT_MS.md) |

Next1.6 menambah semakan **pilihan app FM firmware dan metadata izin driver** dalam Settings → Radio runtime paths. Radio FM dalam panel masih belum siap. Companion Maps/TizenTube dan kod host dua panel tidak berubah.

## Panduan

- [Radio: fail firmware lengkap, penemuan dan semakan berikutnya](docs/RADIO_RUNTIME_FILES_REVIEW.md)
- [Export fail Radio terus dari head unit tanpa ulang APK Extractor](docs/RADIO_EXPORT_MS.md)
- [Dua app companion native serentak: APK dan ujian tanpa ADB](docs/DUAL_NATIVE_MS.md)

- [Tukar launcher head unit dan install beta](https://github.com/shazwannoor95-jpg/NexaDash/blob/main/docs/PUBLIC_BETA_MS.md)
- [Preview kereta sendiri: video, 8 sudut PNG dan contoh prompt](https://github.com/shazwannoor95-jpg/NexaDash/blob/main/docs/CAR_PREVIEW_MS.md)
- [Maps companion, status native panel dan Radio](https://github.com/shazwannoor95-jpg/NexaDash/blob/main/docs/NEXT_INSTALL_MS.md)
- [Ukuran CPU/RAM proses launcher](https://github.com/shazwannoor95-jpg/NexaDash/blob/main/docs/PREVIEW_PERFORMANCE_MS.md)

Plat pada aset awam semasa telah dikosongkan. APK, screenshot dan video lama yang berisiko telah ditarik; gunakan pautan Next 1.6 di atas.

## Source dan build

**[Source dashboard NexaDash semasa](https://github.com/shazwannoor95-jpg/NexaDash/tree/main)** · [Download source ZIP](https://codeload.github.com/shazwannoor95-jpg/NexaDash/zip/refs/heads/main) · [Arahan build Next](https://github.com/shazwannoor95-jpg/NexaDash/blob/main/tools/next/README.md)

Branch **`main`** mengandungi source NexaDash semasa. Build Android Studio lalai menggunakan identiti Lab; ikut arahan build Next untuk identiti APK awam. Simpan signing key di luar repo.

## Lesen dan kredit

NexaDash ialah fork daripada [KlikDash](https://github.com/ashafizullah/klikdash).

[Lesen projek](LICENSE) · [Kredit dan lesen komponen](ATTRIBUTIONS.md)
