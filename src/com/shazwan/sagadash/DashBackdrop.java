package com.shazwan.sagadash;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Static dashboard decoration. Paths/shader change only when its bounds change. */
final class DashBackdrop extends Drawable {
    private final boolean neon;
    private final float unit;
    private final Paint fill=new Paint(),grid=new Paint(),trace=new Paint(Paint.ANTI_ALIAS_FLAG),node=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path gridPath=new Path(),tracePath=new Path();
    private final float[] nodes=new float[12];
    private int opacity=255;
    private ColorFilter filter;

    DashBackdrop(){this(1,true);}
    DashBackdrop(float unit,boolean neon){
        this.unit=Math.max(.5f,unit);this.neon=neon;
        fill.setColor(0xff07131f);
        grid.setColor(0xff47b6ff);grid.setStyle(Paint.Style.STROKE);grid.setStrokeWidth(Math.max(1,this.unit));
        trace.setColor(0xff5bbdf2);trace.setStyle(Paint.Style.STROKE);trace.setStrokeWidth(Math.max(1,this.unit));
        trace.setStrokeJoin(Paint.Join.ROUND);trace.setStrokeCap(Paint.Cap.ROUND);
        node.setColor(0xff8acfff);applyOpacity();
    }
    private void applyOpacity(){
        fill.setAlpha(opacity);grid.setAlpha(10*opacity/255);trace.setAlpha(25*opacity/255);node.setAlpha(46*opacity/255);
    }
    @Override protected void onBoundsChange(Rect bounds){
        gridPath.reset();tracePath.reset();
        if(!neon||bounds.isEmpty()){fill.setShader(null);return;}
        fill.setShader(new LinearGradient(bounds.left,bounds.top,bounds.right,bounds.bottom,
                0xff0b1c2d,0xff06101b,Shader.TileMode.CLAMP));
        float spacing=Math.max(56*unit,Math.min(bounds.width(),bounds.height())/7f);
        for(float x=bounds.left+spacing/2;x<bounds.right;x+=spacing){gridPath.moveTo(x,bounds.top);gridPath.lineTo(x,bounds.bottom);}
        for(float y=bounds.top+spacing/2;y<bounds.bottom;y+=spacing){gridPath.moveTo(bounds.left,y);gridPath.lineTo(bounds.right,y);}
        float w=bounds.width(),h=bounds.height(),l=bounds.left,t=bounds.top;
        tracePath.moveTo(l+w*.64f,t+h*.07f);tracePath.lineTo(l+w*.79f,t+h*.07f);
        tracePath.lineTo(l+w*.82f,t+h*.12f);tracePath.lineTo(l+w*.96f,t+h*.12f);
        tracePath.moveTo(l+w*.69f,t+h*.10f);tracePath.lineTo(l+w*.76f,t+h*.10f);
        tracePath.lineTo(l+w*.79f,t+h*.15f);tracePath.lineTo(l+w*.94f,t+h*.15f);
        tracePath.moveTo(l+w*.02f,t+h*.85f);tracePath.lineTo(l+w*.13f,t+h*.85f);
        tracePath.lineTo(l+w*.17f,t+h*.92f);tracePath.lineTo(l+w*.31f,t+h*.92f);
        nodes[0]=l+w*.64f;nodes[1]=t+h*.07f;nodes[2]=l+w*.96f;nodes[3]=t+h*.12f;
        nodes[4]=l+w*.69f;nodes[5]=t+h*.10f;nodes[6]=l+w*.94f;nodes[7]=t+h*.15f;
        nodes[8]=l+w*.02f;nodes[9]=t+h*.85f;nodes[10]=l+w*.31f;nodes[11]=t+h*.92f;
    }
    @Override public void draw(Canvas canvas){
        canvas.drawRect(getBounds(),fill);
        if(neon){
            canvas.drawPath(gridPath,grid);canvas.drawPath(tracePath,trace);
            for(int i=0;i<nodes.length;i+=2)canvas.drawCircle(nodes[i],nodes[i+1],1.8f*unit,node);
        }
    }
    @Override public void setAlpha(int alpha){
        int next=Math.max(0,Math.min(255,alpha));if(next==opacity)return;
        opacity=next;applyOpacity();invalidateSelf();
    }
    @Override public int getAlpha(){return opacity;}
    @Override public void setColorFilter(ColorFilter colorFilter){
        filter=colorFilter;fill.setColorFilter(colorFilter);grid.setColorFilter(colorFilter);
        trace.setColorFilter(colorFilter);node.setColorFilter(colorFilter);invalidateSelf();
    }
    @Override public int getOpacity(){return opacity==255&&filter==null?PixelFormat.OPAQUE:PixelFormat.TRANSLUCENT;}
}
