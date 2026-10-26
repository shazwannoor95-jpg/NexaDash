package com.shazwan.sagadash;

import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/** Actual image decoding, angle interaction and resource lifetime; no live 3D geometry. */
final class SagaAngleChecks {
    int assertions;
    void check(boolean condition,String message){if(!condition)throw new AssertionError(message);assertions++;}
    interface Work {void run() throws Exception;}
    int run(Instrumentation instrumentation,DashboardActivity dashboard) throws Exception {
        SharedPreferences preferences=dashboard.dashPrefs();
        boolean hadFrame=preferences.contains("vehicle_angle_frame"),hadView=preferences.contains("vehicle_view");
        int savedFrame=preferences.getInt("vehicle_angle_frame",7);
        String savedView=preferences.getString("vehicle_view","angles");
        try{
            for(String removed:new String[]{"SagaView","SagaMesh"}){
                boolean absent=false;
                try{Class.forName("com.shazwan.sagadash."+removed,false,dashboard.getClassLoader());}catch(ClassNotFoundException expected){absent=true;}
                check(absent,"obsolete live-3D class is not packaged: "+removed);
            }
            check(dashboard.getResources().getIdentifier("saga_showroom","raw",dashboard.getPackageName())==0,"removed studio-background video is not packaged");
            boolean removedMesh=false;
            try(InputStream ignored=dashboard.getAssets().open("vehicle/saga-vvt.mesh")){}catch(IOException expected){removedMesh=true;}
            check(removedMesh,"obsolete mesh is not shipped as an unused asset");
            onMain(instrumentation,()->dashboard.setVehicleVideo(false));
            ready(instrumentation,dashboard);
            onMain(instrumentation,()->{
                SagaAngleView view=dashboard.angles;Bitmap atlas=view.atlas;
                check(view.active&&atlas!=null&&!atlas.isRecycled(),"selected image viewer loads its actual atlas");
                check(dashboard.showroom==null,"image angles do not retain a video decoder");
                check(!containsSurface(view),"image angles have no GL, video or browser rendering surface");
                check(atlas.hasAlpha(),"angle atlas retains transparency");
                check(atlas.getAllocationByteCount()<=2*1024*1024,"decoded angle cache stays within two MiB");
                check(atlas.getWidth()<1774&&atlas.getHeight()<887,"atlas is sampled down rather than decoded at full source size");
                for(int frame=0;frame<8;frame++){
                    int column=frame%4,row=frame/4;
                    int left=column*atlas.getWidth()/4,right=(column+1)*atlas.getWidth()/4;
                    int top=row*atlas.getHeight()/2,bottom=(row+1)*atlas.getHeight()/2;
                    int clear=0,visible=0;
                    for(int y=top;y<bottom;y+=4)for(int x=left;x<right;x+=4){int alpha=atlas.getPixel(x,y)>>>24;if(alpha==0)clear++;if(alpha>200)visible++;}
                    check(clear>20&&visible>20,"image angle "+frame+" contains both a visible car and transparent surroundings");
                }
                view.setFrame(0);check(view.frame()==0,"first angle is selectable");
                view.setFrame(8);check(view.frame()==0,"forward angle index wraps after eight images");
                view.setFrame(-1);check(view.frame()==7,"negative angle index wraps to the last image");
                view.setFrame(1001);check(view.frame()==1,"large stored angle indices normalize into the valid range");
                view.reset();check(view.frame()==7,"reset restores the selected default car angle");
                check(preferences.getInt("vehicle_angle_frame",-1)==7,"reset saves the default angle");
                check(view.getWidth()>0&&view.getHeight()>0,"angle view has a measured drawing area");
                Bitmap first=capture(view,0),side=capture(view,2);
                try{check(!first.sameAs(side),"changing the angle draws a different supplied image");}finally{first.recycle();side.recycle();}
                view.setFrame(0);
                long t=SystemClock.uptimeMillis();float center=view.getWidth()/2f,y=view.getHeight()/2f,step=view.getWidth()/8f;
                touch(view,t,t,MotionEvent.ACTION_DOWN,center,y);
                touch(view,t,t+30,MotionEvent.ACTION_MOVE,center+step*1.4f,y);
                touch(view,t,t+60,MotionEvent.ACTION_UP,center+step*1.4f,y);
                check(view.frame()==7,"right drag moves to the previous supplied angle and wraps");
                check(preferences.getInt("vehicle_angle_frame",-1)==7,"completed drag persists the selected angle");
                t+=1000;
                touch(view,t,t,MotionEvent.ACTION_DOWN,center,y);
                touch(view,t,t+30,MotionEvent.ACTION_MOVE,center-step*1.4f,y);
                touch(view,t,t+60,MotionEvent.ACTION_UP,center-step*1.4f,y);
                check(view.frame()==0,"left drag moves forward to the next supplied angle");
                check(view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,null)&&view.frame()==7,"accessible backward action also wraps correctly");
                check(view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,null)&&view.frame()==0,"accessible forward action selects the next angle");
                view.setFrame(3);view.pause();
                check(!view.active&&view.atlas==null,"pause clears the image cache reference");
                view.resume();
            });
            ready(instrumentation,dashboard);
            onMain(instrumentation,()->{
                SagaAngleView view=dashboard.angles;
                check(view.frame()==3,"resuming preserves the selected image angle");
                check(view.active&&view.atlas!=null,"resume reloads a usable image cache");
                view.pause();view.resume();view.pause();
            });
            // Queue after any outstanding decode; then drain UI callbacks before checking
            // that a late decode completion cannot republish into a paused viewer.
            dashboard.io.submit(()->{}).get(15,TimeUnit.SECONDS);
            onMain(instrumentation,()->{
                check(!dashboard.angles.active&&dashboard.angles.atlas==null,"late image decode cannot repopulate a paused view");
                dashboard.angles.resume();
            });
            ready(instrumentation,dashboard);
            final int[] idle={-1};onMain(instrumentation,()->idle[0]=dashboard.angles.frame());
            SystemClock.sleep(120);
            onMain(instrumentation,()->check(dashboard.angles.frame()==idle[0],"idle images do not advance on an animation timer"));
        }finally{
            onMain(instrumentation,()->{
                dashboard.setVehicleVideo(false);dashboard.angles.setFrame(savedFrame);
                SharedPreferences.Editor edit=preferences.edit();
                if(hadFrame)edit.putInt("vehicle_angle_frame",savedFrame);else edit.remove("vehicle_angle_frame");
                if(hadView)edit.putString("vehicle_view",savedView);else edit.remove("vehicle_view");
                edit.commit();
            });
        }
        return assertions;
    }
    private static Bitmap capture(SagaAngleView view,int frame){
        view.setFrame(frame);Bitmap image=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(image));return image;
    }
    private static void touch(SagaAngleView view,long down,long now,int action,float x,float y){
        MotionEvent event=MotionEvent.obtain(down,now,action,x,y,0);try{view.onTouchEvent(event);}finally{event.recycle();}
    }
    private static boolean containsSurface(View view){
        for(Class<?> type=view.getClass();type!=null;type=type.getSuperclass()){
            String name=type.getName();if(name.equals("android.view.SurfaceView")||name.equals("android.view.TextureView")||name.equals("android.webkit.WebView"))return true;
        }
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(containsSurface(group.getChildAt(i)))return true;}
        return false;
    }
    private static void ready(Instrumentation instrumentation,DashboardActivity dashboard) throws Exception {
        long deadline=SystemClock.elapsedRealtime()+30000;final boolean[] ready={false};
        while(!ready[0]&&SystemClock.elapsedRealtime()<deadline){
            onMain(instrumentation,()->ready[0]=dashboard.angles!=null&&dashboard.angles.active&&dashboard.angles.atlas!=null);
            if(!ready[0])SystemClock.sleep(25);
        }
        if(!ready[0])throw new AssertionError("Image angle atlas did not load within 30 seconds");
    }
    private static void onMain(Instrumentation instrumentation,Work work) throws Exception {
        final Throwable[] error={null};instrumentation.runOnMainSync(()->{try{work.run();}catch(Throwable failure){error[0]=failure;}});
        if(error[0] instanceof Error)throw (Error)error[0];if(error[0] instanceof Exception)throw (Exception)error[0];if(error[0]!=null)throw new AssertionError(error[0]);
    }
}
