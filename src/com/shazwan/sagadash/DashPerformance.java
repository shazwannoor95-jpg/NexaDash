package com.shazwan.sagadash;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.function.*;

/** Two own-process snapshots on demand. No service, permanent timer, forced GC or frame hook. */
final class DashPerformance {
    static final long WINDOW_MS=30000;
    private final Activity owner;
    private final Supplier<String> scenario;
    private final BooleanSupplier visible;
    private final Handler main=new Handler(Looper.getMainLooper());
    private ExecutorService worker;
    private AlertDialog dialog;
    private TextView reportView;
    private boolean running;
    private volatile int generation;
    private Runnable finishSample;
    private static final String PREFS="performance_check";

    DashPerformance(Activity owner,Supplier<String> scenario,BooleanSupplier visible){this.owner=owner;this.scenario=scenario;this.visible=visible;}
    static double cpuPercent(long startCpu,long endCpu,long startElapsed,long endElapsed){
        if(startCpu<0||endCpu<startCpu||startElapsed<0||endElapsed<=startElapsed)return Double.NaN;
        return (double)(endCpu-startCpu)*100.0/(endElapsed-startElapsed);
    }
    private android.content.SharedPreferences prefs(){return owner.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    String report(){return "Status: "+prefs().getString("status","No sample yet")+"\n\nLast completed sample:\n"+prefs().getString("report","Start a 30-second sample, then use this dashboard normally. Reopen Performance check to read the result.\n\nOnly this process is measured. WebView renderer and other apps are excluded.");}
    void show(){
        if(owner.isFinishing()||owner.isDestroyed()||!visible.getAsBoolean()||(dialog!=null&&dialog.isShowing()))return;
        TextView text=new TextView(owner);text.setText(report());text.setTextIsSelectable(true);text.setTextSize(14);int p=Math.round(16*owner.getResources().getDisplayMetrics().density);text.setPadding(p,p,p,p);
        ScrollView scroll=new ScrollView(owner);scroll.addView(text);
        AlertDialog shown=new AlertDialog.Builder(owner).setTitle("Performance check · own process")
            .setView(scroll).setPositiveButton(running?"Cancel sample":"Start 30 seconds",(d,w)->{if(running)cancel();else start();})
            .setNeutralButton("Copy report",(d,w)->{ClipboardManager c=(ClipboardManager)owner.getSystemService(Context.CLIPBOARD_SERVICE);if(c!=null)c.setPrimaryClip(ClipData.newPlainText("NexaDash performance",text.getText()));})
            .setNegativeButton("Close",null).create();
        dialog=shown;reportView=text;shown.setOnDismissListener(d->{if(dialog==shown){dialog=null;reportView=null;}});shown.show();
    }
    private void refreshDialog(){
        if(dialog!=null&&dialog.isShowing()&&reportView!=null){
            reportView.setText(report());dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(running?"Cancel sample":"Start 30 seconds");
        }
    }
    void start(){
        cancel();if(!visible.getAsBoolean()||owner.isDestroyed())return;
        running=true;final int run=++generation;final String before=scenario.get();
        prefs().edit().putString("status","Sampling for 30 seconds; keep dashboard visible").apply();
        ExecutorService created=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Nexa-perf");t.setDaemon(true);return t;});worker=created;
        Toast.makeText(owner,"Performance sample started. Use dashboard, then reopen this setting.",Toast.LENGTH_LONG).show();
        created.execute(()->{
            try{final Snapshot first=snapshot();main.post(()->{
                if(run!=generation||!running)return;
                finishSample=()->{
                    if(run!=generation||!running)return;final String after=scenario.get();
                    try{created.execute(()->{try{final Snapshot last=snapshot();final String result=format(first,last,before,after);main.post(()->{
                        if(run!=generation||!running)return;
                        running=false;finishSample=null;worker=null;created.shutdown();
                        prefs().edit().putString("status","Complete").putString("report",result).apply();refreshDialog();
                        if(visible.getAsBoolean()&&!owner.isFinishing()&&!owner.isDestroyed())Toast.makeText(owner,"Performance saved. Settings → Performance check.",Toast.LENGTH_LONG).show();
                    });}catch(RuntimeException failure){failed(run,created,failure);}});}catch(RejectedExecutionException stopped){/* cancelled */}
                };
                main.postDelayed(finishSample,WINDOW_MS);
            });}catch(RuntimeException failure){failed(run,created,failure);}
        });
    }
    private void failed(int run,ExecutorService executor,RuntimeException failure){
        executor.shutdown();main.post(()->{if(run==generation&&running){running=false;worker=null;finishSample=null;prefs().edit().putString("status","Sample unavailable: "+failure.getClass().getSimpleName()).apply();refreshDialog();}});
    }
    void cancel(){
        generation++;if(finishSample!=null)main.removeCallbacks(finishSample);finishSample=null;
        if(running)prefs().edit().putString("status","Cancelled before completion; no new measurement").apply();
        running=false;if(worker!=null)worker.shutdownNow();worker=null;
    }
    void pause(){cancel();if(dialog!=null){dialog.dismiss();dialog=null;}}
    void close(){pause();main.removeCallbacksAndMessages(null);}
    private static final class Snapshot {
        long elapsed,cpu,javaBytes,nativeBytes,totalRam,availableRam;int pssKb;boolean lowMemory;
    }
    private Snapshot snapshot(){
        Snapshot s=new Snapshot();Debug.MemoryInfo process=new Debug.MemoryInfo();Debug.getMemoryInfo(process);s.pssKb=process.getTotalPss();
        Runtime rt=Runtime.getRuntime();s.javaBytes=rt.totalMemory()-rt.freeMemory();s.nativeBytes=Debug.getNativeHeapAllocatedSize();
        ActivityManager manager=(ActivityManager)owner.getSystemService(Context.ACTIVITY_SERVICE);if(manager!=null){ActivityManager.MemoryInfo device=new ActivityManager.MemoryInfo();manager.getMemoryInfo(device);s.totalRam=device.totalMem;s.availableRam=device.availMem;s.lowMemory=device.lowMemory;}
        s.cpu=android.os.Process.getElapsedCpuTime();s.elapsed=SystemClock.elapsedRealtime();return s;
    }
    private String format(Snapshot first,Snapshot last,String before,String after){
        double cpu=cpuPercent(first.cpu,last.cpu,first.elapsed,last.elapsed);
        String version="unknown";try{version=owner.getPackageManager().getPackageInfo(owner.getPackageName(),0).versionName;}catch(android.content.pm.PackageManager.NameNotFoundException ignored){}
        return "NEXADASH PERFORMANCE · own process\nApp: "+owner.getPackageName()+" · "+version+"\nAPI: "+Build.VERSION.SDK_INT
            +"\nStart panels: "+before+"\nEnd panels: "+after
            +String.format(Locale.US,"\nObserved window: %.2f seconds\nCPU: %s%% of one CPU core\nCPU time delta: %d ms\nPSS: %.2f → %.2f MiB\nJava heap used: %.2f → %.2f MiB\nNative heap allocated: %.2f → %.2f MiB\nDevice RAM total: %.2f MiB\nDevice RAM available: %.2f → %.2f MiB\nDevice low-memory flag at end: %s\n",(last.elapsed-first.elapsed)/1000.0,Double.isNaN(cpu)?"unavailable":String.format(Locale.US,"%.2f",cpu),last.cpu-first.cpu,first.pssKb/1024.0,last.pssKb/1024.0,first.javaBytes/1048576.0,last.javaBytes/1048576.0,first.nativeBytes/1048576.0,last.nativeBytes/1048576.0,last.totalRam/1048576.0,first.availableRam/1048576.0,last.availableRam/1048576.0,last.lowMemory)
            +"\nCPU is one-core equivalent and may exceed 100% with parallel work; not a percentage of all device capacity.\nPSS includes this process only, not WebView renderer, native companions, Maps or other app processes. Heap figures overlap PSS and must not be added to it. Device available RAM includes reclaimable memory.\nOnly two memory snapshots; no peak-memory, FPS, jank or crash certification. Sampling overhead is included. No forced GC or continuous monitoring. No accounts, coordinates, URLs or serial numbers recorded.";
    }
}
