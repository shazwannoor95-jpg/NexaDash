package com.shazwan.sagadash;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/** Eight rendered image angles. This viewer does not load or render a live 3D model. */
final class SagaAngleView extends View {
    static final String FRAME_KEY="vehicle_angle_frame";
    static final int ANGLE_COUNT=8, DEFAULT_FRAME=7;
    private static final float ATLAS_WIDTH=1774f, ATLAS_HEIGHT=887f;
    private static final float REFERENCE_WIDTH=480f, REFERENCE_HEIGHT=310f;
    private static final int[][] BOUNDS={
        {58,127,385,396}, {437,129,860,398}, {885,171,1337,362}, {1360,136,1752,394},
        {59,533,376,810}, {425,537,857,800}, {876,583,1331,773}, {1337,540,1759,804}
    };
    private static final int CROP_PADDING=3;
    private final ExecutorService executor;
    private final SharedPreferences preferences;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Paint imagePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint messagePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect source=new Rect();
    private final RectF destination=new RectF();
    private final GestureDetector taps;
    private final ScaleGestureDetector scales;
    private final int touchSlop;
    private Future<?> loading;
    private long generation;
    private boolean loadFailed,dragged;
    private int selected,downFrame;
    private float downX,downY,zoom=1;
    // Package-visible lifecycle state for integration checks. Exactly one decoded atlas is retained.
    volatile Bitmap atlas;
    volatile boolean active;

