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
    boolean mapsSpacious=true;
    static final class Target {
        final String label,pkg,activity,service,permission;
        Target(String l,String p,String a,String s){this(l,p,a,s,CONTROL);}
        Target(String l,String p,String a,String s,String required){label=l;pkg=p;activity=a;service=s;permission=required;}
    }
    static final Target[] TARGETS={
        new Target("YouTube V58 · patched","com.gold.android.youtube","com.google.android.youtube.app.honeycomb.Shell$HomeActivity","com.shazwan.nexapanel.Bridge"),
        new Target("TizenTube · patched ARM","com.shazwan.nexadash.tizentube.lab","dev.cobalt.app.MainActivity","com.shazwan.nexapanel.Bridge"),
        new Target("Demo companion","com.shazwan.embedprobe.target","com.shazwan.embedprobe.target.Main","com.shazwan.embedprobe.target.Bridge"),
        new Target("Google Maps · Next companion","com.shazwan.nexadash.maps.next","com.google.android.maps.MapsActivity","com.shazwan.nexapanel.Bridge","com.shazwan.nexadash.next.permission.CONTROL"),
        new Target("TizenTube · Next companion","com.shazwan.nexadash.tizentube.next","dev.cobalt.app.MainActivity","com.shazwan.nexapanel.Bridge","com.shazwan.nexadash.next.permission.CONTROL"),
        new Target("Native test A","com.shazwan.nexadash.test.a","com.shazwan.nexadash.test.Main","com.shazwan.nexadash.test.Bridge","com.shazwan.nexadash.next.permission.CONTROL"),
        new Target("Native test B","com.shazwan.nexadash.test.b","com.shazwan.nexadash.test.Main","com.shazwan.nexadash.test.Bridge","com.shazwan.nexadash.next.permission.CONTROL")
    };
    static final Map<String,WeakReference<DashNativeFrame>> HOSTS=new HashMap<>();
    final Activity owner;final Handler main=new Handler(Looper.getMainLooper());
    final TextView status;final SurfaceView surface;final Button start,stop,choose;final EditText entry;
    final String token=UUID.randomUUID().toString();
    final String panelKey;
    Target target=TARGETS[0];volatile VirtualDisplay display;volatile IBinder remote;
    boolean active,closed,wanted,bound,proxyLaunched,protocolVerified,targetConfirmed,targetLaunchRequested,startupFinalized;volatile int generation;
    boolean touchInProgress;
    MotionEvent lastTouch;
    int requestedDisplay=-1,proxyDisplay=-1,lastObservedDisplay=-1;
    private java.util.function.IntConsumer targetListener;
    static final String STARTUP_PREFS="native_panel_startup";
    final Runnable startupTimeout=()->{if(wanted&&!targetConfirmed)fail("Native startup timed out · requested "+requestedDisplay+" · observed "+lastObservedDisplay+". No in-panel display was confirmed.");};
    final Runnable readinessProbe=this::probeTargetDisplay;
    HandlerThread inputThread;Handler input;
    final ArrayDeque<MotionEvent> events=new ArrayDeque<>();
    final Runnable drain=this::drainInput;
    ServiceConnection connection;
    private ServiceConnection connectionFor(final int run){return new ServiceConnection(){
        @Override public void onServiceConnected(ComponentName name,IBinder binder){if(run!=generation||!wanted||closed||!bound)return;remote=binder;
            input.post(()->{boolean supported=false;Parcel data=Parcel.obtain(),reply=Parcel.obtain();try{data.writeInterfaceToken(TOKEN);if(binder.transact(8,data,reply,0)){reply.readException();supported=reply.readInt()==1;}}catch(RemoteException|RuntimeException ignored){}finally{data.recycle();reply.recycle();}
                final boolean valid=supported;main.post(()->{if(run!=generation||!wanted||closed)return;if(!valid){fail("Companion protocol mismatch. Install the matching companion for this launcher.");return;}protocolVerified=true;launchIfReady();});});}
        @Override public void onServiceDisconnected(ComponentName name){if(run==generation)fail("App disconnected. Press Start to reconnect.");}
        @Override public void onBindingDied(ComponentName name){if(run==generation)fail("App updated or stopped. Press Start to reconnect.");}
        @Override public void onNullBinding(ComponentName name){if(run==generation)fail("Companion does not provide a usable input service.");}
    };}
    DashNativeFrame(Activity owner){this(owner,"standalone");}
    DashNativeFrame(Activity owner,String panel){
        super(owner);this.owner=owner;panelKey="vehicle".equals(panel)?"vehicle":"media".equals(panel)?"media":"standalone";mapsSpacious=owner.getSharedPreferences("native_panel_layout",Context.MODE_PRIVATE).getBoolean("maps_spacious_"+panelKey,true);setOrientation(VERTICAL);
        int pad=Math.max(3,Math.round(4*owner.getResources().getDisplayMetrics().density));
        status=new TextView(owner);status.setTextSize(11);status.setTextColor(0xffdcefff);status.setMaxLines(2);status.setEllipsize(android.text.TextUtils.TruncateAt.END);
        status.setText("Native app lab · install the matching patched companion, then Start. No autoplay.");addView(status,new LayoutParams(-1,-2));
        LinearLayout toolbar=new LinearLayout(owner);
        if(nextHost())target=TARGETS[3];
        choose=button(nextHost()?"Google Maps":"YouTube V58",()->{
            ArrayList<Integer> indices=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();
            for(int i=0;i<TARGETS.length;i++){
                if(nextHost()&&i<3)continue;
                if(i>=5)try{owner.getPackageManager().getApplicationInfo(TARGETS[i].pkg,0);}catch(PackageManager.NameNotFoundException absent){continue;}
                indices.add(i);labels.add(TARGETS[i].label);
            }
            new AlertDialog.Builder(owner).setTitle("Native panel companion").setItems(labels.toArray(new String[0]),(d,index)->select(indices.get(index))).setNegativeButton("Cancel",null).show();
        });
        start=button("Start",this::start);stop=button("Stop",this::stop);
        toolbar.addView(choose,new LayoutParams(0,-2,2));toolbar.addView(start,new LayoutParams(0,-2,1));toolbar.addView(stop,new LayoutParams(0,-2,1));addView(toolbar,new LayoutParams(-1,-2));
        surface=new SurfaceView(owner);surface.setContentDescription("Cooperative native app surface");surface.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->fitSurface(r-l,b-t));surface.getHolder().addCallback(this);
        addView(surface,new LayoutParams(-1,0,1));surface.setOnTouchListener((view,event)->{forward(event);return true;});
        LinearLayout tools=new LinearLayout(owner);tools.setOrientation(VERTICAL);tools.setVisibility(GONE);
        Button more=button("⋯",()->{boolean showing=tools.getVisibility()!=VISIBLE;tools.setVisibility(showing?VISIBLE:GONE);if(showing){tools.setFocusableInTouchMode(true);tools.requestFocus();}else hideEntryInput();});more.setContentDescription("Show native panel input/setup tools");toolbar.addView(more,new LayoutParams(-2,-2));
        LinearLayout keys=new LinearLayout(owner);
        keys.addView(button("Back",()->command(4,null)),new LayoutParams(0,-2,1));
        keys.addView(button("Open outside panel",()->{stop();DashShortcuts.start(owner,new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(target.pkg,target.activity)));}),new LayoutParams(0,-2,2));
        tools.addView(keys,new LayoutParams(-1,-2));
        tools.addView(button("Maps layout / permissions",()->{
            if(target!=TARGETS[3]){note("Layout options apply to Maps only.");return;}
            new AlertDialog.Builder(owner).setTitle("Maps panel setup").setItems(new String[]{"Spacious map layout (default)","Original compact layout","Maps location permission settings"},(d,index)->{
                if(index==2){stop();try{owner.startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+target.pkg)));}catch(RuntimeException denied){note("Unable to open Maps settings.");}return;}
                boolean restart=wanted;stop();mapsSpacious=index==0;owner.getSharedPreferences("native_panel_layout",Context.MODE_PRIVATE).edit().putBoolean("maps_spacious_"+panelKey,mapsSpacious).apply();fitSurface(surface.getWidth(),surface.getHeight());tools.setVisibility(GONE);if(restart)start();else note("Maps layout selected. Press Start.");
            }).setNegativeButton("Cancel",null).show();
        }),new LayoutParams(-1,-2));
        LinearLayout typing=new LinearLayout(owner);entry=new EditText(owner);entry.setSingleLine(true);entry.setTextSize(12);entry.setHint("Tap text field in app, type here");entry.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(240)});
        entry.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);
        entry.setOnEditorActionListener((v,action,event)->{if(action!=android.view.inputmethod.EditorInfo.IME_ACTION_SEND)return false;sendText();return true;});
        typing.addView(entry,new LayoutParams(0,-2,1));typing.addView(button("Send",this::sendText),new LayoutParams(-2,-2));tools.addView(typing,new LayoutParams(-1,-2));addView(tools,new LayoutParams(-1,-2));
        setPadding(pad,pad,pad,pad);HOSTS.put(token,new WeakReference<>(this));
    }
    /** Preserve the measured panel aspect ratio, with a bounded raster and usable logical UI. */
    static int[] fittedSize(int width,int height){
        return DashNativeGeometry.fittedSize(width,height,MAX_EDGE);
    }
    private void fitSurface(int width,int height){
        if(width<=0||height<=0||closed)return;
        int[] size=target==TARGETS[3]&&mapsSpacious?DashNativeGeometry.mapsSize(width,height,MAX_EDGE):fittedSize(width,height);
        if(size[0]==renderWidth&&size[1]==renderHeight)return;
        cancelTouch();
        renderWidth=size[0];renderHeight=size[1];
        try{
            surface.getHolder().setFixedSize(renderWidth,renderHeight);
            if(display!=null)display.resize(renderWidth,renderHeight,160);
        }catch(RuntimeException failure){fail("Native resize failed. Press Start to retry.");}
    }
    private Button button(String label,Runnable run){Button b=new Button(owner);b.setText(label);b.setTextSize(11);int touch=Math.round(32*owner.getResources().getDisplayMetrics().density);b.setMinHeight(touch);b.setMinimumHeight(touch);b.setMinWidth(touch);b.setMinimumWidth(touch);b.setPadding(4,2,4,2);b.setOnClickListener(v->run.run());return b;}
    private boolean nextHost(){return "com.shazwan.nexadash.next".equals(owner.getPackageName());}
    void setOnTargetSelected(java.util.function.IntConsumer listener){targetListener=listener;}
    void select(int index){stop();int actual=Math.max(0,Math.min(TARGETS.length-1,index));if(nextHost()&&actual<3)actual=3;target=TARGETS[actual];choose.setText(actual==0?"YouTube V58":actual==1||actual==4?"TizenTube":actual==3?"Google Maps":actual==5?"Test A":actual==6?"Test B":"Demo");note("Selected "+target.label+". Press Start to try this panel; Open outside panel is separate.");if(targetListener!=null)targetListener.accept(actual);}
    private boolean duplicateActiveTarget(){
        for(WeakReference<DashNativeFrame> ref:HOSTS.values()){
            DashNativeFrame other=ref.get();if(other!=null&&other!=this&&other.owner==owner&&!other.closed&&other.wanted&&other.target.pkg.equals(target.pkg))return true;
        }
        return false;
    }
    static DashNativeFrame find(String token){WeakReference<DashNativeFrame> ref=HOSTS.get(token);return ref==null?null:ref.get();}
    private void note(String message){if(!closed)status.setText(message);}
    private static String version(Context context,String pkg){try{return String.valueOf(context.getPackageManager().getPackageInfo(pkg,0).versionName);}catch(PackageManager.NameNotFoundException absent){return "not installed";}}
    private void recordStartup(String result){
        saveStartup(owner.getSharedPreferences(STARTUP_PREFS,Context.MODE_PRIVATE),result);
        saveStartup(owner.getSharedPreferences(STARTUP_PREFS+"_"+panelKey,Context.MODE_PRIVATE),result);
    }
    private void saveStartup(android.content.SharedPreferences preferences,String result){
        preferences.edit()
            .putString("result",result).putString("package",target.pkg).putString("target_version",version(owner,target.pkg))
            .putString("host_version",version(owner,owner.getPackageName())).putInt("api",Build.VERSION.SDK_INT)
            .putInt("requested_display",requestedDisplay).putInt("proxy_display",proxyDisplay).putInt("actual_display",lastObservedDisplay)
            .putInt("raster_width",renderWidth).putInt("raster_height",renderHeight).apply();
    }
    static String lastStartupResult(Context context){
        android.content.SharedPreferences left=context.getSharedPreferences(STARTUP_PREFS+"_vehicle",Context.MODE_PRIVATE),right=context.getSharedPreferences(STARTUP_PREFS+"_media",Context.MODE_PRIVATE);
        if(left.contains("result")||right.contains("result"))return "Left panel\n"+startupReport(left)+"\n\nRight panel\n"+startupReport(right);
        return startupReport(context.getSharedPreferences(STARTUP_PREFS,Context.MODE_PRIVATE));
    }
    private static String startupReport(android.content.SharedPreferences p){
        if(!p.contains("result"))return "No native-panel startup attempt recorded.";
        return "Last native-panel attempt: "+p.getString("result","")+"\nCompanion: "+p.getString("package","")+" · "+p.getString("target_version","")
            +"\nHost: "+p.getString("host_version","")+" · API "+p.getInt("api",-1)
            +"\nRequested display: "+p.getInt("requested_display",-1)+" · proxy: "+p.getInt("proxy_display",-1)+" · companion: "+p.getInt("actual_display",-1)
            +"\nRaster: "+p.getInt("raster_width",0)+"x"+p.getInt("raster_height",0)+" @160dpi";
    }
    void proxyDisplayFailed(int expected,int actual){
        if(!wanted||closed||expected!=requestedDisplay)return;
        proxyDisplay=actual;fail("Proxy display mismatch · requested "+expected+" · actual "+actual+". Native panel was not established.");
    }
    private void probeTargetDisplay(){
        final Handler worker=input;final int run=generation,expected=requestedDisplay;final IBinder binder=remote;
        if(worker==null||Looper.myLooper()!=worker.getLooper()||!wanted||binder==null||run!=generation)return;
        int observed=-1;String error=null;Parcel data=Parcel.obtain(),reply=Parcel.obtain();
        try{data.writeInterfaceToken(TOKEN);if(!binder.transact(3,data,reply,0))error="Companion display verification unavailable.";else{reply.readException();observed=reply.readInt();}}
        catch(RemoteException|RuntimeException problem){error="Companion display verification failed: "+problem.getClass().getSimpleName();}
        finally{data.recycle();reply.recycle();}
        final int actual=observed;final String problem=error;
        main.post(()->{
            if(run!=generation||!wanted||closed||targetConfirmed||expected!=requestedDisplay)return;
            if(problem!=null){fail(problem);return;}lastObservedDisplay=actual;
            if(actual==expected&&actual>0){targetConfirmed=true;startupFinalized=true;main.removeCallbacks(startupTimeout);recordStartup("confirmed");status.setMaxLines(1);
                note("Companion reported display "+actual+". Playback, input and continued visibility are not verified.");return;}
            // A previous standalone Activity can be reported while the hosted app starts slowly.
            // Keep the full startup deadline; a different display is evidence, not early proof of failure.
            if(input==worker)worker.postDelayed(readinessProbe,500);
        });
    }
    private String validate(){
        if(Build.VERSION.SDK_INT<26)return "Native hosting needs Android 8.0/API26 or newer.";
        PackageManager pm=owner.getPackageManager();
        if(!pm.hasSystemFeature("android.software.activities_on_secondary_displays"))return "Firmware does not advertise apps on secondary displays. Native panel startup was not attempted.";
        try{
            pm.getApplicationInfo(target.pkg,0);
            if(pm.checkSignatures(owner.getPackageName(),target.pkg)!=PackageManager.SIGNATURE_MATCH)return "Install a companion signed with the same key as this launcher. Original/unpatched apps cannot be used here.";
            ActivityInfo a=pm.getActivityInfo(new ComponentName(target.pkg,target.activity),0);
            ServiceInfo s=pm.getServiceInfo(new ComponentName(target.pkg,target.service),0);
            if(!a.exported||!a.enabled||(a.flags&0x80000000)==0)return "Companion activity has not opted into native display hosting.";
            if(!s.exported||!s.enabled||!target.permission.equals(s.permission))return "Matching signature-protected companion service is missing.";
            if(pm.checkPermission(target.permission,owner.getPackageName())!=PackageManager.PERMISSION_GRANTED)return "Install the companion first, then install/update this launcher again to grant its signature permission.";
        }catch(PackageManager.NameNotFoundException absent){return target.label+" companion not installed. "+(nextHost()?"Install the Maps NexaDash Next companion first.":"Use its matching Lab APK; Demo supports API26+.");}
        return null;
    }
    void start(){
        if(closed||!active){note("Return to the visible panel before starting.");return;}
        stop();requestedDisplay=-1;proxyDisplay=-1;lastObservedDisplay=-1;startupFinalized=false;
        if(duplicateActiveTarget()){fail("This app is already starting/running in the other panel. Choose a different companion; the other panel was kept running.");return;}
        String error=validate();if(error!=null){fail(error);return;}recordStartup("binding companion");
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
            display=manager.createVirtualDisplay("NexaDash "+panelKey,renderWidth,renderHeight,160,surface.getHolder().getSurface(),DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
            if(display==null)throw new IllegalStateException("No virtual display");
            int id=display.getDisplay().getDisplayId();requestedDisplay=id;proxyLaunched=true;recordStartup("waiting for proxy display");
            owner.startActivity(new Intent(owner,DashNativeProxy.class).putExtra("native_token",token).putExtra("native_display",id).putExtra("native_generation",generation)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK),ActivityOptions.makeBasic().setLaunchDisplayId(id).toBundle());
            status.setMaxLines(2);note("Requested native display "+id+" · waiting for companion verification.");
        }catch(RuntimeException failure){fail("Display launch failed: "+failure.getClass().getSimpleName());}
    }
    void launchTarget(Activity proxy,int id){
        if(!wanted||closed||display==null||display.getDisplay().getDisplayId()!=id){proxy.finish();return;}
        if(targetLaunchRequested)return;proxyDisplay=id;targetLaunchRequested=true;recordStartup("waiting for companion display");
        try{proxy.startActivity(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(target.pkg,target.activity))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_MULTIPLE_TASK),ActivityOptions.makeBasic().setLaunchDisplayId(id).toBundle());
            if(input!=null)input.postDelayed(readinessProbe,350);}
        catch(RuntimeException failure){fail("App launch rejected: "+failure.getClass().getSimpleName());proxy.finish();}
    }
    /** A resize/overflow ends the old gesture. Ignore its remaining fingers until a fresh Down. */
    private void cancelTouch(){
        synchronized(events){
            if(touchInProgress&&lastTouch!=null){MotionEvent cancel=MotionEvent.obtain(lastTouch);cancel.setAction(MotionEvent.ACTION_CANCEL);events.add(cancel);}
            touchInProgress=false;if(lastTouch!=null){lastTouch.recycle();lastTouch=null;}
        }
        Handler worker=input;if(worker!=null)worker.post(drain);
    }
    private void forward(MotionEvent event){
        if(!wanted||!targetConfirmed||display==null||remote==null||input==null)return;
        synchronized(events){
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN)touchInProgress=true;
            if(!touchInProgress)return;
            if(events.size()>8&&action==MotionEvent.ACTION_MOVE)return;
            if(events.size()>=32){
                for(MotionEvent queued:events)queued.recycle();events.clear();cancelTouch();return;
            }
            MotionEvent copy=MotionEvent.obtain(event);Matrix scale=new Matrix();scale.setScale(renderWidth/(float)Math.max(1,surface.getWidth()),renderHeight/(float)Math.max(1,surface.getHeight()));copy.transform(scale);events.add(copy);
            if(lastTouch!=null)lastTouch.recycle();lastTouch=MotionEvent.obtain(copy);
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){touchInProgress=false;lastTouch.recycle();lastTouch=null;}
        }
        input.post(drain);
    }
    private void drainInput(){
        Handler worker=input;if(worker==null||Looper.myLooper()!=worker.getLooper())return;final int run=generation;
        while(run==generation&&input==worker){MotionEvent event;synchronized(events){event=events.poll();}if(event==null)return;
            Parcel data=Parcel.obtain();try{IBinder binder=remote;if(binder!=null&&run==generation){data.writeInterfaceToken(TOKEN);VirtualDisplay current=display;if(current!=null){data.writeInt(current.getDisplay().getDisplayId());event.writeToParcel(data,0);binder.transact(6,data,null,IBinder.FLAG_ONEWAY);}}}
            catch(RemoteException|RuntimeException failure){main.post(()->{if(run==generation)fail("Input service disconnected. Press Start to reconnect.");});}
            finally{data.recycle();event.recycle();}
        }
    }
    private void hideEntryInput(){if(entry==null)return;android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)owner.getSystemService(Context.INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(entry.getWindowToken(),0);entry.clearFocus();}
    private void sendText(){
        if(!wanted||!targetConfirmed){note("Start and confirm the companion before sending text.");return;}
        command(5,entry.getText().toString());
        android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)owner.getSystemService(Context.INPUT_METHOD_SERVICE);
        if(keyboard!=null)keyboard.hideSoftInputFromWindow(entry.getWindowToken(),0);entry.clearFocus();
    }
    private void command(int code,String text){
        Handler worker=input;IBinder binder=remote;if(worker==null||binder==null||display==null||!wanted||!targetConfirmed){note("Start and confirm a companion first.");return;}
        final int run=generation,id=display.getDisplay().getDisplayId();
        worker.post(()->{if(run!=generation||input!=worker||remote!=binder)return;Parcel data=Parcel.obtain();try{data.writeInterfaceToken(TOKEN);data.writeInt(id);if(code==5)data.writeString(text);binder.transact(code,data,null,IBinder.FLAG_ONEWAY);}
            catch(RemoteException|RuntimeException failure){main.post(()->{if(generation==run)fail("Companion input service disconnected.");});}finally{data.recycle();}});
    }
    private void fail(String message){startupFinalized=true;recordStartup("failed: "+message);stop();note(message);}
    void stop(){
        if(wanted&&!targetConfirmed&&!startupFinalized){startupFinalized=true;recordStartup("stopped before companion display was confirmed");}
        if(entry.hasFocus()){android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)owner.getSystemService(Context.INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(entry.getWindowToken(),0);entry.clearFocus();}
        wanted=false;targetConfirmed=false;targetLaunchRequested=false;proxyLaunched=false;protocolVerified=false;main.removeCallbacks(startupTimeout);generation++;remote=null;
        if(bound){bound=false;try{owner.unbindService(connection);}catch(IllegalArgumentException ignored){}}
        if(display!=null){display.release();display=null;}
        synchronized(events){for(MotionEvent event:events)event.recycle();events.clear();touchInProgress=false;if(lastTouch!=null){lastTouch.recycle();lastTouch=null;}}
        if(input!=null)input.removeCallbacksAndMessages(null);input=null;
        if(inputThread!=null)inputThread.quit();inputThread=null;
        status.setMaxLines(2);note("Stopped · native display released and input stop requested. Press Start to reopen.");
    }
    void resume(){active=true;}
    void pause(){active=false;stop();}
    void close(){pause();closed=true;HOSTS.remove(token);}
    @Override public void surfaceCreated(SurfaceHolder h){launchIfReady();}
    @Override public void surfaceChanged(SurfaceHolder h,int format,int width,int height){}
    @Override public void surfaceDestroyed(SurfaceHolder h){stop();}
    @Override protected void onDetachedFromWindow(){pause();super.onDetachedFromWindow();}
}
