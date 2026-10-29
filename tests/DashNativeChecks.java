package com.shazwan.sagadash;

import android.app.*;import android.content.*;import android.os.*;import android.view.*;
import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicInteger;
/** Real Demo render/input checks and isolated synthetic startup-state checks; no playback claim. */
final class DashNativeChecks {
    int assertions;
    void check(boolean ok,String message){if(!ok)throw new AssertionError(message);assertions++;}
    interface Ready{boolean get();}
    void await(Instrumentation i,Ready test,String message)throws Exception{
        long end=SystemClock.elapsedRealtime()+30000;boolean[] yes={false};while(!yes[0]&&SystemClock.elapsedRealtime()<end){DashAppsChecks.onMain(i,()->yes[0]=test.get());if(!yes[0])SystemClock.sleep(100);}check(yes[0],message);
    }
    int query(IBinder binder,int code)throws Exception{Parcel a=Parcel.obtain(),b=Parcel.obtain();try{a.writeInterfaceToken(DashNativeFrame.TOKEN);binder.transact(code,a,b,0);b.readException();return b.readInt();}finally{a.recycle();b.recycle();}}
    String queryText(IBinder binder)throws Exception{Parcel a=Parcel.obtain(),b=Parcel.obtain();try{a.writeInterfaceToken(DashNativeFrame.TOKEN);binder.transact(7,a,b,0);b.readException();return b.readString();}finally{a.recycle();b.recycle();}}
    android.widget.Button button(android.view.View root,String text){if(root instanceof android.widget.Button&&text.contentEquals(((android.widget.Button)root).getText()))return (android.widget.Button)root;if(root instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)root;for(int n=0;n<group.getChildCount();n++){android.widget.Button b=button(group.getChildAt(n),text);if(b!=null)return b;}}return null;}
    /** In-process reply fixtures exercise host state only; they are not display-hosting evidence. */
    static final class ProbeBinder extends Binder {
        final int display;final AtomicInteger calls=new AtomicInteger();final CountDownLatch reached,release;
        volatile boolean timedOut;
        ProbeBinder(int display,int readings,boolean blocked){this.display=display;reached=new CountDownLatch(readings);release=new CountDownLatch(blocked?1:0);}
        @Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
            if(code!=3)return super.onTransact(code,data,reply,flags);
            data.enforceInterface(DashNativeFrame.TOKEN);calls.incrementAndGet();reached.countDown();
            try{if(!release.await(10,TimeUnit.SECONDS)){timedOut=true;throw new RemoteException("Synthetic probe was not released");}}
            catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new RemoteException("Synthetic probe interrupted");}
            reply.writeNoException();reply.writeInt(display);return true;
        }
    }
    DashNativeFrame probeFrame(DashboardActivity dash,ProbeBinder binder){
        DashNativeFrame frame=new DashNativeFrame(dash);frame.target=DashNativeFrame.TARGETS[2];
        frame.requestedDisplay=17;frame.proxyDisplay=17;frame.wanted=true;frame.generation++;
        frame.remote=binder;frame.inputThread=new HandlerThread("Nexa-native-synthetic-check");frame.inputThread.start();frame.input=new Handler(frame.inputThread.getLooper());
        frame.main.postDelayed(frame.startupTimeout,60000);return frame;
    }
    void probeStateChecks(Instrumentation i,DashboardActivity dash)throws Exception{
        final DashNativeFrame[] frames=new DashNativeFrame[3];final HandlerThread[] workers=new HandlerThread[3];
        ProbeBinder pending=new ProbeBinder(0,3,false),success=new ProbeBinder(17,1,false),late=new ProbeBinder(17,1,true);
        try{
            DashAppsChecks.onMain(i,()->{frames[0]=probeFrame(dash,pending);workers[0]=frames[0].inputThread;frames[0].input.post(frames[0].readinessProbe);});
            check(pending.reached.await(5,TimeUnit.SECONDS),"synthetic primary-display replies are repeatedly observed during startup");
            DashAppsChecks.onMain(i,()->{
                DashNativeFrame f=frames[0];check(f.wanted&&!f.targetConfirmed&&f.lastObservedDisplay==0&&f.input!=null,"synthetic display 0 stays pending rather than confirming or failing early");
                f.startupTimeout.run();SharedPreferences p=dash.getSharedPreferences(DashNativeFrame.STARTUP_PREFS,Context.MODE_PRIVATE);
                check(!f.wanted&&f.remote==null&&f.inputThread==null,"synthetic deadline stops a pending startup without waiting sixty seconds");
                check(p.getString("result","").contains("timed out")&&p.getInt("requested_display",-1)==17&&p.getInt("actual_display",-1)==0,"synthetic timeout persists requested and last observed displays");
            });
            DashAppsChecks.onMain(i,()->{frames[1]=probeFrame(dash,success);workers[1]=frames[1].inputThread;frames[1].input.post(frames[1].readinessProbe);});
            await(i,()->frames[1].targetConfirmed,"synthetic matching reply confirms the host startup state");
            CountDownLatch settled=new CountDownLatch(1);
            DashAppsChecks.onMain(i,()->frames[1].input.postDelayed(settled::countDown,1000));
            check(settled.await(5,TimeUnit.SECONDS),"synthetic input worker remains responsive after confirmation");
            check(success.calls.get()==1,"confirmed synthetic startup schedules no further display probes");
            DashAppsChecks.onMain(i,()->{
                SharedPreferences p=dash.getSharedPreferences(DashNativeFrame.STARTUP_PREFS,Context.MODE_PRIVATE);Map<String,?> confirmed=new HashMap<>(p.getAll());
                frames[1].stop();check(confirmed.equals(p.getAll()),"Stop preserves a confirmed startup diagnostic");
            });
            DashAppsChecks.onMain(i,()->{frames[2]=probeFrame(dash,late);workers[2]=frames[2].inputThread;frames[2].input.post(frames[2].readinessProbe);});
            check(late.reached.await(5,TimeUnit.SECONDS),"synthetic delayed reply starts before Stop");
            final String[] stoppedStatus={null};final Map<String,Object> stoppedRecord=new HashMap<>();
            DashAppsChecks.onMain(i,()->{frames[2].stop();stoppedStatus[0]=frames[2].status.getText().toString();stoppedRecord.putAll(dash.getSharedPreferences(DashNativeFrame.STARTUP_PREFS,Context.MODE_PRIVATE).getAll());});
            late.release.countDown();workers[2].join(5000);check(!workers[2].isAlive()&&!late.timedOut,"synthetic in-flight reply returns and its stopped worker exits");i.waitForIdleSync();
            DashAppsChecks.onMain(i,()->{
                check(!frames[2].targetConfirmed&&!frames[2].wanted&&stoppedStatus[0].contentEquals(frames[2].status.getText()),"a reply returned after Stop cannot confirm or replace stopped status");
                check(stoppedRecord.equals(dash.getSharedPreferences(DashNativeFrame.STARTUP_PREFS,Context.MODE_PRIVATE).getAll()),"a reply returned after Stop cannot overwrite the cancelled diagnostic");
            });
        }finally{
            pending.release.countDown();success.release.countDown();late.release.countDown();
            DashAppsChecks.onMain(i,()->{for(DashNativeFrame f:frames)if(f!=null)f.close();});
            for(HandlerThread worker:workers)if(worker!=null){worker.join(5000);check(!worker.isAlive(),"synthetic probe worker is fully stopped after its check");}
        }
    }
    @SuppressWarnings("unchecked")
    void restore(SharedPreferences preferences,Map<String,?> saved,boolean clear){
        SharedPreferences.Editor edit=preferences.edit();if(clear)edit.clear();
        for(Map.Entry<String,?> entry:saved.entrySet()){
            String key=entry.getKey();Object value=entry.getValue();
            if(value==null)edit.remove(key);else if(value instanceof String)edit.putString(key,(String)value);else if(value instanceof Integer)edit.putInt(key,(Integer)value);
            else if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);else if(value instanceof Long)edit.putLong(key,(Long)value);else if(value instanceof Float)edit.putFloat(key,(Float)value);else edit.putStringSet(key,(Set<String>)value);
        }
        edit.commit();
    }
    int run(Instrumentation i,DashboardActivity dash)throws Exception{
        final DashNativeFrame[] frame={null};final IBinder[] bridge={null};String left=dash.leftPane.mode,right=dash.rightPane.mode;
        SharedPreferences preferences=dash.dashPrefs(),diagnostics=dash.getSharedPreferences(DashNativeFrame.STARTUP_PREFS,Context.MODE_PRIVATE);
        Map<String,Object> savedTargets=new HashMap<>();for(String key:new String[]{"native_target_vehicle","native_target_media"})savedTargets.put(key,preferences.getAll().get(key));
        Map<String,?> savedDiagnostics=new HashMap<>(diagnostics.getAll());final List<HandlerThread> workers=new ArrayList<>();
        try{
            probeStateChecks(i,dash);
            DashAppsChecks.onMain(i,()->{
                dash.leftPane.setMode("native_app");frame[0]=dash.leftPane.nativeFrame;
                check(frame[0]!=null&&frame[0].display==null&&frame[0].remote==null&&frame[0].inputThread==null,"selecting native panel creates no display, binding or worker before explicit Start");
                frame[0].select(2);frame[0].start();if(frame[0].inputThread!=null)workers.add(frame[0].inputThread);
            });
            await(i,()->frame[0].display!=null&&frame[0].remote!=null,"native panel binds and creates its own real private display");
            DashAppsChecks.onMain(i,()->bridge[0]=frame[0].remote);
            int id=frame[0].display.getDisplay().getDisplayId();
            check(id>0,"native target uses a non-primary display");
            long end=SystemClock.elapsedRealtime()+30000;int actual=-1;while(actual!=id&&SystemClock.elapsedRealtime()<end){actual=query(bridge[0],3);if(actual!=id)SystemClock.sleep(100);}
            check(actual==id,"separate companion Activity really runs on the requested native panel display");
            await(i,()->frame[0].targetConfirmed,"host confirms the real Demo companion's reported startup display");
            DashAppsChecks.onMain(i,()->{
                check("confirmed".equals(diagnostics.getString("result",""))&&DashNativeFrame.TARGETS[2].pkg.equals(diagnostics.getString("package","")),"real Demo confirmation persists its result and companion package");
                check(diagnostics.getInt("requested_display",-1)==id&&diagnostics.getInt("proxy_display",-1)==id&&diagnostics.getInt("actual_display",-1)==id,"real Demo diagnostic records matching requested, proxy and companion displays");
            });
            check(dash.getPackageManager().getApplicationInfo("com.shazwan.embedprobe.target",0).uid!=dash.getApplicationInfo().uid,"host and companion have different UIDs");
            await(i,()->frame[0].surface.getWidth()>0&&frame[0].surface.getHeight()>0,"native surface has measurable panel bounds");
            check(Math.abs(frame[0].renderWidth/(double)frame[0].renderHeight-frame[0].surface.getWidth()/(double)frame[0].surface.getHeight())<.01,"native raster preserves the measured panel aspect ratio");
            check(Math.max(frame[0].renderWidth,frame[0].renderHeight)<=800,"native raster longest edge is bounded to800 pixels");
            int before=query(bridge[0],2);final long down=SystemClock.uptimeMillis();
            for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){
                DashAppsChecks.onMain(i,()->{MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,frame[0].surface.getWidth()*Math.min(300f,frame[0].renderWidth/2f)/frame[0].renderWidth,frame[0].surface.getHeight()*105f/frame[0].renderHeight,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);frame[0].surface.dispatchTouchEvent(event);event.recycle();});SystemClock.sleep(80);
            }
            end=SystemClock.elapsedRealtime()+15000;int after=before;while(after==before&&SystemClock.elapsedRealtime()<end){after=query(bridge[0],2);if(after==before)SystemClock.sleep(100);}
            check(after==before+1,"panel-scaled native touch increments the separate companion's real UI counter");
            android.graphics.Bitmap snapshot=i.getUiAutomation().takeScreenshot();
            if(snapshot!=null){java.io.File image=new java.io.File(i.getTargetContext().getExternalFilesDir(null),"native-lab-demo-left.png");try(java.io.FileOutputStream out=new java.io.FileOutputStream(image)){snapshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{snapshot.recycle();}}
            DashAppsChecks.onMain(i,()->button(frame[0],"⋯").performClick());i.waitForIdleSync();
            await(i,()->frame[0].display!=null&&frame[0].renderHeight>=1,"native display remains available after expanding input tools");
            SystemClock.sleep(1000); // Allow the target Activity's configuration change before focusing its editor.
            final long textDown=SystemClock.uptimeMillis();
            for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){DashAppsChecks.onMain(i,()->{MotionEvent event=MotionEvent.obtain(textDown,SystemClock.uptimeMillis(),action,frame[0].surface.getWidth()*Math.min(300f,frame[0].renderWidth/2f)/frame[0].renderWidth,frame[0].surface.getHeight()*161f/frame[0].renderHeight,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);frame[0].surface.dispatchTouchEvent(event);event.recycle();});SystemClock.sleep(80);}
            DashAppsChecks.onMain(i,()->{frame[0].entry.setText("Nexa native text");button(frame[0],"Send").performClick();});
            end=SystemClock.elapsedRealtime()+15000;String text="";while(!"Nexa native text".equals(text)&&SystemClock.elapsedRealtime()<end){text=queryText(bridge[0]);if(!"Nexa native text".equals(text))SystemClock.sleep(100);}
            check("Nexa native text".equals(text),"host text proxy commits into the focused EditText of the separate companion");
            DashAppsChecks.onMain(i,()->{
                Map<String,?> confirmed=new HashMap<>(diagnostics.getAll());frame[0].stop();
                check(frame[0].display==null&&frame[0].remote==null&&!frame[0].bound&&frame[0].inputThread==null,"Stop releases display, input thread and service binding");
                check("confirmed".equals(confirmed.get("result"))&&confirmed.equals(diagnostics.getAll()),"Stop preserves the real Demo startup confirmation and display IDs");
                frame[0].resume();check(!frame[0].wanted&&frame[0].display==null,"resume does not auto-launch the native app");
            });
            DashAppsChecks.onMain(i,()->{dash.rightPane.setMode("native_app");check(dash.leftPane.nativeFrame==null&&"car".equals(dash.leftPane.mode),"selecting native on the other panel releases the previous inline/native panel");frame[0]=dash.rightPane.nativeFrame;frame[0].select(2);frame[0].start();if(frame[0].inputThread!=null)workers.add(frame[0].inputThread);});
            await(i,()->frame[0].display!=null,"native companion can start in the right panel too");
            await(i,()->frame[0].targetConfirmed,"right native panel confirms the real Demo startup display too");
            DashAppsChecks.onMain(i,()->{
                int requested=frame[0].requestedDisplay;frame[0].proxyDisplayFailed(requested,0);
                check(!frame[0].wanted&&frame[0].display==null&&frame[0].remote==null&&!frame[0].bound&&frame[0].inputThread==null,"synthetically injected wrong-proxy failure releases the real Demo panel's resources");
                check(diagnostics.getString("result","").contains("Proxy display mismatch")&&diagnostics.getInt("requested_display",-1)==requested&&diagnostics.getInt("proxy_display",-1)==0,"injected wrong-proxy failure persists its requested and observed display evidence");
                frame[0].pause();check(frame[0].display==null&&!frame[0].wanted&&frame[0].inputThread==null,"pausing a failed native panel keeps its resources released");
            });
        }finally{
            try{
                DashAppsChecks.onMain(i,()->{restore(preferences,savedTargets,false);dash.leftPane.setMode(left);dash.rightPane.setMode(right);});
                for(HandlerThread worker:workers){worker.join(5000);check(!worker.isAlive(),"real Demo input worker exits after release");}
                DashAppsChecks.onMain(i,()->dash.startActivity(new Intent(dash,DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));
                i.waitForIdleSync();
            }finally{DashAppsChecks.onMain(i,()->restore(diagnostics,savedDiagnostics,true));}
        }
        return assertions;
    }
}
