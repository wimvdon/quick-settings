# Architecture

## This app
Quick Settings adds two Quick Settings tiles and a main screen with setup instructions.

**Screen tile**: four screen settings.

| Setting | Default | Changed to | Where Android keeps it | Permission |
| --- | --- | --- | --- | --- |
| Brightness | Auto | Manual, any level (slider; the X goes back to auto) | `Settings.System` `screen_brightness_mode`, `screen_brightness` | Modify system settings |
| Screen timeout | 1 minute | 30 minutes | `Settings.System` `screen_off_timeout` | Modify system settings |
| Always-on display | Off | On | `Settings.Secure` `doze_always_on` | `WRITE_SECURE_SETTINGS` (adb) |
| Tap to wake | On | Off | `Settings.Secure` `doze_tap_gesture` (Pixel) and `double_tap_to_wake` (AOSP) | `WRITE_SECURE_SETTINGS` (adb) |

**Private DNS tile**: `Settings.Global` `private_dns_mode` between `hostname` and
`opportunistic` (Automatic); the hostname is `private_dns_specifier`, set in Android's Settings.
Needs `WRITE_SECURE_SETTINGS` (adb).

- **Tiles** (`tile/`): `ScreenTileService` is lit while any screen setting differs from its
  default, names the change and opens the popup. `DnsTileService` (a toggle) is lit while the
  hostname is in use; a tap switches to the other mode, or opens the main screen when the adb
  permission or a hostname is missing. Long-pressing either tile reaches
  `TilePreferencesActivity`, an invisible trampoline that opens Display or Network & internet.
- **Shortcuts** (`shortcut/ShortcutActivity.kt`, `res/xml/shortcuts.xml` plus a debug copy that
  names the `.debug` package): four static launcher shortcuts that toggle Private DNS, the
  timeout, always-on display and tap to wake, with a toast. Quick Tap can start them too.
- **Main screen** (`ui/main/`): the launcher entry. Permissions first (with the numbered adb
  steps and a Copy button), then instructions: add the tiles (Android 13+ asks with one tap), the
  launcher shortcuts and Quick Tap. `GuideSteps` turns a translated block into numbered steps.
- **Popup** (`ui/panel/`): a floating, centred activity styled like Android's Internet dialog
  (`PanelActivity`), opened from the Screen tile and the notifications; `PanelModel` holds its
  state and actions. Android closes the shade when an app's tile opens anything. Missing permissions show as setup cards in it.
- **Notifications** (`notify/Notifications.kt`), in two channels: *Display* (manual brightness,
  button Auto; long timeout, button 1 min) and *Private DNS* (whenever it isn't on the hostname: Off or Automatic; button Hostname).
- **Rules** (plain Kotlin, unit tested): `screen/ScreenController.kt` switches auto brightness
  off *before* writing a level, because Android ignores a level written in auto mode;
  `BrightnessCurve` maps the slider like Android's own (HLG gamma). `dns/PrivateDns.kt` decides
  what a tap switches to and when the tile is lit or the notification shows.
- **Tiles stay current while the shade is open**: both extend `WatchingTileService`, which
  watches their settings with a `ContentObserver` between `onStartListening` and
  `onStopListening` and redraws on every change.
- **Keeping in step**: `QuickSettingsApp` watches the settings with a `ContentObserver` while the
  process runs, and the tiles, popup and main screen re-read them whenever they are shown, so
  changes made in Android's Settings show up too.
- **Stored data:** none. The defaults are fixed in `ScreenState.DEFAULTS`; everything else is
  read from Android's settings.

## Structure
```
app/src/main/kotlin/<package>/
  QuickSettingsApp.kt      Application: notification channels, settings observer, sync().
  dns/                     PrivateDns (plain Kotlin, tested) and SystemDnsSettings.
  screen/                  ScreenState, ScreenController, BrightnessCurve (plain Kotlin, tested)
                           and SystemScreenSettings (reads/writes Android's settings).
  notify/                  Persistent notifications (two channels) and their buttons.
  shortcut/                The invisible target of the launcher shortcuts.
  tile/                    The two tiles, and the long-press trampoline to Android's settings.
  ui/main/                 The main screen: MainActivity (state, permissions) and MainScreen (Compose).
  ui/panel/                The popup: PanelActivity (state, permissions) and PanelScreen (Compose).
  ui/theme/                Theme.kt (Android's dynamic colours), Shapes.kt (shared kit).
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
