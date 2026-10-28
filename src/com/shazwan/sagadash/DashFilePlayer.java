package com.shazwan.sagadash;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.*;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/** Local content-URI playback in a native panel; no copying, WebView or external activity. */
final class DashFilePlayer extends LinearLayout implements AutoCloseable,SurfaceHolder.Callback {
    final SurfaceView surface;
    final TextView status,play;
    final SeekBar seek;
    MediaPlayer player;
    private final ExecutorService worker;
    private final AudioManager audio;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final AudioManager.OnAudioFocusChangeListener focus;
    private final FrameLayout frame;
    private Future<?> loading;
    private Uri uri;
    private volatile int generation;
    private volatile boolean active;
    private boolean closed,surfaceReady,prepared,preparing,wantPlay,tracking,hasFocus,completed;
    private int position,videoWidth=16,videoHeight=9;
    private final Runnable progress=new Runnable(){@Override public void run(){
        if(active&&player!=null&&prepared){updateProgress();if(playing())main.postDelayed(this,1000);}
    }};

    DashFilePlayer(Context context,ExecutorService worker,Uri initial,Runnable pick){
        super(context);this.worker=worker;uri=initial;audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);setOrientation(VERTICAL);
        focus=change->{if(change<0)main.post(()->{if(hasFocus)pausePlayback();});};
        frame=new FrameLayout(context){@Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);fit();}};
        frame.setBackgroundColor(android.graphics.Color.BLACK);surface=new SurfaceView(context);surface.getHolder().addCallback(this);frame.addView(surface,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));addView(frame,new LayoutParams(-1,0,1));
        status=new TextView(context);status.setTextColor(DashboardActivity.MUTED);status.setTextSize(11);status.setMaxLines(2);addView(status,new LayoutParams(-1,-2));
        LinearLayout row=new LinearLayout(context);
        row.addView(button("Choose video",pick),new LayoutParams(0,dp(38),1));play=button("Play",this::toggle);row.addView(play,new LayoutParams(0,dp(38),1));addView(row);
        seek=new SeekBar(context);seek.setMax(1000);addView(seek,new LayoutParams(-1,dp(28)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar bar){tracking=true;}
            public void onStopTrackingTouch(SeekBar bar){tracking=false;if(player!=null&&prepared)try{position=(int)((long)player.getDuration()*bar.getProgress()/1000);player.seekTo(position);}catch(IllegalStateException ignored){}}
            public void onProgressChanged(SeekBar bar,int value,boolean user){}
        });refresh();
    }
    private int dp(float value){return Math.max(1,Math.round(value*getResources().getDisplayMetrics().density));}
    private TextView button(String value,Runnable action){TextView b=new TextView(getContext());b.setText(value);b.setTextColor(DashboardActivity.TEXT);b.setTextSize(13);b.setGravity(Gravity.CENTER);android.graphics.drawable.GradientDrawable tile=new android.graphics.drawable.GradientDrawable();tile.setColor(DashboardActivity.PANEL);tile.setCornerRadius(dp(12));tile.setStroke(dp(1),DashboardActivity.LINE);b.setBackground(tile);b.setOnClickListener(v->action.run());return b;}
    void setSource(Uri value){release(true);uri=value;position=0;completed=false;refresh();}
    void resume(){if(!closed){active=true;refresh();}}
    void pause(){active=false;release(false);refresh();}
    boolean playing(){try{return player!=null&&prepared&&player.isPlaying();}catch(IllegalStateException ignored){return false;}}
    void toggle(){
        if(!active||closed||uri==null)return;
        if(preparing){release(false);refresh();return;}
        if(playing()){pausePlayback();return;}
        wantPlay=true;
        if(prepared&&player!=null)start();else if(surfaceReady)prepare();
    }
    private void prepare(){
        if(preparing||!active||!surfaceReady||uri==null)return;
        preparing=true;refresh();final int request=++generation;final Uri source=uri;
        try{loading=worker.submit(()->{
            if(request!=generation||!active)return;
            final MediaPlayer candidate;
            try{candidate=new MediaPlayer();}catch(RuntimeException unavailable){main.post(()->{if(request==generation)fail("Media player unavailable. Tap Play to retry.");});return;}
            try{
                candidate.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build());
                candidate.setDataSource(getContext().getApplicationContext(),source);
                main.post(()->{
                    if(request!=generation||!active||!surfaceReady){candidate.release();return;}
                    player=candidate;
                    candidate.setOnPreparedListener(mp->{if(player!=mp||request!=generation)return;preparing=false;prepared=true;
                        videoWidth=mp.getVideoWidth();videoHeight=mp.getVideoHeight();
                        if(videoWidth<=0||videoHeight<=0){fail("Choose a video file with a supported video track.");return;}
                        if((long)videoWidth*videoHeight>1920L*1080||Math.max(videoWidth,videoHeight)>1920){fail("Choose a video up to 1080p; 720p H.264 is recommended.");return;}
                        fit();if(position>0)mp.seekTo(position);if(wantPlay)start();else refresh();});
                    candidate.setOnCompletionListener(mp->{if(player==mp){position=0;completed=true;wantPlay=false;main.removeCallbacks(progress);abandonFocus();refresh();}});
                    candidate.setOnErrorListener((mp,what,extra)->{if(player==mp)fail("Cannot play this video. Choose another file, preferably H.264 MP4.");return true;});
                    try{candidate.setDisplay(surface.getHolder());candidate.prepareAsync();}catch(RuntimeException failure){fail("Video unavailable. Choose the file again.");}
                });
            }catch(Exception failure){candidate.release();main.post(()->{if(request==generation)fail("Cannot read video. Choose the file again.");});}
        });}catch(RejectedExecutionException unavailable){preparing=false;refresh();}
    }
    @SuppressWarnings("deprecation") private void start(){
        if(player==null||!prepared||!active)return;
        hasFocus=audio!=null&&audio.requestAudioFocus(focus,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        if(!hasFocus){wantPlay=false;status.setText("Audio is in use. Tap Play to retry.");play.setText("Play");return;}
        try{if(completed){player.seekTo(0);completed=false;}player.start();wantPlay=true;main.removeCallbacks(progress);progress.run();refresh();}catch(IllegalStateException failure){fail("Playback stopped. Select the video again.");}
    }
    private void pausePlayback(){wantPlay=false;if(player!=null&&prepared)try{player.pause();position=player.getCurrentPosition();}catch(IllegalStateException ignored){}main.removeCallbacks(progress);abandonFocus();refresh();}
    @SuppressWarnings("deprecation") private void abandonFocus(){if(hasFocus&&audio!=null)audio.abandonAudioFocus(focus);hasFocus=false;}
    private void release(boolean reset){
        ++generation;if(loading!=null){loading.cancel(false);loading=null;}main.removeCallbacks(progress);
        if(player!=null){if(prepared&&!reset&&!completed)try{position=player.getCurrentPosition();}catch(IllegalStateException ignored){}player.release();player=null;}
        prepared=false;preparing=false;wantPlay=false;abandonFocus();
    }
    private void fail(String message){release(false);status.setText(message);play.setText("Play");seek.setEnabled(false);}
    private void refresh(){play.setText(preparing?"Cancel":playing()?"Pause":"Play");play.setEnabled(uri!=null);seek.setEnabled(prepared);if(uri==null)status.setText("Choose a video file · plays inside this panel");else if(preparing)status.setText("Preparing video…");else updateProgress();}
    private void updateProgress(){
        int duration=0;if(player!=null&&prepared)try{position=completed?0:player.getCurrentPosition();duration=player.getDuration();}catch(IllegalStateException ignored){}
        if(!tracking)seek.setProgress(duration>0?(int)((long)position*1000/duration):0);
        status.setText(time(position)+" / "+time(duration)+" · local video");
    }
    static String time(int millis){long seconds=Math.max(0,millis)/1000L;return String.format(Locale.US,"%d:%02d",seconds/60,seconds%60);}
    private void fit(){int w=frame.getWidth(),h=frame.getHeight();if(w<=0||h<=0)return;float ratio=Math.min((float)w/videoWidth,(float)h/videoHeight);
        FrameLayout.LayoutParams params=(FrameLayout.LayoutParams)surface.getLayoutParams();int width=Math.max(1,Math.round(videoWidth*ratio)),height=Math.max(1,Math.round(videoHeight*ratio));
        if(params.width!=width||params.height!=height){params.width=width;params.height=height;surface.setLayoutParams(params);}}
    @Override public void surfaceCreated(SurfaceHolder holder){surfaceReady=true;if(active&&wantPlay)prepare();}
    @Override public void surfaceChanged(SurfaceHolder holder,int format,int width,int height){}
    @Override public void surfaceDestroyed(SurfaceHolder holder){surfaceReady=false;release(false);}
    @Override public void close(){closed=true;pause();surface.getHolder().removeCallback(this);}
}
