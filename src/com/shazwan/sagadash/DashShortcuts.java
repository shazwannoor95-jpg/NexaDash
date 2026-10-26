package com.shazwan.sagadash;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.widget.Toast;
import java.util.*;

/** Persistent app assignments; a missing OEM app is never replaced by an unrelated app. */
final class DashShortcuts {
    static final int PICK_AUDIO=4811, PICK_VIDEO=4812, PICK_FILE=4814;
    private DashShortcuts(){}
    static SharedPreferences prefs(Context context){
        return context.createDeviceProtectedStorageContext().getSharedPreferences("shortcut_apps",0);
    }
    static ComponentName saved(Context context,String name){
        return ComponentName.unflattenFromString(prefs(context).getString(name,""));
    }
    static void assign(Context context,String name,ComponentName component){
        SharedPreferences.Editor edit=prefs(context).edit();
        if(component==null)edit.remove(name);else edit.putString(name,component.flattenToString());
        edit.apply();
    }
    static String[] defaults(String name){
        switch(name){
            case "Music":return new String[]{"com.spotify.music","com.nwd.android.music"};
            case "Bluetooth":return new String[]{"com.nwd.android.bt","com.nwd.android.phone"};
            case "Radio":return new String[]{"com.nwd.android.radio"};
            case "Video":return new String[]{"com.nwd.android.video","org.videolan.vlc","com.mxtech.videoplayer.ad","com.mxtech.videoplayer.pro"};
            case "YouTube":return new String[]{"io.gh.reisxd.tizentube.cobalt","com.google.android.youtube","com.google.android.youtube.tv"};
            case "Navigation":return new String[]{"com.waze","com.google.android.apps.maps"};
            case "Car Link":return new String[]{"com.zjinnova.zlink","com.zjinnova.zlink5"};
            case "Vehicle settings":return new String[]{"com.android.car.setting"};
            default:return new String[0];
        }
    }
    static boolean start(Activity activity,Intent intent){
        if(intent==null)return false;
        try{activity.startActivity(intent);return true;}catch(ActivityNotFoundException|SecurityException e){return false;}
    }
    static Intent appIntent(PackageManager pm,String pkg){
        Intent intent=pm.getLaunchIntentForPackage(pkg);
        return intent!=null?intent:pm.getLeanbackLaunchIntentForPackage(pkg);
    }
    static void open(Activity activity,String name){
        ComponentName assigned=saved(activity,name);
        if(assigned!=null){
            if(start(activity,new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(assigned)))return;
            unavailable(activity,name,true);return;
        }
        for(String pkg:defaults(name))if(start(activity,appIntent(activity.getPackageManager(),pkg)))return;
        if(name.equals("Files")){
            try{activity.startActivityForResult(filePicker("*/*"),PICK_FILE);return;}
            catch(ActivityNotFoundException|SecurityException ignored){}
        }
        if(name.equals("Equalizer")&&start(activity,new Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL")
                .putExtra("android.media.extra.PACKAGE_NAME",activity.getPackageName())))return;
        unavailable(activity,name,false);
    }
    static Intent filePicker(String type){
        return new Intent(Intent.ACTION_OPEN_DOCUMENT).setType(type).addCategory(Intent.CATEGORY_OPENABLE);
    }
    static String fileType(int request){return request==PICK_AUDIO?"audio/*":request==PICK_VIDEO?"video/*":request==PICK_FILE?"*/*":null;}
    static void pick(Activity activity,String name){
        int request=name.equals("Music")?PICK_AUDIO:PICK_VIDEO;
        try{activity.startActivityForResult(filePicker(fileType(request)),request);}
        catch(ActivityNotFoundException|SecurityException e){unavailable(activity,name,false);}
    }
    static Intent playback(Uri uri,String type){
        return new Intent(Intent.ACTION_VIEW).setDataAndType(uri,type).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }
    static boolean handleResult(Activity activity,int request,int result,Intent data){
        String type=fileType(request);if(type==null)return false;
        if(result!=Activity.RESULT_OK||data==null||data.getData()==null)return true;
        Uri uri=data.getData();String actualType=null;try{actualType=activity.getContentResolver().getType(uri);}catch(SecurityException ignored){}
        if(actualType==null)actualType=type;
        if(!start(activity,playback(uri,actualType))){
            new AlertDialog.Builder(activity).setTitle("Player not installed")
                .setMessage("The file was selected, but no installed app can play it. Install a compatible "+(request==PICK_AUDIO?"audio":request==PICK_VIDEO?"video":"file")+" player on this device, then try again.")
                .setPositiveButton("OK",null).show();
        }
        return true;
    }
    static void unavailable(Activity activity,String name,boolean savedMissing){
        String message=savedMissing?"The app assigned to this shortcut is unavailable. Choose another installed app, or long-press this shortcut to reset its assignment.":
            name.equals("Radio")?"This device has no detected FM radio app. On your head unit, choose its built-in radio app. A phone emulator does not provide the head unit's FM hardware.":
            name.equals("Bluetooth")?"Choose the head unit's Bluetooth audio app. Pairing a phone alone does not provide a launchable Bluetooth player in an emulator.":
            name.equals("DSP")||name.equals("Equalizer")?"Choose the audio settings app supplied with your head unit. The emulator does not include its DSP hardware or vendor app.":
            name.equals("YouTube")?"YouTube / TizenTube is not installed on this device. Install a compatible version separately, or choose an installed app for this shortcut.":
            "No compatible "+name.toLowerCase(Locale.US)+" app was detected on this device. Choose an installed app for this shortcut. Long-press the shortcut whenever you want to change it.";
        AlertDialog.Builder dialog=new AlertDialog.Builder(activity).setTitle(name+" shortcut").setMessage(message)
            .setPositiveButton("Choose app",(d,w)->choose(activity,name)).setNegativeButton("Cancel",null);
        if(name.equals("Music")||name.equals("Video"))dialog.setNeutralButton(name.equals("Music")?"Open audio file":"Open video file",(d,w)->pick(activity,name));
        dialog.show();
    }
    static void choose(Activity activity,String name){
        PackageManager pm=activity.getPackageManager();
        List<ResolveInfo> queried=new ArrayList<>(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0));
        queried.addAll(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),0));
        LinkedHashMap<ComponentName,ResolveInfo> unique=new LinkedHashMap<>();
        for(ResolveInfo info:queried){
            ActivityInfo a=info.activityInfo;
            if(a!=null&&a.enabled&&a.exported&&!a.packageName.equals(activity.getPackageName()))unique.put(new ComponentName(a.packageName,a.name),info);
        }
        List<ResolveInfo> apps=new ArrayList<>(unique.values());
        Collections.sort(apps,(a,b)->a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
        List<String> labels=new ArrayList<>();
        for(ResolveInfo info:apps)labels.add(info.loadLabel(pm).toString());
        AlertDialog.Builder dialog=new AlertDialog.Builder(activity).setTitle("Choose app for "+name)
            .setNeutralButton("Use automatic",(d,w)->{assign(activity,name,null);Toast.makeText(activity,"Automatic selection restored for "+name,Toast.LENGTH_SHORT).show();})
            .setNegativeButton("Cancel",null);
        if(apps.isEmpty())dialog.setMessage("No launchable apps are installed. Install the app you want on this device, then try again.");
        else dialog.setItems(labels.toArray(new String[0]),(d,index)->{
            ActivityInfo info=apps.get(index).activityInfo;ComponentName component=new ComponentName(info.packageName,info.name);
            if(start(activity,new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component)))assign(activity,name,component);
            else new AlertDialog.Builder(activity).setTitle("Unable to open app").setMessage("The app could not be launched. The shortcut assignment was not changed.").setPositiveButton("OK",null).show();
        });
        dialog.show();
    }
}
