package com.shazwan.sagadash;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.shazwan.sagadash.HomeActivity.AMBER;
import static com.shazwan.sagadash.HomeActivity.DIM;
import static com.shazwan.sagadash.HomeActivity.HOT;
import static com.shazwan.sagadash.HomeActivity.LINE;
import static com.shazwan.sagadash.HomeActivity.PANEL;
import static com.shazwan.sagadash.HomeActivity.TEXT;

/**
 * Notifikasi singkat bergaya KlikDash, pengganti Toast dan dialog info ber-tombol OK: panel gelap bergaris tipis
 * dengan strip warna di kiri (amber = berhasil, merah = gagal, abu = info), di bawah tengah layar, hilang sendiri
 * atau saat diketuk. Tampil di atas dialog yang sedang terbuka (lihat {@link #track}); yang baru menggantikan yang lama.
 * Boleh dipanggil dari thread mana pun.
 */
final class Notice {
    static final int INFO = 0, OK = 1, FAIL = 2, REMIND = 3;

    private Notice() { }

    private static WeakReference<PopupWindow> current = new WeakReference<>(null);
    private static final List<WeakReference<Dialog>> dialogs = new ArrayList<>();
    private static final List<Expiry> expiries = new ArrayList<>();
    private static boolean expiryHooksInstalled;
    private static Typeface fTag, fMsg;

    static void info(Activity a, String msg) { show(a, INFO, null, msg); }

    static void ok(Activity a, String msg) { show(a, OK, null, msg); }

    static void fail(Activity a, String msg) { show(a, FAIL, null, msg); }

    /** {@code title}: konteks singkat di samping jenisnya, mis. nama profil AI ("BERHASIL · CLAUDE"). */
    static void show(Activity a, int kind, String title, String msg) {
        a.runOnUiThread(() -> showNow(a, kind, title, msg));
    }

    /** Dialog yang sedang terbuka; notifikasi muncul di atasnya, bukan tertutup di belakangnya. */
    static void track(Dialog d) {
        dialogs.add(new WeakReference<>(d));
    }

