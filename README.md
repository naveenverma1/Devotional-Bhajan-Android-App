# Sunderkand

Native Android devotional app for reading and listening to Sunderkand, Hanuman Chalisa, Bajrang Baan, Khatu Shyam Chalisa, and Hanuman Aarti.

[Download on Google Play](https://play.google.com/store/apps/details?id=com.nv.user.sunderkand)

## Latest release

**v4.1** adds auto-scroll (including karaoke-style "follow the singer" sync for
Hanuman Chalisa), a reading-settings sheet, and fixes the v4.0 release that
shipped without its audio files.

- Package: `com.nv.user.sunderkand`
- Version: `4.1`
- Version code: `9`
- Minimum Android version: Android 7.0 / API 24
- Target SDK: Android 15 / API 35

### What's new in 4.1

- **Auto-scroll** with 10 speed levels; touching the text pauses it and it
  resumes by itself two seconds after you let go
- **Follow the singer**: when the chalisa's audio is playing, the verse being
  sung is highlighted and auto-scroll keeps it in view; tap any verse to jump
  the audio there
- **Reading settings** sheet: text size, line spacing, light / dark / system
  theme, keep-screen-on
- Fixed: audio missing from v4.0 (resource shrinker removed the MP3s)
- Fixed: "Continue Reading" always reopened at the top
- Fixed: mini-player covered the last verses and sat under the navigation bar

## Features

- Offline Hindi devotional text bundled with the app
- Sunderkand Path, Hanuman Chalisa, Bajrang Baan, Khatu Shyam Chalisa, and Aarti
- Clean Devanagari typography with a saffron, maroon, and sepia theme
- Audio playback using AndroidX Media3, with per-verse sync for Hanuman Chalisa
- Auto-scroll (fixed pace or following the audio) that keeps the screen awake
- Mini-player with playback controls across screens
- Full Now Playing sheet with seek bar, replay, and 10-second skip controls
- Sleep timer presets for audio playback
- Continue Reading card that resumes the last opened path
- Sankalp tracker for devotional reading goals
- Long-press any verse to share or copy it
- Branded Hanuman launcher icon and splash screen

## Tech stack

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- AndroidX Media3 ExoPlayer + MediaSessionService
- DataStore Preferences
- kotlinx.serialization
- Gradle Android plugin

## Project structure

```text
app/src/main/java/com/nv/user/sunderkand/
├── audio/        Media3 player service and controller
├── data/         Bundled content loading and preferences
├── share/        Verse sharing helpers
├── ui/           Compose screens, components, and theme
└── MainActivity.kt
```

## Local development

Requirements:

- Android Studio
- JDK 17
- Android SDK 35

Build a debug APK:

```bash
./gradlew assembleDebug
```

Run the unit tests (content.json integrity, audio wiring, verse timings):

```bash
./gradlew testDebugUnitTest
```

Build a signed release bundle:

```bash
./gradlew clean bundleRelease
```

After a release build, confirm the audio survived resource shrinking before
uploading (v4.0 shipped without it):

```bash
unzip -l app/build/outputs/bundle/release/app-release.aab | grep "base/res/raw/"
```

Release signing uses `keystore.properties` and `keystore/upload.jks`, which are intentionally not committed.

## Privacy

The app is designed around bundled offline content and does not require Firebase or a network backend. The privacy policy is available in [`docs/privacy-policy.html`](docs/privacy-policy.html).

## Third-party licenses

Third-party dependency notices are maintained in [`THIRD_PARTY_LICENSES.txt`](THIRD_PARTY_LICENSES.txt).
