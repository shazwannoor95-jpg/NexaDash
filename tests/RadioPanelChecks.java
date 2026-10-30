package com.shazwan.sagadash;

import android.app.*;import android.content.*;import android.database.Cursor;import android.net.Uri;import android.os.*;import android.view.*;import android.widget.*;
import java.io.*;import java.lang.reflect.Field;import java.util.*;import org.json.*;

/** Actual native dashboard and cross-UID service IPC; firmware doubles never touch a tuner. */
public final class RadioPanelChecks extends Instrumentation {
    int assertions,fixtureSystemUiDialogs;DashboardActivity dash;DashFmPanel panel;
    interface Ready{boolean get()throws Exception;}
    void check(boolean value,String text){if(!value)throw new AssertionError(text);assertions++;}
    void ui(Runnable work){runOnMainSync(work);}
    void await(Ready ready,String text)throws Exception{long end=SystemClock.elapsedRealtime()+30000;while(SystemClock.elapsedRealtime()<end){if(ready.get()){check(true,text);return;}SystemClock.sleep(100);}throw new AssertionError(text);}
    void awaitUi(Ready ready,String text)throws Exception{await(()->{boolean[] ok={false};ui(()->{try{ok[0]=ready.get();}catch(Exception problem){throw new RuntimeException(problem);}});return ok[0];},text);}
    Uri uri(String pkg){return Uri.parse("content://"+pkg+".nexatest/events");}
    void reset(String pkg,String mode){getTargetContext().getContentResolver().call(uri(pkg),"reset",mode,null);}
    List<JSONObject> rows(String pkg)throws Exception{List<JSONObject> data=new ArrayList<>();try(Cursor c=getTargetContext().getContentResolver().query(uri(pkg),null,null,null,null)){while(c.moveToNext())data.add(new JSONObject(c.getString(0)));}return data;}
    List<JSONObject> commands(String pkg)throws Exception{List<JSONObject> result=new ArrayList<>();for(JSONObject row:rows(pkg))if("service".equals(row.getString("kind")))result.add(row);return result;}
    AlertDialog dialog()throws Exception{Field f=DashFmPanel.class.getDeclaredField("dialog");f.setAccessible(true);return (AlertDialog)f.get(panel);}
    void tap(View view)throws Exception{float[] point=new float[2];ui(()->{android.graphics.Rect r=new android.graphics.Rect();check(view.isShown()&&view.getGlobalVisibleRect(r)&&!r.isEmpty(),"FM control is visible");point[0]=r.exactCenterX();point[1]=r.exactCenterY();});long down=SystemClock.uptimeMillis();for(int action:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,point[0],point[1],0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{check(getUiAutomation().injectInputEvent(e,true),"FM control touch uses Android input routing");}finally{e.recycle();}SystemClock.sleep(60);}}
    View described(View view,String value){if(value.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;if(view instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)view;for(int i=0;i<g.getChildCount();i++){View found=described(g.getChildAt(i),value);if(found!=null)return found;}}return null;}
    EditText entry(View view){if(view instanceof EditText)return (EditText)view;if(view instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)view;for(int i=0;i<g.getChildCount();i++){EditText found=entry(g.getChildAt(i));if(found!=null)return found;}}return null;}
    boolean dismissStartupSystemUiDialog(){
        android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root==null)return false;
        try{if(!"android".contentEquals(root.getPackageName()))return false;
            List<android.view.accessibility.AccessibilityNodeInfo> title=root.findAccessibilityNodeInfosByText("System UI isn't responding");boolean matches=!title.isEmpty();for(android.view.accessibility.AccessibilityNodeInfo n:title)n.recycle();if(!matches)return false;
            List<android.view.accessibility.AccessibilityNodeInfo> buttons=root.findAccessibilityNodeInfosByText("Close app");boolean clicked=false;for(android.view.accessibility.AccessibilityNodeInfo n:buttons){if(!clicked)clicked=n.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);n.recycle();}if(clicked)fixtureSystemUiDialogs++;return clicked;
        }finally{root.recycle();}
    }
    void edit(String value)throws Exception{tap(panel.frequency);awaitUi(()->dialog()!=null,"frequency editor opens");ui(()->{try{entry(dialog().getWindow().getDecorView()).setText(value);}catch(Exception failure){throw new RuntimeException(failure);}});AlertDialog d=dialog();tap(d.getButton(AlertDialog.BUTTON_POSITIVE));}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{runChecks();result.putString("stream","PASS: "+assertions+" Radio panel API27 assertions (native UI, two real service UIDs, units, presets, stop/lifecycle; firmware doubles, not FM hardware)\n");finish(Activity.RESULT_OK,result);}catch(Throwable problem){android.util.Log.e("NexaRadioPanelChecks","Failed",problem);String details="";try{details="\n"+DashFmController.report(getTargetContext())+"\ninternal="+rows("com.android.fmradio")+"\nexternal="+rows("com.acloud.stub.extradio");String[] owner={""};ui(()->owner[0]="\nvisible="+dash.visible+" focus="+dash.getWindow().getDecorView().hasWindowFocus()+" mode="+dash.rightPane.mode+" stage="+panel.controller.phase+" message="+panel.controller.message);details+=owner[0];android.view.accessibility.AccessibilityNodeInfo node=getUiAutomation().getRootInActiveWindow();if(node!=null){details+="\nactiveRoot="+node.getPackageName()+"/"+node.getClassName();node.recycle();}android.graphics.Bitmap shot=getUiAutomation().takeScreenshot();if(shot!=null){try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"fm-panel-failure.png"))){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{shot.recycle();}}}catch(Exception ignored){}result.putString("stream","FAIL after "+assertions+": "+problem+details+"\n");finish(Activity.RESULT_CANCELED,result);}finally{if(dash!=null)ui(()->{dash.leftPane.setMode("car");dash.rightPane.setMode("media");});}}
    void runChecks()throws Exception{
        final String internal="com.android.fmradio",external="com.acloud.stub.extradio";
        check(getTargetContext().getPackageName().equals("com.shazwan.nexadash.next"),"test actual Next release");
        int hostUid=getTargetContext().getApplicationInfo().uid;
        int internalUid=getTargetContext().getPackageManager().getApplicationInfo(internal,0).uid,externalUid=getTargetContext().getPackageManager().getApplicationInfo(external,0).uid;
        check(hostUid>=10000&&internalUid>=10000&&externalUid>=10000&&hostUid!=internalUid&&hostUid!=externalUid&&internalUid!=externalUid,"host and both firmware doubles have distinct normal app UIDs");
        reset(internal,"normal");reset(external,"normal");
        getTargetContext().getSharedPreferences(DashFmController.PREFS,0).edit().clear().putString("source","auto").putInt("frequency_khz",93900).commit();
        dash=(DashboardActivity)startActivitySync(new Intent(getTargetContext(),DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ui(()->{dash.leftPane.setMode("car");dash.shortcut("Radio");panel=dash.rightPane.fmPanel;check(panel!=null&&"radio".equals(dash.rightPane.mode),"Radio dock selects native right panel");check("car".equals(dash.leftPane.mode),"Radio dock preserves other panel");});
        awaitUi(()->panel.power.isShown()&&panel.power.getWidth()>0,"FM controls measure in dashboard");
        await(()->{dismissStartupSystemUiDialog();boolean[] focused={false};ui(()->focused[0]=dash.getWindow().getDecorView().hasWindowFocus());return focused[0];},"dashboard input focus ready before Radio feature actions");
        check(commands(internal).isEmpty()&&commands(external).isEmpty(),"selection/resume do not start FM implicitly");
        ui(()->{android.graphics.Rect r=new android.graphics.Rect(),p=new android.graphics.Rect();panel.getGlobalVisibleRect(p);View last=described(panel,"FM preset 6; hold to save");check(last!=null&&last.getGlobalVisibleRect(r)&&p.contains(r),"bottom preset row stays inside actual panel");});
        tap(panel.power);await(()->commands(internal).size()>=2,"internal initialization completes then tunes");
        List<JSONObject> start=commands(internal);check(start.get(0).getInt("flag")==0&&start.get(1).getInt("flag")==1,"cold FM start opens/scans before tuning");
        check(Math.abs(start.get(1).getDouble("mhz")-93.9)<.001,"internal tune uses MHz float, not kHz");
        List<JSONObject> all=rows(internal);int full=-1,tune=-1;for(int i=0;i<all.size();i++){JSONObject r=all.get(i);if("complete".equals(r.getString("kind")))full=i;if("service".equals(r.getString("kind"))&&r.getInt("flag")==1)tune=i;}check(full>=0&&tune>full,"partial progress cannot trigger premature tune");
        awaitUi(()->!panel.controller.initializing&&panel.controller.stations.length==3,"only valid unique scan stations are offered");
        edit("200");ui(()->{try{check(dialog()!=null&&entry(dialog().getWindow().getDecorView()).getError()!=null,"out-of-band editor value is rejected visibly");check(panel.controller.frequency==93900,"invalid input cannot change tuner frequency");dialog().dismiss();}catch(Exception error){throw new RuntimeException(error);}});
        int before=commands(internal).size();edit("101.8");await(()->commands(internal).size()==before+1,"valid editor tune reaches real service");
        tap(described(panel,"Tune up 0.1 MHz"));awaitUi(()->panel.controller.frequency==101900,"native step control adds0.1MHz");
        View preset=described(panel,"FM preset 1; hold to save");ui(()->check(preset.performLongClick(),"hold saves a preset"));
        tap(described(panel,"Tune down 0.1 MHz"));tap(preset);awaitUi(()->panel.controller.frequency==101900,"saved preset recalls selected frequency");
        tap(panel.power);await(()->{List<JSONObject> data=commands(internal);return data.get(data.size()-1).getInt("flag")==2;},"Stop uses OEM close flag");
        int stopped=commands(internal).size();getTargetContext().sendBroadcast(new Intent(DashFmController.SCAN_RESULT).putExtra("freq",new int[]{9990}));SystemClock.sleep(400);check(commands(internal).size()==stopped,"late firmware replies after Stop cannot retune");
        reset(internal,"normal");tap(panel.power);tap(panel.power);SystemClock.sleep(1800);for(JSONObject command:commands(internal))check(command.getInt("flag")!=1,"Stop during initialization prevents late queued tune");
        tap(panel.source);awaitUi(()->dialog()!=null,"FM source chooser opens");ui(()->{try{android.widget.ListView list=dialog().getListView();list.performItemClick(list.getAdapter().getView(2,null,list),2,2);}catch(Exception error){throw new RuntimeException(error);}});
        tap(panel.power);await(()->commands(external).size()>=2,"external FM opens and receives frequency");
        List<JSONObject> ext=commands(external);check("method_open".equals(ext.get(0).getString("method"))&&"method_setFreq".equals(ext.get(1).getString("method")),"external uses recovered explicit service methods");check(ext.get(1).getInt("khz")==101900,"external uses integer kHz");
        int beforeResize=commands(external).size();
        ui(()->panel.setLayoutParams(new android.widget.FrameLayout.LayoutParams(-1,dash.px(170))));awaitUi(()->panel.getHeight()==dash.px(170),"FM panel measures at compact head-unit size");
        ui(()->{android.graphics.Rect p=new android.graphics.Rect(),r=new android.graphics.Rect();panel.getGlobalVisibleRect(p);check(panel.power.getGlobalVisibleRect(r)&&p.contains(r),"compact power control remains inside panel");check(panel.frequency.getHeight()>dash.px(20),"compact panel keeps a readable frequency area");});
        check(commands(external).size()==beforeResize,"resizing FM UI sends no tuner commands");
        ui(()->panel.setLayoutParams(new android.widget.FrameLayout.LayoutParams(-1,-1)));awaitUi(()->panel.getHeight()>dash.px(230),"expanded FM panel fills slot again");
        ui(()->{dash.leftPane.selectNativeTarget(5);check(panel==dash.rightPane.fmPanel&&panel.controller.owned,"selecting a different native panel preserves FM session");dash.leftPane.setMode("car");});
        check(DashFmController.report(getTargetContext()).contains("com.acloud.stub.extradio")&&DashFmController.report(getTargetContext()).contains("hardware"),"copyable report records FM stage without claiming hardware success");
        SystemClock.sleep(400);android.graphics.Bitmap image=getUiAutomation().takeScreenshot();check(image!=null,"capture actual FM dashboard UI");try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"fm-panel-api27.png"))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}
        ui(()->dash.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS)));await(()->{List<JSONObject> data=commands(external);return "method_close".equals(data.get(data.size()-1).optString("method"));},"leaving dashboard closes only the owned FM session");
        ui(()->dash.startActivity(new Intent(dash,DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));awaitUi(()->dash.visible,"dashboard returns");
        int returned=commands(external).size();SystemClock.sleep(300);ui(()->check(!panel.controller.owned,"returning does not auto-start FM"));check(commands(external).size()==returned,"no command sent by resume");
        getTargetContext().getContentResolver().call(uri(external),"disable",null,null);tap(panel.power);awaitUi(()->"error".equals(panel.controller.phase),"disabled/missing service fails without opening an app");getTargetContext().getContentResolver().call(uri(external),"enable",null,null);
        for(String pkg:new String[]{internal,external})for(JSONObject row:rows(pkg))check(!"activity".equals(row.getString("kind")),"no original FM activity launched");
        ui(()->{panel.controller.source("internal");dash.leftPane.setMode("radio");check("media".equals(dash.rightPane.mode)&&dash.leftPane.fmPanel!=null,"one FM panel owns tuner at a time");});
    }
}
