package com.shazwan.sagadash;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.TreeSet;

/** Frequencies stay in integer kHz until a firmware API explicitly requires MHz. */
final class DashFmBand {
    static final int MIN=87500,MAX=108000;
    private DashFmBand(){}
    static boolean valid(int khz){return khz>=MIN&&khz<=MAX&&khz%50==0;}
    static int parse(String mhz){
        String value=mhz==null?"":mhz.trim().replace(',','.');
        if(!value.matches("[0-9]{2,3}(?:\\.[0-9]{1,2})?"))throw new IllegalArgumentException("Enter 87.50–108.00 MHz");
        int khz;try{khz=new BigDecimal(value).multiply(BigDecimal.valueOf(1000)).intValueExact();}catch(ArithmeticException bad){throw new IllegalArgumentException("Enter 87.50–108.00 MHz");}
        if(!valid(khz))throw new IllegalArgumentException("Use 87.50–108.00 MHz in 0.05 MHz steps");
        return khz;
    }
    static String text(int khz){return String.format(Locale.US,"%.2f",khz/1000.0);}
    static int step(int khz,int direction){int next=khz+(direction<0?-100:100);return next<MIN?MAX:next>MAX?MIN:next;}
    /** FmUtils.computeFrequency divides MediaTek scan entries by100: entries are10kHz. */
    static int[] scanStations(int[] entries){
        if(entries==null||entries.length>256)throw new IllegalArgumentException("Invalid FM station list");
        TreeSet<Integer> stations=new TreeSet<>();
        for(int entry:entries)if(entry>=MIN/10&&entry<=MAX/10&&valid(entry*10))stations.add(entry*10);
        int[] result=new int[stations.size()];int index=0;for(int station:stations)result[index++]=station;
        return result;
    }
}
