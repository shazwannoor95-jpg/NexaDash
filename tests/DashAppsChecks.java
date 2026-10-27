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
                dashboard.apps();
                check(dashboard.appsDialog==shown[0]&&!dashboard.appsLoading,"open app list is reused without another scan");
                dashboard.closeApps();
                check(dashboard.appsDialog==null&&!shown[0].isShowing(),"closing app list releases its dialog");
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
