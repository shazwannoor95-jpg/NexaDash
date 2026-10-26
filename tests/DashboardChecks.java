package com.shazwan.sagadash;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.session.*;
import android.os.*;
import org.json.*;
import java.text.*;
import java.util.*;

/** Device integration checks; synthetic provider data stays inside this test, never in production cache. */
public class DashboardChecks extends Instrumentation {
    int assertions;
    void check(boolean ok,String message){if(!ok)throw new AssertionError(message);assertions++;}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();MediaSession[] session=new MediaSession[1];DashboardActivity[] dash=new DashboardActivity[1];
        try{
            assertions+=new TrustedClockChecks().run();
            final String applicationId="com.shazwan.sagadash";
            check(getTargetContext().getPackageName().equals(applicationId),"personal application ID");
            check(DashboardActivity.class.getName().equals(applicationId+".DashboardActivity")
                    &&R.class.getName().equals(applicationId+".R"),"Java and resource namespace migrated");
            check(getTargetContext().getApplicationInfo().dataDir.endsWith("/"+applicationId),"personal private data directory");
            android.content.pm.PackageInfo installed=getTargetContext().getPackageManager().getPackageInfo(applicationId,
                    android.content.pm.PackageManager.GET_ACTIVITIES|android.content.pm.PackageManager.GET_SERVICES);
            boolean componentsMigrated=true;
            for(android.content.pm.ActivityInfo component:installed.activities)componentsMigrated&=component.name.startsWith(applicationId+".");
            for(android.content.pm.ServiceInfo component:installed.services)componentsMigrated&=component.name.startsWith(applicationId+".");
            check(componentsMigrated,"all registered activity/service classes use personal namespace");
            SagaModelChecks modelChecks=new SagaModelChecks();
            assertions+=modelChecks.run(getTargetContext());
            assertions+=new PrayerGpsChecks().run(getTargetContext());
            JSONObject day=new JSONObject("{\"date\":\"25-Oct-2026\",\"fajr\":\"05:48:00\",\"syuruk\":\"07:00:00\",\"dhuhr\":\"13:10:00\",\"asr\":\"16:25:00\",\"maghrib\":\"19:12:00\",\"isha\":\"20:23:00\"}");
            JakimSchedule.Day parsed=new JakimSchedule.Day(day);
            check(parsed.times.length==6,"six JAKIM times including sunrise");
            check(JakimSchedule.time(parsed.times[0]).equals("05:48"),"Malaysian timezone");
            String response=new JSONObject().put("prayerTime",new JSONArray().put(day)).toString();
            check(JakimSchedule.validate(response).length()==1,"official response shape");
            boolean rejected=false;try{JakimSchedule.validate("{\"prayerTime\":[]}");}catch(Exception e){rejected=true;}check(rejected,"reject empty schedule");
            rejected=false;try{new JakimSchedule.Day(new JSONObject(day.toString()).put("asr","02:00:00"));}catch(Exception e){rejected=true;}check(rejected,"reject out of order times");
            check(JakimSchedule.at(getTargetContext(),0)==null,"no fabricated schedule for missing date");
            DashLyrics l=DashLyrics.parse(new JSONObject().put("syncedLyrics","[00:01.20]First\n[00:02.500][00:04.000]Second").put("plainLyrics","First\nSecond"));
            check(l.times.get(0)==1200,"centisecond lyric timestamp");
            check(l.at(1300).startsWith("First"),"current synced lyric");
            check(l.at(2500).startsWith("Second"),"advance synced lyric");
            check(l.times.size()==3,"multiple timestamps per line");
            check(l.at(0).equals("♪"),"before first lyric");
            check(DashLyrics.parse(new JSONObject().put("instrumental",true)).plain.equals("Instrumental"),"instrumental state");
            for(String removed:new String[]{"QuranActivity","DzikirActivity"}){
                boolean absent=false;
                try{getTargetContext().getPackageManager().getActivityInfo(new ComponentName(getTargetContext(),"com.shazwan.sagadash."+removed),0);}catch(android.content.pm.PackageManager.NameNotFoundException e){absent=true;}
                check(absent,"removed launcher feature: "+removed);
            }
            dash[0]=(DashboardActivity)startActivitySync(new Intent(getTargetContext(),DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            assertions+=modelChecks.view(this,dash[0]);
            // Regression: a busy timetable cache must not make UI refresh wait for its global monitor.
            final java.util.concurrent.CountDownLatch locked=new java.util.concurrent.CountDownLatch(1);
            final java.util.concurrent.CountDownLatch release=new java.util.concurrent.CountDownLatch(1);
            Thread holder=new Thread(()->{synchronized(JakimSchedule.class){locked.countDown();try{release.await(2,java.util.concurrent.TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}},"check-cache-contention");
            holder.start();
            if(!locked.await(15,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("Cache-lock test did not start");
            final long[] uiTime={0};
            try{runOnMainSync(()->{
                boolean wasLoading=dash[0].prayerLoading;dash[0].prayerLoading=false;
                long begin=SystemClock.elapsedRealtime();dash[0].fetchPrayer(true);dash[0].refreshClock();uiTime[0]=SystemClock.elapsedRealtime()-begin;
                dash[0].prayerLoading=wasLoading;
            });}finally{release.countDown();holder.join(5000);}
            check(uiTime[0]<750,"UI does not wait for the timetable cache monitor ("+uiTime[0]+"ms)");
            runOnMainSync(()->{
                session[0]=new MediaSession(getTargetContext(),"Saga integration test");
                session[0].setCallback(new MediaSession.Callback(){
                    @Override public void onPause(){session[0].setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED,2500,0).setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE).build());}
                },new Handler(Looper.getMainLooper()));
                session[0].setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,"Dashboard test track").putString(MediaMetadata.METADATA_KEY_ARTIST,"Test artist").build());
                session[0].setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING,1000,1).setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE).build());
                session[0].setActive(true);dash[0].media.choose(getTargetContext().getPackageName());
            });
            waitForIdleSync();
            runOnMainSync(()->{check(dash[0].title.getText().toString().equals("Dashboard test track"),"session metadata displayed");check(dash[0].play.getText().toString().equals("Pause"),"playing state displayed");dash[0].media.command(0);});
            for(int n=0;n<30;n++){SystemClock.sleep(100);if(!dash[0].media.playing())break;}
            runOnMainSync(()->check(!dash[0].media.playing(),"pause routed to selected session"));
            result.putString("stream","PASS: "+assertions+" assertions (JAKIM parser, lyrics, dashboard, real MediaSession controls)\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL after "+assertions+" assertions: "+e.toString()+"\n");finish(Activity.RESULT_CANCELED,result);}
        finally{runOnMainSync(()->{if(session[0]!=null)session[0].release();});}
    }
}
