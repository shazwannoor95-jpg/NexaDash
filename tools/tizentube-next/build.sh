#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${ANDROID_HOME:?}" "${APKEDITOR_SOURCE:?Set pinned APKEditor source path}" "${NEXADASH_NEXT_KEYSTORE:?}" "${NEXADASH_NEXT_KS_PASS:?}"
INPUT=$(realpath "${1:?Pass the reviewed TizenTube Native Lab ARM APK}")
OUT="$PWD/build/tizentube-next";BT="$ANDROID_HOME/build-tools/36.0.0";JAR="$ANDROID_HOME/platforms/android-35/android.jar"
python3 - "$INPUT" <<'PY'
from pathlib import Path
import sys,hashlib
assert hashlib.sha256(Path(sys.argv[1]).read_bytes()).hexdigest()=='097530c5403f1ebba0db24e2cfcc4b3b33d3ca55ea0724364ed57e976185ba40','Only the reviewed ARM export is supported'
PY
[ "$(git -C "$APKEDITOR_SOURCE" rev-parse HEAD)" = 5d94bc08565e30e3e3bb7accbd83138e4d6a8e45 ]
mkdir -p "$OUT/classes"
javac --release 8 -cp "$APKEDITOR_SOURCE/libs/ARSCLib.jar:$JAR" -d "$OUT/classes" tools/tizentube-next/PatchTizen.java
java -cp "$OUT/classes:$APKEDITOR_SOURCE/libs/*" PatchTizen "$INPUT" "$OUT/patched.apk"
"$BT/zipalign" -f -p 4 "$OUT/patched.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks "$NEXADASH_NEXT_KEYSTORE" --ks-key-alias webtest --ks-pass env:NEXADASH_NEXT_KS_PASS --out "$OUT/TizenTube-NexaDash-Next-arm32.apk" "$OUT/aligned.apk"
"$BT/apksigner" verify --min-sdk-version 27 --max-sdk-version 27 --print-certs "$OUT/TizenTube-NexaDash-Next-arm32.apk"
"$BT/zipalign" -c -p 4 "$OUT/TizenTube-NexaDash-Next-arm32.apk"
python3 - "$INPUT" "$OUT/TizenTube-NexaDash-Next-arm32.apk" <<'PY'
import sys,zipfile,hashlib,pathlib
with zipfile.ZipFile(sys.argv[1]) as src,zipfile.ZipFile(sys.argv[2]) as dst:
 assert dst.testzip() is None
 names=[n for n in src.namelist() if n.endswith(('.dex','.so'))]
 assert len([n for n in names if n.endswith('.dex')])==4
 assert len([n for n in names if n.endswith('.so')])==2
 assert all(src.read(n)==dst.read(n) for n in names),'Payload drift'
p=pathlib.Path(sys.argv[2]);print('Preserved 4 DEX and 2 ARM32 libraries; bytes',p.stat().st_size,'SHA256',hashlib.sha256(p.read_bytes()).hexdigest())
PY
