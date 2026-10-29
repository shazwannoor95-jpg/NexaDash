#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${ANDROID_HOME:?Set Android SDK path}" "${APKEDITOR_SOURCE:?Set pinned APKEditor source directory}" "${MAPS_NEXT_KEYSTORE:?Set private signing key}" "${MAPS_NEXT_KS_PASS:?Set private key password}"
INPUT=$(realpath "${1:?Pass directory containing original Maps25.32 base and ARM32/en/mdpi splits}")
OUT=$(realpath -m "${2:-build/maps-native}")
EDITOR=$(realpath "$APKEDITOR_SOURCE")
BT="$ANDROID_HOME/build-tools/36.0.0";JAR="$ANDROID_HOME/platforms/android-35/android.jar"
PIN=5d94bc08565e30e3e3bb7accbd83138e4d6a8e45
[ "$(git -C "$EDITOR" rev-parse HEAD)" = "$PIN" ] || { echo 'Use the documented APKEditor commit.' >&2; exit 1; }
case "$OUT/" in "$INPUT/"*) echo 'Output must be outside the input directory.' >&2; exit 1;; esac
[ ! -e "$OUT/maps-merged.apk" ] || { echo 'Choose a fresh output directory for a rebuild.' >&2; exit 1; }
mkdir -p "$OUT"/{editor-classes,patch-classes,bridge-classes,bridge-dex}
python3 - "$INPUT" "$OUT/inputs-before.json" <<'PYHASH'
import pathlib,hashlib,json,sys
p=pathlib.Path(sys.argv[1]);pathlib.Path(sys.argv[2]).write_text(json.dumps({f.name:hashlib.sha256(f.read_bytes()).hexdigest() for f in p.glob('*.apk')},indent=2))
PYHASH
for part in Maps_base.apk Maps_split_config.armeabi_v7a.apk Maps_split_config.en.apk Maps_split_config.mdpi.apk; do
  test -s "$INPUT/$part"
  "$BT/apksigner" verify --min-sdk-version 26 "$INPUT/$part"
done
find "$EDITOR/src/main/java" -name '*.java' > "$OUT/editor-sources.txt"
javac --release 8 -cp "$EDITOR/libs/*" -d "$OUT/editor-classes" @"$OUT/editor-sources.txt"
cp -R "$EDITOR/src/main/resources/." "$OUT/editor-classes/"
java -cp "$OUT/editor-classes:$EDITOR/libs/*" com.reandroid.apkeditor.Main m -i "$INPUT" -o "$OUT/maps-merged.apk"
javac --release 8 -cp "$EDITOR/libs/ARSCLib.jar:$JAR" -d "$OUT/patch-classes" tools/maps-native/PatchMaps.java
java -cp "$OUT/patch-classes:$EDITOR/libs/*" PatchMaps "$OUT/maps-merged.apk" "$OUT/maps-patched.apk"
javac --release 8 -cp "$JAR" -d "$OUT/bridge-classes" tools/v58-panel-lab/bridge-src/com/shazwan/nexapanel/*.java
"$BT/d8" --min-api 26 --lib "$JAR" --output "$OUT/bridge-dex" "$OUT/bridge-classes/com/shazwan/nexapanel/"*.class
python3 - "$OUT" <<'PY'
import pathlib,sys,zipfile
out=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(out/'maps-patched.apk','a') as z:
 assert 'classes10.dex' not in z.namelist(),'Unexpected base DEX layout'
 z.write(out/'bridge-dex/classes.dex','classes10.dex',compress_type=zipfile.ZIP_DEFLATED)
PY
"$BT/zipalign" -f -p 4 "$OUT/maps-patched.apk" "$OUT/maps-aligned.apk"
"$BT/apksigner" sign --ks "$MAPS_NEXT_KEYSTORE" --ks-key-alias webtest --ks-pass env:MAPS_NEXT_KS_PASS --out "$OUT/Maps-NexaDash-Next-arm32.apk" "$OUT/maps-aligned.apk"
"$BT/apksigner" verify --print-certs "$OUT/Maps-NexaDash-Next-arm32.apk"
"$BT/zipalign" -c -p 4 "$OUT/Maps-NexaDash-Next-arm32.apk"
python3 tools/maps-native/verify.py "$INPUT" "$OUT/Maps-NexaDash-Next-arm32.apk" "$OUT/artifact.json"
