package com.shazwan.sagadash;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
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
    private final float radius,unit;
    private final boolean neon;
    private final int fillAlpha;
    private int opacity=255;

    DashPanelDrawable(int color,int accent,float radius){this(color,accent,radius,true);}
    DashPanelDrawable(int color,int accent,float radius,boolean neon){
        this.radius=Math.max(0,radius);unit=Math.max(.65f,this.radius/20f);this.neon=neon;
        fill.setColor(color);fillAlpha=(color>>>24)&255;
        edge.setColor(neon?accent:0xff243c51);edge.setStyle(Paint.Style.STROKE);edge.setStrokeWidth(Math.max(1,unit));
        halo.setColor(accent);halo.setStyle(Paint.Style.STROKE);halo.setStrokeWidth(4*unit);
        cyan.setColor(accent);cyan.setStyle(Paint.Style.STROKE);cyan.setStrokeWidth(2*unit);cyan.setStrokeCap(Paint.Cap.ROUND);
        violet.setColor(0xff9275ef);violet.setStyle(Paint.Style.STROKE);violet.setStrokeWidth(2*unit);violet.setStrokeCap(Paint.Cap.ROUND);
        applyOpacity();
    }
    private void applyOpacity(){
        fill.setAlpha(fillAlpha*opacity/255);edge.setAlpha((neon?104:255)*opacity/255);
        halo.setAlpha(15*opacity/255);cyan.setAlpha(174*opacity/255);violet.setAlpha(128*opacity/255);
    }
    @Override protected void onBoundsChange(Rect bounds){
        float inset=neon?2*unit:Math.max(.5f,unit/2);
        area.set(bounds.left+inset,bounds.top+inset,bounds.right-inset,bounds.bottom-inset);
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
    }
    // Rounded corners leave part of the drawable's bounds transparent.
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
