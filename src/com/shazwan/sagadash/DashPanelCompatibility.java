package com.shazwan.sagadash;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/** On-demand, read-only evidence for app hosting. It never attempts to host or launch an app. */
final class DashPanelCompatibility {
    private static final String[] SOURCES={"YouTube","YouTube app","TizenTube","Radio","Navigation","Music","Video","Bluetooth"};
    private static final String[] PERMISSIONS={"android.permission.MANAGE_ACTIVITY_TASKS",
        "android.permission.MANAGE_ACTIVITY_STACKS","android.permission.ACTIVITY_EMBEDDING",
        "android.permission.INJECT_EVENTS","android.permission.ADD_TRUSTED_DISPLAY"};
    private int generation;
    private boolean loading;
    private AlertDialog dialog;

    void show(Activity activity,ExecutorService worker){
        if(loading||activity.isFinishing()||activity.isDestroyed()||(dialog!=null&&dialog.isShowing()))return;
        loading=true;final int request=++generation;
        Toast.makeText(activity,"Checking app panel compatibility…",Toast.LENGTH_SHORT).show();
        try{worker.execute(()->{
            String collected;
            try{collected=collect(activity);}catch(RuntimeException failure){collected="Compatibility check unavailable on this firmware. No apps were launched.\n"+failure.getClass().getSimpleName();}
            final String report=collected;
            activity.runOnUiThread(()->{
                loading=false;
                if(request!=generation||activity.isFinishing()||activity.isDestroyed())return;
                TextView text=new TextView(activity);text.setText(report);text.setTextIsSelectable(true);
                int padding=Math.round(16*activity.getResources().getDisplayMetrics().density);
                text.setPadding(padding,padding,padding,padding);text.setTextSize(14);
                ScrollView scroll=new ScrollView(activity);scroll.addView(text);
                AlertDialog shown=new AlertDialog.Builder(activity).setTitle("App panel compatibility")
                    .setView(scroll).setPositiveButton("Copy report",(d,w)->{
                        ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        if(clipboard!=null){clipboard.setPrimaryClip(ClipData.newPlainText("NexaDash app panel compatibility",report));Toast.makeText(activity,"Report copied. Paste it into your support chat.",Toast.LENGTH_LONG).show();}
                    }).setNegativeButton("Close",null).create();
                dialog=shown;shown.setOnDismissListener(d->{if(dialog==shown)dialog=null;});shown.show();
            });
        });}catch(RejectedExecutionException ignored){loading=false;}
    }

    void close(){generation++;if(dialog!=null){dialog.dismiss();dialog=null;}}

    static boolean candidate(DashShortcuts.App app){
        if(app==null||app.component==null)return false;
        for(DashNativeFrame.Target target:DashNativeFrame.TARGETS)if(target.pkg.equals(app.component.getPackageName()))return true;
        for(String source:SOURCES){
            if(app.label!=null&&DashShortcuts.labelMatches(source,app.label))return true;
            for(String pkg:DashShortcuts.defaults(source))if(pkg.equals(app.component.getPackageName()))return true;
        }
        return false;
    }

    static String embeddingStatus(int sdk,int activityFlags){
        if(sdk<33)return "Not available before API 33";
        return (activityFlags&ActivityInfo.FLAG_ALLOW_UNTRUSTED_ACTIVITY_EMBEDDING)!=0
            ?"Opt-in declared; host support still required":"No untrusted-embedding opt-in declared";
    }

