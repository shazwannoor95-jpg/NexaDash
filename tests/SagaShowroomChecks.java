package com.shazwan.sagadash;

import android.app.Instrumentation;
import android.content.*;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import android.util.Base64;
import java.io.*;
import java.util.Arrays;

/** Exercises actual local-video imports and decoder lifecycle using synthetic test-only media. */
public final class SagaShowroomChecks {
    static final String AUTHORITY="com.shazwan.sagadash.checks.showroom";
    static final Uri VALID=Uri.parse("content://"+AUTHORITY+"/valid");
    static final Uri INVALID=Uri.parse("content://"+AUTHORITY+"/invalid");
    static final Uri TRUNCATED=Uri.parse("content://"+AUTHORITY+"/truncated");
    int assertions;
    void check(boolean condition,String message){if(!condition)throw new AssertionError(message);assertions++;}

    int run(Instrumentation instrumentation,DashboardActivity dashboard) throws Exception {
        int before=assertions;
        Context context=instrumentation.getTargetContext();
        boolean oldMode=dashboard.dashPrefs().getBoolean("vehicle_video",false);
        File video=SagaShowroom.file(context);
        File backup=new File(video.getParentFile(),"vehicle-preview.check-backup");
        if(backup.exists())throw new IOException("Previous showroom test backup exists; restore it before running tests");
        instrumentation.runOnMainSync(()->dashboard.setVehicleVideo(false));
        boolean existed=video.isFile(),protectedOriginal=false;
        try{
            if(existed){
                if(!video.renameTo(backup))throw new IOException("Could not protect the existing video before tests");
                protectedOriginal=true;
            }
            Intent picker=SagaShowroom.pickIntent();
            check(Intent.ACTION_OPEN_DOCUMENT.equals(picker.getAction()),"video import uses the Android document picker");
            check("video/*".equals(picker.getType()),"picker filters video files");
            check(picker.hasCategory(Intent.CATEGORY_OPENABLE),"picker requests readable local documents");
            check(!SagaShowroom.hasVideo(context),"no video is claimed before import");
            SagaShowroom.importVideo(context,VALID);
            byte[] fixture=Base64.decode(FIXTURE,Base64.DEFAULT);
            check(SagaShowroom.hasVideo(context)&&Arrays.equals(read(video),fixture),"content URI is copied intact into private storage");
            check(video.getParentFile().equals(context.createDeviceProtectedStorageContext().getFilesDir()),"video stays in app-private device-protected files");
            MediaMetadataRetriever metadata=new MediaMetadataRetriever();
            try{
                metadata.setDataSource(video.getAbsolutePath());
                check("64".equals(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
                        &&"64".equals(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)),"imported synthetic clip retains its known dimensions");
                check(Long.parseLong(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))>0,"imported clip has a valid duration");
            }finally{metadata.release();}
            rejects(context,INVALID,fixture,"non-video bytes do not replace a valid clip");
            rejects(context,TRUNCATED,fixture,"truncated MP4 does not replace a valid clip");
            rejects(context,Uri.parse("https://example.invalid/car.mp4"),fixture,"network URI is rejected instead of fetched");
            rejects(context,null,fixture,"cancelled/null selection leaves the existing clip intact");
            boolean failed=false;
            try{SagaShowroom.copyVideo(context,new InputStream(){
                @Override public int read() throws IOException {throw new IOException("Synthetic interrupted read");}
            });}catch(IOException expected){failed=true;}
            check(failed&&Arrays.equals(read(video),fixture),"interrupted source read preserves the last working video");
            check(!new File(video.getParentFile(),"vehicle-preview.pending").exists(),"failed imports leave no pending file");
            instrumentation.runOnMainSync(()->dashboard.setVehicleVideo(true));
            waitPrepared(instrumentation,dashboard);
            final Throwable[] failure={null};
            instrumentation.runOnMainSync(()->{
                try{
                    SagaShowroom view=dashboard.showroom;
                    check(view!=null&&view.active&&view.prepared,"visible video mode prepares the real decoder");
                    check(!view.playing(),"video preview is paused by default");
                    check(!view.player.isLooping(),"a recording with a discontinuous seam does not auto-loop");
                    view.toggle();check(view.playing(),"tap starts the video preview");
                    view.toggle();check(!view.playing(),"second tap pauses the video preview");
                    view.pause();
                    check(!view.active&&!view.prepared&&view.player==null,"leaving the preview releases its decoder");
                    view.resume();
                }catch(Throwable e){failure[0]=e;}
            });
            rethrow(failure[0]);
            waitPrepared(instrumentation,dashboard);
            instrumentation.runOnMainSync(()->{
                try{
                    check(!dashboard.showroom.playing(),"resuming does not restart video without a tap");
                    dashboard.setVehicleVideo(false);
                    check(dashboard.car!=null,"switching back restores the interactive 3D viewer");
                }catch(Throwable e){failure[0]=e;}
            });
            rethrow(failure[0]);
        }finally{
            // Stop the decoder before restoring the owner's prior private file.
            instrumentation.runOnMainSync(()->dashboard.setVehicleVideo(false));
            try{
                if(!existed||protectedOriginal){
                    if(video.exists()&&!video.delete())throw new IOException("Could not remove synthetic test clip");
                    if(protectedOriginal&&!backup.renameTo(video))throw new IOException("Restore the previous clip from "+backup.getName());
                }
            }finally{dashboard.dashPrefs().edit().putBoolean("vehicle_video",oldMode).commit();}
        }
        return assertions-before;
    }
    private void rejects(Context context,Uri uri,byte[] fixture,String message) throws Exception {
        boolean failed=false;
        try{SagaShowroom.importVideo(context,uri);}catch(IOException expected){failed=true;}
        check(failed&&Arrays.equals(read(SagaShowroom.file(context)),fixture),message);
    }
    private static byte[] read(File file) throws IOException {
        try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int count;
            while((count=in.read(buffer))!=-1)out.write(buffer,0,count);
            return out.toByteArray();
        }
    }
    private static void waitPrepared(Instrumentation instrumentation,DashboardActivity dashboard) throws Exception {
        long deadline=SystemClock.elapsedRealtime()+30000;
        final boolean[] ready={false};
        while(!ready[0]&&SystemClock.elapsedRealtime()<deadline){
            instrumentation.runOnMainSync(()->ready[0]=dashboard.showroom!=null&&dashboard.showroom.prepared&&dashboard.showroom.player!=null);
            if(!ready[0])SystemClock.sleep(50);
        }
        if(!ready[0])throw new AssertionError("Video decoder did not prepare within 30 seconds");
    }
    private static void rethrow(Throwable failure) throws Exception {
        if(failure instanceof Error)throw (Error)failure;
        if(failure instanceof Exception)throw (Exception)failure;
        if(failure!=null)throw new AssertionError(failure);
    }

    /** Installed only in the checks APK. It can serve synthetic fixture bytes and nothing else. */
    public static final class VideoProvider extends ContentProvider {
        @Override public boolean onCreate(){return true;}
        @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
            if(!"r".equals(mode))throw new FileNotFoundException("Read-only test provider");
            String name=uri.getLastPathSegment();byte[] bytes;
            if("valid".equals(name))bytes=Base64.decode(FIXTURE,Base64.DEFAULT);
            else if("truncated".equals(name))bytes=Arrays.copyOf(Base64.decode(FIXTURE,Base64.DEFAULT),128);
            else if("invalid".equals(name))bytes="This is not video data".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            else throw new FileNotFoundException("Unknown test fixture");
            try{
                File file=new File(getContext().getCacheDir(),"showroom-"+name+".fixture");
                try(FileOutputStream out=new FileOutputStream(file)){out.write(bytes);}
                return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
            }catch(IOException e){throw new FileNotFoundException(e.getMessage());}
        }
        @Override public String getType(Uri uri){return "video/mp4";}
        @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){return null;}
        @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
        @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
        @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
    }
    // Generated with ffmpeg: 1 second, 64x64 blue H264 Baseline at 10fps + a synthetic
    // 440Hz AAC tone. No owner video, registration, photograph or downloaded media.
    static final String FIXTURE=
            "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"+
            "tXGdq4zlWU5VtOVbTYsTjsTlWUhAhAhAhElGSjKoyqUyVpTXXK6u7cwN2KUCUDAwMDAxs3LLLLLLKblllllllNyyyyyyyyyyyyyyyyyyyyyyyy"+
            "yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy"+
            "yyyyyyyyyyyyyyy8AAACbQYF//9p3EXpvebZSLeWLNgg2SPu73gyNjQgLSBjb3JlIDE2NCByMzEwOCAzMWUxOWY5IC0gSC4yNjQvTVBFRy00IE"+
            "FWQyBjb2RlYyAtIENvcHlsZWZ0IDIwMDMtMjAyMyAtIGh0dHA6Ly93d3cudmlkZW9sYW4ub3JnL3gyNjQuaHRtbCAtIG9wdGlvbnM6IGNhYmFj"+
            "PTAgcmVmPTMgZGVibG9jaz0xOjA6MCBhbmFseXNlPTB4MToweDExMSBtZT1oZXggc3VibWU9NyBwc3k9MSBwc3lfcmQ9MS4wMDowLjAwIG1peG"+
            "VkX3JlZj0xIG1lX3JhbmdlPTE2IGNocm9tYV9tZT0xIHRyZWxsaXM9MSA4eDhkY3Q9MCBjcW09MCBkZWFkem9uZT0yMSwxMSBmYXN0X3Bza2lw"+
            "PTEgY2hyb21hX3FwX29mZnNldD0tMiB0aHJlYWRzPTIgbG9va2FoZWFkX3RocmVhZHM9MSBzbGljZWRfdGhyZWFkcz0wIG5yPTAgZGVjaW1hdG"+
            "U9MSBpbnRlcmxhY2VkPTAgYmx1cmF5X2NvbXBhdD0wIGNvbnN0cmFpbmVkX2ludHJhPTAgYmZyYW1lcz0wIHdlaWdodHA9MCBrZXlpbnQ9NSBr"+
            "ZXlpbnRfbWluPTEgc2NlbmVjdXQ9NDAgaW50cmFfcmVmcmVzaD0wIHJjX2xvb2thaGVhZD01IHJjPWNyZiBtYnRyZWU9MSBjcmY9MjMuMCBxY2"+
            "9tcD0wLjYwIHFwbWluPTAgcXBtYXg9NjkgcXBzdGVwPTQgaXBfcmF0aW89MS40MCBhcT0xOjEuMDAAgAAAAB9liIQP8RigAC0jHAAFXKOAAIYM"+
            "nJyddddddddddddeAQCc2T0VRLoZRLsXZL59vn/+L/z/XrjjWrkf9P0//D/z/HnjzepdZ/0/P/p/8/XXHF9VqT/0/P/p/9/rrXHGl0mXRijwMu"+
            "jEnHLoxR4aZmwxXNNMuZczzTTJZo2WX5D9eC7BA4SjCdZzoqqMY1Bo6KI6DdA6hQboN1T0UVhStmyLe2bNhHfSLhG0S4vsnsI7NmyK/IkcFZcy"+
            "znYjzznOdSlRqUqeeeeedU6p1TqnnnnUJ1KVPPPPsnUb6N9FFL86l60odjnxtdg3xO4fgpzqh5uadU8/NPzfJU5EhB0fLYocAAAABkGaOB/hGA"+
            "ES8ori5mVnPPf5/6f/brXHF6ly5darS974qooD3IP/AGfQD/GYD30A/wA6E+4Ie/vohA+AHfcB/4MHfcg/8NhPuQP8Nge5A+bMB76APjME++gw"+
            "kv4eI/jHEFP4+E7+I8n2P4eJH+PhLBjAEgxAgEQiAATCoWDbEAlMBwAAAAZBmlQH+EYBAjKH5n+n9P1z7a7/T//l//xvWt9d39fbnj6/b+f7fU"+
            "eEZ14oFf+796im/e37ySm9TeUpoU379+++/fffv33sFQzhLfv376AgFw1BYKhYKABEIBcJgCoSACIQAEwmFABQAIAOAAAABkGaYD/CMAD8MoQg"+
            "EUIQbt/436d/p//o//l1fG8fd75OPn9f4+uHrvqpUA0rHp/tT0/2Jcs/9np9JJhJwMDAwMyBsGQMDA2LBUNoWiKAgEwmFgqFQkEglEIgCYAsFA"+
            "kAFwBMJhYLBngERS4DgAAAAAZBmoA/wjAAAAAgZYiCAR8RigADEjHAAFtaOAAIfMnJyddddddddddddeAA/jKH5n8/2/fPlnr//D//Pd8eNJ9/"+
            "nvj4/b/n/p8cV3lznXdAvw2bFVtmzpbYUiNmxs6IN2zZs2R2R2bNmzZYLBnCmzZs2UBELhsCxs2eULACQSBMIhYAZwqEgkCIABMKgCQARAcAAA"+
            "AGQZo4H+EYAP4yh+Z+/8fjP289/p//y//43enHr6rxmtfP+P48y+8tvSgWhv3qKb99N+8o37195Sl1N+/fvvvvffvusN9riwYoCgIhMLhYLBnC"+
            "gAQC4XDIAKBYASBAAXCYVCwbYhESmA4AAAAGQZpUB/hGASYyi+UQilVnf/9n/8+pq5ckkWiSIJsYrVj0oZX3LVj0oZX3Xqx6amVi/qBhVcKmV9"+
            "y1aelDK+69WPTUyvuvVj01Mr7r48cKrq+6+PHpq01yXx4rq01yX9WOFWmtF/ViurTWi/qxXVprRf1Yrq09KL+ruvq09KF9XctWnpQzqxvVp6am"+
            "dXdfPp6amR916semplfctWOCGTyLViEMrQtWOFTK5L1EuplYEAHAAAAABkGaYDvCMAFAMovmUVTPX2/ni7u5JJIkiRIIFc+VbJtyFQZVsm3IVB"+
            "labm3OBOV2EZWmc25EMCGmTLcVAhpuZ1yGB9puZbkMCGm5luQwPtc8zrkMD7XPMtyGBDXPMtyGBDXPMtzm3Ia55ls5oEK58q2c25DXPlWzm3IV"+
            "z5Vs5tyFc+VpnNucVBlaZzbnFc+Vpk25xUGVpk25xUD7TJnXFQPtMmdcVAhpky3IYH2m5nXIYENNzcAAAAAGQZqAN8Iw";
}
