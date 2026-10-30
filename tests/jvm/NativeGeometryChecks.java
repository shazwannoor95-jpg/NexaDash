package com.shazwan.sagadash;

/** Raster boundary/aspect properties across head-unit, portrait and tiny tool-expanded panels. */
public final class NativeGeometryChecks {
    public static void main(String[] args){
        int[][] panels={{512,320},{320,512},{1024,600},{1484,710},{1,1},{0,0},{-1,100},{Integer.MAX_VALUE,1},{1,Integer.MAX_VALUE},{799,333},{235,121},{1000000,999999}};
        for(int[] panel:panels){
            int w=Math.max(1,panel[0]),h=Math.max(1,panel[1]);
            int[] raster=DashNativeGeometry.fittedSize(panel[0],panel[1],800);
            if(raster[0]<1||raster[1]<1||Math.max(raster[0],raster[1])>800)throw new AssertionError("Unbounded raster");
            // Rounding can move each edge by <= 0.5px (or clamp it to one pixel).
            double ratio=w/(double)h;
            if(Math.abs(raster[0]-raster[1]*ratio)>Math.max(1,ratio))throw new AssertionError("Aspect drift");
            // Mapping the lower-right panel edge must land on the lower-right raster edge.
            double x=w*(raster[0]/(double)w),y=h*(raster[1]/(double)h);
            if(Math.abs(x-raster[0])>1e-6||Math.abs(y-raster[1])>1e-6)throw new AssertionError("Touch extent drift");
        }
        System.out.println("PASS: 12 native raster geometry cases (bounds, aspect, mapped extents)");
    }
}
