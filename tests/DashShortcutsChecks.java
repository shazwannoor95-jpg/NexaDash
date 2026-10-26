package com.shazwan.sagadash;

import android.content.*;
import android.net.Uri;
import java.util.*;

/** Checks source separation and persistent assignment without launching vendor apps. */
final class DashShortcutsChecks {
    int assertions;
    void check(boolean condition,String message){if(!condition)throw new AssertionError(message);assertions++;}
    int run(Context context){
        int before=assertions;
        String[] video=DashShortcuts.defaults("Video");
        check(video.length>0,"Video has player candidates");
        for(String candidate:video)check(!candidate.contains("youtube")&&!candidate.contains("tizentube"),"Video never automatically launches YouTube/TizenTube");
        String[] youtube=DashShortcuts.defaults("YouTube");
        check(youtube.length>0&&youtube[0].equals("io.gh.reisxd.tizentube.cobalt"),"YouTube retains the separate TizenTube shortcut");
        check(DashShortcuts.defaults("DSP").length==0,"DSP does not guess an unsupported OEM package");
        List<DashShortcuts.App> headUnit=Arrays.asList(
            app("oem.music","Music",true),app("store","Play Store",false),app("astro","My Astro",false),
            app("vlc","VLC",false),app("aux","AUX",true),app("oem.bluetooth","BT",true),
            app("calculator","Calculator",true),app("calendar","Calendar",true),
            app("oem.car","Car Settings",true),app("oem.carset","CarSet",true),
            app("oem.video","Video",true),app("youtube","YouTube",false),app("oem.link","ZLINK",true));
        check(only("Bluetooth",headUnit,true).component.getPackageName().equals("oem.bluetooth"),"unknown OEM package labelled BT can be discovered");
        check(only("Music",headUnit,false).component.getPackageName().equals("oem.music"),"factory Music is discovered without guessing its package ID");
        check(only("Car Link",headUnit,false).component.getPackageName().equals("oem.link"),"ZLINK is recognized as Car Link");
        check(only("YouTube",headUnit,false).component.getPackageName().equals("youtube"),"YouTube discovery excludes local Video and VLC");
        check(DashShortcuts.matching("Video",headUnit,false).size()==2,"two local video players remain ambiguous for manual choice");
        check(DashShortcuts.matching("Vehicle settings",headUnit,true).size()==2,"Car Settings and CarSet require a choice when both are installed");
        check(DashShortcuts.automatic("Vehicle settings",headUnit)==null,"automatic launch does not choose between two built-in car settings apps");
        check(DashShortcuts.automatic("Video",headUnit)==null,"automatic launch does not guess between unknown local video players");
        check(DashShortcuts.matching("DSP",headUnit,false).isEmpty(),"CarSet and AUX do not imply a DSP app");
        check(DashShortcuts.matching("Radio",headUnit,false).isEmpty(),"AUX and My Astro do not imply an FM radio app");
        List<DashShortcuts.App> bluetooth=Arrays.asList(app("third.party","Bluetooth",false),app("oem.bt","BT",true));
        check(only("Bluetooth",bluetooth,true).component.getPackageName().equals("oem.bt"),"system Bluetooth labels take preference over third-party names");
        check(DashShortcuts.automatic("Bluetooth",bluetooth).component.getPackageName().equals("oem.bt"),"automatic Bluetooth launch selects the unique system app");
        check(DashShortcuts.matching("Bluetooth",bluetooth,false).size()==2,"non-system duplicate labels remain ambiguous without system preference");
        check(DashShortcuts.labelMatches("Vehicle settings"," CAR-SET ")&&DashShortcuts.labelMatches("Vehicle settings","Vehicle Settings"),"OEM car-setting separators and casing are normalized");
        check(!DashShortcuts.labelMatches("Vehicle settings","Settings")&&!DashShortcuts.labelMatches("Bluetooth","Bluetooth Settings"),"generic Android settings are not mistaken for car or Bluetooth audio apps");
        check(!DashShortcuts.labelMatches("Video","YouTube Video")&&!DashShortcuts.labelMatches("DSP","DSP Downloader"),"partial names cannot select unrelated apps");
        check(!DashShortcuts.labelMatches("Video","TizenTube")&&!DashShortcuts.labelMatches("YouTube app","TizenTube"),"separate video and YouTube-app shortcuts keep their source boundaries");
        List<DashShortcuts.App> knownPlayers=Arrays.asList(app("com.google.android.youtube","YouTube",false),app("io.gh.reisxd.tizentube.cobalt","TizenTube",false),app("org.videolan.vlc","VLC",false));
        check(DashShortcuts.automatic("Video",knownPlayers).component.getPackageName().equals("org.videolan.vlc"),"known local video player stays separate when YouTube and TizenTube are installed");
        check(DashShortcuts.automatic("YouTube",knownPlayers).component.getPackageName().equals("io.gh.reisxd.tizentube.cobalt"),"combined YouTube shortcut retains TizenTube preference");
        check(DashShortcuts.automatic("YouTube app",knownPlayers).component.getPackageName().equals("com.google.android.youtube"),"YouTube-only source resolves independently from the combined shortcut");
        String[] labels=DashShortcuts.chooserLabels(Arrays.asList(app("vendor.one","BT",true),app("vendor.two","BT",false),app("video","Video",true)));
        check(labels[0].contains("vendor.one")&&labels[1].contains("vendor.two")&&!labels[0].equals(labels[1]),"duplicate chooser names display their component identity");
        check(labels[2].equals("Video"),"unambiguous chooser names remain concise");
        Intent oemLaunch=DashShortcuts.launch(only("Bluetooth",headUnit,true));
        check(oemLaunch.getComponent().getPackageName().equals("oem.bluetooth")&&Intent.ACTION_MAIN.equals(oemLaunch.getAction()),"detected app opens through an explicit launcher component");
        check(DashShortcuts.PICK_AUDIO!=DashShortcuts.PICK_VIDEO&&DashShortcuts.PICK_FILE!=4813,"file picker requests do not collide with car-video import");
        check(DashShortcuts.fileType(DashShortcuts.PICK_AUDIO).equals("audio/*"),"Music file picker only requests audio");
        check(DashShortcuts.fileType(DashShortcuts.PICK_VIDEO).equals("video/*"),"Video file picker only requests video");
        check(DashShortcuts.fileType(4813)==null,"car-video result is not consumed by external player shortcuts");
        Intent picker=DashShortcuts.filePicker("video/*");
        check(picker.getAction().equals(Intent.ACTION_OPEN_DOCUMENT)&&picker.hasCategory(Intent.CATEGORY_OPENABLE),"video selection uses the document provider");
        Intent playback=DashShortcuts.playback(Uri.parse("content://fixture/video/1"),"video/mp4");
        check(playback.getAction().equals(Intent.ACTION_VIEW)&&playback.getType().equals("video/mp4"),"selected video opens with its content type");
        check((playback.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0,"external player receives permission to read the selected video");
        check(playback.getComponent()==null&&playback.getPackage()==null,"local video is not forced into YouTube or an unrelated app");
        final String fixture="__shortcut_assignment_checks__";
        SharedPreferences preferences=DashShortcuts.prefs(context);String old=preferences.getString(fixture,null);
        try{
            ComponentName app=new ComponentName("test.player.fixture","test.player.fixture.PlayerActivity");
            DashShortcuts.assign(context,fixture,app);
            check(app.equals(DashShortcuts.saved(context.createDeviceProtectedStorageContext(),fixture)),"assignment persists independently of helper/activity instance");
            check(DashShortcuts.matches(context,fixture,app.getPackageName())&&!DashShortcuts.matches(context,fixture,"unrelated.player"),"source matching obeys the explicit assignment rather than a package-name substring");
            DashShortcuts.assign(context,fixture,null);
            check(DashShortcuts.saved(context,fixture)==null&&!preferences.contains(fixture),"Use automatic removes only the selected shortcut assignment");
        }finally{
            SharedPreferences.Editor edit=preferences.edit();if(old==null)edit.remove(fixture);else edit.putString(fixture,old);edit.apply();
        }
        return assertions-before;
    }
    DashShortcuts.App app(String pkg,String label,boolean system){return new DashShortcuts.App(new ComponentName(pkg,pkg+".Main"),label,system);}
    DashShortcuts.App only(String name,List<DashShortcuts.App> apps,boolean systemOnly){
        List<DashShortcuts.App> matches=DashShortcuts.matching(name,apps,systemOnly);
        if(matches.size()!=1)throw new AssertionError("Expected one "+name+" match, found "+matches.size());
        return matches.get(0);
    }
}
