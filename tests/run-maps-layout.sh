#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?}" "${DEVICE:?Select an emulator/test device explicitly}" "${NEXADASH_NEXT_KEYSTORE:?}" "${NEXADASH_NEXT_KS_PASS:?}"
BT="$ANDROID_HOME/build-tools/36.0.0";JAR="$ANDROID_HOME/platforms/android-35/android.jar";OUT="$PWD/build/maps-layout-checks"
mkdir -p "$OUT/classes" "$OUT/dex"
bash tools/native-test/build.sh
cat > "$OUT/AndroidManifest.xml" <<'MANIFEST'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.shazwan.nexadash.mapslayoutchecks">
<uses-sdk android:minSdkVersion="26" android:targetSdkVersion="27"/>
<uses-permission android:name="com.shazwan.nexadash.next.permission.CONTROL"/>
<application android:label="NexaDash map checks">

</application>
<instrumentation android:name="com.shazwan.sagadash.MapsLayoutChecks" android:targetPackage="com.shazwan.nexadash.next"/>
</manifest>
MANIFEST
"$BT/aapt2" link -o "$OUT/base.apk" -I "$JAR" --manifest "$OUT/AndroidManifest.xml"
javac --release 8 -encoding UTF-8 -cp "$JAR:build/next/classes" -d "$OUT/classes" tests/MapsLayoutChecks.java
"$BT/d8" --min-api 26 --lib "$JAR" --classpath build/next/classes --output "$OUT/dex" "$OUT/classes/com/shazwan/sagadash/"*.class
(cd "$OUT/dex" && zip -q "$OUT/base.apk" classes.dex)
"$BT/zipalign" -f 4 "$OUT/base.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks "$NEXADASH_NEXT_KEYSTORE" --ks-key-alias webtest --ks-pass env:NEXADASH_NEXT_KS_PASS --out "$OUT/checks.apk" "$OUT/aligned.apk"
adb -s "$DEVICE" install -r "${HOST_APK:-build/next/NexaDash-Next-1.9.apk}"
adb -s "$DEVICE" install -r build/native-tests/NexaDash-Native-Test-a.apk
adb -s "$DEVICE" install -r build/native-tests/NexaDash-Native-Test-b.apk
adb -s "$DEVICE" install -r "$OUT/checks.apk"
adb -s "$DEVICE" shell am instrument -w com.shazwan.nexadash.mapslayoutchecks/com.shazwan.sagadash.MapsLayoutChecks | tee "$OUT/result.txt"
rg -q '^PASS: [1-9][0-9]* Maps-layout host assertions' "$OUT/result.txt"
