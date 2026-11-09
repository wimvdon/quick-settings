package to.wim.quicksettings.ui.panel

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import to.wim.quicksettings.quickSettingsApp
import to.wim.quicksettings.settings.Catalog
import to.wim.quicksettings.settings.Group
import to.wim.quicksettings.settings.Hostnames
import to.wim.quicksettings.settings.Level
import to.wim.quicksettings.settings.PrivateDnsSetting
import to.wim.quicksettings.settings.Tracked
import to.wim.quicksettings.system.AndroidRawSettings
import to.wim.quicksettings.tile.TilePreferencesActivity
import to.wim.quicksettings.ui.theme.AppTheme

/**
 * A popup, shaped like Android's Quick Settings Internet dialog: one per [Group], opened from its
 * tile, a notification, a long press on a single tile or the main screen. A floating window:
 * tapping outside it closes it.
 */
class PanelActivity : ComponentActivity() {
    private val app get() = quickSettingsApp
    private val uiState = mutableStateOf<PanelUiState?>(null)
    private val editingDefaults = mutableStateOf(false)
    private var group = Group.Screen
    private var askedForNotifications = false

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { app.sync() }

    private val observer =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        group = groupOf(intent)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
        // Read before the first frame, so the window never shows empty.
        refresh()
        lifecycleScope.launch { app.repository.defaults.drop(1).collect { refresh() } }
        setContent {
            AppTheme {
                uiState.value?.let { state -> PanelScreen(state = state, editingDefaults = editingDefaults.value, actions = actions) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        group = groupOf(intent)
        editingDefaults.value = false
        refresh()
    }

    override fun onResume() {
        super.onResume()
        // Permissions may have been granted in Settings or with adb meanwhile.
        refresh()
        Catalog.all.flatMap { it.keys }.distinct().forEach { (table, key) ->
            contentResolver.registerContentObserver(AndroidRawSettings.uri(table, key), false, observer)
        }
        app.sync()
    }

    override fun onPause() {
        contentResolver.unregisterContentObserver(observer)
        super.onPause()
    }

    private val actions =
        PanelActions(
            onToggle = { toggle, on -> change { app.controller.set(toggle, on) } },
            onSelect = { choice, value -> change { app.controller.select(choice, value) } },
            // Called for every step of a drag: notifications and tiles follow once it ends.
            onLevel = { level, position -> change(sync = false) { app.controller.setLevel(level, position) } },
            onLevelFinished = { change { } },
            onWrite = { setting, value -> change { app.controller.write(setting, value) } },
            onSetDefault = { setting, value -> app.repository.setDefault(setting, value) },
            onNotify = { on -> app.repository.setNotify(group, on) },
            onUseCurrent = { app.repository.setDefaults(app.controller.currentAsDefaults(group)) },
            onAddHostname = ::addHostname,
            onRemoveHostname = { app.repository.removeHostname(it) },
            onEditDefaults = { editingDefaults.value = it },
            onReset = { change { app.controller.reset(group) } },
            onAllowSystemSettings = ::openWriteSettings,
            onCopyCommand = ::copyAdbCommand,
            onOpenSettings = { runCatching { startActivity(Intent(TilePreferencesActivity.settingsAction(group))) } },
            onDone = ::finish,
        )

    private fun refresh() {
        val defaults = app.repository.defaults.value
        val c = app.controller
        uiState.value =
            PanelUiState(
                group = group,
                rows =
                    group.rows.map { row ->
                        val tracked = row.setting as? Tracked
                        RowState(
                            row = row,
                            value = tracked?.let { c.encoded(it) },
                            default = tracked?.let { defaults.of(it) },
                            changed = tracked?.let { c.isChanged(it) } ?: false,
                            position = (row.setting as? Level)?.let { c.read(it) },
                            levelPosition = row.level?.let { c.read(it) },
                        )
                    },
                hasChanges = c.changes(group).isNotEmpty(),
                notifies = defaults.notifies(group),
                hostnames = Hostnames.merge(defaults.hostnames, c.dnsHostname()),
                savedHostnames = defaults.hostnames,
                dnsHostname = c.dnsHostname(),
                permissions = AndroidRawSettings.permissions(this),
                adbCommand = adbCommand(),
            )
    }

    private fun addHostname(input: String): Boolean {
        val name = Hostnames.clean(input)
        if (!Hostnames.isValid(name)) return false
        app.repository.addHostname(name)
        change { app.controller.write(PrivateDnsSetting, PrivateDnsSetting.hostnameValue(name)) }
        return true
    }

    private fun change(
        sync: Boolean = true,
        block: () -> Unit,
    ) {
        block()
        refresh()
        if (sync) {
            app.sync()
            if (app.repository.defaults.value.notifies(group) && app.controller.changes(group).isNotEmpty()) askForNotifications()
        }
    }

    /** The notifications need permission on Android 13+; ask the first time they're useful. */
    private fun askForNotifications() {
        if (askedForNotifications || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        askedForNotifications = true
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openWriteSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, "package:$packageName".toUri()))
        }
    }

    private fun adbCommand() = adbCommand(packageName)

    private fun copyAdbCommand() {
        getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("adb", adbCommand()))
    }

    companion object {
        private const val EXTRA_GROUP = "group"

        fun intent(
            context: Context,
            group: Group,
        ): Intent =
            Intent(context, PanelActivity::class.java)
                .putExtra(EXTRA_GROUP, group.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        private fun groupOf(intent: Intent?): Group =
            Group.entries.firstOrNull { it.name == intent?.getStringExtra(EXTRA_GROUP) } ?: Group.Screen

        /** The one-time command that grants WRITE_SECURE_SETTINGS. */
        fun adbCommand(packageName: String) = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
    }
}
