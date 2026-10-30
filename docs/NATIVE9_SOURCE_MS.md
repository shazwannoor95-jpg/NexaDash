# Native9 — pilihan native yang jelas dan laporan startup

Source: **1.6.7-nexadash.8-native9 / code41**. **Belum ada APK native9 bertandatangan untuk update terus APK GitHub lama.** Environment lama terputus dan kunci signing asal tidak dipulihkan. APK unsigned tidak boleh dipasang sebagai update; APK dengan kunci lain tidak sepadan dengan app/companion lama.

## Jika masih menggunakan APK GitHub native8

1. Buka pemilih panel kiri/kanan → **Native app · experimental**.
2. Tekan butang pilihan companion yang mungkin masih menunjukkan **YouTube V58** → pilih **TizenTube**.
3. Tekan **Start**. Ambil screenshot status penuh jika panel kosong atau app keluar.

Pilihan **TizenTube app controls** dan butang **Open app / Choose app** membuka app di luar dashboard. Ia bukan percubaan hosting. Penjelasan pengguna terakhir sepadan dengan laluan controls ini, jadi belum ada bukti laluan native Start benar-benar gagal pada unit tersebut.

## Perubahan source native9

- Pemilih panel ada **TizenTube · native panel (experimental)** yang terus memilih companion TizenTube. Tekan Start secara berasingan; tiada autoplay.
- Mod kawalan app lama kekal, tetapi dilabel **external app controls** dan **Open outside**. Teks TizenTube dalam mod controls boleh ditekan untuk beralih ke native panel.
- Companion dipilih secara bebas dan disimpan untuk panel kiri/kanan.
- Host tidak lagi menganggap display yang diminta sebagai bukti kejayaan. Ia meminta actual display daripada bridge semasa startup, maksimum60s. Jawapan display0/tiada Activity diberi masa penuh kerana instance setup lama boleh masih dilaporkan.
- Probes berhenti selepas laporan display sepadan, Stop atau timeout. Laporan disimpan secara private dan disertakan dalam **Settings → App panel compatibility** bersama provider WebView yang aktif, jika boleh dibaca.
- Laporan display pada startup bukan pengesahan playback, sentuhan semua dialog, routing atau kestabilan berterusan. Tiada polling kekal selepas startup. Binder sync yang tidak menjawab boleh menyebabkan worker mengambil masa lebih lama untuk berhenti walaupun UI timeout sudah membersihkan display/queue.
- Proxy memeriksa actual display dan generation. Callback lama selepas Stop tidak boleh mengesahkan startup baharu.
- Layout native8 dikekalkan; companion TizenTube tidak diubah berdasarkan hipotesis singleTask sahaja. Tiada app OEM atau sambungan DiAuto dijalankan.

## Isu metadata versi yang ditemui

Fail bernama `NexaDash-NativeLab-1.6.7-nexadash.8-native8.apk` mengandungi manifest **versionName native1 / code33**. Nama fail bukan bukti versi manifest. Source canonical manifest dan Gradle native9 kini41/native9; shell build menggunakan `--replace-version` supaya override versi benar-benar digunakan. APK lama ditinggalkan byte-for-byte; metadata penerangan diperbetulkan, bukan diganti dengan APK bertandatangan lain.

## Android Studio / signing

[Source ZIP](https://codeload.github.com/shazwannoor95-jpg/klikdash/zip/refs/heads/nexadash-native-panel-experiment). Open folder yang mempunyai `settings.gradle`, jalankan module **panelprobe** kemudian **app** pada emulator bersih untuk mencuba Demo dengan debug key yang sama. Companion ARM TizenTube tetap memerlukan device/ABI yang sesuai dan signing key sepadan; source host sahaja tidak menukar signature APK companion.

Debug APK yang dibina di komputer sendiri tidak semestinya boleh update APK GitHub pada head unit. Kunci private asal diperlukan untuk update yang sepadan. Kunci itu tidak boleh diperoleh daripada sijil awam dalam APK. Tiada arahan uninstall/reset data diberikan oleh perubahan ini.

## Semakan

- Resources + Java + DEX + zip alignment unsigned lulus, manifest output native9/code41 disahkan.
- Gradle release unsigned, compile Java debug app dan compile debug companion lulus.
- Semua source ujian dikompilasi, termasuk ujian pemilihan native dan state startup menggunakan Binder sintetik.
- Ujian runtime native9 **belum dijalankan** selepas workspace diganti. Keputusan emulator37 checks native8 ialah sejarah, bukan keputusan native9.
- Laporan eksport APK: [HEAD_UNIT_EXPORT_REVIEW_MS.md](HEAD_UNIT_EXPORT_REVIEW_MS.md).
