#!/bin/sh
set -eu
cd "$(dirname "$0")"
cat YouTube-V58-native1.apk.part1 YouTube-V58-native1.apk.part2 YouTube-V58-native1.apk.part3 > YouTube-V58-native1.apk
sha256sum YouTube-V58-native1.apk
