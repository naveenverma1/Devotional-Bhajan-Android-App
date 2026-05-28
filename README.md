# Sunderkand

Native Android devotional app for reading and listening to Sunderkand, Hanuman Chalisa, Bajrang Baan, Hanuman Ashtak, and Hanuman Aarti.

[Download on Google Play](https://play.google.com/store/apps/details?id=com.nv.user.sunderkand)

## Latest release

**v4.0** is a complete modern Android rewrite with Jetpack Compose, Media3 audio playback, and refreshed devotional branding.

- Package: `com.nv.user.sunderkand`
- Version: `4.0`
- Version code: `8`
- Minimum Android version: Android 7.0 / API 24
- Target SDK: Android 15 / API 35

## Features

- Offline Hindi devotional text bundled with the app
- Sunderkand Path, Hanuman Chalisa, Bajrang Baan, Hanuman Ashtak, and Aarti
- Clean Devanagari typography with a saffron, maroon, and sepia theme
- Audio playback using AndroidX Media3
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

Build a signed release bundle:

```bash
./gradlew clean bundleRelease
```

Release signing uses `keystore.properties` and `keystore/upload.jks`, which are intentionally not committed.

## Privacy

The app is designed around bundled offline content and does not require Firebase or a network backend. The privacy policy is available in [`docs/privacy-policy.html`](docs/privacy-policy.html).

## Third-party licenses

Third-party dependency notices are maintained in [`THIRD_PARTY_LICENSES.txt`](THIRD_PARTY_LICENSES.txt).
