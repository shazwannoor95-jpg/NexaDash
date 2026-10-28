package com.shazwan.sagadash;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Matrix;
import android.hardware.display.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.ref.WeakReference;
import java.util.*;

/** Explicit-start, cooperative native-app experiment. Never hosts arbitrary installed apps. */
final class DashNativeFrame extends LinearLayout implements SurfaceHolder.Callback {
    static final String CONTROL="com.shazwan.embedprobe.permission.CONTROL",TOKEN="com.shazwan.embedprobe.CONTROL";
    static final int MAX_EDGE=800;
    int renderWidth=640,renderHeight=400;
    static final class Target {
        final String label,pkg,activity,service;
        Target(String l,String p,String a,String s){label=l;pkg=p;activity=a;service=s;}
    }
    static final Target[] TARGETS={
        new Target("YouTube V58 · patched","com.gold.android.youtube","com.google.android.youtube.app.honeycomb.Shell$HomeActivity","com.shazwan.nexapanel.Bridge"),
        new Target("TizenTube · patched ARM","com.shazwan.nexadash.tizentube.lab","dev.cobalt.app.MainActivity","com.shazwan.nexapanel.Bridge"),
        new Target("Demo companion","com.shazwan.embedprobe.target","com.shazwan.embedprobe.target.Main","com.shazwan.embedprobe.target.Bridge")
    };
    static final Map<String,WeakReference<DashNativeFrame>> HOSTS=new HashMap<>();
    final Activity owner;final Handler main=new Handler(Looper.getMainLooper());
    final TextView status;final SurfaceView surface;final Button start,stop,choose;final EditText entry;
    final String token=UUID.randomUUID().toString();
    Target target=TARGETS[0];volatile VirtualDisplay display;volatile IBinder remote;
    boolean active,closed,wanted,bound,proxyLaunched,protocolVerified;volatile int generation;
    final Runnable startupTimeout=()->{if(wanted&&display==null)fail("Companion startup timed out. Open app/setup or choose Demo, then try again.");};
    HandlerThread inputThread;Handler input;
    final ArrayDeque<MotionEvent> events=new ArrayDeque<>();
    final Runnable drain=this::drainInput;
    ServiceConnection connection;
    private ServiceConnection connectionFor(final int run){return new ServiceConnection(){
        @Override public void onServiceConnected(ComponentName name,IBinder binder){if(run!=generation||!wanted||closed||!bound)return;remote=binder;
            input.post(()->{boolean supported=false;Parcel data=Parcel.obtain(),reply=Parcel.obtain();try{data.writeInterfaceToken(TOKEN);if(binder.transact(8,data,reply,0)){reply.readException();supported=reply.readInt()==1;}}catch(RemoteException|RuntimeException ignored){}finally{data.recycle();reply.recycle();}
                final boolean valid=supported;main.post(()->{if(run!=generation||!wanted||closed)return;if(!valid){fail("Older companion protocol. Install the Native Lab companion APK.");return;}protocolVerified=true;launchIfReady();});});}
        @Override public void onServiceDisconnected(ComponentName name){if(run==generation)fail("App disconnected. Press Start to reconnect.");}
        @Override public void onBindingDied(ComponentName name){if(run==generation)fail("App updated or stopped. Press Start to reconnect.");}
        @Override public void onNullBinding(ComponentName name){if(run==generation)fail("Companion does not provide a usable input service.");}
    };}
    DashNativeFrame(Activity owner){
        super(owner);this.owner=owner;setOrientation(VERTICAL);
        int pad=Math.max(3,Math.round(4*owner.getResources().getDisplayMetrics().density));
        status=new TextView(owner);status.setTextSize(11);status.setTextColor(0xffdcefff);status.setMaxLines(2);status.setEllipsize(android.text.TextUtils.TruncateAt.END);
        status.setText("Native app lab · install the matching patched companion, then Start. No autoplay.");addView(status,new LayoutParams(-1,-2));
        LinearLayout toolbar=new LinearLayout(owner);
        choose=button("YouTube V58",()->new AlertDialog.Builder(owner).setTitle("Native panel companion")
            .setItems(new String[]{TARGETS[0].label,TARGETS[1].label,TARGETS[2].label},(d,index)->select(index)).setNegativeButton("Cancel",null).show());
        start=button("Start",this::start);stop=button("Stop",this::stop);
        toolbar.addView(choose,new LayoutParams(0,-2,2));toolbar.addView(start,new LayoutParams(0,-2,1));toolbar.addView(stop,new LayoutParams(0,-2,1));addView(toolbar,new LayoutParams(-1,-2));
        surface=new SurfaceView(owner);surface.setContentDescription("Cooperative native app surface");surface.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->fitSurface(r-l,b-t));surface.getHolder().addCallback(this);
        addView(surface,new LayoutParams(-1,0,1));surface.setOnTouchListener((view,event)->{forward(event);return true;});
        LinearLayout tools=new LinearLayout(owner);tools.setOrientation(VERTICAL);tools.setVisibility(GONE);
        Button more=button("⋯",()->tools.setVisibility(tools.getVisibility()==VISIBLE?GONE:VISIBLE));more.setContentDescription("Show native panel input/setup tools");toolbar.addView(more,new LayoutParams(-2,-2));
        LinearLayout keys=new LinearLayout(owner);
        keys.addView(button("Back",()->command(4,null)),new LayoutParams(0,-2,1));
        keys.addView(button("Open app / setup",()->{stop();DashShortcuts.start(owner,new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(target.pkg,target.activity)));}),new LayoutParams(0,-2,2));
        tools.addView(keys,new LayoutParams(-1,-2));
        LinearLayout typing=new LinearLayout(owner);entry=new EditText(owner);entry.setSingleLine(true);entry.setTextSize(12);entry.setHint("Tap text field in app, type here");entry.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(240)});
        typing.addView(entry,new LayoutParams(0,-2,1));typing.addView(button("Send",()->command(5,entry.getText().toString())),new LayoutParams(-2,-2));tools.addView(typing,new LayoutParams(-1,-2));addView(tools,new LayoutParams(-1,-2));
        setPadding(pad,pad,pad,pad);HOSTS.put(token,new WeakReference<>(this));
    }
    /** Preserve the measured panel aspect ratio, with a bounded raster and usable logical UI. */
    static int[] fittedSize(int width,int height){
        width=Math.max(1,width);height=Math.max(1,height);
        double factor=Math.min(MAX_EDGE/(double)Math.max(width,height),Math.max(1,320.0/Math.min(width,height)));
        return new int[]{Math.max(1,(int)Math.round(width*factor)),Math.max(1,(int)Math.round(height*factor))};
    }
    private void fitSurface(int width,int height){
        if(width<=0||height<=0||closed)return;
        int[] size=fittedSize(width,height);
        if(size[0]==renderWidth&&size[1]==renderHeight)return;
        renderWidth=size[0];renderHeight=size[1];
        try{
            surface.getHolder().setFixedSize(renderWidth,renderHeight);
            if(display!=null)display.resize(renderWidth,renderHeight,160);
        }catch(RuntimeException failure){fail("Native resize failed. Press Start to retry.");}
    }
    private Button button(String label,Runnable run){Button b=new Button(owner);b.setText(label);b.setTextSize(11);int touch=Math.round(32*owner.getResources().getDisplayMetrics().density);b.setMinHeight(touch);b.setMinimumHeight(touch);b.setMinWidth(touch);b.setMinimumWidth(touch);b.setPadding(4,2,4,2);b.setOnClickListener(v->run.run());return b;}
    void select(int index){stop();target=TARGETS[Math.max(0,Math.min(TARGETS.length-1,index))];choose.setText(index==0?"YouTube V58":index==1?"TizenTube":"Demo");note("Selected "+target.label+". Start when its matching companion is installed.");}
    static DashNativeFrame find(String token){WeakReference<DashNativeFrame> ref=HOSTS.get(token);return ref==null?null:ref.get();}
    private void note(String message){if(!closed)status.setText(message);}
    private String validate(){
        if(Build.VERSION.SDK_INT<26)return "Native hosting needs Android 8.0/API26 or newer.";
        PackageManager pm=owner.getPackageManager();
        try{
            pm.getApplicationInfo(target.pkg,0);
            if(pm.checkSignatures(owner.getPackageName(),target.pkg)!=PackageManager.SIGNATURE_MATCH)return "Install a companion signed with the same key as NexaDash Lab. Original/unpatched apps cannot be used here.";
            ActivityInfo a=pm.getActivityInfo(new ComponentName(target.pkg,target.activity),0);
            ServiceInfo s=pm.getServiceInfo(new ComponentName(target.pkg,target.service),0);
            if(!a.exported||!a.enabled||(a.flags&0x80000000)==0)return "Companion activity has not opted into native display hosting.";
            if(!s.exported||!s.enabled||!CONTROL.equals(s.permission))return "Matching signature-protected companion service is missing.";
            if(pm.checkPermission(CONTROL,owner.getPackageName())!=PackageManager.PERMISSION_GRANTED)return "Install the companion first, then reopen/reinstall this Lab APK to grant its signature permission.";
        }catch(PackageManager.NameNotFoundException absent){return target.label+" companion not installed. Use its matching Lab APK; Demo supports API26+.";}
        return null;
    }
    void start(){
        if(closed||!active){note("Return to the visible panel before starting.");return;}
        stop();String error=validate();if(error!=null){note(error);return;}
        wanted=true;generation++;main.postDelayed(startupTimeout,60000);connection=connectionFor(generation);inputThread=new HandlerThread("Nexa-native-input");inputThread.start();input=new Handler(inputThread.getLooper());
        try{bound=owner.bindService(new Intent().setComponent(new ComponentName(target.pkg,target.service)),connection,Context.BIND_AUTO_CREATE);
            if(!bound){fail("Unable to bind companion. Install the matching patched APK.");return;}note("Starting "+target.label+" · initial app setup may take time.");
        }catch(RuntimeException failure){fail("Companion bind failed: "+failure.getClass().getSimpleName());}
    }
    private void launchIfReady(){
        if(!wanted||!protocolVerified||remote==null||display!=null||!surface.getHolder().getSurface().isValid()||closed)return;
        try{
            fitSurface(surface.getWidth(),surface.getHeight());
            if(!wanted)return; // A failed resize already stopped and cleaned up this run.
            DisplayManager manager=(DisplayManager)owner.getSystemService(Context.DISPLAY_SERVICE);
            display=manager.createVirtualDisplay("NexaDash native lab",renderWidth,renderHeight,160,surface.getHolder().getSurface(),DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
            if(display==null)throw new IllegalStateException("No virtual display");
            int id=display.getDisplay().getDisplayId();proxyLaunched=true;main.removeCallbacks(startupTimeout);
            owner.startActivity(new Intent(owner,DashNativeProxy.class).putExtra("native_token",token).putExtra("native_display",id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK),ActivityOptions.makeBasic().setLaunchDisplayId(id).toBundle());
            status.setMaxLines(1);note("Native "+target.label+" · requested display "+id+". Setup/dialog/keyboard support is experimental.");
        }catch(RuntimeException failure){fail("Display launch failed: "+failure.getClass().getSimpleName());}
    }
    void launchTarget(Activity proxy,int id){
        if(!wanted||closed||display==null||display.getDisplay().getDisplayId()!=id){proxy.finish();return;}
        try{proxy.startActivity(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(target.pkg,target.activity))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK),ActivityOptions.makeBasic().setLaunchDisplayId(id).toBundle());}
        catch(RuntimeException failure){fail("App launch rejected: "+failure.getClass().getSimpleName());proxy.finish();}
    }
    private void forward(MotionEvent event){
        if(!wanted||display==null||remote==null||input==null)return;
        synchronized(events){
            if(events.size()>8&&event.getActionMasked()==MotionEvent.ACTION_MOVE)return;
            if(events.size()>=32){for(MotionEvent queued:events)queued.recycle();events.clear();MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);events.add(cancel);}
            MotionEvent copy=MotionEvent.obtain(event);Matrix scale=new Matrix();scale.setScale(renderWidth/(float)Math.max(1,surface.getWidth()),renderHeight/(float)Math.max(1,surface.getHeight()));copy.transform(scale);events.add(copy);
        }
        input.removeCallbacks(drain);input.post(drain);
    }
    private void drainInput(){
        Handler worker=input;if(worker==null||Looper.myLooper()!=worker.getLooper())return;final int run=generation;
        while(run==generation&&input==worker){MotionEvent event;synchronized(events){event=events.poll();}if(event==null)return;
            Parcel data=Parcel.obtain();try{IBinder binder=remote;if(binder!=null&&run==generation){data.writeInterfaceToken(TOKEN);VirtualDisplay current=display;if(current!=null){data.writeInt(current.getDisplay().getDisplayId());event.writeToParcel(data,0);binder.transact(6,data,null,IBinder.FLAG_ONEWAY);}}}
            catch(RemoteException failure){main.post(()->{if(run==generation)fail("Input service disconnected. Press Start to reconnect.");});}
            finally{data.recycle();event.recycle();}
        }
    }
    private void command(int code,String text){
        Handler worker=input;IBinder binder=remote;if(worker==null||binder==null||display==null){note("Start a companion first.");return;}
        final int run=generation,id=display.getDisplay().getDisplayId();
        worker.post(()->{Parcel data=Parcel.obtain();try{data.writeInterfaceToken(TOKEN);data.writeInt(id);if(code==5)data.writeString(text);binder.transact(code,data,null,IBinder.FLAG_ONEWAY);}
            catch(RemoteException failure){main.post(()->{if(generation==run)fail("Companion input service disconnected.");});}finally{data.recycle();}});
    }
    private void fail(String message){stop();note(message);}
    void stop(){
        wanted=false;proxyLaunched=false;protocolVerified=false;main.removeCallbacks(startupTimeout);generation++;remote=null;
        if(bound){bound=false;try{owner.unbindService(connection);}catch(IllegalArgumentException ignored){}}
        if(display!=null){display.release();display=null;}
        synchronized(events){for(MotionEvent event:events)event.recycle();events.clear();}
        if(input!=null)input.removeCallbacksAndMessages(null);input=null;
        if(inputThread!=null)inputThread.quit();inputThread=null;
        status.setMaxLines(2);note("Stopped · no native display or input worker active. Press Start to reopen.");
    }
    void resume(){active=true;}
    void pause(){active=false;stop();}
    void close(){pause();closed=true;HOSTS.remove(token);}
    @Override public void surfaceCreated(SurfaceHolder h){launchIfReady();}
    @Override public void surfaceChanged(SurfaceHolder h,int format,int width,int height){}
    @Override public void surfaceDestroyed(SurfaceHolder h){stop();}
    @Override protected void onDetachedFromWindow(){pause();super.onDetachedFromWindow();}
}
