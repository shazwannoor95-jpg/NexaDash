package com.shazwan.sagadash;

import java.net.*;
import java.io.*;

final class DashNetwork {
    static String get(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", "KlikDash-Personal/1.0");
        try {
            if(c.getResponseCode()!=200) throw new IOException("HTTP " + c.getResponseCode());
            try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] buf=new byte[8192]; int n;
                while((n=in.read(buf))!=-1) { out.write(buf,0,n); if(out.size()>2000000) throw new IOException("Response too large"); }
                return out.toString("UTF-8");
            }
        } finally { c.disconnect(); }
    }
}
