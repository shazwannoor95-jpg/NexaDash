package com.shazwan.sagadash;

/** Pure geometry shared by the native raster and its panel-size checks. */
final class DashNativeGeometry {
    static int[] mapsSize(int width,int height,int maxEdge){
        return fittedSize(width,height,maxEdge,480);
    }
    static int[] fittedSize(int width,int height,int maxEdge){
        return fittedSize(width,height,maxEdge,320);
    }
    private static int[] fittedSize(int width,int height,int maxEdge,int minimumShort){
        width=Math.max(1,width);height=Math.max(1,height);
        double factor=Math.min(maxEdge/(double)Math.max(width,height),Math.max(1,minimumShort/(double)Math.min(width,height)));
        return new int[]{Math.max(1,(int)Math.round(width*factor)),Math.max(1,(int)Math.round(height*factor))};
    }
}
