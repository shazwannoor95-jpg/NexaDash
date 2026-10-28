#!/bin/bash
set -euo pipefail
: "${ANDROID_HOME:?}" "${APKTOOL_JAR:?}" "${PROBE_KEYSTORE:?}" "${KLIKDASH_KS_PASS:?}"
INPUT_APK=$(realpath "${1:?Pass official Cobalt2.0.3 cobalt-arm.apk}")
APKTOOL_JAR=$(realpath "$APKTOOL_JAR");PROBE_KEYSTORE=$(realpath "$PROBE_KEYSTORE")
cd "$(dirname "$0")"
BT="$ANDROID_HOME/build-tools/36.0.0";JAR="$ANDROID_HOME/platforms/android-35/android.jar"
mkdir -p out/framework out/classes out/dex
java -jar "$APKTOOL_JAR" d -s -f -p out/framework "$INPUT_APK" -o out/decoded
python3 patch-cobalt.py out/decoded
javac --release 8 -classpath "$JAR" -d out/classes $(find ../v58-panel-lab/bridge-src -name '*.java')
"$BT/d8" --min-api 24 --lib "$JAR" --output out/dex $(find out/classes -name '*.class')
[ ! -e out/decoded/classes4.dex ] || { echo 'Unexpected base DEX layout' >&2; exit 1; }
cp out/dex/classes.dex out/decoded/classes4.dex
java -jar "$APKTOOL_JAR" b -p out/framework out/decoded -o out/unsigned.apk
"$BT/zipalign" -f -p 4 out/unsigned.apk out/aligned.apk
"$BT/apksigner" sign --ks "$PROBE_KEYSTORE" --ks-pass env:KLIKDASH_KS_PASS --out out/TizenTube-NativeLab-arm.apk out/aligned.apk
"$BT/apksigner" verify out/TizenTube-NativeLab-arm.apk
