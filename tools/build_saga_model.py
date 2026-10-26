#!/usr/bin/env python3
"""Create the personal Saga VVT exterior study from geometric surfaces.

No downloaded mesh, photo billboard or proprietary CAD is used. Units are metres:
X front negative, Y up with ground=0, Z lateral. Output is a little-endian batched
triangle stream consumed directly by SagaView. Re-run with Python 3 only.
"""
from pathlib import Path
from collections import OrderedDict
import math, struct, json, argparse

ROOT=Path(__file__).resolve().parents[1]/'assets'/'vehicle'
PARSER=argparse.ArgumentParser(description=__doc__)
PARSER.add_argument('--json',type=Path,help='Optional JSON inspection export outside app assets')
ARGS=PARSER.parse_args()
TAU=math.tau
M=OrderedDict()
MATS={
 'paint':((.60,.035,.052,1),64),
 'paint_edge':((.64,.045,.064,1),72),
 'paint_dark':((.27,.015,.025,1),55),
 'glass':((.055,.086,.100,1),112),
 'rubber':((.018,.022,.026,1),8),
 'black_trim':((.028,.033,.039,1),34),
 'grille':((.055,.066,.075,1),40),
 'chrome':((.68,.74,.78,1),115),
 'wheel_silver':((.56,.62,.67,1),100),
 'brake':((.19,.21,.23,1),25),
 'headlamp':((.67,.76,.81,1),118),
 'lamp_white':((.91,.93,.89,1),85),
 'tail_red':((.70,.009,.020,1),95),
 'tail_highlight':((.82,.025,.035,1),100),
 'amber':((.69,.30,.045,1),65),
 'plate_front':((.008,.009,.010,1),10),
 'plate_rear':((.008,.009,.010,1),10),
}
for k,(color,shine) in MATS.items():M[k]={'color':color,'shininess':shine,'vertices':[]}
def add(a,b):return tuple(x+y for x,y in zip(a,b))
def sub(a,b):return tuple(x-y for x,y in zip(a,b))
def mul(a,k):return tuple(x*k for x in a)
def dot(a,b):return sum(x*y for x,y in zip(a,b))
def cross(a,b):return(a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0])
def length(v):return math.sqrt(dot(v,v))
def norm(v):
 n=length(v)
 return mul(v,1/n) if n>1e-24 else(0,1,0)
def lerp(a,b,t):return tuple(x+(y-x)*t for x,y in zip(a,b))
def tri(mat,a,b,c,na=None,nb=None,nc=None):
 n=cross(sub(b,a),sub(c,a))
 if length(n)<1e-10:return
 n=norm(n)
 if na is not None and dot(n,add(add(na,nb),nc))<0:
  b,c=c,b;nb,nc=nc,nb;n=mul(n,-1)
 for p,nn in zip((a,b,c),(na,nb,nc)):
  M[mat]['vertices'].extend((*p,*(norm(nn) if nn else n)))
def quad(mat,a,b,c,d,normal=None):
 tri(mat,a,b,c,normal,normal,normal);tri(mat,a,c,d,normal,normal,normal)
def face(mat,points,normal=None):
 for i in range(1,len(points)-1):tri(mat,points[0],points[i],points[i+1],normal,normal,normal)
def surface(mat,func,nu,nv,u0=0,u1=1,v0=0,v1=1,reverse=False):
 def point(i,j):return func(u0+(u1-u0)*i/nu,v0+(v1-v0)*j/nv)
 def normal(i,j):
  u=u0+(u1-u0)*i/nu;v=v0+(v1-v0)*j/nv;eps=1e-5
  n=norm(cross(sub(func(u+eps,v),func(u-eps,v)),sub(func(u,v+eps),func(u,v-eps))))
  return mul(n,-1) if reverse else n
 for i in range(nu):
  for j in range(nv):
   ps=[point(i,j),point(i+1,j),point(i+1,j+1),point(i,j+1)]
   ns=[normal(i,j),normal(i+1,j),normal(i+1,j+1),normal(i,j+1)]
   if reverse:ps.reverse();ns.reverse()
   tri(mat,*ps[:3],*ns[:3]);tri(mat,ps[0],ps[2],ps[3],ns[0],ns[2],ns[3])
