package com.shazwan.sagadash;

import android.app.Instrumentation;
import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.opengl.GLSurfaceView;
import java.io.*;
import java.nio.*;
import java.util.*;

/** Geometry integrity and input behavior, not a claim of visual likeness or device performance. */
final class SagaModelChecks {
    int assertions;
    void check(boolean condition,String message){if(!condition)throw new AssertionError(message);assertions++;}

    int run(Context context) throws Exception {
        int before=assertions;
        SagaMesh mesh=SagaMesh.load(context);
        check(mesh.parts.size()>1&&mesh.parts.size()<=128,"vehicle has bounded material batches");
        check(mesh.triangles>500&&mesh.triangles<60000,"detailed vehicle remains within a mobile triangle budget");
        float length=mesh.max[0]-mesh.min[0],height=mesh.max[1]-mesh.min[1],width=mesh.max[2]-mesh.min[2];
        check(length>4&&length<4.7,"Saga length in metres");
        check(width>1.5&&width<2.1,"Saga width including mirrors in metres");
        check(height>1.3&&height<1.9,"Saga overall height in metres");
        check(mesh.min[1]>=-.05f&&mesh.min[1]<.10f,"tires stand near the ground plane");
        check(Math.abs(mesh.max[2]+mesh.min[2])<.1f,"body is centered laterally");
        Set<String> names=new HashSet<>();
        boolean red=false,dark=false,silver=false;
        int triangles=0,degenerate=0,reversed=0;
        for(SagaMesh.Part part:mesh.parts){
            check(names.add(part.name),"unique material batch name: "+part.name);
            check(part.count%3==0&&part.vertices.remaining()==part.count*6,"complete interleaved triangles: "+part.name);
            check(part.vertices.isDirect()&&part.normals.isDirect(),"direct GPU buffers: "+part.name);
            red|=part.color[0]>.2f&&part.color[0]>part.color[1]*2&&part.color[0]>part.color[2]*1.5f;
            dark|=part.color[0]<.15f&&part.color[1]<.2f&&part.color[2]<.25f;
            silver|=part.color[0]>.4f&&Math.abs(part.color[0]-part.color[1])<.2f&&Math.abs(part.color[1]-part.color[2])<.2f;
            FloatBuffer f=part.vertices.duplicate();
            for(int i=0;i<part.count;i+=3){
                int offset=i*6;
                double ax=f.get(offset),ay=f.get(offset+1),az=f.get(offset+2);
                double bx=f.get(offset+6),by=f.get(offset+7),bz=f.get(offset+8);
                double cx=f.get(offset+12),cy=f.get(offset+13),cz=f.get(offset+14);
                double ux=bx-ax,uy=by-ay,uz=bz-az,vx=cx-ax,vy=cy-ay,vz=cz-az;
                double nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
                double area2=Math.sqrt(nx*nx+ny*ny+nz*nz);
                if(area2<1e-12)degenerate++;
                else {
                    double sx=f.get(offset+3)+f.get(offset+9)+f.get(offset+15);
                    double sy=f.get(offset+4)+f.get(offset+10)+f.get(offset+16);
                    double sz=f.get(offset+5)+f.get(offset+11)+f.get(offset+17);
                    if((nx*sx+ny*sy+nz*sz)/area2<-.01)reversed++;
                }
                triangles++;
            }
        }
        check(triangles==mesh.triangles,"all parsed triangles are present in GPU buffers");
        check(degenerate==0,"no collapsed triangles after float32 export");
        check(reversed==0,"triangle winding agrees with surface normals");
        check(red&&dark&&silver,"paint, dark trim/glass and silver details use distinct materials");
        byte[] valid=fixture();
        check(SagaMesh.parse(ByteBuffer.wrap(valid).order(ByteOrder.LITTLE_ENDIAN)).triangles==1,"known valid fixture decodes");
        reject(new byte[0],"empty mesh rejected");
        reject(Arrays.copyOf(valid,valid.length-1),"truncated final vertex rejected");
        byte[] extra=Arrays.copyOf(valid,valid.length+1);reject(extra,"unexpected trailing data rejected");
        byte[] bad=valid.clone();ByteBuffer.wrap(bad).order(ByteOrder.LITTLE_ENDIAN).putInt(0,129);reject(bad,"excessive material batches rejected");
        bad=valid.clone();ByteBuffer.wrap(bad).order(ByteOrder.LITTLE_ENDIAN).putInt(4,0);reject(bad,"empty batch name rejected");
        int colorAt=8+"fixture".length(),countAt=colorAt+20,vertexAt=countAt+4;
        bad=valid.clone();ByteBuffer.wrap(bad).order(ByteOrder.LITTLE_ENDIAN).putFloat(colorAt,Float.NaN);reject(bad,"nonfinite material rejected");
        bad=valid.clone();ByteBuffer.wrap(bad).order(ByteOrder.LITTLE_ENDIAN).putInt(countAt,4);reject(bad,"incomplete triangle count rejected");
        bad=valid.clone();ByteBuffer.wrap(bad).order(ByteOrder.LITTLE_ENDIAN).putFloat(vertexAt,Float.POSITIVE_INFINITY);reject(bad,"nonfinite coordinates rejected");
        bad=valid.clone();ByteBuffer.wrap(bad).order(ByteOrder.LITTLE_ENDIAN).putFloat(vertexAt,11);reject(bad,"out of bounds coordinates rejected");
        bad=valid.clone();ByteBuffer.wrap(bad).order(ByteOrder.LITTLE_ENDIAN).putFloat(vertexAt+20,0);reject(bad,"zero surface normal rejected");
        return assertions-before;
    }

