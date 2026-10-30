#!/usr/bin/env python3
"""Reject removed Quran-reader classes/data in an actual built APK (no Android runtime needed)."""
import argparse
from pathlib import Path
import re
import struct
import zipfile


def class_descriptors(dex):
    if not dex.startswith(b"dex\n"):
        raise ValueError("Expected a standard DEX")
    def u32(offset):
        return struct.unpack_from("<I", dex, offset)[0]
    strings = []
    for index in range(u32(56)):
        offset = u32(u32(60) + index * 4)
        while dex[offset] & 128:
            offset += 1
        offset += 1
        strings.append(dex[offset:dex.index(0, offset)])
    types = [strings[u32(u32(68) + index * 4)] for index in range(u32(64))]
    return [types[u32(u32(100) + index * 32)] for index in range(u32(96))]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    with zipfile.ZipFile(args.apk) as apk:
        assert apk.testzip() is None, "APK CRC failure"
        data_files = [name for name in apk.namelist()
                      if name.lower().startswith("assets/quran") and name.endswith(".txt")]
        dex_files = [name for name in apk.namelist() if re.fullmatch(r"classes\d*\.dex", name)]
        assert dex_files, "APK has no DEX to inspect"
        removed = []
        for name in dex_files:
            removed += [value.decode("ascii", "replace") for value in class_descriptors(apk.read(name))
                        if re.search(br"/Quran(?:Activity|Service|Data|Notch)(?:\$|;)", value)]
        assert not data_files, "Removed Quran text bundled: " + str(data_files)
        assert not removed, "Removed Quran classes bundled: " + str(removed[:8])
    print("PASS: built APK contains no legacy Quran reader/player classes or offline text assets")


if __name__ == "__main__":
    main()
