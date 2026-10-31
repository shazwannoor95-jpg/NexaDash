package com.shazwan.sagadash;
import android.app.*;import android.content.*;import android.os.*;import android.location.Location;import android.view.*;import android.widget.*;import org.json.*;import java.io.*;import java.net.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;
/** Offline HTTPS transport doubles; no real location, public geocoder, RF or Maps app executed. */
public class PanelMapChecks extends Instrumentation {
 int count,external;DashboardActivity dash;DashNavigationView map;Object directions;
 static volatile String mode="normal";static final List<URL> urls=Collections.synchronizedList(new ArrayList<>());static final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
 static final CountDownLatch routeEntered=new CountDownLatch(1),routeRelease=new CountDownLatch(1);
 static final String SEARCH="{\"features\":[{\"geometry\":{\"coordinates\":[101.70,3.14]},\"properties\":{\"name\":\"Fixture Station\",\"city\":\"Kuala Lumpur\"}},{\"geometry\":{\"coordinates\":[101,999]},\"properties\":{\"name\":\"Invalid\"}}]}";
 static final String ROUTE="{\"code\":\"Ok\",\"routes\":[{\"distance\":2220,\"duration\":300,\"geometry\":{\"coordinates\":[[101.69,3.14],[101.70,3.14],[101.71,3.14]]},\"legs\":[{\"steps\":[{\"distance\":1110,\"name\":\"First Street\",\"maneuver\":{\"type\":\"depart\",\"location\":[101.69,3.14]}},{\"distance\":1110,\"name\":\"Next Street\",\"maneuver\":{\"type\":\"turn\",\"modifier\":\"right\",\"location\":[101.70,3.14]}},{\"distance\":0,\"maneuver\":{\"type\":\"arrive\",\"location\":[101.71,3.14]}}]}]}]}";
 static class Connection extends HttpURLConnection {
  Connection(URL u){super(u);}public void disconnect(){}public boolean usingProxy(){return false;}public void connect(){}
  public int getResponseCode(){return url.getHost().equals("tile.openstreetmap.org")?503:200;}
  public InputStream getInputStream()throws IOException {urls.add(url);if(url.getHost().equals("router.project-osrm.org")&&mode.equals("blocked-route")){routeEntered.countDown();try{routeRelease.await(20,TimeUnit.SECONDS);}catch(InterruptedException interrupted){throw new IOException(interrupted);}}String value=url.getHost().equals("photon.komoot.io")?SEARCH:ROUTE;if(url.getHost().equals("photon.komoot.io")){if(mode.equals("blocked")){entered.countDown();try{release.await(20,TimeUnit.SECONDS);}catch(InterruptedException interrupted){throw new IOException(interrupted);}}if(mode.equals("oversize"))return new ByteArrayInputStream(new byte[2*1024*1024+1]);if(mode.equals("broken"))value="bad JSON";}return new ByteArrayInputStream(value.getBytes("UTF-8"));}
 }
 int routeCalls(){int n=0;synchronized(urls){for(URL u:urls)if(u.getHost().equals("router.project-osrm.org"))n++;}return n;}
 void check(boolean b,String why){if(!b)throw new AssertionError(why);count++;}
 interface Ready{boolean get()throws Exception;}
 void ui(Runnable run){Throwable[] problem={null};runOnMainSync(()->{try{run.run();}catch(Throwable t){problem[0]=t;}});if(problem[0]!=null)throw new AssertionError(problem[0]);}
 void await(Ready ready,String why)throws Exception{long end=SystemClock.elapsedRealtime()+30000;while(SystemClock.elapsedRealtime()<end){boolean[] ok={false};ui(()->{try{ok[0]=ready.get();}catch(Exception e){throw new RuntimeException(e);}});if(ok[0]){check(true,why);return;}SystemClock.sleep(100);}throw new AssertionError(why);}
 Object field(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
 void call(String name)throws Exception{Method m=directions.getClass().getDeclaredMethod(name);m.setAccessible(true);ui(()->{try{m.invoke(directions);}catch(Exception e){throw new RuntimeException(e);}});}
 AlertDialog dialog()throws Exception{return (AlertDialog)field(directions,"dialog");}
 View named(View root,String text){if(root instanceof TextView&&text.contentEquals(((TextView)root).getText()))return root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View found=named(group.getChildAt(i),text);if(found!=null)return found;}}return null;}
 void search()throws Exception{ui(()->named(map,"Search / route").performClick());AlertDialog d=dialog();ui(()->{EditText entry=d.getWindow().getDecorView().findViewWithTag("panel-destination-query");entry.setText("Station & Jalan");d.getButton(-1).performClick();});}
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle out=new Bundle();try{runChecks();out.putString("stream","PASS: "+count+" native map panel assertions (offline transport fixtures, parsing, search/route UI, lifecycle; not real navigation or Maps rendering)\n");finish(Activity.RESULT_OK,out);}catch(Throwable e){android.util.Log.e("NexaPanelMapChecks","failed",e);out.putString("stream","FAIL after "+count+": "+e+"\n");finish(Activity.RESULT_CANCELED,out);}finally{release.countDown();routeRelease.countDown();if(dash!=null)ui(()->dash.rightPane.setMode("media"));}}
 void runChecks()throws Exception{
  URL.setURLStreamHandlerFactory(protocol->"https".equals(protocol)?new URLStreamHandler(){protected URLConnection openConnection(URL url){return new Connection(url);}}:null);
  ActivityMonitor monitor=new ActivityMonitor(){public ActivityResult onStartActivity(Intent i){if(Intent.ACTION_VIEW.equals(i.getAction())){external++;return new ActivityResult(0,null);}return null;}};addMonitor(monitor);
  dash=(DashboardActivity)startActivitySync(new Intent(getTargetContext(),DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  ui(()->{dash.leftPane.setMode("car");dash.rightPane.setMode("navigation");map=dash.rightPane.navigation;});directions=field(map,"directions");
  await(()->map.getWidth()>100&&map.getHeight()>100,"native map fills the right panel");
  check(DashNavigationView.validCoordinate(3.14,101.69)&&!DashNavigationView.validCoordinate(Double.NaN,0),"finite coordinates only");
  double[][] parsed=DashNavigationView.parseRouteCoordinates(new JSONArray("[[101,3],[102,4]]"));check(parsed[0][0]==3&&parsed[0][1]==101,"GeoJSON lon/lat decoded in correct order");
  for(String bad:new String[]{"[]","[[101,3]]","[[101,99],[102,4]]","[[101,3],[\"oops\",4]]"}){boolean rejected=false;try{DashNavigationView.parseRouteCoordinates(new JSONArray(bad));}catch(Exception expected){rejected=true;}check(rejected,"invalid route response rejected");}
  ui(()->{Location fix=new Location("fixture");fix.setLatitude(3.14);fix.setLongitude(101.69);fix.setAccuracy(5);fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());fix.setTime(System.currentTimeMillis());map.setLocation(fix);});
  search();await(()->dialog()!=null&&dialog().getListView()!=null,"online search results render inside a native dialog");
  ui(()->{try{check(dialog().getListView().getAdapter().getCount()==1,"invalid destination coordinates omitted");ListView list=dialog().getListView();list.performItemClick(list.getAdapter().getView(0,null,list),0,0);}catch(Exception e){throw new RuntimeException(e);}});
  await(()->field(directions,"path")!=null,"destination creates route in the same panel");check(external==0,"search and route never open an external app");
  check(((double[][])field(directions,"path")).length==3,"route coordinates retained");check(((String)field(directions,"instruction")).contains("right"),"next turn displayed from route progress");
  boolean encoded=false;for(URL u:new ArrayList<>(urls))if(u.getHost().equals("photon.komoot.io"))encoded="Station & Jalan".equals(android.net.Uri.parse(u.toString()).getQueryParameter("q"));check(encoded,"query punctuation encoded without changing destination text");
  call("overview");check(!(Boolean)field(map,"following"),"route overview suspends GPS follow");ui(()->named(map,"GPS").performClick());check((Boolean)field(map,"following"),"GPS button returns to current location");
  View canvas=map.getChildAt(1);ui(()->{long t=SystemClock.uptimeMillis();for(int action:new int[]{0,2,1}){MotionEvent e=MotionEvent.obtain(t,t+action*10,action,100+(action==0?0:30),100,0);canvas.dispatchTouchEvent(e);e.recycle();}});check(!(Boolean)field(map,"following"),"drag pans native map");
  call("options");ui(()->{try{ListView list=dialog().getListView();list.performItemClick(list.getAdapter().getView(1,null,list),1,1);}catch(Exception e){throw new RuntimeException(e);}});check(field(directions,"path")==null,"clear route removes geometry");
  mode="blocked-route";call("route");check(routeEntered.await(10,TimeUnit.SECONDS),"pending route fixture entered");call("options");ui(()->{try{ListView list=dialog().getListView();list.performItemClick(list.getAdapter().getView(1,null,list),1,1);}catch(Exception e){throw new RuntimeException(e);}});routeRelease.countDown();SystemClock.sleep(300);check(field(directions,"path")==null&&!(Boolean)field(directions,"busy"),"Clear route prevents pending route from reappearing");
  int requests=routeCalls();ui(()->map.setLocation(null));call("route");check(field(directions,"path")==null&&routeCalls()==requests,"route never invents a departure without fresh GPS");
  mode="broken";search();await(()->!(Boolean)field(directions,"busy"),"malformed response ends loading");check(field(directions,"path")==null,"malformed response cannot invent a route");
  mode="oversize";search();await(()->!(Boolean)field(directions,"busy"),"oversized response is bounded and ends loading");check(dialog()==null,"oversized search cannot open results");
  mode="blocked";search();check(entered.await(10,TimeUnit.SECONDS),"pending network fixture entered");ui(()->map.pause());release.countDown();SystemClock.sleep(500);check(dialog()==null,"paused map cannot reopen late search results");check(!(Boolean)field(directions,"busy"),"pause clears request state");
  ui(()->map.resume());check(dialog()==null,"resume does not repeat search");ui(()->dash.rightPane.setMode("media"));check(((ThreadPoolExecutor)field(directions,"worker")).isShutdown(),"replacing panel shuts down request worker");removeMonitor(monitor);
 }
}