    static String collect(Context context){
        PackageManager pm=context.getPackageManager();String own=context.getPackageName();
        StringBuilder out=new StringBuilder(4096);
        out.append("NEXADASH — APP PANEL COMPATIBILITY\nRead-only check; experimental native hosting needs a patched companion.\n\n");
        try{PackageInfo info=pm.getPackageInfo(own,0);out.append("NexaDash: ").append(clean(info.versionName)).append('\n');}catch(PackageManager.NameNotFoundException ignored){}
        out.append("Manufacturer: ").append(clean(Build.MANUFACTURER)).append("\nModel: ").append(clean(Build.MODEL))
            .append("\nAndroid reported version: ").append(clean(Build.VERSION.RELEASE)).append("\nActual SDK API: ").append(Build.VERSION.SDK_INT)
            .append("\nSupported ABIs: ").append(android.text.TextUtils.join(", ",Build.SUPPORTED_ABIS)).append('\n');
        DisplayMetrics metrics=context.getResources().getDisplayMetrics();
        out.append("App display: ").append(metrics.widthPixels).append('x').append(metrics.heightPixels).append("; density DPI ").append(metrics.densityDpi).append('\n');
        ActivityManager manager=(ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        if(manager!=null)out.append("Low-RAM device flag: ").append(manager.isLowRamDevice()).append('\n');
        out.append("System split-screen support: not established by this check\n");
        for(String feature:new String[]{"android.software.picture_in_picture","android.software.freeform_window_management","android.software.activities_on_secondary_displays"}){
            out.append(feature).append(": ").append(pm.hasSystemFeature(feature)).append('\n');
        }
        ApplicationInfo self=context.getApplicationInfo();
        out.append("NexaDash system app: ").append((self.flags&ApplicationInfo.FLAG_SYSTEM)!=0)
            .append("\nNexaDash target SDK: ").append(self.targetSdkVersion).append("\n\nHost permissions (not proof of a working host):\n");
        for(String permission:PERMISSIONS)out.append(permission.substring("android.permission.".length())).append(": ")
            .append(pm.checkPermission(permission,own)==PackageManager.PERMISSION_GRANTED?"granted":"not granted").append('\n');
        out.append("\n").append(DashNativeFrame.lastStartupResult(context)).append("\n");
        if(Build.VERSION.SDK_INT>=26)try{
            PackageInfo provider=android.webkit.WebView.getCurrentWebViewPackage();
            out.append("WebView provider: ").append(provider==null?"not established":clean(provider.packageName)+" · "+clean(provider.versionName)).append('\n');
        }catch(RuntimeException|LinkageError unavailable){out.append("WebView provider: unavailable\n");}
        out.append("\nRoot access: not probed\nOEM / Shizuku hosting integration: not established\n\nRelevant launch activities and saved assignments:\n");
        LinkedHashSet<ComponentName> targets=new LinkedHashSet<>();
        for(String source:SOURCES){ComponentName assigned=DashShortcuts.saved(context,source);if(assigned!=null){
            out.append("Assigned ").append(source).append(": ").append(clean(assigned.flattenToShortString())).append('\n');targets.add(assigned);
        }}
        List<DashShortcuts.App> installed=DashShortcuts.installed(context);
        int found=0;for(DashShortcuts.App app:installed)if(candidate(app)){if(found++<40)targets.add(app.component);}
        if(found>40)out.append("More matching activities omitted after 40 candidates.\n");
        if(targets.isEmpty())out.append("No matching launch activities found. Assign your apps in the dashboard first if their labels differ.\n");
        for(ComponentName target:targets){
            out.append('\n').append(clean(target.flattenToShortString())).append('\n');
            try{
                ActivityInfo activity=pm.getActivityInfo(target,0);CharSequence label=activity.loadLabel(pm);
                out.append("Label: ").append(clean(label==null?"":label.toString())).append("\nSystem app: ")
                    .append((activity.applicationInfo.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0)
                    .append("; target SDK: ").append(activity.applicationInfo.targetSdkVersion)
                    .append("\nActivity flags: 0x").append(Integer.toHexString(activity.flags))
                    .append("\nUntrusted activity embedding: ").append(embeddingStatus(Build.VERSION.SDK_INT,activity.flags)).append('\n');
                try{out.append("Version: ").append(clean(pm.getPackageInfo(target.getPackageName(),0).versionName)).append('\n');}catch(PackageManager.NameNotFoundException ignored){}
            }catch(PackageManager.NameNotFoundException|SecurityException failure){out.append("Activity unavailable or inaccessible\n");}
        }
        out.append("\nThis report is not proof that app panels work. PiP/freeform features or manifest opt-in alone do not establish hosting support.\n")
            .append("No apps were launched. No root commands, settings changes or network uploads were performed.\n")
            .append("No serial numbers, accounts, Wi-Fi details or GPS coordinates are included. Copy or screenshot this report for review.\n");
        return out.toString();
    }

    private static String clean(String value){return value==null?"unknown":value.replace('\n',' ').replace('\r',' ').substring(0,Math.min(200,value.length()));}
}
