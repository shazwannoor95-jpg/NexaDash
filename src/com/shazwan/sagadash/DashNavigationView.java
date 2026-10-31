package com.shazwan.sagadash;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.location.Location;
import android.net.Uri;
import android.os.*;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import org.json.JSONArray;
import android.app.AlertDialog;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Native map with manual online search/routing and opt-in Android speech. */
final class DashNavigationView extends LinearLayout {
    private static final long FIX_LIFETIME=30000;
    private static final int INK=0xffdcefff, MUTED=0xff8ea8be;
    private final MapCanvas map;
    private final TextView status;
    private final TileStore tiles;
    private final float unit;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Set<String> requested=new HashSet<>();
    private final Runnable expire,retry;
    private Location location;
    private boolean active,closed,failed;
    private int zoom=15;
    private final Directions directions=new Directions();
    private long receivedAt,retryAt;
    private boolean following=true,viewSet;private double viewLat,viewLon;

    DashNavigationView(Context context,Runnable openNavigation){
        super(context);setOrientation(VERTICAL);tiles=TileStore.get(context);
        android.util.DisplayMetrics metrics=context.getResources().getDisplayMetrics();
        unit=Math.min(metrics.widthPixels/1280f,metrics.heightPixels/720f);
        int padding=Math.max(3,Math.round(6*unit));
        status=label("Map preview · Waiting for GPS",14,MUTED);addView(status,new LayoutParams(-1,-2));
        map=new MapCanvas(context);addView(map,new LayoutParams(-1,0,1));
        retry=()->{retryAt=0;if(active&&!closed&&hasCenter())requestVisible();};
        expire=()->{if(active){if(directions.speech!=null)directions.speech.stop();clearRetry();tiles.unsubscribe(this,Collections.emptySet());updateStatus();map.invalidate();}};
        TextView attribution=label("© OpenStreetMap contributors · routing OSRM",12,MUTED);
        attribution.setContentDescription("Map data copyright OpenStreetMap contributors");
        attribution.setOnClickListener(v->{try{context.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.openstreetmap.org/copyright")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(RuntimeException ignored){}});
        addView(attribution,new LayoutParams(-1,-2));
        LinearLayout buttons=new LinearLayout(context);buttons.setGravity(Gravity.CENTER_VERTICAL);
        TextView open=label("Search / route",15,INK);open.setGravity(Gravity.CENTER);open.setPadding(padding,padding,padding,padding);open.setOnClickListener(v->directions.search());buttons.addView(open,new LayoutParams(0,-2,1));
        for(int direction:new int[]{-1,1}){TextView button=label(direction<0?"−":"+",22,INK);button.setGravity(Gravity.CENTER);button.setPadding(padding,padding,padding,padding);button.setContentDescription(direction<0?"Zoom map out":"Zoom map in");button.setOnClickListener(v->{int next=Math.max(3,Math.min(17,zoom+direction));if(next!=zoom){zoom=next;failed=false;requestVisible();map.invalidate();}});buttons.addView(button,new LayoutParams(Math.max(1,Math.round(44*unit)),-2));}
        TextView follow=label("GPS",14,INK);follow.setGravity(Gravity.CENTER);follow.setPadding(padding,padding,padding,padding);follow.setContentDescription("Follow GPS location");follow.setOnClickListener(v->{if(!fresh()){directions.message("Waiting for a fresh GPS fix");return;}following=true;requestVisible();map.invalidate();});buttons.addView(follow,new LayoutParams(-2,-2));
        addView(buttons,new LayoutParams(-1,-2));setContentDescription("Map with online destination search and routes inside this panel.");
    }
    private TextView label(String text,int size,int color){TextView view=new TextView(getContext());view.setText(text);view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,size*unit);view.setTextColor(color);view.setMaxLines(1);return view;}
    void setLocation(Location fix){
        if(closed)return;
        if(fix==null){if(directions.speech!=null)directions.speech.stop();location=null;requested.clear();clearRetry();tiles.unsubscribe(this,Collections.emptySet());main.removeCallbacks(expire);updateStatus();map.invalidate();return;}
        if(!Double.isFinite(fix.getLatitude())||!Double.isFinite(fix.getLongitude())||Math.abs(fix.getLatitude())>90||Math.abs(fix.getLongitude())>180)return;
        long age=fix.getElapsedRealtimeNanos()>0?(SystemClock.elapsedRealtimeNanos()-fix.getElapsedRealtimeNanos())/1000000:0;
        if(age<0||age>FIX_LIFETIME)return;
        boolean regained=!fresh(),moved=location==null||location.distanceTo(fix)>3;
        location=new Location(fix);receivedAt=SystemClock.elapsedRealtime()-age;
        directions.position();
        if(active){main.removeCallbacks(expire);main.postDelayed(expire,Math.max(1,FIX_LIFETIME-age));if(moved||regained)failed=false;updateStatus();if(moved||regained){requestVisible();map.invalidate();}}
    }
    void resume(){if(closed)return;active=true;tiles.activate(this);failed=false;updateStatus();requestVisible();map.invalidate();main.removeCallbacks(expire);if(fresh())main.postDelayed(expire,Math.max(1,FIX_LIFETIME-(SystemClock.elapsedRealtime()-receivedAt)));}
    void pause(){directions.pause();active=false;main.removeCallbacks(expire);clearRetry();requested.clear();tiles.unsubscribe(this,Collections.emptySet());tiles.deactivate(this);}
    void close(){pause();closed=true;directions.worker.shutdownNow();location=null;}
    private boolean fresh(){return location!=null&&SystemClock.elapsedRealtime()-receivedAt<FIX_LIFETIME;}
    private void updateStatus(){String text=location==null?"Map preview · Waiting for GPS":!fresh()?"Last GPS fix · Waiting for signal":failed?"Map unavailable · Open navigation":"Map preview · GPS"+(location.hasAccuracy()?" ±"+Math.round(location.getAccuracy())+" m":"");if(directions.path!=null&&fresh())text=directions.instruction;else if(directions.busy)text="Loading online map search / route…";if(!text.contentEquals(status.getText()))status.setText(text);}
    private void requestVisible(){
        if(!active||closed||!hasCenter()||map.getWidth()==0||map.getHeight()==0)return;
        double[] center=centerWorld();int n=1<<zoom;
        int left=(int)Math.floor((center[0]-map.getWidth()/2d)/256),right=(int)Math.floor((center[0]+map.getWidth()/2d)/256);
        int top=(int)Math.floor((center[1]-map.getHeight()/2d)/256),bottom=(int)Math.floor((center[1]+map.getHeight()/2d)/256);
        List<String> keys=new ArrayList<>();for(int y=Math.max(0,top);y<=Math.min(n-1,bottom);y++)for(int x=left;x<=right;x++)keys.add(key(zoom,Math.floorMod(x,n),y));
        if(keys.size()>32)keys=keys.subList(0,32);
        Set<String> next=new HashSet<>(keys);tiles.unsubscribe(this,next);requested.clear();requested.addAll(next);
        for(String tile:keys)tiles.request(tile,this);
    }
    private void tileReady(String key,boolean success){
        if(!active||closed||!requested.contains(key))return;failed=!success;updateStatus();map.invalidate();
        if(!success&&hasCenter()){
            long now=SystemClock.elapsedRealtime(),at=now+Math.max(1,tiles.retryDelay(key));
            if(retryAt==0||at<retryAt){main.removeCallbacks(retry);retryAt=at;main.postDelayed(retry,Math.max(1,at-now));}
        }
    }
    private void clearRetry(){main.removeCallbacks(retry);retryAt=0;}
    static double[] world(double latitude,double longitude,int zoom){double lat=Math.max(-85.05112878,Math.min(85.05112878,latitude))*Math.PI/180;double size=256d*(1<<zoom);return new double[]{(longitude+180)/360*size,(1-Math.log(Math.tan(lat)+1/Math.cos(lat))/Math.PI)/2*size};}
    private boolean hasCenter(){return location!=null||viewSet;}
    private double[] centerWorld(){return following&&location!=null?world(location.getLatitude(),location.getLongitude(),zoom):world(viewSet?viewLat:location.getLatitude(),viewSet?viewLon:location.getLongitude(),zoom);}
    private void centerAt(double lat,double lon){viewLat=lat;viewLon=lon;viewSet=true;following=false;requestVisible();map.invalidate();}
    private static String key(int zoom,int x,int y){return zoom+"_"+x+"_"+y;}
    @Override protected void onDetachedFromWindow(){pause();super.onDetachedFromWindow();}

    private final class MapCanvas extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);final Rect destination=new Rect();
        float downX,downY;long lastRequest;
        MapCanvas(Context context){super(context);setContentDescription("Map. Drag to pan; GPS button resumes following.");}
        @Override public boolean onTouchEvent(MotionEvent event){
            if(!hasCenter())return true;int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();getParent().requestDisallowInterceptTouchEvent(true);return true;}
            if(action==MotionEvent.ACTION_MOVE){double[] center=centerWorld();double size=256d*(1<<zoom);center[0]-=event.getX()-downX;center[1]-=event.getY()-downY;viewLon=DashRouteProgress.wrap(center[0]/size*360-180);double y=Math.max(0,Math.min(size,center[1]));viewLat=Math.toDegrees(Math.atan(Math.sinh(Math.PI*(1-2*y/size))));viewSet=true;following=false;downX=event.getX();downY=event.getY();long now=SystemClock.elapsedRealtime();if(now-lastRequest>200){lastRequest=now;requestVisible();}invalidate();return true;}
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){getParent().requestDisallowInterceptTouchEvent(false);requestVisible();return true;}return true;
        }
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){requestVisible();}
        @Override protected void onDraw(Canvas canvas){
            canvas.drawColor(0xff0a1c2a);
            if(!hasCenter()){paint.setColor(MUTED);paint.setTextSize(Math.max(9,14*unit));paint.setTextAlign(Paint.Align.CENTER);canvas.drawText("Waiting for GPS",getWidth()/2f,getHeight()/2f,paint);return;}
            double[] center=centerWorld();int n=1<<zoom;
            double originX=center[0]-getWidth()/2d,originY=center[1]-getHeight()/2d;
            for(int y=Math.max(0,(int)Math.floor(originY/256));y<=Math.min(n-1,(int)Math.floor((originY+getHeight())/256));y++)for(int x=(int)Math.floor(originX/256);x<=(int)Math.floor((originX+getWidth())/256);x++){
                Tile tile=tiles.memory.get(key(zoom,Math.floorMod(x,n),y));int px=(int)Math.round(x*256-originX),py=(int)Math.round(y*256-originY);
                destination.set(px,py,px+256,py+256);
                if(tile!=null&&!tile.bitmap.isRecycled()){paint.setColor(Color.WHITE);canvas.drawBitmap(tile.bitmap,null,destination,paint);}
                else{paint.setColor(0xff163144);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);canvas.drawRect(destination,paint);paint.setStyle(Paint.Style.FILL);}
            }
            if(directions.path!=null){
                Path line=new Path();boolean first=true;
                for(double[] point:directions.path){double[] projected=world(point[0],point[1],zoom);float rx=(float)(projected[0]-originX),ry=(float)(projected[1]-originY);if(first){line.moveTo(rx,ry);first=false;}else line.lineTo(rx,ry);}
                paint.setColor(0xff64cfff);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(6*unit);canvas.drawPath(line,paint);paint.setStyle(Paint.Style.FILL);
            }
            if(!directions.destination.isEmpty()){double[] dest=world(directions.destLat,directions.destLon,zoom);float dx=(float)(dest[0]-originX),dy=(float)(dest[1]-originY);paint.setColor(0xffffba65);canvas.drawCircle(dx,dy,9*unit,paint);paint.setColor(Color.WHITE);canvas.drawCircle(dx,dy,3*unit,paint);}
            if(location==null)return;
            double[] gpsPoint=world(location.getLatitude(),location.getLongitude(),zoom);
            float x=(float)(gpsPoint[0]-originX),y=(float)(gpsPoint[1]-originY);
            paint.setColor(0x332b9dff);canvas.drawCircle(x,y,16,paint);paint.setColor(fresh()?0xff43c5ff:0xffffba65);canvas.drawCircle(x,y,6,paint);paint.setColor(Color.WHITE);paint.setStrokeWidth(2);paint.setStyle(Paint.Style.STROKE);canvas.drawCircle(x,y,6,paint);paint.setStyle(Paint.Style.FILL);
        }
    }
    /** Manual online search/routing. Never starts another app or calls the Maps companion. */
    interface RouteDone {void accept(JSONObject result)throws Exception;}
    final class Directions {
        final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(2));
        double[][] path;JSONArray steps;double[] stepLengths;double routeDistance,routeDuration;String instruction="";boolean busy;int generation,spoken=-1;Future<?> task;
        AlertDialog dialog;android.speech.tts.TextToSpeech speech;boolean voice,voiceReady;
        double destLat,destLon;String destination="";
        void show(AlertDialog d){if(dialog!=null)dialog.dismiss();dialog=d;d.setOnDismissListener(v->{View query=d.getWindow().getDecorView().findViewWithTag("panel-destination-query");if(query!=null)hideKeyboard(query);if(dialog==d)dialog=null;});d.show();}
        void search(){
            if(!active||closed)return;
            LinearLayout body=new LinearLayout(getContext());body.setOrientation(VERTICAL);
            EditText query=new EditText(getContext());query.setSingleLine(true);query.setHint("Destination / address");query.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(160)});query.setTag("panel-destination-query");body.addView(query);
            TextView notice=label("Online search: Photon · routes: OSRM",12,MUTED);body.addView(notice);
            AlertDialog d=new AlertDialog.Builder(getContext()).setTitle("Search in this panel").setView(body).setPositiveButton("Search",null).setNeutralButton("Route options",(v,w)->options()).setNegativeButton("Cancel",null).create();
            show(d);d.getButton(-1).setOnClickListener(v->{String q=query.getText().toString().trim();if(q.isEmpty()){query.setError("Enter a destination");return;}hideKeyboard(query);d.dismiss();String url="https://photon.komoot.io/api/?limit=6&q="+Uri.encode(q);if(fresh())url+="&lat="+location.getLatitude()+"&lon="+location.getLongitude();request(url,result->{JSONArray rows=result.getJSONArray("features");ArrayList<String> names=new ArrayList<>();ArrayList<double[]> places=new ArrayList<>();for(int i=0;i<Math.min(20,rows.length());i++){JSONObject f=rows.getJSONObject(i),properties=f.getJSONObject("properties");JSONArray xy=f.getJSONObject("geometry").getJSONArray("coordinates");double lat=xy.getDouble(1),lon=xy.getDouble(0);if(!validCoordinate(lat,lon))continue;String name=properties.optString("name","");if(name.isEmpty())name=properties.optString("street","Destination");names.add(shortText(name,160)+" · "+shortText(properties.optString("city",properties.optString("state","")),80));places.add(new double[]{lat,lon});}if(names.isEmpty()){message("No destinations found. Try a fuller address.");return;}show(new AlertDialog.Builder(getContext()).setTitle("Choose destination").setItems(names.toArray(new String[0]),(a,index)->{destLat=places.get(index)[0];destLon=places.get(index)[1];destination=names.get(index);centerAt(destLat,destLon);route();}).setNegativeButton("Cancel",null).create());});});
        }
        void hideKeyboard(View view){android.view.inputmethod.InputMethodManager ime=(android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);if(ime!=null)ime.hideSoftInputFromWindow(view.getWindowToken(),0);view.clearFocus();}
        void options(){show(new AlertDialog.Builder(getContext()).setTitle("Route in panel").setItems(new String[]{"Recalculate selected destination","Clear route","Voice guidance: "+(voice?"On":"Off"),"Show entire route"},(d,index)->{if(index==0)route();else if(index==1){cancel();path=null;steps=null;spoken=-1;instruction="";if(speech!=null)speech.stop();updateStatus();map.invalidate();}else if(index==3){overview();}else{voice=!voice;if(voice&&speech==null)speech=new android.speech.tts.TextToSpeech(getContext(),status->{voiceReady=status==android.speech.tts.TextToSpeech.SUCCESS;if(voiceReady&&speech!=null){int available=speech.setLanguage(Locale.ENGLISH);voiceReady=available>=0;}if(!voiceReady)message("English voice data unavailable. Check Android text-to-speech settings.");});if(!voice&&speech!=null)speech.stop();}}).setNegativeButton("Close",null).create());}
        void route(){
            cancel();
            if(destination.isEmpty()){message("Search and choose a destination first.");return;}
            path=null;steps=null;spoken=-1;instruction="";if(speech!=null)speech.stop();map.invalidate();
            if(!fresh()){message("Wait for a fresh GPS fix, then choose Recalculate selected destination.");updateStatus();return;}
            String url="https://router.project-osrm.org/route/v1/driving/"+location.getLongitude()+","+location.getLatitude()+";"+destLon+","+destLat+"?overview=full&geometries=geojson&steps=true";
            request(url,result->{if(!"Ok".equals(result.optString("code")))throw new IOException("No route returned");JSONObject route=result.getJSONArray("routes").getJSONObject(0);double[][] points=parseRouteCoordinates(route.getJSONObject("geometry").getJSONArray("coordinates"));JSONArray next=route.getJSONArray("legs").getJSONObject(0).getJSONArray("steps");if(next.length()>2000)throw new IOException("Route has too many instructions");double[] lengths=new double[next.length()];for(int i=0;i<lengths.length;i++){JSONObject step=next.getJSONObject(i);lengths[i]=step.getDouble("distance");if(!Double.isFinite(lengths[i])||lengths[i]<0)throw new IOException("Invalid step distance");step.getJSONObject("maneuver");}double distance=route.getDouble("distance"),duration=route.getDouble("duration");if(!Double.isFinite(distance)||distance<=0||!Double.isFinite(duration)||duration<0)throw new IOException("Invalid route summary");following=true;path=points;steps=next;stepLengths=lengths;routeDistance=distance;routeDuration=duration;spoken=-1;instruction=String.format(Locale.US,"%.1f km · %.0f min · %s",route.getDouble("distance")/1000,route.getDouble("duration")/60,destination);map.invalidate();requestVisible();position();});
        }
        void overview(){if(path==null){message("No route selected");return;}double base=path[0][1],minLon=base,maxLon=base,minLat=90,maxLat=-90;for(double[] p:path){double lon=base+DashRouteProgress.wrap(p[1]-base);minLon=Math.min(minLon,lon);maxLon=Math.max(maxLon,lon);minLat=Math.min(minLat,p[0]);maxLat=Math.max(maxLat,p[0]);}for(zoom=17;zoom>3;zoom--){double[] a=world(minLat,minLon,zoom),b=world(maxLat,maxLon,zoom);if(Math.abs(a[0]-b[0])<Math.max(1,map.getWidth()-40)&&Math.abs(a[1]-b[1])<Math.max(1,map.getHeight()-40))break;}centerAt((minLat+maxLat)/2,DashRouteProgress.wrap((minLon+maxLon)/2));}
        void position(){
            if(!active||!fresh()||steps==null)return;
            try{DashRouteProgress.Projection projection=DashRouteProgress.locate(path,location.getLatitude(),location.getLongitude());
                if(projection.off>100){instruction="Off route · Search → Route options → Recalculate";if(speech!=null)speech.stop();return;}
                double along=projection.total<=0?0:projection.along/projection.total*routeDistance;
                int next=DashRouteProgress.nextStep(stepLengths,along);double end=0;for(int i=0;i<next;i++)end+=stepLengths[i];double remaining=Math.max(0,end-along);
                JSONObject step=steps.getJSONObject(next),m=step.getJSONObject("maneuver");String type=m.optString("type","continue");if(type.length()>40)type="continue";String text=("arrive".equals(type)?"Arrive at destination":type.replace('_',' ')+" "+shortText(m.optString("modifier",""),32)+" "+shortText(step.optString("name",""),160)).trim();
                instruction=Math.round(remaining)+" m · "+text+String.format(Locale.US," · %.0f min est.",routeDuration*Math.max(0,1-along/routeDistance)/60);
                if(voice&&voiceReady&&speech!=null&&remaining<120&&next!=spoken){spoken=next;speech.speak(text,android.speech.tts.TextToSpeech.QUEUE_FLUSH,null,"panel-turn");}
            }catch(Exception malformed){instruction="Route loaded · guidance unavailable";}
        }

        String shortText(String value,int max){return value.length()>max?value.substring(0,max):value;}
        void message(String text){if(active&&!closed)Toast.makeText(getContext(),text,Toast.LENGTH_LONG).show();}
        void request(String url,RouteDone done){
            cancel();if(!active||closed)return;final int run=generation;busy=true;updateStatus();
            task=worker.submit(()->{JSONObject result=null;Exception problem=null;HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setRequestProperty("User-Agent","NexaDash/1.9 (+https://github.com/shazwannoor95-jpg/NexaDash)");if(c.getResponseCode()!=200)throw new IOException("Service response "+c.getResponseCode());try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())return;if(out.size()+n>2*1024*1024)throw new IOException("Response too large");out.write(b,0,n);}result=new JSONObject(out.toString("UTF-8"));}}catch(Exception error){problem=error;}finally{if(c!=null)c.disconnect();}final JSONObject data=result;final Exception error=problem;main.post(()->{if(run!=generation||closed||!active)return;busy=false;try{if(error!=null)throw error;done.accept(data);}catch(Exception bad){message("Search / route unavailable. Check Internet or try a shorter route.");}updateStatus();});});
        }
        void cancel(){generation++;busy=false;if(task!=null)task.cancel(true);task=null;worker.purge();}
        void pause(){cancel();if(dialog!=null){dialog.dismiss();dialog=null;}if(speech!=null){speech.stop();speech.shutdown();speech=null;}voiceReady=false;voice=false;}
    }
    static boolean validCoordinate(double lat,double lon){return Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180;}
    static double[][] parseRouteCoordinates(JSONArray values)throws Exception{
        if(values==null||values.length()<2||values.length()>10000)throw new IOException("Invalid route geometry");double[][] result=new double[values.length()][2];for(int i=0;i<values.length();i++){JSONArray p=values.getJSONArray(i);double lon=p.getDouble(0),lat=p.getDouble(1);if(!validCoordinate(lat,lon))throw new IOException("Invalid route coordinate");result[i][0]=lat;result[i][1]=lon;}return result;
    }
    private static final class Tile {final Bitmap bitmap;final long expires;Tile(Bitmap bitmap,long expires){this.bitmap=bitmap;this.expires=expires;}}
    private static final class Request {final Set<WeakReference<DashNavigationView>> listeners=new HashSet<>();volatile HttpURLConnection connection;Future<?> future;boolean cancelled;}
    private static final class TileStore {
        static TileStore instance;static final long DEFAULT_TTL=7L*24*60*60*1000,DISK_LIMIT=12L*1024*1024;
        final File directory;final Handler main=new Handler(Looper.getMainLooper());final Map<String,Request> pending=new HashMap<>();
        final WeakHashMap<DashNavigationView,Boolean> owners=new WeakHashMap<>();
        final LruCache<String,Long> failures=new LruCache<>(128);
        final LruCache<String,Tile> memory=new LruCache<String,Tile>(3*1024*1024){@Override protected int sizeOf(String key,Tile value){return value.bitmap.getAllocationByteCount();}};
        final ThreadPoolExecutor worker=new ThreadPoolExecutor(0,1,15,TimeUnit.SECONDS,new LinkedBlockingQueue<>(),task->{Thread thread=new Thread(task,"Saga-map-tiles");thread.setDaemon(true);return thread;});
        final ThreadPoolExecutor canceller=new ThreadPoolExecutor(0,1,5,TimeUnit.SECONDS,new LinkedBlockingQueue<>(),task->{Thread thread=new Thread(task,"Saga-map-cancel");thread.setDaemon(true);return thread;});
        static synchronized TileStore get(Context context){if(instance==null)instance=new TileStore(context.getApplicationContext());return instance;}
        TileStore(Context context){directory=new File(context.createDeviceProtectedStorageContext().getCacheDir(),"navigation-tiles");}
        synchronized void activate(DashNavigationView view){owners.put(view,Boolean.TRUE);}
        synchronized void deactivate(DashNavigationView view){owners.remove(view);if(owners.isEmpty())memory.evictAll();}
        long retryDelay(String key){Long at=failures.get(key);return at==null?0:Math.max(0,at-SystemClock.elapsedRealtime());}
        synchronized void request(String key,DashNavigationView owner){
            Tile existing=memory.get(key);if(existing!=null&&freshExpiry(existing.expires))return;
            Long retry=failures.get(key);if(retry!=null&&retry>SystemClock.elapsedRealtime()){main.post(()->owner.tileReady(key,false));return;}
            Request task=pending.get(key);boolean start=task==null||task.cancelled;
            if(start){task=new Request();pending.put(key,task);}
            boolean contains=false;for(WeakReference<DashNavigationView> ref:task.listeners)if(ref.get()==owner)contains=true;
            if(!contains)task.listeners.add(new WeakReference<>(owner));
            if(start){final Request current=task;task.future=worker.submit(()->load(key,current));}
        }
        synchronized void unsubscribe(DashNavigationView owner,Set<String> keep){
            Iterator<Map.Entry<String,Request>> iterator=pending.entrySet().iterator();
            while(iterator.hasNext()){
                Map.Entry<String,Request> entry=iterator.next();Request request=entry.getValue();if(keep.contains(entry.getKey()))continue;
                request.listeners.removeIf(ref->ref.get()==null||ref.get()==owner);
                if(request.listeners.isEmpty()){request.cancelled=true;if(request.future!=null)request.future.cancel(false);if(request.connection!=null){final HttpURLConnection connection=request.connection;canceller.execute(connection::disconnect);}iterator.remove();}
            }
            worker.purge();
        }
        synchronized boolean wanted(Request request){return !request.cancelled&&!request.listeners.isEmpty();}
        synchronized boolean publish(String key,Request request,Tile tile){if(!wanted(request)||owners.isEmpty())return false;memory.put(key,tile);return true;}
        void notifyReady(String key,Request task,boolean success){
            if(success)failures.remove(key);else failures.put(key,SystemClock.elapsedRealtime()+60000);
            final List<WeakReference<DashNavigationView>> targets;
            synchronized(this){targets=new ArrayList<>(task.listeners);}
            main.post(()->{for(WeakReference<DashNavigationView> ref:targets){DashNavigationView view=ref.get();if(view!=null)view.tileReady(key,success);}});
        }
        private void load(String key,Request task){
            HttpURLConnection connection=null;File image=new File(directory,key+".png"),metadata=new File(directory,key+".json");JSONObject info=new JSONObject();
            try{
                if(!wanted(task))return;directory.mkdirs();
                if(metadata.isFile())try{info=new JSONObject(readText(metadata));}catch(Exception ignored){}
                if(image.isFile()){
                    Bitmap bitmap=null;
                    try{bitmap=decodeTile(image);}catch(IOException invalid){image.delete();metadata.delete();info=new JSONObject();}
                    if(bitmap!=null){Tile cached=new Tile(bitmap,info.optLong("expires",0));if(!publish(key,task,cached))return;notifyReady(key,task,true);if(freshExpiry(cached.expires))return;}
                }
                if(!wanted(task))return;
                String[] coordinate=key.split("_");URL url=new URL("https://tile.openstreetmap.org/"+coordinate[0]+"/"+coordinate[1]+"/"+coordinate[2]+".png");
                connection=(HttpURLConnection)url.openConnection();task.connection=connection;
                connection.setConnectTimeout(5000);connection.setReadTimeout(7000);connection.setRequestProperty("User-Agent","NexaDash/1.6.7 (+https://github.com/shazwannoor95-jpg/NexaDash)");connection.setRequestProperty("Accept","image/png");
                if(info.has("etag"))connection.setRequestProperty("If-None-Match",info.optString("etag"));if(info.has("modified"))connection.setRequestProperty("If-Modified-Since",info.optString("modified"));
                if(!wanted(task))return;
                int code=connection.getResponseCode();boolean noStore=directive(connection,"no-store");long expiry=expiry(connection);
                if(code==HttpURLConnection.HTTP_NOT_MODIFIED&&image.isFile()){
                    if(noStore){image.delete();metadata.delete();}else{info.put("expires",expiry);writeText(metadata,info.toString());}
                    Tile cached=memory.get(key);if(cached!=null)publish(key,task,new Tile(cached.bitmap,expiry));notifyReady(key,task,true);return;
                }
                if(code!=HttpURLConnection.HTTP_OK)throw new IOException("Map response "+code);
                byte[] bytes;try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1){if(!wanted(task))return;if(out.size()+count>512*1024)throw new IOException("Map tile exceeds limit");out.write(buffer,0,count);}bytes=out.toByteArray();
                }
                Bitmap bitmap=decodeTile(bytes);
                if(!wanted(task))return;
                if(noStore){image.delete();metadata.delete();}
                else{
                    File temporary=new File(directory,key+".pending");try(FileOutputStream out=new FileOutputStream(temporary)){out.write(bytes);}if(!temporary.renameTo(image))temporary.delete();
                    JSONObject latest=new JSONObject().put("expires",expiry);String etag=connection.getHeaderField("ETag"),modified=connection.getHeaderField("Last-Modified");if(etag!=null)latest.put("etag",etag);if(modified!=null)latest.put("modified",modified);writeText(metadata,latest.toString());
                }
                if(!publish(key,task,new Tile(bitmap,expiry)))return;if(!noStore)prune();notifyReady(key,task,true);
            }catch(Exception error){if(wanted(task))notifyReady(key,task,false);}
            finally{if(connection!=null)connection.disconnect();synchronized(this){if(pending.get(key)==task)pending.remove(key);}task.connection=null;}
        }
        static Bitmap decodeTile(byte[] bytes)throws IOException{
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);validateBounds(options);
            options.inJustDecodeBounds=false;options.inScaled=false;options.inPreferredConfig=Bitmap.Config.RGB_565;
            return validateBitmap(BitmapFactory.decodeByteArray(bytes,0,bytes.length,options));
        }
        static Bitmap decodeTile(File image)throws IOException{
            if(image.length()>512*1024)throw new IOException("Map tile exceeds limit");
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
            BitmapFactory.decodeFile(image.getAbsolutePath(),options);validateBounds(options);
            options.inJustDecodeBounds=false;options.inScaled=false;options.inPreferredConfig=Bitmap.Config.RGB_565;
            return validateBitmap(BitmapFactory.decodeFile(image.getAbsolutePath(),options));
        }
        private static void validateBounds(BitmapFactory.Options options)throws IOException{if(options.outWidth!=256||options.outHeight!=256)throw new IOException("Invalid map tile dimensions");}
        private static Bitmap validateBitmap(Bitmap bitmap)throws IOException{if(bitmap==null)throw new IOException("Invalid map tile");if(bitmap.getWidth()!=256||bitmap.getHeight()!=256){bitmap.recycle();throw new IOException("Invalid map tile dimensions");}return bitmap;}
        static boolean freshExpiry(long expires){long now=System.currentTimeMillis();return expires>now&&expires-now<=30L*24*60*60*1000;}
        static boolean directive(HttpURLConnection connection,String wanted){
            String control=connection.getHeaderField("Cache-Control");if(control==null)return false;
            for(String part:control.split(","))if(part.trim().split("=",2)[0].trim().equalsIgnoreCase(wanted))return true;return false;
        }
        static long expiry(HttpURLConnection connection){
            if(directive(connection,"no-store")||directive(connection,"no-cache"))return 0;
            long now=System.currentTimeMillis();String control=connection.getHeaderField("Cache-Control");
            if(control!=null)for(String part:control.split(",")){String value=part.trim().toLowerCase(Locale.US);if(value.startsWith("max-age="))try{return now+Math.max(0,Math.min(30L*24*60*60,Long.parseLong(value.substring(8).replace("\"",""))))*1000;}catch(NumberFormatException ignored){}}
            long expires=connection.getHeaderFieldDate("Expires",0);return expires>now?expires:now+DEFAULT_TTL;
        }
        void prune(){File[] files=directory.listFiles();if(files==null)return;long total=0;for(File file:files)total+=file.length();if(total<=DISK_LIMIT)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified));for(File file:files){long length=file.length();if(file.delete())total-=length;if(total<=DISK_LIMIT)break;}}
        static String readText(File file)throws IOException{try(FileInputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] bytes=new byte[1024];int count;while((count=in.read(bytes))!=-1){if(out.size()+count>8192)throw new IOException("Map metadata exceeds limit");out.write(bytes,0,count);}return out.toString("UTF-8");}}
        static void writeText(File file,String text)throws IOException{try(FileOutputStream out=new FileOutputStream(file)){out.write(text.getBytes("UTF-8"));}}
    }
}
