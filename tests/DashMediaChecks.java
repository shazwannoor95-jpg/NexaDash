package com.shazwan.sagadash;

import android.app.Instrumentation;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import java.util.Collections;

/** Late callback regression: a paused dashboard must not regain a session observer. */
final class DashMediaChecks {
    int assertions;
    void check(boolean condition,String message){if(!condition)throw new AssertionError(message);assertions++;}
    int run(Instrumentation instrumentation){
        final Throwable[] failure={null};
        instrumentation.runOnMainSync(()->{
            MediaSession session=new MediaSession(instrumentation.getTargetContext(),"Saga media lifecycle checks");
            int[] changes={0};DashMedia media=new DashMedia(instrumentation.getTargetContext(),()->changes[0]++);
            try{
                session.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING,1000,1).setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE).build());
                session.setActive(true);
                media.choose(instrumentation.getTargetContext().getPackageName());
                check(media.current==null&&changes[0]==0,"source selection while stopped does not attach a media observer");
                media.start();
                check(media.started&&media.current!=null,"starting connects to the selected active session");
                media.stop();int stoppedChanges=changes[0];
                // Deliver notifications as if they were queued just before stop().
                media.callback.onMetadataChanged(new MediaMetadata.Builder().build());
                media.callback.onPlaybackStateChanged(session.getController().getPlaybackState());
                media.callback.onSessionDestroyed();
                media.listener.onActiveSessionsChanged(Collections.emptyList());
                media.update();
                check(!media.started&&media.current==null,"late session callbacks cannot reconnect a stopped observer");
                check(changes[0]==stoppedChanges,"late media callbacks do not update a paused dashboard");
                media.start();
                check(media.started&&media.current!=null,"media reconnects normally after resume");
            }catch(Throwable error){failure[0]=error;}
            finally{media.stop();session.release();}
        });
        if(failure[0]!=null)throw new AssertionError("Media lifecycle regression",failure[0]);
        return assertions;
    }
}
