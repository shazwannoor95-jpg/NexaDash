package com.shazwan.sagadash;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.provider.Settings;

/**
 * Adapts KlikDash v1.7.2–1.7.3's verified-clock behavior to an app-local GPS clock.
 * Satellite UTC is anchored to elapsedRealtime, including sleep. No SET_TIME/ADB is needed.
 * Unknown time can be displayed as such but cannot drive prayer countdowns or notifications.
 */
final class TrustedClock {
    static final int UNKNOWN=0, GPS=1, CONFIRMED_DEVICE=2;
    // Earliest supported bundled schedule year, also rejects historical GPS-week rollovers.
    static final long MIN_UTC=1767225600000L; // 2026-01-01 UTC
    static final long MAX_UTC=4102444800000L; // 2100-01-01 UTC, exclusive
    static final long MAX_AGE=24*60*60_000L, MAX_FIX_AGE=10_000L, SKEW=120_000L;
    static final class Reading {
        final long utc;
        final int source;
        final boolean verified;
        Reading(long utc,int source,boolean verified){this.utc=utc;this.source=source;this.verified=verified;}
    }
    /** Pure time model; Android I/O is confined to the wrapper below. */
    static final class Engine {
        private long utc, elapsed, lastFix=-1, pendingUtc, pendingElapsed;
        private int source=UNKNOWN;
        static boolean plausible(long value){return value>=MIN_UTC&&value<MAX_UTC;}
        synchronized Reading read(long wall,long now){
            boolean good=source!=UNKNOWN&&plausible(utc)&&now>=elapsed&&now-elapsed<=MAX_AGE&&plausible(utc+(now-elapsed));
            return new Reading(good?utc+(now-elapsed):wall,good?source:UNKNOWN,good);
        }
        synchronized boolean restore(long utc,long elapsed,int source,int savedBoot,int currentBoot,long now){
            if(currentBoot<0||savedBoot!=currentBoot||(source!=GPS&&source!=CONFIRMED_DEVICE)||!plausible(utc)||elapsed<0||now<elapsed||now-elapsed>MAX_AGE||!plausible(utc+(now-elapsed)))return false;
            this.utc=utc;this.elapsed=elapsed;this.source=source;return true;
        }
        synchronized boolean gps(String provider,long fixUtc,long fixElapsed,long now,boolean mock){
            if(mock||!LocationManager.GPS_PROVIDER.equals(provider)||fixElapsed<0||fixElapsed<=lastFix||now<fixElapsed||now-fixElapsed>MAX_FIX_AGE)return false;
            long candidate=fixUtc+(now-fixElapsed);
            if(!plausible(candidate))return false;
            lastFix=fixElapsed;
            Reading old=read(candidate,now);
            if(old.verified&&old.source==GPS&&Math.abs(candidate-old.utc)>SKEW){
                long age=now-pendingElapsed;
                if(pendingUtc==0||age<0||age>MAX_FIX_AGE||Math.abs(candidate-(pendingUtc+age))>2000){pendingUtc=candidate;pendingElapsed=now;return false;}
                if(age<1000)return false;
            }
            boolean changed=!old.verified||old.source!=GPS||Math.abs(candidate-old.utc)>1000;
            utc=candidate;elapsed=now;source=GPS;pendingUtc=0;return changed;
        }
        synchronized boolean confirmDevice(long wall,long now){
            if(!plausible(wall)||now<0)return false;
            utc=wall;elapsed=now;source=CONFIRMED_DEVICE;pendingUtc=0;return true;
        }
        synchronized void wallChanged(){if(source==CONFIRMED_DEVICE)clear();}
        synchronized void clear(){utc=0;elapsed=0;source=UNKNOWN;lastFix=-1;pendingUtc=0;pendingElapsed=0;}
        synchronized long anchorUtc(){return utc;}
        synchronized long anchorElapsed(){return elapsed;}
        synchronized int source(){return source;}
    }
    private static final Engine engine=new Engine();
    private static boolean initialized;
    private static int boot=-1;
    private static long persistedAt=-60_000;
    private TrustedClock(){}
    private static SharedPreferences prefs(Context c){return c.createDeviceProtectedStorageContext().getSharedPreferences("trusted_clock",0);}
    static synchronized void initialize(Context c){
        if(initialized)return;
        boot=Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,-1);
        SharedPreferences p=prefs(c);
        engine.restore(p.getLong("utc",0),p.getLong("elapsed",-1),p.getInt("source",UNKNOWN),p.getInt("boot",-1),boot,SystemClock.elapsedRealtime());
        initialized=true;
    }
    static boolean manualClockChanged(Reading reading,long wall){return reading.verified&&reading.source==CONFIRMED_DEVICE&&Math.abs(reading.utc-wall)>5000;}
    static Reading read(Context c){
        initialize(c);long wall=System.currentTimeMillis(),elapsed=SystemClock.elapsedRealtime();Reading reading=engine.read(wall,elapsed);
        if(manualClockChanged(reading,wall)){engine.wallChanged();persist(c,elapsed);return engine.read(wall,elapsed);}
        return reading;
    }
    static boolean verified(Context c){return read(c).verified;}
    static long now(Context c){return read(c).utc;}
    static boolean gps(Context c,Location location){
        initialize(c);
        boolean changed=engine.gps(location.getProvider(),location.getTime(),location.getElapsedRealtimeNanos()/1_000_000,SystemClock.elapsedRealtime(),location.isFromMockProvider());
        long elapsed=SystemClock.elapsedRealtime();
        if(engine.source()==GPS&&(changed||elapsed-persistedAt>=60_000))persist(c,elapsed);
        return changed;
    }
    static boolean confirmDevice(Context c){
        initialize(c);
        if(!engine.confirmDevice(System.currentTimeMillis(),SystemClock.elapsedRealtime()))return false;
        persist(c,SystemClock.elapsedRealtime());return true;
    }
    static void wallChanged(Context c){initialize(c);engine.wallChanged();persist(c,SystemClock.elapsedRealtime());}
    static void forget(Context c){initialize(c);engine.clear();persist(c,SystemClock.elapsedRealtime());}
    private static synchronized void persist(Context c,long elapsed){
        prefs(c).edit().putLong("utc",engine.anchorUtc()).putLong("elapsed",engine.anchorElapsed()).putInt("source",engine.source()).putInt("boot",boot).apply();persistedAt=elapsed;
    }
    static String label(Context c){
        Reading r=read(c);
        if(!r.verified)return "Time unverified · Waiting for GPS";
        if(r.source==CONFIRMED_DEVICE)return "Device time · Confirmed by you";
        return Math.abs(r.utc-System.currentTimeMillis())>SKEW?"GPS time · Device clock differs":"GPS time verified";
    }
}
