#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${ANDROID_HOME:?}" "${NEXADASH_NEXT_KEYSTORE:?}" "${NEXADASH_NEXT_KS_PASS:?}"
BT="$ANDROID_HOME/build-tools/36.0.0";JAR="$ANDROID_HOME/platforms/android-35/android.jar";OUT="$PWD/build/radio-fixtures"
mkdir -p "$OUT/classes" "$OUT/dex"
mapfile -d '' SOURCES < <(find tools/radio-fixture/src -name '*.java' -print0)
javac --release 8 -cp "$JAR" -d "$OUT/classes" "${SOURCES[@]}"
mapfile -d '' CLASSES < <(find "$OUT/classes" -name '*.class' -print0)
"$BT/d8" --min-api 26 --lib "$JAR" --output "$OUT/dex" "${CLASSES[@]}"
for KIND in internal external;do
 if [ "$KIND" = internal ];then PACKAGE=com.android.fmradio;CLASS=com.android.fmradio.FmtestService;ACTIVITY=com.android.fmradio.FmMainActivity;VERSION=3.1;TARGET=23;else PACKAGE=com.acloud.stub.extradio;CLASS=com.radio.service.RadioService;ACTIVITY=com.acloud.stub.extradio.QtActivity;VERSION=2.3.24;TARGET=21;fi
 cat > "$OUT/$KIND.xml" <<MANIFEST
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="$PACKAGE" android:versionCode="1" android:versionName="$VERSION"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="$TARGET"/><application android:label="NexaDash Radio fixture $KIND" android:allowBackup="false"><service android:name="$CLASS" android:exported="true"/><activity android:name="dev.nexadash.radiofixture.TrapActivity" android:exported="false"/><activity-alias android:name="$ACTIVITY" android:targetActivity="dev.nexadash.radiofixture.TrapActivity" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity-alias><provider android:name="dev.nexadash.radiofixture.Events" android:authorities="$PACKAGE.nexatest" android:exported="true" android:permission="com.shazwan.nexadash.next.permission.CONTROL"/></application></manifest>
MANIFEST
 "$BT/aapt2" link -o "$OUT/$KIND-base.apk" -I "$JAR" --manifest "$OUT/$KIND.xml"
 (cd "$OUT/dex" && zip -q "$OUT/$KIND-base.apk" classes.dex)
 "$BT/zipalign" -f 4 "$OUT/$KIND-base.apk" "$OUT/$KIND-aligned.apk"
 "$BT/apksigner" sign --ks "$NEXADASH_NEXT_KEYSTORE" --ks-key-alias webtest --ks-pass env:NEXADASH_NEXT_KS_PASS --out "$OUT/$KIND.apk" "$OUT/$KIND-aligned.apk"
done