def tube(mat,points,radius=.004,sides=6):
 sides=min(sides,4 if radius<.012 else 6)
 for a,b in zip(points,points[1:]):
  direction=norm(sub(b,a));ref=(0,1,0) if abs(direction[1])<.9 else(1,0,0)
  u=norm(cross(direction,ref));v=cross(direction,u)
  for j in range(sides):
   th=j*TAU/sides;ph=(j+1)*TAU/sides
   n=add(mul(u,math.cos(th)),mul(v,math.sin(th)));nn=add(mul(u,math.cos(ph)),mul(v,math.sin(ph)))
   aa=add(a,mul(n,radius));bb=add(b,mul(n,radius));cc=add(b,mul(nn,radius));dd=add(a,mul(nn,radius))
   tri(mat,aa,bb,cc,n,n,nn);tri(mat,aa,cc,dd,n,nn,nn)
def ellipsoid(mat,center,scale,nu=24,nv=12):
 nu=min(nu,28);nv=min(nv,14)
 def f(u,v):
  th=u*TAU;ph=v*math.pi
  return(center[0]+scale[0]*math.sin(ph)*math.cos(th),center[1]+scale[1]*math.cos(ph),center[2]+scale[2]*math.sin(ph)*math.sin(th))
 surface(mat,f,nu,nv)
def torus(mat,center,major,minor,axial=None,segments=48,sides=8):
 axial=minor if axial is None else axial
 segments=min(segments,48);sides=min(sides,10)
 def f(u,v):
  t=u*TAU;p=v*TAU;r=major+minor*math.cos(p)
  return(center[0]+r*math.cos(t),center[1]+r*math.sin(t),center[2]+axial*math.sin(p))
 surface(mat,f,segments,sides)
def disk(mat,center,r,side=1,segments=48):
 for i in range(segments):
  a=i*TAU/segments;b=(i+1)*TAU/segments
  tri(mat,center,(center[0]+r*math.cos(a),center[1]+r*math.sin(a),center[2]),(center[0]+r*math.cos(b),center[1]+r*math.sin(b),center[2]),(0,0,side),(0,0,side),(0,0,side))
def box(mat,c,s):
 x,y,z=c;a,b,d=s
 pts=[(x-a,y-b,z-d),(x+a,y-b,z-d),(x+a,y+b,z-d),(x-a,y+b,z-d),(x-a,y-b,z+d),(x+a,y-b,z+d),(x+a,y+b,z+d),(x-a,y+b,z+d)]
 for ids in[(0,3,2,1),(4,5,6,7),(0,1,5,4),(3,7,6,2),(0,4,7,3),(1,2,6,5)]:quad(mat,*(pts[i] for i in ids))
def profile(x,points):
 # Shape-preserving cubic interpolation: continuous slopes rather than a flat
 # tangent at every station, which made the roof and fenders look stepped.
 if x<=points[0][0]:return points[0][1]
 if x>=points[-1][0]:return points[-1][1]
 def slope(i):
  if i==0:return (points[1][1]-points[0][1])/(points[1][0]-points[0][0])
  if i==len(points)-1:return (points[-1][1]-points[-2][1])/(points[-1][0]-points[-2][0])
  left=(points[i][1]-points[i-1][1])/(points[i][0]-points[i-1][0])
  right=(points[i+1][1]-points[i][1])/(points[i+1][0]-points[i][0])
  return 0 if left*right<=0 else 2*left*right/(left+right)
 for i,(a,b) in enumerate(zip(points,points[1:])):
  if a[0]<=x<=b[0]:
   h=b[0]-a[0];t=(x-a[0])/h
   return (2*t**3-3*t*t+1)*a[1]+(t**3-2*t*t+t)*h*slope(i)+(-2*t**3+3*t*t)*b[1]+(t**3-t*t)*h*slope(i+1)
WIDTH=[(-2.1655,.735),(-2.01,.802),(-1.65,.84),(-.5,.8445),(.8,.8445),(1.65,.84),(2.04,.802),(2.1655,.735)]
TOP=[(-2.1655,.905),(-1.97,.94),(-.95,1.005),(.8,1.005),(1.45,.99),(2.0,.96),(2.1655,.94)]
AXLES=(-1.25,1.215)
def width(x):return profile(x,WIDTH)
def top(x):return profile(x,TOP)
def lower(x):
 y=.215
 for axle in AXLES:
  dx=x-axle
  if abs(dx)<.337:y=max(y,.30+math.sqrt(max(0,.337**2-dx**2)))
 return y
