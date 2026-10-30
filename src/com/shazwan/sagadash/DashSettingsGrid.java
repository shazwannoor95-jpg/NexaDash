package com.shazwan.sagadash;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.function.IntConsumer;

/** A native settings drawer. System icons need no package scan, bitmap cache or worker. */
final class DashSettingsGrid extends GridView implements AutoCloseable {
    private static final int[] ICONS={
        android.R.drawable.ic_menu_my_calendar,
        android.R.drawable.ic_media_play,
        android.R.drawable.ic_menu_agenda,
        android.R.drawable.ic_menu_mylocation,
        android.R.drawable.ic_menu_manage,
        android.R.drawable.ic_dialog_dialer,
        android.R.drawable.ic_lock_lock,
        android.R.drawable.ic_lock_idle_charging,
        android.R.drawable.ic_menu_myplaces,
        android.R.drawable.ic_menu_set_as,
        android.R.drawable.ic_lock_idle_alarm,
        android.R.drawable.ic_menu_crop,
        android.R.drawable.ic_menu_gallery,
        android.R.drawable.ic_menu_preferences,
        android.R.drawable.ic_dialog_info
    };
    private final Activity activity;
    private final DashAppearance appearance;
    private String[] labels;
    private IntConsumer action;
    private boolean closed;

    DashSettingsGrid(Activity activity,DashAppearance appearance,String[] labels,IntConsumer action){
        super(activity);this.activity=activity;this.appearance=appearance;
        this.labels=labels==null?new String[0]:labels.clone();this.action=action;
        setNumColumns(4);setStretchMode(STRETCH_COLUMN_WIDTH);setGravity(Gravity.CENTER);
        setHorizontalSpacing(dp(10));setVerticalSpacing(dp(10));setPadding(dp(12),dp(12),dp(12),dp(12));
        setClipToPadding(false);setSelector(android.R.color.transparent);setAdapter(new Settings());
        setContentDescription("Dashboard settings icon grid");
        setOnItemClickListener((parent,view,position,id)->{
            if(!closed&&position>=0&&position<this.labels.length&&this.action!=null)this.action.accept(position);
        });
    }
    static int columnsFor(int width,float density){return DashAppGrid.columnsFor(width,density);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        // Configure columns before GridView measures children. A change requested from
        // onSizeChanged can be swallowed by the layout pass that is already running.
        if(MeasureSpec.getMode(widthSpec)!=MeasureSpec.UNSPECIFIED){
            int width=MeasureSpec.getSize(widthSpec)-getPaddingLeft()-getPaddingRight();
            int columns=columnsFor(width,getResources().getDisplayMetrics().density);
            if(columns!=getNumColumns())setNumColumns(columns);
        }
        super.onMeasure(widthSpec,heightSpec);
    }
    private int dp(float value){return Math.max(1,Math.round(value*getResources().getDisplayMetrics().density));}

    private final class Cell extends LinearLayout {
        final ImageView icon;final TextView label;
        Cell(){
            super(activity);setOrientation(VERTICAL);setGravity(Gravity.CENTER);setPadding(dp(6),dp(10),dp(6),dp(6));
            icon=new ImageView(activity);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            icon.setColorFilter(appearance.accent(),PorterDuff.Mode.SRC_IN);
            addView(icon,new LayoutParams(dp(44),dp(44)));
            label=new TextView(activity);label.setTextColor(DashboardActivity.TEXT);label.setTextSize(13);
            label.setGravity(Gravity.CENTER);label.setMaxLines(3);label.setEllipsize(TextUtils.TruncateAt.END);
            label.setPadding(0,dp(6),0,0);addView(label,new LayoutParams(-1,dp(64)));
            setBackground(appearance.control(0x550c1b29,dp(18)));
        }
    }
    private final class Settings extends BaseAdapter {
        @Override public int getCount(){return labels.length;}
        @Override public String getItem(int position){return labels[position];}
        @Override public long getItemId(int position){return position;}
        @Override public boolean hasStableIds(){return true;}
        @Override public View getView(int position,View recycled,ViewGroup parent){
            Cell cell=recycled instanceof Cell?(Cell)recycled:new Cell();String text=getItem(position);
            cell.icon.setImageResource(position<ICONS.length?ICONS[position]:android.R.drawable.ic_dialog_info);
            cell.label.setText(text);cell.setContentDescription(text);
            cell.setLayoutParams(new AbsListView.LayoutParams(-1,dp(134)));return cell;
        }
    }
    @Override public void close(){
        if(closed)return;closed=true;action=null;setOnItemClickListener(null);setAdapter(null);labels=new String[0];
    }
}
