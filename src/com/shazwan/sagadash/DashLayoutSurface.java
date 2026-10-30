package com.shazwan.sagadash;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Native panel positioning. No animation, polling or preference writes while dragging. */
final class DashLayoutSurface extends ViewGroup {
    private static final String PREFIX="panel_layout_v1_", Z_ORDER=PREFIX+"order";
    private final SharedPreferences preferences;
    private final float unit;
    private final List<Panel> panels=new ArrayList<>();
    private final Map<String,RectF> beforeEdit=new HashMap<>();
    private final List<String> beforeOrder=new ArrayList<>();
    private final Paint outline=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF scratch=new RectF();
    private boolean editing,resizing,dragging;
    private Panel selected;
    private float downX,downY;
    private RectF downBounds,downCustom;
    private int pointer=-1;

    private static final class Panel {
        final String key;
        final View view;
        RectF custom;
        Panel(String key,View view,RectF custom){this.key=key;this.view=view;this.custom=custom;}
    }

    /** unitPx is the activity's scaled pixel size for one of its 1280x720 design units. */
    DashLayoutSurface(Context context,SharedPreferences preferences,float unitPx){
        super(context);this.preferences=preferences;unit=Math.max(.25f,unitPx);
        setWillNotDraw(false);setClipChildren(true);setClipToPadding(true);
        outline.setStyle(Paint.Style.STROKE);outline.setStrokeWidth(2*unit);
        label.setTextSize(13*unit);label.setColor(0xffd6f4ff);
    }

