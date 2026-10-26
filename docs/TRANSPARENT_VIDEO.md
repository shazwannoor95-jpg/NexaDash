# Transparent Saga video

The supplied ten-second Saga clip has been segmented into a car cutout. NEXA is retained.
This remains a recorded video, not a freely rotatable 3D model. Automatic segmentation can leave
small edge/antenna imperfections; changes of scale, framing and car details already in the
AI-generated recording remain.

## Files

- `docs/media/saga-transparent.webm`: standalone VP9 WebM with a real alpha channel, 960×540,
  24fps, ten seconds, no audio. Use a player/editor that supports WebM alpha; an unsupported player
  may show black behind it or ignore transparency.
- `docs/media/saga-transparent.png`: transparent opening-frame preview.
- `docs/media/saga-transparent-source.json`: source/model hashes and generation details.
- `res/raw/saga_showroom_alpha.mp4`: Android transport, H.264 Baseline, 1280×360 at24fps,
  no audio. Left half is RGB640×360; right half is its grayscale opacity mask. It is intentionally
  not a normal alpha MP4. Saga Dash's GLES shader combines the halves into premultiplied RGBA.
- `res/raw/saga_showroom.mp4`: original studio-background video remains available.

Android's regular MediaPlayer does not reliably expose VP9-alpha to a normal video view.
The app therefore decodes an ordinary H.264 RGB/matte video, then restores alpha in GLES2.
An RGBA translucent SurfaceView reveals the dashboard beneath it. The renderer fits the original
16:9 car view, draws only on video frame updates, starts paused, is muted and releases the
player when paused/hidden. Imported ordinary videos are drawn normally with opacity1; choosing
a file does not automatically segment that file.

## Windows update

1. Stop the app in Android Studio.
2. Download/extract the current `saga-dash-android-studio` branch ZIP to another folder.
3. Copy both `src` and `res` from that ZIP into your existing project, replacing matching files.
   Keep your own signing key, `.idea` and `local.properties`.
4. Click Run.
5. At the top of the vehicle card, tap **3D ▾**, **Video ▾** or **Cutout ▾**, then choose
   **Transparent Saga video**.
6. Tap the car to play/pause. The same menu offers **Original Saga video**, **Interactive 3D model**,
   **Choose car video…** and an imported video if one has previously been saved.

A new install defaults to the transparent preview. Existing 3D/video mode choices are retained;
select the transparent option explicitly if needed. The bundled transparent option is independent
of any private video imported earlier. Physical TEST9278B driver/performance tests remain deferred.

## Regenerate the mask (optional; not needed to build the APK)

Use a Python environment with `onnxruntime`, `opencv-python-headless`, `numpy` and `pillow`.
Download the U2Net ONNX model separately from:
https://github.com/danielgatis/rembg/releases/download/v0.0.0/u2net.onnx
The model is used offline at build time; its weights/runtime are not shipped in the app.

```sh
python3 tools/matte_saga_video.py --input car.mp4 --model u2net.onnx --output build/car-cutout
ffmpeg -framerate 24 -i build/car-cutout/packed/%04d.png -c:v libx264 -profile:v baseline -level:v 3.1 -pix_fmt yuv420p -crf 16 -g 12 -an -movflags +faststart res/raw/saga_showroom_alpha.mp4
ffmpeg -framerate 24 -i build/car-cutout/rgba/%04d.png -c:v libvpx-vp9 -pix_fmt yuva420p -auto-alt-ref 0 -crf 22 -b:v 0 -an docs/media/saga-transparent.webm
```

The mask tool uses U2Net semantic foreground prediction, keeps the main car component, refines
edges with a guided filter and gently blends flow-aligned neighbouring masks to reduce jitter.
It does not generate a new car, redraw the plate or convert the recording into a 3D mesh.


## Validation

Gradle debug build and348 emulator assertions passed, including transparent/original/imported
source selection, decoder play/pause/release, and existing prayer/model/media behavior.
The standalone WebM was decoded with libvpx and verified to contain actual alpha0..255.
On the1024×600 Android emulator, all four video corners matched the card RGB(16,34,54) exactly,
and the settings dialog correctly covered the video layer. This is not physical head-unit testing.

![Transparent video in Android](img/saga-transparent-android.png)
