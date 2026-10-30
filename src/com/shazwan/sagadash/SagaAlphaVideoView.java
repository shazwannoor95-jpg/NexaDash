package com.shazwan.sagadash;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.SurfaceHolder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Decodes ordinary Android video, then restores alpha from a side-by-side matte in GLES. */
final class SagaAlphaVideoView extends GLSurfaceView implements GLSurfaceView.Renderer {
    interface Listener {void ready();void lost();void frame();void error();}
    private final Handler main=new Handler(Looper.getMainLooper());
    private final FloatBuffer vertices=buffer(new float[]{-1,-1,1,-1,-1,1,1,1});
    private final FloatBuffer coordinates=buffer(new float[]{0,0,1,0,0,1,1,1});
    private final float[] matrix=new float[16];
    private final Object frameLock=new Object();
    private volatile Surface decoderSurface;
    private volatile SurfaceTexture decoderTexture;
    private Listener listener;
    private boolean framePending,hasFrame,frameNoticePosted;
    private long frameNoticeGeneration;
    private volatile boolean packed;
    private volatile int videoWidth=16,videoHeight=9;
    private int width,height,texture,program,positionAt,coordinateAt,matrixAt,scaleAt,packedAt,imageAt;
    volatile int drawnFrames;
    volatile int frameNotifications;