def side_point(x,y,sign,extra=0):
 t=max(0,min(1,(y-.215)/max(.01,top(x)-.215)))
 bulge=.91+.09*math.sin(math.pi*t*.86)
 return(x,y,sign*(width(x)*bulge+extra))
# Smooth painted side shell with genuine wheel arch openings, rather than black decals.
xs=sorted(set([-2.1655+i*4.331/120 for i in range(121)]+[a+dx for a in AXLES for dx in[-.338,-.337,-.336,.336,.337,.338]]))
for sign in[-1,1]:
 for xa,xb in zip(xs,xs[1:]):
  for j in range(8):
   def pt(x,t):
    lo=lower(x);hi=top(x)-.09
    return side_point(x,lo+(hi-lo)*t,sign)
   a,b,c,d=pt(xa,j/8),pt(xb,j/8),pt(xb,(j+1)/8),pt(xa,(j+1)/8)
   # Longitudinal/vertical finite differences yield a smooth side surface normal.
   def no(x,t):
    y=lower(x)+(top(x)-.09-lower(x))*t
    return norm(cross(sub(pt(x+1e-5,t),pt(x-1e-5,t)),sub(pt(x,t+1e-5),pt(x,t-1e-5)))) if sign>0 else mul(norm(cross(sub(pt(x+1e-5,t),pt(x-1e-5,t)),sub(pt(x,t+1e-5),pt(x,t-1e-5)))),-1)
   ns=[no(xa,j/8),no(xb,j/8),no(xb,(j+1)/8),no(xa,(j+1)/8)]
   if sign<0:a,b,c,d=d,c,b,a;ns.reverse()
   tri('paint',a,b,c,*ns[:3]);tri('paint',a,c,d,ns[0],ns[2],ns[3])
 # Painted wheel arch lip and black inner liner are actual thin curved surfaces.
 for axle in AXLES:
  pts=[]
  for i in range(33):
   t=math.pi*i/32;xx=axle+.337*math.cos(t);yy=.30+.337*math.sin(t)
   pts.append(side_point(xx,yy,sign,.003))
  tube('paint_edge',pts,.009,5)
  def liner(u,v):
   t=math.pi*u;r=.338+v*.012
   return(axle+r*math.cos(t),.30+r*math.sin(t),sign*(.76-v*.025))
  surface('black_trim',liner,32,2,reverse=sign<0)
 # Real body crease and sill; the wheel openings stay open.
 crease=[side_point(-1.95+i*3.93/48,.755+.035*math.sin(i/48*math.pi),sign,.003) for i in range(49)]
 tube('paint_edge',crease,.003,5)
 sill=[side_point(-.86+i*1.72/20,.228,sign,.001) for i in range(21)]
 tube('paint_dark',sill,.012,6)
# Hood/deck surface; cabin will cover the central section.
def deck(u,v):
 x=-2.1655+u*4.331;t=max(-1.,min(1.,v*2-1))
 edge=abs(side_point(x,top(x)-.09,1)[2])
 # A rounded shoulder meets the side shell tangentially, instead of a 90deg
 # box edge. The wide centre retains the Saga's pressed bonnet/deck shape.
 y=top(x)-.09+.09*math.sqrt(max(0.,1-t**6))
 return(x,y,t*edge)
surface('paint',deck,96,24,reverse=True)
# Front/rear end caps round slightly into the fenders.
for end in[-1,1]:
 def cap(u,v,end=end):
  y=.215+v*(top(end*2.1655)-.215-.09+.09*math.sqrt(max(0.,1-(u*2-1)**6)))
  z=(u*2-1)*abs(side_point(end*2.1655,y,1)[2])
  return(end*2.1655,y,z)
 surface('paint',cap,32,12,reverse=end>0)
# Opaque chassis only visible deep within the wheel wells.
box('black_trim',(0,.225,0),(1.90,.035,.66))
# Hood perimeter, boot seams and integrated rear lip.
for sign in[-1,1]:
 pts=[]
 for i in range(25):
  x=-1.98+i*1.02/24;z=sign*(.61+.10*i/24);pts.append((x,top(x)-.045*(z/width(x))**2+.003,z))
 tube('paint_dark',pts,.003,5)
 tube('paint_dark',[(1.40,.962,sign*.65),(1.74,.945,sign*.65),(2.06,.90,sign*.57),(2.172,.57,sign*.53)],.003,5)
