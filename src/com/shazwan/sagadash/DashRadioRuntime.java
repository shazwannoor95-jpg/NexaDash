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
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
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

/** Passive paths, selector and device metadata. Never opens a driver or loads OEM code. */
final class DashRadioRuntime {
    private static final String[] PACKAGES={"com.acloud.stub.localradio","com.acloud.stub.extradio","com.android.fmradio"};
    private static final String[] LIBRARIES={"libRadioInterface.so","libfmjni.so"};
    private static final String[] SYSTEM_DIRS={"/system/lib","/vendor/lib","/system/vendor/lib","/odm/lib"};
    private static final String[] DRIVER_PATHS={"/dev/tef6606_i2c","/dev/keypad","/dev/fm"};
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
    private static void backend(StringBuilder out){
        out.append("\nRecovered Radio selector (read only)\n  Property: ").append(DashRadioBackend.PROPERTY).append('\n');
        try{
            // The original Radio stub reads this one framework property with default "0".
            // No shell, property writes, OEM class initialization or library loads.
            Class<?> properties=Class.forName("android.os.SystemProperties");
            String value=(String)properties.getMethod("get",String.class,String.class).invoke(null,DashRadioBackend.PROPERTY,"0");
            String component=DashRadioBackend.component(value);
            if(component==null){out.append("  Selector value is not an integer; backend unresolved.\n");return;}
            out.append("  Selector value: ").append(Integer.parseInt(value)).append(" (0 is also the firmware default when unset)\n");
            out.append("  Selected component: ").append(component).append("\n  Selection inferred from recovered stub; no app launched and no FM control tested.\n");
        }catch(ReflectiveOperationException|RuntimeException|LinkageError unavailable){
            out.append("  Property not accessible: ").append(unavailable.getClass().getSimpleName()).append("; backend unresolved.\n");
        }
    }
    private static void driverMetadata(StringBuilder out,String path){
        out.append("  ").append(path).append(": ");
        try{
            StructStat info=Os.stat(path);
            out.append("mode ").append(Integer.toOctalString(info.st_mode&07777)).append("; UID ").append(info.st_uid).append("; GID ").append(info.st_gid)
                .append("; character device ").append(OsConstants.S_ISCHR(info.st_mode)).append('\n');
            out.append("    current host UID access flags: read ").append(Os.access(path,OsConstants.R_OK)).append("; write ").append(Os.access(path,OsConstants.W_OK)).append('\n');
        }catch(ErrnoException denied){out.append("metadata/access check unavailable; errno ").append(denied.errno).append('\n');}
        catch(SecurityException denied){out.append("metadata/access check denied\n");}
    }
    private static String collect(Context context,BooleanSupplier stop){
        StringBuilder out=new StringBuilder(8192);
        out.append("NEXADASH — RADIO RUNTIME PATHS\nRead-only visibility check; not a Radio controller or native-hosting test.\nAPI: ").append(Build.VERSION.SDK_INT)
            .append("\nHost UID: ").append(android.os.Process.myUid()).append('\n');
        backend(out);
        out.append("\nLast in-panel FM session\n").append(DashFmController.report(context)).append('\n');
        out.append("\nInstalled Radio packages\n");PackageManager pm=context.getPackageManager();
        for(String pkg:PACKAGES){
            cancelled(stop);out.append('\n').append(pkg).append('\n');
            try{
                PackageInfo info=pm.getPackageInfo(pkg,PackageManager.GET_SHARED_LIBRARY_FILES);ApplicationInfo app=info.applicationInfo;
                out.append("  Version: ").append(clean(info.versionName)).append('\n');
                if(app==null){out.append("  Application metadata not supplied\n");continue;}
                out.append("  App UID: ").append(app.uid).append("; system app: ").append((app.flags&ApplicationInfo.FLAG_SYSTEM)!=0).append('\n');
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
        out.append("\nKnown FM device metadata (drivers not opened)\n");
        for(String path:DRIVER_PATHS){cancelled(stop);driverMetadata(out,path);}
        out.append("  Metadata/access flags are for the host UID only. They do not prove that a different companion UID, the linker or SELinux will permit driver open, ioctl, audio routing or tuner control.\n");
        out.append("\nProcess BOOTCLASSPATH (paths only, if exposed)\n");
        String boot;try{boot=System.getenv("BOOTCLASSPATH");}catch(SecurityException denied){boot=null;}
        if(boot==null||boot.isEmpty())out.append("  not exposed to this process\n");
        else{String[] paths=boot.split(":");for(int i=0;i<Math.min(paths.length,48);i++)out.append("  ").append(clean(paths[i])).append('\n');if(paths.length>48)out.append("  further paths omitted\n");}
        cancelled(stop);
        out.append("\nOEM class visibility: not attempted; no OEM class initialization or native library loading.\n\nNot visible does not prove absent from firmware: permissions, SELinux, namespaces or package visibility may hide files. A readable flag does not prove a usable ABI, accessible tuner or valid control API.\nNo apps launched, service binding, broadcasts, native library loads, driver opens, reads/writes/ioctls, shell/root commands, settings writes or network uploads. Only one fixed FM selector property and the fixed paths/metadata above are inspected. No account data, app databases, GPS coordinates, serial numbers or file contents collected. Report is shared only when Copy report is pressed.");
        return out.toString();
    }
}
