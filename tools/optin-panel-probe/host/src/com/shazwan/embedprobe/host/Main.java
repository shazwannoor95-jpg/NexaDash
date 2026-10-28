package com.shazwan.embedprobe.host;
import android.app.*;
import android.content.*;
import android.hardware.display.*;
import android.graphics.Matrix;
import android.os.*;
import android.view.*;
import android.widget.*;

public class Main extends Activity implements SurfaceHolder.Callback {
    static Main instance;
    final Handler ui=new Handler(Looper.getMainLooper());
    SurfaceView surface;TextView status;VirtualDisplay display;IBinder bridge;
    static final String TOKEN="com.shazwan.embedprobe.CONTROL";
    void note(String text){ui.post(()->status.setText(text));}
    final ServiceConnection connection=new ServiceConnection(){
        public void onServiceConnected(ComponentName name,IBinder binder){bridge=binder;note("Cooperative target service bound. No system permissions requested.");}
        public void onServiceDisconnected(ComponentName name){bridge=null;}
    };
    public void onCreate(Bundle state){super.onCreate(state);instance=this;
        LinearLayout column=new LinearLayout(this);column.setOrientation(1);column.setGravity(Gravity.CENTER_HORIZONTAL);column.setPadding(24,12,24,12);
        status=new TextView(this);status.setTextSize(18);status.setText("Unprivileged private-display proof. Target is a separate APK.");column.addView(status,new LinearLayout.LayoutParams(-1,64));
        surface=new SurfaceView(this);surface.getHolder().addCallback(this);column.addView(surface,new LinearLayout.LayoutParams(640,400));
        TextView stop=new TextView(this);stop.setText("STOP / RELEASE DISPLAY");stop.setTextSize(18);stop.setOnClickListener(v->release());column.addView(stop,new LinearLayout.LayoutParams(-1,60));setContentView(column);
        bindService(new Intent().setComponent(new ComponentName("com.shazwan.embedprobe.target","com.shazwan.embedprobe.target.Bridge")),connection,BIND_AUTO_CREATE);
        surface.setOnTouchListener((view,event)->{
            IBinder remote=bridge;if(remote==null||display==null)return true;
            MotionEvent copy=MotionEvent.obtain(event);Matrix scale=new Matrix();scale.setScale(640f/view.getWidth(),400f/view.getHeight());copy.transform(scale);
            Parcel data=Parcel.obtain();try{data.writeInterfaceToken(TOKEN);copy.writeToParcel(data,0);remote.transact(1,data,null,IBinder.FLAG_ONEWAY);}catch(Exception error){note("Touch bridge: "+error);}finally{data.recycle();copy.recycle();}
            if(event.getActionMasked()==MotionEvent.ACTION_UP)ui.postDelayed(()->{
                Parcel request=Parcel.obtain(),reply=Parcel.obtain();try{request.writeInterfaceToken(TOKEN);remote.transact(2,request,reply,0);reply.readException();note("Native target counter = "+reply.readInt()+" · private display "+display.getDisplay().getDisplayId());}catch(Exception error){note("Query: "+error);}finally{request.recycle();reply.recycle();}
            },500);return true;
        });
    }
    public void surfaceCreated(SurfaceHolder holder){
        if(display!=null||isFinishing())return;
        try{DisplayManager manager=(DisplayManager)getSystemService(DISPLAY_SERVICE);
            display=manager.createVirtualDisplay("Opt-in native probe",640,400,160,holder.getSurface(),DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
            int id=display.getDisplay().getDisplayId();
            startActivity(new Intent(this,Proxy.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK),ActivityOptions.makeBasic().setLaunchDisplayId(id).toBundle());
            note("Private display "+id+" created by ordinary app UID. Host proxy is starting.");
        }catch(Exception error){note("DISPLAY ERROR: "+error);}
    }
    public void surfaceChanged(SurfaceHolder h,int f,int w,int height){}
    public void surfaceDestroyed(SurfaceHolder holder){release();}
    void release(){if(display!=null){display.release();display=null;note("Display released");}}
    protected void onResume(){super.onResume();if(surface!=null&&surface.getHolder().getSurface().isValid())surfaceCreated(surface.getHolder());}
    protected void onStop(){release();super.onStop();}
    protected void onDestroy(){release();try{unbindService(connection);}catch(Exception ignored){}instance=null;super.onDestroy();}
}
