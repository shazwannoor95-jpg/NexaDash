package com.shazwan.sagadash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.widget.Toast;
import java.io.*;
import java.util.concurrent.*;

/** Owner-chosen local ZIP via Android's save picker; no storage permission or automatic sharing. */
final class DashRadioExport {
    static final int REQUEST=4821;
    private volatile boolean cancelled;
    private boolean busy;
    private Future<?> pending;
    private AlertDialog dialog;

    void choose(Activity owner){
        if(busy){Toast.makeText(owner,"Radio export is already running",Toast.LENGTH_SHORT).show();return;}
        owner.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/zip").putExtra(Intent.EXTRA_TITLE,"NexaDash-Radio-files.zip"),REQUEST);
    }
    void save(Activity owner,ExecutorService worker,Uri destination){
        if(busy||owner.isFinishing()||owner.isDestroyed())return;
        if(!"content".equals(destination.getScheme())){Toast.makeText(owner,"Choose a location in the Android save picker",Toast.LENGTH_LONG).show();return;}
        busy=true;cancelled=false;Context context=owner.getApplicationContext();
        Toast.makeText(owner,"Saving Radio files…",Toast.LENGTH_LONG).show();
        try{pending=worker.submit(()->{
            String message;boolean saved=false;File zip=null;
            try{
                zip=File.createTempFile("nexadash-radio-",".zip",context.getCacheDir());
                DashRadioFiles.Result result=DashRadioFiles.write(zip,context.getCacheDir(),DashRadioRuntime.collect(context),()->cancelled);
                try(InputStream input=new BufferedInputStream(new FileInputStream(zip));OutputStream output=context.getContentResolver().openOutputStream(destination,"wt")){
                    if(output==null)throw new IOException("No destination stream");byte[] buffer=new byte[32768];int n;
                    while((n=input.read(buffer))!=-1){if(cancelled||Thread.currentThread().isInterrupted())throw new CancellationException();output.write(buffer,0,n);}
                }
                saved=true;message=result.summary()+"\n\nSend NexaDash-Radio-files.zip for the next Radio analysis.";
            }catch(CancellationException stopped){return;}
            catch(IOException|RuntimeException failure){message="Radio export could not be saved ("+failure.getClass().getSimpleName()+"). Try a local Download folder with free space.";}
            finally{
                if(zip!=null)zip.delete();
                if(!saved)try{DocumentsContract.deleteDocument(context.getContentResolver(),destination);}catch(Exception ignored){}
            }
            final String text=message;final boolean success=saved;
            owner.runOnUiThread(()->{
                busy=false;pending=null;if(cancelled||owner.isFinishing()||owner.isDestroyed())return;
                AlertDialog shown=new AlertDialog.Builder(owner).setTitle(success?"Radio files saved":"Radio export failed")
                    .setMessage(text).setPositiveButton("Close",null).create();dialog=shown;
                shown.setOnDismissListener(d->{if(dialog==shown)dialog=null;});shown.show();
            });
        });}catch(RejectedExecutionException unavailable){busy=false;Toast.makeText(owner,"Radio export unavailable; reopen NexaDash and try again",Toast.LENGTH_LONG).show();}
    }
    void close(){cancelled=true;busy=false;if(pending!=null){pending.cancel(true);pending=null;}if(dialog!=null){dialog.dismiss();dialog=null;}}
}
