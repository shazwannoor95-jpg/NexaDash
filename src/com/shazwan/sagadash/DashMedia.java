package com.shazwan.sagadash;

import android.content.*;
import android.media.MediaMetadata;
import android.media.session.*;
import android.os.*;
import java.util.*;

/** Reads public Android media sessions; does not pretend vendor radio/BT publish sessions. */
final class DashMedia {
    final Context c;
    final Runnable changed;
    final MediaSessionManager manager;
    final ComponentName access;
    MediaController current;
    String selected="", problem="Enable media access to show track details";
    boolean started;
    final MediaController.Callback callback=new MediaController.Callback(){
        @Override public void onMetadataChanged(MediaMetadata m){ changed.run(); }
        @Override public void onPlaybackStateChanged(PlaybackState s){ changed.run(); }
        @Override public void onSessionDestroyed(){ update(); }
    };
    final MediaSessionManager.OnActiveSessionsChangedListener listener=sessions->update();
    DashMedia(Context c,Runnable changed){this.c=c;this.changed=changed;manager=(MediaSessionManager)c.getSystemService(Context.MEDIA_SESSION_SERVICE);access=new ComponentName(c,DashMediaAccess.class);}
    void start(){
        stop(); started=true;
        try{manager.addOnActiveSessionsChangedListener(listener,access,new Handler(Looper.getMainLooper()));}catch(SecurityException e){problem="Enable media access in Settings";}
        update();
    }
    List<MediaController> sessions(){try{return manager.getActiveSessions(access);}catch(SecurityException e){return Collections.emptyList();}}
    void choose(String pkg){selected=pkg;update();}
    void update(){
        List<MediaController> list=sessions(); MediaController best=null;
        for(MediaController m:list){
            if(!selected.isEmpty()&&!selected.equals(m.getPackageName()))continue;
            if(best==null)best=m;
            PlaybackState s=m.getPlaybackState(); if(s!=null&&s.getState()==PlaybackState.STATE_PLAYING){best=m;break;}
        }
        if(current!=null)current.unregisterCallback(callback);
        current=best;if(current!=null)current.registerCallback(callback);
        problem=selected.isEmpty()?"Play media in an app, or enable media access":"This source is not publishing a media session";
        changed.run();
    }
    void stop(){started=false; manager.removeOnActiveSessionsChangedListener(listener);if(current!=null)current.unregisterCallback(callback);current=null;}
    boolean playing(){PlaybackState s=current==null?null:current.getPlaybackState();return s!=null&&s.getState()==PlaybackState.STATE_PLAYING;}
    void command(int which){
        if(current==null)return;
        PlaybackState s=current.getPlaybackState(); if(s==null)return;
        long required=which<0?PlaybackState.ACTION_SKIP_TO_PREVIOUS:which>0?PlaybackState.ACTION_SKIP_TO_NEXT:playing()?PlaybackState.ACTION_PAUSE:PlaybackState.ACTION_PLAY;
        if((s.getActions()&required)==0 && !(which==0&&(s.getActions()&PlaybackState.ACTION_PLAY_PAUSE)!=0))return;
        MediaController.TransportControls t=current.getTransportControls();if(which<0)t.skipToPrevious();else if(which>0)t.skipToNext();else if(playing())t.pause();else t.play();
    }
    long position(){
        PlaybackState s=current==null?null:current.getPlaybackState();if(s==null)return 0;
        return Math.max(0,s.getPosition()+(playing()?(long)((SystemClock.elapsedRealtime()-s.getLastPositionUpdateTime())*s.getPlaybackSpeed()):0));
    }
}
