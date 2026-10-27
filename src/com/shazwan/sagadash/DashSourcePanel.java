package com.shazwan.sagadash;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Controls for an installed source app; it never embeds or starts a hidden player. */
final class DashSourcePanel extends LinearLayout {
    final TextView title,artist,play;
    private final TextView previous,next,source,explanation;
    private final float unit;
    private final String name;
    private boolean compact;

    DashSourcePanel(Context context,float unitPx,String sourceName,Runnable open,Runnable choose,Runnable previousAction,Runnable toggle,Runnable nextAction){
        super(context);unit=Math.max(.1f,unitPx);name=sourceName==null?"Media":sourceName;setOrientation(VERTICAL);
        source=label(name.toUpperCase(java.util.Locale.US),12,DashboardActivity.BLUE);
        addView(source,new LayoutParams(-1,p(22)));
        title=label("Open app to start playback",23,DashboardActivity.TEXT);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);
        addView(title,new LayoutParams(-1,-2));
        artist=label("Track details appear when available",15,DashboardActivity.MUTED);artist.setMaxLines(2);artist.setEllipsize(TextUtils.TruncateAt.END);
        addView(artist,new LayoutParams(-1,-2));
        explanation=label("Uses the installed app in its own window",13,DashboardActivity.MUTED);explanation.setMaxLines(2);explanation.setGravity(Gravity.CENTER);
        addView(explanation,new LayoutParams(-1,0,1));

        LinearLayout apps=new LinearLayout(context);apps.setGravity(Gravity.CENTER_VERTICAL);
        addControl(apps,control("Open "+name,"Open "+name,open));
        addControl(apps,control("Choose app","Choose app for "+name,choose));
        addView(apps,new LayoutParams(-1,p(43)));

        LinearLayout transport=new LinearLayout(context);transport.setGravity(Gravity.CENTER_VERTICAL);
        previous=control("Previous","Previous track",previousAction);addControl(transport,previous);
        play=control("Play","Play or pause",toggle);addControl(transport,play);
        next=control("Next","Next track",nextAction);addControl(transport,next);
        addView(transport,new LayoutParams(-1,p(45)));
        setTrack("","",false,false);
    }

    @Override protected void onMeasure(int widthSpec,int heightSpec){
        // A 200-unit panel leaves 145 units after its outer padding and selector.
        // Keep both control rows reachable; expanded panels retain the original layout.
        boolean small=MeasureSpec.getMode(heightSpec)!=MeasureSpec.UNSPECIFIED&&MeasureSpec.getSize(heightSpec)<p(210);
        if(small!=compact){
            compact=small;source.setVisibility(small?GONE:VISIBLE);explanation.setVisibility(small?GONE:VISIBLE);
            title.setMaxLines(small?1:2);artist.setMaxLines(small?1:2);
        }
        super.onMeasure(widthSpec,heightSpec);
    }

    void setTrack(String trackTitle,String trackArtist,boolean matching,boolean playing){
        setText(title,matching?(empty(trackTitle)?"No track information":trackTitle):"Open app to start playback");
        setText(artist,matching?(empty(trackArtist)?"Track details appear when available":trackArtist):"Track details appear when available");
        setText(play,matching&&playing?"Pause":"Play");
        enable(previous,matching);enable(play,matching);enable(next,matching);
    }

    private int p(float value){return Math.max(1,Math.round(value*unit));}
    private TextView label(String value,int size,int color){
        TextView text=new TextView(getContext());text.setText(value);text.setTextColor(color);
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX,p(size));text.setGravity(Gravity.CENTER_VERTICAL);return text;
    }
    private TextView control(String value,String description,Runnable action){
        TextView text=label(value,14,DashboardActivity.TEXT);text.setGravity(Gravity.CENTER);
        GradientDrawable background=new GradientDrawable();background.setColor(DashboardActivity.PANEL);
        background.setCornerRadius(p(15));background.setStroke(p(1),DashboardActivity.LINE);text.setBackground(background);
        text.setContentDescription(description);text.setOnClickListener(v->{if(action!=null)action.run();});return text;
    }
    private void addControl(LinearLayout row,View child){
        LayoutParams parameters=new LayoutParams(0,-1,1);parameters.setMargins(p(3),p(3),p(3),p(3));row.addView(child,parameters);
    }
    private static void enable(TextView view,boolean enabled){if(view.isEnabled()!=enabled){view.setEnabled(enabled);view.setAlpha(enabled?1f:.4f);}}
    private static boolean empty(String value){return value==null||value.isEmpty();}
    private static void setText(TextView view,CharSequence value){if(!TextUtils.equals(view.getText(),value))view.setText(value);}
}
