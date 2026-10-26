# Transparent Saga video

The optional ten-second car video has its studio background removed, with **NEXA** retained.
It is a recorded clip, not a freely rotatable 3D model. Automatic masks can leave small edge or
antenna imperfections; source framing and AI-generated detail changes remain.

The default car display is now **Saga image angles (lightweight)**: eight static rendered angles,
changed by dragging. The old **Original Saga video** and **Interactive 3D model** app choices
are removed. Use the image mode to avoid a video decoder/GLES view while idle.

## Use in the app

1. Open the **Car preview** panel.
2. Tap the vehicle-view button with **▾** above the car.
3. Select **Transparent Saga video** and tap the preview to Play/Pause.
4. Select **Saga image angles (lightweight)** to return to the static angle viewer.

The preview is muted, does not auto-loop, creates no MediaSession and requests no audio focus.
Its decoder is released when the panel/app is left. Physical TEST9278B graphics-driver and
performance validation is still required.

## Export and Android transport

| File | Purpose |
| --- | --- |
| `docs/media/saga-transparent.webm` | Standalone VP9 WebM with real alpha, 960×540, 24fps, ten seconds, no audio. Requires a player/editor that supports WebM alpha. |
| `docs/media/saga-transparent.png` | Transparent opening-frame preview. |
| `docs/media/saga-transparent-source.json` | Source/model hashes and generation notes. |
| `res/raw/saga_showroom_alpha.mp4` | Internal Android H.264 RGB/matte transport, 1280×360, no audio. Left half is color; right half is opacity. It is not a normal alpha MP4 for external playback. |

Android MediaPlayer does not reliably retain VP9 alpha in a normal video view. Saga Dash decodes
an ordinary H.264 color/matte stream, then combines its halves into premultiplied RGBA in GLES2.
Imported ordinary videos remain opaque; importing does not perform segmentation on the device.

## Windows update

Open a fresh copy of the current project ZIP as described in the
[Windows guide](WINDOWS_ANDROID_STUDIO_MS.md#2-buka-projek-terkini). Copying new files onto an old
source tree can leave removed mesh classes/assets behind. Keep your own signing key outside
project folders and use the same key for future updates.

## Regenerate the mask (optional)

This is an offline build-time step, not an APK requirement. Use Python with `onnxruntime`,
`opencv-python-headless`, `numpy` and `pillow`, plus a separately downloaded
[U2Net ONNX model](https://github.com/danielgatis/rembg/releases/download/v0.0.0/u2net.onnx).
Model weights and Python/ONNX runtime are not shipped in the app.

```sh
python3 tools/matte_saga_video.py --input car.mp4 --model u2net.onnx --output build/car-cutout
ffmpeg -framerate 24 -i build/car-cutout/packed/%04d.png -c:v libx264 -profile:v baseline -level:v 3.1 -pix_fmt yuv420p -crf 16 -g 12 -an -movflags +faststart res/raw/saga_showroom_alpha.mp4
ffmpeg -framerate 24 -i build/car-cutout/rgba/%04d.png -c:v libvpx-vp9 -pix_fmt yuva420p -auto-alt-ref 0 -crf 22 -b:v 0 -an docs/media/saga-transparent.webm
```

The tool keeps the main car component, refines edges and blends flow-aligned neighbouring masks.
It does not reconstruct a 3D mesh or redraw the number plate. For current build/test and resource
results, see [PERSONAL_EDITION.md](PERSONAL_EDITION.md).
