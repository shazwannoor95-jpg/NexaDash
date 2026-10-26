package com.shazwan.sagadash;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Pengingat masuk waktu sholat, hanya notifikasi (tanpa adzan). AlarmManager.RTC (tidak membangunkan headunit yang
 * tidur): alarm yang baru terkirim saat headunit bangun dan sudah lewat lebih dari {@link #LATE_MS} diabaikan. Tiap
 * alarm menjadwalkan waktu berikutnya; home memanggil {@link #schedule} saat dibuka, saat lokasi atau jam berubah.
 * Home tampil: notifikasi KlikDash ({@link Notice}); aplikasi lain di depan: Notice melayang (izin tampil di atas
 * aplikasi lain, diberikan lewat ADB lokal oleh {@link #allowOverlay}); tanpa izin itu: notifikasi sistem.
 */
public class PrayerAlarm extends BroadcastReceiver {
    static final String CHANNEL = "sholat";
    static final long LATE_MS = 10 * 60_000L;

    /** Home yang sedang tampil (di antara onResume dan onPause), untuk notifikasi KlikDash. */
    static WeakReference<Activity> home = new WeakReference<>(null);

    static boolean enabled(SharedPreferences p) { return p.getBoolean("prayerAlert", true); }

    private static final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor(r->new Thread(r,"Saga-prayer-scheduler"));
    private static final int[] JAKIM_INDICES={0,2,3,4,5};
    static boolean shouldDeliver(boolean verified,long at,long now){return verified&&at>0&&now>=at&&now-at<=LATE_MS;}
    static long elapsedDeadline(long elapsed,long utc,long target){
        if(elapsed<0||target<=utc)return -1;
        long delay=target-utc;return delay>Long.MAX_VALUE-elapsed?-1:elapsed+delay;
    }
    private static final class Event {
        final String zone,id;final int index;final long at;
        Event(String zone,int index,long at){this.zone=zone;this.index=index;this.at=at;id=JakimSchedule.dateKey(at)+"|"+index;}
    }
    /** Called on the prayer worker. Every alarm uses elapsed time, even if the device RTC is wrong. */
    static void schedule(Context c){Context app=c.getApplicationContext();worker.execute(()->scheduleNow(app));}
    private static void scheduleNow(Context c){
        AlarmManager manager=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent old=PendingIntent.getBroadcast(c,0,new Intent(c,PrayerAlarm.class),PendingIntent.FLAG_NO_CREATE);
        if(old!=null)manager.cancel(old);
        TrustedClock.Reading reading=TrustedClock.read(c);String zone=JakimSchedule.zone(c);
        if(!enabled(prefs(c))||!reading.verified||zone.isEmpty())return;
        java.util.List<Event> events=new java.util.ArrayList<>();
        for(int offset=-1;offset<=1;offset++){
            JakimSchedule.Day day=JakimSchedule.at(c,zone,reading.utc+offset*86_400_000L);
            if(day!=null)for(int i=0;i<5;i++)events.add(new Event(zone,i,day.times[JAKIM_INDICES[i]]));
        }
        // Parsing a cold cache can take time; do not turn that I/O duration into alarm drift.
        reading=TrustedClock.read(c);if(!reading.verified)return;
        if(!zone.equals(JakimSchedule.zone(c))){schedule(c);return;}
        Event recent=null,future=null;
        for(Event event:events){
            if(shouldDeliver(true,event.at,reading.utc)&&(recent==null||event.at>recent.at))recent=event;
            if(event.at>reading.utc&&(future==null||event.at<future.at))future=event;
        }
        if(recent!=null)deliver(c,recent);
        if(future!=null){
            reading=TrustedClock.read(c);if(!reading.verified)return;
            if(!zone.equals(JakimSchedule.zone(c))){schedule(c);return;}
            long deadline=elapsedDeadline(android.os.SystemClock.elapsedRealtime(),reading.utc,future.at);
            if(deadline<0)return;
            Intent intent=new Intent(c,PrayerAlarm.class).putExtra("i",future.index).putExtra("at",future.at).putExtra("zone",zone);
            manager.setExact(AlarmManager.ELAPSED_REALTIME,deadline,PendingIntent.getBroadcast(c,0,intent,PendingIntent.FLAG_UPDATE_CURRENT));
        }
    }
    static boolean alreadyDelivered(int ledger,int index){return index<0||index>=5||(ledger&(1<<index))!=0;}
    static int markDelivered(int ledger,int index){return index<0||index>=5?ledger:ledger|(1<<index);}
    private static void deliver(Context c,Event event){
        new android.os.Handler(android.os.Looper.getMainLooper()).post(()->deliverOnMain(c,event));
    }
    private static synchronized void deliverOnMain(Context c,Event event){
        TrustedClock.Reading reading=TrustedClock.read(c);
        if(!enabled(prefs(c))||!event.zone.equals(JakimSchedule.zone(c))||!shouldDeliver(reading.verified,event.at,reading.utc))return;
        String legacyKey="lastPrayerAlert_"+event.zone;
        String ledgerKey="prayerAlertLedger_"+event.zone+"_"+JakimSchedule.dateKey(event.at);
        int ledger=prefs(c).getInt(ledgerKey,0);
        if(alreadyDelivered(ledger,event.index)||event.id.equals(prefs(c).getString(legacyKey,"")))return;
        prefs(c).edit().putInt(ledgerKey,markDelivered(ledger,event.index)).putString(legacyKey,event.id).apply();
        notify(c,event.index,event.at);
    }
    @Override public void onReceive(Context context,Intent intent){
        final PendingResult pending=goAsync();
        worker.execute(()->{
            try{
                String zone=intent.getStringExtra("zone");int index=intent.getIntExtra("i",-1);long at=intent.getLongExtra("at",0);
                if(zone!=null&&zone.equals(JakimSchedule.zone(context))&&index>=0&&index<5){
                    JakimSchedule.Day day=JakimSchedule.at(context,zone,at);
                    if(day!=null&&day.times[JAKIM_INDICES[index]]==at)deliver(context,new Event(zone,index,at));
                }
                scheduleNow(context);
            }finally{pending.finish();}
        });
    }

    /** Tampilkan pengingat untuk waktu sholat {@code i} (indeks {@link Lang#prayers}) pada {@code at}. */
    static void notify(Context c, int i, long at) {
        Context w = Lang.wrap(c); // receiver: context tanpa bahasa KlikDash; juga memuat Lang.prayers
        String name = titleCase(Lang.prayers[i]);
        Calendar t = Calendar.getInstance(JakimSchedule.MY);
        t.setTimeInMillis(at);
        String time = String.format(Locale.US, "%02d:%02d", t.get(Calendar.HOUR_OF_DAY), t.get(Calendar.MINUTE));
        String city = JakimSchedule.zone(c);
        String msg = w.getString(R.string.prayer_alert_message, name, time) + (city != null ? " · " + titleCase(city) : "");
        String title = w.getString(R.string.prayer_alert_title);
        Activity a = home.get();
        if (a != null && !a.isFinishing()) {
            Notice.show(a, Notice.REMIND, title, msg);
            return;
        }
        // Aplikasi lain di depan: notifikasi KlikDash melayang di atasnya. Firmware NWD memblokir notifikasi sistem
        // dari aplikasi pihak ketiga ("Not allowed show notification"), jadi notifikasi sistem hanya cadangan.
        Theme.load(c);
        if (Notice.overlay(c, Notice.REMIND, title, msg)) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent open = PendingIntent.getActivity(c, 0,
                new Intent(c, DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
        Notification.Builder b = Compat.alertNotification(c, CHANNEL, w.getString(R.string.channel_prayer))
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(w.getString(R.string.prayer_alert_notification, name))
                .setContentText(msg)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setWhen(System.currentTimeMillis()).setShowWhen(false);
        if (Compat.O) b.setTimeoutAfter(30 * 60_000L);
        nm.notify(2, b.build());
    }

    /** Izin tampil di atas aplikasi lain lewat ADB lokal (seperti Tweaks); diam-diam dilewati bila ADB tidak ada. */
    static void allowOverlay(Context c) {
        if (android.provider.Settings.canDrawOverlays(c) || !enabled(prefs(c))) return;
        Tweaks.shellAsync("cmd appops set " + c.getPackageName() + " SYSTEM_ALERT_WINDOW allow");
    }

    /** Lokasi tersimpan dari GPS, atau tebakan zona waktu seperti di home. */
    static double[] location(SharedPreferences p) {
        String la = p.getString("lat", null), lo = p.getString("lon", null);
        if (la != null && lo != null) return new double[]{Double.parseDouble(la), Double.parseDouble(lo)};
        return PrayerTimes.guess(TimeZone.getDefault().getID());
    }

    static String titleCase(String s) {
        StringBuilder b = new StringBuilder(s.length());
        boolean up = true;
        for (char ch : s.toCharArray()) {
            b.append(up ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
            up = ch == ' ' || ch == '-';
        }
        return b.toString();
    }

    private static SharedPreferences prefs(Context c) {
        return c.createDeviceProtectedStorageContext().getSharedPreferences("klikdash", Context.MODE_PRIVATE);
    }
}
