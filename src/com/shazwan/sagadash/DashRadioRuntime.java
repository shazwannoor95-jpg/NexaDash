package com.shazwan.sagadash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BooleanSupplier;

/** Passive path visibility only. Never loads OEM classes/libraries, binds services or controls the tuner. */
final class DashRadioRuntime {
    private static final String[] PACKAGES={"com.acloud.stub.localradio","com.acloud.stub.extradio","com.android.fmradio"};
    private static final String[] LIBRARIES={"libRadioInterface.so","libfmjni.so"};
    private static final String[] SYSTEM_DIRS={"/system/lib","/vendor/lib","/system/vendor/lib","/odm/lib"};
    private static final int MAX_OAT_FILES=16,MAX_SHARED_LIBRARIES=16;
    private volatile int generation;
    private boolean loading;
    private Future<?> pending;
    private AlertDialog dialog;

    void show(Activity owner,ExecutorService worker){
        if(loading||owner.isFinishing()||owner.isDestroyed()||(dialog!=null&&dialog.isShowing()))return;
        loading=true;final int request=++generation;
        Toast.makeText(owner,"Reading Radio runtime paths…",Toast.LENGTH_SHORT).show();
        try{pending=worker.submit(()->{
            String result;
            try{result=collect(owner,()->request!=generation);}catch(CancellationException cancelled){return;}
            catch(RuntimeException failure){result="Radio path check unavailable: "+failure.getClass().getSimpleName()+"\nNo tuner command or app launch was performed.";}
            final String report=result;
            owner.runOnUiThread(()->{
                if(request!=generation)return;loading=false;pending=null;
                if(owner.isFinishing()||owner.isDestroyed())return;
                TextView text=new TextView(owner);text.setText(report);text.setTextIsSelectable(true);text.setTextSize(14);
                int p=Math.round(16*owner.getResources().getDisplayMetrics().density);text.setPadding(p,p,p,p);
                ScrollView scroll=new ScrollView(owner);scroll.addView(text);
                AlertDialog shown=new AlertDialog.Builder(owner).setTitle("Radio runtime paths · read only").setView(scroll)
                    .setPositiveButton("Copy report",(d,w)->{
                        ClipboardManager clipboard=(ClipboardManager)owner.getSystemService(Context.CLIPBOARD_SERVICE);
                        if(clipboard!=null){clipboard.setPrimaryClip(ClipData.newPlainText("NexaDash Radio runtime paths",report));Toast.makeText(owner,"Radio path report copied",Toast.LENGTH_SHORT).show();}
                    }).setNegativeButton("Close",null).create();
                dialog=shown;shown.setOnDismissListener(d->{if(dialog==shown)dialog=null;});shown.show();
            });
        });}catch(RejectedExecutionException unavailable){loading=false;pending=null;Toast.makeText(owner,"Radio path check unavailable",Toast.LENGTH_SHORT).show();}
    }
    void close(){
        generation++;loading=false;if(pending!=null){pending.cancel(false);pending=null;}
        if(dialog!=null){dialog.dismiss();dialog=null;}
    }
    static String collect(Context context){return collect(context,()->false);}
    private static void cancelled(BooleanSupplier stop){if(stop.getAsBoolean())throw new CancellationException();}
    private static String clean(String value){
        if(value==null||value.isEmpty())return "not supplied";
        String safe=value.replaceAll("[\\p{Cntrl}]"," ");return safe.length()>1024?safe.substring(0,1024)+"…":safe;
    }
    private static String visibility(File file){
        try{return file.exists()?(file.canRead()?"visible; readable flag true":"visible; readable flag false"):"not visible to this app";}
        catch(SecurityException denied){return "not visible to this app (access denied)";}
    }
    private static void libraryPaths(StringBuilder out,String directory){
        for(String library:LIBRARIES){File file=new File(directory,library);out.append("  ").append(clean(file.getPath())).append(": ").append(visibility(file)).append('\n');}
    }
    private static void oatPaths(StringBuilder out,String apkPath){
        if(apkPath==null||!apkPath.endsWith(".apk")){out.append("  OAT paths: source path not supplied\n");return;}
        File parent=new File(apkPath).getParentFile();if(parent==null)return;
        // Only the known package's immediate oat/arm directory. No recursion or app data access.
        File directory=new File(parent,"oat/arm");out.append("  ").append(clean(directory.getPath())).append(": ").append(visibility(directory)).append('\n');
        String[] names;try{names=directory.list();}catch(SecurityException denied){names=null;}
        if(names==null){out.append("    directory entries not visible\n");return;}
        Arrays.sort(names);int shown=0,total=0;
        for(String name:names)if(name.endsWith(".odex")||name.endsWith(".vdex")){
            total++;if(shown++<MAX_OAT_FILES)out.append("    ").append(clean(name)).append('\n');
        }
        if(total==0)out.append("    no .odex/.vdex entry visible in this directory\n");
        if(total>MAX_OAT_FILES)out.append("    further entries omitted\n");
    }
    private static String collect(Context context,BooleanSupplier stop){
        StringBuilder out=new StringBuilder(8192);
        out.append("NEXADASH — RADIO RUNTIME PATHS\nRead-only visibility check; not a Radio controller or native-hosting test.\nAPI: ").append(Build.VERSION.SDK_INT).append('\n');
        out.append("\nInstalled Radio packages\n");PackageManager pm=context.getPackageManager();
        for(String pkg:PACKAGES){
            cancelled(stop);out.append('\n').append(pkg).append('\n');
            try{
                PackageInfo info=pm.getPackageInfo(pkg,PackageManager.GET_SHARED_LIBRARY_FILES);ApplicationInfo app=info.applicationInfo;
                out.append("  Version: ").append(clean(info.versionName)).append('\n');
                if(app==null){out.append("  Application metadata not supplied\n");continue;}
                out.append("  APK source: ").append(clean(app.sourceDir)).append("\n  Native library directory: ").append(clean(app.nativeLibraryDir)).append('\n');
                out.append("  Declared shared library files:\n");
                if(app.sharedLibraryFiles==null||app.sharedLibraryFiles.length==0)out.append("    none supplied by PackageManager (not the complete bootclasspath)\n");
                else{for(int i=0;i<Math.min(app.sharedLibraryFiles.length,MAX_SHARED_LIBRARIES);i++)out.append("    ").append(clean(app.sharedLibraryFiles[i])).append('\n');if(app.sharedLibraryFiles.length>MAX_SHARED_LIBRARIES)out.append("    further entries omitted\n");}
                cancelled(stop);if(app.nativeLibraryDir!=null)libraryPaths(out,app.nativeLibraryDir);oatPaths(out,app.sourceDir);
            }catch(PackageManager.NameNotFoundException unavailable){out.append("  Package not visible/installed for this app\n");}
            catch(SecurityException denied){out.append("  Package metadata not visible (access denied)\n");}
        }
        out.append("\nKnown 32-bit system/vendor library paths\n");
        for(String dir:SYSTEM_DIRS){cancelled(stop);libraryPaths(out,dir);}
        out.append("\nProcess BOOTCLASSPATH (paths only, if exposed)\n");
        String boot;try{boot=System.getenv("BOOTCLASSPATH");}catch(SecurityException denied){boot=null;}
        if(boot==null||boot.isEmpty())out.append("  not exposed to this process\n");
        else{String[] paths=boot.split(":");for(int i=0;i<Math.min(paths.length,48);i++)out.append("  ").append(clean(paths[i])).append('\n');if(paths.length>48)out.append("  further paths omitted\n");}
        cancelled(stop);
        out.append("\nOEM class visibility: not attempted; no OEM class initialization or native library loading.\n\nNot visible does not prove absent from firmware: permissions, SELinux, namespaces or package visibility may hide files. A readable flag does not prove a usable ABI, accessible tuner or valid control API.\nNo apps launched, service binding, broadcasts, native library loads, shell/root commands, settings writes or network uploads. No account data, app databases, GPS coordinates, serial numbers or file contents collected. Report is shared only when Copy report is pressed.");
        return out.toString();
    }
}
