package com.shazwan.sagadash;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.*;

/** Firmware-free checks: allowlist, byte integrity, denied/partial reads, budgets and cancellation. */
public final class RadioFilesChecks {
    static int assertions;
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);assertions++;}
    static Map<String,byte[]> contents(File file)throws IOException{
        Map<String,byte[]> out=new LinkedHashMap<>();try(ZipFile zip=new ZipFile(file)){
            Enumeration<? extends ZipEntry> entries=zip.entries();while(entries.hasMoreElements()){
                ZipEntry entry=entries.nextElement();try(InputStream input=zip.getInputStream(entry)){out.put(entry.getName(),read(input));}
            }
        }return out;
    }
    static byte[] read(InputStream input)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=input.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}
    static String report(Map<String,byte[]> zip){return new String(zip.get("RADIO_EXPORT_REPORT.txt"),StandardCharsets.UTF_8);}
    static String hash(byte[] bytes)throws Exception{StringBuilder h=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))h.append(String.format(Locale.ROOT,"%02x",b&255));return h.toString();}
    static void noSnapshots(File root){String[] names=root.list((dir,name)->name.startsWith("radio-source-"));check(names!=null&&names.length==0,"temporary binary snapshots removed");}
    public static void main(String[] args)throws Exception{
        File dir=Files.createTempDirectory("nexa-radio-checks-").toFile();String[] paths=DashRadioFiles.paths();
        try{
            check(paths.length==5,"exactly the requested five paths");
            String first=paths[0];paths[0]="/data/private";check(DashRadioFiles.paths()[0].equals(first),"callers cannot mutate firmware allowlist");paths=DashRadioFiles.paths();
            Map<String,byte[]> sources=new LinkedHashMap<>();for(int i=0;i<paths.length;i++)sources.put(paths[i],new byte[]{(byte)i,0,1,127,(byte)255});
            List<String> opened=new ArrayList<>();File full=new File(dir,"full.zip");
            DashRadioFiles.Result r=DashRadioFiles.write(full,dir,"fixture runtime paths",path->{opened.add(path);return new ByteArrayInputStream(sources.get(path));},()->false,32,128);
            Map<String,byte[]> all=contents(full);check(r.included==5&&r.bytes==25&&all.size()==6,"five binaries and one report");
            check(opened.equals(Arrays.asList(paths)),"only the fixed firmware paths are opened");
            for(String path:paths){check(Arrays.equals(sources.get(path),all.get("files"+path)),"binary bytes preserved: "+path);check(report(all).contains(hash(sources.get(path))),"report contains correct SHA256");}
            check(report(all).contains("Files copied: 5/5")&&report(all).contains("fixture runtime paths"),"report includes result and runtime paths");noSnapshots(dir);

            File partial=new File(dir,"partial.zip");String[] targets=paths;
            r=DashRadioFiles.write(partial,dir,"",path->{
                if(path.equals(targets[0]))throw new FileNotFoundException("sensitive exception detail must not be copied");
                if(path.equals(targets[1]))throw new SecurityException("denied detail");
                if(path.equals(targets[2]))return new InputStream(){int reads;public int read()throws IOException{if(reads++==0)return 3;throw new IOException("broken read");}};
                return new ByteArrayInputStream(sources.get(path));
            },()->false,32,128);
            Map<String,byte[]> some=contents(partial);check(r.included==2&&some.size()==3,"readable files still exported after three failures");
            check(!some.containsKey("files"+targets[2]),"failed mid-read binary never appears as a partial library");
            check(report(some).contains("FileNotFoundException")&&report(some).contains("SecurityException")&&report(some).contains("IOException"),"each failure is recorded");
            check(!report(some).contains("sensitive exception")&&!report(some).contains("denied detail"),"exception messages not included");noSnapshots(dir);

            File bounded=new File(dir,"bounded.zip");
            r=DashRadioFiles.write(bounded,dir,"",path->new ByteArrayInputStream(sources.get(path)),()->false,5,12);
            Map<String,byte[]> limited=contents(bounded);check(r.included==2&&r.bytes==10,"combined byte budget enforced");
            check(report(limited).contains("SizeLimitException")&&limited.size()==3,"over-budget files omitted with report");noSnapshots(dir);
            File oversized=new File(dir,"oversized.zip");
            r=DashRadioFiles.write(oversized,dir,"",path->new ByteArrayInputStream(sources.get(path)),()->false,4,128);
            check(r.included==0&&contents(oversized).size()==1,"per-file size limit yields a valid report-only ZIP");

            File cancel=new File(dir,"cancel.zip");AtomicBoolean stop=new AtomicBoolean();
            try{DashRadioFiles.write(cancel,dir,"",path->new FilterInputStream(new ByteArrayInputStream(sources.get(path))){
                @Override public int read(byte[] b,int off,int count)throws IOException{int n=super.read(b,off,count);stop.set(true);return n;}
            },stop::get,32,128);throw new AssertionError("cancellation ignored");}catch(CancellationException expected){check(!cancel.exists(),"cancelled archive removed");}noSnapshots(dir);
            File failed=new File(dir,"missing-parent/failed.zip");
            try{DashRadioFiles.write(failed,dir,"",path->new ByteArrayInputStream(sources.get(path)),()->false,32,128);throw new AssertionError("write failure ignored");}
            catch(IOException expected){check(!failed.exists(),"destination failure leaves no archive");}
            File stalled=new File(dir,"stalled.zip");
            r=DashRadioFiles.write(stalled,dir,"",path->new InputStream(){public int read(){return 0;}public int read(byte[] b){return 0;}},()->false,32,128);
            check(r.included==0&&report(contents(stalled)).contains("IOException"),"zero-progress sources fail instead of hanging");noSnapshots(dir);
            System.out.println("PASS: "+assertions+" Radio file checks (no firmware executed)");
        }finally{File[] files=dir.listFiles();if(files!=null)for(File f:files)f.delete();dir.delete();}
    }
}
