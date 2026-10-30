package com.shazwan.sagadash;

import android.content.Context;
import android.location.Location;
import android.os.SystemClock;

/** Conservative zone switching: fresh precise GPS, boundary buffer and two consistent fixes. */
final class PrayerLocation {
    final Context context;
    final PrayerZones zones;
    String pending="";
    int fixes;
    long pendingAt, lastAcceptedNanos=-1, lastFixElapsed;
    String status="Waiting for GPS";
    PrayerLocation(Context c) throws Exception {context=c;zones=PrayerZones.load(c);}
    boolean update(Location l){return update(l,SystemClock.elapsedRealtimeNanos());}
    boolean update(Location l,long nowNanos){
        if(!JakimSchedule.automatic(context)){status="Manual zone";reset();return false;}
        long age=nowNanos-l.getElapsedRealtimeNanos();
        if(!l.hasAccuracy()||l.getAccuracy()>50||age<0||age>10_000_000_000L||l.getElapsedRealtimeNanos()<=lastAcceptedNanos){return false;}
        lastAcceptedNanos=l.getElapsedRealtimeNanos();lastFixElapsed=nowNanos/1_000_000;
        PrayerZones.Match found=zones.locate(l.getLatitude(),l.getLongitude(),l.getAccuracy());
        if(found==null){status="Outside offline map coverage · Select a zone manually";reset();return false;}
        if(found.boundary){status="Near zone boundary · Keeping last zone";reset();return false;}
        if(found.zone.equals(JakimSchedule.zone(context))){status="GPS · "+found.district;reset();return false;}
        if(!found.zone.equals(pending)||nowNanos-pendingAt>30_000_000_000L){pending=found.zone;fixes=1;pendingAt=nowNanos;status="Confirming GPS zone · "+found.zone;return false;}
        if(nowNanos-pendingAt<1_000_000_000L)return false;
        if(++fixes<2)return false;
        JakimSchedule.selectAutomatic(context,found.zone,found.district);
        status="GPS · "+found.district;reset();return true;
    }
    void signalLost(){lastFixElapsed=0;reset();}
    void reset(){pending="";fixes=0;pendingAt=0;}
    String label(long elapsed){
        if(!JakimSchedule.automatic(context))return "Manual zone";
        if(lastFixElapsed==0||elapsed-lastFixElapsed>15000)return JakimSchedule.zone(context).isEmpty()?"Waiting for GPS":"GPS unavailable · Last confirmed zone";
        return status;
    }
}
