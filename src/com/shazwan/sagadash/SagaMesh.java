package com.shazwan.sagadash;

import android.content.Context;
import java.io.*;
import java.nio.*;
import java.util.*;

/** Immutable, prebuilt position/normal batches. No geometry allocation in the rendering loop. */
final class SagaMesh {
    static final String ASSET = "vehicle/saga-vvt.mesh";
    static final class Part {
        final String name;
        final float[] color, specular;
        final float shininess;
        final int count;
        final FloatBuffer vertices, normals;
        Part(String name,float[] color,float shine,int count,ByteBuffer data){
            this.name=name;this.color=color;this.shininess=shine;this.count=count;
            float reflection=shine>100?.85f:shine>40?.38f:.06f;
            specular=new float[]{reflection,reflection,reflection,1};
            FloatBuffer shared=data.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
            vertices=shared.duplicate();normals=shared.duplicate();normals.position(3);
        }
    }
    final List<Part> parts=new ArrayList<>();
    final float[] min={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY};
    final float[] max={Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
    int triangles;
    static SagaMesh load(Context context) throws IOException {
        byte[] bytes;
        try(InputStream in=context.getAssets().open(ASSET);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] chunk=new byte[8192];int read;
            while((read=in.read(chunk))!=-1){out.write(chunk,0,read);if(out.size()>8_000_000)throw new IOException("Vehicle mesh too large");}
            bytes=out.toByteArray();
        }
        try{return parse(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN));}
        catch(RuntimeException e){throw new IOException("Invalid vehicle mesh",e);}
    }
    static SagaMesh parse(ByteBuffer input) throws IOException {
        SagaMesh mesh=new SagaMesh();int batches=input.getInt();
        if(batches<1||batches>128)throw new IOException("Invalid mesh batch count");
        for(int i=0;i<batches;i++){
            int length=input.getInt();if(length<1||length>256)throw new IOException("Invalid mesh name");
            byte[] name=new byte[length];input.get(name);float[] color=new float[4];
            for(int c=0;c<4;c++){color[c]=input.getFloat();if(!Float.isFinite(color[c])||color[c]<0||color[c]>1)throw new IOException("Invalid material");}
            float shine=input.getFloat();if(!Float.isFinite(shine)||shine<0||shine>128)throw new IOException("Invalid shininess");
            int count=input.getInt();if(count<3||count%3!=0||count>300000)throw new IOException("Invalid triangle batch");
            int size=Math.multiplyExact(count,24);if(size>input.remaining())throw new IOException("Truncated mesh");
            ByteBuffer data=ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN);
            for(int v=0;v<count;v++){
                for(int axis=0;axis<3;axis++){
                    float value=input.getFloat();if(!Float.isFinite(value)||Math.abs(value)>10)throw new IOException("Invalid vehicle position");
                    mesh.min[axis]=Math.min(mesh.min[axis],value);mesh.max[axis]=Math.max(mesh.max[axis],value);data.putFloat(value);
                }
                float x=input.getFloat(),y=input.getFloat(),z=input.getFloat();float norm=x*x+y*y+z*z;
                if(!Float.isFinite(norm)||norm<.95f||norm>1.05f)throw new IOException("Invalid vehicle normal");
                data.putFloat(x).putFloat(y).putFloat(z);
            }
            data.flip();mesh.parts.add(new Part(new String(name,"UTF-8"),color,shine,count,data));mesh.triangles+=count/3;
        }
        if(input.hasRemaining())throw new IOException("Unexpected vehicle data");
        return mesh;
    }
}
