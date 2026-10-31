package com.shazwan.sagadash;
public final class RouteProgressChecks {
 static int count;static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);count++;}
 public static void main(String[] args){
  double[][] path={{0,0},{0,.01},{0,.02}};
  DashRouteProgress.Projection p=DashRouteProgress.locate(path,0,.005);
  check(p.off<.01,"point on segment has zero distance");check(Math.abs(p.along-556)<2,"projects between vertices instead of snapping to nearest maneuver");check(Math.abs(p.total-2224)<3,"complete path distance");
  p=DashRouteProgress.locate(path,.001,.015);check(p.off>110&&p.off<112,"off-route distance");check(p.along>1660&&p.along<1675,"along-route progress retained while offset");
  p=DashRouteProgress.locate(path,0,-.01);check(p.along==0,"projection clamps before departure");p=DashRouteProgress.locate(path,0,.04);check(Math.abs(p.along-p.total)<.1,"projection clamps after arrival");
  p=DashRouteProgress.locate(new double[][]{{0,179.99},{0,-179.99}},0,180);check(p.off<.1&&p.total<2300&&p.along>1100,"date-line uses short crossing");
  p=DashRouteProgress.locate(new double[][]{{3,101},{3,101},{3.01,101}},3.005,101);check(Double.isFinite(p.along)&&p.off<.1,"duplicate vertices cannot divide by zero");
  check(DashRouteProgress.nextStep(new double[]{100,200,0},0)==1,"depart points to next turn");check(DashRouteProgress.nextStep(new double[]{100,200,0},100)==2,"passed turn advances to arrival");check(DashRouteProgress.nextStep(new double[]{100,200,0},400)==2,"arrival index bounded");
  for(double[] steps:new double[][]{{-1},{Double.NaN},{Double.POSITIVE_INFINITY},{}}){boolean bad=false;try{DashRouteProgress.nextStep(steps,0);}catch(IllegalArgumentException expected){bad=true;}check(bad,"bad step distances rejected");}
  for(double[][] bad:new double[][][]{null,{{0,0}},{{91,0},{0,0}},{{0,0},{0,Double.NaN}}}){boolean rejected=false;try{DashRouteProgress.locate(bad,0,0);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"malformed routes rejected");}
  for(int[] wh:new int[][]{{450,280},{1,1},{2000,10},{512,320},{320,512},{0,-3}}){int[] size=DashNativeGeometry.mapsSize(wh[0],wh[1],800);check(Math.max(size[0],size[1])<=800&&size[0]>0&&size[1]>0,"Maps raster remains bounded");}
  check(DashNativeGeometry.mapsSize(450,280,800)[1]>=480,"Maps layout gains logical height");
  System.out.println("PASS: "+count+" route progress and Maps layout assertions");
 }
}
