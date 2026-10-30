package com.shazwan.nexadash.test;
import android.app.*;import android.content.Intent;import android.os.*;import android.view.*;import android.view.inputmethod.*;
public class Bridge extends Service {
 final Handler main=new Handler(Looper.getMainLooper());
 final java.util.ArrayDeque<Touch> events=new java.util.ArrayDeque<>();boolean closed;
 static final class Touch{final int display;final MotionEvent event;Touch(int d,MotionEvent e){display=d;event=e;}}
 final Runnable deliver=()->{while(true){Touch touch;synchronized(events){touch=events.poll();}if(touch==null)return;int display=touch.display;try{Activity a=Main.current.get();if(usable(a,display))a.dispatchTouchEvent(touch.event);}catch(RuntimeException ignored){}finally{touch.event.recycle();}}};
 void enqueue(int display,MotionEvent event){synchronized(events){if(closed||(events.size()>8&&event.getActionMasked()==MotionEvent.ACTION_MOVE)){event.recycle();return;}if(events.size()>=32){for(Touch t:events)t.event.recycle();events.clear();MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);events.add(new Touch(display,cancel));}events.add(new Touch(display,event));}main.removeCallbacks(deliver);main.post(deliver);}
 @Override public void onDestroy(){synchronized(events){closed=true;for(Touch t:events)t.event.recycle();events.clear();}main.removeCallbacksAndMessages(null);super.onDestroy();}
 final Binder binder=new Binder(){protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
  if(code==INTERFACE_TRANSACTION){reply.writeString("com.shazwan.embedprobe.CONTROL");return true;}
  data.enforceInterface("com.shazwan.embedprobe.CONTROL");
  if(code==1||code==6){int display=code==6?data.readInt():-1;MotionEvent event=MotionEvent.CREATOR.createFromParcel(data);enqueue(display,event);return true;}
  if(code==8){reply.writeNoException();reply.writeInt(1);return true;}
  if(code==7){reply.writeNoException();reply.writeString(Main.lastInput);return true;}
  if(code==2){reply.writeNoException();reply.writeInt(Main.count.get());return true;}
  if(code==3){Activity a=Main.current.get();reply.writeNoException();reply.writeInt(a==null||a.getWindow().getDecorView().getWidth()==0?-1:a.getWindowManager().getDefaultDisplay().getDisplayId());return true;}
  if(code==4||code==5){final int display=data.readInt();final String text=code==5?data.readString():null;main.post(()->{Activity a=Main.current.get();if(!usable(a,display))return;if(code==4){a.onBackPressed();return;}if(text==null||text.length()>240)return;View focus=a.getCurrentFocus();if(focus==null||!focus.onCheckIsTextEditor())return;EditorInfo info=new EditorInfo();InputConnection connection=focus.onCreateInputConnection(info);if(connection!=null){connection.commitText(text,1);connection.finishComposingText();connection.performEditorAction(EditorInfo.IME_ACTION_SEARCH);}});return true;}
  return super.onTransact(code,data,reply,flags);
 }};
 static boolean usable(Activity a,int display){return a!=null&&!a.isFinishing()&&a.getWindowManager().getDefaultDisplay().getDisplayId()>0&&(display<0||a.getWindowManager().getDefaultDisplay().getDisplayId()==display);}
 public IBinder onBind(Intent i){return binder;}
}
