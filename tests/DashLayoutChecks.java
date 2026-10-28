package com.shazwan.sagadash;

import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

/** Layout persistence, resolution changes and touch contracts without vendor apps or networking. */
final class DashLayoutChecks {
    int assertions;
    void check(boolean condition,String message){if(!condition)throw new AssertionError(message);assertions++;}
    boolean close(float a,float b){return Math.abs(a-b)<1.1f;}
    void same(RectF a,RectF b,String message){check(close(a.left,b.left)&&close(a.top,b.top)&&close(a.right,b.right)&&close(a.bottom,b.bottom),message);}

    int run(Instrumentation instrumentation) throws Exception {
        final Throwable[] failure={null};
        instrumentation.runOnMainSync(()->{
            Context context=instrumentation.getTargetContext();
            SharedPreferences prefs=context.createDeviceProtectedStorageContext().getSharedPreferences("__layout_checks__",0);
            prefs.edit().clear().putString("unrelated","keep").commit();
            try{
                check(DashLayoutSurface.decode("NaN,0,.2,.3")==null,"nonfinite saved position is rejected");
                check(DashLayoutSurface.decode("0,0,-1,.3")==null,"negative saved size is rejected");
                check(DashLayoutSurface.decode("broken")==null,"malformed saved layout falls back to default");
                check(DashLayoutSurface.decode("3.4E38,0,3.4E38,.3")==null,"overflowing saved bounds are rejected");
                RectF bounded=DashLayoutSurface.bounded(new RectF(-50,480,10,490),1000,500,240,180);
                same(bounded,new RectF(0,320,240,500),"minimum size and offscreen positions clamp together");
                bounded=DashLayoutSurface.bounded(new RectF(-5,-5,500,500),100,80,240,180);
                same(bounded,new RectF(0,0,100,80),"small surfaces remain bounded even below the normal minimum size");
                DashLayoutSurface surface=surface(context,prefs,1000,500);
                RectF vehicle=surface.boundsFor("vehicle"),shortcuts=surface.boundsFor("shortcuts"),media=surface.boundsFor("media"),dock=surface.boundsFor("dock");
                same(vehicle,new RectF(0,6,495,407),"default left panel occupies half the content");
                same(shortcuts,new RectF(505,0,1000,115),"default shortcuts retain upper right location");
                same(media,new RectF(505,6,1000,407),"default right panel matches left size and vertical alignment");
                same(dock,new RectF(0,413,1000,500),"default dock remains below the panels");
                MotionEvent down=event(MotionEvent.ACTION_DOWN,100,100);
                check(!surface.onInterceptTouchEvent(down)&&!surface.onTouchEvent(down),"normal mode leaves video, car and child touch gestures alone");down.recycle();
                surface.setEditing(true);down=event(MotionEvent.ACTION_DOWN,100,100);
                check(surface.isEditing()&&surface.onInterceptTouchEvent(down),"edit mode explicitly intercepts panel movement");down.recycle();
                gesture(surface,100,100,200,150,false);measure(surface,1000,500);
                RectF moved=surface.boundsFor("vehicle");
                same(moved,new RectF(100,56,595,457),"drag moves the whole panel without changing its size");
                check(!prefs.contains("panel_layout_v1_vehicle"),"dragging does not repeatedly write preferences");
                gesture(surface,995,402,500,150,false);measure(surface,1000,500);
                RectF resized=surface.boundsFor("media");
                check(close(resized.width(),260)&&close(resized.height(),200),"bottom right handle respects a usable minimum panel size");
                check(close(resized.left,media.left)&&close(resized.top,media.top),"resizing anchors the panel's upper left corner");
                surface.setEditing(false);
                check(prefs.contains("panel_layout_v1_vehicle")&&!surface.isEditing(),"Done commits panel layout");
                DashLayoutSurface restored=surface(context,prefs,1000,500);
                same(restored.boundsFor("vehicle"),moved,"saved positions survive a new layout instance");
                same(restored.boundsFor("media"),resized,"saved sizes survive a new layout instance");
                check("media".equals(restored.getChildAt(restored.getChildCount()-1).getTag()),"panel stacking order survives a new layout instance");
                DashLayoutSurface large=surface(context,prefs,2000,1000);
                RectF twice=large.boundsFor("vehicle");
                check(close(twice.left,moved.left*2)&&close(twice.top,moved.top*2)&&close(twice.width(),moved.width()*2),"normalized positions adapt to a different screen resolution");
                restored.setEditing(true);
                gesture(restored,moved.left+20,moved.top+20,5000,5000,false);measure(restored,1000,500);
                RectF edge=restored.boundsFor("vehicle");
                check(close(edge.right,1000)&&close(edge.bottom,500),"dragging cannot lose a panel beyond the screen");
                restored.resetLayout();
                same(restored.boundsFor("vehicle"),vehicle,"Reset previews the original positions while editing");
                restored.cancelEditing();measure(restored,1000,500);
                same(restored.boundsFor("vehicle"),moved,"Cancel restores the layout from before the entire edit session");
                check("media".equals(restored.getChildAt(restored.getChildCount()-1).getTag()),"Cancel restores the previous stacking order");
                same(surface(context,prefs,1000,500).boundsFor("vehicle"),moved,"Cancel leaves persisted layout unchanged");
                restored.setEditing(true);
                gesture(restored,moved.left+10,moved.top+10,moved.left+60,moved.top+60,true);
                same(restored.boundsFor("vehicle"),moved,"cancelled pointer gesture rolls back its partial movement");
                restored.setEditing(false);restored.resetLayout();measure(restored,1000,500);
                same(restored.boundsFor("vehicle"),vehicle,"Reset outside editing restores defaults immediately");
                check("dock".equals(restored.getChildAt(restored.getChildCount()-1).getTag()),"Reset restores the original panel stacking order");
                check(!prefs.contains("panel_layout_v1_vehicle")&&"keep".equals(prefs.getString("unrelated","")),"Reset removes only layout preferences");
                prefs.edit().putInt("panel_layout_v1_vehicle",42).putInt("panel_layout_v1_order",42).commit();
                same(surface(context,prefs,1000,500).boundsFor("vehicle"),vehicle,"wrong-type preference values do not crash the launcher");
            }catch(Throwable error){failure[0]=error;}
            finally{prefs.edit().clear().commit();}
        });
        if(failure[0] instanceof Error)throw (Error)failure[0];
        if(failure[0] instanceof Exception)throw (Exception)failure[0];
        if(failure[0]!=null)throw new AssertionError(failure[0]);
        return assertions;
    }
    private DashLayoutSurface surface(Context context,SharedPreferences prefs,int width,int height){
        DashLayoutSurface out=new DashLayoutSurface(context,prefs,1);
        for(String key:new String[]{"vehicle","shortcuts","media","dock"}){View view=new View(context);view.setTag(key);out.addPanel(key,view);}
        measure(out,width,height);return out;
    }
    private static void measure(DashLayoutSurface surface,int width,int height){
        surface.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
        surface.layout(0,0,width,height);
    }
    private static MotionEvent event(int action,float x,float y){long now=SystemClock.uptimeMillis();return MotionEvent.obtain(now,now,action,x,y,0);}
    private static void gesture(DashLayoutSurface surface,float x,float y,float endX,float endY,boolean cancel){
        MotionEvent down=event(MotionEvent.ACTION_DOWN,x,y),move=event(MotionEvent.ACTION_MOVE,endX,endY),up=event(cancel?MotionEvent.ACTION_CANCEL:MotionEvent.ACTION_UP,endX,endY);
        try{surface.onTouchEvent(down);surface.onTouchEvent(move);surface.onTouchEvent(up);}finally{down.recycle();move.recycle();up.recycle();}
    }
}
