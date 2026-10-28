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
    int assertions;boolean nativeOnly,layoutOnly;
    void check(boolean ok,String message){if(!ok)throw new AssertionError(message);assertions++;}
    @Override public void onCreate(Bundle args){super.onCreate(args);nativeOnly=args!=null&&"true".equals(args.getString("nativeOnly"));layoutOnly=args!=null&&"true".equals(args.getString("layoutOnly"));start();}
    @Override public void onStart(){
        Bundle result=new Bundle();MediaSession[] session=new MediaSession[1];DashboardActivity[] dash=new DashboardActivity[1];
        try{
            if(layoutOnly){
                assertions+=new DashLayoutChecks().run(this);
                dash[0]=(DashboardActivity)startActivitySync(new Intent(getTargetContext(),DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                waitForIdleSync();
                runOnMainSync(()->{
                    android.graphics.RectF left=dash[0].layoutSurface.boundsFor("vehicle"),right=dash[0].layoutSurface.boundsFor("media"),dock=dash[0].layoutSurface.boundsFor("dock");
                    check(Math.abs(left.height()-right.height())<1&&Math.abs(left.width()-right.width())<1,"expanded panels remain equal");
                    check(dock.top>=left.bottom&&dock.bottom==dash[0].layoutSurface.getHeight(),"compact dock sits below panels at the bottom");
                    check(dash[0].footerText.getVisibility()==android.view.View.GONE,"hidden footer reserves no layout space");
                    android.widget.TextView clock=dash[0].clock;
                    check(clock.getWidth()>0&&clock.getPaint().measureText(clock.getText().toString())<=clock.getWidth()-clock.getPaddingLeft()-clock.getPaddingRight(),"clock text fits available horizontal space");
                    android.graphics.Paint.FontMetrics metrics=clock.getPaint().getFontMetrics();
                    check(metrics.descent-metrics.ascent<=clock.getHeight()-clock.getPaddingTop()-clock.getPaddingBottom(),"clock text fits available vertical space");
                });
                result.putString("stream","PASS: "+assertions+" assertions (compact dock, layout drag/resize, panel space and clock fit)\n");
                finish(Activity.RESULT_OK,result);return;
            }
            if(nativeOnly){
                dash[0]=(DashboardActivity)startActivitySync(new Intent(getTargetContext(),DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                assertions+=new DashNativeChecks().run(this,dash[0]);
                result.putString("stream","PASS: "+assertions+" assertions (focused native-panel display, aspect, input, cleanup)\n");
                finish(Activity.RESULT_OK,result);return;
            }
            assertions+=new TrustedClockChecks().run();
            assertions+=new DashShortcutsChecks().run(getTargetContext());
            assertions+=new DashPanelCompatibilityChecks().run(getTargetContext());
            assertions+=new DashLayoutChecks().run(this);
            assertions+=new DashNavigationChecks().run();
            assertions+=new DashMediaChecks().run(this);
            final String applicationId="com.shazwan.nexadash.lab";
            final String namespace="com.shazwan.sagadash";
            check(getTargetContext().getPackageName().equals(applicationId),"personal application ID");
            check(DashboardActivity.class.getName().equals(namespace+".DashboardActivity")
                    &&R.class.getName().equals(namespace+".R"),"Java and resource namespace migrated");
            check(getTargetContext().getApplicationInfo().dataDir.endsWith("/"+applicationId),"personal private data directory");
            android.content.pm.PackageInfo installed=getTargetContext().getPackageManager().getPackageInfo(applicationId,
                    android.content.pm.PackageManager.GET_ACTIVITIES|android.content.pm.PackageManager.GET_SERVICES);
            boolean componentsMigrated=true;
            for(android.content.pm.ActivityInfo component:installed.activities)componentsMigrated&=component.name.startsWith(namespace+".");
            for(android.content.pm.ServiceInfo component:installed.services)componentsMigrated&=component.name.startsWith(namespace+".");
            check(componentsMigrated,"all registered activity/service classes use personal namespace");
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
            assertions+=new DashNavigationChecks().runUi(this,dash[0]);
            assertions+=new DashPanelCompatibilityChecks().runUi(this,dash[0]);
            assertions+=new DashAppsChecks().run(this,dash[0]);
            assertions+=new DashSettingsGridChecks().run(this,dash[0]);
            assertions+=new DashCustomizationChecks().run(this,dash[0]);
            assertions+=new DashInlineChecks().run(this,dash[0]);
            assertions+=new DashAppearanceChecks().run(this,dash[0]);
            assertions+=new SagaShowroomChecks().run(this,dash[0]);
            assertions+=new SagaAngleChecks().run(this,dash[0]);
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
                session[0].setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,"Dashboard test track").putString(MediaMetadata.METADATA_KEY_ARTIST,"Test artist").putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,android.graphics.Bitmap.createBitmap(512,512,android.graphics.Bitmap.Config.ARGB_8888)).build());
                session[0].setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING,1000,1).setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE).build());
                session[0].setActive(true);dash[0].media.choose(getTargetContext().getPackageName());
            });
            waitForIdleSync();
            runOnMainSync(()->{check(dash[0].title.getText().toString().equals("Dashboard test track"),"session metadata displayed");check(dash[0].play.getText().toString().equals("Pause"),"playing state displayed");check(((android.graphics.drawable.BitmapDrawable)dash[0].artwork.getDrawable()).getBitmap().getWidth()<=128,"album artwork is bounded before display");dash[0].media.command(0);});
            for(int n=0;n<30;n++){SystemClock.sleep(100);if(!dash[0].media.playing())break;}
            runOnMainSync(()->check(!dash[0].media.playing(),"pause routed to selected session"));
            assertions+=new DashNativeChecks().run(this,dash[0]);
            result.putString("stream","PASS: "+assertions+" assertions (JAKIM parser, lyrics, dashboard, real MediaSession controls)\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL after "+assertions+" assertions: "+e.toString()+"\n");finish(Activity.RESULT_CANCELED,result);}
        finally{runOnMainSync(()->{if(session[0]!=null)session[0].release();});}
    }
}