    int view(Instrumentation instrumentation,DashboardActivity dashboard) throws Exception {
        int before=assertions;
        long deadline=SystemClock.elapsedRealtime()+30000;
        while(dashboard.car.model==null&&dashboard.car.modelError==null&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(25);
        check(dashboard.car.modelError==null&&dashboard.car.model!=null,"GL viewer loaded actual vehicle asset");
        final Throwable[] failure=new Throwable[1];
        instrumentation.runOnMainSync(()->{
            try {
                SagaView car=dashboard.car;
                check(car.getRenderMode()==GLSurfaceView.RENDERMODE_WHEN_DIRTY,"stationary car uses on-demand rendering");
                car.resetView();long t=SystemClock.uptimeMillis();
                single(car,t,t,MotionEvent.ACTION_DOWN,50,70);
                single(car,t,t+20,MotionEvent.ACTION_MOVE,150,100);
                single(car,t,t+40,MotionEvent.ACTION_UP,150,100);
                check(Math.abs(car.yaw-SagaView.DEFAULT_YAW)>1,"horizontal drag rotates vehicle");
                check(car.pitch>SagaView.DEFAULT_PITCH&&car.pitch<=48,"vertical drag tilts vehicle within bounds");
                // The 1024x600 emulator selects a sw600dp minimum scale span (32mm).
                // Send several MOVE events after that threshold; the event that begins
                // scaling correctly has factor1 and cannot itself prove zoom works.
                int spanResource=car.getResources().getIdentifier("config_minScalingSpan","dimen","android");
                float minimumSpan=spanResource==0?220:car.getResources().getDimension(spanResource);
                float startSpan=Math.max(80,minimumSpan*.65f),endSpan=Math.max(320,minimumSpan*1.8f);
                float centerX=car.getWidth()/2f,centerY=car.getHeight()/2f;
                t+=1000;
                single(car,t,t,MotionEvent.ACTION_DOWN,centerX-startSpan/2,centerY);
                multiple(car,t,t+30,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),centerX-startSpan/2,centerY,centerX+startSpan/2,centerY);
                for(int i=1;i<=12;i++){
                    float span=startSpan+(endSpan-startSpan)*i/12;
                    multiple(car,t,t+30+i*25,MotionEvent.ACTION_MOVE,centerX-span/2,centerY,centerX+span/2,centerY);
                }
                multiple(car,t,t+360,MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),centerX-endSpan/2,centerY,centerX+endSpan/2,centerY);
                single(car,t,t+390,MotionEvent.ACTION_UP,centerX-endSpan/2,centerY);
                check(car.zoom>1&&car.zoom<=1.6f,"two-finger pinch zooms within limits");
                car.resetView();
                check(car.yaw==SagaView.DEFAULT_YAW&&car.pitch==SagaView.DEFAULT_PITCH&&car.zoom==1,"reset restores the camera");
                car.yaw=100;car.pitch=30;car.zoom=1.4f;
                t+=1000;
                single(car,t,t,MotionEvent.ACTION_DOWN,70,70);single(car,t,t+30,MotionEvent.ACTION_UP,70,70);
                single(car,t+100,t+100,MotionEvent.ACTION_DOWN,70,70);single(car,t+100,t+130,MotionEvent.ACTION_UP,70,70);
                check(car.yaw==SagaView.DEFAULT_YAW&&car.pitch==SagaView.DEFAULT_PITCH&&car.zoom==1,"double tap resets vehicle view");
            } catch(Throwable error) { failure[0]=error; }
        });
        if(failure[0] instanceof Error)throw (Error)failure[0];
        if(failure[0] instanceof Exception)throw (Exception)failure[0];
        if(failure[0]!=null)throw new AssertionError(failure[0]);
        return assertions-before;
    }

    private void reject(byte[] data,String message){
        boolean failed=false;
        try{SagaMesh.parse(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));}catch(IOException|RuntimeException expected){failed=true;}
        check(failed,message);
    }
    private static byte[] fixture(){
        byte[] name="fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteBuffer b=ByteBuffer.allocate(8+name.length+24+3*24).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(1).putInt(name.length).put(name).putFloat(.5f).putFloat(.5f).putFloat(.5f).putFloat(1).putFloat(32).putInt(3);
        for(float[] p:new float[][]{{0,0,0},{1,0,0},{0,1,0}})b.putFloat(p[0]).putFloat(p[1]).putFloat(p[2]).putFloat(0).putFloat(0).putFloat(1);
        return b.array();
    }
    private static void single(SagaView view,long down,long time,int action,float x,float y){
        MotionEvent e=MotionEvent.obtain(down,time,action,x,y,0);try{view.onTouchEvent(e);}finally{e.recycle();}
    }
    private static void multiple(SagaView view,long down,long time,int action,float x0,float y0,float x1,float y1){
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[2];
        MotionEvent.PointerCoords[] coordinates=new MotionEvent.PointerCoords[2];
        for(int i=0;i<2;i++){properties[i]=new MotionEvent.PointerProperties();properties[i].id=i;properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coordinates[i]=new MotionEvent.PointerCoords();coordinates[i].pressure=1;coordinates[i].size=1;}
        coordinates[0].x=x0;coordinates[0].y=y0;coordinates[1].x=x1;coordinates[1].y=y1;
        MotionEvent e=MotionEvent.obtain(down,time,action,2,properties,coordinates,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);
        try{view.onTouchEvent(e);}finally{e.recycle();}
    }
}
