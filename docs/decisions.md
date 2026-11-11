# Decision log

Newest first. Add an entry for every notable choice: what was decided, why, and what was rejected.

## 2026-11-11: The shortcuts open the popups; wide tiles list what they bundle

The owner asked for the launcher shortcuts to be the popups, all but Developer (#24). That is
exactly four, the number of manifest slots, so each slot now opens one popup (Screen, Color,
Sound, Network) and carries its popup's name and icon instead of "Shortcut 1" to "4". The
choice per slot (⋮ › Shortcuts) is gone: with a fixed popup per slot there is nothing to choose,
and a real name beats a numbered slot. Ids and actions stay, so Quick Tap and home-screen
shortcuts keep working and now open the popup. Stored slot choices are removed on the next write.

On the main screen a popup's wide tile looked like any other tile (#25). It now lists the
settings its popup shows under its name (as many as fit on two lines), which says it is a group
that opens a popup. The hint text above the tiles is gone at the owner's request; the
Instructions page still explains adding tiles.

## 2026-11-10: Main screen by section, set up per tile, shortcut slots

The owner asked for a main screen with a wide tile per popup and its single tiles below, each
with a tune button and a plus. The tune button replaces the popup's defaults mode, so defaults
live in one place: a setting's sheet has its default, whether it notifies (now per setting, the
per-popup choice stays as the fallback) and which launcher shortcut taps it. A popup's sheet
chooses the rows the popup shows. The popup's own tune button opens that sheet in the app.

The shortcuts stay the four manifest shortcuts, because Quick Tap only offers those (see the
2026-11-09 entries). Android can't relabel a manifest shortcut, so they are now "Shortcut 1" to
"Shortcut 4" with one icon, and the toast says what changed. Their ids and actions are unchanged,
so shortcuts on a home screen or set as Quick Tap keep working, and each slot starts with the
setting it had.

A clean install now takes every default from the phone, also for the five screen and network
settings that kept fixed defaults so far. Existing installs are not seeded again.

## 2026-11-09: Every possible tile, popups with defaults, DataStore, a new main screen

The owner asked for every tile an app can offer, popups that group related settings, and
defaults they can set. One tested catalogue (`settings/Catalog.kt`) now describes every setting
(where Android keeps it, what it needs, its default); popups, tiles, notifications and the main
screen are all built from it, so a new setting is one entry plus its strings.

- **Popups**: Screen (as before, plus rotation; the timeout offers 1, 5 and 30 min), Color,
  Sound, Developer and Network (Private DNS). Single tiles for what is one tap. Each popup shows
  what differs from its default, has Reset, and a defaults mode (tune button) with "Use current"
  and a per-popup notification switch.
- **Notifications** are now one per popup with a Reset button, replacing the separate brightness,
  timeout and Private DNS ones. On by default for Screen, Developer and Network; off for Color
  (Night Light usually runs on a schedule) and Sound.
- **Defaults on update**: the first start stores the phone's current values as the defaults of
  every new setting, so an update lights nothing up. The four screen settings and Private DNS
  keep the defaults earlier versions had.
- **Stored data**: DataStore (already in the version catalog; the template's rules name it for
  storage) is added as a dependency. Defaults, notification choices and saved hostnames go in the
  backed-up `backed_up_settings`; which tiles are added in `device_state` (only true on this
  phone). `PRIVACY.md` says so.
- **Private DNS hostnames**: the app now saves hostnames and can switch between them, which the
  owner wanted for the Network popup. This reverses "no in-app hostname field" (2026-11-09):
  switching between several providers can't be done in Android's dialog without retyping.
- **Main screen**: Tiles grouped by category and Popups, drawn like large Quick Settings tiles
  with their live state; tap adds (Android 13+), long-press opens the popup. Permissions and
  Instructions moved to the overflow menu as pages (a saved page state and a back handler, not
  Navigation Compose, which would be a new dependency for two pages); a banner shows while a
  permission is missing. The adb guide uses Snoozer's wording, plus what adb is and a link to
  Google's download page (opened in the browser; the app still has no internet access).
