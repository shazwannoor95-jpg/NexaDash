#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?}" "${DEVICE:?Select the disposable emulator explicitly}" "${NEXADASH_NEXT_KEYSTORE:?}" "${NEXADASH_NEXT_KS_PASS:?}"
# These doubles deliberately use real OEM package names. Never install them on a physical head unit.
[[ "$DEVICE" == emulator-* ]] || { echo 'Radio fixtures require a disposable emulator serial'; exit 1; }
[[ "$(adb -s "$DEVICE" shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]] || { echo 'Refusing to install firmware doubles outside an emulator'; exit 1; }
BT="$ANDROID_HOME/build-tools/36.0.0";JAR="$ANDROID_HOME/platforms/android-35/android.jar";OUT="$PWD/build/radio-panel-checks"
mkdir -p "$OUT"
for PACKAGE in com.android.fmradio com.acloud.stub.extradio;do
 EXISTING=$(adb -s "$DEVICE" shell pm path "$PACKAGE" | tr -d '\r' || true)
 if [ -n "$EXISTING" ];then
  [[ "$EXISTING" == package:/* && "$EXISTING" != *$'\n'* ]] || { echo 'Unexpected existing firmware package layout'; exit 1; }
  adb -s "$DEVICE" pull "${EXISTING#package:}" "$OUT/existing.apk" >/dev/null
  CERT=$("$BT/apksigner" verify --print-certs "$OUT/existing.apk")
  [[ "$CERT" == *'SHA-256 digest: fa2d67d42fd40612e025c3f87b6a2f7ca9fc48bf613fca9f7a6cdcfcbfc5c752'* ]] || { echo 'Refusing to replace a non-fixture Radio package'; exit 1; }
 fi
done
bash tools/radio-fixture/build.sh
rm -rf "$OUT/classes" "$OUT/dex"
mkdir -p "$OUT/classes" "$OUT/dex"
cat > "$OUT/AndroidManifest.xml" <<'MANIFEST'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.shazwan.nexadash.fmchecks"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="27"/><uses-permission android:name="com.shazwan.nexadash.next.permission.CONTROL"/><application android:label="NexaDash FM checks"/><instrumentation android:name="com.shazwan.sagadash.RadioPanelChecks" android:targetPackage="com.shazwan.nexadash.next"/></manifest>
MANIFEST
"$BT/aapt2" link -o "$OUT/base.apk" -I "$JAR" --manifest "$OUT/AndroidManifest.xml"
javac --release 8 -encoding UTF-8 -cp "$JAR:build/next/classes" -d "$OUT/classes" tests/RadioPanelChecks.java
mapfile -d '' CLASSES < <(find "$OUT/classes" -name '*.class' -print0)
"$BT/d8" --min-api 26 --lib "$JAR" --classpath build/next/classes --output "$OUT/dex" "${CLASSES[@]}"
(cd "$OUT/dex" && zip -q "$OUT/base.apk" classes.dex)
"$BT/zipalign" -f 4 "$OUT/base.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks "$NEXADASH_NEXT_KEYSTORE" --ks-key-alias webtest --ks-pass env:NEXADASH_NEXT_KS_PASS --out "$OUT/checks.apk" "$OUT/aligned.apk"
cleanup(){ adb -s "$DEVICE" uninstall com.android.fmradio >/dev/null 2>&1 || true; adb -s "$DEVICE" uninstall com.acloud.stub.extradio >/dev/null 2>&1 || true; }
trap cleanup EXIT
adb -s "$DEVICE" install -r "${HOST_APK:-build/next/NexaDash-Next-1.8.apk}"
adb -s "$DEVICE" install -r build/radio-fixtures/internal.apk
adb -s "$DEVICE" install -r build/radio-fixtures/external.apk
adb -s "$DEVICE" install -r "$OUT/checks.apk"
adb -s "$DEVICE" shell am instrument -w com.shazwan.nexadash.fmchecks/com.shazwan.sagadash.RadioPanelChecks | tee "$OUT/result.txt"
rg -q '^PASS: [1-9][0-9]* Radio panel API27 assertions' "$OUT/result.txt"
adb -s "$DEVICE" pull /sdcard/Android/data/com.shazwan.nexadash.next/files/fm-panel-api27.png "$OUT/fm-panel-api27.png"
