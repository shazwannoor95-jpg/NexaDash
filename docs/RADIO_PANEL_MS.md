# Radio FM dalam panel NexaDash

Next1.8 menambah **kawalan FM native dalam panel**: Start/Stop, masukkan frekuensi87.50–108.00MHz, langkah±0.1MHz, enam preset dan senarai frekuensi yang dibalas oleh servis FM. UI dilukis oleh NexaDash; servis Radio asal head unit mengurus tuner/audio. Ini bukan paparan Activity Radio asal, PiP atau radio Internet.

**Ujian bunyi/tuner pada TEST9278B masih memerlukan hasil pemilik.** Kejayaan build, arahan diterima servis dan balasan status bukan pengesahan ada bunyi atau frekuensi RF sebenar sudah berubah.

## Mula pada head unit

1. Install **[NexaDash Next1.9](https://raw.githubusercontent.com/shazwannoor95-jpg/NexaDash/main/dist/next/NexaDash-Next-1.9.apk)** atas Next sedia ada. Package/signing key sama.
2. Tekan ikon **Radio** pada dock. Ia memilih **Radio FM** dalam panel kanan. Radio juga boleh dipilih daripada menu panel kiri/kanan.
3. Kekalkan **Auto**, kemudian tekan **Start FM**. Untuk FM dalaman, tunggu initialization servis selesai sebelum tune dihantar. Had menunggu ialah60s.
4. Ketuk nombor frekuensi atau **Tune**, masukkan frekuensi stesen yang diketahui dan tekan Tune. Butang±0.1 menukar pilihan sebanyak0.1MHz. Ketika Stop, ia hanya memilih frekuensi untuk Start seterusnya.
5. Tahan P1–P6 untuk menyimpan pilihan semasa; ketuk untuk menggunakannya. Dalam panel kecil, preset tersedia melalui **⋯ → Presets / Save preset**.

UI mengikut ruang panel dan mempunyai susunan compact. Memilih panel, menukar saiz dan kembali ke dashboard tidak auto-start radio. Satu panel FM sahaja dibenarkan; panel sebelah boleh kekal pada car preview atau companion native.

**Tiada APK companion Radio tambahan atau ADB diperlukan oleh laluan ini.** App Radio firmware yang serasi mesti sudah terpasang pada head unit. Pada emulator/telefon tanpa servis FM itu, UI akan memaparkan ralat servis tidak tersedia; ia tidak menghasilkan audio FM palsu.

## Auto dan pilihan sumber

Auto menggunakan cabang firmware yang dipulihkan daripada Radio dispatcher: property `persist.sys.xy.proinfo.exfm`. Pemilih **Auto ▾** juga menyediakan **Built-in FM · MediaTek** dan **External FM · GalaRadio** secara manual. Menukar sumber menghantar Stop bagi sesi yang dimulakan panel dan memerlukan Start semula.

Adapter ini berdasarkan versi yang terdapat dalam arkib pemilik:

| Sumber | App/versi | Servis |
|---|---|---|
|Built-in FM|`com.android.fmradio`3.1|`com.android.fmradio.FmtestService`|
|External FM|`com.acloud.stub.extradio`2.3.24|`com.radio.service.RadioService`|

App/servis mesti enabled, exported dan permission servis mesti tersedia kepada caller. Versi lain ditolak daripada dihantar arahan oleh adapter ini. Kehadiran nama package sahaja bukan bukti semua firmware head unit serasi.

## Apa yang panel lakukan?

- **FM dalaman:** Intent servis dengan`flag=0` membuka device dan menjalankan initialization/factory scan sedia ada. Panel menunggu balasan lengkap`freq:int[]` sebelum menghantar`flag=1` dengan`freq:float` dalamMHz. Progress`freqone` tidak dianggap siap. Stop menggunakan`flag=2`.
- **FM luaran:** Intent explicit kepada servis dengan action`com.android.xygala.ACTION_CONTROL_RADIO`; method`method_open`, `method_setFreq` dengan`param_freq:int` dalamkHz, dan`method_close`.
- **Preset:** disimpan dalam data NexaDash, bukan menulis senarai preset/factory setting app OEM.
- **Apabila keluar dashboard:** panel menghantar permintaan Stop untuk sesi yang dimulakannya, membuang receiver dan timer. Kembali memerlukan Start secara sengaja.

Tiada panggilan `stopService` digunakan: OEM RadioService boleh membunuh prosesnya dalam onDestroy. Tiada mod APK, shared UID sistem, root, library OEM dimuat ke proses NexaDash, akses driver terus atau arahan MCU mentah diperlukan oleh client ini. Band/area/sensitivity firmware asal dikekalkan.

## Had semasa dan laporan

Paparan frekuensi ialah **pilihan/request NexaDash**. Teks **Tune requested** tidak bermakna playback telah disahkan. Balasan radio-state juga hanya laporan servis; dalam implementation OEM, status boleh dihantar walaupun JNI/tuner gagal. Senarai **Stations** datang daripada hasil initialization, boleh kosong atau separa, dan tidak dijanjikan sebagai scan lengkap seluruh band. RDS/nama stesen, AM dan auto-seek umum belum disediakan.

Initialization OEM adalah kerja synchronous di proses servis. Jika Stop ditekan ketika servis masih sibuk, arahan tutup boleh beratur sehingga operasi itu kembali. Panel tidak menjamin audio atau proses OEM terus berhenti/memulangkan RAM serta-merta. Fokus audio ketika app lain bermain masih perlu diuji pada head unit.

Jika tiada bunyi atau initialization gagal, tekan Stop dan buka **⋯ → Radio report**, atau **Settings → Radio runtime paths → Copy report**. Report menyertakan komponen, stage, permintaan terakhir dan frekuensi pilihan. Hantar teks itu bersama hasil sebenar: adakah suara FM keluar dan adakah Tune menukar stesen. Fail Radio yang sudah diterima tidak perlu diexport semula.

[Penemuan fail firmware](RADIO_RUNTIME_FILES_REVIEW.md) · [Protokol servis dan batas ujian](RADIO_SERVICE_PROTOCOL.md)

## Pengesahan build1.8

133 semakan Android lulus pada emulator API27 dengan dua servis simulasi berasingan UID, serta25 semakan JVM bagi frekuensi/unit. Semakan meliputi sentuhan native, Tune/preset, susunan initialization, Stop, layout compact dan kembali dari Settings tanpa auto-start. Build debug/release Android Studio, tandatangan API27 dan integriti APK turut lulus. Servis simulasi tidak menjalankan library tuner atau menghasilkan audio FM. [Rekod QA dan batas ujian](qa/radio-panel-next18-api27.json).
