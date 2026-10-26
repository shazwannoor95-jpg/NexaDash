"""Blender-only review tool: render the actual Android mesh, not a separate illustration.
Run: blender --background --python tools/render_saga_preview.py -- <mesh> <output-directory>
"""
import sys, struct, math
from pathlib import Path
import bpy
from mathutils import Vector

args=sys.argv[sys.argv.index('--')+1:]
source=Path(args[0]);out=Path(args[1]);out.mkdir(parents=True,exist_ok=True)
data=memoryview(source.read_bytes());offset=0

def integer():
    global offset
    n=struct.unpack_from('<i',data,offset)[0];offset+=4;return n

def floats(n):
    global offset
    v=struct.unpack_from('<'+'f'*n,data,offset);offset+=4*n;return v

bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
model_objects=[]
parts=integer()
for part in range(parts):
    length=integer();name=bytes(data[offset:offset+length]).decode();offset+=length
    rgba=floats(4);shine=floats(1)[0];count=integer();vertices=[];normals=[]
    for i in range(count):
        x,y,z,nx,ny,nz=floats(6);vertices.append((x,-z,y));normals.append((nx,-nz,ny))
    mesh=bpy.data.meshes.new(name);mesh.from_pydata(vertices,[],[(i,i+1,i+2) for i in range(0,count,3)]);mesh.update()
    # Split normals are identical to the asset rendered on Android.
    mesh.normals_split_custom_set_from_vertices(normals)
    for p in mesh.polygons:p.use_smooth=True
    obj=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(obj);model_objects.append(obj)
    mat=bpy.data.materials.new(name);mat.use_nodes=True;p=mat.node_tree.nodes.get('Principled BSDF')
    p.inputs['Base Color'].default_value=rgba;p.inputs['Roughness'].default_value=max(.15,1-math.sqrt(shine/128)*.82)
    p.inputs['Metallic'].default_value=.55 if ('chrome' in name or 'rim' in name or 'paint' in name) else .05
    mesh.materials.append(mat)
assert offset==len(data),'Unexpected trailing mesh bytes'
# Studio floor and lighting, deliberately independent from model geometry.
bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.016));floor=bpy.context.object
mat=bpy.data.materials.new('Studio floor');mat.diffuse_color=(.024,.039,.06,1);floor.data.materials.append(mat)
world=bpy.context.scene.world;world.color=(.15,.15,.15)
for name,loc,power,size in [('Key',(-3,-4,7),1600,5),('Softbox',(3,3,5),1300,4),('Fill',(-4,3,3),700,3)]:
    light=bpy.data.lights.new(name,'AREA');light.energy=power;light.shape='DISK';light.size=size
    obj=bpy.data.objects.new(name,light);bpy.context.collection.objects.link(obj);obj.location=loc;obj.rotation_euler=(Vector((0,0,.6))-obj.location).to_track_quat('-Z','Y').to_euler()
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=24;scene.cycles.use_denoising=False
scene.render.threads_mode='FIXED';scene.render.threads=8;scene.render.resolution_x=1000;scene.render.resolution_y=650;scene.render.resolution_percentage=100
scene.view_settings.view_transform='AgX';scene.render.image_settings.file_format='PNG'
cam=bpy.data.cameras.new('Review camera');obj=bpy.data.objects.new('Review camera',cam);bpy.context.collection.objects.link(obj);scene.camera=obj;cam.type='ORTHO';cam.ortho_scale=5.4
views={'front-three-quarter':(-6,-5,2.8),'rear-three-quarter':(6,5,2.8),'side':(0,-7,2.1),'front':(-7,0,1.7),'rear':(7,0,1.7)}
for name,loc in views.items():
    obj.location=loc;obj.rotation_euler=(Vector((0,0,.65))-obj.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath=str(out/(name+'.png'));bpy.ops.render.render(write_still=True)
bpy.ops.wm.save_as_mainfile(filepath=str(out/'saga-vvt-review.blend'))
bpy.ops.object.select_all(action='DESELECT')
for part in model_objects:part.select_set(True)
bpy.ops.export_scene.gltf(filepath=str(out/'saga-vvt.glb'),export_format='GLB',use_selection=True,export_cameras=False,export_lights=False)
