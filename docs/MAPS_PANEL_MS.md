# Maps dalam panel — status ujian pemilik

> **Keputusan pemilik31 Oktober selepas Next1.9: masih tidak berhasil.** Display707x480@160dpi dan ID yang sepadan tidak menyelesaikan Maps companion. [Rekod terbaru](qa/owner-feedback-next19-20261031.json) · [Handoff Claude Code](CLAUDE_HANDOFF.md).

> **Next1.9:** [Peta native NexaDash dengan carian/laluan dan calon pembaikan layout Maps](PANEL_MAP_NEXT19_MS.md). Pemerhatian pemilik di bawah kekal belum diuji semula pada head unit. APK companion tidak berubah.

Rakaman TEST9278B menunjukkan **video TizenTube bergerak dalam panel kiri** ketika UI Maps berada di kanan. Maps memaparkan **Latest in the area / Explore** dan bottom navigation, tetapi kandungan peta/carian masih kosong. Pemilik melaporkan Maps companion berfungsi apabila dibuka di luar panel, dengan sedikit tersangkut-sangkut; Maps asal belum dibandingkan. Maps sahaja dalam panel juga masih kosong walaupun TizenTube dihentikan. [Rekod pemerhatian](qa/head-unit-native-owner-20261030.json).

Ini mengesahkan video TizenTube dalam sesi yang dirakam, bukan semua fungsi/kawalan, audio focus atau kestabilan jangka panjang. UI Maps berjaya dilukis; peta, carian, route dan suara dalam panel belum disahkan. Punca khusus belum diketahui. RAM1GB sahaja tidak membuktikan punca lag, dan kejayaan penuh skrin tidak menentukan satu punca bagi panel kosong.

## Langkah layout yang masih belum dicuba

Jika **Latest in the area** memenuhi panel, cuba tarik pemegang kecil di tengah atas helaian itu **ke bawah**. Perhatikan sama ada bahagian peta atau medan carian muncul di belakangnya. Pemilik belum mencuba langkah ini. Ia tidak memerlukan APK baharu dan belum dibuktikan menyelesaikan masalah.

Untuk teks, ketuk medan carian Maps dahulu, kemudian **⋯ → medan teks NexaDash → taip destinasi → Send**. Ia menghantar teks kepada editor app yang sedang fokus; medan NexaDash tidak menyediakan carian destinasi sendiri. Jika perlu setup permission, **⋯ → Open outside panel** membuka companion berasingan untuk setup. Kembali dan tekan Start selepas setup; kejayaan di luar panel tidak dikira sebagai kejayaan panel.

Sertakan hasil ujian itu dan **Settings → App panel compatibility → Copy report** apabila melaporkan masalah. Jangan sertakan akaun atau lokasi rumah.

## Update semasa

Next1.9 menambah peta sendiri dalam panel serta layout Maps Spacious/Compact yang menggunakan restart sesi dengan DPI160 tetap. Ini implementation baharu berasingan daripada eksperimen saiz lama yang tidak dilepas. Pemulihan tile/carian Google Maps sebenar masih memerlukan ujian pemilik; [langkah semasa](PANEL_MAP_NEXT19_MS.md).

Apabila dashboard berhenti kerana app lain memenuhi skrin, NexaDash melepaskan kedua-dua virtual display, input worker dan service binding. Kembali memerlukan Start secara sengaja. Ia tidak force-stop proses/servis client atau menjamin RAM terus dipulangkan. Performance check launcher juga tidak mengukur keseluruhan proses Maps/TizenTube.

[QA host dua native app API27](qa/dual-native-api27.json) ialah bukti host dengan app ujian berasingan. Ia bukan pengesahan Google Maps ARM, navigasi/voice atau semua kawalan TizenTube pada head unit fizikal.
