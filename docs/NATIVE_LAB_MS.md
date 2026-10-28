# NexaDash .8 Native Lab — install dan cuba

**Build eksperimen berasingan**, package `com.shazwan.nexadash.lab`, versi **1.6.7-nexadash.8-native1 / code33**. Boleh dipasang bersama NexaDash .8 biasa. Setting/data .8 biasa tidak diganti atau dipindahkan. Jangan jadikan Lab default launcher sebelum ujian sendiri memuaskan.

## Download

- [NexaDash Native Lab APK (~5.5MB)](https://raw.githubusercontent.com/shazwannoor95-jpg/klikdash/nexadash-native-panel-experiment/dist/native-lab/NexaDash-NativeLab-1.6.7-nexadash.8-native1.apk)
- [Source ZIP Android Studio](https://codeload.github.com/shazwannoor95-jpg/klikdash/zip/refs/heads/nexadash-native-panel-experiment)

Pilih companion yang hendak diuji, **install companion dahulu, kemudian Lab**. APK download cloud menggunakan sijil sama. APK asal tanpa bridge tidak akan menjadi native player melalui pilihan ini.

| Companion | Download / keperluan | Status |
|---|---|---|
| Demo | [APK kecil](https://raw.githubusercontent.com/shazwannoor95-jpg/klikdash/nexadash-native-panel-experiment/dist/native-lab/NexaDash-DemoCompanion-native1.apk); API26+ | Counter, input teks dan video ujian sintetik; bukan YouTube. Diuji dalam panel sebenar pada emulator API29 |
| TizenTube ARM | [APK](https://raw.githubusercontent.com/shazwannoor95-jpg/klikdash/nexadash-native-companions/TizenTube-NativeLab-arm.apk); minAPI24, ARM32 | Fork berasingan; manifest/signature/CRC diperiksa. Rendering/playback/touch belum diuji pada hardware ARM. Calon ujian head unit API27 |
| YouTube V58 | [ZIP companion](https://codeload.github.com/shazwannoor95-jpg/klikdash/zip/refs/heads/nexadash-v58-companion); **minAPI29** | Extract semua, jalankan `Join-V58-Windows.cmd`, dapat `YouTube-V58-native1.apk`. Original V58 tidak mempunyai bridge. MicroG/setup masih diperlukan |

API host untuk native display: **API26+**. Minimum Lab APK kekal API24, tetapi ciri native disekat secara jelas sebelum API26. **V58 tetap tidak boleh dipasang secara biasa pada head unit API27.** Menukar APK launcher tidak mengubah minimum SDK companion. Untuk hardware API27, mula dengan Demo atau calon TizenTube ARM. Cobalt ARM tidak berjalan pada emulator x86-only.

APK V58 >100MB, jadi GitHub menyimpannya dalam tiga bahagian 90MB/90MB/baki pada branch berasingan. `Join-V58-Windows.cmd` mencantum menggunakan `copy /b` dan mencetak SHA256; bandingkan dengan `artifact.json`. Linux/Termux boleh guna `sh Join-V58-Termux.sh` tanpa simbol pipe. Jangan install fail `.part` satu-satu. Source ZIP launcher tidak membawa APK besar itu.

## Cara dalam panel kiri / kanan

1. Buka **NexaDash Native Lab** dari Apps, semak footer **1.6.7-nexadash.8-native1**.
2. Tekan pilihan di penjuru atas panel kiri atau kanan → **Native app · experimental**.
3. Tekan butang pilihan app dalam panel, pilih **YouTube V58 / TizenTube / Demo** yang sepadan dengan companion terpasang.
4. Tekan **Start**. Tiada auto-launch pada load/resume. Startup dibatalkan selepas 60 saat jika companion/display tidak bersedia; tiada auto-retry. App native diharapkan muncul dalam permukaan panel; status error dipaparkan jika firmware/companion menolak.
5. **Stop** melepaskan display, binding dan worker input milik Lab. Tukar mode, edit layout atau Home/onStop juga melepaskannya. Selepas Done/resume, tekan Start semula jika mahu.
6. Butang **⋯** membuka **Back**, **Open app / setup**, dan proxy teks. Tap medan teks dalam app dahulu, taip pada medan host, kemudian **Send**. Tidak semua dialog atau input app menyokong kaedah ini. Setup awal MicroG/V58 dibuat secara normal melalui Open app/setup, bukan melalui panel.

Untuk jimat sumber, hanya **satu panel native/web/video fail** dipilih pada satu masa. Panel sebelah boleh kekal GPS map preview atau media controls. Ini belum menjadi host universal dua app asal yang tidak diubah.

## Android Studio

Extract Source ZIP ke folder baharu, Open folder dengan `settings.gradle`. Pilih module **panelprobe** dan Run dahulu, kemudian pilih **app** dan Run. Kedua-duanya menggunakan debug key tempatan yang sama. Dalam Lab, pilih companion **Demo** dan Start; counter serta medan teks boleh diuji terus tanpa APK mod atau internet.

Untuk YouTube/Cobalt yang didownload: **cloud companion tidak sama signing key dengan Studio debug app**. Jalankan dari terminal projek pada Windows:

```bat
gradlew.bat :app:resignNativeCompanion "-PnativeCompanion=C:\Downloads\YouTube-V58-native1.apk"
```

Task ini menandatangani salinan menggunakan debug key Studio komputer itu; output di `app/build/outputs/native-companion/*-debug.apk`. Install salinan debug itu bersama app debug. Ia tidak menyalin/menerbitkan private key. Alternatif paling mudah untuk cuba companion cloud: install APK cloud Lab dan companion cloud sebagai pasangan, tanpa Run versi debug menimpanya.

Gunakan emulator ujian bersih jika ada signing conflict. Konflik pada APK Lab/Demo tidak memerlukan uninstall NexaDash .8 biasa. Source dan resource namespace Java masih `com.shazwan.sagadash`, tetapi application ID Lab berasingan.

## Peta / directions

Carian destinasi dan directions .8 dikekalkan: **Navigation → alamat/tempat → pilih Maps/Waze → Search/Directions**. Routing dan suara dijalankan dalam app Maps/Waze **di luar panel**. Panel peta masih GPS preview. Lab ini **belum meng-embed UI asal Maps/Waze**, dan bukan navigasi penuh dalam panel.

## Semakan dan had sebenar

- **708 assertions** pada emulator Android10/API29: app asal .8, native display cross-UID di panel kiri/kanan, sentuhan berskala ke counter sebenar companion, input text proxy ke EditText app berasingan, explicit-start/no-autoresume, one-player limit serta cleanup.
- Build shell dan Gradle debug/release + panelprobe berjaya. APK signature/ZIP alignment/CRC diperiksa. Debug re-sign task diuji dan certificate pasangan dibandingkan.
- YouTube V58 prototaip terdahulu merender UI native dan skrin Search melalui sentuhan. Ini **bukan bukti video streaming berjaya**. Live YouTube playback masih belum disahkan; persekitaran ujian menunjukkan masalah sambungan.
- Terdapat log error lifecycle **Primes** dalam proses V58 semasa teardown pada prototaip terdahulu dan teardown ujian Lab. Companion bukan crash-free yang disahkan. Window dialog, IME, back handling dan lifecycle semua app belum lengkap. Lab mengasingkan proses target dan melepaskan sumber miliknya; ia tidak memaksa-stop semua service/background playback milik companion.
- Main Lab tidak meminta MANAGE_ACTIVITY_TASKS, INJECT_EVENTS atau system hosting permission. Ia hanya menerima companion yang opt-in, mempunyai service signature-protected, protocol sesuai dan signing key sama. Tiada auto-patch APK asal, bypass DRM/secure-window/login atau root command.
- Tiada ukuran RAM/CPU head unit fizikal. Default tidak memulakan native display, binding atau input worker sebelum Start; relay input mempunyai queue terhad. Ini tidak menjamin penggunaan rendah oleh YouTube/Cobalt/MicroG sendiri.

Source: [native frame](../src/com/shazwan/sagadash/DashNativeFrame.java), [proxy](../src/com/shazwan/sagadash/DashNativeProxy.java), [tests](../tests/DashNativeChecks.java), [V58 patch](../tools/v58-panel-lab), [Cobalt patch](../tools/native-companion). Cobalt asal dan notis/lesen pihak ketiga dikekalkan dalam companion; ia bukan keluaran rasmi Google/Cobalt dan bukan APK standard .8.

![Demo companion native dalam panel dashboard](img/native-lab-demo-left.png)
