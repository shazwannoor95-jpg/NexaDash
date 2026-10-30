package com.shazwan.sagadash;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.util.*;

/** Offline district polygons mapped to current e-Solat zone codes. Never uses nearest-city guessing. */
final class PrayerZones {
    static final String[] TRAVEL_ZONES = {"SGR01", "SGR02", "SGR03", "WLY01"};
    static final class Area {
        final String district, zone;
        final List<double[][][]> polygons = new ArrayList<>();
        Area(JSONObject feature) throws JSONException {
            JSONObject p=feature.getJSONObject("properties");
            district=p.getString("district");zone=p.getString("zone");
            JSONObject g=feature.getJSONObject("geometry");JSONArray coordinates=g.getJSONArray("coordinates");
            if(g.getString("type").equals("Polygon")) polygons.add(polygon(coordinates));
            else if(g.getString("type").equals("MultiPolygon")) for(int i=0;i<coordinates.length();i++)polygons.add(polygon(coordinates.getJSONArray(i)));
            else throw new JSONException("Unsupported geometry");
        }
        static double[][][] polygon(JSONArray rings) throws JSONException {
            double[][][] out=new double[rings.length()][][];
            for(int r=0;r<rings.length();r++){
                JSONArray points=rings.getJSONArray(r);out[r]=new double[points.length()][2];
                for(int i=0;i<points.length();i++){JSONArray xy=points.getJSONArray(i);out[r][i][0]=xy.getDouble(0);out[r][i][1]=xy.getDouble(1);}
            }
            return out;
        }
    }
    static final class Match {
        final String zone,district;
        final boolean boundary;
        Match(String zone,String district,boolean boundary){this.zone=zone;this.district=district;this.boundary=boundary;}
    }
    final List<Area> areas=new ArrayList<>();
    PrayerZones(JSONObject data) throws JSONException {
        JSONArray features=data.getJSONArray("features");
        for(int i=0;i<features.length();i++)areas.add(new Area(features.getJSONObject(i)));
    }
    static PrayerZones load(Context c) throws Exception {
        try(InputStream in=c.getAssets().open("prayer-zones-klang-valley.geojson");ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
            return new PrayerZones(new JSONObject(out.toString("UTF-8")));
        }
    }
    Match locate(double lat,double lon,float accuracy){
        if(!Double.isFinite(lat)||!Double.isFinite(lon)||lat < -90||lat>90||lon < -180||lon>180||!Float.isFinite(accuracy)||accuracy<0)return null;
        Area hit=null;double edge=Double.POSITIVE_INFINITY;
        for(Area area:areas)for(double[][][] polygon:area.polygons){
            boolean inside=ringContains(polygon[0],lat,lon);
            for(int r=1;r<polygon.length;r++)if(ringContains(polygon[r],lat,lon))inside=false;
            if(!inside)continue;
            if(hit!=null&&!hit.zone.equals(area.zone))return new Match("","",true);
            hit=area;
            for(double[][] ring:polygon)edge=Math.min(edge,edgeDistance(ring,lat,lon));
        }
        if(hit==null)return null;
        // Conservative buffer for GPS error and simplified community district geometry.
        return new Match(hit.zone,hit.district,edge<=Math.max(100,accuracy+50));
    }
    static boolean ringContains(double[][] ring,double lat,double lon){
        boolean inside=false;
        for(int i=0,j=ring.length-1;i<ring.length;j=i++){
            double[] a=ring[i],b=ring[j];
            if((a[1]>lat)!=(b[1]>lat)&&lon<(b[0]-a[0])*(lat-a[1])/(b[1]-a[1])+a[0])inside=!inside;
        }
        return inside;
    }
    static double edgeDistance(double[][] ring,double lat,double lon){
        double result=Double.POSITIVE_INFINITY,kx=111320*Math.cos(Math.toRadians(lat)),ky=111320;
        for(int i=0,j=ring.length-1;i<ring.length;j=i++){
            double ax=(ring[j][0]-lon)*kx,ay=(ring[j][1]-lat)*ky,bx=(ring[i][0]-lon)*kx,by=(ring[i][1]-lat)*ky;
            double dx=bx-ax,dy=by-ay,denom=dx*dx+dy*dy;
            double t=denom==0?0:Math.max(0,Math.min(1,-(ax*dx+ay*dy)/denom));
            result=Math.min(result,Math.hypot(ax+t*dx,ay+t*dy));
        }
        return result;
    }
}
