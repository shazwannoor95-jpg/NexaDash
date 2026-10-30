package com.shazwan.embedprobe.hostv58;
import android.app.*;import android.content.*;import android.os.*;import android.widget.*;
public class Proxy extends Activity {
    boolean launched;
    public void onCreate(Bundle state){super.onCreate(state);TextView text=new TextView(this);text.setText("Host-owned opt-in proxy");setContentView(text);}
    protected void onResume(){super.onResume();if(launched)return;launched=true;
        new Handler().postDelayed(()->{
            try{int id=getWindowManager().getDefaultDisplay().getDisplayId();
                startActivity(new Intent().setComponent(new ComponentName("com.gold.android.youtube","com.google.android.youtube.app.honeycomb.Shell$HomeActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK),ActivityOptions.makeBasic().setLaunchDisplayId(id).toBundle());
                if(Main.instance!=null)Main.instance.note("Native opted-in target launched on private display "+id);
            }catch(Exception error){if(Main.instance!=null)Main.instance.note("TARGET LAUNCH ERROR: "+error);}
        },500);
    }
}
