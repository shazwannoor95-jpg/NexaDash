package com.shazwan.sagadash;

import android.content.ComponentName;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/** Device inspection must not infer embedding support, launch source apps or change assignments. */
final class DashPanelCompatibilityChecks {
    int assertions;
    void check(boolean condition,String message){if(!condition)throw new AssertionError(message);assertions++;}

    int run(Context context){
        final int embedding=ActivityInfo.FLAG_ALLOW_UNTRUSTED_ACTIVITY_EMBEDDING;
        // AOSP manifest flag fixture only; this is not a public SDK capability query.
        final int pictureInPictureFlag=0x400000;
        check(DashPanelCompatibility.embeddingStatus(32,embedding).equals("Not available before API 33"),
            "an API 33 manifest bit cannot establish opt-in on older Android");
        check(DashPanelCompatibility.embeddingStatus(24,0).equals("Not available before API 33"),
            "minimum-supported Android reports the unavailable cooperative embedding API");
        check(DashPanelCompatibility.embeddingStatus(33,0).equals("No untrusted-embedding opt-in declared"),
            "API 33 alone is not app embedding opt-in");
        check(DashPanelCompatibility.embeddingStatus(35,pictureInPictureFlag)
                .equals("No untrusted-embedding opt-in declared"),
            "picture-in-picture support does not mean an activity can be embedded in a launcher panel");
        check(DashPanelCompatibility.embeddingStatus(33,embedding).equals("Opt-in declared; host support still required"),
            "declared activity opt-in never promises working panel hosting");
        check(DashPanelCompatibility.embeddingStatus(35,embedding|pictureInPictureFlag)
                .equals("Opt-in declared; host support still required"),
            "additional activity flags do not change the cooperative embedding caveat");

        for(String source:new String[]{"YouTube app","TizenTube","Radio","Navigation","Music","Video","Bluetooth"}){
            for(String pkg:DashShortcuts.defaults(source))
                check(DashPanelCompatibility.candidate(app(pkg,"Translated player label")),
                    "known "+source+" package is inspected independently of its localized label");
        }
        for(String label:new String[]{"YouTube","TizenTube","FM Radio","Maps","Music","Video","BT"})
            check(DashPanelCompatibility.candidate(app("fixture.vendor.player",label)),
                "OEM source label "+label+" is included without guessing its package");
        for(String label:new String[]{"Calculator","Files","YouTube Video","Radio Settings","Bluetooth Settings"})
            check(!DashPanelCompatibility.candidate(app("fixture.unrelated",label)),
                "unrelated app "+label+" is excluded from the compatibility report");

        SharedPreferences preferences=DashShortcuts.prefs(context);
        Map<String,?> before=new HashMap<>(preferences.getAll());
        String report=DashPanelCompatibility.collect(new NoLaunchContext(context));
        check(report!=null&&!report.isEmpty(),"read-only inspection produces a report on the running device");
        check(report.contains(Integer.toString(Build.VERSION.SDK_INT)),"report includes the actual API level");
        check(report.contains(Build.MODEL),"report uses the device-reported model rather than the TEST9278B example");
        check(Build.SUPPORTED_ABIS.length==0||report.contains(Build.SUPPORTED_ABIS[0]),
            "report includes the running device ABI rather than assuming ARM");
        check(report.contains("Root access: not probed"),"inspection does not claim root was tested");
        check(report.contains("No apps were launched"),"report explicitly describes passive app inspection");
        check(report.contains("not proof that app panels work"),"report does not present declared capabilities as tested hosting");
        check(before.equals(preferences.getAll()),"device inspection leaves all saved shortcut assignments unchanged");
        return assertions;
    }

    private DashShortcuts.App app(String pkg,String label){
        return new DashShortcuts.App(new ComponentName(pkg,pkg+".Player"),label,false);
    }

