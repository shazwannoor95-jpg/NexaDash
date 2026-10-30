package com.shazwan.sagadash;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.zip.*;

/** Actual API27 settings action/result and content-provider ZIP write. No FM hardware calls. */
public class RadioExportChecks extends Instrumentation {
    static final Uri DEST=Uri.parse("content://com.shazwan.nexadash.radiochecks.files/radio.zip");
    int assertions;DashboardActivity dash;
    void check(boolean value,String message){if(!value)throw new AssertionError(message);assertions++;}
    void ui(Runnable work){runOnMainSync(work);}
    AlertDialog dialog()throws Exception{Field f=DashRadioExport.class.getDeclaredField("dialog");f.setAccessible(true);return (AlertDialog)f.get(dash.radioExport);}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle out=new Bundle();try{runChecks();out.putString("stream","PASS: "+assertions+" Radio export API27 assertions (settings, save-picker handoff, local ZIP and read failures; not FM control)\n");finish(Activity.RESULT_OK,out);}
        catch(Throwable failure){android.util.Log.e("NexaRadioChecks","Failed",failure);out.putString("stream","FAIL after "+assertions+": "+failure+"\n");finish(Activity.RESULT_CANCELED,out);}
        finally{if(dash!=null)ui(()->{dash.closeSettings();if(dash.radioExport!=null)dash.radioExport.close();});}}
    void runChecks()throws Exception{
        dash=(DashboardActivity)startActivitySync(new Intent(getTargetContext(),DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        check(getTargetContext().getPackageName().equals("com.shazwan.nexadash.next"),"actual Next release installed");
        check(getTargetContext().getApplicationInfo().uid>=10000,"export runs as normal app UID");
        IntentFilter filter=new IntentFilter(Intent.ACTION_CREATE_DOCUMENT);filter.addCategory(Intent.CATEGORY_OPENABLE);filter.addDataType("application/zip");
        ActivityMonitor monitor=addMonitor(filter,new ActivityResult(Activity.RESULT_CANCELED,null),true);
        try{
            ui(()->{
                dash.settings();DashSettingsGrid grid=dash.settingsGrid;ListAdapter adapter=grid.getAdapter();
                check(adapter.getCount()==19&&"Export Radio files".equals(adapter.getItem(18)),"new export tile appended");
                check("Radio runtime paths".equals(adapter.getItem(17))&&"App panel compatibility".equals(adapter.getItem(13)),"existing setting indices preserved");
                grid.performItemClick(adapter.getView(18,null,grid),18,18);
                check(dash.settingsDialog==null&&dash.radioExport!=null,"export tile closes drawer and invokes exporter");
            });waitForIdleSync();check(monitor.getHits()==1,"Android ZIP save picker requested exactly once");
            check(dialog()==null,"cancelling picker performs no export");
        }finally{removeMonitor(monitor);}
        ui(()->dash.onActivityResult(DashRadioExport.REQUEST,Activity.RESULT_OK,new Intent().setData(DEST)));
        long deadline=SystemClock.elapsedRealtime()+30000;boolean[] ready={false};
        while(!ready[0]&&SystemClock.elapsedRealtime()<deadline){ui(()->{try{ready[0]=dialog()!=null&&dialog().isShowing();}catch(Exception failure){throw new RuntimeException(failure);}});if(!ready[0])SystemClock.sleep(100);}
        check(ready[0],"background export completes with visible result");
        Set<String> names=new HashSet<>();String report="";int binaryFiles=0;
        try(InputStream input=getContext().getContentResolver().openInputStream(DEST);ZipInputStream zip=new ZipInputStream(input)){
            ZipEntry entry;while((entry=zip.getNextEntry())!=null){check(names.add(entry.getName()),"no duplicate ZIP entries");ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=zip.read(b))!=-1)bytes.write(b,0,n);
                if(entry.getName().equals("RADIO_EXPORT_REPORT.txt"))report=new String(bytes.toByteArray(),"UTF-8");
                else{check(Arrays.asList(DashRadioFiles.paths()).contains(entry.getName().substring("files".length())),"binary is one of the five allowlisted paths");binaryFiles++;}
            }
        }
        check(report.contains("Files copied: "+binaryFiles+"/5"),"ZIP report agrees with actual binary entries");
        check(report.contains("NEXADASH — RADIO RUNTIME PATHS")&&report.contains("API: 27"),"runtime path report included");
        check(report.contains("Property: "+DashRadioBackend.PROPERTY)&&report.contains("Selected component: "+DashRadioBackend.INTERNAL),"unset API27 selector reads firmware default and resolves actual FM component");
        check(report.contains("Host UID: "+getTargetContext().getApplicationInfo().uid),"device access flags identify the current normal host UID");
        for(String path:new String[]{"/dev/tef6606_i2c","/dev/keypad","/dev/fm"})check(report.contains(path+":"),"fixed driver metadata path included: "+path);
        check(report.contains("drivers not opened")&&report.contains("do not prove that a different companion UID"),"metadata check distinguished from tested driver access");
        check(report.contains("NOT INCLUDED")&&report.contains("FileNotFoundException"),"unavailable emulator firmware recorded without crashing");
        check(report.contains("does not prove FM control or native panel hosting"),"export not reported as working FM");
        File[] snapshots=getTargetContext().getCacheDir().listFiles((dir,name)->name.startsWith("radio-source-")||name.startsWith("nexadash-radio-"));
        check(snapshots!=null&&snapshots.length==0,"temporary files removed after save");
        ui(()->{try{check(label(dialog().getWindow().getDecorView(),"Radio files saved"),"saved ZIP distinguished from FM success");}catch(Exception failure){throw new RuntimeException(failure);}
            dash.radioExport.close();dash.radioExport.close();});
        waitForIdleSync();check(dialog()==null,"result dialog released idempotently");
        // Non-content inputs must be rejected rather than writing arbitrary paths.
        ui(()->dash.onActivityResult(DashRadioExport.REQUEST,Activity.RESULT_OK,new Intent().setData(Uri.parse("file:///data/local/tmp/unwanted.zip"))));
        check(!new File("/data/local/tmp/unwanted.zip").exists(),"arbitrary filesystem output rejected");
    }
    static boolean label(View view,String text){if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return true;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(label(group.getChildAt(i),text))return true;}return false;}
    public static final class FilesProvider extends ContentProvider {
        File target(){return new File(getContext().getCacheDir(),"radio-check-output.zip");}
        @Override public boolean onCreate(){return true;}
        @Override public String getType(Uri uri){return "application/zip";}
        @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{return ParcelFileDescriptor.open(target(),ParcelFileDescriptor.parseMode(mode));}
        @Override public int delete(Uri uri,String where,String[] values){return target().delete()?1:0;}
        @Override public Cursor query(Uri uri,String[] cols,String where,String[] values,String order){return null;}
        @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
        @Override public int update(Uri uri,ContentValues values,String where,String[] args){throw new UnsupportedOperationException();}
    }
}