    void addPanel(String key,View child){
        if(find(key)!=null)throw new IllegalArgumentException("Duplicate panel key: "+key);
        panels.add(new Panel(key,child,decode(saved(PREFIX+key,null))));
        addView(child,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        restoreOrder(saved(Z_ORDER,""));
    }

    boolean isEditing(){return editing;}

    /** Leaving edit mode commits; cancelEditing restores the state at entry. */
    void setEditing(boolean edit){
        if(editing==edit)return;
        stopGesture(false);
        if(edit){
            beforeEdit.clear();beforeOrder.clear();
            for(Panel panel:panels)beforeEdit.put(panel.key,copy(panel.custom));
            beforeOrder.addAll(order());
        }else save();
        editing=edit;selected=null;
        setContentDescription(edit?"Edit layout. Drag panels to move. Drag a bottom right corner to resize.":null);
        invalidate();
    }

    void cancelEditing(){
        if(!editing)return;
        stopGesture(false);
        for(Panel panel:panels)panel.custom=copy(beforeEdit.get(panel.key));
        restoreOrder(join(beforeOrder));
        editing=false;selected=null;beforeEdit.clear();beforeOrder.clear();
        setContentDescription(null);requestLayout();invalidate();
    }

    void resetLayout(){
        stopGesture(false);
        for(Panel panel:panels)panel.custom=null;
        for(Panel panel:panels)bringChildToFront(panel.view);
        selected=null;
        if(!editing)save();
        requestLayout();invalidate();
    }

    void save(){
        SharedPreferences.Editor edit=preferences.edit();
        for(Panel panel:panels){
            if(panel.custom==null)edit.remove(PREFIX+panel.key);
            else edit.putString(PREFIX+panel.key,encode(panel.custom));
        }
        edit.putString(Z_ORDER,join(order())).apply();
    }

    RectF boundsFor(String key){
        Panel panel=find(key);if(panel==null)return null;
        return resolve(panel,getWidth()>0?getWidth():getMeasuredWidth(),getHeight()>0?getHeight():getMeasuredHeight());
    }

    private Panel find(String key){for(Panel p:panels)if(p.key.equals(key))return p;return null;}
    private Panel find(View view){for(Panel p:panels)if(p.view==view)return p;return null;}
    private static RectF copy(RectF source){return source==null?null:new RectF(source);}
    private static boolean finite(float value){return !Float.isNaN(value)&&!Float.isInfinite(value);}
    private String saved(String key,String fallback){try{return preferences.getString(key,fallback);}catch(ClassCastException ignored){return fallback;}}

    static RectF decode(String saved){
        if(saved==null)return null;
        String[] parts=saved.split(",");if(parts.length!=4)return null;
        try{
            float x=Float.parseFloat(parts[0]),y=Float.parseFloat(parts[1]);
            float w=Float.parseFloat(parts[2]),h=Float.parseFloat(parts[3]);
            if(!finite(x)||!finite(y)||!finite(w)||!finite(h)||!finite(x+w)||!finite(y+h)||w<=0||h<=0)return null;
            return new RectF(x,y,x+w,y+h);
        }catch(NumberFormatException ignored){return null;}
    }
    static String encode(RectF bounds){return bounds.left+","+bounds.top+","+bounds.width()+","+bounds.height();}

    /** Pixel-space clamp shared by dragging, resizing and restored normalized positions. */
    static RectF bounded(RectF proposed,float width,float height,float minWidth,float minHeight){
        width=Math.max(0,width);height=Math.max(0,height);
        float w=Math.max(Math.min(minWidth,width),Math.min(proposed.width(),width));
        float h=Math.max(Math.min(minHeight,height),Math.min(proposed.height(),height));
        float x=Math.max(0,Math.min(proposed.left,width-w));
        float y=Math.max(0,Math.min(proposed.top,height-h));
        return new RectF(x,y,x+w,y+h);
    }

    private RectF defaults(String key,int width,int height){
        float gap=10*unit,dock=Math.min(height,54*unit),content=height-dock;
        float left=Math.max(0,(width-gap)*.5f),right=Math.min(width,left+gap);
        float shortcut=Math.min(content,115*unit),margin=Math.min(content/2,6*unit);
        if(key.equals("vehicle"))return new RectF(0,margin,left,Math.max(margin,content-margin));
        if(key.equals("shortcuts"))return new RectF(right,0,width,shortcut);
        if(key.equals("media"))return new RectF(right,margin,width,Math.max(margin,content-margin));
        if(key.equals("dock"))return new RectF(0,content,width,height);
        return new RectF(0,0,width,height);
    }
    private float minimumWidth(Panel panel){return (panel.key.equals("dock")||panel.key.equals("shortcuts")?280:panel.key.equals("media")?260:240)*unit;}
    private float minimumHeight(Panel panel){return (panel.key.equals("dock")?48:panel.key.equals("shortcuts")?80:panel.key.equals("media")?200:200)*unit;}
    private RectF resolve(Panel panel,int width,int height){
        if(panel.custom==null)return defaults(panel.key,width,height);
        RectF rect=new RectF(panel.custom.left*width,panel.custom.top*height,panel.custom.right*width,panel.custom.bottom*height);
        return bounded(rect,width,height,minimumWidth(panel),minimumHeight(panel));
    }
    private void position(Panel panel,RectF pixels){
        if(getWidth()==0||getHeight()==0)return;
        RectF rect=bounded(pixels,getWidth(),getHeight(),minimumWidth(panel),minimumHeight(panel));
        panel.custom=new RectF(rect.left/getWidth(),rect.top/getHeight(),rect.right/getWidth(),rect.bottom/getHeight());
        requestLayout();invalidate();
    }

    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec),height=MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(resolveSize(width,widthSpec),resolveSize(height,heightSpec));
        for(Panel panel:panels){
            if(panel.view.getVisibility()==GONE)continue;
            RectF rect=resolve(panel,width,height);
            panel.view.measure(MeasureSpec.makeMeasureSpec(Math.max(0,Math.round(rect.width())),MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(Math.max(0,Math.round(rect.height())),MeasureSpec.EXACTLY));
        }
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
        for(Panel panel:panels){
            if(panel.view.getVisibility()==GONE)continue;
            RectF rect=resolve(panel,right-left,bottom-top);
            int x=Math.round(rect.left),y=Math.round(rect.top);
            panel.view.layout(x,y,x+panel.view.getMeasuredWidth(),y+panel.view.getMeasuredHeight());
        }
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event){return editing;}
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!editing)return false;
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            downX=event.getX();downY=event.getY();pointer=event.getPointerId(0);selected=null;
            for(int i=getChildCount()-1;i>=0;i--){
                Panel panel=find(getChildAt(i));
                if(panel!=null&&panel.view.getVisibility()==VISIBLE&&resolve(panel,getWidth(),getHeight()).contains(downX,downY)){selected=panel;break;}
            }
            if(selected!=null){
                downBounds=resolve(selected,getWidth(),getHeight());downCustom=copy(selected.custom);
                resizing=downX>=downBounds.right-34*unit&&downY>=downBounds.bottom-34*unit;
                dragging=true;bringChildToFront(selected.view);getParentDisallow(true);
            }
            invalidate();return true;
        }
        if(action==MotionEvent.ACTION_MOVE&&dragging&&selected!=null){
            int index=event.findPointerIndex(pointer);if(index<0){stopGesture(true);return true;}
            float dx=event.getX(index)-downX,dy=event.getY(index)-downY;
            RectF moved=new RectF(downBounds);
            if(resizing){moved.right+=dx;moved.bottom+=dy;}else moved.offset(dx,dy);
            position(selected,moved);return true;
        }
        if(action==MotionEvent.ACTION_CANCEL){stopGesture(true);return true;}
        if(action==MotionEvent.ACTION_UP||(action==MotionEvent.ACTION_POINTER_UP&&event.getPointerId(event.getActionIndex())==pointer)){
            stopGesture(false);return true;
        }
        return true;
    }
    private void getParentDisallow(boolean disallow){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(disallow);}
    private void stopGesture(boolean cancel){
        if(cancel&&dragging&&selected!=null){selected.custom=copy(downCustom);requestLayout();invalidate();}
        dragging=false;resizing=false;pointer=-1;getParentDisallow(false);
    }

    @Override protected void dispatchDraw(Canvas canvas){
        super.dispatchDraw(canvas);if(!editing)return;
        for(int i=0;i<getChildCount();i++){
            Panel panel=find(getChildAt(i));if(panel==null||panel.view.getVisibility()!=VISIBLE)continue;
            scratch.set(resolve(panel,getWidth(),getHeight()));scratch.inset(unit,unit);
            outline.setColor(panel==selected?0xff65f5e7:0xff43a7e0);
            canvas.drawRoundRect(scratch,12*unit,12*unit,outline);
            float inset=8*unit;
            canvas.drawText(panel.key.toUpperCase(java.util.Locale.US),scratch.left+inset,scratch.top+19*unit,label);
            float x=scratch.right-5*unit,y=scratch.bottom-5*unit;
            canvas.drawLine(x-22*unit,y,x,y-22*unit,outline);
            canvas.drawLine(x-12*unit,y,x,y-12*unit,outline);
        }
    }
    private List<String> order(){
        List<String> keys=new ArrayList<>();
        for(int i=0;i<getChildCount();i++){Panel panel=find(getChildAt(i));if(panel!=null)keys.add(panel.key);}
        return keys;
    }
    private void restoreOrder(String encoded){
        for(String key:encoded.split("\\|")){Panel panel=find(key);if(panel!=null)bringChildToFront(panel.view);}
    }
    private static String join(List<String> values){
        StringBuilder out=new StringBuilder();for(String value:values){if(out.length()>0)out.append('|');out.append(value);}return out.toString();
    }
}
