# FM service protocol recovered from the owner firmware

This is a client of existing exported `onStartCommand` entry points. It does not cast a remote BinderProxy to the local Binder classes previously reviewed. The local-Binder limitation remains true; the later Intent entry-point review supplies a separate control route.

Inputs were the same owner-supplied Radio APKs and the complete runtime ZIP already inventoried in [the runtime review](RADIO_RUNTIME_FILES_REVIEW.md). No OEM binary/decompiled implementation is redistributed with this client. No direct device/MCU command bytes are inferred or sent.

## External Radio2.3.24

`com.acloud.stub.extradio/com.radio.service.RadioService` is exported without a manifest service permission. `onStartCommand` calls `parseIntent`. For action`com.android.xygala.ACTION_CONTROL_RADIO`, the string extra`method` selects commands:

| Client operation | Extras | OEM path |
|---|---|---|
|Start|`method_open`|`openRadioDevice()` then`setAudioToRadio()`|
|Tune|`method_setFreq`, `param_freq:int`|`setFreq(int)`; external units arekHz|
|Stop|`method_close`|`setAudioToGps()` then`closeRadioDevice()`|

The source also contains area/sensitivity/init/band methods, but this adapter does not change those settings or guess a band identifier. `method_audioToRadio` is another call to`setAudioToRadio`, not a remote wrapper for`interfaceSetAudioToRadio`'s additional platform selector. Physical audio routing remains a head-unit test requirement.

The service emits`com.acloud.intent.android.MAINUI_UPDATE` with`extras:String[]{"local_radio_state","1"|"0"}`. This is an unprotected global status channel, not authenticated hardware acknowledgement. In the supplied code, the open flag can be reported even when the native interface has not actually opened successfully. NexaDash labels it as service-reported state.

The adapter never calls`stopService`: OEM`onDestroy` closes the tuner and calls its own process-kill path. A power/tune request remains distinct from audible FM verification.

## Internal FM3.1

`com.android.fmradio/com.android.fmradio.FmtestService` is exported without a manifest service permission. It accepts:

| Operation | Intent extras | Behavior in supplied code |
|---|---|---|
|Initialize|`flag:int=0`|`RadioTest()` calls`openDevice()`/powerUp and factory autoScan|
|Tune|`flag:int=1`, `freq:float`|`RadioTesttune(float)` operates only after its device-open state is set; unitsMHz|
|Close|`flag:int=2`|`RadioTestClose()` powers down/closes and releases its audio patch|

Cold tuning with flag1 alone silently does nothing. The controller therefore waits for the complete scan-result array before tuning the latest selected frequency.

Result action is`android.intent.action.fmradioseekok`. `freq:int[]` is the complete result in10kHz units; `FmUtils.computeFrequency` divides these station integers by100 to obtainMHz. `freqone:int` is partial progress after a different conversion and must not be interpreted as completion. Arrays are bounded, range/grid checked, de-duplicated and sorted. Error sentinels are not listed as stations.

This is the OEM factory initialization route. Its callback includes an early scan-stop condition; the client does not promise full-band discovery. The synchronous service operation can also delay a queued close request. Neither complete scan nor accepted tune establishes audible output on TEST9278B.

## Verification boundary

The Android test uses two disposable firmware doubles with the exact service/component contracts and normal, distinct app UIDs. They record received commands and emit synthetic protocol replies. Activity aliases trap any unintended original-app launch. They contain no FM native code, driver or audio route. The test exercises native dashboard views, Android touch/Intent delivery, units, initialization ordering, input/presets, layout, cleanup and late replies. It cannot certify physical FM reception or sound.

Fixture source is under`tools/radio-fixture`; the runner is restricted to a disposable emulator and refuses replacing a differently signed package. Fixture APKs are not release downloads and must not be installed on a head unit.
