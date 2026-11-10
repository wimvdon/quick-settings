# Architecture

## This app
Quick Settings adds five popup tiles and fifteen single tiles, and a main screen that shows them
all the way Quick Settings draws them: a section per popup, its wide tile with the single tiles
below, each with a tune button (set it up) and a plus (add it).

**The catalogue** (`settings/Catalog.kt`, plain Kotlin, tested): every setting the app can
change, where Android keeps it and what it needs.

| Popup | Setting | Where Android keeps it | Permission | Default |
| --- | --- | --- | --- | --- |
| Screen | Brightness (auto / manual + slider) | `System` `screen_brightness_mode`, `screen_brightness` | Modify system settings | Auto |
| Screen | Screen timeout (1, 5, 30 min) | `System` `screen_off_timeout` | Modify system settings | 1 min |
| Screen | Always-on display | `Secure` `doze_always_on` | adb | Off |
| Screen | Tap to wake | `Secure` `doze_tap_gesture` (Pixel) + `double_tap_to_wake` (AOSP) | adb | On |
| Screen | Rotation (auto / portrait / landscape) | `System` `accelerometer_rotation`, `user_rotation` | Modify system settings | phone's* |
| Color | Night Light + warmth | `Secure` `night_display_activated`, `night_display_color_temperature` (2596–4082 K) | adb | phone's* |
| Color | Extra dim + intensity | `Secure` `reduce_bright_colors_activated`, `reduce_bright_colors_level` | adb | phone's* |
| Color | Grayscale | `Secure` `accessibility_display_daltonizer_enabled` + `accessibility_display_daltonizer` = 0 | adb | phone's* |
| Color | Color inversion | `Secure` `accessibility_display_inversion_enabled` | adb | phone's* |
| Sound | Ringer (sound / vibrate) | `AudioManager.ringerMode` (mirrored in `Global` `mode_ringer`) | none | phone's* |
| Sound | Media, ring, notification, alarm volume (sliders) | `AudioManager` stream volumes | none | (none) |
| Sound | Touch vibration | `System` `haptic_feedback_enabled` | Modify system settings | phone's* |
| Sound | Pop-up notifications | `Global` `heads_up_notifications_enabled` | adb | phone's* |
| Developer | USB debugging | `Global` `adb_enabled` | adb | phone's* |
| Developer | Wireless debugging | `Global` `adb_wifi_enabled` | adb | phone's* |
| Developer | Animations (off / 0.5× / 1×) | `Global` `window_animation_scale`, `transition_animation_scale`, `animator_duration_scale` | adb | phone's* |
| Developer | Stay awake while charging | `Global` `stay_on_while_plugged_in` (7 = every charger) | adb | phone's* |
| Network | Private DNS (off / automatic / a hostname) | `Global` `private_dns_mode`, `private_dns_specifier` | adb | any hostname |

\* The first start takes the phone's value as the default of every setting (`Catalog.seeded`,
`SettingsController.seedDefaults`), so nothing lights up after installing. A value in the column
is the app's fallback, used when the phone has a value the app doesn't offer (a 2-minute
timeout). "adb" is `WRITE_SECURE_SETTINGS`, granted once with adb.

- **Settings model** (`settings/`): `Setting` is a `Toggle`, a `Choice` (options as text) or a
  `Level` (slider 0..1); toggles and choices are `Tracked` (they have a default). They read and
  write through `Device`: `RawSettings` (Android's tables as text) and `Audio`, both faked in
  the tests. `PrivateDnsSetting` is a tracked setting of its own ("off", "opportunistic",
  "hostname:<name>"; the default "hostname" means any). `Group` lists each popup's rows;
  `TileId` every tile and what a tap does (`TileKind`). `SettingsController` holds the rules:
  what differs from its default, reset, toggle, cycle, timeout default ↔ long, Private DNS
  hostname ↔ Automatic.
- **Defaults** (`settings/Defaults.kt`, `data/SettingsRepository.kt`): the user's defaults (only
  those that differ from the app's), whether each setting notifies (falling back to the per-popup
  choice of earlier versions), the rows each popup leaves out, what each shortcut slot taps, the
  saved hostnames and the seeded flag, in the backed-up DataStore `backed_up_settings`. Which tiles are added lives in
  `device_state` (not backed up). Both are `StateFlow`s, read once at start.
