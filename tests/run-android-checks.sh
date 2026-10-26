#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?Source the Android/JDK environment first}"
: "${DEVICE:?Set DEVICE to an explicitly selected emulator or test device serial}"
BT="$ANDROID_HOME/build-tools/36.0.0"
JAR="$ANDROID_HOME/platforms/android-35/android.jar"
TEST_BUILD=$(mktemp -d /tmp/saga-checks.XXXXXX)
mkdir -p "$TEST_BUILD/classes" "$TEST_BUILD/dex"
cat > "$TEST_BUILD/AndroidManifest.xml" <<'MANIFEST'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.shazwan.sagadash.checks">
<uses-sdk android:minSdkVersion="24" android:targetSdkVersion="29" />
<application android:label="Saga Dash checks">
<provider android:name="com.shazwan.sagadash.SagaShowroomChecks$VideoProvider" android:authorities="com.shazwan.sagadash.checks.showroom" android:exported="true" />
</application>
<instrumentation android:name="com.shazwan.sagadash.DashboardChecks" android:targetPackage="com.shazwan.sagadash" />
</manifest>
MANIFEST
"$BT/aapt2" link -o "$TEST_BUILD/base.apk" -I "$JAR" --manifest "$TEST_BUILD/AndroidManifest.xml"
javac --release 8 -encoding UTF-8 -classpath "$JAR:build/classes" -d "$TEST_BUILD/classes" tests/DashboardChecks.java tests/PrayerGpsChecks.java tests/SagaAngleChecks.java tests/TrustedClockChecks.java tests/DashShortcutsChecks.java tests/SagaShowroomChecks.java tests/DashLayoutChecks.java tests/DashNavigationChecks.java tests/DashCustomizationChecks.java tests/DashMediaChecks.java
"$BT/d8" --min-api 24 --lib "$JAR" --classpath build/classes --output "$TEST_BUILD/dex" "$TEST_BUILD/classes/com/shazwan/sagadash/"*.class
(cd "$TEST_BUILD/dex" && zip -q "$TEST_BUILD/base.apk" classes.dex)
"$BT/zipalign" -f 4 "$TEST_BUILD/base.apk" "$TEST_BUILD/aligned.apk"
if [ -z "${KLIKDASH_KS_PASS:-}" ] && [ -f /workspace/.cloud-setup/dev-key-password ]; then
  export KLIKDASH_KS_PASS="$(cat /workspace/.cloud-setup/dev-key-password)"
fi
"$BT/apksigner" sign --ks klikdash.keystore --ks-pass env:KLIKDASH_KS_PASS --out "$TEST_BUILD/checks.apk" "$TEST_BUILD/aligned.apk"
adb -s "$DEVICE" install --no-incremental -r build/klikdash.apk
adb -s "$DEVICE" install --no-incremental -r "$TEST_BUILD/checks.apk"
adb -s "$DEVICE" shell cmd notification allow_listener com.shazwan.sagadash/.DashMediaAccess
adb -s "$DEVICE" shell am instrument -w com.shazwan.sagadash.checks/com.shazwan.sagadash.DashboardChecks | tee "$TEST_BUILD/result.txt"
rg -q '^PASS: [1-9][0-9]* assertions' "$TEST_BUILD/result.txt"
