package com.shazwan.nexapanel;
import android.app.*;import android.content.*;import android.database.Cursor;import android.net.Uri;import android.content.ContentValues;import android.os.Bundle;import java.lang.ref.WeakReference;
public class Init extends ContentProvider implements Application.ActivityLifecycleCallbacks {
 static volatile WeakReference<Activity> active=new WeakReference<>(null);
 static final java.util.concurrent.ConcurrentHashMap<Integer,WeakReference<Activity>> displays=new java.util.concurrent.ConcurrentHashMap<>();
 static Activity onDisplay(int id){WeakReference<Activity> ref=displays.get(id);return ref==null?null:ref.get();}
 public boolean onCreate(){((Application)getContext().getApplicationContext()).registerActivityLifecycleCallbacks(this);return true;}
 public void onActivityResumed(Activity a){active=new WeakReference<>(a);displays.put(a.getWindowManager().getDefaultDisplay().getDisplayId(),new WeakReference<>(a));}
 public void onActivityDestroyed(Activity a){if(active.get()==a)active=new WeakReference<>(null);int id=a.getWindowManager().getDefaultDisplay().getDisplayId();WeakReference<Activity> ref=displays.get(id);if(ref!=null&&ref.get()==a)displays.remove(id);}
 public void onActivityCreated(Activity a,Bundle b){} public void onActivityStarted(Activity a){}public void onActivityPaused(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}
 public Cursor query(Uri u,String[] p,String s,String[] a,String sort){return null;}public String getType(Uri u){return null;}public Uri insert(Uri u,ContentValues v){return null;}public int delete(Uri u,String s,String[] a){return 0;}public int update(Uri u,ContentValues v,String s,String[] a){return 0;}
}
