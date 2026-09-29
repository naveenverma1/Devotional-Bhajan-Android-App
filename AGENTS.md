# Project rules for AI agents

Repository: Sunderkand Path and Chalisa (`com.nv.user.sunderkand`).

## Git / commit rules

- **NEVER** add `Generated with [Devin](...)` or
  `Co-Authored-By: Devin <...>` lines to commit messages. Plain, focused
  commit subjects and bodies only. No tool attribution of any kind.
- Don't push branches without being asked.
- Don't change `git config`.
- Use the existing keystore at `keystore/upload.jks` for signing. The
  password is in `keystore.properties` (git-ignored).

## Release workflow

- Bump `versionCode` and `versionName` in `app/build.gradle`.
  Play history: 7 = v3.0, 8 = v4.0, 9 = v4.1.
- Run `./gradlew testDebugUnitTest` (content/audio integrity tests).
- Build with `./gradlew clean bundleRelease`.
- **Verify audio is in the bundle**:
  `unzip -l app/build/outputs/bundle/release/app-release.aab | grep base/res/raw/`
  must list every MP3. v4.0 shipped without them because the resource
  shrinker saw no static reference (see `data/AudioCatalog.kt`).
- Deploy to Play Console via `python scripts/play_deploy.py` (the
  service account JSON lives at `keystore/play-service-account.json`,
  git-ignored). It also uploads the R8 mapping for readable crash traces.
  On the corporate laptop pass `--ca-bundle` for the proxy CA.
- Never deploy without explicit user instruction.

## Adding content

- Text: add a chalisa object to `assets/content.json` (ids `[a-z0-9_]+`).
- Audio: drop `res/raw/<name>.mp3`, set `"audio": "<name>"` on the
  chalisa, AND add a row to `AudioCatalog.byName`. The unit test fails
  if any of the three is missing.
- Verse timings (karaoke highlight / follow-audio): run
  `python scripts/align_audio.py --chalisa <id> --model medium --device cpu`
  and review the printed table before committing. Never hand-edit
  `startMs` values without re-checking monotonicity.
- Do not bundle third-party audio without clear licensing. Public-domain
  *text* (Tulsidas etc.) is fine; recordings are separately copyrighted.

## Emulator testing

- AVD `hikertest` (Pixel-ish, API 35, 1080x2340 @ 440dpi). Boot with
  `emulator -avd hikertest -gpu swiftshader_indirect`; cold start of the
  app on this AVD takes ~8 s.
- Debug build id is `com.nv.user.sunderkand.debug`.
- `adb exec-out screencap -p` screenshots are full-res (1080x2340);
  tap coordinates must be in device pixels, not screenshot-viewer pixels.

## v4.x architecture

- Kotlin only. No new Java files.
- Single `MainActivity` hosting a Jetpack Compose graph. No Fragments.
- Bottom overlays (mini-player) publish their height via
  `LocalBottomOverlayHeight`; every scrolling screen adds it to its
  bottom content padding.
- Reader auto-scroll lives in `ui/reader/AutoScroll.kt`: constant-pace
  mode uses `withFrameNanos` + `scrollBy`; when the chalisa's timed audio
  is loaded it flips to follow-audio (`animateScrollToItem` on verse
  change). A user drag is detected by the `scrollBy` mutation being
  rejected (CancellationException while our job is still active).
- Material 3, fixed brand palette (saffron primary, maroon accent,
  sepia surface). Dynamic color **off**.
- Devanagari typography: Noto Serif Devanagari (body), Tiro Devanagari
  Hindi (headings) — bundled in `res/font/`.
- Audio: AndroidX Media3 `MediaSessionService`, foreground notification,
  lock-screen + Bluetooth controls. No bare `MediaPlayer` in new code.
- Content: bundled `assets/content.json` parsed via
  `kotlinx.serialization`. **No Firebase**, no network.
- Preferences/bookmarks/sankalp: DataStore Preferences.
- Min SDK 24 (was 21 in v3.x), Target SDK 35.
