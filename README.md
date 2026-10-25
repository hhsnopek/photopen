# PhotoPen

A simple photo and video gallery for Android with one extra idea: **lock the
viewer to one folder and hand the phone over.**

Pick a folder, hold the lock for three seconds, and whoever is holding the phone
can only swipe left and right through the photos and videos in that folder. Made
for kids who want to look at pictures without wandering into the rest of the
device.

## What it does

- Lists the photos and videos on the device, newest first, from internal storage
  and any SD card.
- A folder picker narrows the view to one folder (Camera, Movies, an album you
  copied over). The choice is remembered the next time the app opens.
- Full-screen viewer: swipe between items, pinch to zoom, videos play on their
  own. Zoom is available until the viewer is locked.
- Hold the lock control for three seconds to lock. While locked the close button
  and video controls disappear and zoom is switched off: all that is left is
  swiping and playback. The unlock control fades out; tap the screen to bring it
  back, then hold it for three seconds to unlock.

## Using it

1. Open the app and allow access to photos and videos.
2. Tap the folder chip under the title and choose a folder.
3. Tap an item, then hold the lock control at the bottom for three seconds.
4. Hand the phone over. Hold the control again to unlock.

## What "locked" really means

Locking also pins the app with Android's lock task mode, so Home, Recents and the
notification shade are blocked and the phone cannot be taken out of PhotoPen
until it is unlocked. The first time you lock, Android shows a short
confirmation ("App is pinned") — accept it once.

If that confirmation is declined, or the device does not allow pinning, the
in-app lock still hides the close button, Back and the video controls, but Home
will work. Nothing an ordinary app can do covers that case; screen pinning is the
Android mechanism for it.

To leave a pinned viewer, hold the unlock control for three seconds, or use
Android's own unpin gesture (swipe up and hold).

## Install

Signed APKs are published at
<https://github.com/hhsnopek/photopen/releases> and can be updated in place.

For automatic updates, add the repository to
[Obtainium](https://github.com/ImranR98/Obtainium) as a GitHub source:

```
https://github.com/hhsnopek/photopen
```

The repository is public, so no token is needed.

## Privacy

- No internet permission at all: the app cannot send anything anywhere.
- No analytics, no accounts, no cloud. Photos stay where they are.
- The only permissions are the Android media-read permissions.

## Build and test

Requirements: JDK 21, Android SDK 35, `adb`.

```bash
make android    # debug APK
make install    # install it on a connected device
make check      # ktlint, detekt, lint, unit tests
./gradlew connectedDebugAndroidTest   # gesture tests on a device
```

Toolchain: Gradle 8.13, AGP 8.9.3, Kotlin 2.1.10, Compose, Media3 1.5.1,
compileSdk 35, minSdk 26. The release variant signs with
`app/keystore/release.keystore` when it exists; CI decodes it from repository
secrets, and a local build reads the passwords from the environment or from
`local.properties` (gitignored). Nothing secret is committed.
`KEYSTORE_PASSWORD=... KEY_PASSWORD=... make keystore` creates one. Back the keystore up: losing it means no future release can update
an installed copy.

Tests: 9 JVM tests and 12 instrumented tests (zoom, zoom-disabled locking, lock
hold, folder picker, folder grouping). The instrumented ones cover what unit
tests cannot: real Compose gestures, real MediaStore rows and a real screen.

## Limits

- Keeping the phone inside the app relies on Android's lock task mode, which
  needs the one-time pinning confirmation. Declining it leaves the in-app lock
  only.
- Folders come from Android's media index. A folder the system has not indexed
  yet will not appear until it does.
- Two folders with the same name on different volumes appear as separate
  entries with their own counts.
- English only; designed for phone-sized screens.

## License

MIT. See [LICENSE](LICENSE).
