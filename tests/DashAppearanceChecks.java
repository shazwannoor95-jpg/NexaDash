package com.shazwan.sagadash;

import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Theme lifecycle and state preservation; does not claim physical head-unit performance. */
final class DashAppearanceChecks {
    private static final String[] KEYS={"appearance_theme","appearance_brightness","appearance_transparency",
        "appearance_neon","appearance_glass","appearance_animation","appearance_reduce_motion","style_neon"};
    int assertions;
    void check(boolean condition,String message){if(!condition)throw new AssertionError(message);assertions++;}

    int run(Instrumentation instrumentation,DashboardActivity dashboard) throws Exception {
        int before=assertions;
        waitMeasured(instrumentation,dashboard);
        SharedPreferences preferences=dashboard.dashPrefs();
        Map<String,Object> saved=new HashMap<>();
        for(String key:KEYS)if(preferences.contains(key))saved.put(key,preferences.getAll().get(key));
        Field zoom=SagaAngleView.class.getDeclaredField("zoom");zoom.setAccessible(true);
        final int[] oldFrame={0};final float[] oldZoom={1};final Snapshot[] snapshot={null};
        onMain(instrumentation,()->{
            oldFrame[0]=dashboard.angles.frame();oldZoom[0]=zoom.getFloat(dashboard.angles);
            // Deliberately use non-default viewer state so a hidden rebuild/reset is detectable.
            dashboard.angles.setFrame(4);zoom.setFloat(dashboard.angles,1.17f);dashboard.angles.invalidate();
            snapshot[0]=new Snapshot(dashboard,zoom);
        });
        try{
            onMain(instrumentation,()->{
                SharedPreferences.Editor edit=preferences.edit();for(String key:KEYS)edit.remove(key);edit.commit();
                dashboard.applyStyle();dashboard.appearance.start();
                check(dashboard.appearance.synthwave(),"new appearance defaults to the requested Synthwave theme");
                check(dashboard.appearance.brightness()==65&&dashboard.appearance.transparency()==25&&dashboard.appearance.intensity()==65,"default readability controls have bounded conservative values");
                check(dashboard.appearance.glass(),"glass panels are enabled by default");
                check(dashboard.appearance.mode().equals("static")&&dashboard.appearance.reduceMotion(),"static and Reduce motion are the default");
                check(!dashboard.appearance.synth.isAnimating(),"default appearance does not schedule ambient animation");
                check(dashboard.root.getBackground()==dashboard.appearance.synth,"wallpaper is attached behind the existing root");
                snapshot[0].verify(dashboard,zoom);
            });
            waitImage(instrumentation,dashboard);
            final Bitmap[] decoded={null};
            onMain(instrumentation,()->{
                Bitmap image=dashboard.appearance.image;decoded[0]=image;
                check(image!=null&&!image.isRecycled(),"Synthwave image is decoded asynchronously");
                check(image.getAllocationByteCount()<=2*1024*1024,"decoded wallpaper stays within two MiB");
                check(image.getConfig()==Bitmap.Config.RGB_565,"opaque wallpaper uses the requested RGB565 memory budget");
                BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
                BitmapFactory.decodeResource(dashboard.getResources(),R.drawable.saga_synthwave,bounds);
                check(bounds.outWidth>0&&bounds.outHeight>0&&Math.abs((float)image.getWidth()/image.getHeight()-(float)bounds.outWidth/bounds.outHeight)<.01f,"sampling preserves the supplied wallpaper aspect ratio");
                check(!dashboard.appearance.synth.isAnimating(),"loading the image does not silently enable motion");
                Drawable glass=dashboard.appearance.panel(DashboardActivity.PANEL,18);
                int glassAlpha=centerAlpha(glass);
                check(glassAlpha>0&&glassAlpha<255,"Synthwave panels have a translucent fill");

                preferences.edit().putInt("appearance_brightness",30).putInt("appearance_transparency",40).putInt("appearance_neon",20).commit();
                dashboard.applyStyle();
                check(dashboard.appearance.brightness()==30&&dashboard.appearance.transparency()==40&&dashboard.appearance.intensity()==20,"appearance controls read persisted custom values");
                check(dashboard.appearance.image==decoded[0],"changing opacity, brightness or neon does not decode another wallpaper");
                check(centerAlpha(dashboard.appearance.panel(DashboardActivity.PANEL,18))<glassAlpha,"increasing panel transparency reduces the rendered fill opacity");
                snapshot[0].verify(dashboard,zoom);
                preferences.edit().putBoolean("appearance_glass",false).commit();dashboard.applyStyle();
                check(!dashboard.appearance.glass()&&dashboard.appearance.image==decoded[0],"glass finish can be disabled without another wallpaper decode");
                snapshot[0].verify(dashboard,zoom);
                preferences.edit().putBoolean("appearance_glass",true).commit();dashboard.applyStyle();

                preferences.edit().putString("appearance_animation","cinematic").putBoolean("appearance_reduce_motion",true).commit();
                dashboard.applyStyle();
                check(!dashboard.appearance.synth.isAnimating(),"Reduce motion overrides a persisted Cinematic selection");
                preferences.edit().putBoolean("appearance_reduce_motion",false).commit();dashboard.applyStyle();
                check(dashboard.appearance.synth.isAnimating(),"Cinematic effects run only after motion is explicitly allowed");
                preferences.edit().putString("appearance_animation","static").commit();dashboard.applyStyle();
                check(!dashboard.appearance.synth.isAnimating(),"switching to Static cancels running effects");
                preferences.edit().putString("appearance_animation","ambient").commit();dashboard.applyStyle();
                check(dashboard.appearance.synth.isAnimating(),"Ambient effects are independently selectable");
                dashboard.appearance.stop();
                check(dashboard.appearance.image==null&&!dashboard.appearance.synth.isAnimating(),"pause releases the wallpaper reference and stops effects");

                preferences.edit().putString("appearance_theme","original").commit();dashboard.applyStyle();dashboard.appearance.start();
                check(!dashboard.appearance.synthwave()&&dashboard.root.getBackground() instanceof DashBackdrop,"Original restores the previous dashboard backdrop");
                check(dashboard.appearance.image==null&&!dashboard.appearance.synth.isAnimating(),"Original has no Synthwave bitmap or scheduled effects");
                check(centerAlpha(dashboard.appearance.panel(DashboardActivity.PANEL,18))==255,"Original restores opaque panel fill");
                snapshot[0].verify(dashboard,zoom);

                // Queue an image load and revoke it before its main-thread result may be published.
                preferences.edit().putString("appearance_theme","synthwave").putString("appearance_animation","cinematic").putBoolean("appearance_reduce_motion",false).commit();
                dashboard.applyStyle();dashboard.appearance.start();dashboard.appearance.stop();
            });
            // The fence follows any in-flight decode on the controller's existing serial worker.
            Future<?> fence=dashboard.io.submit(()->{});fence.get(30,TimeUnit.SECONDS);
            onMain(instrumentation,()->{
                check(dashboard.appearance.image==null,"a late decoded image cannot repopulate a paused controller");
                check(!dashboard.appearance.synth.isAnimating(),"late image completion cannot restart paused effects");
                snapshot[0].verify(dashboard,zoom);
                preferences.edit().putString("appearance_animation","static").putBoolean("appearance_reduce_motion",true).commit();
                dashboard.applyStyle();dashboard.appearance.start();
            });
            waitImage(instrumentation,dashboard);
            onMain(instrumentation,()->{
                check(dashboard.appearance.image!=null&&!dashboard.appearance.synth.isAnimating(),"returning to Synthwave reloads a bounded static wallpaper");
                snapshot[0].verify(dashboard,zoom);
            });
        }finally{
            onMain(instrumentation,()->{
                dashboard.appearance.stop();
                SharedPreferences.Editor restore=preferences.edit();for(String key:KEYS)restore.remove(key);
                for(Map.Entry<String,Object> entry:saved.entrySet())put(restore,entry.getKey(),entry.getValue());
                restore.commit();
                dashboard.angles.setFrame(oldFrame[0]);zoom.setFloat(dashboard.angles,oldZoom[0]);dashboard.angles.invalidate();
                dashboard.applyStyle();if(dashboard.visible)dashboard.appearance.start();else dashboard.appearance.stop();
            });
        }
        return assertions-before;
    }

