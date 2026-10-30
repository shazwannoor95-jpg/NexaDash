package com.shazwan.sagadash;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native media controls. The dashboard owns the session, clock and artwork cache. */
final class DashMediaPanel extends LinearLayout {
    final TextView title,artist,source,lyric,play,progress;
    final ImageView artwork;
    private final float unit;
    private Bitmap shownArtwork;
    private long shownSecond=Long.MIN_VALUE;

    DashMediaPanel(Context context,float unitPx,Runnable previous,Runnable toggle,Runnable next,Runnable lyricClick){
        super(context);unit=Math.max(.1f,unitPx);setOrientation(VERTICAL);
        source=label("NOW PLAYING",12,DashboardActivity.BLUE);addView(source,new LayoutParams(-1,p(23)));

        LinearLayout details=new LinearLayout(context);details.setGravity(Gravity.CENTER_VERTICAL);
        artwork=new ImageView(context);artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);artwork.setVisibility(GONE);
        artwork.setContentDescription("Album artwork");details.addView(artwork,new LayoutParams(p(68),p(68)));
        LinearLayout labels=new LinearLayout(context);labels.setOrientation(VERTICAL);labels.setPadding(p(8),0,0,0);
        title=label("No active media",24,DashboardActivity.TEXT);title.setMaxLines(1);title.setEllipsize(TextUtils.TruncateAt.END);
        artist=label("Play media in an app, or enable media access",15,DashboardActivity.MUTED);artist.setMaxLines(2);artist.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(title,new LayoutParams(-1,-2));labels.addView(artist,new LayoutParams(-1,-2));
        details.addView(labels,new LayoutParams(0,-2,1));addView(details,new LayoutParams(-1,-2));

        lyric=label("Play media to show track details",17,DashboardActivity.TEXT);lyric.setGravity(Gravity.CENTER);lyric.setMaxLines(3);
        lyric.setEllipsize(TextUtils.TruncateAt.END);lyric.setOnClickListener(v->{if(lyricClick!=null)lyricClick.run();});
        addView(lyric,new LayoutParams(-1,0,1));

        LinearLayout controls=new LinearLayout(context);controls.setGravity(Gravity.CENTER_VERTICAL);
        progress=label("0:00",13,DashboardActivity.MUTED);controls.addView(progress,new LayoutParams(p(47),-1));
        addControl(controls,control("Previous","Previous track",previous));
        play=control("Play","Play or pause",toggle);addControl(controls,play);
        addControl(controls,control("Next","Next track",next));
        addView(controls,new LayoutParams(-1,p(54)));
    }

    void update(String trackTitle,String trackArtist,String sourceName,boolean playing,Bitmap cover){
        setText(title,empty(trackTitle)?"No active media":trackTitle);
        setText(artist,empty(trackArtist)?"Track details appear when available":trackArtist);
        setText(source,empty(sourceName)?"NOW PLAYING":sourceName);
        setText(play,playing?"Pause":"Play");
        Bitmap usable=cover!=null&&!cover.isRecycled()?cover:null;
        if(shownArtwork!=usable){
            shownArtwork=usable;artwork.setImageBitmap(usable);artwork.setVisibility(usable==null?GONE:VISIBLE);
        }
    }

    void updatePosition(long positionMs,String line){
        long second=Math.max(0,positionMs/1000);
        if(second!=shownSecond){shownSecond=second;long remainder=second%60;progress.setText(second/60+":"+(remainder<10?"0":"")+remainder);}
        setText(lyric,line==null?"":line);
    }

    private int p(float value){return Math.max(1,Math.round(value*unit));}
    private TextView label(String value,int size,int color){
        TextView text=new TextView(getContext());text.setText(value);text.setTextColor(color);
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX,p(size));text.setGravity(Gravity.CENTER_VERTICAL);text.setFontFeatureSettings("tnum");return text;
    }
    private TextView control(String value,String description,Runnable action){
        TextView text=label(value,14,DashboardActivity.TEXT);text.setGravity(Gravity.CENTER);
        GradientDrawable background=new GradientDrawable();background.setColor(DashboardActivity.PANEL);
        background.setCornerRadius(p(16));background.setStroke(p(1),DashboardActivity.LINE);text.setBackground(background);
        text.setContentDescription(description);text.setOnClickListener(v->{if(action!=null)action.run();});return text;
    }
    private void addControl(LinearLayout row,View child){
        LayoutParams parameters=new LayoutParams(0,-1,1);parameters.setMargins(p(3),p(3),p(3),p(3));row.addView(child,parameters);
    }
    private static boolean empty(String value){return value==null||value.isEmpty();}
    private static void setText(TextView view,CharSequence value){if(!TextUtils.equals(view.getText(),value))view.setText(value);}
}
