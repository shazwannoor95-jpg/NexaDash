package com.shazwan.sagadash;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.io.*;
import java.text.*;
import java.util.*;

/** Official e-Solat times only. Bundled and refreshed per-zone caches work without Internet. */
final class JakimSchedule {
    static final TimeZone MY = TimeZone.getTimeZone("Asia/Kuala_Lumpur");
    static final String[] KEYS={"fajr","syuruk","dhuhr","asr","maghrib","isha"};
    static final String[] NAMES={"Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"};
    static final String[] MS_MONTHS={"Jan","Feb","Mac","Apr","Mei","Jun","Jul","Ogos","Sep","Okt","Nov","Dis"};
    static final String[] EN_MONTHS={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
    static SharedPreferences prefs(Context c){return c.createDeviceProtectedStorageContext().getSharedPreferences("jakim",0);}
    static String zone(Context c){return prefs(c).getString("zone", "");}
    static boolean automatic(Context c){return prefs(c).getBoolean("auto",true);}
    static boolean validZone(String zone){return zone.matches("(JHR|KDH|KTN|MLK|NGS|PHG|PLS|PNG|PRK|SBH|SGR|SWK|TRG|WLY)\\d{2}");}
    static void select(Context c,String zone){
        if(!validZone(zone))throw new IllegalArgumentException("Enter a JAKIM zone code, e.g. SGR01");
        prefs(c).edit().putString("zone",zone).putBoolean("auto",false).apply();
    }
    static void selectAutomatic(Context c,String zone,String district){
        if(!validZone(zone)||!automatic(c))return;
        prefs(c).edit().putString("zone",zone).putString("district",district).apply();
    }
    static void useGps(Context c){prefs(c).edit().putBoolean("auto",true).apply();}
    static String dateKey(long when){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setTimeZone(MY);return f.format(new Date(when));}
    static Date parseDate(String value) throws Exception {
        String[] parts=value.split("-");
        if(parts.length!=3)throw new ParseException("Invalid JAKIM date",0);
        for(int i=0;i<12;i++)if(parts[1].equalsIgnoreCase(MS_MONTHS[i])){parts[1]=EN_MONTHS[i];break;}
        String normalized=parts[0]+"-"+parts[1]+"-"+parts[2];
        SimpleDateFormat f=new SimpleDateFormat("dd-MMM-yyyy",Locale.US);f.setTimeZone(MY);f.setLenient(false);
        Date parsed=f.parse(normalized);
        if(!f.format(parsed).equalsIgnoreCase(normalized))throw new ParseException("Invalid JAKIM date",0);
        return parsed;
    }
    static class Day {
        final long[] times=new long[6];
        final String date;
        Day(JSONObject obj) throws Exception {
            Date parsed=parseDate(obj.getString("date"));date=dateKey(parsed.getTime());
            for(int i=0;i<6;i++){
                String raw=obj.getString(KEYS[i]);
                if(!raw.matches("\\d{2}:\\d{2}(:\\d{2})?"))throw new IllegalArgumentException("Invalid time");
                String[] parts=raw.split(":");int h=Integer.parseInt(parts[0]),m=Integer.parseInt(parts[1]),sec=parts.length==3?Integer.parseInt(parts[2]):0;
                if(h>23||m>59||sec>59)throw new IllegalArgumentException("Invalid time");
                Calendar t=Calendar.getInstance(MY);t.setTime(parsed);t.set(Calendar.HOUR_OF_DAY,h);t.set(Calendar.MINUTE,m);t.set(Calendar.SECOND,sec);t.set(Calendar.MILLISECOND,0);
                times[i]=t.getTimeInMillis();if(i>0&&times[i]<=times[i-1])throw new IllegalArgumentException("Invalid timetable order");
            }
        }
    }
    static JSONArray validate(String text) throws Exception {
        JSONArray days=new JSONObject(text).getJSONArray("prayerTime");
        if(days.length()==0)throw new IllegalArgumentException("Empty JAKIM timetable");
        Set<String> seen=new HashSet<>();
        for(int i=0;i<days.length();i++)if(!seen.add(new Day(days.getJSONObject(i)).date))throw new IllegalArgumentException("Duplicate JAKIM date");
        return days;
    }
    static final class Cache {
        String raw="";
        final TreeMap<String,Day> days=new TreeMap<>();
    }
    private static final Map<String,Cache> caches=new java.util.concurrent.ConcurrentHashMap<>();
    static synchronized Cache cache(Context c,String zone){
        Cache cache=caches.get(zone);
        String raw=prefs(c).getString("data_"+zone,"");
        if(cache!=null&&cache.raw.equals(raw))return cache;
        cache=new Cache();cache.raw=raw;
        try{load(cache,readAsset(c,"jakim/"+zone+".json"));}catch(Exception ignored){}
        if(!raw.isEmpty())try{load(cache,raw);}catch(Exception ignored){}
        caches.put(zone,cache);return cache;
    }
    static boolean ready(Context c){return zone(c).isEmpty()||caches.containsKey(zone(c));}
    static Day readyAt(Context c,long when){Cache data=caches.get(zone(c));return data==null?null:data.days.get(dateKey(when));}
    static long readyNext(Context c,long now){
        if(readyAt(c,now)==null)return 0;
        for(int d=0;d<2;d++){Day day=readyAt(c,now+d*86400000L);if(day!=null)for(int i=0;i<6;i++)if(i!=1&&day.times[i]>now)return day.times[i];}
        return 0;
    }
    static String readyCoverage(String zone){Cache cache=caches.get(zone);return cache==null?"Loading…":cache.days.isEmpty()?"Not downloaded":cache.days.firstKey()+" to "+cache.days.lastKey();}
    static String readAsset(Context c,String path) throws Exception {
        try(InputStream in=c.getAssets().open(path);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");
        }
    }
    static void load(Cache cache,String text) throws Exception {
        JSONArray days=validate(text);for(int i=0;i<days.length();i++){Day day=new Day(days.getJSONObject(i));cache.days.put(day.date,day);}
    }
    static Day at(Context c,long when){return at(c,zone(c),when);}
    static Day at(Context c,String zone,long when){return zone.isEmpty()?null:cache(c,zone).days.get(dateKey(when));}
    static String coverage(Context c,String zone){Cache cache=cache(c,zone);return cache.days.isEmpty()?"Not downloaded":cache.days.firstKey()+" to "+cache.days.lastKey();}
    static boolean refreshDue(Context c,String zone,long now){
        long fetched=prefs(c).getLong("fetched_"+zone,0);
        return at(c,zone,now)==null||at(c,zone,now+86400000L)==null||fetched<=0||fetched>now||now-fetched>7*86400000L;
    }
    static void refresh(Context c) throws Exception {String z=zone(c);if(!z.isEmpty())refresh(c,z);}
    static void refresh(Context c,String z) throws Exception {
        if(!validZone(z))throw new IllegalArgumentException("Invalid zone");
        String text=DashNetwork.get("https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=year&zone="+z);
        JSONObject root=new JSONObject(text);
        if(!z.equals(root.optString("zone"))||!root.optString("status").startsWith("OK"))throw new IOException("JAKIM zone/status mismatch");
        JSONArray days=validate(text);
        // Merge by exact Malaysia date: a new year must not remove still-needed cached dates.
        TreeMap<String,JSONObject> merged=new TreeMap<>();
        try{JSONArray old=validate(prefs(c).getString("data_"+z,"{}"));for(int i=0;i<old.length();i++)merged.put(new Day(old.getJSONObject(i)).date,old.getJSONObject(i));}catch(Exception ignored){}
        for(int i=0;i<days.length();i++)merged.put(new Day(days.getJSONObject(i)).date,days.getJSONObject(i));
        JSONArray all=new JSONArray();for(JSONObject day:merged.values())all.put(day);
        root.put("prayerTime",all);
        TrustedClock.Reading fetchedAt=TrustedClock.read(c);
        if(!prefs(c).edit().putString("data_"+z,root.toString()).putLong("fetched_"+z,fetchedAt.verified?fetchedAt.utc:0).commit())throw new IOException("Could not save timetable");
        cache(c,z);
    }
    static String time(long at){SimpleDateFormat f=new SimpleDateFormat("HH:mm",Locale.US);f.setTimeZone(MY);return f.format(new Date(at));}
    static long next(Context c,long now){
        // Never show a countdown as today's schedule when today's cache is absent/expired.
        if(at(c,now)==null)return 0;
        for(int d=0;d<2;d++){Day day=at(c,now+d*86400000L);if(day!=null)for(int i=0;i<6;i++)if(i!=1&&day.times[i]>now)return day.times[i];}
        return 0;
    }
    static String summary(Context c){
        TrustedClock.Reading time=TrustedClock.read(c);
        if(!time.verified)return "Prayer time is waiting for GPS time verification or your clock confirmation";
        Day d=at(c,time.utc);if(d==null)return "JAKIM · Waiting for GPS/zone or today's timetable";
        StringBuilder s=new StringBuilder(zone(c));for(int i=0;i<6;i++)s.append(" · ").append(NAMES[i]).append(' ').append(time(d.times[i]));return s.toString();
    }
}