    int runUi(Instrumentation instrumentation,DashboardActivity dashboard)throws Exception{
        int before=assertions;
        DashPanelCompatibility compatibility=new DashPanelCompatibility();
        QueuedExecutor worker=new QueuedExecutor();
        final AlertDialog[] shown={null};
        final ClipboardManager[] clipboard={null};
        final ClipData[] previousClip={null};
        final boolean[] clipboardSaved={false};
        final String[] displayedReport={null};
        try{
            DashAppsChecks.onMain(instrumentation,()->{
                compatibility.show(dashboard,worker);
                compatibility.show(dashboard,worker);
                check(worker.size()==1,"repeated compatibility requests coalesce into one worker task");
                check(field(compatibility,"dialog")==null,"compatibility dialog does not appear before worker completion");
                compatibility.close();
            });
            // Run package inspection on the instrumentation thread, never the UI thread.
            worker.take().run();
            DashAppsChecks.onMain(instrumentation,()->{
                check(!(Boolean)field(compatibility,"loading"),"cancelled completion releases its loading state");
                check(field(compatibility,"dialog")==null,"completion cannot reopen a closed compatibility report");
                compatibility.show(dashboard,worker);
                check(worker.size()==1,"a completed cancelled check can be requested again");
            });
            worker.take().run();
            DashAppsChecks.onMain(instrumentation,()->{
                shown[0]=(AlertDialog)field(compatibility,"dialog");
                check(shown[0]!=null&&shown[0].isShowing(),"completed on-demand inspection shows one report dialog");
                ScrollView scroll=findScroll(shown[0].getWindow().getDecorView());
                check(scroll!=null&&scroll.getChildCount()==1&&scroll.getChildAt(0) instanceof TextView,
                    "long compatibility report is in a scrollable text view");
                TextView report=(TextView)scroll.getChildAt(0);
                check(report.isTextSelectable()&&report.getText().toString().contains("not proof that app panels work"),
                    "displayed report is selectable and preserves the hosting limitation");
                check("Copy report".contentEquals(shown[0].getButton(AlertDialog.BUTTON_POSITIVE).getText())
                        &&"Close".contentEquals(shown[0].getButton(AlertDialog.BUTTON_NEGATIVE).getText()),
                    "report exposes the requested copy and close controls");
                compatibility.show(dashboard,worker);
                check(worker.size()==0&&field(compatibility,"dialog")==shown[0],
                    "requesting an already visible report does not scan again or stack dialogs");
                clipboard[0]=(ClipboardManager)dashboard.getSystemService(Context.CLIPBOARD_SERVICE);
                check(clipboard[0]!=null,"device exposes clipboard service for copying the report");
                previousClip[0]=clipboard[0].getPrimaryClip();clipboardSaved[0]=true;
                displayedReport[0]=report.getText().toString();
                shown[0].getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            });
            // Button handling posts dismiss/onDismiss work in turn; wait for the entire queue.
            instrumentation.waitForIdleSync();
            DashAppsChecks.onMain(instrumentation,()->{
                ClipData copied=clipboard[0].getPrimaryClip();
                check(copied!=null&&copied.getItemCount()==1
                        &&displayedReport[0].contentEquals(copied.getItemAt(0).getText()),
                    "Copy report places the exact displayed report text on the clipboard");
                check(field(compatibility,"dialog")==null&&!shown[0].isShowing(),
                    "Copy report dismisses and releases the report dialog");
                compatibility.show(dashboard,worker);
                check(worker.size()==1,"report can be reopened after copying");
            });
            worker.take().run();
            DashAppsChecks.onMain(instrumentation,()->{
                shown[0]=(AlertDialog)field(compatibility,"dialog");
                check(shown[0]!=null&&shown[0].isShowing(),"reopened report is available for independent lifecycle cleanup");
                compatibility.close();
                check(field(compatibility,"dialog")==null&&!shown[0].isShowing(),
                    "lifecycle close dismisses and releases the compatibility dialog");
            });
        }finally{
            DashAppsChecks.onMain(instrumentation,()->{
                compatibility.close();
                if(clipboardSaved[0]){
                    if(previousClip[0]!=null)clipboard[0].setPrimaryClip(previousClip[0]);
                    else if(Build.VERSION.SDK_INT>=28)clipboard[0].clearPrimaryClip();
                    else clipboard[0].setPrimaryClip(ClipData.newPlainText("",""));
                }
            });
            worker.shutdownNow();
        }
        return assertions-before;
    }

    private static Object field(Object object,String name)throws Exception{
        Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);
    }

    private static ScrollView findScroll(View view){
        if(view instanceof ScrollView){
            ScrollView scroll=(ScrollView)view;
            if(scroll.getChildCount()==1&&scroll.getChildAt(0) instanceof TextView){
                CharSequence text=((TextView)scroll.getChildAt(0)).getText();
                if(text!=null&&text.toString().startsWith("NEXADASH — APP PANEL COMPATIBILITY"))return scroll;
            }
        }
        if(view instanceof ViewGroup){
            ViewGroup parent=(ViewGroup)view;
            for(int i=0;i<parent.getChildCount();i++){ScrollView found=findScroll(parent.getChildAt(i));if(found!=null)return found;}
        }
        return null;
    }

    /** Deliberately does not create a thread or perform collection on execute(). */
    private static final class QueuedExecutor extends AbstractExecutorService {
        private final ArrayDeque<Runnable> tasks=new ArrayDeque<>();
        private boolean stopped;
        @Override public synchronized void execute(Runnable task){if(stopped)throw new RejectedExecutionException();tasks.add(task);}
        synchronized int size(){return tasks.size();}
        synchronized Runnable take(){return tasks.remove();}
        @Override public synchronized void shutdown(){stopped=true;}
        @Override public synchronized List<Runnable> shutdownNow(){stopped=true;List<Runnable> pending=new ArrayList<>(tasks);tasks.clear();return pending;}
        @Override public synchronized boolean isShutdown(){return stopped;}
        @Override public synchronized boolean isTerminated(){return stopped&&tasks.isEmpty();}
        @Override public boolean awaitTermination(long timeout,TimeUnit unit){return isTerminated();}
    }

    /** Fail the check immediately if collecting the report attempts to open an app or settings. */
    private static final class NoLaunchContext extends ContextWrapper {
        NoLaunchContext(Context context){super(context);}
        @Override public Context getApplicationContext(){return this;}
        @Override public void startActivity(Intent intent){throw new AssertionError("Compatibility inspection launched an activity");}
        @Override public void startActivity(Intent intent,Bundle options){throw new AssertionError("Compatibility inspection launched an activity");}
        @Override public void startActivities(Intent[] intents){throw new AssertionError("Compatibility inspection launched activities");}
        @Override public void startActivities(Intent[] intents,Bundle options){throw new AssertionError("Compatibility inspection launched activities");}
    }
}
