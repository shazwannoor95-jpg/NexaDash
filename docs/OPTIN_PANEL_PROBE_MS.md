# Demo panel native tanpa ADB (eksperimen berasingan)

Ini **bukan** YouTube/TizenTube/Maps dan **bukan** update launcher. Dua app kecil ini menguji sama ada firmware boleh memaparkan app yang bekerjasama dalam private VirtualDisplay dan meneruskan sentuhan melalui Binder. Min Android 8.0/API26. Tiada internet, root, accessibility, screen capture atau system input permission digunakan. Bridge dilindungi custom permission bertaraf signature; kedua-dua APK ditandatangani sijil sama dan mempunyai UID berlainan.

1. Download dan install [target](../dist/experimental/OptInPanel-target.apk?raw=true) dahulu.
2. Install [host](../dist/experimental/OptInPanel-host.apk?raw=true).
3. Buka **Opt-in display host** dari Apps. Jangan buka target berasingan untuk menilai hosting.
4. Cari tulisan **SEPARATE NATIVE APP · display N** di dalam ruang host. N mestilah bukan 0.
5. Tekan **TOUCH COUNTER**. Ia patut meningkat dan status host menunjukkan nilai sama. Warna video ujian kecil membuktikan surface decoder, bukan playback YouTube.
6. Tekan **STOP / RELEASE DISPLAY**, kemudian tutup. App ujian boleh dinyahpasang selepas keputusan dirakam.

Jika gagal, screenshot mesej **DISPLAY ERROR / TARGET LAUNCH ERROR / Touch bridge**. Tidak perlu ADB untuk ujian ini. Berjaya hanya mengesahkan laluan untuk app yang opt-in dan mempunyai bridge; ia tidak menjadikan semua installed apps boleh di-embed. App sasaran seperti YouTube/TizenTube/Maps/WhatsApp masih perlu integrasi dan pengesahan sendiri.

Pada emulator Android10/API29, demo asal berjaya memaparkan Activity app berasingan di display1 dan counter bertukar 0→1 melalui sentuhan pada host. Belum disahkan pada TEST9278B API27. Demo pendek ini bukan komponen production dan tidak mengukur RAM/CPU head unit.

Source: [tools/optin-panel-probe](../tools/optin-panel-probe). Build dengan Android SDK35/build-tools36, JDK17 dan signing key sendiri (set `ANDROID_HOME`, `PROBE_KEYSTORE`, `KLIKDASH_KS_PASS`). Jangan commit key/password. Kedua-dua APK mesti menggunakan key sama.

Kaedah ini menggunakan opt-in `android:allowEmbedded` serta host-owned proxy pada private display; tiada spoof UID/system permission. Perlindungan secure content tidak diubah. Rujukan platform: Android 8.1 `ActivityStackSupervisor.isCallerAllowedToLaunchOnDisplay` dalam AOSP.
