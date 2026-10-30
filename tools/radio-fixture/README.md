# Emulator-only FM firmware doubles

These test APKs deliberately use `com.android.fmradio` and `com.acloud.stub.extradio` with the recovered service names. **Do not install them on a physical head unit.** They contain no OEM code, driver or audio. They only record service Intent data, emit synthetic callbacks and trap unintended Activity launches.

Build with `bash tools/radio-fixture/build.sh` after setting the existing private Next signing environment. Run through `DEVICE=emulator-5554 bash tests/run-radio-panel.sh` after building the Next host. The runner requires an emulator serial and QEMU marker, refuses replacing packages signed by a different key, and removes its installed doubles on exit.

The signature-protected event providers are test-only. No test endpoint, provider, fixture APK, hardware mock mode or fake station list is compiled into the released launcher. A public build on a device without compatible OEM services reports that FM is unavailable.

Use this evidence for the client UI/IPC contract only. Physical tuner/audio and the exact head-unit firmware still need owner testing. See `../../docs/RADIO_SERVICE_PROTOCOL.md`.

The runner temporarily disables and then restores window/transition/animator scales on the disposable emulator. This prevents animation-time test touches from being intercepted by a closing dialog. A narrowly matched startup System UI ANR may be dismissed before feature actions; product ANRs are never dismissed by that preflight. Core button input must reach the actual view via Android input routing.
