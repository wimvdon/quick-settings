# Architecture

## This app
Screener changes four screen settings from a Quick Settings tile:

| Setting | Default | Changed to | Where Android keeps it | Permission |
| --- | --- | --- | --- | --- |
| Brightness | Auto | Manual, any level (slider; the X goes back to auto) | `Settings.System` `screen_brightness_mode`, `screen_brightness` | Modify system settings |
| Screen timeout | 1 minute | 30 minutes | `Settings.System` `screen_off_timeout` | Modify system settings |
| Always-on display | Off | On | `Settings.Secure` `doze_always_on` | `WRITE_SECURE_SETTINGS` (adb) |
| Tap to wake | On | Off | `Settings.Secure` `doze_tap_gesture` (Pixel) and `double_tap_to_wake` (AOSP) | `WRITE_SECURE_SETTINGS` (adb) |

- **Tile** (`tile/ScreenTileService.kt`): lit while any setting differs from its default; the
  subtitle names the change (or "2 changed"). Tapping it opens the popup; long-pressing opens it too.
- **Popup** (`ui/panel/`): a floating activity styled like Android's Internet dialog. It is also
  the launcher entry. Missing permissions show as setup cards in it.
- **Notifications** (`notify/ScreenNotifications.kt`): one ongoing notification while brightness
  is manual (button: Auto) and one while the timeout isn't 1 minute (button: 1 min).
- **Rules** (`screen/ScreenController.kt`, plain Kotlin, unit tested): manual brightness always
  switches auto brightness off *before* writing the level, because Android ignores a level
  written in auto mode. `BrightnessCurve` maps the slider like Android's own (HLG gamma).
- **Keeping in step**: `ScreenerApp` watches the settings with a `ContentObserver` while the
  process runs, and the tile and popup re-read them whenever they are shown, so changes made in
  Android's Settings show up too.
- **Stored data:** none. The defaults are fixed in `ScreenState.DEFAULTS`; everything else is
  read from Android's settings.

## Structure
```
app/src/main/kotlin/<package>/
  ScreenerApp.kt           Application: notification channel, settings observer, sync().
  screen/                  ScreenState, ScreenController, BrightnessCurve (plain Kotlin, tested)
                           and SystemScreenSettings (reads/writes Android's settings).
  notify/                  Persistent notifications and their reset buttons.
  tile/                    The Quick Settings tile.
  ui/panel/                The popup: PanelActivity (state, permissions) and PanelScreen (Compose).
  ui/theme/                Theme.kt (Snoozer's colours), Shapes.kt (shared kit).
app/src/main/res/
  values/strings.xml       All user-visible text (English, the default).
  values-xx/strings.xml    Dutch, French, German, Spanish and Italian. Every string must exist in every language.
app/src/test/kotlin/       JVM unit tests (mirrors the main package structure).
scripts/                   Release helpers (Python/Java) with their own tests.
.github/workflows/         ci.yml (every PR), release.yml (signed releases).
```

## Conventions
- **Single `app` module.** Split into modules only if the app grows large; record it in `decisions.md`.
- **Logic in plain Kotlin classes** with no `Context`, `View` or Compose types, so it can be
  tested on the JVM. Example: `screen/ScreenController.kt`, tested with a fake `ScreenSettings`.
- **UI state**: small screens use `rememberSaveable`. When a screen gets real logic or data,
  add a `ViewModel` (`androidx.lifecycle:lifecycle-viewmodel-compose`) that exposes a
  `StateFlow<UiState>` and calls into the plain-Kotlin logic. Record it in `decisions.md`.
- **Navigation**: one screen today. With several screens, use Navigation Compose.
- **Persistence**: Jetpack DataStore (version already in the catalog). Wrap it behind a small
  repository interface so logic tests can use a fake. Decide per value whether it is backed up
  (see "Backup" below).
- **No network, analytics, crash reporting or ads** unless the owner agreed (see `CLAUDE.md`),
  in which case update `PRIVACY.md`.
- **Strings** in resources only; plurals via `<plurals>`.
- **Versions** of libraries only in `gradle/libs.versions.toml`.
- **App identity** (`applicationId`/`namespace`) only in `app/build.gradle.kts` (`appId`).

## Backup
Android backs up app data to the user's Google account (if they turned backup on) and copies it
to a new phone during set-up. The app decides which files take part, in
`app/src/main/res/xml/data_extraction_rules.xml` (`cloud-backup` = Google account,
`device-transfer` = new phone). The rule is an **allow list**: only listed files are copied, so a
new file is never backed up by accident.

- **Backed up:** settings the user chose and would want back after a reset or on a new phone
  (theme, layout, toggles, names). Keep them in one DataStore named `backed_up_settings`
  (`preferencesDataStore(name = "backed_up_settings")`), which the rules already list.
- **Not backed up:** sign-in tokens, passwords and API keys (security, and keys encrypted with the
  Android Keystore can't be read on another phone anyway); state that only means something on this
  phone (a running job, notification IDs, "switched on right now"); settings that only work after
  a permission or set-up on this phone; copies of data an online service can download again; caches
  (Android never backs up `cacheDir` and `noBackupFilesDir`). Keep these in another DataStore file.
- Android rules work per **file**, not per key. Moving a value from one file to another in an app
  that is already released needs a `DataMigration` (see Snoozer's `MoveKeysMigration`).
- `BackupRulesTest` checks both sections list exactly the settings file. Change the test together
  with the rules, and update `PRIVACY.md` when what is backed up changes.
- **minSdk below 31:** Android 8-11 ignore `data_extraction_rules.xml` and read
  `android:fullBackupContent` instead. Add `res/xml/backup_rules.xml` with the same `include` lines,
  point `fullBackupContent` at it in the manifest, and let `BackupRulesTest` check it too.
- To test on a phone: change a setting, then open Settings, search for "Backup" and tap **Back up now**. Uninstall the app and install the same APK again: the
  setting comes back. Restore only works for an APK signed with the same key.
