package com.shazwan.sagadash;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

/** A cached wallpaper and optional low-rate light effects. It never decodes or owns a bitmap. */
final class DashSynthBackdrop extends Drawable {
    private static final long FRAME_MS=100; // Opt-in effects are capped at 10 frames/second.
    private final float unit;
    private final Paint base=new Paint(),imagePaint=new Paint(Paint.FILTER_BITMAP_FLAG),overlay=new Paint(),
            moon=new Paint(Paint.ANTI_ALIAS_FLAG),highway=new Paint(Paint.ANTI_ALIAS_FLAG),
            streak=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect source=new Rect();
    private final RectF destination=new RectF();
    private Bitmap image;
    private int brightness=65,intensity=65,opacity=255;
    private boolean reduceMotion=true,active,scheduled;
    private String mode="static";
    private float moonX,moonY,moonRadius,highwayX,highwayY,highwayRadius;
    private ColorFilter filter;
    private Callback scheduledCallback;
    private long animationStart;
    private final Runnable frame=new Runnable(){
        @Override public void run(){
            Callback previous=scheduledCallback;
            scheduled=false;scheduledCallback=null;
            if(!canAnimate()||getCallback()!=previous)return;
            invalidateSelf();
            updateScheduling();
        }
    };

    DashSynthBackdrop(float unit){
        this.unit=Math.max(.5f,unit);
        base.setColor(0xff050817);overlay.setColor(0xff000000);
        streak.setColor(0xff00f5ff);streak.setStrokeWidth(Math.max(1,this.unit));
        streak.setStrokeCap(Paint.Cap.ROUND);
        animationStart=SystemClock.uptimeMillis();applyOpacity();
    }

    void configure(int brightness,int neonIntensity,String mode,boolean reduceMotion){
        int nextBrightness=Math.max(0,Math.min(100,brightness));
        int nextIntensity=Math.max(0,Math.min(100,neonIntensity));
        String nextMode="ambient".equalsIgnoreCase(mode)?"ambient":
                "cinematic".equalsIgnoreCase(mode)?"cinematic":"static";
        boolean changed=this.brightness!=nextBrightness||intensity!=nextIntensity||
                !this.mode.equals(nextMode)||this.reduceMotion!=reduceMotion;
        this.brightness=nextBrightness;intensity=nextIntensity;this.mode=nextMode;
        this.reduceMotion=reduceMotion;applyOpacity();updateScheduling();
        if(changed)invalidateSelf();
    }

    void setImage(Bitmap image){
        if(this.image==image)return;
        this.image=image;rebuildGeometry();updateScheduling();invalidateSelf();
    }

    void setActive(boolean active){
        if(this.active!=active){this.active=active;if(active)animationStart=SystemClock.uptimeMillis();}
        updateScheduling();
    }

    /** True only while a real Drawable callback has a frame runnable queued. */
    boolean isAnimating(){return scheduled&&scheduledCallback==getCallback()&&canAnimate();}

    private boolean canAnimate(){
        return active&&isVisible()&&!reduceMotion&&intensity>0&&brightness>0&&opacity>0&&
                !"static".equals(mode)&&image!=null&&!image.isRecycled()&&!getBounds().isEmpty();
    }

    private void stopScheduling(){
        if(scheduledCallback!=null)scheduledCallback.unscheduleDrawable(this,frame);
        scheduledCallback=null;scheduled=false;
    }

    private void updateScheduling(){
        Callback callback=getCallback();
        if(!canAnimate()||callback==null){stopScheduling();return;}
        if(scheduled&&scheduledCallback==callback)return;
        stopScheduling();scheduledCallback=callback;scheduled=true;
        callback.scheduleDrawable(this,frame,SystemClock.uptimeMillis()+FRAME_MS);
    }

    @Override public boolean setVisible(boolean visible,boolean restart){
        boolean changed=super.setVisible(visible,restart);
        if(restart)animationStart=SystemClock.uptimeMillis();
        updateScheduling();return changed;
    }

    private void applyOpacity(){
        base.setAlpha(opacity);imagePaint.setAlpha(opacity);
        overlay.setAlpha(Math.round((100-brightness)*2.55f*opacity/255f));
    }

    @Override protected void onBoundsChange(Rect bounds){rebuildGeometry();updateScheduling();}

