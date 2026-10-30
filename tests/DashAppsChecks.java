package com.shazwan.sagadash;

import android.app.AlertDialog;
import android.app.Instrumentation;
import java.util.concurrent.TimeUnit;

/** Repeated user taps must not queue app scans or reopen a dismissed app list. */
final class DashAppsChecks {
    interface Work {void run() throws Exception;}
    static void onMain(Instrumentation instrumentation,Work work)throws Exception{
        final Throwable[] error={null};instrumentation.runOnMainSync(()->{try{work.run();}catch(Throwable failure){error[0]=failure;}});
        if(error[0] instanceof Error)throw (Error)error[0];if(error[0] instanceof Exception)throw (Exception)error[0];
    }
    int assertions;
    void check(boolean ok,String message){if(!ok)throw new AssertionError(message);assertions++;}
    int run(Instrumentation instrumentation,DashboardActivity dashboard)throws Exception{
        final AlertDialog[] shown={null};
        final DashAppGrid[] opened={null};
        check(DashAppGrid.columnsFor(950,1)==5,"head-unit-sized drawer has five icon columns");
        check(DashAppGrid.columnsFor(1900,2)==5,"drawer column count follows density rather than raw pixels");
        check(DashAppGrid.columnsFor(10,1)==1&&DashAppGrid.columnsFor(4000,1)==6,"column count stays bounded on narrow and wide displays");
        android.graphics.drawable.GradientDrawable oversized=new android.graphics.drawable.GradientDrawable();
        oversized.setColor(0xff00ffaa);oversized.setSize(4096,4096);
        android.graphics.Bitmap raster=DashAppGrid.raster(oversized);
        check(raster.getWidth()==96&&raster.getHeight()==96&&raster.getAllocationByteCount()<=96*96*4,"oversized/vector-style app icons render into a bounded raster");raster.recycle();
        try{
            onMain(instrumentation,()->{
                dashboard.closeApps();dashboard.apps();int generation=dashboard.appsGeneration;
                check(dashboard.appsLoading,"app scan starts on request");
                dashboard.apps();dashboard.apps();
                check(dashboard.appsGeneration==generation,"repeated taps coalesce into one pending app scan");
            });
            dashboard.io.submit(()->{}).get(15,TimeUnit.SECONDS);
            onMain(instrumentation,()->{
                shown[0]=dashboard.appsDialog;
                check(shown[0]!=null&&shown[0].isShowing(),"one installed-app dialog appears after the scan");
                opened[0]=dashboard.appsGrid;
                check(opened[0]!=null&&opened[0].getAdapter()!=null,"Apps uses a native icon grid instead of a text list");
                check(opened[0].getAdapter().getCount()==DashShortcuts.installed(dashboard).size(),"grid contains the same safe launchable activities as shortcut discovery");
                check(opened[0].iconBytes()<=DashAppGrid.CACHE_BYTES,"app icon cache is bounded while open");
                dashboard.apps();
                check(dashboard.appsDialog==shown[0]&&!dashboard.appsLoading,"open app list is reused without another scan");
                dashboard.closeApps();
                check(dashboard.appsDialog==null&&!shown[0].isShowing(),"closing app list releases its dialog");
                check(dashboard.appsGrid==null&&opened[0].getAdapter()==null&&opened[0].iconBytes()==0,"closing drawer removes its adapter and retained icon cache");
                dashboard.apps();dashboard.closeApps();
            });
            dashboard.io.submit(()->{}).get(15,TimeUnit.SECONDS);
            onMain(instrumentation,()->{
                check(!dashboard.appsLoading,"cancelled scan completion clears loading state");
                check(dashboard.appsDialog==null,"late scan cannot reopen a cancelled app list");
            });
        }finally{onMain(instrumentation,dashboard::closeApps);}
        return assertions;
    }
}
