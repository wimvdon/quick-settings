# Testing

## Automatic (CI, on every pull request and push to `main`)

| Check | Command | Catches |
|---|---|---|
| Release script tests | `python3 -m unittest discover -s scripts/tests` | Version maths, keystore-secret decoding |
| Unit tests | `./gradlew testDebugUnitTest` | Logic bugs in plain-Kotlin classes (`app/src/test/`) |
| Lint | `./gradlew lintDebug` | Android API misuse, missing translations, accessibility, hard-coded text, … (errors fail the build) |
| Debug build | `./gradlew assembleDebug` | Compile errors; produces the APK to test on the phone |
| Release build | `./gradlew assembleRelease` | Problems caused by R8 minification / resource shrinking |

Reports (test results, lint HTML) are attached to every CI run as the `reports` artifact, also
when the run fails.

Run all of it locally before pushing:
```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
python3 -m unittest discover -s scripts/tests
```

### Writing unit tests
- Every logic change gets a test. Put logic in plain Kotlin classes (no Android types) and test
  them with JUnit in `app/src/test/kotlin/`, mirroring the package. See `SettingsControllerTest`.
- Test names describe behaviour in words: `` fun `tap stops at the maximum`() ``.
- No device tests (instrumented/UI tests) are set up: they need an emulator, which makes CI slow and
  flaky. Add them only if a decision in `decisions.md` says so.

## Manual (on the phone)

What automation can't check: how it looks and feels on a real device. For each pull request (before or after it is merged: merging only waits for green CI),
install the debug APK (see README, "Testing a pull request") and follow the steps in the pull request
description. In addition, check every now and then (always before a release):

- [ ] The app starts, and the changed screen looks right.
- [ ] **Dark mode**: switch the phone's dark theme on/off. Text stays readable.
- [ ] **Rotation** (if auto-rotate is on): the screen still works and keeps its state (the popup keeps working).
- [ ] **Language**: Settings → System → Languages → App languages → this app → Nederlands.
      All text is translated.
- [ ] **Large text**: Settings → Display → Font size at maximum. Nothing is cut off.
- [ ] **Back** gesture/button behaves as expected.
- [ ] After a release: install the release APK over the previous release. It updates without
      losing data.
- [ ] **Main screen**: a section per popup: its wide tile, then its tiles, all with their live
      state. A tap opens the popup; + offers to add the tile, a check shows once it is in Quick
      Settings. The banner shows while a permission is missing; ⋮ opens Permissions and
      Instructions.
- [ ] **Popup tiles**: with everything at its default the tile is off and says "Default";
      change one setting and it lights up and names it. Reset puts it back. Long-press opens
      Android's matching settings page.
- [ ] **Set up**: a tile's tune button sets its default (the tile lights up or goes off at once),
      its notification and its shortcut. A wide tile's tune button chooses the popup's rows (the
      last one can't be switched off); "Use current" takes every value as it is now. The tune
      button in a popup opens the same sheet in the app.
- [ ] **Brightness**: drag the slider while auto brightness is on: auto switches off and the
      level follows the slider. The X brings auto back.
- [ ] **Single tiles**: each switches its setting; Rotation, Ringer and Animations step through
      their choices. Keyboard opens the keyboard picker, Volume the volume panel.
- [ ] **Private DNS**: the tile switches hostname ↔ Automatic. In the Network popup, add a
      hostname, pick it, and remove a hostname that isn't in use.
- [ ] **Notifications**: a popup shows one while a setting that notifies differs; its Reset
      button puts those back.
- [ ] **Shortcuts**: touch and hold the app icon; each of Shortcut 1 to 4 switches the setting
      chosen for it and shows a toast. An empty one says so.
- [ ] **Changes made elsewhere** (Android's Settings) show up in the tile, popup and notifications.

## Reading a crash from a release build
Release builds are minified, so stack traces show short names like `a.b.c`. Download
`<app>-<version>-mapping.txt` from the matching GitHub Release and decode the trace with R8's
`retrace` (Android SDK `cmdline-tools/latest/bin/retrace`):
```sh
retrace <app>-<version>-mapping.txt stacktrace.txt
```
Or ask Claude Code to do it: give it the stack trace and the version.
