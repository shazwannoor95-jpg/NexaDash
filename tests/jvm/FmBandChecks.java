package com.shazwan.sagadash;
import java.util.Arrays;

/** Frequency/unit boundaries matter because the two recovered firmware APIs use different units. */
public final class FmBandChecks {
    static int count;
    static void check(boolean yes,String text){if(!yes)throw new AssertionError(text);count++;}
    public static void main(String[] args){
        String[] values={"87.5","93.90","108","95.85","99,9"," 100.00 "};
        int[] expected={87500,93900,108000,95850,99900,100000};
        for(int i=0;i<values.length;i++)check(DashFmBand.parse(values[i])==expected[i],"MHz -> kHz: "+values[i]);
        for(String bad:new String[]{"","NaN","Infinity","87500","0","86.9","108.05","93.91","1e2","87.500","-93.9","93..9"}){
            boolean rejected=false;try{DashFmBand.parse(bad);}catch(IllegalArgumentException expectedFailure){rejected=true;}check(rejected,"reject invalid/out-of-band MHz: "+bad);
        }
        check("93.90".equals(DashFmBand.text(93900)),"stable two-decimal MHz display");
        check(DashFmBand.step(108000,1)==87500&&DashFmBand.step(87500,-1)==108000,"FM step wraps at band boundaries");
        check(DashFmBand.step(93900,1)==94000&&DashFmBand.step(93900,-1)==93800,"0.1MHz tuning step");
        check(Arrays.equals(DashFmBand.scanStations(new int[]{10800,8750,9390,8750,0,-100,Integer.MAX_VALUE,9391}),new int[]{87500,93900,108000}),"10kHz scan results are sorted, unique and range/grid-checked");
        check(DashFmBand.scanStations(new int[]{0,-100}).length==0,"OEM scan error sentinels are not shown as stations");
        for(int[] bad:new int[][]{null,new int[257]}){boolean rejected=false;try{DashFmBand.scanStations(bad);}catch(IllegalArgumentException expectedFailure){rejected=true;}check(rejected,"reject malformed/oversized station data");}
        System.out.println("PASS: "+count+" FM band/unit checks");
    }
}
