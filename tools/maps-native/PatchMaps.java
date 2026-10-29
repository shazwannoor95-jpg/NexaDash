import com.reandroid.apk.ApkModule;
import com.reandroid.arsc.chunk.xml.*;
import com.reandroid.arsc.value.DataType;
import java.io.*;import java.util.*;
/** Preserve resource IDs while giving the merged Maps export a separate companion identity. */
public final class PatchMaps {
 static final String OLD="com.google.android.apps.maps",NEW="com.shazwan.nexadash.maps.next",CONTROL="com.shazwan.nexadash.next.permission.CONTROL";
 static void str(ResXmlElement e,String n,int id,String v){e.getOrCreateAndroidAttribute(n,id).setDataAsString(v);}
 static void bool(ResXmlElement e,String n,int id,boolean v){e.getOrCreateAndroidAttribute(n,id).setValueAsBoolean(v);}
 public static void main(String[] args)throws Exception{
  try(ApkModule apk=ApkModule.loadApkFile(new File(args[0]))){
   AndroidManifestBlock m=apk.getAndroidManifestBlock();if(!OLD.equals(m.getPackageName()))throw new IllegalArgumentException("Unexpected Maps package");
   if(m.getMinSdkVersion()!=26||m.getVersionCode()!=1068185295)throw new IllegalArgumentException("Only the reviewed Maps25.32/API26 build is supported");
   m.ensureFullClassNames();ResXmlElement root=m.getManifestElement(),app=m.getApplicationElement();root.removeAttributesWithId(android.R.attr.sharedUserId);root.removeAttributesWithId(android.R.attr.sharedUserMaxSdkVersion);
   Map<String,String> own=new HashMap<>();for(Iterator<ResXmlElement> it=root.getElements();it.hasNext();){ResXmlElement e=it.next();if(e.getName().equals("permission")||e.getName().equals("permission-group")||e.getName().equals("permission-tree")){ResXmlAttribute a=e.searchAttributeByName("name");if(a!=null){String v=a.getDataAsString();if(v!=null)own.put(v,v.startsWith(OLD)?NEW+v.substring(OLD.length()):NEW+".owned."+v);}}}
   for(Iterator<ResXmlAttribute> it=root.recursiveAttributes();it.hasNext();){ResXmlAttribute a=it.next();String v=a.getDataAsString();if(v==null)continue;String n=a.getName();if(n.equals("authorities")){String[] vals=v.split(";");for(int i=0;i<vals.length;i++)vals[i]=vals[i].startsWith(OLD)?NEW+vals[i].substring(OLD.length()):NEW+".provider."+vals[i];a.setDataAsString(String.join(";",vals));}else if(n.equals("permission")||n.equals("readPermission")||n.equals("writePermission")||n.equals("name")){if(own.containsKey(v))a.setDataAsString(own.get(v));}else if(n.equals("taskAffinity")&&!v.isEmpty())a.setDataAsString(NEW+".task."+v);}
   m.setPackageName(NEW);apk.setPackageName(NEW);m.setApplicationLabel("Maps · NexaDash Next");m.setVersionName("25.32.00.790560726-next1");m.setExtractNativeLibs(true);
   ResXmlElement main=m.getActivity("com.google.android.maps.MapsActivity",false);if(main==null)throw new IllegalStateException("MapsActivity missing");bool(main,"allowEmbedded",android.R.attr.allowEmbedded,true);bool(main,"resizeableActivity",android.R.attr.resizeableActivity,true);ResXmlAttribute lm=main.getOrCreateAndroidAttribute("launchMode",android.R.attr.launchMode);lm.setDataType(DataType.DEC);lm.setData(0);str(main,"taskAffinity",android.R.attr.taskAffinity,NEW+".native");
   ResXmlElement perm=root.newElement("permission");str(perm,"name",android.R.attr.name,CONTROL);ResXmlAttribute level=perm.getOrCreateAndroidAttribute("protectionLevel",android.R.attr.protectionLevel);level.setDataType(DataType.HEX);level.setData(2);
   ResXmlElement init=app.newElement("provider");str(init,"name",android.R.attr.name,"com.shazwan.nexapanel.Init");str(init,"authorities",android.R.attr.authorities,NEW+".nexapanel.init");bool(init,"exported",android.R.attr.exported,false);ResXmlAttribute order=init.getOrCreateAndroidAttribute("initOrder",android.R.attr.initOrder);order.setDataType(DataType.DEC);order.setData(100);
   ResXmlElement service=app.newElement("service");str(service,"name",android.R.attr.name,"com.shazwan.nexapanel.Bridge");str(service,"permission",android.R.attr.permission,CONTROL);bool(service,"exported",android.R.attr.exported,true);
   for(String n:new String[]{"META-INF/CERT.RSA","META-INF/CERT.SF","META-INF/MANIFEST.MF","stamp-cert-sha256"})apk.removeInputSource(n);apk.setApkSignatureBlock(null);apk.refreshManifest();apk.writeApk(new File(args[1]));System.out.println("Patched separate identity/providers/permissions and main task/display opt-in; no API credential values emitted.");
  }
 }
}