tube('paint_edge',[(2.09,.932,-.70),(2.13,.935,0),(2.09,.932,.70)],.014,7)
# Cabin loft: raised roof and curved windscreen/rear screen, not a rectangular extrusion.
RW=[(-.97,.76),(-.44,.663),(.20,.658),(.70,.667),(1.37,.75)]
def roof(x):
 return profile(x,[(-.97,1.005),(-.72,1.205),(-.44,1.435),(-.22,1.485),(.25,1.492),(.61,1.46),(.72,1.435),(1.10,1.16),(1.37,.98)])

def cabin(u,v):
 x=-.97+u*2.34;t=v*2-1
 y=roof(x)-.045*t*t;z=profile(x,RW)*t
 return(x,y,z)
surface('paint',cabin,64,16,reverse=True)
# Painted side cabin supports provide real pillars behind the glazing.
for sign in[-1,1]:
 def cabin_side(u,v,sign=sign):
  x=-.97+u*2.34;y0=top(x)-.015;y1=roof(x)-.045
  return(x,y0+(y1-y0)*v,sign*(.785+(profile(x,RW)-.785)*v+.018*math.sin(math.pi*v)))
 surface('paint',cabin_side,36,3,reverse=sign<0)
 # Window frames: front window and rear window with a real B-pillar gap.
 windows=[ [(-.875,.988),(-.394,1.390),(.142,1.402),(.154,.982)],[(.257,.982),(.249,1.403),(.678,1.378),(1.185,1.000)] ]
 def window_point(x,y,offset=0):
  y0=top(x)-.015;y1=roof(x)-.045;t=max(0,min(1,(y-y0)/max(.02,y1-y0)))
  return(x,y,sign*(.785+(profile(x,RW)-.785)*t+.018*math.sin(math.pi*t)+offset))
 for window in windows:
  def window_surface(corners,offset):
   def f(u,v):
    bottom=lerp(corners[0],corners[3],u);upper=lerp(corners[1],corners[2],u);xx,yy=lerp(bottom,upper,v)
    return window_point(xx,yy,offset)
   return f
  surface('black_trim',window_surface(window,.007),8,4,reverse=sign<0)
  cx=sum(x for x,y in window)/4;cy=sum(y for x,y in window)/4
  inner=[(cx+(x-cx)*.94,cy+(y-cy)*.91) for x,y in window]
  surface('glass',window_surface(inner,.015),8,4,reverse=sign<0)
  # Visors sit above each window, matching the owner's dark rain guards.
  topedge=[window_point(window[1][0],window[1][1]+.018,.016),window_point(window[2][0],window[2][1]+.018,.016)]
  tube('black_trim',topedge,.017,6)
 # Door shut-lines follow rear wheel opening rather than crossing the tire.
 for path in[[(-.86,.962),(-.80,.60),(-.77,.285),(.20,.265),(.205,.976)],[(.205,.976),(.21,.265),(.84,.27),(.87,.52),(1.11,.75),(1.20,.974)]]:
  tube('paint_dark',[side_point(x,y,sign,.006) for x,y in path],.003,5)
 # Black/chrome handle covers and mirror housings.
 for hx in[-.23,.83]:
  z=sign*(width(hx)+.007)
  ellipsoid('black_trim',(hx,.897,z),(.114,.031,.016),20,8)
  tube('chrome',[(hx-.065,.900,z+sign*.014),(hx+.067,.902,z+sign*.014)],.007,6)
 tube('black_trim',[(-.77,1.018,sign*.756),(-.77,1.015,sign*.863)],.023,8)
 ellipsoid('paint',(-.80,1.041,sign*.889),(.115,.069,.074),24,10)
 ellipsoid('black_trim',(-.74,1.040,sign*.903),(.016,.048,.054),16,8)
 # Side indicator on the mirror body.
 tube('amber',[(-.87,1.038,sign*.929),(-.78,1.036,sign*.956)],.006,6)
# Windshield/rear glass in curved grids, black peripheral rubber gaskets.
def screen(front,u,v,offset=0):
 t=u*2-1
 x=(-.955+v*.545) if front else (1.318-v*.598)
 return(x,roof(x)-.045*t*t+offset,profile(x,RW)*t)
for front in[True,False]:
 surface('black_trim',lambda u,v:screen(front,u,v,.005),16,8,reverse=not front)
 surface('glass',lambda u,v:screen(front,u,v,.012),16,8,.035,.965,.035,.965,reverse=not front)
