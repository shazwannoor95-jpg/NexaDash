# Official KlikDash release review

Reviewed 2026-10-26 against the official author's repository, not only the selected fork.
The initial review was read-only. The clock/prayer/notice behavior was subsequently adapted selectively; see the integration notes below.

- Official source: https://github.com/ashafizullah/klikdash
- Latest published release: https://github.com/ashafizullah/klikdash/releases/tag/v1.7.4
- Published: 2026-10-25T23:31:06Z = 26 October 2026, 07:31 MYT.
- Release commit: 28d8a99c21722a7fcec612586599f12653344c98.
- At review time, main points to that same commit; no additional unreleased main commits observed.
- Saga Dash's starting checkout: 6e637cbb848cdd83c839d1b8b6325a6fad50372e, original build version1.6.7/code25.
  This checkout includes two documentation commits after the v1.6.7 release tag.
- Diff: 50 files, 2,348 insertions and1,688 deletions, including a substantial home-code split.

## Relevant changes since the baseline

| Release | Change | Relevance to Saga Dash |
| --- | --- | --- |
| 1.6.8 | Rewrites prayer-time calculations from public astronomical formulae; upstream describes project code as MIT again. Kemenag parameters retained. | Keep our official JAKIM timetable path. Legacy PrayerTimes code still needs a separate licensing/dead-code review; this release does not change our local files automatically. |
| 1.6.9 | Glossy theme; optional automatic update checks; clearer errors for closed ADB TCP5555 on Android11+. | ADB guidance is useful for the shown Android12 unit. Do not enable upstream automatic APK updates for the separate Saga Dash package. |
| 1.7.0 | Glossy UI refinement, correct space for prayer panel, steadier update-check behavior. | Most home changes concern the original layout; preserve the owner's custom dashboard. |
| 1.7.1 | KlikBeam Android phone sender in Play Store closed testing; iPhone sender still coming. | Our older iPhone-only beta notes are now outdated. Optional future capability, not a Bluetooth media bridge. |
| 1.7.2 | Delay prayer alerts until clock is trusted; remove notices left over from sleep. | Important head-unit reliability fixes to adapt before finalizing an offline prayer workflow. |
| 1.7.3 | Check/fix head-unit time using GPS satellite time, including while another app is foreground; home refactor; continuous Quran playback. | GPS clock/alert handling is important. Quran playback changes do not apply to the stripped-down edition. |
| 1.7.4 | Dedicated KlikBeam Settings tab; fixes selecting an ayah during basmalah. | Beam settings organization is optional; Quran UI changes are excluded from Saga Dash. |

## Clock behavior verified in code

`Clock.java` compares system time with time from a GPS-provider fix, adjusted by elapsed realtime.
It rejects times earlier than generated `Built.TIME`, allows a two-minute difference, and throttles
system-time setting attempts to once a minute. When automatic time is enabled, setting the device
clock uses the existing local ADB shell route (`service call alarm 2 i64 ...`).
Without a usable ADB route, the original app leaves system time unchanged and withholds prayer
alerts until time is trusted. `PrayerAlarm` rechecks pending alerts; `HomeLocation` feeds GPS time
into the guard. The new background-GPS path also depends on permissions/ADB configuration and
an Android location-throttle whitelist. These hardware-dependent parts are not verified for TEST9278B.
Copying Clock.java alone would miss its generated Built.TIME, receiver/lifecycle, permission and
alarm dependencies. Our separate package and JAKIM schedule require deliberate adaptation.

Saga Dash currently resolves zones from GPS but still bases the dashboard date/countdown/alarms
on system wall-clock time. GPS zone selection alone does not fix a bad head-unit date/time.
Prioritize a trusted-time guard and GPS-derived time handling before the beginner Android Studio
handoff; evaluate an app-local trusted clock where setting the system clock is unavailable.

## Features not supplied by this update

No new JAKIM integration or rotatable Saga3D model was found in the reviewed source. The added
MediaSession code is for Quran playback; it does not aggregate YouTube/Bluetooth/radio metadata
or supply the requested lyrics mini player. Those remain custom Saga Dash features.

Release notes: https://github.com/ashafizullah/klikdash/releases


## Selective integration into Saga Dash

Implemented the relevant verified-clock and elapsed-notice behavior without importing the original
home-screen refactor, Kemenag calculation, Quran/dhikr features or upstream APK updater.
`TrustedClock` uses fresh non-mock GPS-provider time plus elapsedRealtime inside the app. The
system clock is not changed, no local ADB command is issued, and no background-location permission
or global whitelist is silently granted. A first fix is needed while Saga Dash is receiving GPS;
the verified clock then advances during sleep/background, independently of the device RTC.

A same-boot anchor can survive process recreation if BOOT_COUNT is available. It expires after24h
without another accepted fix and is rejected after reboot. Historical GPS rollover dates, stale,
duplicate, mock and out-of-order fixes cannot assert time. Large corrections to an existing GPS
anchor require two consistent fixes. When GPS is unavailable, the owner can explicitly confirm
correct device time by tapping the clock; changing that device time revokes manual confirmation.

Dashboard date, JAKIM date selection, countdown and prayer scheduling use verified app time.
Unverified time is labelled and cannot trigger a countdown or prayer alert. Alarms use
ELAPSED_REALTIME, so a bad device RTC cannot fire them years early or late. Catch-up is limited
to10min after a real prayer time, excludes sunrise, checks the current zone and deduplicates by
zone/date/prayer. Notification UI runs on the main thread, with parsing/scheduling on a worker.

Notice popup and overlay expiry uses elapsedRealtime with<=1s polling and resume/screen-on cleanup.
Actual head-unit deep sleep, GPS time quality and OEM notification behavior remain to be tested.
The added engine/deadline/alarm-policy tests plus existing Android integration checks pass286
assertions without changing Android system time or using real GPS/ADB clock-setting privileges.
REA was assessed separately in docs/REA_REVIEW.md; no binary investigation was needed here.

The final review also aligned AI/status/Hijri dates and cache freshness with verified app UTC.
Downloads made before verification record unknown freshness; a bad future RTC stamp cannot suppress
refresh indefinitely. Alarm work is serialized independently from network refresh, zones are
rechecked before arming, and a per-zone/day prayer ledger prevents A→B→A replay after backwards
clock correction. Manual clock confirmation is revoked on observed time changes or significant
wall divergence after process recreation. No system-time setting or physical-device test ran.
