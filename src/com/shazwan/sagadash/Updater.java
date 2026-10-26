package com.shazwan.sagadash;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

/**
 * Cek & pasang pembaruan dari GitHub Releases. APK hanya dipasang bila ditandatangani kunci yang sama dengan
 * KlikDash yang terpasang (Android juga menolak pembaruan dengan kunci lain, ini cek lebih awal + pesan jelas).
 */
final class Updater {
    static final String ACTION_STATUS = "com.shazwan.sagadash.INSTALL_STATUS";

    static final class Release {
        String version, notes, apkUrl;
        long size;
    }

    interface Progress { void onProgress(int percent); }

    private Updater() { }

    /**
     * Catatan rilis (Markdown GitHub) sebagai teks biasa untuk dialog: [teks](url) → teks, "- " → "• ", tanpa
     * #, **, ` dan garis ---.
     */
    static String plainNotes(String md) {
        String s = md.replace("\r", "")
                .replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1")
                .replaceAll("(?m)^\\s*-{3,}\\s*$", "")
                .replaceAll("(?m)^#+\\s*", "")
                .replaceAll("(?m)^\\s*[-*] ", "• ")
                .replaceAll("[*`]", "")
                .replaceAll("\n{3,}", "\n\n");
        return s.trim();
    }

    /** Personal builds have no remote update channel. Keep this guard even if an old UI calls it. */
    static Release latest() throws Exception {
        return null;
    }

