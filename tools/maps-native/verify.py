import pathlib,zipfile,struct,hashlib,re,json,subprocess,os,sys
src=pathlib.Path(sys.argv[1]);apk=pathlib.Path(sys.argv[2]);report=pathlib.Path(sys.argv[3]);root=apk.parent;bt=pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0'
text=subprocess.check_output([str(bt/'aapt'),'dump','xmltree',str(apk),'AndroidManifest.xml'],text=True);stack=[];nodes=[]
for l in text.splitlines():
 ind=len(l)-len(l.lstrip())
 if l.lstrip().startswith('E: '):
  while stack and stack[-1]['indent']>=ind:stack.pop()
  n={'tag':l.strip().split()[1],'indent':ind,'attrs':{},'parent':stack[-1] if stack else None};nodes.append(n);stack.append(n)
 elif l.lstrip().startswith('A: ') and stack:
  q=re.search(r'A: (?:(?:http://schemas.android.com/apk/res/android|android):)?([^=(]+)(?:\([^)]*\))?=(.*)',l.strip())
  if q:
   key,val=q.groups();v=re.match(r'"([^"]*)"',val)
   if v:value=v.group(1)
   else:
    t=re.match(r'\(type (0x[0-9a-f]+)\)(0x[0-9a-f]+)',val)
    if t:
     typ,num=int(t.group(1),16),int(t.group(2),16)
     value=('true' if num else 'false') if typ==18 else (str(num) if typ==16 else '0x%08x'%num)
    else:value=val.split()[0]
   stack[-1]['attrs'][key]=value
r=nodes[0];new='com.shazwan.nexadash.maps.next';assert r['attrs']['package']==new
assert all(k not in r['attrs'] for k in ['sharedUserId','sharedUserMaxSdkVersion','requiredSplitTypes','splitTypes','split'])
assert not any(n['tag']=='uses-split' for n in nodes)
app=next(n for n in nodes if n['tag']=='application');assert app['attrs'].get('isSplitRequired','false')=='false';assert app['attrs']['label']=='Maps · NexaDash Next'
assert not any(n['tag']=='meta-data' and n['attrs'].get('name')=='com.android.vending.splits.required' and n['attrs'].get('value')=='true' for n in nodes)
main=next(n for n in nodes if n['tag']=='activity' and n['attrs'].get('name')=='com.google.android.maps.MapsActivity');assert main['attrs']['launchMode']=='0' and main['attrs']['allowEmbedded']=='true' and main['attrs']['resizeableActivity']=='true';assert main['attrs']['taskAffinity']==new+'.native'
assert main['attrs'].get('process',app['attrs'].get('process','default'))==app['attrs'].get('process','default')
providers=[n['attrs']['authorities'] for n in nodes if n['tag']=='provider'];assert all(all(s.startswith(new) for s in a.split(';')) for a in providers)
perm='com.shazwan.nexadash.next.permission.CONTROL';decl=next(n for n in nodes if n['tag']=='permission' and n['attrs'].get('name')==perm);assert decl['attrs']['protectionLevel']=='0x00000002'
service=next(n for n in nodes if n['tag']=='service' and n['attrs'].get('name')=='com.shazwan.nexapanel.Bridge');assert service['attrs']['permission']==perm and service['attrs']['exported']=='true';assert service['attrs'].get('process',app['attrs'].get('process','default'))==main['attrs'].get('process',app['attrs'].get('process','default'))
original=json.loads((root/'inputs-before.json').read_text())
for p in src.glob('*.apk'):assert hashlib.sha256(p.read_bytes()).hexdigest()==original[p.name]
with zipfile.ZipFile(apk) as out,zipfile.ZipFile(src/'Maps_split_config.armeabi_v7a.apk') as arch,zipfile.ZipFile(src/'Maps_base.apk') as base:
 assert out.testzip() is None
 native=[n for n in arch.namelist() if n.startswith('lib/') and n.endswith('.so')];assert len(native)==10
 assert all(out.read(n)==arch.read(n) for n in native)
 assert all(out.read(n)==base.read(n) for n in base.namelist() if re.fullmatch(r'classes\d*\.dex',n))
 assert 'classes10.dex' in out.namelist()
 d=out.read('classes10.dex');u=lambda off:struct.unpack_from('<I',d,off)[0];strings=[]
 for i in range(u(56)):
  pos=u(u(60)+4*i)
  while d[pos]&128:pos+=1
  pos+=1;strings.append(d[pos:d.index(0,pos)].decode('utf8','replace'))
 types=[strings[u(u(68)+4*i)] for i in range(u(64))];classes=[types[u(u(100)+32*i)] for i in range(u(96))];assert 'Lcom/shazwan/nexapanel/Bridge;' in classes and 'Lcom/shazwan/nexapanel/Init;' in classes
def resources(path):
 with zipfile.ZipFile(path) as z:
  if 'resources.arsc' not in z.namelist():return {}
 dump=subprocess.check_output([str(bt/'aapt'),'dump','resources',str(path)],text=True)
 return dict(re.findall(r'spec resource (0x[0-9a-f]+) [^:]+:([^: ]+):',dump))
oldres={}
for p in src.glob('*.apk'):oldres.update(resources(p))
newres=resources(apk);missing={k:v for k,v in oldres.items() if k not in newres}
assert missing=={'0x7f170044':'xml/splits0'},missing
assert not any(k not in oldres for k in newres)
assert '0x7f170044' not in text
sig=subprocess.check_output([str(bt/'apksigner'),'verify','--print-certs',str(apk)],text=True)
signer=re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]+)',sig).group(1)
subprocess.run([str(bt/'zipalign'),'-c','-p','4',str(apk)],check=True)
subprocess.run([str(bt/'apksigner'),'verify','--min-sdk-version','27','--max-sdk-version','27',str(apk)],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
summary={'file':apk.name,'package':new,'label':'Maps · NexaDash Next','min_sdk':26,'target_sdk':36,'base_version':'25.32.00.790560726','version_name':'25.32.00.790560726-next1','size_bytes':apk.stat().st_size,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'signer_sha256':signer,'single_apk_split_markers_removed':True,'original_inputs_unchanged':True,'native_arm32_libraries_preserved':len(native),'original_dex_preserved':9,'bridge_dex_added':'classes10.dex','main_activity_embedding_optin':True,'main_activity_launch_mode':'standard','shared_uid_removed':True,'provider_authorities_renamed':len(providers),'control_permission':perm,'runtime_validation':'NOT RUN: ARM/headunit map rendering, GMS/login, search, routing, voice, resize and full lifecycle unverified','limitations':['Re-signed/repackaged Google app may reject identity/backend or hardcoded provider lookups','Manifest/bridge checks do not establish functioning navigation or native-panel rendering']}
summary.update(original_resources=len(oldres),merged_resources=len(newres),expected_removed_resources=missing,original_input_sha256=original)
report.write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2))
