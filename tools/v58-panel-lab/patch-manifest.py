#!/usr/bin/env python3
"""Opt-in patch for the owner's V58 APK. Does not change the SDK floor or secure-window flags."""
import pathlib,sys,xml.etree.ElementTree as E
p=pathlib.Path(sys.argv[1]);ns='http://schemas.android.com/apk/res/android';E.register_namespace('android',ns)
t=E.parse(p);r=t.getroot();a=r.find('application');key=lambda k:'{'+ns+'}'+k
if r.get('package')!='com.gold.android.youtube':raise SystemExit('This experiment only accepts the reviewed com.gold.android.youtube base.')
if any(x.get(key('name'))=='com.shazwan.nexapanel.Init' for x in a.findall('provider')):raise SystemExit('Already patched.')
for act in a.findall('activity'):act.set(key('allowEmbedded'),'true');act.set(key('resizeableActivity'),'true')
E.SubElement(r,'permission',{key('name'):'com.shazwan.embedprobe.permission.CONTROL',key('protectionLevel'):'signature'})
E.SubElement(a,'provider',{key('name'):'com.shazwan.nexapanel.Init',key('authorities'):'com.gold.android.youtube.nexapanel.init',key('exported'):'false',key('initOrder'):'100'})
E.SubElement(a,'service',{key('name'):'com.shazwan.nexapanel.Bridge',key('exported'):'true',key('permission'):'com.shazwan.embedprobe.permission.CONTROL'})
t.write(p,encoding='utf-8',xml_declaration=True)
