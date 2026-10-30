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
        directions();
        decoder();
        return assertions;
    }
    int runUi(android.app.Instrumentation instrumentation,DashboardActivity dashboard){
        int before=assertions;final android.app.AlertDialog[] dialog={null};final android.content.Intent[] launched={null};
        android.content.SharedPreferences prefs=DashShortcuts.prefs(dashboard);
        String oldDestination=prefs.getString("directions_destination",null),oldProvider=prefs.getString("directions_provider",null);
        android.app.Instrumentation.ActivityMonitor monitor=new android.app.Instrumentation.ActivityMonitor(){
            @Override public android.app.Instrumentation.ActivityResult onStartActivity(android.content.Intent intent){
                if(android.content.Intent.ACTION_VIEW.equals(intent.getAction())&&intent.getData()!=null){launched[0]=intent;return new android.app.Instrumentation.ActivityResult(0,null);}return null;
            }
        };
        instrumentation.addMonitor(monitor);
        try{
            instrumentation.runOnMainSync(()->dialog[0]=DashDirections.show(dashboard));
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->{
            android.widget.EditText entry=dialog[0].getWindow().getDecorView().findViewWithTag("directions_destination");
            entry.setText("  ");dialog[0].getButton(-1).performClick();
            check(dialog[0].isShowing()&&entry.getError()!=null&&launched[0]==null,"empty destination stays in the dialog without launching anything");
            entry.setText("KL Sentral & Jalan Tun #2");dialog[0].getButton(-1).performClick();
            check(launched[0]!=null&&!dialog[0].isShowing(),"Directions hands off a valid destination and closes the native entry dialog");
            android.net.Uri uri=launched[0].getData();String value=uri.getQueryParameter("destination");if(value==null)value=uri.getQueryParameter("q");
            check("KL Sentral & Jalan Tun #2".equals(value),"UI destination is passed to the selected navigation provider intact");
        });}finally{
            instrumentation.removeMonitor(monitor);
            instrumentation.runOnMainSync(()->{if(dialog[0]!=null)dialog[0].dismiss();});
            android.content.SharedPreferences.Editor edit=prefs.edit();if(oldDestination==null)edit.remove("directions_destination");else edit.putString("directions_destination",oldDestination);if(oldProvider==null)edit.remove("directions_provider");else edit.putString("directions_provider",oldProvider);edit.commit();
        }
        return assertions-before;
    }
    private void directions(){
        android.content.Intent maps=DashDirections.intent(DashDirections.MAPS,"  KL Sentral & Jalan Tun #2  ",true);
        check(DashDirections.MAPS.equals(maps.getPackage()),"Maps directions target the installed Maps package");
        check("KL Sentral & Jalan Tun #2".equals(maps.getData().getQueryParameter("destination")),"destination punctuation is URI-encoded without changing the search text");
        check("driving".equals(maps.getData().getQueryParameter("travelmode")),"Maps requests driving directions");
        android.content.Intent search=DashDirections.intent("web","医院 Kuala Lumpur",false);
        check(search.getPackage()==null&&"www.google.com".equals(search.getData().getHost()),"missing Maps uses an explicit Maps web URL");
        check("医院 Kuala Lumpur".equals(search.getData().getQueryParameter("query")),"Unicode destination survives encoding");
        android.content.Intent waze=DashDirections.intent(DashDirections.WAZE,"Shah Alam",true);
        check(DashDirections.WAZE.equals(waze.getPackage())&&"yes".equals(waze.getData().getQueryParameter("navigate")),"Waze directions stay assigned to Waze");
        check("no".equals(DashDirections.intent(DashDirections.WAZE,"Shah Alam",false).getData().getQueryParameter("navigate")),"Waze search does not request navigation");
        for(String invalid:new String[]{" ",new String(new char[241]).replace('\0','a')}){
            boolean rejected=false;try{DashDirections.intent(DashDirections.MAPS,invalid,true);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"empty and excessive destinations are rejected");
        }
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
