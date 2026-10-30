# Kredit dan lesen NexaDash

NexaDash dibangunkan daripada [KlikDash](https://github.com/ashafizullah/klikdash), projek asal oleh **Adam Suchi Hafizullah**. Copyright asal **Copyright (c) 2026 Adam Suchi Hafizullah** dan teks lesen dikekalkan dalam [LICENSE](LICENSE). Pengembangan NexaDash, penyesuaian dashboard dan dokumentasi projek diselenggara oleh **[shazwannoor95-jpg](https://github.com/shazwannoor95-jpg)** melalui [repo NexaDash](https://github.com/shazwannoor95-jpg/NexaDash).

Nama produk dan panduan pengguna bagi fork ini ialah **NexaDash**. Tutorial, APK, badge prestasi dan status peranti projek asal tidak dianggap bukti untuk build NexaDash. Gunakan panduan pemasangan NexaDash semasa.

## Sumber dan lesen komponen

- **Code:** [MIT](LICENSE), except `PrayerTimes.java` (see prayer times below).
- **Fonts:** [SIL Open Font License 1.1](assets/licenses/): Big Shoulders, Martian Mono, Amiri Quran, and
  Literata (via Google Fonts). Literata is instanced and subset; its name and license are unchanged.
- **Text, transliteration, and translation** (not included): [equran.id](https://equran.id), i.e. the
  Kemenag RI standard mushaf and translation. Rights and terms belong to their owners.
- **English translation** (not included, downloaded on request): Saheeh International via
  [alquran.cloud](https://alquran.cloud) (from [Tanzil](https://tanzil.net/trans/)). Rights belong to its owners.
- **Audio** (not included): [everyayah.com](https://everyayah.com) / versebyversequran.com, Mishary Rashid
  Alafasy 128 kbps. Follow the license on that site.
- **Prayer times:** `PrayerTimes.java` is a Java port of [PrayTimes.js](https://praytimes.org) (© PrayTimes.org,
  Hamid Zarrabi-Zadeh, GNU LGPL v3.0, used with credit and a link as its terms of use ask).
- **Dhikr** (`assets/dzikir.json`): from [KlikBand](https://github.com/ashafizullah/klikband); order and counts from
  [Almanhaj](https://almanhaj.or.id/11518-dzikir-pagi-dan-petang.html), Latin following Hisnul Muslim, Qur'an
  verses from [Tanzil](https://tanzil.net) (© Tanzil Project, CC BY 3.0 with Tanzil's terms: verbatim copies only,
  with credit and a link). Rebuild with
  `node tools/build_dzikir.mjs <klikband>/src/common/dhikrmorningevening.js assets/dzikir.json`.

This project is not affiliated with NWD, Zjinnova (Zlink), Apple, Google, Anthropic, OpenAI, ElevenLabs, TinyFish,
or Kemenag RI. Product names are trademarks of their owners.

## Aset dan APK pihak ketiga

Lesen projek tidak melesenkan semula Google Maps, YouTube atau app vendor. Eksperimen companion berasingan tidak patut dianggap sebahagian daripada kod berlesen MIT. Tanda dagang serta hak aset pihak ketiga kekal pada pemiliknya. Foto input peribadi dan signing key tidak patut diterbitkan. Kredit/lesen font dan header lesen komponen dalam source kekal.

TizenTube/Cobalt Next companion is adapted from [TizenTubeCobalt2.0.3](https://github.com/reisxd/TizenTubeCobalt/tree/v2.0.3). Its upstream copyright/license is reproduced in [tools/tizentube-next/UPSTREAM_LICENSE.txt](tools/tizentube-next/UPSTREAM_LICENSE.txt), and original APK third-party notices/assets are retained. Host tests with A/B do not certify the upstream app.
