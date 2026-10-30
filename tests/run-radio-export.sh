#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?}" "${DEVICE:?Select an emulator/test device explicitly}" "${NEXADASH_NEXT_KEYSTORE:?}" "${NEXADASH_NEXT_KS_PASS:?}"
BT="$ANDROID_HOME/build-tools/36.0.0";JAR="$ANDROID_HOME/platforms/android-35/android.jar";OUT="$PWD/build/radio-export-checks"
mkdir -p "$OUT/classes" "$OUT/dex"
cat > "$OUT/AndroidManifest.xml" <<'MANIFEST'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.shazwan.nexadash.radiochecks">
<uses-sdk android:minSdkVersion="26" android:targetSdkVersion="27"/>
<uses-permission android:name="com.shazwan.nexadash.next.permission.CONTROL"/>
<application android:label="NexaDash Radio export checks">
<provider android:name="com.shazwan.sagadash.RadioExportChecks$FilesProvider" android:authorities="com.shazwan.nexadash.radiochecks.files" android:exported="true" android:permission="com.shazwan.nexadash.next.permission.CONTROL"/>
</application>
<instrumentation android:name="com.shazwan.sagadash.RadioExportChecks" android:targetPackage="com.shazwan.nexadash.next"/>
</manifest>
MANIFEST
"$BT/aapt2" link -o "$OUT/base.apk" -I "$JAR" --manifest "$OUT/AndroidManifest.xml"
javac --release 8 -encoding UTF-8 -cp "$JAR:build/next/classes" -d "$OUT/classes" tests/RadioExportChecks.java
"$BT/d8" --min-api 26 --lib "$JAR" --classpath build/next/classes --output "$OUT/dex" "$OUT/classes/com/shazwan/sagadash/"*.class
(cd "$OUT/dex" && zip -q "$OUT/base.apk" classes.dex)
"$BT/zipalign" -f 4 "$OUT/base.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks "$NEXADASH_NEXT_KEYSTORE" --ks-key-alias webtest --ks-pass env:NEXADASH_NEXT_KS_PASS --out "$OUT/checks.apk" "$OUT/aligned.apk"
adb -s "$DEVICE" install -r "${HOST_APK:-build/next/NexaDash-Next-1.8.apk}"
adb -s "$DEVICE" install -r "$OUT/checks.apk"
adb -s "$DEVICE" shell am instrument -w com.shazwan.nexadash.radiochecks/com.shazwan.sagadash.RadioExportChecks | tee "$OUT/result.txt"
rg -q '^PASS: [1-9][0-9]* Radio export API27 assertions' "$OUT/result.txt"
