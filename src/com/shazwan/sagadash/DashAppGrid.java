package com.shazwan.sagadash;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/** Recycled native cells; icons load on demand and exist only while the app drawer is open. */
final class DashAppGrid extends GridView implements AutoCloseable {
    static final int ICON_SIZE=96,CACHE_BYTES=1024*1024;
    private final Activity activity;
    private final ExecutorService worker;
    private final DashAppearance appearance;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final List<DashShortcuts.App> apps;
    private final String[] descriptions;
    private final Map<String,Future<?>> pending=new HashMap<>();
    private final HashSet<String> failed=new HashSet<>();
    private final LruCache<String,Bitmap> icons=new LruCache<String,Bitmap>(CACHE_BYTES){
        @Override protected int sizeOf(String key,Bitmap image){return image.getAllocationByteCount();}
    };
    private volatile boolean closed;
    private final Apps adapter=new Apps();

    DashAppGrid(Activity activity,List<DashShortcuts.App> apps,ExecutorService worker,DashAppearance appearance){
        super(activity);this.activity=activity;this.apps=new ArrayList<>(apps);this.worker=worker;this.appearance=appearance;
        descriptions=DashShortcuts.chooserLabels(apps);
        setNumColumns(4);setStretchMode(STRETCH_COLUMN_WIDTH);setGravity(Gravity.CENTER);
        setHorizontalSpacing(dp(10));setVerticalSpacing(dp(10));setPadding(dp(12),dp(12),dp(12),dp(12));
        setClipToPadding(false);setSelector(android.R.color.transparent);setAdapter(adapter);
        setContentDescription("Installed apps icon grid");
    }
    static int columnsFor(int width,float density){return Math.max(1,Math.min(6,(int)(width/Math.max(1,160*density))));}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int columns=columnsFor(MeasureSpec.getSize(widthSpec)-getPaddingLeft()-getPaddingRight(),getResources().getDisplayMetrics().density);
        if(columns!=getNumColumns())setNumColumns(columns);
        super.onMeasure(widthSpec,heightSpec);
    }
    private int dp(float value){return Math.max(1,Math.round(value*getResources().getDisplayMetrics().density));}
    private String key(DashShortcuts.App app){return app.component.flattenToString();}

    private final class Cell extends LinearLayout {
        final ImageView image;final TextView label;String key;
        Cell(){super(activity);setOrientation(VERTICAL);setGravity(Gravity.CENTER);setPadding(dp(4),dp(8),dp(4),dp(4));
            image=new ImageView(activity);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(image,new LayoutParams(dp(64),dp(64)));
            label=new TextView(activity);label.setTextColor(DashboardActivity.TEXT);label.setTextSize(14);label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);label.setEllipsize(TextUtils.TruncateAt.END);label.setPadding(0,dp(6),0,0);
            addView(label,new LayoutParams(-1,dp(46)));
            setBackground(appearance.control(0x330c1b29,dp(18)));
        }
    }
    private final class Apps extends BaseAdapter {
        @Override public int getCount(){return apps.size();}
        @Override public DashShortcuts.App getItem(int position){return apps.get(position);}
        @Override public long getItemId(int position){return position;}
        @Override public View getView(int position,View recycled,ViewGroup parent){
            Cell cell=recycled instanceof Cell?(Cell)recycled:new Cell();DashShortcuts.App app=getItem(position);
            cell.key=key(app);cell.label.setText(app.label);cell.setContentDescription(descriptions[position]);
            cell.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1,dp(126)));
            Bitmap icon=icons.get(cell.key);
            if(icon!=null){cell.image.setImageBitmap(icon);cell.image.setTag(cell.key);}
            else if(!cell.key.equals(cell.image.getTag())){cell.image.setImageResource(android.R.drawable.sym_def_app_icon);cell.image.setTag(null);requestIcon(app);}
            return cell;
        }
    }
    /** Bounds the retained raster, including vector and adaptive package icons. */
    static Bitmap raster(Drawable icon){
        if(icon==null)return null;Bitmap bitmap=Bitmap.createBitmap(ICON_SIZE,ICON_SIZE,Bitmap.Config.ARGB_8888);
        android.graphics.Rect previous=new android.graphics.Rect(icon.getBounds());
        try{icon.setBounds(0,0,ICON_SIZE,ICON_SIZE);icon.draw(new Canvas(bitmap));return bitmap;}
        catch(RuntimeException failure){bitmap.recycle();return null;}
        finally{icon.setBounds(previous);}
    }
    private void requestIcon(DashShortcuts.App app){
        if(closed)return;final String key=key(app);
        if(icons.get(key)!=null||failed.contains(key)||pending.containsKey(key)||pending.size()>=24)return;
        try{Future<?> future=worker.submit(()->{
            if(closed)return;Bitmap decoded=null;
            try{decoded=raster(activity.getPackageManager().getActivityIcon(app.component));}
            catch(PackageManager.NameNotFoundException|RuntimeException ignored){}
            final Bitmap result=decoded;
            main.post(()->{
                pending.remove(key);
                if(closed){if(result!=null)result.recycle();return;}
                if(result==null)failed.add(key);
                if(result!=null){icons.put(key,result);
                    for(int i=0;i<getChildCount();i++){View view=getChildAt(i);if(view instanceof Cell&&key.equals(((Cell)view).key)){((Cell)view).image.setImageBitmap(result);((Cell)view).image.setTag(key);}}
                }
                // Retry cells deferred by the queue cap, without rebuilding the grid or an idle timer.
                for(int i=0;i<getChildCount();i++){
                    int position=getFirstVisiblePosition()+i;
                    View view=getChildAt(i);
                    if(position<apps.size()&&view instanceof Cell&&!((Cell)view).key.equals(((Cell)view).image.getTag()))requestIcon(apps.get(position));
                }
            });
        });pending.put(key,future);}catch(RejectedExecutionException ignored){}
    }
    int iconBytes(){return icons.size();}
    @Override public void close(){
        if(closed)return;closed=true;
        for(Future<?> task:pending.values())task.cancel(false);pending.clear();
        for(int i=0;i<getChildCount();i++)if(getChildAt(i) instanceof Cell)((Cell)getChildAt(i)).image.setImageDrawable(null);
        setOnItemClickListener(null);setAdapter(null);apps.clear();icons.evictAll();failed.clear();
    }
}
