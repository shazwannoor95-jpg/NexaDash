# NexaDash — launcher head unit Android

![Dashboard NexaDash](https://raw.githubusercontent.com/shazwannoor95-jpg/klikdash/nexadash-native-panel-experiment/docs/img/nexadash-dashboard-public.png)

*Contoh reka bentuk versi terdahulu, disunting untuk mengosongkan plat. Susunan Next semasa mungkin berbeza.*

NexaDash menyediakan dashboard dengan panel kaca, jam, maklumat solat, preview kereta, shortcut app dan susunan yang boleh diubah.

**[Download NexaDash Next 1.2](https://raw.githubusercontent.com/shazwannoor95-jpg/klikdash/nexadash-native-panel-experiment/dist/next/NexaDash-Next-1.2.apk)** · **[Panduan pemasangan head unit](https://github.com/shazwannoor95-jpg/klikdash/blob/nexadash-native-panel-experiment/docs/PUBLIC_BETA_MS.md)** · [Lapor masalah](https://github.com/shazwannoor95-jpg/klikdash/issues/new?template=nexadash-beta.md)

Versi semasa ialah **public beta**, belum disahkan stabil pada head unit fizikal. Android minimum7.0/API24; eksperimen native display memerlukan API26+. Sokongan firmware perlu diuji pada peranti sendiri.

## Mula guna

1. Download APK Next1.2 dan install melalui file manager head unit.
2. Buka **NexaDash Next** dari Apps dan cuba dashboard dahulu.
3. Selepas menguji, pilih **NexaDash Next** dalam Android Default launcher jika firmware menyediakan pilihan itu. Kekalkan launcher asal untuk kembali jika perlu.

Next dipasang berasingan daripada NexaDash lama/Native Lab. Update Next1.0/1.1 kepada1.2 menggunakan package dan signing key sama. Pemasangan launcher asas tidak memerlukan companion Maps.

## Ciri dan status

| Bahagian | Status semasa |
| --- | --- |
| Dashboard kaca, Apps/Settings dan susunan panel | Tersedia dalam beta |
| Preview kereta |8 imej sudut; import video sendiri tersedia |
| Local video / YouTube web | Tersedia; playback dan fullscreen perlu disahkan pada peranti |
| Navigation shortcut | Membuka app Maps/Waze berasingan |
| Google Maps native dalam panel | Eksperimen companion; rendering, carian, route dan suara belum disahkan pada head unit |
| Radio FM native / Waze native dalam panel | Belum siap |

## Panduan

- [Tukar launcher head unit dan install beta](https://github.com/shazwannoor95-jpg/klikdash/blob/nexadash-native-panel-experiment/docs/PUBLIC_BETA_MS.md)
- [Preview kereta sendiri: video,8 sudut PNG dan contoh prompt](https://github.com/shazwannoor95-jpg/klikdash/blob/nexadash-native-panel-experiment/docs/CAR_PREVIEW_MS.md)
- [Maps companion, status native panel dan Radio](https://github.com/shazwannoor95-jpg/klikdash/blob/nexadash-native-panel-experiment/docs/NEXT_INSTALL_MS.md)
- [Ukuran CPU/RAM proses launcher](https://github.com/shazwannoor95-jpg/klikdash/blob/nexadash-native-panel-experiment/docs/PREVIEW_PERFORMANCE_MS.md)

Plat pada aset awam semasa telah dikosongkan. APK, screenshot dan video lama yang berisiko telah ditarik; gunakan pautan Next1.2 di atas.

## Source dan build

**[Source dashboard NexaDash semasa](https://github.com/shazwannoor95-jpg/klikdash/tree/nexadash-native-panel-experiment)** · [Download source ZIP](https://codeload.github.com/shazwannoor95-jpg/klikdash/zip/refs/heads/nexadash-native-panel-experiment) · [Arahan build Next](https://github.com/shazwannoor95-jpg/klikdash/blob/nexadash-native-panel-experiment/tools/next/README.md)

Untuk membina dashboard dalam gambar, gunakan branch **`nexadash-native-panel-experiment`**. Branch `main` menyediakan halaman projek dan masih menyimpan source asas terdahulu. Build Android Studio lalai menggunakan identiti Lab; build Next mempunyai langkah/signing berasingan seperti dalam panduan. Simpan signing key di luar repo.

## Lesen dan kredit

[Lesen projek](LICENSE) · [Kredit asal dan lesen komponen](ATTRIBUTIONS.md)
