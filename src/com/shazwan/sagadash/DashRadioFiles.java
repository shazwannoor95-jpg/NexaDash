package com.shazwan.sagadash;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Copies only these firmware binaries. No shell, app data, tuner calls or native loading. */
final class DashRadioFiles {
    private static final String[] PATHS={
        "/system/app/GalaRadio_integrate/GalaRadio_integrate.apk",
        "/system/app/GalaRadio_integrate/oat/arm/GalaRadio_integrate.odex",
        "/system/app/GalaRadio_integrate/oat/arm/GalaRadio_integrate.vdex",
        "/system/lib/libRadioInterface.so",
        "/system/lib/libfmjni.so"
    };
    static final long MAX_FILE_BYTES=64L*1024*1024,MAX_TOTAL_BYTES=128L*1024*1024;
    interface Source {InputStream open(String path)throws IOException;}
    static final class Result {
        final int included;final long bytes;
        Result(int included,long bytes){this.included=included;this.bytes=bytes;}
        String summary(){return included+" of "+PATHS.length+" Radio files copied. "+(included==PATHS.length?
            "The ZIP is ready to share for analysis.":"Some files could not be read; the ZIP report lists the results.");}
    }
    static String[] paths(){return PATHS.clone();}
    static Result write(File zip,File temporaryDirectory,String runtimeReport,BooleanSupplier stop)throws IOException{
        return write(zip,temporaryDirectory,runtimeReport,FileInputStream::new,stop,MAX_FILE_BYTES,MAX_TOTAL_BYTES);
    }
    // The source/limits seam lets checks simulate denied, truncated and oversized firmware reads.
    static Result write(File zip,File temporaryDirectory,String runtimeReport,Source source,BooleanSupplier stop,
                        long maxFile,long maxTotal)throws IOException{
        if(maxFile<=0||maxFile>MAX_FILE_BYTES||maxTotal<=0||maxTotal>MAX_TOTAL_BYTES)throw new IllegalArgumentException("Invalid export limits");
        int included=0;long bytes=0;boolean complete=false;
        StringBuilder records=new StringBuilder();
        try{
            try(ZipOutputStream output=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zip)))){
                for(String path:PATHS){
                    cancelled(stop);File snapshot=File.createTempFile("radio-source-",".tmp",temporaryDirectory);
                    try{
                        MessageDigest digest=sha256();long size;
                        try(InputStream input=source.open(path);OutputStream staged=new BufferedOutputStream(new FileOutputStream(snapshot))){
                            size=copy(input,staged,Math.min(maxFile,maxTotal-bytes),stop,digest);
                        }catch(IOException|SecurityException failure){
                            records.append("NOT INCLUDED\t").append(path).append('\t').append(failure.getClass().getSimpleName()).append('\n');
                            continue;
                        }
                        // Only complete reads become binary entries; never ship a partial library/ODEX.
                        output.putNextEntry(new ZipEntry("files"+path));
                        try(InputStream input=new BufferedInputStream(new FileInputStream(snapshot))){copy(input,output,size,stop,null);}
                        output.closeEntry();included++;bytes+=size;
                        records.append("COPIED\t").append(path).append('\t').append(size).append(" bytes\tSHA256 ").append(hex(digest.digest())).append('\n');
                    }finally{snapshot.delete();}
                }
                cancelled(stop);
                String report="NEXADASH — RADIO FILE EXPORT\nFiles copied: "+included+"/"+PATHS.length+"\nBinary bytes copied: "+bytes+
                    "\n\n"+records+"\nFixed firmware paths only. Files that could not be read are omitted, not replaced with empty binaries.\n"+
                    "This export does not prove FM control or native panel hosting. No tuner commands, service bindings, native library loads, root/shell commands, system changes or network uploads were performed.\n\n"+runtimeReport;
                output.putNextEntry(new ZipEntry("RADIO_EXPORT_REPORT.txt"));output.write(report.getBytes(StandardCharsets.UTF_8));output.closeEntry();
            }
            cancelled(stop);complete=true;return new Result(included,bytes);
        }finally{if(!complete)zip.delete();}
    }
    private static long copy(InputStream input,OutputStream output,long limit,BooleanSupplier stop,MessageDigest digest)throws IOException{
        byte[] buffer=new byte[32768];long count=0;
        while(true){cancelled(stop);int n=input.read(buffer);if(n<0)break;if(n==0)throw new IOException("Read made no progress");
            if(n>limit-count)throw new SizeLimitException();output.write(buffer,0,n);if(digest!=null)digest.update(buffer,0,n);count+=n;
        }
        return count;
    }
    private static void cancelled(BooleanSupplier stop){if(Thread.currentThread().isInterrupted()||stop.getAsBoolean())throw new CancellationException();}
    private static MessageDigest sha256(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    private static String hex(byte[] bytes){StringBuilder out=new StringBuilder(bytes.length*2);for(byte b:bytes){out.append(Character.forDigit((b&255)>>>4,16));out.append(Character.forDigit(b&15,16));}return out.toString();}
    private static final class SizeLimitException extends IOException {}
}
