package com.shazwan.nexapanel;
import android.app.*;import android.content.Intent;import android.os.*;import android.view.MotionEvent;
public class Bridge extends Service {
 final Handler main=new Handler(Looper.getMainLooper());
 final Binder binder=new Binder(){protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
  if(code==INTERFACE_TRANSACTION){reply.writeString("com.shazwan.embedprobe.CONTROL");return true;}
  if(code==1){data.enforceInterface("com.shazwan.embedprobe.CONTROL");final MotionEvent event=MotionEvent.CREATOR.createFromParcel(data);main.post(()->{try{Activity a=Init.active.get();if(a!=null&&!a.isFinishing()&&a.getWindowManager().getDefaultDisplay().getDisplayId()!=0)a.dispatchTouchEvent(event);}finally{event.recycle();}});return true;}
  if(code==2){data.enforceInterface("com.shazwan.embedprobe.CONTROL");reply.writeNoException();reply.writeInt(0);return true;}
  return super.onTransact(code,data,reply,flags);
 }};
 public IBinder onBind(Intent i){return binder;}
}
