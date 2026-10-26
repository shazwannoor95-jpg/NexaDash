Saga VVT exterior study — handmade geometry for Saga Dash.

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
