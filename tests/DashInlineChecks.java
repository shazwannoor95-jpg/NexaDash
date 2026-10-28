package com.shazwan.sagadash;

import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.webkit.WebSettings;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Real in-panel file playback and local HTML5 video; no claim of YouTube service/hardware availability. */
final class DashInlineChecks {
    int assertions;
    interface Ready{boolean get();}
    void check(boolean value,String label){if(!value)throw new AssertionError(label);assertions++;}
    void await(Instrumentation instrumentation,Ready ready,String label)throws Exception{
        long end=SystemClock.elapsedRealtime()+20000;boolean[] done={false};
        while(!done[0]&&SystemClock.elapsedRealtime()<end){DashAppsChecks.onMain(instrumentation,()->done[0]=ready.get());if(!done[0])SystemClock.sleep(50);}
        check(done[0],label);
    }
    String js(Instrumentation instrumentation,android.webkit.WebView view,String script)throws Exception{
        AtomicReference<String> value=new AtomicReference<>();CountDownLatch done=new CountDownLatch(1);
        DashAppsChecks.onMain(instrumentation,()->view.evaluateJavascript(script,result->{value.set(result);done.countDown();}));
        if(!done.await(10,TimeUnit.SECONDS))throw new AssertionError("WebView script callback timed out");return value.get();
    }
    int run(Instrumentation instrumentation,DashboardActivity dashboard)throws Exception{
        check(DashWebPlayer.allowed("https://m.youtube.com/watch?v=abc"),"first-party HTTPS YouTube URL accepted");
        for(String url:new String[]{"http://youtube.com/","https://youtube.com.evil.invalid/","https://youtube.com@evil.invalid/","intent://video","file:///sdcard/private","javascript:alert(1)"})
            check(!DashWebPlayer.allowed(url),"unsafe/cross-app navigation rejected: "+url);
        check(DashWebPlayer.address("red saga").equals("https://m.youtube.com/results?search_query=red%20saga"),"native search encodes a YouTube query");
        check(DashWebPlayer.address("https://www.youtube.com/watch?v=abc").startsWith("https://m.youtube.com/watch"),"desktop links normalize to the mobile site");
        check(DashFilePlayer.time(65000).equals("1:05"),"local-player time is formatted safely");
        SharedPreferences prefs=dashboard.dashPrefs();Map<String,Object> saved=new HashMap<>();
        for(String key:new String[]{"pane_vehicle","pane_media","panel_video_uri","panel_video_target"})saved.put(key,prefs.getAll().get(key));
        final DashWebPlayer[] web={null};final DashFilePlayer[] file={null};final boolean[] visible={true};
        try{
            DashAppsChecks.onMain(instrumentation,()->{
                visible[0]=dashboard.visible;dashboard.visible=false;dashboard.leftPane.setMode("youtube_web");web[0]=dashboard.leftPane.webPlayer;
                check(web[0]!=null&&web[0].web==null,"saved/hidden YouTube selection creates no WebView");
                web[0].resume();android.webkit.WebView browser=web[0].ensureWeb();
                check(browser!=null&&browser.getParent()==web[0].body,"WebView is actually hosted in the selected panel");
                WebSettings settings=browser.getSettings();
                check(settings.getJavaScriptEnabled()&&settings.getMediaPlaybackRequiresUserGesture(),"website scripts work but media requires a user gesture");
                check(!settings.getAllowFileAccess()&&!settings.getAllowContentAccess()&&settings.getMixedContentMode()==WebSettings.MIXED_CONTENT_NEVER_ALLOW,"web player cannot read file/content URIs or mixed content");
                web[0].open("");
                check(web[0].query.getParent() instanceof android.view.View && ((android.view.View)web[0].query.getParent()).getVisibility()==android.view.View.GONE && web[0].status.getVisibility()==android.view.View.GONE,"opening YouTube reserves panel height for browsing instead of search/help rows");
                check(settings.getTextZoom()==100&&settings.getUseWideViewPort()&&settings.getLoadWithOverviewMode(),"website uses a fitted viewport independent of dashboard font scaling");
                browser.stopLoading();
                browser.loadDataWithBaseURL("https://example.invalid/","<html><meta name='viewport' content='width=device-width,initial-scale=1'><body style='margin:0;background:black'><button style='position:absolute;left:0;top:0;width:100%;height:48px;z-index:2' onclick='document.getElementById(\"v\").play()'>Play test fixture</button><video id='v' playsinline style='width:100%;height:100%' src='data:video/mp4;base64,"+SagaShowroomChecks.FIXTURE+"'></video></body></html>","text/html","UTF-8",null);
                dashboard.visible=visible[0];
            });
            await(instrumentation,()->web[0].web!=null&&web[0].web.getWidth()>100&&web[0].web.getHeight()>60,"local web video fixture has a measured in-panel surface");
            long readyEnd=SystemClock.elapsedRealtime()+15000;
            while(SystemClock.elapsedRealtime()<readyEnd&&!"true".equals(js(instrumentation,web[0].web,"!!document.getElementById('v')")))SystemClock.sleep(50);
            check("true".equals(js(instrumentation,web[0].web,"!!document.getElementById('v')")),"local HTML5 video is present in the real WebView");
            final int[] xy=new int[2];DashAppsChecks.onMain(instrumentation,()->web[0].web.getLocationOnScreen(xy));
            float density=dashboard.getResources().getDisplayMetrics().density;long at=SystemClock.uptimeMillis();
            MotionEvent down=MotionEvent.obtain(at,at,MotionEvent.ACTION_DOWN,xy[0]+40*density,xy[1]+24*density,0);
            MotionEvent up=MotionEvent.obtain(at,at+40,MotionEvent.ACTION_UP,xy[0]+40*density,xy[1]+24*density,0);
            try{instrumentation.sendPointerSync(down);instrumentation.sendPointerSync(up);}finally{down.recycle();up.recycle();}
            boolean advanced=false;long playbackEnd=SystemClock.elapsedRealtime()+15000;
            while(!advanced&&SystemClock.elapsedRealtime()<playbackEnd){String value=js(instrumentation,web[0].web,"document.getElementById('v').currentTime");try{advanced=Double.parseDouble(value)>.1;}catch(RuntimeException ignored){}if(!advanced)SystemClock.sleep(50);}
            check(advanced,"HTML5 fixture actually advances after a real touch inside the panel");
            DashAppsChecks.onMain(instrumentation,()->{
                web[0].pause();check(web[0].web==null,"background/pause destroys the web player");web[0].resume();check(web[0].web==null,"returning does not silently recreate a browser");
                dashboard.rightPane.setMode("video_file");file[0]=dashboard.rightPane.filePlayer;
                check(dashboard.leftPane.webPlayer==null&&!DashboardActivity.inlineMode(dashboard.leftPane.mode),"moving video to the other pane closes the old in-panel player");
                check(file[0]!=null&&file[0].player==null,"native panel does not create a decoder before Play");
                file[0].setSource(SagaShowroomChecks.VALID);file[0].resume();
            });
            await(instrumentation,()->file[0].surface.getHolder().getSurface().isValid(),"native panel has an attached video surface");
            DashAppsChecks.onMain(instrumentation,file[0]::toggle);
            await(instrumentation,()->file[0].playing(),"content-URI video really plays in the native panel");
            DashAppsChecks.onMain(instrumentation,()->{
                file[0].player.setLooping(true);check(file[0].player.getVideoWidth()==64,"decoded synthetic video has its real dimensions");
                file[0].toggle();check(!file[0].playing(),"native Pause stops playback");file[0].toggle();
            });
            await(instrumentation,file[0]::playing,"native Play resumes paused playback");
            DashAppsChecks.onMain(instrumentation,()->{
                file[0].pause();check(file[0].player==null,"pause releases the decoder and audio focus");file[0].resume();check(file[0].player==null,"resume does not autoplay or allocate a decoder");
                file[0].setSource(SagaShowroomChecks.INVALID);file[0].toggle();
            });
            await(instrumentation,()->file[0].player==null&&file[0].status.getText().toString().startsWith("Cannot"),"an invalid video shows a recoverable error and releases its decoder");
            DashAppsChecks.onMain(instrumentation,()->{
                dashboard.rightPane.setMode("media");check(file[0].player==null&&file[0].getParent()==null,"changing panel detaches and releases the file player");
            });
        }finally{
            DashAppsChecks.onMain(instrumentation,()->{
                dashboard.visible=visible[0];dashboard.leftPane.setMode("car");dashboard.rightPane.setMode("media");
                SharedPreferences.Editor edit=prefs.edit();for(Map.Entry<String,Object> entry:saved.entrySet()){if(entry.getValue()==null)edit.remove(entry.getKey());else edit.putString(entry.getKey(),(String)entry.getValue());}edit.commit();
                dashboard.leftPane.setMode(saved.get("pane_vehicle") instanceof String?(String)saved.get("pane_vehicle"):"car");
                dashboard.rightPane.setMode(saved.get("pane_media") instanceof String?(String)saved.get("pane_media"):"media");
            });
        }
        return assertions;
    }
}
