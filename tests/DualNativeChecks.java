package com.shazwan.sagadash;

import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;
import java.io.*;import java.util.*;
/** Actual two-app/two-UID display and input isolation checks. Never counts these as Maps/TizenTube validation. */
public class DualNativeChecks extends Instrumentation {
 int assertions;DashboardActivity dash;
 interface Ready{boolean get();}
 void check(boolean ok,String message){if(!ok)throw new AssertionError(message);assertions++;}
 void ui(Runnable run){runOnMainSync(run);}
 void await(Ready ready,String message)throws Exception{long end=SystemClock.elapsedRealtime()+45000;boolean[] ok={false};while(!ok[0]&&SystemClock.elapsedRealtime()<end){ui(()->ok[0]=ready.get());if(!ok[0])SystemClock.sleep(150);}check(ok[0],message);}
 int query(IBinder service,int code)throws Exception{Parcel a=Parcel.obtain(),b=Parcel.obtain();try{a.writeInterfaceToken(DashNativeFrame.TOKEN);service.transact(code,a,b,0);b.readException();return b.readInt();}finally{a.recycle();b.recycle();}}
 String text(IBinder service)throws Exception{Parcel a=Parcel.obtain(),b=Parcel.obtain();try{a.writeInterfaceToken(DashNativeFrame.TOKEN);service.transact(7,a,b,0);b.readException();return b.readString();}finally{a.recycle();b.recycle();}}
 Button button(View v,String name){if(v instanceof Button&&name.contentEquals(((Button)v).getText()))return (Button)v;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int n=0;n<g.getChildCount();n++){Button found=button(g.getChildAt(n),name);if(found!=null)return found;}}return null;}
 void tap(DashNativeFrame f,float y)throws Exception{long down=SystemClock.uptimeMillis();float[] point=new float[2];ui(()->{int[] xy=new int[2];f.surface.getLocationOnScreen(xy);point[0]=xy[0]+f.surface.getWidth()/2f;point[1]=xy[1]+f.surface.getHeight()*y/f.renderHeight;});for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,point[0],point[1],0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{check(getUiAutomation().injectInputEvent(e,true),"primary-screen touch injected through Android input routing");}finally{e.recycle();}SystemClock.sleep(60);}}
 void awaitCount(IBinder service,int value,String message)throws Exception{long end=SystemClock.elapsedRealtime()+10000;while(query(service,2)!=value&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);check(query(service,2)==value,message);}
 void awaitText(IBinder service,String value,String message)throws Exception{long end=SystemClock.elapsedRealtime()+10000;while(!value.equals(text(service))&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);check(value.equals(text(service)),message);}
 boolean rendered(android.graphics.Bitmap image,DashNativeFrame frame,int color){android.graphics.Rect bounds=new android.graphics.Rect();ui(()->frame.surface.getGlobalVisibleRect(bounds));int matched=0,total=0;for(int y=Math.max(0,bounds.top);y<Math.min(image.getHeight(),bounds.bottom);y+=3)for(int x=Math.max(0,bounds.left);x<Math.min(image.getWidth(),bounds.right);x+=3){total++;if((image.getPixel(x,y)&0x00ffffff)==(color&0x00ffffff))matched++;}return total>0&&matched>total*.08;}
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle out=new Bundle();try{runChecks();out.putString("stream","PASS: "+assertions+" assertions (two real native test apps, separate UIDs/displays, touch/text, resize, failure, cleanup; not Maps/TizenTube playback)\n");finish(Activity.RESULT_OK,out);}catch(Throwable failure){android.util.Log.e("NexaDualChecks","Failed",failure);out.putString("stream","FAIL after "+assertions+": "+failure+"\n"+(dash==null?"":DashNativeFrame.lastStartupResult(dash)+"\nLeft: "+(dash.leftPane.nativeFrame==null?"none":dash.leftPane.nativeFrame.status.getText())+"\nRight: "+(dash.rightPane.nativeFrame==null?"none":dash.rightPane.nativeFrame.status.getText()))+"\n");finish(Activity.RESULT_CANCELED,out);}finally{if(dash!=null)ui(()->{dash.leftPane.setMode("car");dash.rightPane.setMode("media");});}}
 void runChecks()throws Exception{
  check(getTargetContext().getPackageName().equals("com.shazwan.nexadash.next"),"checks target the actual signed Next host");
  dash=(DashboardActivity)startActivitySync(new Intent(getTargetContext(),DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  ui(()->{dash.leftPane.setMode("car");dash.rightPane.setMode("media");dash.leftPane.selectNativeTarget(5);dash.rightPane.selectNativeTarget(6);
   check(dash.leftPane.nativeFrame!=null&&dash.rightPane.nativeFrame!=null,"both native host views coexist");
   check(!dash.leftPane.nativeFrame.token.equals(dash.rightPane.nativeFrame.token),"independent proxy tokens");
   check(dash.leftPane.nativeFrame.display==null&&dash.rightPane.nativeFrame.display==null,"selection does not auto-start either app");
   dash.leftPane.nativeFrame.start();dash.rightPane.nativeFrame.start();});
  await(()->dash.leftPane.nativeFrame.targetConfirmed&&dash.rightPane.nativeFrame.targetConfirmed,"both real companion Activities confirm private displays");
  DashNativeFrame left=dash.leftPane.nativeFrame,right=dash.rightPane.nativeFrame;IBinder a=left.remote,b=right.remote;
  int leftId=query(a,3),rightId=query(b,3);
  check(leftId>0&&rightId>0&&leftId!=rightId,"two distinct non-primary displays");
  check(leftId==left.requestedDisplay&&rightId==right.requestedDisplay,"both actual app display IDs match their own request");
  int uidA=getTargetContext().getPackageManager().getApplicationInfo(DashNativeFrame.TARGETS[5].pkg,0).uid,uidB=getTargetContext().getPackageManager().getApplicationInfo(DashNativeFrame.TARGETS[6].pkg,0).uid;
  check(uidA!=uidB&&uidA!=getTargetContext().getApplicationInfo().uid&&uidB!=getTargetContext().getApplicationInfo().uid,"host and both test apps have three independent UIDs");
  ui(()->{check(dash.leftPane.nativeFrame!=dash.rightPane.nativeFrame,"independent native sessions");check(dash.leftPane.nativeFrame.inputThread!=dash.rightPane.nativeFrame.inputThread,"independent input workers");});
  int beforeA=query(a,2),beforeB=query(b,2);tap(left,92);awaitCount(a,beforeA+1,"left touch updates the left real app");check(query(b,2)==beforeB,"left touch does not reach the right app");tap(right,92);awaitCount(b,beforeB+1,"right touch updates the right real app");check(query(a,2)==beforeA+1,"right touch does not reach the left app");
  int oldHeight=left.renderHeight,oldWidth=left.renderWidth;ui(()->button(left,"⋯").performClick());await(()->left.renderHeight!=oldHeight||left.renderWidth!=oldWidth,"opening input tools resizes the left raster");
  check(query(b,3)==rightId&&right.targetConfirmed,"left resize keeps right display and session");tap(left,150);ui(()->{left.entry.setText("left-only");button(left,"Send").performClick();});awaitText(a,"left-only","left text is committed into left app editor");check(text(b).isEmpty(),"left text is absent from right editor");
  ui(()->button(right,"⋯").performClick());SystemClock.sleep(1000);tap(right,150);ui(()->{right.entry.setText("right-only");button(right,"Send").performClick();});awaitText(b,"right-only","right text is committed into right app editor");check(text(a).equals("left-only"),"right text leaves left editor unchanged");
  ui(()->{String report=DashNativeFrame.lastStartupResult(dash);check(report.contains("Left panel")&&report.contains("Right panel")&&report.contains(DashNativeFrame.TARGETS[5].pkg)&&report.contains(DashNativeFrame.TARGETS[6].pkg),"diagnostic retains both app startup results");});
  SystemClock.sleep(1000);android.graphics.Bitmap image=getUiAutomation().takeScreenshot();check(image!=null,"capture real dual-native dashboard");
  try{File dest=new File(getTargetContext().getExternalFilesDir(null),"dual-native-api27.png");try(FileOutputStream stream=new FileOutputStream(dest)){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);}
   check(rendered(image,left,0xff103b55),"left native app really renders into its panel, not a blank surface ("+image.getWidth()+"x"+image.getHeight()+")");check(rendered(image,right,0xff3c204c),"right native app really renders into its panel, not a blank surface");
  }finally{image.recycle();}
  ui(()->{right.select(5);right.start();check(!right.wanted&&right.display==null&&right.status.getText().toString().contains("already"),"duplicate package is rejected without stealing the first display");check(left.targetConfirmed&&left.display.getDisplay().getDisplayId()==leftId,"duplicate failure keeps the original app running");right.select(6);right.start();});
  await(()->right.targetConfirmed,"right app restarts after duplicate selection");
  ui(()->{right.proxyDisplayFailed(right.requestedDisplay,0);check(right.display==null&&right.inputThread==null&&!right.bound,"failed right session releases its own resources");check(left.targetConfirmed&&left.display!=null&&left.remote!=null,"right failure leaves left session running");
   android.content.SharedPreferences l=dash.getSharedPreferences(DashNativeFrame.STARTUP_PREFS+"_vehicle",0),r=dash.getSharedPreferences(DashNativeFrame.STARTUP_PREFS+"_media",0);check(l.getString("result","").equals("confirmed")&&r.getString("result","").contains("failed"),"a failed right record does not overwrite the confirmed left record");right.select(6);right.start();});
  await(()->right.targetConfirmed,"right restarts after isolated failure");
  ui(()->{left.stop();check(left.display==null&&left.inputThread==null&&left.remote==null&&!left.bound,"left Stop releases its display/worker/binding");check(right.targetConfirmed&&right.display!=null,"left Stop keeps right app live");left.resume();check(left.display==null&&!left.wanted,"resume does not auto-start the stopped app");dash.leftPane.setMode("media");check(dash.leftPane.nativeFrame==null&&right.targetConfirmed,"switching left mode keeps right native app");dash.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));});
  await(()->right.display==null&&!right.wanted&&right.inputThread==null,"leaving dashboard stops remaining native session");
  ui(()->dash.startActivity(new Intent(dash,DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));
  await(()->dash.visible,"dashboard returns from Settings");ui(()->check(right.display==null&&!right.wanted,"returning to dashboard requires explicit Start"));
  check(DashboardActivity.inlineConflict("youtube_web","native_app")&&DashboardActivity.inlineConflict("video_file","native_app")&&!DashboardActivity.inlineConflict("native_app","native_app"),"decoder exclusivity retained while allowing dual native");
 }
}