# Rear screen heater lines and front wipers.
for i in range(1,8):tube('grille',[screen(False,.055+j*.89/20,i/9,.014) for j in range(21)],.001,4)
for z in[-.38,.15]:
 tube('black_trim',[(-.947,1.037,z-.14),(-.80,1.137,z+.13)],.005,6)
# Slim roof aerial as in the photos.
tube('black_trim',[(.68,1.465,0),(.83,1.72,0)],.007,7)
ellipsoid('black_trim',(.68,1.467,0),(.048,.017,.033),16,6)
# Front grille and lamp assembly. X is curved outward by the wrap at each corner.
def hull_width_at_height(x,y):
 if y<=top(x)-.09:return abs(side_point(x,y,1)[2])
 if y>=top(x):return 0.
 h=(y-top(x)+.09)/.09
 return abs(side_point(x,top(x)-.09,1)[2])*(max(0.,1-h*h)**(1/6))

def hull_end_x(z,y,front):
 end=-2.1655 if front else 2.1655
 if abs(z)<=hull_width_at_height(end,y):return end
 lo,hi=(-2.1655,-.5) if front else(.8,2.1655)
 for _ in range(24):
  mid=(lo+hi)/2;w=hull_width_at_height(mid,y)
  if (w<abs(z))==front:lo=mid
  else:hi=mid
 return (lo+hi)/2

def frontx(z,y=.75):return hull_end_x(z,y,True)-.008

def rearx(z,y=.82):return hull_end_x(z,y,False)+.008

def endpoly(mat,zy,front,offset):
 # Conform each patch to the actual exterior hull, rather than a planar fan
 # that cuts through the rounded quarter panel between its corner vertices.
 def pt(q):
  z,y=q;x=(frontx(z,y)-offset) if front else(rearx(z,y)+offset)
  return(x,y,z)
 def nn(q):
  z,y=q;e=.0001;fn=frontx if front else rearx
  dxdy=(fn(z,y+e)-fn(z,y-e))/(2*e);dxdz=(fn(z+e,y)-fn(z-e,y))/(2*e)
  return norm((-1,dxdy,dxdz) if front else(1,-dxdy,-dxdz))
 N=6
 for k in range(1,len(zy)-1):
  a,b,c=zy[0],zy[k],zy[k+1]
  def q(i,j):return(a[0]+(b[0]-a[0])*i/N+(c[0]-a[0])*j/N,a[1]+(b[1]-a[1])*i/N+(c[1]-a[1])*j/N)
  for i in range(N):
   for j in range(N-i):
    qs=[q(i,j),q(i+1,j),q(i,j+1)]
    tri(mat,*(pt(v) for v in qs),*(nn(v) for v in qs))
    if i+j<N-1:
     qs=[q(i+1,j),q(i+1,j+1),q(i,j+1)]
     tri(mat,*(pt(v) for v in qs),*(nn(v) for v in qs))

def frontpoly(mat,zy,offset=0):endpoly(mat,zy,True,offset)
def rearpoly(mat,zy,offset=0):endpoly(mat,zy,False,offset)
frontpoly('black_trim',[(-.405,.873),(.405,.873),(.435,.748),(-.435,.748)])
for y in[.787,.825,.867]:tube('chrome',[(frontx(z)-.003,y,z) for z in[-.405,-.20,0,.20,.405]],.007,6)
# Badge oval/shield surround without claiming an exact logo reproduction.
ellipsoid('chrome',(-2.180,.815,0),(.009,.061,.063),24,10)
ellipsoid('black_trim',(-2.188,.816,0),(.004,.050,.051),20,8)
frontpoly('chrome',[(-.032,.852),(.032,.852),(.024,.800),(0,.779),(-.024,.800)])
for sign in[-1,1]:
 shape=[(.345,.847),(.55,.928),(.790,.973),(.795,.790),(.570,.735),(.382,.775)]
 shape=[(sign*z,y-.04) for z,y in shape]
 frontpoly('black_trim',shape)
 centerz=sum(z for z,y in shape)/len(shape);centery=sum(y for z,y in shape)/len(shape)
 lens=[(centerz+(z-centerz)*.94,centery+(y-centery)*.87) for z,y in shape]
 frontpoly('headlamp',lens,.006)
 for zz,rr in[(.61,.069),(.46,.047)]:
  x=frontx(sign*zz,.789)-.008
  ellipsoid('chrome',(x,.789,sign*zz),(.004,rr,rr),20,10)
  ellipsoid('lamp_white',(x-.004,.789,sign*zz),(.002,rr*.65,rr*.65),20,10)
 tube('amber',[(frontx(sign*.765)-.009,.777,sign*.765),(frontx(sign*.760)-.009,.850,sign*.760)],.009,6)
 # Black foglamp pockets, chrome reflector and round lens.
 frontpoly('black_trim',[(sign*.512,.515),(sign*.729,.548),(sign*.780,.335),(sign*.536,.330)])
 zz=sign*.635;x=frontx(zz,.424)-.008
 ellipsoid('chrome',(x,.424,zz),(.004,.063,.063),24,10)
 ellipsoid('headlamp',(x-.004,.424,zz),(.002,.049,.049),24,10)
