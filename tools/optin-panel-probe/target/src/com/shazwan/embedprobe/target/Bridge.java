package com.shazwan.embedprobe.target;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;
public class Bridge extends Service {
    final Handler main=new Handler(Looper.getMainLooper());
    final Binder binder=new Binder(){protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
        if(code==INTERFACE_TRANSACTION){reply.writeString("com.shazwan.embedprobe.CONTROL");return true;}
        data.enforceInterface("com.shazwan.embedprobe.CONTROL");
        if(code==1){MotionEvent event=MotionEvent.CREATOR.createFromParcel(data);main.post(()->{try{Main activity=Main.current.get();if(activity!=null&&activity.getWindowManager().getDefaultDisplay().getDisplayId()!=0)activity.dispatchTouchEvent(event);}finally{event.recycle();}});return true;}
        if(code==2){reply.writeNoException();reply.writeInt(Main.count.get());return true;}
        return super.onTransact(code,data,reply,flags);
    }};
    public IBinder onBind(Intent intent){return binder;}
}
