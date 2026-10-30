package com.shazwan.sagadash;
public final class PerformanceCpuChecks {
    private static void same(double actual,double expected){if(Double.isNaN(actual)||Double.isInfinite(actual)||Math.abs(actual-expected)>.000001)throw new AssertionError(actual+" != "+expected);}
    public static void main(String[] args){
        same(DashPerformance.cpuPercent(100,400,1000,31000),1.0);
        same(DashPerformance.cpuPercent(0,60000,0,30000),200.0);
        same(DashPerformance.cpuPercent(0,0,10,30010),0.0);
        if(!Double.isNaN(DashPerformance.cpuPercent(10,9,0,30000)))throw new AssertionError("regressed CPU counter");
        if(!Double.isNaN(DashPerformance.cpuPercent(0,1,10,10)))throw new AssertionError("zero window");
        if(!Double.isNaN(DashPerformance.cpuPercent(-1,2,0,30000)))throw new AssertionError("unavailable CPU");
        same(DashPerformance.cpuPercent(Long.MAX_VALUE-100000,Long.MAX_VALUE-70000,0,30000),100.0);
        System.out.println("PASS: 7 CPU measurement cases");
    }
}
