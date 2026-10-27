package com.shazwan.sagadash;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** A neon impression made with a few static strokes; no blur, bitmap or animation. */
final class DashPanelDrawable extends Drawable {
    private final Paint fill=new Paint(Paint.ANTI_ALIAS_FLAG),edge=new Paint(Paint.ANTI_ALIAS_FLAG),
            halo=new Paint(Paint.ANTI_ALIAS_FLAG),cyan=new Paint(Paint.ANTI_ALIAS_FLAG),violet=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF area=new RectF();
    private final Path upperAccent=new Path(),lowerAccent=new Path();
    private final Paint reflection=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean glass;
    private final int baseColor;
    private final float radius,unit,strength;
    private final boolean neon;
    private final int fillAlpha;
    private int opacity=255;

    DashPanelDrawable(int color,int accent,float radius){this(color,accent,radius,true);}
    DashPanelDrawable(int color,int accent,float radius,boolean neon){this(color,accent,radius,neon,1);}
    DashPanelDrawable(int color,int accent,float radius,boolean neon,float strength){
        this(color,accent,radius,neon,strength,false);
    }
    DashPanelDrawable(int color,int accent,float radius,boolean neon,float strength,boolean glass){
        this.glass=glass;baseColor=color;
        this.radius=Math.max(0,radius);unit=Math.max(.65f,this.radius/20f);
        this.strength=Math.max(0,Math.min(1,strength));this.neon=neon&&this.strength>0;
        fill.setColor(color);fillAlpha=(color>>>24)&255;
        edge.setColor(this.neon?accent:0xff243c51);edge.setStyle(Paint.Style.STROKE);edge.setStrokeWidth(Math.max(1,unit));
        halo.setColor(accent);halo.setStyle(Paint.Style.STROKE);halo.setStrokeWidth(4*unit);
        cyan.setColor(accent);cyan.setStyle(Paint.Style.STROKE);cyan.setStrokeWidth(2*unit);cyan.setStrokeCap(Paint.Cap.ROUND);
        violet.setColor(0xff9d4edd);violet.setStyle(Paint.Style.STROKE);violet.setStrokeWidth(2*unit);violet.setStrokeCap(Paint.Cap.ROUND);
        applyOpacity();
    }
    private void applyOpacity(){
        fill.setAlpha(fillAlpha*opacity/255);edge.setAlpha(Math.round((neon?55+115*strength:255)*opacity/255f));
        halo.setAlpha(Math.round(20*strength*opacity/255f));cyan.setAlpha(Math.round(210*strength*opacity/255f));
        violet.setAlpha(Math.round(160*strength*opacity/255f));
        reflection.setAlpha(Math.round(100*opacity/255f));
    }
    @Override protected void onBoundsChange(Rect bounds){
        float inset=neon?2*unit:Math.max(.5f,unit/2);
        area.set(bounds.left+inset,bounds.top+inset,bounds.right-inset,bounds.bottom-inset);
        if(glass&&area.height()>0){
            fill.setShader(new LinearGradient(0,area.top,0,area.bottom,
                new int[]{tint(baseColor,0xffd2f3ff,.20f),tint(baseColor,0xff8fc8df,.035f),tint(baseColor,0xff000716,.15f)},
                new float[]{0,.46f,1},Shader.TileMode.CLAMP));
            reflection.setStyle(Paint.Style.STROKE);reflection.setStrokeWidth(Math.max(1,unit));
            reflection.setShader(new LinearGradient(area.left,area.top,area.right,area.bottom,
                new int[]{0xffe1f8ff,0x2587cce2,0x0087cce2,0x5587cce2},new float[]{0,.35f,.7f,1},Shader.TileMode.CLAMP));
        }
        upperAccent.reset();lowerAccent.reset();
        if(!neon||area.width()<=0||area.height()<=0)return;
        float r=Math.min(radius,Math.min(area.width(),area.height())/2);
        float segment=Math.min(48*unit,Math.min(area.width(),area.height())*.25f);
        upperAccent.moveTo(area.left,area.top+r+segment*.4f);
        upperAccent.lineTo(area.left,area.top+r);
        upperAccent.quadTo(area.left,area.top,area.left+r,area.top);
        upperAccent.lineTo(area.left+r+segment,area.top);
        lowerAccent.moveTo(area.right,area.bottom-r-segment*.4f);
        lowerAccent.lineTo(area.right,area.bottom-r);
        lowerAccent.quadTo(area.right,area.bottom,area.right-r,area.bottom);
        lowerAccent.lineTo(area.right-r-segment,area.bottom);
    }
    @Override public void draw(Canvas canvas){
        if(area.width()<=0||area.height()<=0)return;
        canvas.drawRoundRect(area,radius,radius,fill);
        if(neon)canvas.drawRoundRect(area,radius,radius,halo);
        canvas.drawRoundRect(area,radius,radius,edge);
        if(glass)canvas.drawRoundRect(area,radius,radius,reflection);
        if(neon){canvas.drawPath(upperAccent,cyan);canvas.drawPath(lowerAccent,violet);}
    }
    @Override public void setAlpha(int alpha){
        int next=Math.max(0,Math.min(255,alpha));if(next==opacity)return;
        opacity=next;applyOpacity();invalidateSelf();
    }
    @Override public int getAlpha(){return opacity;}
    @Override public void setColorFilter(ColorFilter colorFilter){
        fill.setColorFilter(colorFilter);edge.setColorFilter(colorFilter);halo.setColorFilter(colorFilter);
        cyan.setColorFilter(colorFilter);violet.setColorFilter(colorFilter);invalidateSelf();
        reflection.setColorFilter(colorFilter);
    }
    // Rounded corners leave part of the drawable's bounds transparent.
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    static int tint(int color,int target,float weight){
        int r=Math.round(((color>>16)&255)*(1-weight)+((target>>16)&255)*weight);
        int g=Math.round(((color>>8)&255)*(1-weight)+((target>>8)&255)*weight);
        int b=Math.round((color&255)*(1-weight)+(target&255)*weight);
        return 0xff000000|(r<<16)|(g<<8)|b;
    }
}
