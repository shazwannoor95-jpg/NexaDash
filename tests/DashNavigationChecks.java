package com.shazwan.sagadash;

import android.graphics.Bitmap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Projection and tile-decoder checks; no tile-server contact or real GPS/navigation claim. */
final class DashNavigationChecks {
    int assertions;
    void check(boolean condition,String message){if(!condition)throw new AssertionError(message);assertions++;}
    int run(){
        double[] origin=DashNavigationView.world(0,0,12);double size=256d*(1<<12);
        check(Math.abs(origin[0]-size/2)<.001&&Math.abs(origin[1]-size/2)<.001,"equator/prime meridian projects to the world centre");
        double[] higherZoom=DashNavigationView.world(0,0,13);
        check(higherZoom[0]==origin[0]*2&&higherZoom[1]==origin[1]*2,"one map zoom step doubles projected scale");
        double[] north=DashNavigationView.world(90,0,12),south=DashNavigationView.world(-90,0,12);
        check(Double.isFinite(north[1])&&north[1]>-1&&north[1]<1,"north pole clamp does not produce infinite tile coordinates");
        check(Double.isFinite(south[1])&&Math.abs(south[1]-size)<1,"south pole clamp stays within the map edge");
        double[] selangor=DashNavigationView.world(3.33,101.25,12);
        check(selangor[0]>origin[0]&&selangor[0]<size,"Kuala Selangor projects east of the prime meridian");
        check(selangor[1]<origin[1]&&selangor[1]>0,"Kuala Selangor projects north of the equator");
        double[] west=DashNavigationView.world(0,-180,12),east=DashNavigationView.world(0,180,12);
        check(west[0]==0&&east[0]==size,"date-line endpoints retain exact tile-wrap boundaries");
        decoder();
        return assertions;
    }
    private void decoder(){
        try{
            Method decode=Class.forName(DashNavigationView.class.getName()+"$TileStore").getDeclaredMethod("decodeTile",byte[].class);decode.setAccessible(true);
            Bitmap source=Bitmap.createBitmap(256,256,Bitmap.Config.RGB_565),result=null;
            try{
                ByteArrayOutputStream png=new ByteArrayOutputStream();source.compress(Bitmap.CompressFormat.PNG,100,png);
                result=(Bitmap)decode.invoke(null,(Object)png.toByteArray());
                check(result.getWidth()==256&&result.getHeight()==256&&result.getAllocationByteCount()<=256*256*4,"valid map tile has a bounded decoded allocation");
            }finally{source.recycle();if(result!=null)result.recycle();}
            source=Bitmap.createBitmap(257,256,Bitmap.Config.RGB_565);
            try{
                ByteArrayOutputStream png=new ByteArrayOutputStream();source.compress(Bitmap.CompressFormat.PNG,100,png);
                boolean rejected=false;try{decode.invoke(null,(Object)png.toByteArray());}catch(InvocationTargetException error){rejected=error.getCause() instanceof IOException;}
                check(rejected,"wrong tile dimensions are rejected by the bounded decoder");
            }finally{source.recycle();}
            boolean rejected=false;try{decode.invoke(null,(Object)new byte[]{1,2,3,4});}catch(InvocationTargetException error){rejected=error.getCause() instanceof IOException;}
            check(rejected,"invalid compressed tile data is rejected");
        }catch(Exception error){throw new AssertionError("Tile decoder checks failed",error);}
    }
}