    private static void showNow(Activity a, int kind, String title, String msg) {
        if (a.isFinishing() || a.isDestroyed()) return;
        View parent = topWindow(a);
        if (parent.getWindowToken() == null) {
            parent.post(() -> showNow(a, kind, title, msg)); // jendela belum terpasang (onCreate)
            return;
        }
        float dp = a.getResources().getDisplayMetrics().density;
        LinearLayout box = build(a, kind, title, msg);

        PopupWindow old = current.get();
        if (old != null) dismiss(old);
        PopupWindow p = new PopupWindow(box, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        p.setBackgroundDrawable(new ColorDrawable(0)); // tanpa ini Android 7 tidak meneruskan ketukan ke isi
        p.setTouchable(true);
        p.setAnimationStyle(android.R.style.Animation_Toast);
        p.setElevation(16 * dp); // bayangan, supaya terpisah dari isi layar yang sama gelapnya
        box.setOnClickListener(v -> dismiss(p));
        // Selalu di bawah tengah layar aktivitas. Posisi popup dihitung dari jendela induknya, yang bisa berupa dialog
        // kecil di tengah, jadi koordinatnya digeser dari dialog ke layar dan boleh keluar dari batas dialog.
        View screen = a.getWindow().getDecorView();
        int[] s = new int[2], t = new int[2];
        screen.getLocationOnScreen(s);
        parent.getLocationOnScreen(t);
        box.measure(View.MeasureSpec.makeMeasureSpec(screen.getWidth(), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(screen.getHeight(), View.MeasureSpec.AT_MOST));
        int x = s[0] - t[0] + (screen.getWidth() - box.getMeasuredWidth()) / 2;
        int y = s[1] - t[1] + screen.getHeight() - box.getMeasuredHeight() - Math.round(32 * dp);
        p.setClippingEnabled(false);
        try {
            p.showAtLocation(parent, Gravity.TOP | Gravity.START, x, y);
        } catch (RuntimeException e) {
            return; // jendela induk baru saja ditutup
        }
        current = new WeakReference<>(p);
        hideAfter(box, duration(kind, msg), () -> dismiss(p));
        box.announceForAccessibility(msg);
    }

    /** Isi notifikasi: panel bergaris tipis, strip warna jenisnya, label, lalu pesan. */
    private static LinearLayout build(Context c, int kind, String title, String msg) {
        float dp = c.getResources().getDisplayMetrics().density;
        if (fTag == null) {
            fTag = Compat.font(c.getAssets(), "mono.ttf", "'wdth' 100, 'wght' 700", Typeface.MONOSPACE);
            fMsg = Compat.font(c.getAssets(), "display.ttf", "'opsz' 36, 'wght' 700", Typeface.DEFAULT); // 700: ada instans statisnya untuk Android 7–8 (assets/static)
        }
        int color = kind == OK || kind == REMIND ? AMBER : kind == FAIL ? HOT : DIM;

        LinearLayout box = new LinearLayout(c);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PANEL);
        bg.setStroke(Math.round(dp), LINE);
        box.setBackground(bg);
        box.setPadding(Math.round(dp), Math.round(dp), Math.round(dp), Math.round(dp));

        View strip = new View(c);
        strip.setBackgroundColor(color);
        box.addView(strip, new LinearLayout.LayoutParams(Math.round(4 * dp), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        int ph = Math.round(22 * dp), pv = Math.round(16 * dp);
        col.setPadding(ph, pv, Math.round(24 * dp), pv);
        TextView tag = new TextView(c);
        String kindText = c.getString(kind == OK ? R.string.notice_ok : kind == FAIL ? R.string.notice_fail
                : kind == REMIND ? R.string.notice_remind : R.string.notice_info);
        tag.setText(title != null && !title.isEmpty() ? kindText + "  ·  " + title.toUpperCase(Lang.locale()) : kindText);
        tag.setTypeface(fTag);
        tag.setTextSize(12);
        tag.setLetterSpacing(0.15f);
        tag.setTextColor(color);
        tag.setSingleLine(true);
        col.addView(tag);
        TextView body = new TextView(c);
        body.setText(msg);
        body.setTypeface(fMsg);
        body.setTextSize(22);
        body.setTextColor(TEXT);
        body.setLineSpacing(0, 1.15f);
        body.setMaxLines(5);
        body.setEllipsize(android.text.TextUtils.TruncateAt.END);
        body.setMaxWidth(Math.round(560 * dp));
        body.setPadding(0, Math.round(6 * dp), 0, 0);
        col.addView(body);
        box.addView(col, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        box.setContentDescription(tag.getText() + ": " + msg);
        return box;
    }

    /** Cukup lama untuk dibaca sekilas sambil menyetir: 3 detik + 50 ms per huruf, paling lama 9 detik; pengingat 15 detik. */
    private static long duration(int kind, String msg) {
        return kind == REMIND ? 15_000 : Math.max(3000, Math.min(9000, 2500 + 50L * msg.length()));
    }

    /**
     * Di atas aplikasi lain (izin "tampil di atas aplikasi lain"), mis. pengingat sholat saat Maps di depan. false bila
     * izinnya belum ada; pemanggil memakai cara lain.
     */
    @SuppressWarnings("deprecation")
    static boolean overlay(Context c, int kind, String title, String msg) {
        if (!android.provider.Settings.canDrawOverlays(c)) return false;
        Context app = c.getApplicationContext();
        WindowManager wm = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
        LinearLayout box = build(Lang.wrap(app), kind, title, msg); // context aplikasi tidak ikut bahasa KlikDash
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Compat.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.y = Math.round(32 * app.getResources().getDisplayMetrics().density);
        lp.windowAnimations = android.R.style.Animation_Toast;
        Runnable remove = () -> {
            try {
                if (box.isAttachedToWindow()) wm.removeView(box);
            } catch (RuntimeException ignored) { }
        };
        box.setOnClickListener(v -> remove.run());
        try {
            wm.addView(box, lp);
        } catch (RuntimeException e) {
            return false;
        }
        hideAfter(box, duration(kind, msg), remove);
        return true;
    }

    /** Milliseconds remaining on an elapsedRealtime deadline; deep sleep counts toward expiry. */
    static long remaining(long deadlineElapsed, long nowElapsed) {
        return Math.max(0, deadlineElapsed - nowElapsed);
    }

    /**
     * View.postDelayed uses uptime, which pauses in deep sleep. Recheck an elapsedRealtime deadline
     * at most one awake second apart, so a notice cannot regain its old lifetime after wake-up.
     */
    private static void hideAfter(View view, long ms, Runnable hide) {
        installExpiryHooks(view.getContext().getApplicationContext());
        Expiry expiry = new Expiry(view, SystemClock.elapsedRealtime() + ms, hide);
        synchronized (expiries) { expiries.add(expiry); }
        view.addOnAttachStateChangeListener(expiry);
        view.postDelayed(expiry, Math.min(ms, 1000));
    }

    /** Resume/wake hooks only expire old notices; they never restart a still-valid notice's timer. */
    static void dismissExpired() {
        List<Expiry> snapshot;
        synchronized (expiries) { snapshot = new ArrayList<>(expiries); }
        long now = SystemClock.elapsedRealtime();
        for (Expiry expiry : snapshot) if (remaining(expiry.deadline, now) == 0) expiry.expire();
    }

    private static void installExpiryHooks(Context app) {
        if (app == null || expiryHooksInstalled) return;
        expiryHooksInstalled = true;
        if (app instanceof Application) {
            ((Application) app).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override public void onActivityResumed(Activity a) { dismissExpired(); }
                @Override public void onActivityCreated(Activity a, Bundle state) { }
                @Override public void onActivityStarted(Activity a) { }
                @Override public void onActivityPaused(Activity a) { }
                @Override public void onActivityStopped(Activity a) { }
                @Override public void onActivitySaveInstanceState(Activity a, Bundle state) { }
                @Override public void onActivityDestroyed(Activity a) { }
            });
        }
        try {
            app.registerReceiver(new BroadcastReceiver() {
                @Override public void onReceive(Context c, Intent intent) { dismissExpired(); }
            }, new IntentFilter(Intent.ACTION_SCREEN_ON));
        } catch (RuntimeException ignored) { } // The elapsed-time polling remains the fallback.
    }

    /** A detached/dismissed window stops polling and releases its pending removal action. */
    private static final class Expiry implements Runnable, View.OnAttachStateChangeListener {
        final WeakReference<View> view;
        final long deadline;
        final Runnable hide;
        boolean finished;

        Expiry(View view, long deadline, Runnable hide) {
            this.view = new WeakReference<>(view);
            this.deadline = deadline;
            this.hide = hide;
        }

        @Override public void run() {
            if (finished) return;
            View v = view.get();
            if (v == null || !v.isAttachedToWindow()) { finish(); return; }
            long left = remaining(deadline, SystemClock.elapsedRealtime());
            if (left == 0) expire();
            else v.postDelayed(this, Math.min(left, 1000));
        }

        void expire() { if (finish()) hide.run(); }

        boolean finish() {
            if (finished) return false;
            finished = true;
            View v = view.get();
            if (v != null) {
                v.removeCallbacks(this);
                v.removeOnAttachStateChangeListener(this);
            }
            synchronized (expiries) { expiries.remove(this); }
            return true;
        }

        @Override public void onViewAttachedToWindow(View v) { }
        @Override public void onViewDetachedFromWindow(View v) { finish(); }
    }

    /** Jendela teratas milik {@code a}: dialog terbuka yang terakhir di-{@link #track}, atau jendela aktivitasnya. */
    private static View topWindow(Activity a) {
        for (int i = dialogs.size() - 1; i >= 0; i--) {
            Dialog d = dialogs.get(i).get();
            if (d == null || !d.isShowing()) {
                dialogs.remove(i); // dialog KlikDash tidak dibuka ulang setelah ditutup
                continue;
            }
            if (owner(d.getContext()) == a && d.getWindow() != null) return d.getWindow().getDecorView();
        }
        return a.getWindow().getDecorView();
    }

    private static Activity owner(Context c) {
        while (c instanceof ContextWrapper && !(c instanceof Activity)) c = ((ContextWrapper) c).getBaseContext();
        return c instanceof Activity ? (Activity) c : null;
    }

    private static void dismiss(PopupWindow p) {
        if (!p.isShowing()) return;
        try {
            p.dismiss();
        } catch (RuntimeException ignored) { } // jendela induk sudah hilang lebih dulu
    }
}
