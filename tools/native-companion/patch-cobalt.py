#!/usr/bin/env python3
"""Co-installable Cobalt experiment: explicit embedding opt-in and own input bridge."""
import pathlib,sys,xml.etree.ElementTree as E
root=pathlib.Path(sys.argv[1]);p=root/'AndroidManifest.xml';ns='http://schemas.android.com/apk/res/android';E.register_namespace('android',ns);key=lambda s:'{'+ns+'}'+s
old='io.gh.reisxd.tizentube.cobalt';new='com.shazwan.nexadash.tizentube.lab'
t=E.parse(p);r=t.getroot()
if r.get('package')!=old:raise SystemExit('Unexpected Cobalt package')
r.set('package',new);a=r.find('application');a.set(key('label'),'TizenTube Native Lab')
for element in r.iter():
 for attr in ['authorities','permission']:
  value=element.get(key(attr))
  if value and old in value:element.set(key(attr),value.replace(old,new))
 if element.tag in ['permission','uses-permission']:
  value=element.get(key('name'),'')
  if value.startswith(old+'.'):element.set(key('name'),new+value[len(old):])
for act in a.findall('activity'):act.set(key('allowEmbedded'),'true');act.set(key('resizeableActivity'),'true')
E.SubElement(r,'permission',{key('name'):'com.shazwan.embedprobe.permission.CONTROL',key('protectionLevel'):'signature'})
E.SubElement(a,'provider',{key('name'):'com.shazwan.nexapanel.Init',key('authorities'):new+'.nexapanel.init',key('exported'):'false',key('initOrder'):'100'})
E.SubElement(a,'service',{key('name'):'com.shazwan.nexapanel.Bridge',key('exported'):'true',key('permission'):'com.shazwan.embedprobe.permission.CONTROL'})
t.write(p,encoding='utf-8',xml_declaration=True)
yml=root/'apktool.yml';s=yml.read_text().replace('versionName: 2.0.3','versionName: 2.0.3-native1');yml.write_text(s)
