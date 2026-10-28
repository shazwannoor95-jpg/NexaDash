package com.shazwan.sagadash;

import android.app.*;import android.content.*;import android.os.*;import android.view.*;
/** Real cross-UID companion render/input and release checks; no YouTube playback claim. */
final class DashNativeChecks {
    int assertions;
    void check(boolean ok,String message){if(!ok)throw new AssertionError(message);assertions++;}
    interface Ready{boolean get();}
    void await(Instrumentation i,Ready test,String message)throws Exception{
        long end=SystemClock.elapsedRealtime()+30000;boolean[] yes={false};while(!yes[0]&&SystemClock.elapsedRealtime()<end){i.runOnMainSync(()->yes[0]=test.get());if(!yes[0])SystemClock.sleep(100);}check(yes[0],message);
    }
    int query(IBinder binder,int code)throws Exception{Parcel a=Parcel.obtain(),b=Parcel.obtain();try{a.writeInterfaceToken(DashNativeFrame.TOKEN);binder.transact(code,a,b,0);b.readException();return b.readInt();}finally{a.recycle();b.recycle();}}
    String queryText(IBinder binder)throws Exception{Parcel a=Parcel.obtain(),b=Parcel.obtain();try{a.writeInterfaceToken(DashNativeFrame.TOKEN);binder.transact(7,a,b,0);b.readException();return b.readString();}finally{a.recycle();b.recycle();}}
    android.widget.Button button(android.view.View root,String text){if(root instanceof android.widget.Button&&text.contentEquals(((android.widget.Button)root).getText()))return (android.widget.Button)root;if(root instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)root;for(int n=0;n<group.getChildCount();n++){android.widget.Button b=button(group.getChildAt(n),text);if(b!=null)return b;}}return null;}
    int run(Instrumentation i,DashboardActivity dash)throws Exception{
        final DashNativeFrame[] frame={null};final IBinder[] bridge={null};String left=dash.leftPane.mode,right=dash.rightPane.mode;
        try{
            i.runOnMainSync(()->{
                dash.leftPane.setMode("native_app");frame[0]=dash.leftPane.nativeFrame;
                check(frame[0]!=null&&frame[0].display==null&&frame[0].remote==null&&frame[0].inputThread==null,"selecting native panel creates no display, binding or worker before explicit Start");
                frame[0].select(2);frame[0].start();
            });
            await(i,()->frame[0].display!=null&&frame[0].remote!=null,"native panel binds and creates its own real private display");
            i.runOnMainSync(()->bridge[0]=frame[0].remote);
            int id=frame[0].display.getDisplay().getDisplayId();
            check(id>0,"native target uses a non-primary display");
            long end=SystemClock.elapsedRealtime()+30000;int actual=-1;while(actual!=id&&SystemClock.elapsedRealtime()<end){actual=query(bridge[0],3);if(actual!=id)SystemClock.sleep(100);}
            check(actual==id,"separate companion Activity really runs on the requested native panel display");
            check(dash.getPackageManager().getApplicationInfo("com.shazwan.embedprobe.target",0).uid!=dash.getApplicationInfo().uid,"host and companion have different UIDs");
            await(i,()->frame[0].surface.getWidth()>0&&frame[0].surface.getHeight()>0,"native surface has measurable panel bounds");
            check(Math.abs(frame[0].renderWidth/(double)frame[0].renderHeight-frame[0].surface.getWidth()/(double)frame[0].surface.getHeight())<.01,"native raster preserves the measured panel aspect ratio");
            check(Math.max(frame[0].renderWidth,frame[0].renderHeight)<=800,"native raster longest edge is bounded to800 pixels");
            int before=query(bridge[0],2);final long down=SystemClock.uptimeMillis();
            for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){
                i.runOnMainSync(()->{MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,frame[0].surface.getWidth()*Math.min(300f,frame[0].renderWidth/2f)/frame[0].renderWidth,frame[0].surface.getHeight()*105f/frame[0].renderHeight,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);frame[0].surface.dispatchTouchEvent(event);event.recycle();});SystemClock.sleep(80);
            }
            end=SystemClock.elapsedRealtime()+15000;int after=before;while(after==before&&SystemClock.elapsedRealtime()<end){after=query(bridge[0],2);if(after==before)SystemClock.sleep(100);}
            check(after==before+1,"panel-scaled native touch increments the separate companion's real UI counter");
            android.graphics.Bitmap snapshot=i.getUiAutomation().takeScreenshot();
            if(snapshot!=null){java.io.File image=new java.io.File(i.getTargetContext().getExternalFilesDir(null),"native-lab-demo-left.png");try(java.io.FileOutputStream out=new java.io.FileOutputStream(image)){snapshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{snapshot.recycle();}}
            i.runOnMainSync(()->button(frame[0],"⋯").performClick());i.waitForIdleSync();
            await(i,()->frame[0].display!=null&&frame[0].renderHeight>=1,"native display remains available after expanding input tools");
            SystemClock.sleep(1000); // Allow the target Activity's configuration change before focusing its editor.
            final long textDown=SystemClock.uptimeMillis();
            for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){i.runOnMainSync(()->{MotionEvent event=MotionEvent.obtain(textDown,SystemClock.uptimeMillis(),action,frame[0].surface.getWidth()*Math.min(300f,frame[0].renderWidth/2f)/frame[0].renderWidth,frame[0].surface.getHeight()*161f/frame[0].renderHeight,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);frame[0].surface.dispatchTouchEvent(event);event.recycle();});SystemClock.sleep(80);}
            i.runOnMainSync(()->{frame[0].entry.setText("Nexa native text");button(frame[0],"Send").performClick();});
            end=SystemClock.elapsedRealtime()+15000;String text="";while(!"Nexa native text".equals(text)&&SystemClock.elapsedRealtime()<end){text=queryText(bridge[0]);if(!"Nexa native text".equals(text))SystemClock.sleep(100);}
            check("Nexa native text".equals(text),"host text proxy commits into the focused EditText of the separate companion");
            i.runOnMainSync(()->{frame[0].stop();check(frame[0].display==null&&frame[0].remote==null&&!frame[0].bound&&frame[0].inputThread==null,"Stop releases display, input thread and service binding");frame[0].resume();check(!frame[0].wanted&&frame[0].display==null,"resume does not auto-launch the native app");});
            i.runOnMainSync(()->{dash.rightPane.setMode("native_app");check(dash.leftPane.nativeFrame==null&&"car".equals(dash.leftPane.mode),"selecting native on the other panel releases the previous inline/native panel");frame[0]=dash.rightPane.nativeFrame;frame[0].select(2);frame[0].start();});
            await(i,()->frame[0].display!=null,"native companion can start in the right panel too");
            i.runOnMainSync(()->{frame[0].pause();check(frame[0].display==null&&!frame[0].wanted&&frame[0].inputThread==null,"hidden/stopped panel releases its native resources");});
        }finally{
            i.runOnMainSync(()->{dash.leftPane.setMode(left);dash.rightPane.setMode(right);});
            i.runOnMainSync(()->dash.startActivity(new Intent(dash,DashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));
            i.waitForIdleSync();
        }
        return assertions;
    }
}
