package com.shazwan.sagadash;
import android.app.*;import android.content.*;import android.os.*;import android.graphics.*;import android.view.*;import android.widget.*;import java.io.*;
/** Actual secondary-display/input test using a test app in place of the Maps target. Not Google Maps. */
public final class MapsLayoutChecks extends Instrumentation {
 int assertions;DashboardActivity dash;DashNativeFrame maps,other;DashNativeFrame.Target original;
 interface Ready{boolean get()throws Exception;}
 void check(boolean ok,String text){if(!ok)throw new AssertionError(text);assertions++;android.util.Log.i("NexaMapsLayoutChecks",assertions+": "+text);}
 void ui(Runnable job){Throwable[] error={null};runOnMainSync(()->{try{job.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError(error[0]);}
 void await(Ready ready,String text)throws Exception{long deadline=SystemClock.elapsedRealtime()+45000;while(SystemClock.elapsedRealtime()<deadline){boolean[] ok={false};ui(()->{try{ok[0]=ready.get();}catch(Exception e){throw new RuntimeException(e);}});if(ok[0]){check(true,text);return;}SystemClock.sleep(150);}throw new AssertionError(text);}
 int query(DashNativeFrame frame,int code)throws Exception{Parcel a=Parcel.obtain(),b=Parcel.obtain();try{a.writeInterfaceToken(DashNativeFrame.TOKEN);frame.remote.transact(code,a,b,0);b.readException();return b.readInt();}finally{a.recycle();b.recycle();}}
 View named(View root,String text){if(root instanceof TextView&&text.contentEquals(((TextView)root).getText()))return root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){View v=named(g.getChildAt(i),text);if(v!=null)return v;}}return null;}
 void keyboardGone()throws Exception{long deadline=SystemClock.elapsedRealtime()+15000;boolean ime=true;while(ime&&SystemClock.elapsedRealtime()<deadline){ime=false;for(android.view.accessibility.AccessibilityWindowInfo w:getUiAutomation().getWindows()){if(w.getType()==android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD)ime=true;w.recycle();}if(ime)SystemClock.sleep(100);}check(!ime,"keyboard touch region is absent before tapping a host control");}
 void touch(View view)throws Exception{
  keyboardGone();
  float[] p={0,0};await(()->view.isShown()&&view.getWidth()>0,"native host control visible before touch");
  ui(()->{int[] xy=new int[2];view.getLocationOnScreen(xy);android.graphics.Rect r=new android.graphics.Rect();view.getLocalVisibleRect(r);p[0]=xy[0]+r.exactCenterX();p[1]=xy[1]+r.exactCenterY();});
  long down=SystemClock.uptimeMillis();for(int action:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,p[0],p[1],0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{check(getUiAutomation().injectInputEvent(e,true),"native toolbar touch uses Android routing");}finally{e.recycle();}SystemClock.sleep(100);}waitForIdleSync();
 }
 void layout(int index)throws Exception{
  touch(named(maps,"⋯"));
  touch(named(maps,"Maps layout / permissions"));
  await(()->dash.getWindow().getDecorView().hasWindowFocus()==false,"Maps setup dialog receives focus");
  // Pick the displayed native list through accessibility, retaining actual Android dialog routing.
  String text=index==0?"Spacious map layout (default)":"Original compact layout";
  long deadline=SystemClock.elapsedRealtime()+15000;boolean clicked=false;
  while(!clicked&&SystemClock.elapsedRealtime()<deadline){android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root!=null)try{java.util.List<android.view.accessibility.AccessibilityNodeInfo> items=root.findAccessibilityNodeInfosByText(text);for(android.view.accessibility.AccessibilityNodeInfo item:items){if(!clicked){android.view.accessibility.AccessibilityNodeInfo target=item;while(target!=null&&!target.isClickable()){android.view.accessibility.AccessibilityNodeInfo parent=target.getParent();if(target!=item)target.recycle();target=parent;}if(target!=null){clicked=target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);if(target!=item)target.recycle();}}item.recycle();}}finally{root.recycle();}if(!clicked)SystemClock.sleep(100);}
  check(clicked,"choose Maps layout via actual dialog accessibility action");
 }
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle out=new Bundle();try{runChecks();out.putString("stream","PASS: "+assertions+" Maps-layout host assertions (separate-UID native test apps, real displays and touch; not Google Maps rendering)\n");finish(Activity.RESULT_OK,out);}catch(Throwable e){android.util.Log.e("NexaMapsLayoutChecks","failed",e);out.putString("stream","FAIL after "+assertions+": "+e+"\n"+(maps==null?"":("visible="+dash.visible+" wanted="+maps.wanted+" confirmed="+maps.targetConfirmed+" remote="+(maps.remote!=null)))+"\n"+(dash==null?"":DashNativeFrame.lastStartupResult(dash))+"\n");finish(Activity.RESULT_CANCELED,out);}finally{if(dash!=null)ui(()->{dash.leftPane.setMode("car");dash.rightPane.setMode("media");});if(original!=null)DashNativeFrame.TARGETS[3]=original;}}
 void tapCounter()throws Exception{
  keyboardGone();
  float[] point={0,0};await(()->maps.surface.isShown()&&maps.surface.getWidth()>0,"Maps surface visible before primary-display touch");
  ui(()->{int[] xy=new int[2];maps.surface.getLocationOnScreen(xy);point[0]=xy[0]+maps.surface.getWidth()/2f;point[1]=xy[1]+92f/maps.renderHeight*maps.surface.getHeight();});
  long deadline=SystemClock.elapsedRealtime()+10000;boolean painted=false;
  while(!painted&&SystemClock.elapsedRealtime()<deadline){android.graphics.Bitmap image=getUiAutomation().takeScreenshot();if(image!=null){int color=image.getPixel(Math.round(point[0]),Math.round(point[1]))&0xffffff;painted=color==0x076f88;if(!painted)try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"maps-layout-touch.png"))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}image.recycle();}if(!painted)SystemClock.sleep(200);}
  check(painted,"counter is actually rendered and unobscured before touch");
  long down=SystemClock.uptimeMillis();for(int action:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,point[0],point[1],0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{check(getUiAutomation().injectInputEvent(e,true),"touch injected through Android routing");}finally{e.recycle();}SystemClock.sleep(80);}
 }
 void runChecks()throws Exception{
  android.accessibilityservice.AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;getUiAutomation().setServiceInfo(info);
  original=DashNativeFrame.TARGETS[3];DashNativeFrame.TARGETS[3]=DashNativeFrame.TARGETS[5];
  dash=(DashboardActivity)startActivitySync(new Intent(getTargetContext(),DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  ui(()->{dash.getSharedPreferences("native_panel_layout",0).edit().clear().commit();dash.leftPane.selectNativeTarget(6);dash.rightPane.selectNativeTarget(3);maps=dash.rightPane.nativeFrame;other=dash.leftPane.nativeFrame;check(maps.mapsSpacious,"Maps starts in spacious layout");maps.start();other.start();});
  await(()->maps.targetConfirmed&&other.targetConfirmed,"both normal-UID apps confirm secondary displays");
  int first=maps.requestedDisplay,otherId=other.requestedDisplay;
  check(first>0&&otherId>0&&first!=otherId,"separate display IDs");check(maps.renderHeight>=480,"Maps receives at least480px logical height at normal panel shape");check(Math.max(maps.renderWidth,maps.renderHeight)<=800,"Maps raster capped at800px");
  Point actual=new Point();ui(()->maps.display.getDisplay().getSize(actual));check(actual.x==maps.renderWidth&&actual.y==maps.renderHeight,"actual Android display follows Maps raster");
  int before=query(maps,2),untouched=query(other,2);final int firstExpected=before+1;tapCounter();await(()->{try{return query(maps,2)==firstExpected;}catch(Exception e){return false;}},"scaled Maps touch reaches actual native view");check(query(other,2)==untouched,"Maps touch leaves other app unchanged");
  layout(1);await(()->maps.targetConfirmed&&!maps.mapsSpacious&&maps.requestedDisplay!=first,"compact layout restarts only Maps with a new confirmed display");check(other.requestedDisplay==otherId&&other.targetConfirmed,"other app remains live across Maps layout change");
  int compact=maps.renderHeight,second=maps.requestedDisplay;
  layout(0);await(()->maps.targetConfirmed&&maps.mapsSpacious&&maps.requestedDisplay!=second,"spacious layout restores through clean session restart");check(maps.renderHeight>compact,"spacious layout provides more content height");
  before=query(maps,2);final int expected=before+1;tapCounter();await(()->{try{return query(maps,2)==expected;}catch(Exception e){return false;}},"touch works after layout restart");
  ui(()->maps.stop());layout(1);check(!maps.wanted&&maps.display==null,"changing stopped Maps layout does not auto-start");
  ui(()->{dash.rightPane.selectNativeTarget(3);maps=dash.rightPane.nativeFrame;check(!maps.mapsSpacious,"Maps layout choice survives view replacement");maps.start();});await(()->maps.targetConfirmed,"saved compact layout starts successfully");
  ui(()->{dash.leftPane.setMode("media");dash.rightPane.setMode("media");});check(maps.display==null&&maps.inputThread==null&&!maps.bound,"Maps teardown releases display, input worker and binding");
 }
}
