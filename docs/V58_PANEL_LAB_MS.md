# Keputusan eksperimen V58 native panel — 28 Oktober 2026

**Bukan fungsi NexaDash .8.** Ini dua APK lab berasingan pada emulator Android10/API29 x86_64. UI app V58 sebenar dapat dipaparkan di SurfaceView host melalui private VirtualDisplay, tanpa menggunakan PiP.

## Apa yang diubah/dicuba

- APK asal pengguna: `com.gold.android.youtube`, YouTube Pro21.26.364, SHA256 `a0770d82fd2afcacf6872f15d10844bc570c6ceb6d2035e8bf14968b7f21324c`.
- Apktool2.11.1 decode/rebuild, framework Android SDK, javac/d8 dan apksigner digunakan. REA tidak tersedia sebagai tool dalam sesi ini; jangan anggap REA dijalankan.
- Tambah opt-in `allowEmbedded` / `resizeableActivity` pada activities, provider yang merekod Activity aktif, dan service input Binder dengan signature permission. Tambah DEX sendiri, sign semula APK. Minimum **API29 dikekalkan**.
- Host memulakan activity proxy sendiri pada private display, kemudian activity YouTube di display itu. Tiada system hosting/input permission ditambah kepada app.
- APK asal tanpa MicroG terus membuka `morphe.software/microg`. Dalam emulator, dependency rasmi ReVanced GmsCore0.3.13.2.250932 dipasang dari release GitHub. Permintaan pengecualian battery optimization diselesaikan melalui UI Android biasa, di luar panel, pada emulator sahaja. Tiada akaun peribadi digunakan.

## Bukti yang berjaya

- `dumpsys activity` menunjukkan host pada display0 dan Activity package YouTube pada private display bukan0, dengan UID berlainan.
- [UI YouTube sebenar dalam ruang host](img/v58-native-panel-lab.png) termasuk dialog MicroG dapat dirender.
- Selepas setup biasa dan host dimulakan semula, sentuhan pada ikon Search di SurfaceView host membuka [skrin Search YouTube native](img/v58-native-search-lab.png). App host menghantar MotionEvent melalui service kepada Activity sasaran; ia bukan player web NexaDash.
- APK salinan eksperimen lulus semakan signature dan masih melaporkan minSDK29. Signature verification bukan pemeriksaan keselamatan mod.

## Apa yang belum berjaya / belum disahkan

- Playback video **belum disahkan**. UI menunjukkan `Connect to the internet`; akses YouTube dari rangkaian workspace juga disekat. Kebergantungan/configuration app turut boleh mempengaruhi hasil; jangan anggap hanya satu punca.
- Bridge Activity tidak meliputi semua window dialog, keyboard/IME, back/key handling, accessibility dan lifecycle. Sentuhan pada dialog MicroG tidak menyelesaikan prompt itu dalam ujian panel; setup dibuat secara normal di luar panel. Selepas target force-stop, host perlu dimulakan semula untuk bind semula.
- Belum ada hosting ini dalam panel kiri/kanan **DashboardActivity NexaDash .8**. Tiada APK YouTube mod dibundel atau dipush ke GitHub.
- API27 head unit **tidak serasi** dengan minimum API29 V58. Ujian emulator ini tidak menyelesaikannya. Menukar nombor SDK sahaja bukan backport yang disahkan.
- Tiada jaminan Maps/Waze/WhatsApp/Radio yang tidak diubah boleh dihost melalui kaedah ini. Setiap target memerlukan opt-in dan integrasi sendiri, atau sokongan host OEM/privileged yang sah.
- Tiada ukuran RAM/CPU fizikal head unit atau jaminan stutter/crash untuk native hosting. Masa startup software emulator bukan benchmark head unit.

Source lab yang boleh disemak: [tools/v58-panel-lab](../tools/v58-panel-lab). APK pihak ketiga dan key/password tidak termasuk.

## Langkah seterusnya pada head unit

Cuba [generic opt-in panel probe tanpa ADB](OPTIN_PANEL_PROBE_MS.md) dahulu. Dua APK kecil itu boleh diuji pada API27 dan mempunyai app target sendiri yang bekerjasama. Keputusan firmware diperlukan sebelum membina integrasi production.

TizenTube Cobalt2.0.3 rasmi telah disemak: package `io.gh.reisxd.tizentube.cobalt`, minSDK24 / target36; release menyediakan ARM dan ARM64. Varian ARM sesuai dengan ABI head unit berdasarkan manifest. Ia belum dipatch/diuji sebagai native panel; emulator ini hanya x86_64/x86, jadi tidak menjalankan varian ARM. Keserasian install sahaja bukan bukti hosting/playback.
