#!/usr/bin/env python3
"""Remove the studio background from a user-owned car video with a local U2Net ONNX model.

Build-time tool only: pip install onnxruntime opencv-python-headless numpy pillow
The model is not included in the APK/repository. Download its weights separately.
Outputs RGBA PNGs, Android side-by-side color/matte PNGs and a provenance JSON.
Encode deliverables with ffmpeg; see docs/TRANSPARENT_VIDEO.md.
"""
import argparse, hashlib, json, time
from pathlib import Path
import cv2
import numpy as np
import onnxruntime as ort
from PIL import Image


def guided_mask(rgb, mask):
    guide=cv2.cvtColor(rgb,cv2.COLOR_RGB2GRAY).astype(np.float32)/255
    box=lambda a:cv2.boxFilter(a,-1,(7,7),borderType=cv2.BORDER_REFLECT)
    mean_i,mean_p=box(guide),box(mask)
    variance=box(guide*guide)-mean_i*mean_i
    a=(box(guide*mask)-mean_i*mean_p)/(variance+.0008)
    return np.clip(box(a)*guide+box(mean_p-a*mean_i),0,1)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--input',type=Path,required=True)
    parser.add_argument('--model',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    rgba_dir=args.output/'rgba';packed_dir=args.output/'packed'
    rgba_dir.mkdir(parents=True,exist_ok=True);packed_dir.mkdir(parents=True,exist_ok=True)
    ort.disable_telemetry_events()
    options=ort.SessionOptions();options.intra_op_num_threads=4;options.inter_op_num_threads=1
    session=ort.InferenceSession(str(args.model),sess_options=options,providers=['CPUExecutionProvider'])
    capture=cv2.VideoCapture(str(args.input))
    if not capture.isOpened():raise RuntimeError('Cannot read video')
    fps=capture.get(cv2.CAP_PROP_FPS);count=0;previous_gray=previous_mask=None;started=time.monotonic();fractions=[]
    while True:
        ok,bgr=capture.read()
        if not ok:break
        rgb=cv2.cvtColor(cv2.resize(bgr,(960,540),interpolation=cv2.INTER_AREA),cv2.COLOR_BGR2RGB)
        normalized=cv2.resize(rgb,(320,320),interpolation=cv2.INTER_AREA).astype(np.float32)/255
        normalized=(normalized-np.array([.485,.456,.406],np.float32))/np.array([.229,.224,.225],np.float32)
        predicted=session.run(None,{session.get_inputs()[0].name:normalized.transpose(2,0,1)[None]})[0][0,0]
        mask=cv2.resize(np.clip(predicted,0,1),(960,540),interpolation=cv2.INTER_CUBIC)
        # Reject detached generated callouts and background speckles, keeping the car.
        components,labels,stats,_=cv2.connectedComponentsWithStats((mask>.35).astype(np.uint8),8)
        if components<2:raise RuntimeError(f'No foreground car at frame {count}')
        largest=1+np.argmax(stats[1:,cv2.CC_STAT_AREA])
        region=cv2.dilate((labels==largest).astype(np.uint8),np.ones((9,9),np.uint8))
        mask=guided_mask(rgb,np.clip(mask,0,1)*region)
        mask=np.clip((mask-.045)/.91,0,1)
        # A small inward bias avoids retaining the pale studio border on dark UI.
        mask=.8*mask+.2*cv2.erode(mask,np.ones((3,3),np.uint8))
        gray=cv2.cvtColor(rgb,cv2.COLOR_RGB2GRAY)
        if previous_gray is not None:
            flow=cv2.calcOpticalFlowFarneback(gray,previous_gray,None,.5,3,15,3,5,1.2,0)
            xx,yy=np.meshgrid(np.arange(960,dtype=np.float32),np.arange(540,dtype=np.float32))
            aligned=cv2.remap(previous_mask,xx+flow[:,:,0],yy+flow[:,:,1],cv2.INTER_LINEAR,borderMode=cv2.BORDER_CONSTANT)
            # Blend only close estimates; abrupt real motion does not leave a ghost trail.
            mask=np.where(np.abs(aligned-mask)<.2,.2*aligned+.8*mask,mask)
        mask=np.clip(mask,0,1).astype(np.float32);previous_gray=gray;previous_mask=mask
        fraction=float(np.mean(mask>.5));fractions.append(fraction)
        if not .03<fraction<.9:raise RuntimeError(f'Implausible cutout at frame {count}: {fraction}')
        alpha=np.rint(mask*255).astype(np.uint8)
        Image.fromarray(np.dstack([rgb,alpha])).save(rgba_dir/f'{count:04}.png',compress_level=3)
        color_small=cv2.resize(rgb,(640,360),interpolation=cv2.INTER_AREA)
        alpha_small=cv2.resize(alpha,(640,360),interpolation=cv2.INTER_AREA)
        # Color and matte are ordinary RGB for Android's standard H.264 decoder.
        packed=np.concatenate([color_small,np.repeat(alpha_small[:,:,None],3,axis=2)],axis=1)
        Image.fromarray(packed).save(packed_dir/f'{count:04}.png',compress_level=3)
        count+=1
        if count%24==0:print(f'{count} frames processed in {time.monotonic()-started:.1f}s',flush=True)
    capture.release()
    if count==0:raise RuntimeError('Empty video')
    summary={'frames':count,'fps':fps,'rgba_size':[960,540],'android_packed_size':[1280,360],
             'source_sha256':hashlib.sha256(args.input.read_bytes()).hexdigest(),
             'model_sha256':hashlib.sha256(args.model.read_bytes()).hexdigest(),
             'foreground_fraction_range':[min(fractions),max(fractions)],
             'method':'U2Net segmentation, guided edge filter, gentle erosion and flow-aligned temporal blending',
             'limitation':'Automatic masks may omit fine antenna/edge detail; source framing and generated car detail changes remain.'}
    (args.output/'manifest.json').write_text(json.dumps(summary,indent=2)+'\n')
    print(json.dumps(summary,indent=2),flush=True)

if __name__=='__main__':main()
