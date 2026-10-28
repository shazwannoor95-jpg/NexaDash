#!/bin/bash
set -euo pipefail
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"
BT="$ANDROID_HOME/build-tools/36.0.0"
JAR="$ANDROID_HOME/platforms/android-35/android.jar"
: "${KLIKDASH_KS_PASS:?Set the password for your signing key}"
: "${PROBE_KEYSTORE:?Set the path to a signing key for both APKs}"
cd "$(dirname "$0")"
for part in target host; do
  mkdir -p "$part/out/classes" "$part/out/dex"
  if [ -d "$part/res" ]; then
    "$BT/aapt2" compile --dir "$part/res" -o "$part/out/res.zip"
    "$BT/aapt2" link -o "$part/out/base.apk" -I "$JAR" --manifest "$part/AndroidManifest.xml" "$part/out/res.zip"
  else
    "$BT/aapt2" link -o "$part/out/base.apk" -I "$JAR" --manifest "$part/AndroidManifest.xml"
  fi
  javac --release 8 -classpath "$JAR" -d "$part/out/classes" $(rg --files "$part/src" -g '*.java')
  "$BT/d8" --min-api 26 --lib "$JAR" --output "$part/out/dex" $(find "$part/out/classes" -name '*.class')
  cp "$part/out/base.apk" "$part/out/unaligned.apk"
  (cd "$part/out/dex" && zip -q ../unaligned.apk classes.dex)
  "$BT/zipalign" -f 4 "$part/out/unaligned.apk" "$part/out/aligned.apk"
  "$BT/apksigner" sign --ks "$PROBE_KEYSTORE" --ks-pass env:KLIKDASH_KS_PASS --out "$part/out/probe.apk" "$part/out/aligned.apk"
  "$BT/apksigner" verify "$part/out/probe.apk"
done