frontpoly('black_trim',[(-.43,.566),(.43,.566),(.487,.323),(-.487,.323)])
for y in[.35,.39,.43,.47,.51]:
 tube('grille',[(frontx(z)-.010,y,z) for z in[-.43,-.21,0,.21,.43]],.009,5)
for z in[-.33,-.22,-.11,0,.11,.22,.33]:tube('grille',[(frontx(z)-.012,.337,z),(frontx(z)-.012,.546,z)],.004,5)
tube('black_trim',[(frontx(z)-.006,.248,z) for z in[-.72,-.5,0,.5,.72]],.025,8)
# Plate planes are separate batches so the Android renderer can overlay SAGA DASH text.
quad('plate_front',(-2.211,.529,-.25),(-2.211,.529,.25),(-2.211,.641,.25),(-2.211,.641,-.25),(-1,0,0))
# Rear light clusters wrap onto the rear quarter and have inner clear wedges.
for sign in[-1,1]:
 outline=[(.365,.954),(.610,.982),(.793,.941),(.795,.731),(.665,.696),(.472,.756)]
 rearpoly('black_trim',[(sign*z,y-.035) for z,y in outline])
 rearpoly('tail_red',[(sign*(.59+(z-.59)*.95),.805+(y-.84)*.94) for z,y in outline],.014)
 rearpoly('lamp_white',[(sign*.378,.910),(sign*.624,.919),(sign*.554,.826),(sign*.448,.798)],.022)
 # Red segmented lens ribs are actual mesh strips.
 for yy in[.724,.752,.780]:tube('tail_highlight',[(rearx(sign*z,yy)+.020,yy,sign*z) for z in[.58,.65,.73,.78]],.004,5)
 # Curved outer lens follows the rear quarter, rather than a floating flat
 # rectangle. Its upper edge sits below the rounded deck shoulder.
 def quarter_lens(u,v,sign=sign):
  x=1.86+u*.30;y=.735+v*(.17-.035*u)
  return side_point(x,y,sign,.010)
 surface('tail_red',quarter_lens,16,8,reverse=sign<0)
