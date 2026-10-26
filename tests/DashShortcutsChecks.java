package com.shazwan.sagadash;

import android.content.*;
import android.net.Uri;

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
            DashShortcuts.assign(context,fixture,null);
            check(DashShortcuts.saved(context,fixture)==null&&!preferences.contains(fixture),"Use automatic removes only the selected shortcut assignment");
        }finally{
            SharedPreferences.Editor edit=preferences.edit();if(old==null)edit.remove(fixture);else edit.putString(fixture,old);edit.apply();
        }
        return assertions-before;
    }
}
