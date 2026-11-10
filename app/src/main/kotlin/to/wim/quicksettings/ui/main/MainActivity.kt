package to.wim.quicksettings.ui.main

import android.Manifest
import android.app.StatusBarManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import to.wim.quicksettings.quickSettingsApp
import to.wim.quicksettings.settings.Group
import to.wim.quicksettings.settings.Hostnames
import to.wim.quicksettings.settings.TileId
import to.wim.quicksettings.system.AndroidRawSettings
import to.wim.quicksettings.tile.TileLook
import to.wim.quicksettings.tile.serviceOf
import to.wim.quicksettings.ui.iconOf
import to.wim.quicksettings.ui.labelOf
import to.wim.quicksettings.ui.panel.PanelActivity
import to.wim.quicksettings.ui.theme.AppTheme

/** The launcher screen: every tile and popup, ready to add and set up, plus permissions and instructions. */
class MainActivity : ComponentActivity() {
    private val uiState = mutableStateOf<MainUiState?>(null)

    /** A tune sheet asked for in the intent (a popup's own tune button). */
    private val setUpRequest = mutableStateOf<SetUp?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Android stops asking after two refusals and answers "no" at once: go to Settings then.
            if (!granted &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                openNotificationSettings()
            }
            refresh()
            quickSettingsApp.sync()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) setUpRequest.value = SetUp.parse(intent?.getStringExtra(EXTRA_SET_UP))
        refresh()
        val repository = quickSettingsApp.repository
        lifecycleScope.launch { repository.defaults.combine(repository.addedTiles) { _, _ -> }.collect { refresh() } }
        setContent {
            AppTheme {
                uiState.value?.let { state ->
                    MainScreen(
                        state = state,
                        setUpRequest = setUpRequest.value,
                        onSetUpRequestHandled = { setUpRequest.value = null },
                        actions =
                            MainActions(
                                onAddTile = ::addTile,
                                onOpenPopup = { open(PanelActivity.intent(this, it)) },
                                onSetDefault = { setting, value -> repository.setDefault(setting, value) },
                                onNotify = { setting, on -> repository.update { it.withNotify(setting, on) } },
                                onShortcut = { tile, slot -> repository.update { it.withShortcut(tile, slot) } },
                                onShowRow = { row, shown -> repository.update { it.withRow(row, shown) } },
                                onUseCurrent = { group -> repository.update { quickSettingsApp.controller.currentAsDefaults(group, it) } },
                                onAllowSystemSettings = {
                                    open(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, "package:$packageName".toUri()))
                                },
                                onCopyCommand = ::copyAdbCommand,
                                onAllowNotifications = ::allowNotifications,
                                onOpenPlatformTools = { open(Intent(Intent.ACTION_VIEW, PLATFORM_TOOLS.toUri())) },
                            ),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setUpRequest.value = SetUp.parse(intent.getStringExtra(EXTRA_SET_UP))
    }

    override fun onResume() {
        super.onResume()
        // Permissions and settings may have changed in Settings or with adb meanwhile.
        refresh()
        quickSettingsApp.sync()
    }

    private fun refresh() {
        val added = quickSettingsApp.repository.addedTiles.value
        val defaults = quickSettingsApp.repository.defaults.value
        val dnsHostname = quickSettingsApp.controller.dnsHostname()
        uiState.value =
            MainUiState(
                canAddTilesHere = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                permissions = AndroidRawSettings.permissions(this),
                tiles =
                    TileId.entries.map { tile ->
                        val look = TileLook.of(this, tile)
                        TileCard(tile, look.active, look.subtitle, added = tile in added)
                    },
                defaults = defaults,
                hostnames = Hostnames.merge(defaults.hostnames, dnsHostname),
                dnsHostname = dnsHostname,
                adbCommand = PanelActivity.adbCommand(packageName),
            )
    }

    private fun addTile(tile: TileId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requestTile(tile)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestTile(tile: TileId) {
        getSystemService(StatusBarManager::class.java)?.requestAddTileService(
            ComponentName(this, serviceOf(tile)),
            getString(labelOf(tile)),
            Icon.createWithResource(this, iconOf(tile)),
            mainExecutor,
        ) { result ->
            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
            ) {
                quickSettingsApp.repository.setAdded(tile, true)
            }
        }
    }

    private fun allowNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Permission given but notifications switched off in Settings.
            openNotificationSettings()
        }
    }

    private fun openNotificationSettings() =
        open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))

    private fun open(intent: Intent) {
        runCatching { startActivity(intent) }
    }

    private fun copyAdbCommand() {
        getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("adb", PanelActivity.adbCommand(packageName)))
    }

    companion object {
        /** Google's download page for adb (Android SDK Platform-Tools). Opens in the browser. */
        private const val PLATFORM_TOOLS = "https://developer.android.com/tools/releases/platform-tools"

        private const val EXTRA_SET_UP = "set_up"

        fun intent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        /** Opens the main screen with [group]'s tune sheet. */
        fun setUpIntent(
            context: Context,
            group: Group,
        ): Intent = intent(context).putExtra(EXTRA_SET_UP, SetUp.Popup(group).key)
    }
}