# Chrome boot garnish and badge.
tube('chrome',[(rearx(z)+.004,.858,z) for z in[-.57,-.30,0,.30,.57]],.024,7)
ellipsoid('chrome',(2.190,.862,0),(.011,.052,.052),24,10)
ellipsoid('black_trim',(2.202,.862,0),(.005,.043,.043),20,8)
rearpoly('chrome',[(-.025,.890),(.025,.890),(.018,.855),(0,.835),(-.018,.855)])
quad('plate_rear',(2.201,.575,-.25),(2.201,.687,-.25),(2.201,.687,.25),(2.201,.575,.25),(1,0,0))
# Plate recess outline and camera keyhole.
tube('paint_dark',[(2.192,.558,-.278),(2.192,.705,-.278),(2.192,.705,.278),(2.192,.558,.278),(2.192,.558,-.278)],.005,6)
ellipsoid('black_trim',(2.205,.854,0),(.007,.012,.017),12,6)
rearpoly('black_trim',[(-.73,.370),(-.60,.417),(.60,.417),(.73,.370),(.70,.220),(-.70,.220)])
rearpoly('tail_red',[(-.16,.333),(.16,.333),(.115,.252),(-.115,.252)])
for z in[-.62,-.22,.22,.62]:ellipsoid('black_trim',(rearx(z)+.004,.457,z),(.005,.013,.013),12,6)
# Side fuel filler flap on the owner's right-rear quarter.
pts=[]
for x,y in[(1.55,.70),(1.75,.70),(1.75,.875),(1.55,.875),(1.55,.70)]:pts.append(side_point(x,y,1,.008))
tube('paint_dark',pts,.003,6)
# Wheels: rounded tires, recessed discs, silver eight-way split spokes, four lug nuts.
for axle in AXLES:
 for sign in[-1,1]:
  center=(axle,.30,sign*.783)
  torus('rubber',center,.242,.052,.081,48,10)
  outerz=sign*.869
  # Fine concentric sidewall molding, subtly visible rather than a flat dark disk.
  torus('black_trim',(axle,.30,outerz),.245,.0025,.0025,48,4)
  torus('rubber',(axle,.30,outerz),.205,.005,.004,48,5)
  disk('brake',(axle,.30,sign*.842),.172,sign,48)
  torus('chrome',(axle,.30,sign*.876),.185,.009,.009,48,6)
  torus('wheel_silver',(axle,.30,sign*.855),.173,.009,.019,48,6)
  # 8 spokes divide into paired arms at their outer end, like the owner's rim.
  def spoke(a,r1,r2,wa,wb,aa=0,bb=0):
   pa=(axle+r1*math.cos(a+aa),.30+r1*math.sin(a+aa))
   pb=(axle+r2*math.cos(a+bb),.30+r2*math.sin(a+bb))
   tangent=(-math.sin(a),math.cos(a))
   pts=[(pa[0]+tangent[0]*wa,pa[1]+tangent[1]*wa,sign*.879),(pb[0]+tangent[0]*wb,pb[1]+tangent[1]*wb,sign*.879),(pb[0]-tangent[0]*wb,pb[1]-tangent[1]*wb,sign*.879),(pa[0]-tangent[0]*wa,pa[1]-tangent[1]*wa,sign*.879)]
   face('wheel_silver',pts,(0,0,sign))
   # Darkened side edge catches depth under mobile fixed-function lighting.
   for i in range(4):
    a1=pts[i];b1=pts[(i+1)%4];a2=(a1[0],a1[1],sign*.862);b2=(b1[0],b1[1],sign*.862)
    quad('chrome',a1,a2,b2,b1)
  for i in range(8):
   a=TAU*i/8+.10
   spoke(a,.042,.113,.020,.014)
   spoke(a,.107,.181,.013,.010,0,-.125)
   spoke(a,.107,.181,.013,.010,0,.125)
  disk('wheel_silver',(axle,.30,sign*.885),.051,sign,32)
  disk('black_trim',(axle,.30,sign*.887),.023,sign,24)
  for i in range(4):
   a=TAU*i/4+.35
   ellipsoid('chrome',(axle+.035*math.cos(a),.30+.035*math.sin(a),sign*.889),(.007,.007,.004),10,5)
# Low rear exhaust pipe, separate from the wheels.
ellipsoid('black_trim',(2.107,.216,-.573),(.11,.032,.033),16,8)

# Round the bumper rollover and lower corners as one coherent deformation.
# Lamps, grilles and trim travel with the body so they cannot be buried by the change.
def bumper_warp(p):
 x,y,z=p;t=max(0.,min(1.,(y-.215)/.755))
 blend=max(0.,min(1.,(abs(x)-1.78)/.43));blend=blend*blend*(3-2*blend)
 shift=.045*math.sin(math.pi*t)-.105*(1-t)**2-.040*t**4-.19*(abs(z)/.85)**3
 return(x+(1 if x>=0 else -1)*blend*shift,y,z*(1-.04*blend*(1-t)**2))
for mesh in M.values():
 vv=mesh['vertices']
 for i in range(0,len(vv),6):
  p=tuple(vv[i:i+3]);n=tuple(vv[i+3:i+6]);columns=[];eps=1e-5
  for axis in range(3):
   a=list(p);b=list(p);a[axis]+=eps;b[axis]-=eps
   columns.append(mul(sub(bumper_warp(a),bumper_warp(b)),.5/eps))
  u,v,w=columns
  transformed=norm(add(add(mul(cross(v,w),n[0]),mul(cross(w,u),n[1])),mul(cross(u,v),n[2])))
  vv[i:i+6]=[*bumper_warp(p),*transformed]

# Orient the final tessellation after the nonlinear bumper deformation.
# Cofactor normals preserve smooth shading, but broad trim triangles can change
# winding when their corners move different distances around the bumper.
for mesh in M.values():
 vv=mesh['vertices']
 for i in range(0,len(vv),18):
  area=cross(sub(vv[i+6:i+9],vv[i:i+3]),sub(vv[i+12:i+15],vv[i:i+3]))
  average=add(add(vv[i+3:i+6],vv[i+9:i+12]),vv[i+15:i+18])
  if dot(area,average)<0:
   vv[i+6:i+18]=vv[i+12:i+18]+vv[i+6:i+12]

