import com.reandroid.apk.ApkModule;
import com.reandroid.arsc.chunk.xml.*;
import com.reandroid.arsc.value.DataType;
import java.io.*;import java.util.*;
/** Adapt the reviewed Cobalt ARM export; preserve code/native payload and isolate the Next identity. */
public final class PatchTizen {
 static final String OLD="com.shazwan.nexadash.tizentube.lab",NEW="com.shazwan.nexadash.tizentube.next",LEGACY="com.shazwan.embedprobe.permission.CONTROL",CONTROL="com.shazwan.nexadash.next.permission.CONTROL";
 static void str(ResXmlElement e,String n,int id,String v){e.getOrCreateAndroidAttribute(n,id).setDataAsString(v);}
 static void bool(ResXmlElement e,String n,int id,boolean v){e.getOrCreateAndroidAttribute(n,id).setValueAsBoolean(v);}
 public static void main(String[] args)throws Exception{
  try(ApkModule apk=ApkModule.loadApkFile(new File(args[0]))){
   AndroidManifestBlock m=apk.getAndroidManifestBlock();if(!OLD.equals(m.getPackageName())||m.getVersionCode()!=203)throw new IllegalArgumentException("Expected the reviewed TizenTube Native Lab2.0.3 export");
   m.ensureFullClassNames();ResXmlElement root=m.getManifestElement(),app=m.getApplicationElement();
   for(Iterator<ResXmlAttribute> it=root.recursiveAttributes();it.hasNext();){ResXmlAttribute a=it.next();String v=a.getDataAsString();if(v==null)continue;String n=a.getName();if(n.equals("name")||n.equals("permission")||n.equals("readPermission")||n.equals("writePermission")||n.equals("authorities")||n.equals("taskAffinity")){if(v.equals(LEGACY))a.setDataAsString(CONTROL);else if(v.startsWith(OLD))a.setDataAsString(NEW+v.substring(OLD.length()));}}
   m.setPackageName(NEW);apk.setPackageName(NEW);m.setApplicationLabel("TizenTube · NexaDash Next");m.setVersionName("2.0.3-next1");m.setVersionCode(204);m.setExtractNativeLibs(true);
   ResXmlElement main=m.getActivity("dev.cobalt.app.MainActivity",false);if(main==null)throw new IllegalStateException("MainActivity missing");
   bool(main,"allowEmbedded",android.R.attr.allowEmbedded,true);bool(main,"resizeableActivity",android.R.attr.resizeableActivity,true);str(main,"taskAffinity",android.R.attr.taskAffinity,NEW+".native");
   ResXmlAttribute lm=main.getOrCreateAndroidAttribute("launchMode",android.R.attr.launchMode);lm.setDataType(DataType.DEC);lm.setData(0);
   for(String n:new String[]{"META-INF/CERT.RSA","META-INF/CERT.SF","META-INF/MANIFEST.MF","stamp-cert-sha256"})apk.removeInputSource(n);
   apk.setApkSignatureBlock(null);apk.refreshManifest();apk.writeApk(new File(args[1]));
  }
 }
}
