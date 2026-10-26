#!/bin/bash
# Installs the pinned upstream Android app on an explicitly chosen ARM device.
set -euo pipefail
: "${DEVICE:?Set DEVICE to the intended Android head unit serial}"
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"
sdk=$(adb -s "$DEVICE" shell getprop ro.build.version.sdk | tr -d '\r')
case "$sdk" in
  ''|*[!0-9]*) echo "Cannot verify the device's Android API level: $sdk" >&2; exit 1 ;;
esac
if [ "$sdk" -lt 24 ]; then
  echo "TizenTube Cobalt v2.0.3 requires Android 7.0 / API 24 or newer; device API: $sdk" >&2
  exit 1
fi
abi=$(adb -s "$DEVICE" shell getprop ro.product.cpu.abilist | tr -d '\r')
case "$abi" in
  *arm64-v8a*) arch=arm64; hash=4e3df211abb243cb06b06ba3676fe62c3d5b59baa47c1a0050cb180b3493e539 ;;
  *armeabi-v7a*) arch=arm; hash=72a3527f520db3a1b414916ad96481244e451c41e272ae33344ae5e79abb37dd ;;
  *) echo "TizenTube Cobalt v2.0.3 only supplies ARM APKs; device ABI: $abi" >&2; exit 1 ;;
esac
cache=${XDG_CACHE_HOME:-/workspace/.cloud-setup/cache}/tizentube
mkdir -p "$cache"
apk="$cache/cobalt-$arch-2.0.3.apk"
if [ ! -f "$apk" ]; then
  curl -fL "https://github.com/reisxd/TizenTubeCobalt/releases/download/v2.0.3/cobalt-$arch.apk" -o "$apk"
fi
printf '%s  %s\n' "$hash" "$apk" | sha256sum -c -
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify "$apk"
adb -s "$DEVICE" install --no-incremental -r "$apk"