    SagaAngleView(Context context,ExecutorService executor,SharedPreferences preferences){
        super(context);
        this.executor=executor;this.preferences=preferences;
        selected=wrap(preferences.getInt(FRAME_KEY,DEFAULT_FRAME));
        touchSlop=ViewConfiguration.get(context).getScaledTouchSlop();
        messagePaint.setColor(0xff8ea8be);messagePaint.setTextAlign(Paint.Align.CENTER);
        messagePaint.setTextSize(13*getResources().getDisplayMetrics().scaledDensity);
        setClickable(true);setFocusable(true);describe();
        taps=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent event){return true;}
            @Override public boolean onSingleTapUp(MotionEvent event){return performClick();}
            @Override public boolean onDoubleTap(MotionEvent event){reset();return true;}
        });
        taps.setIsLongpressEnabled(false);
        scales=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScale(ScaleGestureDetector detector){
                float next=Math.max(.8f,Math.min(1.25f,zoom*detector.getScaleFactor()));
                if(Math.abs(next-zoom)>.0001f){zoom=next;invalidate();}
                dragged=true;return true;
            }
        });
        scales.setQuickScaleEnabled(false);
    }

    static int wrap(int frame){return ((frame%ANGLE_COUNT)+ANGLE_COUNT)%ANGLE_COUNT;}
    int frame(){return selected;}
    void setFrame(int frame){
        int next=wrap(frame);
        if(next==selected)return;
        selected=next;describe();invalidate();
    }
    void reset(){
        boolean changed=selected!=DEFAULT_FRAME||zoom!=1;
        selected=DEFAULT_FRAME;zoom=1;describe();persist();if(changed)invalidate();
    }
    private void persist(){preferences.edit().putInt(FRAME_KEY,selected).apply();}
    private void describe(){
        setContentDescription("Red Saga NEXA, rendered image angle "+(selected+1)+" of 8. Drag to change image angle, pinch to zoom, double tap to reset. These are eight images, not a live 3D model.");
    }

    void resume(){
        if(active&&(atlas!=null||loading!=null||loadFailed))return;
        active=true;loadFailed=false;
        final long epoch=++generation;
        try{
            loading=executor.submit(()->{
                Bitmap decoded=null;
                try{
                    BitmapFactory.Options options=new BitmapFactory.Options();
                    options.inScaled=false;options.inSampleSize=2;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
                    decoded=BitmapFactory.decodeResource(getResources(),R.drawable.saga_angles,options);
                    if(decoded!=null&&decoded.getAllocationByteCount()>2*1024*1024){decoded.recycle();decoded=null;}
                }catch(RuntimeException ignored){ }
                final Bitmap result=decoded;
                main.post(()->{
                    if(epoch!=generation||!active){if(result!=null&&!result.isRecycled())result.recycle();return;}
                    loading=null;atlas=result;loadFailed=result==null;invalidate();
                });
            });
        }catch(RejectedExecutionException ignored){loading=null;loadFailed=true;invalidate();}
    }
    void pause(){
        if(!active&&atlas==null&&loading==null)return;
        active=false;++generation;
        if(loading!=null){loading.cancel(false);loading=null;}
        // A hardware display list may still reference the old bitmap until the next frame;
        // dropping our reference is safer than recycling a bitmap already displayed by Canvas.
        atlas=null;loadFailed=false;invalidate();
    }
    @Override protected void onDetachedFromWindow(){pause();super.onDetachedFromWindow();}

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        Bitmap image=atlas;
        if(!active||image==null||image.isRecycled()){
            if(active)canvas.drawText(loadFailed?"Vehicle angles unavailable":"Loading vehicle angles…",getWidth()/2f,getHeight()/2f,messagePaint);
            return;
        }
        int[] bounds=BOUNDS[selected];
        float left=Math.max(0,bounds[0]-CROP_PADDING),top=Math.max(0,bounds[1]-CROP_PADDING);
        float right=Math.min(ATLAS_WIDTH,bounds[2]+CROP_PADDING),bottom=Math.min(ATLAS_HEIGHT,bounds[3]+CROP_PADDING);
        source.set(Math.max(0,(int)Math.floor(left*image.getWidth()/ATLAS_WIDTH)),
            Math.max(0,(int)Math.floor(top*image.getHeight()/ATLAS_HEIGHT)),
            Math.min(image.getWidth(),(int)Math.ceil(right*image.getWidth()/ATLAS_WIDTH)),
            Math.min(image.getHeight(),(int)Math.ceil(bottom*image.getHeight()/ATLAS_HEIGHT)));
        float availableWidth=Math.max(1,getWidth()-getPaddingLeft()-getPaddingRight());
        float availableHeight=Math.max(1,getHeight()-getPaddingTop()-getPaddingBottom());
        float scale=Math.min(availableWidth/REFERENCE_WIDTH,availableHeight/REFERENCE_HEIGHT)*zoom;
        float width=(right-left)*scale,height=(bottom-top)*scale;
        float cx=getPaddingLeft()+availableWidth/2,cy=getPaddingTop()+availableHeight/2;
        destination.set(cx-width/2,cy-height/2,cx+width/2,cy+height/2);
        canvas.drawBitmap(image,source,destination,imagePaint);
    }

    @Override public boolean onTouchEvent(MotionEvent event){
        if(!isEnabled())return false;
        scales.onTouchEvent(event);taps.onTouchEvent(event);
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                downX=event.getX();downY=event.getY();downFrame=selected;dragged=false;
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                if(event.getPointerCount()==1&&!scales.isInProgress()){
                    float dx=event.getX()-downX,dy=event.getY()-downY;
                    if(Math.abs(dx)>touchSlop||Math.abs(dy)>touchSlop)dragged=true;
                    if(dragged){float step=Math.max(1,getWidth()/(float)ANGLE_COUNT);setFrame(downFrame-Math.round(dx/step));}
                }else dragged=true;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int remaining=event.getActionIndex()==0?1:0;
                downX=event.getX(remaining);downY=event.getY(remaining);downFrame=selected;dragged=true;
                break;
            case MotionEvent.ACTION_UP:
                persist();
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
                break;
            case MotionEvent.ACTION_CANCEL:
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){
        super.onInitializeAccessibilityNodeInfo(info);info.setScrollable(true);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }
    @Override public boolean performAccessibilityAction(int action,Bundle arguments){
        if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD||action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){setFrame(selected+(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD?1:-1));persist();return true;}
        return super.performAccessibilityAction(action,arguments);
    }
}
