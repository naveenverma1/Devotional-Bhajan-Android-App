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
- Build with `./gradlew clean bundleRelease`.
- Deploy to Play Console via `python scripts/play_deploy.py` (the
  service account JSON lives at `keystore/play-service-account.json`,
  git-ignored).
- Never deploy without explicit user instruction.

## v4.0 architecture (in-progress rewrite)

- Kotlin only. No new Java files.
- Single `MainActivity` hosting a Jetpack Compose graph (no Fragments
  in new code; old `fraghome`/`chalisaFragment`/etc. are scheduled for
  deletion once parity is reached).
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
