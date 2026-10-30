#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${ANDROID_HOME:?Set ANDROID_HOME}" "${NEXADASH_NEXT_KEYSTORE:?Set private signing keystore}" "${NEXADASH_NEXT_KS_PASS:?Set private keystore password}"
BT="$ANDROID_HOME/build-tools/36.0.0"; JAR="$ANDROID_HOME/platforms/android-35/android.jar"; OUT="$PWD/build/next"
rm -rf "$OUT"
mkdir -p "$OUT"/{classes,dex,gen,assets}
python3 tools/next/manifest.py "$OUT/AndroidManifest.xml"
"$BT/aapt2" compile --dir res -o "$OUT/res.zip"
cp -R assets/. "$OUT/assets/"
rm -f "$OUT/assets/quran.txt" "$OUT/assets/quran-en.txt"
"$BT/aapt2" link -o "$OUT/base.apk" -I "$JAR" --manifest "$OUT/AndroidManifest.xml" -A "$OUT/assets" -0 ttf --rename-manifest-package com.shazwan.nexadash.next --min-sdk-version 24 --target-sdk-version 29 --replace-version --version-code 5 --version-name 1.4-next5 --java "$OUT/gen" "$OUT/res.zip"
mapfile -d '' SOURCES < <(find src "$OUT/gen" -name '*.java' -print0)
javac --release 8 -encoding UTF-8 -classpath "$JAR" -d "$OUT/classes" "${SOURCES[@]}"
mapfile -d '' CLASSES < <(find "$OUT/classes" -name '*.class' -print0)
"$BT/d8" --release --min-api 24 --lib "$JAR" --output "$OUT/dex" "${CLASSES[@]}"
cp "$OUT/base.apk" "$OUT/unaligned.apk"
(cd "$OUT/dex" && zip -q "$OUT/unaligned.apk" classes.dex)
"$BT/zipalign" -f -p 4 "$OUT/unaligned.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks "$NEXADASH_NEXT_KEYSTORE" --ks-key-alias webtest --ks-pass env:NEXADASH_NEXT_KS_PASS --out "$OUT/NexaDash-Next-1.4.apk" "$OUT/aligned.apk"
"$BT/apksigner" verify --print-certs "$OUT/NexaDash-Next-1.4.apk"
"$BT/zipalign" -c 4 "$OUT/NexaDash-Next-1.4.apk"

python3 tests/check_no_quran.py "$OUT/NexaDash-Next-1.4.apk"
