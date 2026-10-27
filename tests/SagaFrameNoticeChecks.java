package com.shazwan.sagadash;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.View;

/** A small synthetic fixture exercises decoded-frame callbacks without benchmarking the emulator's video codec. */
final class SagaFrameNoticeChecks {
    int assertions;
    interface Ready {boolean ready();}
    interface Work {void run();}
    void check(boolean condition,String message){if(!condition)throw new AssertionError(message);assertions++;}
    int run(Instrumentation instrumentation,SagaShowroom showroom) throws Exception {
        final boolean[] wasPlaying={false},wasLooping={false};final int[] startFrames={0},startNotices={0},beforeReset={0};
        try{
            onMain(instrumentation,()->{
                wasPlaying[0]=showroom.playing();wasLooping[0]=showroom.player.isLooping();showroom.player.setLooping(true);
                startFrames[0]=showroom.video.drawnFrames;startNotices[0]=showroom.video.frameNotifications;
                if(!wasPlaying[0])showroom.toggle();
            });
            await(instrumentation,showroom,()->showroom.video.drawnFrames>=startFrames[0]+8&&showroom.status.getVisibility()==View.GONE,"video frames/status did not become visible (starting drawn="+startFrames[0]+", notices="+startNotices[0]+")");
            onMain(instrumentation,()->{
                check(showroom.video.drawnFrames>=startFrames[0]+8,"decoded video keeps rendering while status notifications are gated");
                check(showroom.video.frameNotifications-startNotices[0]<=3,"rendering eight frames does not post eight status notifications");
                check(showroom.status.getVisibility()==View.GONE,"the prepared opening frame hides loading status");
                beforeReset[0]=showroom.video.frameNotifications;showroom.status.setVisibility(View.VISIBLE);showroom.video.clearFrame();
            });
            await(instrumentation,showroom,()->showroom.video.frameNotifications>beforeReset[0]&&showroom.status.getVisibility()==View.GONE,"cleared source did not receive a new first-frame notice");
            onMain(instrumentation,()->{
                check(showroom.video.frameNotifications<=beforeReset[0]+2,"clearing a source rearms a bounded first-frame notification");
                if(showroom.playing())showroom.toggle();
                beforeReset[0]=showroom.video.frameNotifications;showroom.status.setVisibility(View.VISIBLE);
                // Exercise onPrepared's path when an opening frame was already decoded:
                // the paused renderer must notify on the existing dirty redraw.
                showroom.video.setVideoSize(showroom.player.getVideoWidth(),showroom.player.getVideoHeight(),showroom.packedAlpha);
            });
            await(instrumentation,showroom,()->showroom.video.frameNotifications>beforeReset[0]&&showroom.status.getVisibility()==View.GONE,"prepared-size redraw did not notify for an existing paused frame");
            onMain(instrumentation,()->check(!showroom.playing(),"first-frame notification does not restart a paused video"));
        }finally{
            onMain(instrumentation,()->{if(showroom.player!=null&&showroom.prepared){showroom.player.setLooping(wasLooping[0]);if(showroom.playing()!=wasPlaying[0])showroom.toggle();}});
        }
        return assertions;
    }
    private static void onMain(Instrumentation instrumentation,Work work) throws Exception {
        final Throwable[] failure={null};
        instrumentation.runOnMainSync(()->{try{work.run();}catch(Throwable error){failure[0]=error;}});
        if(failure[0] instanceof Error)throw (Error)failure[0];
        if(failure[0] instanceof Exception)throw (Exception)failure[0];
        if(failure[0]!=null)throw new AssertionError(failure[0]);
    }
    private static void await(Instrumentation instrumentation,SagaShowroom showroom,Ready condition,String message) throws Exception {
        long deadline=SystemClock.elapsedRealtime()+15000;final boolean[] ready={false};
        while(!ready[0]&&SystemClock.elapsedRealtime()<deadline){
            onMain(instrumentation,()->ready[0]=condition.ready());
            if(!ready[0])SystemClock.sleep(50);
        }
        if(!ready[0]){
            final String[] state={""};
            onMain(instrumentation,()->state[0]=showroom.video.frameState()+", "+playerState(showroom)+", active="+showroom.active+", prepared="+showroom.prepared+", playing="+showroom.playing()+", statusVisibility="+showroom.status.getVisibility()+", status="+showroom.status.getText()+", parent="+(showroom.getParent()!=null));
            throw new AssertionError(message+": "+state[0]);
        }
    }
    private static String playerState(SagaShowroom showroom){try{return "position="+(showroom.player==null?-1:showroom.player.getCurrentPosition())+", duration="+(showroom.player==null?-1:showroom.player.getDuration());}catch(IllegalStateException error){return "playerState="+error;}}
}