    SagaAlphaVideoView(Context context){
        super(context);setEGLContextClientVersion(2);
        setEGLConfigChooser(8,8,8,8,0,0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        // An RGBA SurfaceView must be above normal app views for its transparent pixels
        // to reveal the card below. Separate dialog windows remain above this surface.
        setZOrderOnTop(true);setPreserveEGLContextOnPause(true);
        setRenderer(this);setRenderMode(RENDERMODE_WHEN_DIRTY);
    }
    void setListener(Listener listener){this.listener=listener;}
    Surface decoderSurface(){return decoderSurface;}
    String frameState(){synchronized(frameLock){return "drawn="+drawnFrames+", notices="+frameNotifications+", hasFrame="+hasFrame+", pending="+framePending+", noticePosted="+frameNoticePosted+", generation="+frameNoticeGeneration+", surface="+(decoderSurface!=null)+", renderSize="+width+"x"+height+", viewSize="+getWidth()+"x"+getHeight();}}
    void setVideoSize(int physicalWidth,int physicalHeight,boolean packedAlpha){
        packed=packedAlpha;videoWidth=Math.max(1,packedAlpha?physicalWidth/2:physicalWidth);
        videoHeight=Math.max(1,physicalHeight);
        // onPrepared calls this after its prepared flag becomes true. Rearm the
        // one-shot notice even if the decoder delivered an opening frame earlier.
        synchronized(frameLock){frameNoticePosted=false;frameNoticeGeneration++;}
        requestRender();
    }
    void clearFrame(){synchronized(frameLock){framePending=false;hasFrame=false;frameNoticePosted=false;frameNoticeGeneration++;}requestRender();}
    private static FloatBuffer buffer(float[] values){
        FloatBuffer out=ByteBuffer.allocateDirect(values.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        out.put(values).position(0);return out;
    }
    private static int shader(int type,String source){
        int shader=GLES20.glCreateShader(type);GLES20.glShaderSource(shader,source);GLES20.glCompileShader(shader);
        int[] compiled=new int[1];GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,compiled,0);
        if(compiled[0]==0){String reason=GLES20.glGetShaderInfoLog(shader);GLES20.glDeleteShader(shader);throw new IllegalStateException(reason);}
        return shader;
    }
    @Override public void onSurfaceCreated(GL10 ignored,EGLConfig config){
        try{
            if(decoderSurface!=null)decoderSurface.release();
            if(decoderTexture!=null)decoderTexture.release();
            decoderSurface=null;decoderTexture=null;drawnFrames=0;
            synchronized(frameLock){framePending=false;hasFrame=false;frameNoticePosted=false;frameNoticeGeneration++;}
            int vertex=shader(GLES20.GL_VERTEX_SHADER,
                "attribute vec2 aPosition;attribute vec2 aCoordinate;uniform vec2 uScale;varying vec2 uv;"+
                "void main(){gl_Position=vec4(aPosition*uScale,0.0,1.0);uv=aCoordinate;}");
            int fragment=shader(GLES20.GL_FRAGMENT_SHADER,
                "#extension GL_OES_EGL_image_external : require\nprecision mediump float;"+
                "uniform samplerExternalOES image;uniform mat4 uMatrix;uniform float uPacked;varying vec2 uv;"+
                "vec2 transform(vec2 p){return (uMatrix*vec4(p,0.0,1.0)).xy;}"+
                "void main(){vec2 p=uv;float alpha=1.0;"+
                "if(uPacked>0.5){p.x=clamp(uv.x,0.001,0.999)*0.5;"+
                "alpha=smoothstep(0.015,0.985,texture2D(image,transform(vec2(p.x+0.5,p.y))).r);}"+
                "vec3 color=texture2D(image,transform(p)).rgb;"+
                // Android's surface compositor expects premultiplied alpha.
                "gl_FragColor=vec4(color*alpha,alpha);}");
            program=GLES20.glCreateProgram();GLES20.glAttachShader(program,vertex);GLES20.glAttachShader(program,fragment);
            GLES20.glLinkProgram(program);GLES20.glDeleteShader(vertex);GLES20.glDeleteShader(fragment);
            int[] linked=new int[1];GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,linked,0);
            if(linked[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));
            positionAt=GLES20.glGetAttribLocation(program,"aPosition");coordinateAt=GLES20.glGetAttribLocation(program,"aCoordinate");
            matrixAt=GLES20.glGetUniformLocation(program,"uMatrix");scaleAt=GLES20.glGetUniformLocation(program,"uScale");
            packedAt=GLES20.glGetUniformLocation(program,"uPacked");imageAt=GLES20.glGetUniformLocation(program,"image");
            int[] textures=new int[1];GLES20.glGenTextures(1,textures,0);texture=textures[0];
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,texture);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
            SurfaceTexture fresh=new SurfaceTexture(texture);decoderTexture=fresh;
            fresh.setOnFrameAvailableListener(source->{
                synchronized(frameLock){if(decoderTexture!=source)return;framePending=true;}
                requestRender();
            });
            decoderSurface=new Surface(fresh);
            main.post(()->{if(listener!=null&&decoderSurface!=null)listener.ready();});
        }catch(RuntimeException error){main.post(()->{if(listener!=null)listener.error();});}
    }
    @Override public void onSurfaceChanged(GL10 ignored,int width,int height){
        this.width=width;this.height=height;GLES20.glViewport(0,0,width,height);
    }
    @Override public void onDrawFrame(GL10 ignored){
        GLES20.glClearColor(0,0,0,0);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        boolean notifyFrame=false;long noticeGeneration=0;
        synchronized(frameLock){
            if(framePending&&decoderTexture!=null){
                try{
                    decoderTexture.updateTexImage();decoderTexture.getTransformMatrix(matrix);
                    framePending=false;hasFrame=true;
                }catch(RuntimeException error){
                    framePending=false;hasFrame=false;
                    main.post(()->{if(listener!=null)listener.error();});return;
                }
            }
            if(!hasFrame||program==0)return;
            if(!frameNoticePosted){frameNoticePosted=true;notifyFrame=true;noticeGeneration=frameNoticeGeneration;}
        }
        float viewAspect=(float)width/Math.max(1,height),videoAspect=(float)videoWidth/videoHeight;
        float x=viewAspect>videoAspect?videoAspect/viewAspect:1;
        float y=viewAspect>videoAspect?1:viewAspect/videoAspect;
        GLES20.glDisable(GLES20.GL_BLEND);GLES20.glUseProgram(program);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,texture);
        GLES20.glUniform1i(imageAt,0);
        GLES20.glUniformMatrix4fv(matrixAt,1,false,matrix,0);GLES20.glUniform2f(scaleAt,x,y);
        GLES20.glUniform1f(packedAt,packed?1:0);
        vertices.position(0);coordinates.position(0);
        GLES20.glEnableVertexAttribArray(positionAt);GLES20.glEnableVertexAttribArray(coordinateAt);
        GLES20.glVertexAttribPointer(positionAt,2,GLES20.GL_FLOAT,false,0,vertices);
        GLES20.glVertexAttribPointer(coordinateAt,2,GLES20.GL_FLOAT,false,0,coordinates);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        GLES20.glDisableVertexAttribArray(positionAt);GLES20.glDisableVertexAttribArray(coordinateAt);
        drawnFrames++;
        if(notifyFrame){
            final long epoch=noticeGeneration;
            main.post(()->{
                synchronized(frameLock){if(epoch!=frameNoticeGeneration||!hasFrame)return;}
                if(listener!=null){frameNotifications++;listener.frame();}
            });
        }
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder){
        if(listener!=null)listener.lost();super.surfaceDestroyed(holder);
    }
    @Override protected void onDetachedFromWindow(){
        if(listener!=null)listener.lost();super.onDetachedFromWindow();
        synchronized(frameLock){
            if(decoderSurface!=null)decoderSurface.release();decoderSurface=null;
            if(decoderTexture!=null)decoderTexture.release();decoderTexture=null;
            hasFrame=false;framePending=false;frameNoticePosted=false;frameNoticeGeneration++;
        }
    }
}
