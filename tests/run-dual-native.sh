#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?}" "${DEVICE:?Select an emulator/test device explicitly}" "${NEXADASH_NEXT_KEYSTORE:?}" "${NEXADASH_NEXT_KS_PASS:?}"
BT="$ANDROID_HOME/build-tools/36.0.0";JAR="$ANDROID_HOME/platforms/android-35/android.jar";OUT="$PWD/build/dual-native-checks"
mkdir -p "$OUT/classes" "$OUT/dex"
cat > "$OUT/AndroidManifest.xml" <<'MANIFEST'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.shazwan.nexadash.dualchecks"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="27"/><application android:label="NexaDash dual checks"/><instrumentation android:name="com.shazwan.sagadash.DualNativeChecks" android:targetPackage="com.shazwan.nexadash.next"/></manifest>
MANIFEST
"$BT/aapt2" link -o "$OUT/base.apk" -I "$JAR" --manifest "$OUT/AndroidManifest.xml"
javac --release 8 -cp "$JAR:build/next/classes" -d "$OUT/classes" tests/DualNativeChecks.java
"$BT/d8" --min-api 26 --lib "$JAR" --classpath build/next/classes --output "$OUT/dex" "$OUT/classes/com/shazwan/sagadash/"*.class
(cd "$OUT/dex" && zip -q "$OUT/base.apk" classes.dex)
"$BT/zipalign" -f 4 "$OUT/base.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks "$NEXADASH_NEXT_KEYSTORE" --ks-key-alias webtest --ks-pass env:NEXADASH_NEXT_KS_PASS --out "$OUT/checks.apk" "$OUT/aligned.apk"
adb -s "$DEVICE" install -r "${HOST_APK:-build/next/NexaDash-Next-1.5.apk}"
adb -s "$DEVICE" install -r build/native-tests/NexaDash-Native-Test-a.apk
adb -s "$DEVICE" install -r build/native-tests/NexaDash-Native-Test-b.apk
adb -s "$DEVICE" install -r "$OUT/checks.apk"
adb -s "$DEVICE" shell am instrument -w com.shazwan.nexadash.dualchecks/com.shazwan.sagadash.DualNativeChecks | tee "$OUT/result.txt"
rg -q '^PASS: [1-9][0-9]* assertions' "$OUT/result.txt"
adb -s "$DEVICE" pull /sdcard/Android/data/com.shazwan.nexadash.next/files/dual-native-api27.png "$OUT/dual-native-api27.png"
