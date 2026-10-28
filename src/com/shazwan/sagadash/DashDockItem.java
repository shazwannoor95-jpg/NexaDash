package com.shazwan.sagadash;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** One native dock shortcut; adapts to a resized dock without an animation or worker. */
final class DashDockItem extends LinearLayout {
    final TextView icon,caption;
    private final float unit;
    private final int requestedSize,requestedText,iconColor;

    DashDockItem(Context context,float unitPx,float requestedScale,String name,String symbol,int color,Runnable action,boolean neon){
        super(context);unit=Math.max(.1f,unitPx);setOrientation(VERTICAL);setGravity(Gravity.CENTER);
        float scale=Float.isNaN(requestedScale)||Float.isInfinite(requestedScale)?1:Math.max(.7f,Math.min(1.3f,requestedScale));
        requestedSize=p(36*scale);requestedText=p(Math.round(21*scale));iconColor=color;
        setContentDescription(name);setOnClickListener(v->{if(action!=null)action.run();});
        icon=label(symbol,requestedText);GradientDrawable background=new GradientDrawable();background.setColor(color);
        background.setCornerRadius(p(13));background.setStroke(p(1),neon?0x8061ccff:DashboardActivity.LINE);icon.setBackground(background);
        addView(icon,new LayoutParams(requestedSize,requestedSize));
        caption=label(name,p(13));caption.setMaxLines(1);caption.setEllipsize(TextUtils.TruncateAt.END);
        addView(caption,new LayoutParams(-1,p(20)));
    }

    /** Changes only drawables; shortcut dimensions, ordering and actions remain intact. */
    void style(boolean synth,int panelAlpha,float intensity,boolean neon,boolean reduceMotion){
        float strength=Float.isNaN(intensity)||Float.isInfinite(intensity)?0:Math.max(0,Math.min(1,intensity));
        int alpha=Math.max(0,Math.min(255,panelAlpha));
        GradientDrawable badge=new GradientDrawable();badge.setColor(synth?(0xd9000000|(iconColor&0xffffff)):iconColor);
        badge.setCornerRadius(p(13));badge.setStroke(p(1),neon?(synth?((Math.round(150*strength)<<24)|0x005ceaff):0x8061ccff):DashboardActivity.LINE);icon.setBackground(badge);
        DockSurface normal=new DockSurface(synth,alpha,strength,neon,0);
        if(reduceMotion){
            StateListDrawable states=new StateListDrawable();
            states.addState(new int[]{android.R.attr.state_pressed},new DockSurface(synth,alpha,strength,neon,1));
            states.addState(new int[]{android.R.attr.state_focused},new DockSurface(synth,alpha,strength,neon,1));
            states.addState(new int[]{},normal);setBackground(states);
        }else{
            GradientDrawable mask=new GradientDrawable();mask.setColor(0xffffffff);mask.setCornerRadius(p(14));
            int ripple=(Math.round(38+26*strength)<<24)|0x005ceaff;
            setBackground(new RippleDrawable(ColorStateList.valueOf(ripple),normal,mask));
        }
    }

    /** Static glass and selected-session marker; state changes are driven by Android. */
    private final class DockSurface extends Drawable {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF area=new RectF();
        final boolean synth,neon;
        final int alpha,highlight;
        final float strength;
        boolean selected;
        int opacity=255;
        DockSurface(boolean synth,int alpha,float strength,boolean neon,int highlight){this.synth=synth;this.alpha=alpha;this.strength=strength;this.neon=neon;this.highlight=highlight;paint.setStrokeCap(Paint.Cap.ROUND);}
        @Override public boolean isStateful(){return true;}
        @Override protected boolean onStateChange(int[] states){
            boolean next=false;for(int state:states)if(state==android.R.attr.state_selected){next=true;break;}
            if(selected==next)return false;selected=next;invalidateSelf();return true;
        }
        @Override public void draw(Canvas canvas){
            area.set(getBounds());area.inset(unit,unit);if(area.width()<=0||area.height()<=0)return;
            paint.setStyle(Paint.Style.FILL);paint.setColor(0x00141a35);
            paint.setAlpha(Math.round((synth?alpha*.28f:0)+highlight*32)*opacity/255);canvas.drawRoundRect(area,p(14),p(14),paint);
            if(synth||highlight>0){
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,unit));paint.setColor(0xff5ceaff);
                paint.setAlpha(Math.round((neon?(24+highlight*70)*strength:highlight*48)*opacity/255));canvas.drawRoundRect(area,p(14),p(14),paint);
            }
            if(selected){
                float y=area.bottom-Math.max(1,2*unit),left=area.left+area.width()*.2f,middle=area.centerX(),right=area.right-area.width()*.2f;
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,2*unit));
                paint.setColor(0xff5ceaff);paint.setAlpha(opacity);canvas.drawLine(left,y,middle,y,paint);
                paint.setColor(synth?0xffff63d8:0xff5ceaff);paint.setAlpha(opacity);canvas.drawLine(middle,y,right,y,paint);
            }
        }
        @Override public void setAlpha(int value){int next=Math.max(0,Math.min(255,value));if(opacity!=next){opacity=next;invalidateSelf();}}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }

    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getMode(widthSpec)==MeasureSpec.UNSPECIFIED?requestedSize:Math.max(0,MeasureSpec.getSize(widthSpec)-getPaddingLeft()-getPaddingRight());
        int height=MeasureSpec.getMode(heightSpec)==MeasureSpec.UNSPECIFIED?requestedSize+p(20):Math.max(0,MeasureSpec.getSize(heightSpec)-getPaddingTop()-getPaddingBottom());
        int captionHeight=Math.min(p(20),height),size=Math.min(requestedSize,Math.min(width,Math.max(0,height-captionHeight)));
        LayoutParams iconBounds=(LayoutParams)icon.getLayoutParams();iconBounds.width=size;iconBounds.height=size;
        ((LayoutParams)caption.getLayoutParams()).height=captionHeight;
        int font=Math.max(1,Math.round(requestedText*(float)size/requestedSize));
        if(Math.abs(icon.getTextSize()-font)>.1f)icon.setTextSize(TypedValue.COMPLEX_UNIT_PX,font);
        int captionFont=Math.max(1,Math.min(p(13),Math.round(captionHeight*.65f)));
        if(Math.abs(caption.getTextSize()-captionFont)>.1f)caption.setTextSize(TypedValue.COMPLEX_UNIT_PX,captionFont);
        super.onMeasure(widthSpec,heightSpec);
    }

    private int p(float value){return Math.max(1,Math.round(value*unit));}
    private TextView label(String value,int size){
        TextView text=new TextView(getContext());text.setText(value);text.setTextColor(DashboardActivity.TEXT);
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX,size);text.setGravity(Gravity.CENTER);text.setFontFeatureSettings("tnum");return text;
    }
}