- **Left out**: show taps, pointer location, mono audio and smooth display are private System
  settings that Android refuses to apps even with `WRITE_SECURE_SETTINGS` (only shell or system
  apps); mobile data and airplane mode need system permissions; silent mode needs Do Not Disturb
  access and a lock screen or screenshot tile an accessibility service, both new permissions the
  owner didn't ask for. Night Light, extra dim and color inversion have no single tiles because
  Android has its own.
- **Not verified on a phone yet**: wireless debugging and touch vibration (some Android versions
  may ignore the key the app writes); the test steps in the pull request cover them.

## 2026-11-09: Timeout as a segmented button, popup without opening animation, no Quick Tap guide

The screen timeout is a choice between two values, not on/off, so the popup shows it as a
Material 3 segmented button `1 min | 30 min` (`ScreenState.timeoutChoice`; a value set
elsewhere selects neither and is shown as text). Rejected: chips (less clearly one-of-two), a
row that cycles (you can't see what a tap does) and a choice dialog (two taps).
The popup flashed when opened from the tile: Android closes the shade while the popup's window
animation plays. The popup theme now has no window animation (`windowAnimationStyle` `@null`).
Rejected: waiting until the shade has closed (more code; an empty window first, as before).
The Quick Tap guide is removed from the main screen at the owner's request; the shortcuts still
work with it. Dutch uses "Scherm altijd aan" for always-on display.

## 2026-11-09: Shortcuts back in the manifest, no relay

Pixel Quick Tap shows no settings gear for an app without manifest shortcuts: the dynamic
shortcuts of v0.0.15 could not be chosen at all. The owner chose the gear over staying in the open
app, so the four shortcuts are back in `res/xml/shortcuts.xml` (and the debug copy) with their
original ids, pointing straight at `ShortcutActivity`. After a Quick Tap the home screen shows,
because Android adds `FLAG_ACTIVITY_TASK_ON_HOME` to manifest shortcuts. The relay is not brought
back: it made no difference. `QuickSettingsApp` removes the dynamic shortcuts v0.0.15 published.
Rejected: a separate app-list entry per action (Quick Tap would open it without the home screen,
but it adds icons to the app drawer).

## 2026-11-09: Shortcuts published from code again; the relay is gone

The relay below did not help: Quick Tap still showed the home screen. AOSP `Task.moveToFront`
moves the home screen behind any task whose first intent has `FLAG_ACTIVITY_NEW_TASK |
FLAG_ACTIVITY_TASK_ON_HOME` the moment that task starts, so the relay's own task already put home
in between. Only shortcuts that never get `TASK_ON_HOME` avoid this: dynamic shortcuts published
with `ShortcutManager` (`Shortcuts.publish`, at app start when they differ and every time the main
screen shows). Their ids differ from the old manifest ids (a pinned manifest shortcut stays locked
under its id). `ShortcutActionActivity` and both copies of `res/xml/shortcuts.xml` are removed.
Open question: the first try with dynamic shortcuts could not be chosen in Quick Tap; the app now
has to be opened once after installing so the shortcuts exist before picking one.
Rejected: a usage-access permission to bring the previous app back (the owner doesn't want it).

## 2026-11-09: Shortcuts go through a relay, so Quick Tap returns to the open app

With a shortcut set as Pixel Quick Tap, the app the owner was in disappeared and the launcher
showed. Android adds `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_TASK_ON_HOME`
to every manifest shortcut (AOSP `ShortcutParser`). That flag is only stored on the task at launch
(`ActivityStarter`) and read when the task finishes (`Task.returnsToHomeRootTask`). So the manifest
shortcut now targets an invisible relay, `ShortcutActivity`, which starts `ShortcutActionActivity`
in a task of its own (its own `taskAffinity`) with only `FLAG_ACTIVITY_NEW_TASK`, and closes; the
action does the work and closes, and Android returns to the app the user was in. A first attempt
with dynamic shortcuts (published from code) was reverted: Pixel Quick Tap only offers manifest
shortcuts. The shortcut ids are the original ones again, so existing Quick Tap choices keep working.

## 2026-11-09: No tile refresh requests; app name translated

`TileService.requestListeningState` only reaches tiles declared as active tiles; ours are not, so
Android ignored the call after every change. It is removed: the tiles redraw when Quick Settings
opens and when they are tapped. The app name follows each language's own word for Quick Settings
(Snelle instellingen, Schnelleinstellungen, Réglages rapides, Ajustes rápidos, Impostazioni
rapide).

## 2026-11-09: Tiles and popup back to the simple v0.0.4 code

The live-update attempts (tiles watching their settings while the shade is open, drawing the DNS
tile before writing, `PanelModel`) still left a visible delay on the owner's phone, and the
popup flashed when it opened. Android decides when a tile's new state appears, so the extra code
bought little. The tiles redraw when Quick Settings opens and after their own tap again, and
`PanelActivity` is back to its v0.0.4 form (with the status line still removed).

Update, same day: the popup still flashed. Its settings were only read in `onResume`, so the
window's standard animation played on an empty window and the card appeared a moment later. They
are now read in `onCreate`, before the first frame; the animation itself is Android's default.

Update, same day: the owner asked to go back to exactly how the app was after the popup was
centred (v0.0.4), with no added code. All app code is now the v0.0.4 code again; only the popup's
status line stays removed, as asked earlier.

## 2026-11-09: Back to the popup activity; the shade can't stay open

Update, same day: on the owner's phone the shade still closed with `TileService.showDialog`.
Android closes the shade itself when an app's tile shows anything, and an app can't draw above
the shade; only System UI's own tiles (such as Internet) can show a dialog over it. The dialog
gave no benefit, and it couldn't ask for the notification permission, so the tile opens
`PanelActivity` again (`PanelDialog.kt` removed; `PanelModel` stays). The Private DNS tile now
draws its new state before writing the setting and refreshes the notification afterwards, without
asking Android to rebind the tiles, so it reacts to a tap at once.

## 2026-11-09: The Screen tile shows its popup over the open shade

Like Android's own Internet dialog, the popup now opens on top of Quick Settings instead of
closing the shade and starting an activity: `TileService.showDialog` with a `ComponentDialog`,
which gives Compose its lifecycle inside the service. The tiles stay visible and redraw live as
settings change. State and actions live in `PanelModel`, shared with `PanelActivity`, which is
still used when the popup is opened from a notification (the shade is closed then). The
expand-from-tile animation stays out of reach: `showDialog` gets the standard dialog animation,
because the tile's position and System UI's animator aren't available to apps. A service can't
ask for the notification permission, so the dialog sends the user to the main screen when a
change needs notifications and they are off.

## 2026-11-09: The popup uses Android's standard dialog animation

Update, same day: the custom grow-in animation looked wonky on the owner's phone, so it was
removed. The popup now opens with the dialog theme's own window animation, which every Android
version draws reliably. The reasoning below about the expand-from-tile animation still holds.

## 2026-11-09: The popup grows in itself; the real expand-from-tile animation isn't available

Android's Internet dialog expands out of its tile with System UI's own dialog animator, which
only System UI can use: an app's tile can only start an activity (`startActivityAndCollapse`) or
show a plain dialog (`TileService.showDialog`), and neither gets the tile's position or that
animation. The popup therefore grows in from the top of the screen, where the tile was, with
Material's emphasized-decelerate easing while the dim fades in. Rejected: `showDialog` (no
animation either, and Compose in a service dialog needs hand-made lifecycle owners).

## 2026-11-09: Screener becomes Quick Settings (`to.wim.quicksettings`) with a Private DNS tile

The owner wants one app for their own Quick Settings tiles, so Screener is renamed Quick Settings
and gets a second tile, Private DNS, and a main screen with setup instructions. The application ID
changes to `to.wim.quicksettings`: v0.0.1 and v0.0.2 were released as `to.wim.screener`, but only
the owner installed them, so the old app is simply uninstalled. Private DNS (`Settings.Global`)
needs the same adb-granted `WRITE_SECURE_SETTINGS` as always-on display, so no new permission.
The tile toggles hostname ↔ Automatic and never edits the hostname: Android keeps it when
switching to Automatic, and typing a hostname belongs in Android's own dialog. Notifications are
split into a Display and a Private DNS channel so either can be silenced on its own. Long-pressing
a tile goes through one trampoline activity that reads which tile was pressed. The Private DNS notification shows whenever the hostname isn't in use (Off or Automatic). Four
static launcher shortcuts (like Snoozer's) toggle Private DNS, the timeout, always-on display and
tap to wake, so Quick Tap and gesture apps can use them. Rejected: a
separate app per tile (two installs, two adb grants) and an in-app hostname field (Android's
Settings already has one and validates it).

## 2026-11-09: Screener uses Android's dynamic colours, not Snoozer's palette

The owner wants the popup to look like Android's own Quick Settings, so it uses Material 3
dynamic colours (from the wallpaper) instead of Snoozer's fixed blue palette. This differs from
the shared rule that dynamic color is off by default; Screener has no settings screen, so there
is no switch either. The icon keeps the shared icon style, with its own gradient (see `DESIGN.md`, "This app"). Rejected: a dynamic color switch (it
would need a settings screen for one option).

## 2026-11-09: Screener: a tile and a popup, three permissions, no stored data

The template becomes Screener (`to.wim.quicksettings`), Snoozer's sister app (at first with the same
colours; see the entry above). What it changes decides its permissions: brightness and screen timeout need "Modify
system settings" (`WRITE_SETTINGS`, the user grants it in Settings); always-on display and tap to
wake are secure settings, which only `WRITE_SECURE_SETTINGS` may write, and only adb can grant
that (one command, shown in the popup with a Copy button); the persistent notifications need
`POST_NOTIFICATIONS` on Android 13+. No network.

- The popup is a floating activity (dialog theme, at the bottom, dims behind it) opened with
  `startActivityAndCollapse`. Rejected: `TileService.showDialog` (keeps the shade open, but Compose
  in a service's dialog needs hand-made lifecycle owners).
- Notifications are plain ongoing notifications, kept in step by a `ContentObserver` while the
  process lives and re-checked whenever the tile or popup is shown. Rejected: a foreground service
  (a permanent extra notification and battery use for very little).
- Tap to wake writes both `doze_tap_gesture` (Pixel's "Tap to check phone") and
  `double_tap_to_wake` (AOSP's "Tap to wake"), because phones use different keys; the state is
  read from the Pixel key.
- Application ID `to.wim.quicksettings`: the owner's apps use `to.wim.<name>`. Set before the first
  release (the first release attempt stopped at the missing signing secrets), so no phone has
  the earlier `io.github.impca201.screener`.
- No DataStore: the defaults are fixed, and everything else is Android's own settings.
- No settings screen, so no Theme / Dynamic color choice (`DESIGN.md`, "This app").

## 2026-11-01: Claude Code sessions don't build the APK before pushing

Sessions ran `./gradlew testDebugUnitTest lintDebug assembleDebug` before every push, and CI then
runs the same and more. The APK built in the session was never used: the owner installs the one
from CI. Sessions now run only the unit tests and lint, which catch nearly every problem and
compile the app's code and resources. CI still builds the debug and release APKs, so a packaging
problem still turns the pull request red. Rejected: no local checks at all (every test or lint
failure would then cost a CI round trip).

## 2026-10-17: Design review across the apps: one way to reorder, readable widths, shared wording

A Material 3 review of every app built from this template found the same things done in different ways. Lists in the user's own order used four controls (drag handles, a long press, chevrons and arrows), even within one app; navigation bars mixed filled and outlined icons differently; most lists stretched across tablets although `DESIGN.md` asked for about 640dp; English said "Log out" next to "Sign in" and "Dismiss" where other apps said "Close"; and one app removed a workspace (with its presets) without asking. New shared rules in `DESIGN.md`: "Reordering" (a drag handle at the end of the row, `Reorder.kt`; a long press only for grids of cards), filled selected and outlined unselected navigation icons, `Modifier.readableWidth()` (`Layout.kt`), and wording for Close, Done, Sign out, Reorder and Move up / Move down. The two new kit files are copied into every app (Snoozer, without lists, takes only `Layout.kt`). The template now ships the same six languages as the apps. Rejected: up and down arrows for short lists (easier to build, but a second way to do the same thing) and a long press on list rows (hidden: nothing on screen says the row can move).

## 2026-10-17: Help text only where the screen doesn't speak for itself

The owner found the apps over-explanatory. A scan of every hint, summary and help paragraph removed text that restated a label, described standard controls (drag handles, tapping a row) or explained the app's design, and shortened the rest to what the screen can't show: consequences, where data goes, unfamiliar terms. `DESIGN.md` ("Wording") now says so, and the Dynamic color supporting text is one shared phrase without the app's colour name. Rejected: removing all help text (consequences and privacy notes are information, not decoration) and tooltips or info buttons instead (hidden text people rarely open, and more to translate).

## 2026-10-16: One empty state for every app, setup as a full-screen state

A second Material 3 audit found empty screens drawn three different ways: an icon in a tinted circle with a title (Tracker for Simkl), a title and text whose icon didn't show (RSS Reader), and a grey icon with one line of text (Noter for Notion). Apps that can't work until something is set up showed it differently too: Time Tracker for Notion put a small banner above a disabled timer, Noter filled the screen, Snoozer did both. Material 3 asks an empty state to say what it means and offer the next useful step, so the shared kit gained `ui/theme/EmptyState.kt` and `DESIGN.md` a section "Empty and setup states": one centred layout for empty, no-result, failed and setup-needed screens, with setup-needed replacing the screen's content. The same audit added two smaller rules: button labels name the action in as few words as possible ("Gezien", not "Markeren als gezien"), and rows that scroll sideways keep one height. Rejected: illustrations per screen (more to draw and translate for each app, and Material 3 warns against decoration that hides the next step) and keeping the banner for setup (easy to miss, and it leaves a screen full of controls that do nothing).

## 2026-10-16: Settings are one list; top-level navigation is a docked bar

An audit of the apps against Material 3 found two structural problems. Settings were a stack of collapsed cards, so even a single switch took two taps to find and every section was boxed in a card, which Material 3 lists as an anti-pattern. Settings are now one list grouped by section headers, with `ListItem` rows (switches apply at once) and only setup instructions folded away (`SettingsKit.kt`: `SettingsSection`, `SwitchSetting`, `SettingItem`, `ExpandableSetting`, `SettingsBlock`). Some apps put their top-level sections in a floating pill bar; Material 3 Expressive reserves floating toolbars for actions on the current screen, and keeps the navigation bar docked for destinations, so the shared rules now require the docked `NavigationBar`. `DESIGN.md` gained a "Navigation and structure" section. Rejected: a settings home page that links to sub-pages (more taps for apps with a handful of settings) and keeping the collapsible cards with everything expanded (still a card per section).

## 2026-10-15: Android's backup copies settings only, from an allow list

Users expect their settings back after a reset or on a new phone, and Android does that for free with its own backup (Google account) and phone-to-phone transfer. The template now lists exactly one file, the DataStore `backed_up_settings`, in `data_extraction_rules.xml`; everything else stays on the phone. An allow list means a file added later (a sign-in token, device state) is never backed up by accident, and `BackupRulesTest` fails if the rules drift. Rules for what goes where are in `docs/architecture.md`, "Backup". Rejected: backing up everything with exclusions (one forgotten exclusion leaks a token into the backup), switching backup off (users lose their settings) and our own backup to Google Drive (needs network access, sign-in and more code for what Android already does).

## 2026-10-15: Phone testing doesn't block merging

The owner can't review code and can't always test a pull request on the device right away, so pull requests waited for a manual test that adds little over CI. Green CI (unit tests, lint, builds) is now enough to merge; the owner tests afterwards with the steps from the pull request and always before a release, and problems are fixed in a new pull request. Merging to `main` does not reach users: releases stay manual. Rejected: keeping the phone test as a merge gate (pull requests pile up and go stale).

## 2026-10-15: Code is "all rights reserved" by default

Without a `LICENSE` file the legal default is already all rights reserved, but that isn't visible and is easy to misread for a public repository. The template now ships an explicit all-rights-reserved `LICENSE`, so every app starts strict and the owner opts in to sharing. Switching to a permissive licence (MIT or Apache-2.0) is a one-file swap, described in the README. Rejected: a commented-out alternative inside `LICENSE` (GitHub and licence scanners read the whole file, so it would be misdetected) and starting with MIT (opening up later is easy, taking it back for code already shared is not).

## 2026-10-14: Shared design rules have their master copy in the template

`DESIGN.md` was kept identical in the three sibling apps by hand, with no single place to start a change, and the copies had drifted (Snoozer was missing the Loading section; the Tracker for Simkl icon row was out of date in two apps). The android-app template (`wimvdon/android-app`) now holds the master copy; each app keeps a full copy so Claude sessions can read the rules without opening another repository, and only its "This app" section is its own. Rejected: keeping the file only in the template (sessions in an app would have to fetch it every time) and a shared library for the design kit (a new dependency and more setup than three small apps need). The design kit files (`ui/theme/`) stay in the apps.

## 2026-10-13: Template baseline

- **Language/UI:** Kotlin + Jetpack Compose + Material 3, single `app` module. This is Google's
  recommended, mainstream stack with the most documentation and examples.
- **Tool versions (latest stable on this date):** Gradle 9.8.1, Android Gradle Plugin 9.4.1
  (uses its built-in Kotlin support, so no separate `kotlin-android` plugin), Kotlin 2.4.21
  (Compose compiler plugin), Compose BOM 2026.09.00. Dependabot keeps these current.
- **minSdk 31 (Android 12).** The owner installs apps on their own, recent phone, so older
  Android versions aren't worth the extra code and testing. Android 12 brings Material You dynamic
  colour (no fallback code path), the modern splash screen and per-app language settings. It still
  covers the large majority of active devices. Lower it only if someone with an older phone needs
  the app.
- **compileSdk / targetSdk 37 (Android 17)**, the latest stable API level. The minor SDK release
  37.2 exists but isn't needed by anything yet.
- **Java 17 bytecode, JDK 21 to build.** JDK 21 is what Claude Code cloud sessions provide, so local
  checks match CI exactly.
- **Version numbers:** release `X.Y.Z` → `versionCode = X*10000 + Y*100 + Z`, passed in by the release
  workflow. All other builds use `git describe --tags --long` (e.g. `1.2.0-3-gabc1234`), falling back to
  `0.0.0-dev`, so a dev build can never be mistaken for a release. Version logic lives in
  `scripts/release_version.py` (unit tested) and `app/build.gradle.kts`.
- **Debug builds use `applicationIdSuffix = ".debug"`** so a pull request's APK installs next to the
  released app instead of conflicting with it.
- **Releases are GitHub Releases with a signed APK**, published by `gh release create` (preinstalled
  on runners) instead of a third-party action, which means one fewer dependency with write access.
  The R8 mapping file is attached to each release.
- **Signing secrets** are base64 and decoded by a tolerant, tested script (`scripts/decode_keystore.py`)
  that never prints the secret. The key password is checked with a small Java program
  (`scripts/VerifySigningKey.java`), because `keytool` ignores the key password of PKCS12 keystores and
  the build would otherwise fail later with an unclear error.
- **Caching:** `gradle/actions/setup-gradle` with `cache-provider: basic` (MIT licensed). The default
  "enhanced" provider is closed source under separate terms of use.
- **CI artifacts:** the debug APK is uploaded unzipped (`archive: false`) so it can be installed on a
  phone straight from the download.
- **Languages:** English (default), Dutch, French, German, Spanish and Italian, the languages every app built from this template ships. `generateLocaleConfig`
  makes the app appear under Android's per-app language settings.
- **Persistence:** DataStore when needed (version in the catalog, not added as a dependency yet). No
  network, analytics or crash-reporting libraries.
- **No instrumented/UI tests** yet: they need an emulator in CI (slow, flaky). Logic is kept in plain
  Kotlin and unit tested instead. Manual device checks are listed in `docs/testing.md`.
- **No lint baseline:** the template has zero lint issues.
- **Play Store upload** is prepared but commented out in `release.yml` until a Play Console app and
  service account exist.
