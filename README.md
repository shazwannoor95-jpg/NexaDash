# YouTube V58 native1 companion — experimental

This branch contains the owner's modified V58 APK split into <100MiB files because GitHub rejects a single 214MiB APK. The normal NexaDash source ZIP does not include this APK.

[Download this companion ZIP](https://codeload.github.com/shazwannoor95-jpg/klikdash/zip/refs/heads/nexadash-v58-companion), extract ALL parts into one folder, then run **Join-V58-Windows.cmd**. It creates `YouTube-V58-native1.apk`; compare the printed SHA256 with `artifact.json`. Linux/Termux: `sh Join-V58-Termux.sh` (no pipe symbol needed). Never install a part file by itself.

**Minimum Android10 / API29 is unchanged. API27 will refuse installation.** NexaDash Native Lab itself can install on older Android, but does not lower a companion app's SDK floor. For an API27 firmware experiment use its Demo companion or the separately marked ARM TizenTube candidate.

Install ONLY alongside the matching cloud-signed NexaDash Native Lab APK. An original SamMods V58 installation has a different signing certificate; use a clean test emulator rather than deleting saved personal data. V58's MicroG dependency/setup still applies. No personal account is needed for the UI test. Playback has not been verified, and dialog/keyboard/lifecycle behavior remains experimental. Standalone and NexaDash Lab tests logged a V58/Primes lifecycle error during Activity destruction; the Lab is not certified crash-free.

The native app appears only after selecting **Native app · experimental** in a NexaDash Lab panel and pressing **Start**. Unmodified V58 does not provide this bridge.

[Native Lab source, install order and limits](https://github.com/shazwannoor95-jpg/klikdash/blob/nexadash-native-panel-experiment/docs/NATIVE_LAB_MS.md). Input manifest/code changes and rebuild scripts are provided there; this APK retains its original third-party code and notices. It is not an official Google release, is not part of the standard .8 launcher, and has not been safety-audited.