    private final class Snapshot {
        final Object root,left,right,leftChild,rightChild,viewer,session,leftNavigation,rightNavigation;
        final String leftMode,rightMode,trackTitle,trackArtist,zone,dockOrder;
        final RectF[] bounds=new RectF[4];final String[] keys={"vehicle","shortcuts","media","dock"};
        final int frame,surfaces;final float magnification;final double trip;
        Snapshot(DashboardActivity dashboard,Field zoom) throws Exception {
            root=dashboard.root;left=dashboard.leftPane;right=dashboard.rightPane;
            leftChild=dashboard.leftPane.slot.getChildCount()==0?null:dashboard.leftPane.slot.getChildAt(0);
            rightChild=dashboard.rightPane.slot.getChildCount()==0?null:dashboard.rightPane.slot.getChildAt(0);
            leftNavigation=dashboard.leftPane.navigation;rightNavigation=dashboard.rightPane.navigation;
            viewer=dashboard.angles;session=dashboard.media;
            leftMode=dashboard.leftPane.mode;rightMode=dashboard.rightPane.mode;
            trackTitle=dashboard.title.getText().toString();trackArtist=dashboard.artist.getText().toString();
            zone=JakimSchedule.zone(dashboard);trip=dashboard.tripMetres;
            dockOrder=dashboard.dockOrder.toString();frame=dashboard.angles.frame();magnification=zoom.getFloat(dashboard.angles);
            surfaces=countSurfaces(dashboard.root);
            for(int i=0;i<keys.length;i++)bounds[i]=new RectF(dashboard.layoutSurface.boundsFor(keys[i]));
        }
        void verify(DashboardActivity dashboard,Field zoom) throws Exception {
            check(dashboard.root==root&&dashboard.leftPane==left&&dashboard.rightPane==right,"theme switching preserves the existing root and pane objects");
            check(leftMode.equals(dashboard.leftPane.mode)&&rightMode.equals(dashboard.rightPane.mode),"theme switching keeps both selected panel modes");
            check(leftChild==(dashboard.leftPane.slot.getChildCount()==0?null:dashboard.leftPane.slot.getChildAt(0))
                    &&rightChild==(dashboard.rightPane.slot.getChildCount()==0?null:dashboard.rightPane.slot.getChildAt(0)),"theme switching does not replace map or playback surfaces");
            check(dashboard.leftPane.navigation==leftNavigation&&dashboard.rightPane.navigation==rightNavigation,"navigation preview instances survive appearance changes");
            check(dashboard.angles==viewer&&dashboard.angles.frame()==frame&&Math.abs(zoom.getFloat(dashboard.angles)-magnification)<.0001f,"theme switching preserves the supplied car viewer, selected angle and pinch zoom");
            check(dashboard.media==session&&trackTitle.equals(dashboard.title.getText().toString())&&trackArtist.equals(dashboard.artist.getText().toString()),"media controller and displayed track data survive appearance changes");
            check(zone.equals(JakimSchedule.zone(dashboard))&&dashboard.tripMetres==trip,"appearance does not change the JAKIM zone or GPS trip data");
            check(dockOrder.equals(dashboard.dockOrder.toString()),"appearance preserves app dock order");
            for(int i=0;i<keys.length;i++)check(bounds[i].equals(dashboard.layoutSurface.boundsFor(keys[i])),"theme switching preserves "+keys[i]+" panel geometry");
            check(countSurfaces(dashboard.root)==surfaces,"appearance adds no browser or GPU rendering surface");
        }
    }
    private static int centerAlpha(Drawable drawable){
        Bitmap image=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);
        try{drawable.setBounds(0,0,128,128);drawable.draw(new Canvas(image));return image.getPixel(64,64)>>>24;}
        finally{image.recycle();}
    }
    private static int countSurfaces(View view){
        int count=view instanceof SurfaceView||view instanceof TextureView||view.getClass().getName().equals("android.webkit.WebView")?1:0;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)count+=countSurfaces(group.getChildAt(i));}
        return count;
    }
    private static void waitMeasured(Instrumentation instrumentation,DashboardActivity dashboard) throws Exception {
        long deadline=SystemClock.elapsedRealtime()+15000;final boolean[] ready={false};
        while(!ready[0]&&SystemClock.elapsedRealtime()<deadline){
            onMain(instrumentation,()->ready[0]=dashboard.layoutSurface.getWidth()>0&&dashboard.layoutSurface.getHeight()>0);
            if(!ready[0])SystemClock.sleep(50);
        }
        if(!ready[0])throw new AssertionError("Dashboard layout was not measured within 15 seconds");
    }
    private static void waitImage(Instrumentation instrumentation,DashboardActivity dashboard) throws Exception {
        long deadline=SystemClock.elapsedRealtime()+30000;final boolean[] ready={false};
        while(!ready[0]&&SystemClock.elapsedRealtime()<deadline){
            onMain(instrumentation,()->ready[0]=dashboard.appearance.image!=null);
            if(!ready[0])SystemClock.sleep(50);
        }
        if(!ready[0])throw new AssertionError("Synthwave wallpaper did not decode within 30 seconds");
    }
    private interface Work {void run() throws Exception;}
    private static void onMain(Instrumentation instrumentation,Work work) throws Exception {
        final Throwable[] failure={null};instrumentation.runOnMainSync(()->{try{work.run();}catch(Throwable error){failure[0]=error;}});
        if(failure[0] instanceof Error)throw (Error)failure[0];
        if(failure[0] instanceof Exception)throw (Exception)failure[0];
        if(failure[0]!=null)throw new AssertionError(failure[0]);
    }
    @SuppressWarnings("unchecked") private static void put(SharedPreferences.Editor editor,String key,Object value){
        if(value instanceof String)editor.putString(key,(String)value);
        else if(value instanceof Integer)editor.putInt(key,(Integer)value);
        else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
        else if(value instanceof Float)editor.putFloat(key,(Float)value);
        else if(value instanceof Long)editor.putLong(key,(Long)value);
        else if(value instanceof java.util.Set)editor.putStringSet(key,new java.util.HashSet<>((java.util.Set<String>)value));
    }
}
