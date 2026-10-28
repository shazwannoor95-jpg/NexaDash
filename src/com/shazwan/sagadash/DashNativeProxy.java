package com.shazwan.sagadash;
import android.app.Activity;import android.os.*;
/** Host UID must be present on its own private display before launching an opted-in target. */
public final class DashNativeProxy extends Activity {
    private final Handler main=new Handler(Looper.getMainLooper());private boolean launched;
    @Override protected void onResume(){super.onResume();if(launched)return;launched=true;
        main.postDelayed(()->{DashNativeFrame frame=DashNativeFrame.find(getIntent().getStringExtra("native_token"));int id=getWindowManager().getDefaultDisplay().getDisplayId();
            if(frame==null||id==0||id!=getIntent().getIntExtra("native_display",-1)){finish();return;}frame.launchTarget(this,id);},150);
    }
    @Override protected void onDestroy(){main.removeCallbacksAndMessages(null);super.onDestroy();}
}
