# V58 cooperative native-display experiment

This is separate from the NexaDash launcher. It is not a working general app host and is not included in the launcher APK. See [results and limits](../../docs/V58_PANEL_LAB_MS.md).

The input is the owner's reviewed V58 APK (SHA256 pinned in the build script). No third-party APK or signing key is distributed here. Minimum Android **29 is preserved**; this does not run on the owner's API27 head unit. Use a disposable API29+ emulator with no personal accounts. The patched target is re-signed, so use a clean emulator instead of replacing an existing installation with saved data.

Requirements: JDK17, Android SDK35/build-tools36, apktool-cli2.11.1 jar, Python3, zip, and one disposable signing key for BOTH APKs. Set `ANDROID_HOME`, `APKTOOL_JAR`, `PROBE_KEYSTORE`, `KLIKDASH_KS_PASS`; then run:

```sh
bash tools/v58-panel-lab/build-lab.sh '/absolute/path/to/YouTube_Pro V58-SamMods .apk'
```

Outputs are ignored under `out/`: `target.apk` and `host.apk`. The target's existing MicroG dependency must be installed/configured normally in the emulator before opening the host. The experiment does not remove authentication, DRM, secure-window flags or the SDK minimum. Do not log in to personal accounts for this test.

Install target, then host, and launch **NexaDash V58 lab host**. It creates a host-owned private VirtualDisplay and starts an opted-in target Activity using a host proxy; no root command or privileged host permission is used by either APK. A signature-protected service in the target receives touch parcels and dispatches them to its own active Activity. The host and target have different UIDs.

The status `target query = 0` is only a dummy Binder roundtrip, NOT a click count or playback proof. Only actual visible UI changes count as interaction evidence. Activity-only dispatch does not handle separate dialog windows or IME fully. If the target is force-stopped while the host is open, close/reopen the lab host to restore its binding. Use STOP/RELEASE DISPLAY before leaving; this lab host has not received production lifecycle/back/key/input/accessibility hardening.

No video playback, real-head-unit compatibility, Maps/Waze/WhatsApp hosting or low-resource claim follows from this experiment. No modified APK is published to GitHub. The small self-written two-APK [generic probe](../optin-panel-probe) is the safer API27 firmware check first.
