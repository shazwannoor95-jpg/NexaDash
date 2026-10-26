package com.shazwan.sagadash;

import android.content.*;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.*;
import android.media.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.*;

/** Bundled or locally imported, muted video preview. It is never an Android media session or a 3D model. */
final class SagaShowroom extends FrameLayout implements TextureView.SurfaceTextureListener {
    static final int PICK_CAR_VIDEO=4813;
    static final long MAX_BYTES=32L*1024*1024, MAX_DURATION_MS=120000;
    final TextureView texture;
    final TextView status;
    MediaPlayer player;
    boolean prepared,active;
    private Surface surface;
    private int position,videoWidth,videoHeight;

    SagaShowroom(Context context){
        super(context);setBackgroundColor(0xff0b1725);
        texture=new TextureView(context);texture.setSurfaceTextureListener(this);
        addView(texture,new LayoutParams(-1,-1));
        status=new TextView(context);status.setTextColor(0xffb6cddd);status.setGravity(Gravity.CENTER);
        status.setText("Choose a car video using View");addView(status,new LayoutParams(-1,-1));
        setContentDescription("Car video preview. Tap to play or pause. This is a recorded video, not an interactive 3D model.");
        setOnClickListener(v->toggle());
    }
    static File file(Context context){return new File(context.createDeviceProtectedStorageContext().getFilesDir(),"vehicle-preview.mp4");}
    static boolean hasImportedVideo(Context context){return file(context).isFile();}
    static boolean hasVideo(Context context){
        if(hasImportedVideo(context))return true;
        try(AssetFileDescriptor video=context.getResources().openRawResourceFd(R.raw.saga_showroom)){
            return video!=null&&video.getLength()>0;
        }catch(IOException|Resources.NotFoundException e){return false;}
    }
    static Intent pickIntent(){return new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("video/*").addCategory(Intent.CATEGORY_OPENABLE);}
    static void importVideo(Context context,Uri uri) throws IOException {
        if(uri==null||!"content".equals(uri.getScheme()))throw new IOException("Choose a local video file");
        try(InputStream in=context.getContentResolver().openInputStream(uri)){copyVideo(context,in);}
    }
    static synchronized void copyVideo(Context context,InputStream in) throws IOException {
        if(in==null)throw new IOException("Cannot read this file");
        File destination=file(context),temporary=new File(destination.getParentFile(),"vehicle-preview.pending");
        try{
            try(FileOutputStream out=new FileOutputStream(temporary)){
                byte[] buffer=new byte[32768];long total=0;int count;
                while((count=in.read(buffer))!=-1){
                    total+=count;if(total>MAX_BYTES)throw new IOException("Choose a video under 32 MB");
                    out.write(buffer,0,count);
                }
                out.getFD().sync();
            }
            MediaMetadataRetriever metadata=new MediaMetadataRetriever();
            try{
                metadata.setDataSource(temporary.getAbsolutePath());
                long duration=Long.parseLong(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                int width=Integer.parseInt(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                int height=Integer.parseInt(metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                if(duration<=0||duration>MAX_DURATION_MS||width<=0||height<=0||(long)width*height>3840L*2160)throw new IOException("Choose a video up to 2 minutes and 4K resolution");
                Bitmap frame=android.os.Build.VERSION.SDK_INT>=27
                    ?metadata.getScaledFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,320,180)
                    :metadata.getFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if(frame==null)throw new IOException("The video track could not be decoded");
                frame.recycle();
            }catch(RuntimeException e){throw new IOException("Choose a playable video, preferably MP4 / H.264",e);}
            finally{metadata.release();}
            // Same-directory rename preserves the previous working video on failed imports.
            if(!temporary.renameTo(destination))throw new IOException("Could not save this video");
        }finally{temporary.delete();}
    }
    void resume(){active=true;if(texture.isAvailable())open();}
    void pause(){active=false;release();}
    void reload(){release();position=0;if(active&&texture.isAvailable())open();}
    void release(){
        MediaPlayer old=player;player=null;
        if(old!=null){
            if(prepared)try{position=old.getCurrentPosition();}catch(IllegalStateException ignored){}
            prepared=false;old.reset();old.release();
        }
        if(surface!=null){surface.release();surface=null;}
    }
    void open(){
        if(!active||!texture.isAvailable()||player!=null)return;
        if(!hasVideo(getContext())){status.setText("Choose a car video using View");status.setVisibility(VISIBLE);return;}
        status.setText("Loading video preview…");status.setVisibility(VISIBLE);
        MediaPlayer current=new MediaPlayer();player=current;
        try{
            surface=new Surface(texture.getSurfaceTexture());current.setSurface(surface);
            current.setVolume(0,0); // No audio focus request, audio output or MediaSession.
            current.setLooping(false);
            current.setOnVideoSizeChangedListener((m,w,h)->{if(player==m){videoWidth=w;videoHeight=h;fit();}});
            current.setOnPreparedListener(m->{
                if(player!=m||!active)return;prepared=true;m.setVolume(0,0);
                videoWidth=m.getVideoWidth();videoHeight=m.getVideoHeight();fit();
                m.seekTo(Math.min(Math.max(1,position),Math.max(1,m.getDuration()-1)));
            });
            current.setOnCompletionListener(m->{if(player==m)position=0;});
            current.setOnErrorListener((m,what,extra)->{
                if(player==m){release();status.setText("Cannot play this video. Use View to choose another video or the 3D model.");status.setVisibility(VISIBLE);}return true;
            });
            if(hasImportedVideo(getContext()))current.setDataSource(file(getContext()).getAbsolutePath());
            else try(AssetFileDescriptor video=getResources().openRawResourceFd(R.raw.saga_showroom)){
                current.setDataSource(video.getFileDescriptor(),video.getStartOffset(),video.getLength());
            }
            current.prepareAsync();
        }catch(IOException|RuntimeException e){release();status.setText("Video unavailable. Use View to choose another video or the 3D model.");status.setVisibility(VISIBLE);}
    }
    boolean playing(){if(!prepared||player==null)return false;try{return player.isPlaying();}catch(IllegalStateException e){return false;}}
    void toggle(){
        if(!prepared||player==null)return;
        try{
            if(player.isPlaying())player.pause();
            else{if(player.getCurrentPosition()>=player.getDuration()-100)player.seekTo(1);player.start();}
        }catch(IllegalStateException ignored){}
    }
    void fit(){
        int w=texture.getWidth(),h=texture.getHeight();if(w==0||h==0||videoWidth==0||videoHeight==0)return;
        float factor=Math.min((float)w/videoWidth,(float)h/videoHeight);
        Matrix matrix=new Matrix();matrix.setScale(videoWidth*factor/w,videoHeight*factor/h,w/2f,h/2f);texture.setTransform(matrix);
    }
    @Override public void onSurfaceTextureAvailable(SurfaceTexture surface,int width,int height){if(active)open();}
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface,int width,int height){fit();}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface){release();return true;}
    @Override public void onSurfaceTextureUpdated(SurfaceTexture surface){if(prepared)status.setVisibility(GONE);}
    @Override protected void onDetachedFromWindow(){pause();super.onDetachedFromWindow();}
}