- **Tiles** (`tile/TileServices.kt`): one `AppTileService` subclass per tile; the class names are
  how Android remembers a tile (`ScreenTileService` and `DnsTileService` are the old names). A
  popup tile is lit while something in it differs from its default and names it; a switch tile
  while on; a cycle tile (rotation, ringer, animations) and the timeout tile while not at their
  default. A tile without the permission it needs opens the main screen. A popup tile only counts the
  rows its popup shows. Long-press
  (`TilePreferencesActivity`): a popup tile opens Android's settings page, a single tile its popup.
  Keyboard and Volume go through the invisible `action/ActionActivity`.
- **Popups** (`ui/panel/`): one floating activity for every `Group`, styled like Android's Internet
  dialog, with the rows the user chose. Rows that differ from their default are highlighted. The
  tune button opens the popup's tune sheet on the main screen; Reset puts everything back. Network has radio rows for Off, Automatic and each hostname, with add and
  remove.
- **Shortcuts** (`settings/Shortcuts.kt`, `shortcut/ShortcutActivity.kt`, `res/xml/shortcuts.xml`
  plus a debug copy): four static launcher shortcuts, "Shortcut 1" to "Shortcut 4". Each taps the
  tile the user picked for it (at first: Private DNS, timeout, always-on display, tap to wake)
  and confirms with a toast. Quick Tap can start them too. Their ids and actions keep the names
  from when each had a fixed setting.
- **Main screen** (`ui/main/`): a section per `Category` (`TileId.sections`): the popup's wide
  tile (its state and the rows it shows), then its single tiles, each drawn like a Quick Settings
  tile with its live state. A tap opens the popup; + adds the tile (Android 13+, a check once
  added); the tune button opens a bottom sheet (`SetUp`). A popup's sheet chooses its rows, each
  with its own tune button; a setting's sheet sets its default, its notification and its
  shortcut slot (a dropdown). A banner shows while a permission is missing. The overflow
  menu opens Permissions (with the adb guide, from Snoozer) and Instructions.
- **Notifications** (`notify/Notifications.kt`): one per popup while a setting in it that notifies
  differs, listing those settings, with a Reset button for them, in a channel per popup (`display` and `private_dns`
  keep their old ids).
- **Keeping in step**: `QuickSettingsApp` watches every catalogue key with a `ContentObserver`
  while the process runs, and the tiles, popups and main screen re-read them whenever they are
  shown, so changes made in Android's Settings show up too.

## Structure
```
app/src/main/kotlin/<package>/
  QuickSettingsApp.kt      Application: repository, controller, settings observer, sync().
  settings/                The model (plain Kotlin, tested): Catalog, Setting, Group, Tiles,
                           Defaults, PrivateDns, SettingsController, BrightnessCurve.
  system/                  AndroidRawSettings and AndroidAudio: the Android side of the model.
  data/                    SettingsRepository: DataStore for defaults and added tiles.
  notify/                  One notification per popup, and its Reset button.
  shortcut/                The invisible target of the launcher shortcuts.
  action/                  The invisible activity for the Keyboard and Volume tiles.
  tile/                    The tiles, and the long-press trampoline.
  ui/Labels.kt             Names, icons and value texts of settings, popups and tiles.
  ui/main/                 The main screen: MainActivity (state, permissions) and MainScreen (Compose).
  ui/panel/                The popups: PanelActivity (state) and PanelScreen (Compose).
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
  tested on the JVM. Example: `settings/SettingsController.kt`, tested with a fake `RawSettings`.
- **UI state**: small screens use `rememberSaveable`. When a screen gets real logic or data,
  add a `ViewModel` (`androidx.lifecycle:lifecycle-viewmodel-compose`) that exposes a
  `StateFlow<UiState>` and calls into the plain-Kotlin logic. Record it in `decisions.md`.
- **Navigation**: the main screen's two extra pages (Permissions, Instructions) are a saved
  page state with a back handler, not Navigation Compose (see `decisions.md`). With more
  screens, use Navigation Compose.
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
