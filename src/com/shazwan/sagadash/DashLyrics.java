package com.shazwan.sagadash;

import java.util.*;
import java.util.regex.*;
import org.json.*;

final class DashLyrics {
    final List<Long> times=new ArrayList<>();
    final List<String> lines=new ArrayList<>();
    String plain="Lyrics unavailable";
    static DashLyrics parse(JSONObject o){
        DashLyrics result=new DashLyrics();
        if(o.optBoolean("instrumental")){result.plain="Instrumental";return result;}
        String text=o.optString("plainLyrics", "");if(!text.isEmpty()&&!text.equals("null")) result.plain=text;
        String synced=o.optString("syncedLyrics", "");
        Pattern p=Pattern.compile("\\[(\\d+):(\\d{2})(?:[.:](\\d{1,3}))?\\]");
        TreeMap<Long,String> sorted=new TreeMap<>();
        for(String row:synced.split("\n")){
            Matcher m=p.matcher(row);List<Long> at=new ArrayList<>();int end=0;
            while(m.find()){String frac=m.group(3);long ms=frac==null?0:Long.parseLong((frac+"000").substring(0,3));at.add(Long.parseLong(m.group(1))*60000+Long.parseLong(m.group(2))*1000+ms);end=m.end();}
            for(long t:at)sorted.put(t,row.substring(end).trim());
        }
        for(Map.Entry<Long,String> e:sorted.entrySet()){result.times.add(e.getKey());result.lines.add(e.getValue());}
        return result;
    }
    String at(long position){if(times.isEmpty())return plain;int i=-1;while(i+1<times.size()&&times.get(i+1)<=position)i++;if(i<0)return "♪";return lines.get(i)+(i+1<lines.size()?"\n"+lines.get(i+1):"");}
}
