package com.shazwan.embedprobe.target;
import android.app.*;import android.os.*;import android.net.*;import android.media.*;import android.widget.*;import java.lang.ref.*;import java.util.concurrent.atomic.AtomicInteger;
public class Main extends Activity {
    static WeakReference<Main> current=new WeakReference<>(null);static final AtomicInteger count=new AtomicInteger();
    TextView counter;VideoView video;
    public void onCreate(Bundle state){super.onCreate(state);current=new WeakReference<>(this);
        LinearLayout column=new LinearLayout(this);column.setOrientation(1);column.setPadding(12,12,12,12);column.setBackgroundColor(0xff123947);
        TextView title=new TextView(this);title.setTextColor(0xff00ffff);title.setTextSize(22);title.setText("SEPARATE NATIVE APP · display "+getWindowManager().getDefaultDisplay().getDisplayId());column.addView(title,new LinearLayout.LayoutParams(-1,64));
        counter=new TextView(this);counter.setTextColor(0xffffffff);counter.setTextSize(24);counter.setText("TOUCH COUNTER: "+count.get());counter.setBackgroundColor(0xff6644aa);counter.setOnClickListener(v->counter.setText("TOUCH COUNTER: "+count.incrementAndGet()));column.addView(counter,new LinearLayout.LayoutParams(-1,64));
        video=new VideoView(this);video.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE);video.setOnPreparedListener(mp->{mp.setLooping(true);mp.setVolume(0,0);video.start();});video.setVideoURI(Uri.parse("android.resource://"+getPackageName()+"/raw/probe"));column.addView(video,new LinearLayout.LayoutParams(-1,0,1));setContentView(column);
    }
    protected void onStop(){video.pause();super.onStop();}
    protected void onDestroy(){video.stopPlayback();current.clear();super.onDestroy();}
}
