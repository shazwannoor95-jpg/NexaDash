package com.shazwan.sagadash;

import android.app.Instrumentation;
import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

/** A settings tile must keep its original action index when cells are reused. */
final class DashSettingsGridChecks {
    int assertions;
    void check(boolean condition,String message){if(!condition)throw new AssertionError(message);assertions++;}
    int run(Instrumentation instrumentation,DashboardActivity dashboard)throws Exception{
        check(DashSettingsGrid.columnsFor(950,1)==5,"head-unit settings use five icon columns");
        check(DashSettingsGrid.columnsFor(1900,2)==5,"settings columns account for density");
        check(DashSettingsGrid.columnsFor(10,1)==1&&DashSettingsGrid.columnsFor(4000,1)==6,"settings columns stay bounded");
        final DashSettingsGrid[] grid={null};final int[] selected={-1};
        try{
            DashAppsChecks.onMain(instrumentation,()->{
                String[] labels={"Prayer zone & timetable","Media access","Lyrics: Off","Location permission","Android settings",
                    "AI assistant","Screen lock","Power saver","AI profiles","Set as default launcher","Clock / GPS time",
                    "Dashboard layout","Appearance","App panel compatibility","About NexaDash"};
                grid[0]=new DashSettingsGrid(dashboard,dashboard.appearance,labels,index->selected[0]=index);
                ListAdapter adapter=grid[0].getAdapter();
                check(adapter!=null&&adapter.getCount()==15,"settings grid retains all original actions and About");
                labels[0]="changed caller array";
                check("Prayer zone & timetable".equals(adapter.getItem(0)),"drawer owns its labels instead of mutable caller storage");
                check(adapter.hasStableIds()&&adapter.getItemId(13)==13,"compatibility tile retains its stable action index");
                View first=adapter.getView(0,null,grid[0]);
                check("Prayer zone & timetable".contentEquals(first.getContentDescription()),"settings tile supplies its full accessibility label");
                check(hasIcon(first)&&hasLabel(first,"Prayer zone & timetable"),"settings tile has an actual system icon and visible label");
                check(first.getLayoutParams().height>=Math.round(112*dashboard.getResources().getDisplayMetrics().density),"settings cells keep a large touch target");
                View reused=adapter.getView(13,first,grid[0]);
                check(reused==first&&hasLabel(reused,"App panel compatibility"),"recycled cell updates its label for a different setting");
                grid[0].performItemClick(reused,13,adapter.getItemId(13));
                check(selected[0]==13,"clicking a recycled tile dispatches the original compatibility action index");
                grid[0].performItemClick(reused,14,adapter.getItemId(14));
                check(selected[0]==14,"About can be appended without changing existing action indices");
                grid[0].close();grid[0].close();
                check(grid[0].getAdapter()==null&&grid[0].getOnItemClickListener()==null,"closing settings releases its adapter and action listener idempotently");
                check(adapter.getCount()==0,"closing settings releases its label entries");
            });
        }finally{DashAppsChecks.onMain(instrumentation,()->{if(grid[0]!=null)grid[0].close();});}
        integration(instrumentation,dashboard);
        return assertions;
    }
    private void integration(Instrumentation instrumentation,DashboardActivity dashboard)throws Exception{
        final AlertDialog[] shown={null};final DashSettingsGrid[] opened={null};
        final String version=dashboard.getPackageManager().getPackageInfo(dashboard.getPackageName(),0).versionName;
        try{
            DashAppsChecks.onMain(instrumentation,()->{
                dashboard.closeSettings();dashboard.settings();shown[0]=dashboard.settingsDialog;opened[0]=dashboard.settingsGrid;
                check(shown[0]!=null&&shown[0].isShowing(),"Dashboard settings opens its settings drawer");
                check(opened[0]!=null&&findGrid(shown[0].getWindow().getDecorView())==opened[0]&&shown[0].getListView()==null,
                    "Dashboard settings contains the native settings grid instead of a text list");
                check(opened[0].getAdapter().getCount()==dashboard.settingsLabels().length
                        &&"App panel compatibility".equals(opened[0].getAdapter().getItem(13)),
                    "integrated settings preserve all actions and compatibility index");
                check(hasLabel(shown[0].getWindow().getDecorView(),"Dashboard settings · v"+version),
                    "settings dialog shows the actual installed package version");
                check(dashboard.footerText.getText().toString().contains(version),
                    "dashboard footer shows the actual installed package version");
                dashboard.settings();
                check(dashboard.settingsDialog==shown[0]&&dashboard.settingsGrid==opened[0],
                    "repeated Settings taps reuse one existing drawer");
            });
            instrumentation.waitForIdleSync();
            final int[] width={0};long deadline=android.os.SystemClock.elapsedRealtime()+10000;
            while(width[0]<=0&&android.os.SystemClock.elapsedRealtime()<deadline){
                DashAppsChecks.onMain(instrumentation,()->width[0]=opened[0].getWidth());
                if(width[0]<=0)android.os.SystemClock.sleep(50);
            }
            DashAppsChecks.onMain(instrumentation,()->{
                check(opened[0].getWidth()>0&&opened[0].getNumColumns()==DashSettingsGrid.columnsFor(
                        opened[0].getWidth()-opened[0].getPaddingLeft()-opened[0].getPaddingRight(),
                        dashboard.getResources().getDisplayMetrics().density),
                    "first visible settings layout uses columns calculated from its actual measured width");
                dashboard.closeSettings();
                check(dashboard.settingsDialog==null&&dashboard.settingsGrid==null
                        &&!shown[0].isShowing()&&opened[0].getAdapter()==null,
                    "closing integrated settings releases the dialog and grid adapter");
                dashboard.settings();opened[0]=dashboard.settingsGrid;
                ListAdapter adapter=opened[0].getAdapter();View about=adapter.getView(14,null,opened[0]);
                opened[0].performItemClick(about,14,adapter.getItemId(14));
                check(dashboard.settingsDialog==null&&dashboard.settingsGrid==null&&opened[0].getAdapter()==null,
                    "opening About through its tile closes the settings drawer");
                check(dashboard.aboutDialog!=null&&dashboard.aboutDialog.isShowing(),
                    "About tile opens the actual About dialog");
                TextView message=dashboard.aboutDialog.findViewById(android.R.id.message);
                check(message!=null&&message.getText().toString().contains("Version: "+version)
                        &&message.getText().toString().contains(dashboard.getPackageName()),
                    "About identifies the installed version and application ID");
                check(message.getText().toString().contains("original app UI hosting is not implemented"),
                    "About states the unresolved app-hosting limitation");
                dashboard.aboutDialog.dismiss();
            });
            // Dialog dismissal posts its onDismiss listener after dismiss() returns.
            instrumentation.waitForIdleSync();
            DashAppsChecks.onMain(instrumentation,()->check(dashboard.aboutDialog==null,
                "closing About releases the tracked dialog"));
        }finally{
            DashAppsChecks.onMain(instrumentation,()->{
                dashboard.closeSettings();
                if(dashboard.aboutDialog!=null){dashboard.aboutDialog.dismiss();dashboard.aboutDialog=null;}
            });
        }
    }
    private static DashSettingsGrid findGrid(View view){
        if(view instanceof DashSettingsGrid)return (DashSettingsGrid)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){
            DashSettingsGrid found=findGrid(group.getChildAt(i));if(found!=null)return found;
        }}
        return null;
    }
    private static boolean hasIcon(View view){
        if(view instanceof ImageView)return ((ImageView)view).getDrawable()!=null;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(hasIcon(group.getChildAt(i)))return true;}
        return false;
    }
    private static boolean hasLabel(View view,String label){
        if(view instanceof TextView&&label.contentEquals(((TextView)view).getText()))return true;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(hasLabel(group.getChildAt(i),label))return true;}
        return false;
    }
}
