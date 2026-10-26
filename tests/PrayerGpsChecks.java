package com.shazwan.sagadash;

import android.content.*;
import android.location.Location;
import org.json.*;
import java.util.*;

/** GPS/offline tests use isolated preferences and actual bundled official data. No head unit required. */
final class PrayerGpsChecks {
    int assertions;
    void check(boolean ok,String message){if(!ok)throw new AssertionError(message);assertions++;}
    static long date(String value) throws Exception {return JakimSchedule.parseDate(value).getTime();}
    static Location fix(double lat,double lon,float accuracy,long nanos){Location l=new Location("gps");l.setLatitude(lat);l.setLongitude(lon);l.setAccuracy(accuracy);l.setElapsedRealtimeNanos(nanos);l.setTime(nanos/1000000);return l;}
    int run(Context target) throws Exception {
        Context c=new ContextWrapper(target){
            @Override public Context createDeviceProtectedStorageContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){return getBaseContext().createDeviceProtectedStorageContext().getSharedPreferences("checks_"+name,mode);}
        };
        JakimSchedule.prefs(c).edit().clear().commit();
        try{
            for(int i=0;i<12;i++){
                String ms="01-"+JakimSchedule.MS_MONTHS[i]+"-2026";
                check(JakimSchedule.dateKey(date(ms)).equals(String.format(Locale.US,"2026-%02d-01",i+1)),"Malay month "+ms);
            }
            boolean invalid=false;try{date("31-Feb-2026");}catch(Exception e){invalid=true;}check(invalid,"invalid calendar date rejected");
            for(String zone:PrayerZones.TRAVEL_ZONES){
                JSONObject official=new JSONObject(JakimSchedule.readAsset(c,"jakim/"+zone+".json"));
                check(official.getString("zone").equals(zone),"official zone matches asset "+zone);
                check(JakimSchedule.validate(official.toString()).length()==365,"full 2026 timetable "+zone);
                check(JakimSchedule.at(c,zone,date("01-Jan-2026"))!=null,"January cached offline "+zone);
                check(JakimSchedule.at(c,zone,date("31-Dis-2026"))!=null,"December cached offline "+zone);
                check(JakimSchedule.at(c,zone,date("01-Jan-2027"))==null,"expired cache not reused "+zone);
            }
            long reference=date("25-Okt-2026")+12*60*60_000L;
            JakimSchedule.prefs(c).edit().putLong("fetched_SGR01",reference+365*86400000L).commit();
            check(JakimSchedule.refreshDue(c,"SGR01",reference),"future wrong-RTC freshness timestamp cannot suppress GPS-time refresh");
            JakimSchedule.prefs(c).edit().putLong("fetched_SGR01",reference).commit();
            check(!JakimSchedule.refreshDue(c,"SGR01",reference),"recent verified-time download remains fresh");
            JakimSchedule.prefs(c).edit().putLong("fetched_SGR01",0).commit();
            check(JakimSchedule.refreshDue(c,"SGR01",reference),"download without verified time is not treated as verified freshness");
            JakimSchedule.prefs(c).edit().putLong("fetched_SGR01",reference-8*86400000L).commit();
            check(JakimSchedule.refreshDue(c,"SGR01",reference),"week-old download is refreshed on trusted UTC timeline");
            PrayerZones map=PrayerZones.load(c);
            double[][] locations={{3.3408,101.2498},{3.0738,101.5183},{3.1390,101.6869},{3.0449,101.4456},{2.9264,101.6964},{3.1073,101.6067}};
            String[] zones={"SGR02","SGR01","WLY01","SGR03","WLY01","SGR01"};
            String[] names={"Kuala Selangor","Shah Alam","Kuala Lumpur","Klang","Putrajaya","Petaling Jaya"};
            for(int i=0;i<zones.length;i++){PrayerZones.Match m=map.locate(locations[i][0],locations[i][1],5);check(m!=null&&!m.boundary&&m.zone.equals(zones[i]),"offline GPS polygon: "+names[i]);}
            check(map.locate(1.4927,103.7414,5)==null,"outside map not assigned nearest city");
            check(map.locate(Double.NaN,101,5)==null,"invalid coordinates rejected");
            PrayerLocation tracker=new PrayerLocation(c);long now=1_000_000_000_000L;
            check(JakimSchedule.automatic(c),"auto GPS enabled by default");
            check(!tracker.update(fix(3.3408,101.2498,5,now),now)&&JakimSchedule.zone(c).isEmpty(),"first fix cannot switch zone");
            now+=2_000_000_000L;
            check(tracker.update(fix(3.3408,101.2498,5,now),now)&&JakimSchedule.zone(c).equals("SGR02"),"second stable fix selects Kuala Selangor");
            now+=2_000_000_000L;
            tracker.update(fix(3.0738,101.5183,5,now),now);
            now+=2_000_000_000L;
            check(tracker.update(fix(3.0738,101.5183,5,now),now)&&JakimSchedule.zone(c).equals("SGR01"),"travel to Shah Alam changes zone offline");
            now+=2_000_000_000L;tracker.update(fix(3.139,101.6869,5,now),now);
            now+=2_000_000_000L;
            check(tracker.update(fix(3.139,101.6869,5,now),now)&&JakimSchedule.zone(c).equals("WLY01"),"travel to KL changes zone offline");
            long fresh=now+2_000_000_000L;
            check(!tracker.update(fix(3.3408,101.2498,500,fresh),fresh)&&JakimSchedule.zone(c).equals("WLY01"),"poor accuracy keeps last zone");
            check(!tracker.update(fix(3.3408,101.2498,5,now),now+60_000_000_000L),"stale GPS ignored");
            check(!tracker.update(fix(3.139,101.6869,5,now),now),"duplicate fix ignored");
            now=fresh+2_000_000_000L;
            tracker.update(fix(1.4927,103.7414,5,now),now);
            check(JakimSchedule.zone(c).equals("WLY01")&&tracker.status.contains("Outside"),"outside coverage keeps last zone and warns");
            check(tracker.label(now/1000000+16000).contains("Last confirmed"),"lost GPS labelled as last zone");
            JakimSchedule.select(c,"SGR03");now+=2_000_000_000L;
            check(!tracker.update(fix(3.3408,101.2498,5,now),now)&&JakimSchedule.zone(c).equals("SGR03"),"manual override prevents GPS change");
            JakimSchedule.useGps(c);check(JakimSchedule.automatic(c),"automatic mode can be restored");
            // Isolated geometry exercises holes and exact boundary uncertainty, without guessed Malaysian lines.
            JSONObject geometry=new JSONObject("{\"features\":[{\"properties\":{\"district\":\"Test\",\"zone\":\"SGR01\"},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[100,2],[102,2],[102,4],[100,4],[100,2]],[[100.5,2.5],[101.5,2.5],[101.5,3.5],[100.5,3.5],[100.5,2.5]]]}}]}");
            PrayerZones synthetic=new PrayerZones(geometry);
            check(synthetic.locate(3,101,5)==null,"polygon hole excluded");
            check(synthetic.locate(2.0001,101,5).boundary,"near boundary held instead of flapping");
            check(!synthetic.locate(2.25,101,5).boundary,"interior location accepted");
            JakimSchedule.select(c,"SGR01");
            long when=date("25-Okt-2026");JakimSchedule.Day day=JakimSchedule.at(c,when);
            check(JakimSchedule.next(c,day.times[0]+1000)==day.times[2],"sunrise is not next prayer");
            JakimSchedule.Day tomorrow=JakimSchedule.at(c,when+86400000L);
            check(JakimSchedule.next(c,day.times[5]+1000)==tomorrow.times[0],"after Isha countdown targets tomorrow Fajr");
            check(JakimSchedule.next(c,date("01-Jan-2027"))==0,"no countdown for expired day");
            check(JakimSchedule.dateKey(when-1).equals("2026-10-24"),"Malaysia midnight selects previous date correctly");
            check(JakimSchedule.at(c,when).times[0]==day.times[0],"cached timetable stable without network");
            return assertions;
        } finally {JakimSchedule.prefs(c).edit().clear().commit();}
    }
}
