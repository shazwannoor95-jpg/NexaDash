# NexaDash Web Test

Separate, internet-only APK for testing the production `DashWebPlayer` fullscreen fix. Package `com.shazwan.nexadash.webtest`, label **NexaDash Web Test**, version1.0. It co-installs beside Native Lab, has no HOME/default-launcher intent, boot receiver, services, providers, native companions or native libraries. No original app/settings are migrated or replaced.

The left panel runs the same WebView player as native10 source. Right panel shows instructions. Tap **Start YouTube web**, search/play a video, tap the video's fullscreen icon, then **Exit fullscreen** or Android **Back**. Pause/close releases the player; returning does not silently autoplay.

Build with JDK17, SDK35 and build-tools36:

```
ANDROID_HOME=/path/to/sdk WEB_TEST_KEYSTORE=/private/path/webtest.jks WEB_TEST_KS_PASS='<private password>' bash tools/web-test/build.sh
```

Do not put a password into shared shell history. Prefer supplying it from your private environment/password manager. Alias `webtest` is expected. The new Web Test signing key is separate from the unrecovered Native Lab key; keep a private backup for future Web Test updates. No private key/password is published in this repository.

Only the color constants needed to compile the shared player are supplied as a compile-time stub. No DashboardActivity stub is packaged. Assets/launcher/media/native-host code are excluded. Output: `build/web-test/NexaDash-WebTest-1.0.apk`.

Validation performed: resources/Java/DEX, signing, alignment, archive CRC, manifest package/label/version, Internet-only permission and no HOME filter. Real WebView/YouTube fullscreen and hardware behavior remain to be tested by the owner; static checks are not playback proof.
