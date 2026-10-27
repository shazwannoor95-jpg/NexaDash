package com.shazwan.sagadash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import java.util.concurrent.*;

/** Theme-only preferences and a bounded, asynchronously loaded dashboard wallpaper. */
final class DashAppearance {
    static final int CYAN=0xff00f5ff,MAGENTA=0xffff2d95,GLASS=0xff080b1f;
    private final Activity activity;
    private final SharedPreferences preferences;
    private final ExecutorService io;
    private final Runnable changed;
    private final float unit;
    private final Handler main=new Handler(Looper.getMainLooper());
    final DashSynthBackdrop synth;
    Bitmap image;
    private Future<?> loading;
    private long generation;
    private boolean active;

    DashAppearance(Activity activity,SharedPreferences preferences,ExecutorService io,float unit,Runnable changed){
        this.activity=activity;this.preferences=preferences;this.io=io;this.unit=unit;this.changed=changed;
        synth=new DashSynthBackdrop(unit);refresh();
    }
    private String value(String key,String fallback){try{return preferences.getString(key,fallback);}catch(ClassCastException ignored){return fallback;}}
    private int value(String key,int fallback,int min,int max){try{return Math.max(min,Math.min(max,preferences.getInt(key,fallback)));}catch(ClassCastException ignored){return fallback;}}
    boolean synthwave(){return !"original".equals(value("appearance_theme","synthwave"));}
    int brightness(){return value("appearance_brightness",65,0,100);}
    int transparency(){return value("appearance_transparency",25,0,65);}
    boolean glass(){try{return preferences.getBoolean("appearance_glass",true);}catch(ClassCastException ignored){return true;}}
    int panelAlpha(){return Math.round(255*(100-transparency())/100f);}
    int intensity(){return value("appearance_neon",65,0,100);}
    boolean reduceMotion(){try{return preferences.getBoolean("appearance_reduce_motion",true);}catch(ClassCastException ignored){return true;}}
    String mode(){String mode=value("appearance_animation","static");return mode.equals("ambient")||mode.equals("cinematic")?mode:"static";}
    boolean neon(){return preferences.getBoolean("style_neon",true)&&intensity()>0;}
    float strength(){return neon()?intensity()/100f:0;}
    int accent(){return synthwave()?CYAN:DashboardActivity.BLUE;}
    void refresh(){
        synth.configure(brightness(),neon()?intensity():0,mode(),reduceMotion());
        synth.setActive(active&&synthwave());
        if(active&&synthwave())loadImage();else clearImage();
    }
    void start(){active=true;refresh();}
    void stop(){active=false;synth.setActive(false);clearImage();}
    private void clearImage(){
        ++generation;if(loading!=null){loading.cancel(false);loading=null;}
        image=null;synth.setImage(null);
    }
    private void loadImage(){
        if(image!=null||loading!=null)return;final long epoch=++generation;
        try{loading=io.submit(()->{
            Bitmap decoded=null;
            try{
                BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
                BitmapFactory.decodeResource(activity.getResources(),R.drawable.saga_synthwave,options);
                if(options.outWidth>0&&options.outHeight>0&&options.outWidth<=4096&&options.outHeight<=4096){
                    options.inSampleSize=1;
                    while((long)((options.outWidth+options.inSampleSize-1)/options.inSampleSize)*((options.outHeight+options.inSampleSize-1)/options.inSampleSize)*2>2*1024*1024)options.inSampleSize*=2;
                    options.inJustDecodeBounds=false;options.inScaled=false;options.inPreferredConfig=Bitmap.Config.RGB_565;options.inDither=true;
                    decoded=BitmapFactory.decodeResource(activity.getResources(),R.drawable.saga_synthwave,options);
                    if(decoded!=null&&decoded.getAllocationByteCount()>2*1024*1024){decoded.recycle();decoded=null;}
                }
            }catch(RuntimeException ignored){}
            final Bitmap result=decoded;
            main.post(()->{
                if(epoch!=generation||!active||!synthwave()){if(result!=null&&!result.isRecycled())result.recycle();return;}
                loading=null;image=result;synth.setImage(result);
            });
        });}catch(RejectedExecutionException ignored){loading=null;}
    }
    Drawable backdrop(){return synthwave()?synth:new DashBackdrop(unit,neon());}
    Drawable panel(int original,float radius){
        int color=synthwave()?(panelAlpha()<<24)|(GLASS&0x00ffffff):original;
        return new DashPanelDrawable(color,accent(),radius,neon(),strength(),glass());
    }
    Drawable control(int original,float radius){return new Control(original,radius);}
    final class Control extends StateListDrawable {
        final int original;
        Control(int original,float radius){
            this.original=original;
            int fill=synthwave()&&(original==DashboardActivity.PANEL||original==0xff0c1b29)?(panelAlpha()<<24)|(GLASS&0xffffff):original;
            addState(new int[]{android.R.attr.state_pressed},shape(fill,radius,true));
            addState(new int[]{android.R.attr.state_focused},shape(fill,radius,true));
            addState(new int[]{android.R.attr.state_selected},shape(fill,radius,true));
            addState(new int[]{},shape(fill,radius,false));
            if(!reduceMotion()){setEnterFadeDuration(90);setExitFadeDuration(120);}
        }
    }
    private GradientDrawable shape(int fill,float radius,boolean selected){
        GradientDrawable drawable=new GradientDrawable();drawable.setColor(fill);drawable.setCornerRadius(radius);
        if(glass()){
            int alpha=fill&0xff000000;
            drawable.setColors(new int[]{alpha|(DashPanelDrawable.tint(fill,0xffb1e7f5,selected?.20f:.12f)&0xffffff),fill});
            drawable.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        }
        int alpha=neon()?Math.round((selected?230:145)*strength()):255;
        int edge=neon()?(alpha<<24)|((selected&&synthwave()?MAGENTA:accent())&0xffffff):DashboardActivity.LINE;
        drawable.setStroke(Math.max(1,Math.round(unit*(selected?2:1))),edge);return drawable;
    }
    void show(){
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);int pad=Math.max(8,Math.round(20*unit));content.setPadding(pad,pad,pad,pad);
        Spinner theme=spinner(content,"Theme",new String[]{"Original","Synthwave"},synthwave()?1:0);
        CheckBox glass=new CheckBox(activity);glass.setText("Glass panels");glass.setChecked(glass());content.addView(glass);
        SeekBar brightness=slider(content,"Background brightness",brightness(),100);
        SeekBar transparency=slider(content,"Panel transparency",transparency(),65);
        SeekBar neon=slider(content,"Neon intensity",neon()?intensity():0,100);
        Spinner animation=spinner(content,"Animation mode",new String[]{"Static","Ambient","Cinematic"},mode().equals("ambient")?1:mode().equals("cinematic")?2:0);
        CheckBox motion=new CheckBox(activity);motion.setText("Reduce motion");motion.setChecked(reduceMotion());content.addView(motion);
        TextView hint=new TextView(activity);hint.setText("Static is recommended for this head unit. Reduce motion keeps all background effects still.");content.addView(hint);
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);
        new AlertDialog.Builder(activity).setTitle("Appearance").setView(scroll).setPositiveButton("Save",(d,w)->{
            preferences.edit().putString("appearance_theme",theme.getSelectedItemPosition()==0?"original":"synthwave")
                .putBoolean("appearance_glass",glass.isChecked())
                .putInt("appearance_brightness",brightness.getProgress()).putInt("appearance_transparency",transparency.getProgress())
                .putInt("appearance_neon",neon.getProgress()).putBoolean("style_neon",neon.getProgress()>0)
                .putString("appearance_animation",animation.getSelectedItemPosition()==1?"ambient":animation.getSelectedItemPosition()==2?"cinematic":"static")
                .putBoolean("appearance_reduce_motion",motion.isChecked()).apply();changed.run();
        }).setNeutralButton("Defaults",(d,w)->{
            SharedPreferences.Editor edit=preferences.edit();for(String key:new String[]{"theme","glass","brightness","transparency","neon","animation","reduce_motion"})edit.remove("appearance_"+key);
            edit.putBoolean("style_neon",true).apply();changed.run();
        }).setNegativeButton("Cancel",null).show();
    }
    private Spinner spinner(LinearLayout parent,String title,String[] labels,int selected){
        TextView label=new TextView(activity);label.setText(title);parent.addView(label);
        Spinner spinner=new Spinner(activity);ArrayAdapter<String> adapter=new ArrayAdapter<>(activity,android.R.layout.simple_spinner_item,labels);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(adapter);spinner.setSelection(selected);parent.addView(spinner);return spinner;
    }
    private SeekBar slider(LinearLayout parent,String title,int initial,int max){
        TextView label=new TextView(activity);label.setText(title+": "+initial+"%");parent.addView(label);SeekBar slider=new SeekBar(activity);slider.setMax(max);slider.setProgress(initial);parent.addView(slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int value,boolean user){label.setText(title+": "+value+"%");}});return slider;
    }
}
