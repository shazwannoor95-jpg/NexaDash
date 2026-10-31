package com.shazwan.sagadash;

/** Bounded, local geometry only. No GPS, network or Android dependencies. */
final class DashRouteProgress {
    static final double METRES=111195.0802335329;
    static double wrap(double degrees){return ((degrees+180)%360+360)%360-180;}
    static double distance(double a,double b,double c,double d){double lat=Math.toRadians(c-a),lon=Math.toRadians(wrap(d-b));double q=Math.sin(lat/2)*Math.sin(lat/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*Math.sin(lon/2)*Math.sin(lon/2);return 12742000*Math.asin(Math.sqrt(Math.max(0,Math.min(1,q))));}
    static final class Projection {final double off,along,total;Projection(double o,double a,double t){off=o;along=a;total=t;}}
    static Projection locate(double[][] path,double lat,double lon){
        if(path==null||path.length<2||path.length>10000||!valid(lat,lon))throw new IllegalArgumentException("Invalid route");
        double best=Double.POSITIVE_INFINITY,along=0,total=0;
        for(int i=1;i<path.length;i++){
            double[] a=path[i-1],b=path[i];if(a==null||b==null||a.length!=2||b.length!=2||!valid(a[0],a[1])||!valid(b[0],b[1]))throw new IllegalArgumentException("Invalid coordinate");
            double cosine=Math.cos(Math.toRadians((a[0]+b[0])/2)),x=wrap(b[1]-a[1])*METRES*cosine,y=(b[0]-a[0])*METRES;
            double px=wrap(lon-a[1])*METRES*cosine,py=(lat-a[0])*METRES,squared=x*x+y*y;
            double t=squared==0?0:Math.max(0,Math.min(1,(px*x+py*y)/squared));double off=Math.hypot(px-t*x,py-t*y),length=distance(a[0],a[1],b[0],b[1]);
            if(off<best){best=off;along=total+t*length;}total+=length;
        }
        return new Projection(best,along,total);
    }
    static boolean valid(double lat,double lon){return Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180;}
    static int nextStep(double[] lengths,double progress){
        if(lengths==null||lengths.length==0||lengths.length>2000||!Double.isFinite(progress))throw new IllegalArgumentException("Invalid instructions");double end=0;
        for(int i=0;i<lengths.length;i++){double n=lengths[i];if(!Double.isFinite(n)||n<0)throw new IllegalArgumentException("Invalid step distance");end+=n;}
        end=0;for(int i=0;i<lengths.length;i++){end+=lengths[i];if(progress<end)return Math.min(i+1,lengths.length-1);}return lengths.length-1;
    }
}
