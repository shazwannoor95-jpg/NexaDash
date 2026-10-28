package com.shazwan.sagadash;

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import java.util.HashMap;
import java.util.Map;

/** Real pane/lifecycle/session integration. No external app launch, GPS fix or tile request. */
final class DashCustomizationChecks {
    int assertions;
    void check(boolean condition,String message){if(!condition)throw new AssertionError(message);assertions++;}
    interface Work {void run() throws Exception;}
    interface Ready {boolean get();}

    int run(Instrumentation instrumentation,DashboardActivity dashboard) throws Exception {
        SharedPreferences preferences=dashboard.dashPrefs(),assignments=DashShortcuts.prefs(dashboard);
        Map<String,Object> saved=new HashMap<>(),savedAssignments=new HashMap<>();
        for(String key:new String[]{"pane_vehicle","pane_media","vehicle_video","vehicle_view","vehicle_angle_frame","vehicle_preview_source","lyrics"})saved.put(key,preferences.getAll().get(key));
        for(String key:new String[]{"YouTube app","TizenTube","Radio"})savedAssignments.put(key,assignments.getAll().get(key));
        final MediaSession[] session={null};final String[] selected={""};final Location[] fix={null};final boolean[] videoMode={true};
        try{
            onMain(instrumentation,()->{
                selected[0]=dashboard.media.selected;fix[0]=dashboard.latestFix;videoMode[0]=dashboard.vehicleVideo;
                dashboard.latestFix=null;preferences.edit().putBoolean("lyrics",false).apply();
                assignments.edit().remove("YouTube app").remove("TizenTube").remove("Radio").apply();
                DashMedia shared=dashboard.media;
                compactPanelChecks(dashboard);

                dashboard.leftPane.setMode("car");dashboard.setVehicleVideo(false);
                check(dashboard.angles!=null&&dashboard.angles.active&&dashboard.showroom==null,"idle car panel uses the active image angle viewer without a video decoder");
                check(dashboard.angles.getParent()==dashboard.vehicleStage,"angle images occupy the existing vehicle stage");
                check(!containsRenderSurface(dashboard.angles),"idle angle viewer has no GL, video or web rendering surface");
                check(!containsWebView(dashboard.root),"initial dashboard does not instantiate a WebView");

                dashboard.setVehicleVideo(true);SagaShowroom video=dashboard.showroom;
                check(video!=null&&video.active,"explicit video selection lazily creates its player view");
                dashboard.leftPane.setMode("navigation");
                check(dashboard.angles!=null&&!dashboard.angles.active&&dashboard.angles.atlas==null&&dashboard.showroom==null&&video.player==null&&!video.active,"hiding the car panel releases image and video references");
                check(dashboard.leftPane.navigation!=null&&dashboard.leftPane.sourcePanel==null,"left panel switches to native navigation preview");
                check("navigation".equals(preferences.getString("pane_vehicle","")),"left pane choice is saved");
                DashNavigationView leftMap=dashboard.leftPane.navigation;
                dashboard.rightPane.setMode("navigation");
                check(dashboard.rightPane.navigation!=null&&dashboard.rightPane.navigation!=leftMap,"both panels can show independent native map views");
                check("navigation".equals(preferences.getString("pane_media","")),"right pane choice is saved");
                check(!containsWebView(dashboard.root),"navigation preview does not create a web browser");

                dashboard.leftPane.setMode("youtube");
                check(dashboard.leftPane.navigation==null&&leftMap.getParent()==null,"switching panels detaches the old navigation view");
                check(dashboard.leftPane.sourcePanel!=null&&!dashboard.leftPane.sourcePanel.play.isEnabled(),"YouTube controls stay disabled without a matching session");
                check("youtube".equals(preferences.getString("pane_vehicle","")),"YouTube pane choice is saved");
                dashboard.leftPane.setMode("tizentube");
                check(dashboard.leftPane.sourcePanel!=null&&dashboard.leftPane.slot.getChildCount()==1,"TizenTube uses one native source-control view");
                check(!containsWebView(dashboard.root),"source panels do not retain an embedded browser");
                dashboard.rightPane.setMode("radio");
                check(dashboard.rightPane.sourcePanel!=null&&!dashboard.rightPane.sourcePanel.play.isEnabled(),"Radio controls do not commandeer a nonmatching media source");

                dashboard.leftPane.setMode("car");dashboard.setVehicleVideo(false);SagaAngleView angles=dashboard.angles;
                check(angles!=null&&dashboard.showroom==null,"returning to the car panel restores its image angle viewer");
                dashboard.leftPane.setMode("media");
                check(dashboard.angles==angles&&dashboard.showroom==null&&dashboard.vehicleContent.getParent()==null&&!angles.active&&angles.atlas==null,"switching away detaches the angle viewer and clears its bitmap reference");
                dashboard.rightPane.setMode("media");
                check(dashboard.leftPane.mediaPanel!=null&&dashboard.rightPane.mediaPanel==dashboard.primaryMedia,"either panel can show native media controls");
                check(dashboard.leftPane.mediaPanel!=dashboard.rightPane.mediaPanel,"two media panes use independent UI views");
                check(dashboard.media==shared,"pane switching reuses the dashboard's single media-session observer");
                dashboard.rightPane.setMode("car");
                check("media".equals(dashboard.rightPane.mode),"right panel rejects an unsupported car mode safely");
                dashboard.leftPane.setMode("invalid-saved-mode");
                check("media".equals(dashboard.leftPane.mode),"an invalid saved pane mode falls back to media controls");
                check(dashboard.leftPane.slot.getChildCount()==1&&dashboard.rightPane.slot.getChildCount()==1,"mode changes do not stack hidden content views");

                long now=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,100,100,0);
                try{
                    check(!dashboard.layoutSurface.isEditing()&&!dashboard.layoutSurface.onInterceptTouchEvent(down),"normal layout does not intercept car, video or control gestures");
                    dashboard.layoutSurface.setEditing(true);
                    check(dashboard.layoutSurface.onInterceptTouchEvent(down),"panel movement requires explicit Edit mode");
                    dashboard.layoutSurface.cancelEditing();
                    check(!dashboard.layoutSurface.isEditing(),"cancelling Edit restores normal child interaction");
                }finally{down.recycle();}

                session[0]=new MediaSession(dashboard,"Saga customization checks");
                session[0].setCallback(new MediaSession.Callback(){
                    @Override public void onPause(){session[0].setPlaybackState(state(PlaybackState.STATE_PAUSED));}
                    @Override public void onPlay(){session[0].setPlaybackState(state(PlaybackState.STATE_PLAYING));}
                },new Handler(Looper.getMainLooper()));
                session[0].setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,"Customization check track").putString(MediaMetadata.METADATA_KEY_ARTIST,"Synthetic session").build());
                session[0].setPlaybackState(state(PlaybackState.STATE_PLAYING));session[0].setActive(true);
                dashboard.media.choose(dashboard.getPackageName());
            });
            await(instrumentation,()->dashboard.leftPane.mediaPanel!=null&&"Customization check track".contentEquals(dashboard.leftPane.mediaPanel.title.getText()),"synthetic media metadata did not reach the left pane");
            onMain(instrumentation,()->{
                check("Customization check track".contentEquals(dashboard.rightPane.mediaPanel.title.getText()),"both media panes show the same real Android session metadata");
                check("Pause".contentEquals(dashboard.leftPane.mediaPanel.play.getText())&&"Pause".contentEquals(dashboard.rightPane.mediaPanel.play.getText()),"both media panes reflect shared playing state");
                dashboard.leftPane.setMode("youtube");
                check(!dashboard.leftPane.sourcePanel.play.isEnabled()&&!"Customization check track".contentEquals(dashboard.leftPane.sourcePanel.title.getText()),"YouTube pane does not display or control an unrelated active track");
                dashboard.rightPane.setMode("radio");
                check(!dashboard.rightPane.sourcePanel.play.isEnabled(),"unassigned Radio remains disabled while another source plays");
                DashShortcuts.assign(dashboard,"Radio",new ComponentName(dashboard.getPackageName(),DashboardActivity.class.getName()));
                dashboard.refreshMedia();
                check(dashboard.rightPane.sourcePanel.play.isEnabled()&&"Customization check track".contentEquals(dashboard.rightPane.sourcePanel.title.getText()),"assigned source session enables Radio metadata and transport controls");
                dashboard.rightPane.sourcePanel.play.performClick();
            });
            await(instrumentation,()->!dashboard.media.playing()&&"Play".contentEquals(dashboard.rightPane.sourcePanel.play.getText()),"source control did not pause its assigned session");
            onMain(instrumentation,()->{
                check("Play".contentEquals(dashboard.rightPane.sourcePanel.play.getText()),"source-specific controls reflect the real pause result");
                dashboard.leftPane.setMode("media");dashboard.rightPane.setMode("media");dashboard.refreshMedia();
                check("Play".contentEquals(dashboard.leftPane.mediaPanel.play.getText())&&"Play".contentEquals(dashboard.rightPane.mediaPanel.play.getText()),"both shared media panes reflect pause after switching back");
                check("1:05".contentEquals(dashboard.leftPane.mediaPanel.progress.getText())&&"1:05".contentEquals(dashboard.rightPane.mediaPanel.progress.getText()),"both media panes share the published playback position");
                check(!containsWebView(dashboard.root),"all tested pane choices remain native without a hidden WebView");
            });
        }finally{
            onMain(instrumentation,()->{
                if(session[0]!=null)session[0].release();
                dashboard.releaseVehicleResources();dashboard.vehicleVideo=videoMode[0];dashboard.latestFix=fix[0];
                dashboard.leftPane.setMode("car");dashboard.rightPane.setMode("media");dashboard.releaseVehicleResources();
                restore(preferences,saved);restore(assignments,savedAssignments);dashboard.media.choose(selected[0]);
            });
        }
        return assertions;
    }
    private void compactPanelChecks(DashboardActivity dashboard){
        String longTitle="A long title that must remain readable without pushing any controls outside the smallest dashboard panel ";
        String longArtist="A long artist and album description that is ellipsized while the dashboard panel is compact ";
        String[] controls={"Open TizenTube","Choose app for TizenTube","Previous track","Play or pause","Next track"};
        for(float unit:new float[]{.8f,1f}){
            final int[] actions={0};
            DashSourcePanel panel=new DashSourcePanel(dashboard,unit,"TizenTube",()->actions[0]++,()->actions[0]++,()->actions[0]++,()->actions[0]++,()->actions[0]++);
            panel.setTrack(longTitle+longTitle,longArtist+longArtist,true,true);
            measure(panel,Math.round(212*unit),Math.round(145*unit));
            check(panel.title.getMaxLines()==1&&panel.artist.getMaxLines()==1&&panel.getChildAt(0).getVisibility()==View.GONE,"small source body uses compact single-line metadata at scale "+unit);
            check(insideTree(panel),"all source metadata and rows fit inside the smallest 240x200-unit outer pane at scale "+unit);
            for(String description:controls){
                View control=described(panel,description);
                check(control!=null&&control.isEnabled()&&control.getWidth()>0&&control.getHeight()>0&&within(control,panel),description+" stays visible and reachable at minimum size, scale "+unit);
                control.performClick();
            }
            check(actions[0]==controls.length,"all five compact source controls retain their actions at scale "+unit);
            measure(panel,Math.round(600*unit),Math.round(350*unit));
            check(panel.getChildAt(0).getVisibility()==View.VISIBLE&&panel.getChildAt(3).getVisibility()==View.VISIBLE&&panel.getChildAt(3).getHeight()>0&&panel.title.getMaxLines()==2&&panel.artist.getMaxLines()==2,"enlarging a source panel restores its original source label, explanation and multiline metadata at scale "+unit);
            check(insideTree(panel)&&longTitle.concat(longTitle).contentEquals(panel.title.getText()),"enlarging preserves complete track metadata and control bounds at scale "+unit);
            measure(panel,Math.round(212*unit),Math.round(145*unit));
            check(insideTree(panel)&&panel.title.getMaxLines()==1,"source controls remain bounded after repeated resize at scale "+unit);

            DashDockItem item=new DashDockItem(dashboard,unit,1.3f,"Bluetooth","B",DashboardActivity.BLUE,()->actions[0]++,true);
            measure(item,Math.round(25*unit),Math.round(56*unit));
            check(insideTree(item)&&item.icon.getWidth()<=Math.round(25*unit)&&item.icon.getBottom()<=item.caption.getTop(),"130-percent dock icon and caption fit a minimum 280x64-unit eight-item dock at scale "+unit);
            check(item.caption.getMaxLines()==1&&item.caption.getEllipsize()==android.text.TextUtils.TruncateAt.END,"compact dock labels use one-line ellipsis at scale "+unit);
            float compactFont=item.icon.getTextSize();item.performClick();
            check(actions[0]==controls.length+1&&"Bluetooth".contentEquals(item.getContentDescription()),"resizing retains the dock shortcut action and accessible full name at scale "+unit);
            measure(item,Math.round(125*unit),Math.round(100*unit));
            check(item.icon.getWidth()==Math.round(30*1.3f*unit)&&item.icon.getHeight()==Math.round(30*1.3f*unit)&&item.caption.getHeight()==Math.round(17*unit)&&item.icon.getTextSize()>compactFont&&insideTree(item),"enlarging the dock restores the requested 130-percent icon and standard caption height at scale "+unit);
            int iconWidth=item.icon.getWidth(),captionHeight=item.caption.getHeight();
            item.style(true,170,.65f,true,true);
            check(item.getBackground() instanceof android.graphics.drawable.StateListDrawable&&item.icon.getWidth()==iconWidth&&item.caption.getHeight()==captionHeight,"reduced-motion glass styling uses immediate native states and preserves dock geometry at scale "+unit);
            android.graphics.Bitmap idle=background(item),active=null,restored=null;
            try{
                item.setSelected(true);active=background(item);
                check(!idle.sameAs(active),"actual selected-session state renders a visible dock indicator at scale "+unit);
                item.setSelected(false);restored=background(item);
                check(idle.sameAs(restored),"clearing selected-session state removes its dock indicator immediately at scale "+unit);
            }finally{idle.recycle();if(active!=null)active.recycle();if(restored!=null)restored.recycle();}
            item.style(true,170,.65f,true,false);
            check(item.getBackground() instanceof android.graphics.drawable.RippleDrawable,"motion-enabled dock styling uses Android's bounded native ripple at scale "+unit);
            item.style(false,170,.65f,true,true);
            check(((android.graphics.drawable.GradientDrawable)item.icon.getBackground()).getColor().getDefaultColor()==DashboardActivity.BLUE,"original theme restores the shortcut's original badge color at scale "+unit);
            DashDockItem normal=new DashDockItem(dashboard,unit,1,"Music","M",DashboardActivity.BLUE,null,false);
            measure(normal,Math.round(125*unit),Math.round(79*unit));
            check(normal.icon.getWidth()==Math.round(30*unit)&&normal.caption.getHeight()==Math.round(17*unit)&&insideTree(normal),"normal dock dimensions retain the original 30-unit icon and 17-unit caption at scale "+unit);
        }
    }
    private static android.graphics.Bitmap background(View view){
        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(view.getWidth(),view.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.drawable.Drawable background=view.getBackground();background.setBounds(0,0,view.getWidth(),view.getHeight());background.draw(new android.graphics.Canvas(bitmap));return bitmap;
    }
    private static void measure(View view,int width,int height){
        view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);
    }
    private static boolean insideTree(View view){
        if(!(view instanceof ViewGroup))return true;
        ViewGroup group=(ViewGroup)view;
        for(int i=0;i<group.getChildCount();i++){
            View child=group.getChildAt(i);if(child.getVisibility()==View.GONE)continue;
            if(child.getLeft()<0||child.getTop()<0||child.getRight()>group.getWidth()||child.getBottom()>group.getHeight()||!insideTree(child))return false;
        }
        return true;
    }
    private static View described(View view,String description){
        if(android.text.TextUtils.equals(description,view.getContentDescription()))return view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View result=described(group.getChildAt(i),description);if(result!=null)return result;}}
        return null;
    }
    private static boolean within(View child,View ancestor){
        int x=0,y=0;View current=child;
        while(current!=ancestor){x+=current.getLeft();y+=current.getTop();if(!(current.getParent() instanceof View))return false;current=(View)current.getParent();}
        return x>=0&&y>=0&&x+child.getWidth()<=ancestor.getWidth()&&y+child.getHeight()<=ancestor.getHeight();
    }
    private static PlaybackState state(int value){return new PlaybackState.Builder().setState(value,65000,value==PlaybackState.STATE_PLAYING?1:0).setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE).build();}
    private static boolean containsRenderSurface(View view){
        for(Class<?> type=view.getClass();type!=null;type=type.getSuperclass()){
            String name=type.getName();
            if(name.equals("android.webkit.WebView")||name.equals("android.view.SurfaceView")||name.equals("android.view.TextureView"))return true;
        }
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(containsRenderSurface(group.getChildAt(i)))return true;}
        return false;
    }
    private static boolean containsWebView(View view){
        for(Class<?> type=view.getClass();type!=null;type=type.getSuperclass())if(type.getName().equals("android.webkit.WebView"))return true;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(containsWebView(group.getChildAt(i)))return true;}
        return false;
    }
    private static void restore(SharedPreferences preferences,Map<String,Object> snapshot){
        SharedPreferences.Editor editor=preferences.edit();
        for(Map.Entry<String,Object> entry:snapshot.entrySet()){
            Object value=entry.getValue();if(value==null)editor.remove(entry.getKey());else if(value instanceof Boolean)editor.putBoolean(entry.getKey(),(Boolean)value);else if(value instanceof Integer)editor.putInt(entry.getKey(),(Integer)value);else editor.putString(entry.getKey(),(String)value);
        }
        editor.commit();
    }
    private static void onMain(Instrumentation instrumentation,Work action) throws Exception {
        final Throwable[] failure={null};instrumentation.runOnMainSync(()->{try{action.run();}catch(Throwable error){failure[0]=error;}});
        if(failure[0] instanceof Error)throw (Error)failure[0];if(failure[0] instanceof Exception)throw (Exception)failure[0];if(failure[0]!=null)throw new AssertionError(failure[0]);
    }
    private static void await(Instrumentation instrumentation,Ready condition,String message) throws Exception {
        long deadline=SystemClock.elapsedRealtime()+10000;final boolean[] done={false};
        while(!done[0]&&SystemClock.elapsedRealtime()<deadline){onMain(instrumentation,()->done[0]=condition.get());if(!done[0])SystemClock.sleep(50);}
        if(!done[0])throw new AssertionError(message);
    }
}
