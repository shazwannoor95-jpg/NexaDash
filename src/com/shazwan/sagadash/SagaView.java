package com.shazwan.sagadash;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.util.Log;
import java.nio.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Touch-driven studio viewer for the hand-modelled Saga VVT asset. */
final class SagaView extends GLSurfaceView implements GLSurfaceView.Renderer {
    static final float DEFAULT_YAW=35, DEFAULT_PITCH=14;
    volatile float yaw=DEFAULT_YAW, pitch=DEFAULT_PITCH, zoom=1;
    volatile SagaMesh model;
    volatile String modelError;
    private float x,y,dragDistance,distance=7;
    private final ScaleGestureDetector scaleGesture;
    private final GestureDetector gestures;
    private final FloatBuffer shadowVertices,shadowColors;
    private final float[] ambient={.32f,.37f,.44f,1};
    private final float[] keyPosition={-3,5,4,0},keyDiffuse={.88f,.91f,1,1};
    private final float[] fillPosition={4,2,-3,0},fillDiffuse={.32f,.42f,.60f,1};
    private FloatBuffer frontPlate, rearPlate;
    private final FloatBuffer plateUv=buffer(new float[]{0,1,1,1,1,0,0,0});
    private int plateTexture;

    SagaView(Context context){
        super(context);
        setEGLConfigChooser(8,8,8,0,16,0);
        setPreserveEGLContextOnPause(true);
        int steps=64;float[] vertices=new float[(steps+2)*3],colors=new float[(steps+2)*4];
        vertices[1]=.006f;colors[3]=.48f;
        for(int i=0;i<=steps;i++){
            double a=i*Math.PI*2/steps;int v=(i+1)*3,c=(i+1)*4;
            vertices[v]=(float)Math.cos(a)*2.6f;vertices[v+1]=.006f;vertices[v+2]=(float)Math.sin(a)*1.22f;
            colors[c+3]=0;
        }
        shadowVertices=buffer(vertices);shadowColors=buffer(colors);
        scaleGesture=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScale(ScaleGestureDetector detector){zoom=Math.max(.75f,Math.min(1.6f,zoom*detector.getScaleFactor()));requestRender();return true;}
        });
        gestures=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onDoubleTap(MotionEvent e){resetView();return true;}
        });
        setRenderer(this);setRenderMode(RENDERMODE_WHEN_DIRTY);
        setContentDescription("Red Proton Saga VVT 3D model. Drag to rotate, pinch to zoom, double tap to reset.");
        // Asset decoding must not hold the GL thread while SurfaceView waits for its first frame.
        new Thread(()->{
            try{
                SagaMesh loaded=SagaMesh.load(getContext());
                frontPlate=plate(loaded,"plate_front",new int[]{0,1,2,5},-1);
                rearPlate=plate(loaded,"plate_rear",new int[]{5,0,1,2},1);
                model=loaded;requestRender();
            }catch(Exception e){modelError=e.getMessage();Log.e("SagaDash","Vehicle model could not load",e);}
        },"Saga-model-loader").start();
    }
    static FloatBuffer buffer(float[] data){FloatBuffer b=ByteBuffer.allocateDirect(data.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(data).position(0);return b;}
    void resetView(){yaw=DEFAULT_YAW;pitch=DEFAULT_PITCH;zoom=1;requestRender();}
    @Override public void onSurfaceCreated(GL10 gl,EGLConfig config){
        gl.glClearColor(.045f,.09f,.15f,1);gl.glEnable(GL10.GL_DEPTH_TEST);gl.glDepthFunc(GL10.GL_LEQUAL);
        gl.glEnable(GL10.GL_NORMALIZE);gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT,ambient,0);
        gl.glLightModelf(GL10.GL_LIGHT_MODEL_TWO_SIDE,1);
        gl.glEnable(GL10.GL_LIGHT0);gl.glEnable(GL10.GL_LIGHT1);
        gl.glLightfv(GL10.GL_LIGHT0,GL10.GL_DIFFUSE,keyDiffuse,0);
        gl.glLightfv(GL10.GL_LIGHT0,GL10.GL_SPECULAR,keyDiffuse,0);
        gl.glLightfv(GL10.GL_LIGHT1,GL10.GL_DIFFUSE,fillDiffuse,0);
        gl.glShadeModel(GL10.GL_SMOOTH);
        Bitmap bitmap=Bitmap.createBitmap(256,64,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);canvas.drawColor(0xff090b10);
        Paint text=new Paint(Paint.ANTI_ALIAS_FLAG);text.setColor(0xffedf2f5);text.setTypeface(Typeface.create(Typeface.MONOSPACE,Typeface.BOLD));text.setTextSize(37);text.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm=text.getFontMetrics();canvas.drawText("SAGA DASH",128,32-(fm.ascent+fm.descent)/2,text);
        int[] textures=new int[1];gl.glGenTextures(1,textures,0);plateTexture=textures[0];gl.glBindTexture(GL10.GL_TEXTURE_2D,plateTexture);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D,GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);gl.glTexParameterf(GL10.GL_TEXTURE_2D,GL10.GL_TEXTURE_MAG_FILTER,GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D,GL10.GL_TEXTURE_WRAP_S,GL10.GL_CLAMP_TO_EDGE);gl.glTexParameterf(GL10.GL_TEXTURE_2D,GL10.GL_TEXTURE_WRAP_T,GL10.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D,0,bitmap,0);bitmap.recycle();

    }
    private FloatBuffer plate(SagaMesh mesh,String name,int[] corners,int outward) throws java.io.IOException {
        for(SagaMesh.Part part:mesh.parts)if(part.name.equals(name)&&part.count>=6){
            float[] data=new float[12];
            for(int i=0;i<4;i++){int offset=corners[i]*6;data[i*3]=part.vertices.get(offset)+outward*.003f;data[i*3+1]=part.vertices.get(offset+1);data[i*3+2]=part.vertices.get(offset+2);}
            return buffer(data);
        }
        throw new java.io.IOException("Missing vehicle plate geometry: "+name);
    }
    @Override public void onSurfaceChanged(GL10 gl,int width,int height){
        float aspect=(float)width/Math.max(1,height);
        distance=Math.max(5.2f,2.6f/((float)Math.tan(Math.toRadians(17))*aspect)+1.4f);
        gl.glViewport(0,0,width,height);gl.glMatrixMode(GL10.GL_PROJECTION);gl.glLoadIdentity();
        GLU.gluPerspective(gl,34,aspect,.1f,40);gl.glMatrixMode(GL10.GL_MODELVIEW);
    }
    @Override public void onDrawFrame(GL10 gl){
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT|GL10.GL_DEPTH_BUFFER_BIT);gl.glLoadIdentity();
        gl.glLightfv(GL10.GL_LIGHT0,GL10.GL_POSITION,keyPosition,0);gl.glLightfv(GL10.GL_LIGHT1,GL10.GL_POSITION,fillPosition,0);
        gl.glTranslatef(0,-.02f,-distance/zoom);gl.glRotatef(pitch,1,0,0);gl.glRotatef(yaw,0,1,0);gl.glTranslatef(0,-.68f,0);
        gl.glDisable(GL10.GL_LIGHTING);gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
        gl.glEnable(GL10.GL_BLEND);gl.glBlendFunc(GL10.GL_SRC_ALPHA,GL10.GL_ONE_MINUS_SRC_ALPHA);gl.glDepthMask(false);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);gl.glColorPointer(4,GL10.GL_FLOAT,0,shadowColors);gl.glVertexPointer(3,GL10.GL_FLOAT,0,shadowVertices);
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN,0,66);gl.glDisableClientState(GL10.GL_COLOR_ARRAY);gl.glDisable(GL10.GL_BLEND);gl.glDepthMask(true);
        SagaMesh mesh=model;if(mesh==null)return;
        gl.glEnable(GL10.GL_LIGHTING);gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
        for(SagaMesh.Part part:mesh.parts){
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK,GL10.GL_AMBIENT_AND_DIFFUSE,part.color,0);
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK,GL10.GL_SPECULAR,part.specular,0);
            gl.glMaterialf(GL10.GL_FRONT_AND_BACK,GL10.GL_SHININESS,part.shininess);
            gl.glVertexPointer(3,GL10.GL_FLOAT,24,part.vertices);gl.glNormalPointer(GL10.GL_FLOAT,24,part.normals);
            gl.glDrawArrays(GL10.GL_TRIANGLES,0,part.count);
        }
        gl.glDisable(GL10.GL_LIGHTING);gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
        gl.glEnable(GL10.GL_TEXTURE_2D);gl.glBindTexture(GL10.GL_TEXTURE_2D,plateTexture);gl.glColor4f(1,1,1,1);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);gl.glTexCoordPointer(2,GL10.GL_FLOAT,0,plateUv);
        gl.glVertexPointer(3,GL10.GL_FLOAT,0,frontPlate);gl.glDrawArrays(GL10.GL_TRIANGLE_FAN,0,4);
        gl.glVertexPointer(3,GL10.GL_FLOAT,0,rearPlate);gl.glDrawArrays(GL10.GL_TRIANGLE_FAN,0,4);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);gl.glDisable(GL10.GL_TEXTURE_2D);
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        scaleGesture.onTouchEvent(event);gestures.onTouchEvent(event);
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){x=event.getX();y=event.getY();dragDistance=0;getParent().requestDisallowInterceptTouchEvent(true);}
        else if(action==MotionEvent.ACTION_MOVE){
            if(!scaleGesture.isInProgress()&&event.getPointerCount()==1){float dx=event.getX()-x,dy=event.getY()-y;dragDistance+=Math.abs(dx)+Math.abs(dy);yaw=(yaw+dx*.4f)%360;pitch=Math.max(3,Math.min(48,pitch+dy*.25f));requestRender();}
            x=event.getX();y=event.getY();
        }else if(action==MotionEvent.ACTION_POINTER_UP){int remaining=event.getActionIndex()==0?1:0;x=event.getX(remaining);y=event.getY(remaining);}
        else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){getParent().requestDisallowInterceptTouchEvent(false);if(action==MotionEvent.ACTION_UP&&dragDistance<8)performClick();}
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
