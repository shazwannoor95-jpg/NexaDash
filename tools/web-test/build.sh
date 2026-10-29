#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${ANDROID_HOME:?Set ANDROID_HOME}" "${WEB_TEST_KEYSTORE:?Set private keystore path}" "${WEB_TEST_KS_PASS:?Set keystore password}"
BT="$ANDROID_HOME/build-tools/36.0.0"; JAR="$ANDROID_HOME/platforms/android-35/android.jar"
OUT="$PWD/build/web-test"
rm -rf "$OUT"
mkdir -p "$OUT"/{classes,dex,res/drawable,stub/com/shazwan/sagadash}
cp res/drawable/ic_launcher.xml "$OUT/res/drawable/"
# Compile-time color constants only; this stub never enters the APK.
cat > "$OUT/stub/com/shazwan/sagadash/DashboardActivity.java" <<'STUB'
package com.shazwan.sagadash;
final class DashboardActivity {
 static final int TEXT=0xffedf5ff,MUTED=0xff8ea8be,PANEL=0xff102236,LINE=0xff243c51;
}
STUB
javac --release 8 -d "$OUT/stub-classes" "$OUT/stub/com/shazwan/sagadash/DashboardActivity.java"
javac --release 8 -classpath "$JAR:$OUT/stub-classes" -d "$OUT/classes" src/com/shazwan/sagadash/DashWebPlayer.java tools/web-test/src/com/shazwan/sagadash/WebTestActivity.java
"$BT/aapt2" compile --dir "$OUT/res" -o "$OUT/res.zip"
"$BT/aapt2" link -I "$JAR" --manifest tools/web-test/AndroidManifest.xml -o "$OUT/base.apk" "$OUT/res.zip"
mapfile -d '' CLASSES < <(find "$OUT/classes" -name '*.class' -print0)
"$BT/d8" --min-api 24 --lib "$JAR" --output "$OUT/dex" "${CLASSES[@]}"
cp "$OUT/base.apk" "$OUT/unaligned.apk"
(cd "$OUT/dex" && zip -q "$OUT/unaligned.apk" classes.dex)
"$BT/zipalign" -f 4 "$OUT/unaligned.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks "$WEB_TEST_KEYSTORE" --ks-key-alias webtest --ks-pass env:WEB_TEST_KS_PASS --out "$OUT/NexaDash-WebTest-1.0.apk" "$OUT/aligned.apk"
"$BT/apksigner" verify --print-certs "$OUT/NexaDash-WebTest-1.0.apk"
"$BT/zipalign" -c 4 "$OUT/NexaDash-WebTest-1.0.apk"
