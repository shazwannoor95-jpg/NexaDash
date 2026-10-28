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
    private static volatile List<App> cachedApps;
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
            case "YouTube app":return new String[]{"com.google.android.youtube","com.google.android.youtube.tv"};
            case "TizenTube":return new String[]{"io.gh.reisxd.tizentube.cobalt"};
            case "Navigation":return new String[]{"com.waze","com.google.android.apps.maps"};
            case "DiAuto":return new String[]{"com.andrerinas.headunitrevived"};
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
    /** Names help discover OEM apps, but screenshots cannot establish their package IDs. */
    static final class App {
        final ComponentName component;final String label;final boolean system;
        App(ComponentName component,String label,boolean system){this.component=component;this.label=label;this.system=system;}
    }
    static String normalized(String label){
        StringBuilder value=new StringBuilder();
        for(char c:label.toLowerCase(Locale.ROOT).toCharArray())if(Character.isLetterOrDigit(c))value.append(c);
        return value.toString();
    }
    static boolean labelMatches(String name,String label){
        String aliases;
        switch(name){
            case "Music":aliases="music|musicplayer|localmusic";break;
            case "Bluetooth":aliases="bt|bluetooth|btmusic|bluetoothmusic|bluetoothaudio|bluetoothplayer";break;
            case "Radio":aliases="radio|fm|fmradio|amfmradio";break;
            case "Video":aliases="video|videos|videoplayer|vlc|vlcforandroid|mxplayer";break;
            case "YouTube":aliases="youtube|tizentube";break;
            case "YouTube app":aliases="youtube";break;
            case "TizenTube":aliases="tizentube";break;
            case "Navigation":aliases="navigation|maps|googlemaps|waze";break;
            case "Car Link":aliases="zlink|zlink5|carlink";break;
            case "Vehicle settings":aliases="carsettings|carsetting|carset|vehiclesettings|vehiclesetting";break;
            case "Files":aliases="files|filemanager|fileexplorer|filebrowser";break;
            case "Equalizer":aliases="equalizer|equaliser";break;
            case "DSP":aliases="dsp";break;
            default:return false;
        }
        String value=normalized(label);if(value.isEmpty())return false;
        for(String alias:aliases.split("\\|"))if(value.equals(alias))return true;
        return false;
    }
    static List<App> matching(String name,List<App> apps,boolean systemOnly){
        List<App> matches=new ArrayList<>();
        for(App app:apps)if((!systemOnly||app.system)&&labelMatches(name,app.label))matches.add(app);
        return matches;
    }
    static App automatic(String name,List<App> apps){
        if(name.equals("Bluetooth")||name.equals("Vehicle settings")){
            List<App> system=matching(name,apps,true);
            if(!system.isEmpty())return system.size()==1?system.get(0):null;
        }
        for(String pkg:defaults(name))for(App app:apps)if(app.component.getPackageName().equals(pkg))return app;
        List<App> matches=matching(name,apps,false);
        return matches.size()==1?matches.get(0):null;
    }
    static List<App> installed(Context context){
        PackageManager pm=context.getPackageManager();
        List<ResolveInfo> queried=new ArrayList<>(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0));
        queried.addAll(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),0));
        LinkedHashMap<ComponentName,App> unique=new LinkedHashMap<>();
        for(ResolveInfo info:queried){
            ActivityInfo a=info.activityInfo;
            if(a==null||!a.enabled||!a.exported||a.packageName.equals(context.getPackageName())
                    ||a.applicationInfo==null||!a.applicationInfo.enabled)continue;
            if(a.permission!=null&&pm.checkPermission(a.permission,context.getPackageName())!=PackageManager.PERMISSION_GRANTED)continue;
            ComponentName component=new ComponentName(a.packageName,a.name);
            CharSequence label=info.loadLabel(pm);
            boolean system=(a.applicationInfo.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0;
            unique.put(component,new App(component,label==null?a.packageName:label.toString(),system));
        }
        List<App> apps=new ArrayList<>(unique.values());
        Collections.sort(apps,(a,b)->{int result=a.label.compareToIgnoreCase(b.label);return result!=0?result:a.component.flattenToString().compareTo(b.component.flattenToString());});
        cachedApps=Collections.unmodifiableList(apps);
        return cachedApps;
    }
    /** No launching; package discovery is cached and refreshed when the chooser/shortcut is opened. */
    static boolean matches(Context context,String name,String pkg){
        if(pkg==null||pkg.isEmpty())return false;
        ComponentName assigned=saved(context,name);
        if(assigned!=null)return assigned.getPackageName().equals(pkg);
        List<App> apps=cachedApps;if(apps==null)apps=installed(context);
        App automatic=automatic(name,apps);
        return automatic!=null&&automatic.component.getPackageName().equals(pkg);
    }
    static Intent launch(App app){return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(app.component);}
    static String[] chooserLabels(List<App> apps){
        Map<String,Integer> counts=new HashMap<>();
        for(App app:apps){String label=app.label.toLowerCase(Locale.ROOT);counts.put(label,counts.getOrDefault(label,0)+1);}
        String[] labels=new String[apps.size()];
        for(int i=0;i<apps.size();i++){
            App app=apps.get(i);String label=app.label;
            if(counts.get(label.toLowerCase(Locale.ROOT))>1)label+="\n"+app.component.flattenToShortString();
            labels[i]=label;
        }
        return labels;
    }
    static void open(Activity activity,String name){
        ComponentName assigned=saved(activity,name);
        if(assigned!=null){
            if(start(activity,new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(assigned)))return;
            unavailable(activity,name,true);return;
        }
        App automatic=automatic(name,installed(activity));
        if(automatic!=null&&start(activity,launch(automatic)))return;
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
        List<App> apps=installed(activity);
        AlertDialog.Builder dialog=new AlertDialog.Builder(activity).setTitle("Choose app for "+name)
            .setNeutralButton("Use automatic",(d,w)->{assign(activity,name,null);Toast.makeText(activity,"Automatic selection restored for "+name,Toast.LENGTH_SHORT).show();})
            .setNegativeButton("Cancel",null);
        if(apps.isEmpty())dialog.setMessage("No launchable apps are installed. Install the app you want on this device, then try again.");
        else dialog.setItems(chooserLabels(apps),(d,index)->{
            App app=apps.get(index);
            if(start(activity,launch(app)))assign(activity,name,app.component);
            else new AlertDialog.Builder(activity).setTitle("Unable to open app").setMessage("The app could not be launched. The shortcut assignment was not changed.").setPositiveButton("OK",null).show();
        });
        dialog.show();
    }
}
