#!/bin/bash
# Separate emulator experiment. Never bundles V58 in the main launcher.
set -euo pipefail
: "${ANDROID_HOME:?Set ANDROID_HOME}"
: "${APKTOOL_JAR:?Set APKTOOL_JAR to apktool-cli 2.11.1 jar}"
: "${PROBE_KEYSTORE:?Set PROBE_KEYSTORE to a disposable lab signing key}"
: "${KLIKDASH_KS_PASS:?Set KLIKDASH_KS_PASS}"
APKTOOL_JAR=$(realpath "$APKTOOL_JAR")
PROBE_KEYSTORE=$(realpath "$PROBE_KEYSTORE")
INPUT_APK=$(realpath "${1:?Pass the original owner-supplied V58 APK path}")
cd "$(dirname "$0")"
BT="$ANDROID_HOME/build-tools/36.0.0"
JAR="$ANDROID_HOME/platforms/android-35/android.jar"
mkdir -p out/bridge-classes out/bridge-dex out/framework out/host-classes out/host-dex
# Pin the reviewed base; do not silently modify an unrelated mod/version.
EXPECTED=a0770d82fd2afcacf6872f15d10844bc570c6ceb6d2035e8bf14968b7f21324c
ACTUAL=$(sha256sum "$INPUT_APK" | cut -d ' ' -f1)
[ "$ACTUAL" = "$EXPECTED" ] || { echo 'Input hash is not the reviewed V58 APK.' >&2; exit 1; }
java -jar "$APKTOOL_JAR" d -s -f -p out/framework "$INPUT_APK" -o out/decoded
python3 patch-manifest.py out/decoded/AndroidManifest.xml
javac --release 8 -classpath "$JAR" -d out/bridge-classes $(find bridge-src -name '*.java')
"$BT/d8" --min-api 29 --lib "$JAR" --output out/bridge-dex $(find out/bridge-classes -name '*.class')
[ ! -e out/decoded/classes11.dex ] || { echo 'Unexpected DEX layout.' >&2; exit 1; }
cp out/bridge-dex/classes.dex out/decoded/classes11.dex
java -jar "$APKTOOL_JAR" b -p out/framework out/decoded -o out/target-unsigned.apk
"$BT/zipalign" -f -p 4 out/target-unsigned.apk out/target-aligned.apk
"$BT/apksigner" sign --ks "$PROBE_KEYSTORE" --ks-pass env:KLIKDASH_KS_PASS --out out/target.apk out/target-aligned.apk
"$BT/aapt2" link -o out/host-base.apk -I "$JAR" --manifest host/AndroidManifest.xml
javac --release 8 -classpath "$JAR" -d out/host-classes $(find host/src -name '*.java')
"$BT/d8" --min-api 26 --lib "$JAR" --output out/host-dex $(find out/host-classes -name '*.class')
cp out/host-base.apk out/host-unsigned.apk
(cd out/host-dex && zip -q ../host-unsigned.apk classes.dex)
"$BT/zipalign" -f 4 out/host-unsigned.apk out/host-aligned.apk
"$BT/apksigner" sign --ks "$PROBE_KEYSTORE" --ks-pass env:KLIKDASH_KS_PASS --out out/host.apk out/host-aligned.apk
"$BT/apksigner" verify out/target.apk
"$BT/apksigner" verify out/host.apk