    /** &gt; 0 bila {@code a} lebih baru dari {@code b} ("1.10.0" &gt; "1.9.2"). */
    static int compare(String a, String b) {
        String[] x = a.split("[.-]"), y = b.split("[.-]");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? num(x[i]) : 0, q = i < y.length ? num(y[i]) : 0;
            if (p != q) return p - q;
        }
        return 0;
    }

    static int num(String s) {
        try {
            return Integer.parseInt(s.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static String installedVersion(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "0";
        }
    }

    static File download(Context c, Release r, Progress p) throws Exception {
        File dir = new File(c.getExternalFilesDir(null), "update");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException(c.getString(R.string.update_err_storage));
        for (File old : dir.listFiles()) old.delete();
        File f = new File(dir, "klikdash-" + r.version + ".apk");
        // browser_download_url mengalihkan ke CDN GitHub; HttpURLConnection mengikuti pengalihan https→https.
        HttpURLConnection con = (HttpURLConnection) new URL(r.apkUrl).openConnection();
        con.setConnectTimeout(15000);
        con.setReadTimeout(30000);
        con.setRequestProperty("User-Agent", "KlikDash");
        try {
            if (con.getResponseCode() >= 400) throw new IOException(c.getString(R.string.update_err_http, con.getResponseCode()));
            long total = con.getContentLengthLong() > 0 ? con.getContentLengthLong() : r.size;
            try (InputStream in = con.getInputStream(); OutputStream out = new FileOutputStream(f)) {
                byte[] buf = new byte[32768];
                long got = 0;
                int last = -1;
                for (int k; (k = in.read(buf)) > 0; ) {
                    out.write(buf, 0, k);
                    got += k;
                    int pct = total > 0 ? (int) (got * 100 / total) : 0;
                    if (pct != last && p != null) p.onProgress(last = pct);
                }
            }
        } finally {
            con.disconnect();
        }
        verify(c, f, r.version);
        return f;
    }

    /** Paket, versi, dan tanda tangan APK unduhan harus cocok dengan KlikDash yang terpasang. */
    static void verify(Context c, File apk, String version) throws Exception {
        PackageManager pm = c.getPackageManager();
        PackageInfo got = pm.getPackageArchiveInfo(apk.getPath(), PackageManager.GET_SIGNATURES);
        if (got == null) throw new IOException(c.getString(R.string.update_err_corrupt));
        if (!c.getPackageName().equals(got.packageName)) throw new IOException(c.getString(R.string.update_err_not_klikdash));
        if (!version.equals(got.versionName)) throw new IOException(c.getString(R.string.update_err_version));
        PackageInfo me = pm.getPackageInfo(c.getPackageName(), PackageManager.GET_SIGNATURES);
        if (!sameSigners(me.signatures, got.signatures)) {
            throw new IOException(c.getString(R.string.update_err_signature));
        }
    }

    static boolean sameSigners(Signature[] a, Signature[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        String[] x = new String[a.length], y = new String[b.length];
        for (int i = 0; i < a.length; i++) { x[i] = a[i].toCharsString(); y[i] = b[i].toCharsString(); }
        Arrays.sort(x);
        Arrays.sort(y);
        return Arrays.equals(x, y);
    }

    /**
     * Headunit dengan ADB lokal: pasang diam-diam lewat {@code pm install}. KlikDash berhenti saat paketnya diganti
     * (koneksi ADB ikut putus), tapi pemasangan diselesaikan sistem dan launcher dibuka lagi otomatis. Perintah latar
     * (nohup/&) tidak bisa dipakai: adbd mode lama mematikannya begitu koneksi ditutup.
     * Lainnya: PackageInstaller, Android menampilkan konfirmasi.
     */
    static void install(Activity a, File apk, Runnable onFail) {
        String path = apk.getPath().replace("'", "");
        new Thread(() -> {
            String out;
            try {
                out = Tweaks.exec("pm install -r '" + path + "' 2>&1", 120000);
            } catch (IOException e) {
                // Putus karena KlikDash sedang diganti (berhasil), atau ADB tidak ada: cek versinya.
                out = null;
            }
            boolean failed = out != null && !out.contains("Success");
            if (out == null && !isInstalled(a, apk)) {
                a.runOnUiThread(() -> installWithPrompt(a, apk, onFail));
            } else if (failed) {
                android.util.Log.w("KlikDash", "pm install: " + out);
                a.runOnUiThread(() -> installWithPrompt(a, apk, onFail));
            }
        }, "update-install").start();
    }

    /** Versi terpasang sudah sama dengan APK? (dipakai bila ADB putus di tengah pemasangan) */
    static boolean isInstalled(Context c, File apk) {
        PackageInfo got = c.getPackageManager().getPackageArchiveInfo(apk.getPath(), 0);
        return got != null && got.versionName.equals(installedVersion(c));
    }

    static void installWithPrompt(Activity a, File apk, Runnable onFail) {
        if (!Compat.canRequestPackageInstalls(a)) {
            // Sekali saja: izinkan KlikDash memasang aplikasi, lalu tekan PASANG lagi.
            a.startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    android.net.Uri.parse("package:" + a.getPackageName())));
            if (onFail != null) onFail.run();
            return;
        }
        try {
            PackageInstaller pi = a.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams sp = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            sp.setAppPackageName(a.getPackageName());
            int id = pi.createSession(sp);
            try (PackageInstaller.Session s = pi.openSession(id)) {
                try (InputStream in = new FileInputStream(apk); OutputStream out = s.openWrite("klikdash.apk", 0, apk.length())) {
                    byte[] buf = new byte[65536];
                    for (int k; (k = in.read(buf)) > 0; ) out.write(buf, 0, k);
                    s.fsync(out);
                }
                a.getApplicationContext().registerReceiver(new BroadcastReceiver() {
                    @Override public void onReceive(Context c, Intent i) {
                        int st = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
                        if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                            Intent confirm = i.getParcelableExtra(Intent.EXTRA_INTENT);
                            if (confirm != null) c.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        } else {
                            c.unregisterReceiver(this);
                            if (st != PackageInstaller.STATUS_SUCCESS && onFail != null) onFail.run();
                        }
                    }
                }, new IntentFilter(ACTION_STATUS));
                PendingIntent done = PendingIntent.getBroadcast(a, id,
                        new Intent(ACTION_STATUS).setPackage(a.getPackageName()), PendingIntent.FLAG_UPDATE_CURRENT);
                s.commit(done.getIntentSender());
            }
        } catch (Exception e) {
            if (onFail != null) onFail.run();
        }
    }
}