# Validate finite coordinates and normals before saving.
ROOT.mkdir(parents=True,exist_ok=True)
meshes=[]
for name,mesh in M.items():
 if not mesh['vertices']:continue
 vv=mesh['vertices']
 assert len(vv)%18==0
 assert all(math.isfinite(x) for x in vv)
 for i in range(0,len(vv),6):
  assert abs(length(vv[i+3:i+6])-1)<1e-5,(name,i,vv[i+3:i+6])
 # Audit the actual float32 stream, not only the higher precision source math.
 packed=struct.unpack('<%df'%len(vv),struct.pack('<%df'%len(vv),*vv))
 for i in range(0,len(packed),18):
  pa,pb,pc=packed[i:i+3],packed[i+6:i+9],packed[i+12:i+15]
  area=cross(sub(pb,pa),sub(pc,pa))
  average=add(add(packed[i+3:i+6],packed[i+9:i+12]),packed[i+15:i+18])
  assert length(area)>1e-12,(name,i,'collapsed float32 triangle')
  assert dot(area,average)>=-1e-10,(name,i,'inverted winding')
 meshes.append((name,mesh))
temporary_mesh=ROOT/'saga-vvt.mesh.tmp'
with temporary_mesh.open('wb') as out:
 out.write(struct.pack('<i',len(meshes)))
 for name,mesh in meshes:
  b=name.encode();out.write(struct.pack('<i',len(b)));out.write(b)
  out.write(struct.pack('<5fi',*mesh['color'],mesh['shininess'],len(mesh['vertices'])//6))
  out.write(struct.pack('<%df'%len(mesh['vertices']),*mesh['vertices']))
temporary_mesh.replace(ROOT/'saga-vvt.mesh')
positions=[mesh['vertices'][i:i+3] for _,mesh in meshes for i in range(0,len(mesh['vertices']),6)]
bounds=[[min(p[i] for p in positions) for i in range(3)],[max(p[i] for p in positions) for i in range(3)]]
stats={'triangles':sum(len(mesh['vertices'])//18 for _,mesh in meshes),'batches':len(meshes),'bounds':bounds,'coordinates':'metres; front -X, up +Y, lateral Z','plate_front':{'x':-2.212,'y':.585,'width':.5,'height':.112},'plate_rear':{'x':2.202,'y':.631,'width':.5,'height':.112}}
(ROOT/'saga-vvt-stats.json').write_text(json.dumps(stats,indent=2)+'\n')
# Optional portable preview data for Blender or a browser viewer.
if ARGS.json:
 ARGS.json.parent.mkdir(parents=True,exist_ok=True)
 ARGS.json.write_text(json.dumps({'meshes':[{'name':name,**mesh} for name,mesh in meshes]},separators=(',',':')))
(ROOT/'README.txt').write_text('''Saga VVT exterior study — handmade geometry for Saga Dash.

This model is an original geometric approximation based on the owner's supplied
photos and 2016-era Proton Saga brochure proportions. It is not manufacturer CAD,
not a photogrammetry reconstruction, and not an exact scale survey of the car.
The shell, wheel arches, curved cabin, lamps, grille, split-spoke wheels, mirrors,
door seams, handles and dark window visors are actual three-dimensional geometry.
No photograph is used as a fake rotatable billboard. The plate contains no owner's
registration; Android overlays SAGA DASH as a neutral plate label.

Generate: python3 tools/build_saga_model.py
Coordinates: front -X, ground Y=0, up +Y, left/right Z; metres.
Body reference dimensions: 4.331 m length, 1.689 m width, 1.491 m roof height;
wheelbase 2.465 m. Mirrors and aerial extend beyond those body dimensions.

saga-vvt.mesh stream (little endian): int32 batch count. Per batch:
int32 UTF-8 name-byte count, name bytes; 4 float32 RGBA; float32 shininess;
int32 vertex count; vertex_count * (float32 x,y,z,nx,ny,nz).
DrawArrays TRIANGLES; materials are opaque. Buffers are immutable and precomputed.
Optional JSON export: python3 tools/build_saga_model.py --json build/saga-model.json
Keep inspection exports outside assets so the APK ships one mesh only.
All mesh normals are normalized and all values are checked for finiteness.
''')
print(json.dumps(stats,indent=2))