    private void rebuildGeometry(){
        Rect bounds=getBounds();destination.set(bounds);source.setEmpty();
        moon.setShader(null);highway.setShader(null);
        if(bounds.isEmpty())return;
        float width=bounds.width(),height=bounds.height();
        moonX=bounds.left+width*.52f;moonY=bounds.top+height*.26f;
        highwayX=bounds.left+width*.52f;highwayY=bounds.top+height*.87f;
        if(image!=null&&!image.isRecycled()){
            float scale=Math.max(width/image.getWidth(),height/image.getHeight());
            float cropWidth=width/scale,cropHeight=height/scale;
            float left=Math.max(0,Math.min(image.getWidth()-cropWidth,(image.getWidth()-cropWidth)*.5f));
            // Keep the shared moon/car focal region at 40% height when a vertical crop is needed.
            float top=Math.max(0,Math.min(image.getHeight()-cropHeight,image.getHeight()*.4f-cropHeight*.4f));
            source.set(Math.round(left),Math.round(top),Math.min(image.getWidth(),Math.round(left+cropWidth)),
                    Math.min(image.getHeight(),Math.round(top+cropHeight)));
            if(!source.isEmpty()){
                moonX=bounds.left+(image.getWidth()*.52f-source.left)*width/source.width();
                moonY=bounds.top+(image.getHeight()*.26f-source.top)*height/source.height();
                highwayX=bounds.left+(image.getWidth()*.52f-source.left)*width/source.width();
                highwayY=bounds.top+(image.getHeight()*.87f-source.top)*height/source.height();
            }
        }
        moonRadius=Math.min(width,height)*.34f;highwayRadius=width*.45f;
        moon.setShader(new RadialGradient(moonX,moonY,moonRadius,0xff00f5ff,0x0000f5ff,Shader.TileMode.CLAMP));
        highway.setShader(new RadialGradient(highwayX,highwayY,highwayRadius,0xff9d4edd,0x009d4edd,Shader.TileMode.CLAMP));
    }

    @Override public void draw(Canvas canvas){
        Rect bounds=getBounds();if(bounds.isEmpty())return;
        canvas.drawRect(bounds,base);
        if(image!=null&&!image.isRecycled()&&!source.isEmpty())canvas.drawBitmap(image,source,destination,imagePaint);
        canvas.drawRect(bounds,overlay);
        if(canAnimate()){
            long elapsed=SystemClock.uptimeMillis()-animationStart;
            float phase=(elapsed%12000L)/12000f;
            float pulse=.5f+.5f*(float)Math.sin(phase*Math.PI*2);
            float strength=intensity/100f*brightness/100f*opacity/255f;
            moon.setAlpha(Math.round((9+12*pulse)*strength));
            canvas.drawCircle(moonX,moonY,moonRadius,moon);
            if("cinematic".equals(mode)){
                highway.setAlpha(Math.round((8+7*pulse)*strength));
                canvas.drawRect(bounds,highway);
                streak.setAlpha(Math.round(34*strength));
                float horizon=bounds.top+bounds.height()*.56f;
                for(int i=0;i<6;i++){
                    float travel=(phase*2+i/6f)%1;
                    float depth=travel*travel;
                    float y=horizon+(bounds.bottom-horizon)*depth;
                    float side=(i&1)==0?-1:1;
                    float x=highwayX+side*bounds.width()*(.07f+depth*.40f);
                    float length=(5+22*depth)*unit;
                    canvas.drawLine(x,y,x+side*length,y+length*.55f,streak);
                }
            }
        }
        // setBackground attaches the callback after configure/setActive; the first draw arms effects.
        updateScheduling();
    }

    @Override public void setAlpha(int alpha){
        int next=Math.max(0,Math.min(255,alpha));if(next==opacity)return;
        opacity=next;applyOpacity();updateScheduling();invalidateSelf();
    }
    @Override public int getAlpha(){return opacity;}
    @Override public void setColorFilter(ColorFilter colorFilter){
        filter=colorFilter;base.setColorFilter(colorFilter);imagePaint.setColorFilter(colorFilter);
        overlay.setColorFilter(colorFilter);moon.setColorFilter(colorFilter);highway.setColorFilter(colorFilter);
        streak.setColorFilter(colorFilter);invalidateSelf();
    }
    @Override public int getOpacity(){return opacity==255&&filter==null?PixelFormat.OPAQUE:PixelFormat.TRANSLUCENT;}
}
