package com.shazwan.sagadash;

import android.content.*;
import android.content.pm.*;
import android.os.*;

/** Native FM controls using only the explicit service protocol recovered from this firmware. */
final class DashFmController {
    static final String PREFS="fm_panel",CONTROL="com.android.xygala.ACTION_CONTROL_RADIO";
    static final String SCAN_RESULT="android.intent.action.fmradioseekok",MAIN_UPDATE="com.acloud.intent.android.MAINUI_UPDATE";
    static final ComponentName INTERNAL=new ComponentName("com.android.fmradio","com.android.fmradio.FmtestService");
    static final ComponentName EXTERNAL=new ComponentName("com.acloud.stub.extradio","com.radio.service.RadioService");
    private final Context context;
    private final android.content.SharedPreferences prefs;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable changed;
    private boolean registered,closed;
    boolean owned,initializing;
    int frequency;
    int[] stations=new int[0];
    String sourceMode,message="Select a frequency, then Start FM.",phase="idle",lastCommand="none";
    ComponentName component;
    private final Runnable deadline=()->{if(owned&&initializing)fail("FM initialization did not respond. Retry or check Radio report.");};
    private final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent intent){receive(intent);}};

    DashFmController(Context context,Runnable changed){
        this.context=context;this.changed=changed;prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        try{frequency=prefs.getInt("frequency_khz",DashFmBand.MIN);}catch(ClassCastException bad){frequency=DashFmBand.MIN;}
        if(!DashFmBand.valid(frequency))frequency=DashFmBand.MIN;
        try{sourceMode=prefs.getString("source","auto");}catch(ClassCastException bad){sourceMode="auto";}
        if(!"internal".equals(sourceMode)&&!"external".equals(sourceMode))sourceMode="auto";
    }
    String sourceLabel(){return "internal".equals(sourceMode)?"Built-in FM":"external".equals(sourceMode)?"External FM":"Auto";}
    void source(String value){
        if(closed||!("auto".equals(value)||"internal".equals(value)||"external".equals(value)))return;
        stop();sourceMode=value;component=null;stations=new int[0];prefs.edit().putString("source",value).apply();
        update("idle","FM source selected. Press Start FM.");
    }
    private ComponentName resolve(){
        if("internal".equals(sourceMode))return INTERNAL;
        if("external".equals(sourceMode))return EXTERNAL;
        final String selected;
        try{
            Class<?> properties=Class.forName("android.os.SystemProperties");
            String value=(String)properties.getMethod("get",String.class,String.class).invoke(null,DashRadioBackend.PROPERTY,"0");
            selected=DashRadioBackend.component(value);
        }catch(ReflectiveOperationException|RuntimeException|LinkageError denied){throw new IllegalStateException("Choose an FM source using the Auto button.");}
        if(selected==null)throw new IllegalStateException("FM source could not be read. Choose a source manually.");
        return DashRadioBackend.INTERNAL.equals(selected)?INTERNAL:EXTERNAL;
    }
    private void validate(ComponentName target)throws PackageManager.NameNotFoundException{
        PackageManager pm=context.getPackageManager();ServiceInfo service=pm.getServiceInfo(target,0);
        if(!service.exported||!service.enabled||service.applicationInfo==null||!service.applicationInfo.enabled)throw new IllegalStateException("The FM service is disabled or unavailable.");
        if(service.permission!=null&&pm.checkPermission(service.permission,context.getPackageName())!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("FM service permission denied");
        String version=pm.getPackageInfo(target.getPackageName(),0).versionName;
        if(!(INTERNAL.equals(target)?"3.1":"2.3.24").equals(version))throw new IllegalStateException("This FM firmware version is not supported by this adapter.");
    }
    void start(){
        if(closed||owned)return;
        try{
            component=resolve();validate(component);
            IntentFilter filter=new IntentFilter(SCAN_RESULT);filter.addAction(MAIN_UPDATE);
            if(Build.VERSION.SDK_INT>=33)context.registerReceiver(receiver,filter,Context.RECEIVER_EXPORTED);else context.registerReceiver(receiver,filter);
            registered=true;owned=true;initializing=INTERNAL.equals(component);stations=new int[0];
            if(initializing){
                update("initializing","Starting FM · waiting for station initialization…");
                // flag0 is the only exposed device-open path. flag1 alone silently does nothing on a cold service.
                send(new Intent().putExtra("flag",0));main.postDelayed(deadline,60000);
            }else{
                method("method_open");
                // Keep the firmware's existing band/area/sensitivity configuration. Do not guess a band identifier.
                tune();
            }
        }catch(PackageManager.NameNotFoundException absent){fail("Compatible FM service is not installed on this device.");}
        catch(SecurityException denied){fail("FM service access was denied. Check Radio report.");}
        catch(RuntimeException failure){fail(failure instanceof IllegalStateException&&failure.getMessage()!=null?failure.getMessage():"FM could not start. Check Radio report.");}
    }
    void frequency(int khz){
        if(closed||!DashFmBand.valid(khz))return;
        frequency=khz;prefs.edit().putInt("frequency_khz",khz).apply();
        if(owned&&!initializing){try{tune();}catch(RuntimeException failure){fail("FM tune request failed. Check Radio report.");}}
        else if(!owned)update("idle","Frequency selected. Press Start FM.");
        else changed.run();
    }
    private void tune(){
        if(INTERNAL.equals(component))send(new Intent().putExtra("flag",1).putExtra("freq",frequency/1000f));
        else send(new Intent(CONTROL).putExtra("method","method_setFreq").putExtra("param_freq",frequency));
        update("tune_requested","Tune requested · "+DashFmBand.text(frequency)+" MHz");
    }
    private void method(String value){send(new Intent(CONTROL).putExtra("method",value));}
    private void send(Intent intent){
        intent.setComponent(component);
        ComponentName accepted=context.startService(intent);
        if(accepted==null||!component.equals(accepted))throw new IllegalStateException("FM service did not accept the request.");
        lastCommand=INTERNAL.equals(component)?"flag="+intent.getIntExtra("flag",-1)+(intent.hasExtra("freq")?"; MHz="+intent.getFloatExtra("freq",0):""):intent.getStringExtra("method")+(intent.hasExtra("param_freq")?"; kHz="+intent.getIntExtra("param_freq",0):"");
    }
    void receive(Intent intent){
        if(closed||!owned||intent==null)return;
        if(INTERNAL.equals(component)&&initializing&&SCAN_RESULT.equals(intent.getAction())&&intent.hasExtra("freq")){
            final int[] found;
            // Decode global/untrusted extras separately from command failures.
            try{found=DashFmBand.scanStations(intent.getIntArrayExtra("freq"));}catch(RuntimeException malformed){return;}
            stations=found;initializing=false;main.removeCallbacks(deadline);
            try{tune();}catch(RuntimeException failure){fail("FM initialization returned, but the tune request failed. Check Radio report.");}
        }else if(EXTERNAL.equals(component)&&MAIN_UPDATE.equals(intent.getAction())){
            final String[] extra;try{extra=intent.getStringArrayExtra("extras");}catch(RuntimeException malformed){return;}
            if(extra!=null&&extra.length==2&&"local_radio_state".equals(extra[0])){
                if("1".equals(extra[1]))update("service_response","Radio service reported on · requested "+DashFmBand.text(frequency)+" MHz");
                else if("0".equals(extra[1]))update("service_response","Radio service reported off. Stop then Start FM to reconnect.");
            }
        }
    }
    void stop(){
        boolean requested=owned;owned=false;initializing=false;main.removeCallbacks(deadline);releaseReceiver();
        if(!requested)return;
        try{closeHardwareRequest();update("stop_requested","Stop requested. Press Start FM to reopen.");}
        catch(RuntimeException failure){update("error","FM stop request was rejected. Check the original Radio app.");}
    }
    private void closeHardwareRequest(){
        if(INTERNAL.equals(component))send(new Intent().putExtra("flag",2));else if(EXTERNAL.equals(component))method("method_close");
        // Never stopService: OEM onDestroy kills its own process and can affect other OEM components.
    }
    private void fail(String reason){
        boolean requested=owned;owned=false;initializing=false;main.removeCallbacks(deadline);releaseReceiver();
        String failedCommand=lastCommand;
        if(requested)try{closeHardwareRequest();}catch(RuntimeException ignored){}
        lastCommand=failedCommand;
        update("error",reason);
    }
    private void releaseReceiver(){if(registered){registered=false;try{context.unregisterReceiver(receiver);}catch(IllegalArgumentException ignored){}}}
    private void update(String stage,String text){
        phase=stage;message=text;
        prefs.edit().putString("last_phase",stage).putString("last_message",text).putString("last_component",component==null?"not selected":component.flattenToShortString()).putString("last_command",lastCommand).putInt("last_frequency_khz",frequency).apply();
        changed.run();
    }
    void close(){if(closed)return;stop();closed=true;main.removeCallbacksAndMessages(null);releaseReceiver();}
    static String report(Context context){
        android.content.SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        if(!p.contains("last_phase"))return "No in-panel FM request recorded.";
        return "FM panel stage: "+p.getString("last_phase","")+"\nComponent: "+p.getString("last_component","")+"\nLast command: "+p.getString("last_command","")+"\nRequested kHz: "+p.getInt("last_frequency_khz",0)+"\nResult: "+p.getString("last_message","")+"\nService acceptance/state broadcasts do not verify tuner hardware or audible playback.";
    }
}
