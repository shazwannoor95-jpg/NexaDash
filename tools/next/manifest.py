from pathlib import Path
import sys,xml.etree.ElementTree as E
ns='http://schemas.android.com/apk/res/android';E.register_namespace('android',ns);a=lambda k:'{'+ns+'}'+k
root=E.parse('AndroidManifest.xml');r=root.getroot();app=r.find('application');app.set(a('label'),'NexaDash Next')
# Keep original package here for generated Java R namespace; aapt2 renames installed package.
for node in app:
 name=node.get(a('name'),'')
 if name.startswith('.'):node.set(a('name'),'com.shazwan.sagadash'+name)
 affinity=node.get(a('taskAffinity'))
 if affinity:node.set(a('taskAffinity'),'com.shazwan.nexadash.next.'+affinity.split('.')[-1])
 for intent in list(node.findall('intent-filter')):
  # Keep the Dashboard HOME filter: user may choose Next manually as their launcher.
  # Only one app-drawer entry: the dashboard, not the auxiliary AI activity.
  if name!='.DashboardActivity' and any(c.get(a('name'))=='android.intent.category.LAUNCHER' for c in intent.findall('category')):node.remove(intent)
 if name=='.DashboardActivity':node.set(a('excludeFromRecents'),'false')
for node in list(app.findall('receiver')):
 if node.get(a('name'),'').endswith('.UpdatedReceiver'):app.remove(node)
for perm in list(r.findall('uses-permission')):
 if perm.get(a('name'))=='com.shazwan.embedprobe.permission.CONTROL':r.remove(perm)
out=Path(sys.argv[1]);out.parent.mkdir(parents=True,exist_ok=True);root.write(out,encoding='utf-8',xml_declaration=True)
