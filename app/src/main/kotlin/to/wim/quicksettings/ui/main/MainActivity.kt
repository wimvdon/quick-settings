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
import to.wim.quicksettings.R
import to.wim.quicksettings.notify.Notifications
import to.wim.quicksettings.quickSettingsApp
import to.wim.quicksettings.screen.SystemScreenSettings
import to.wim.quicksettings.tile.DnsTileService
import to.wim.quicksettings.tile.ScreenTileService
import to.wim.quicksettings.ui.theme.AppTheme

/** The launcher screen: how to add the tiles and grant what they need. */
class MainActivity : ComponentActivity() {
    private val uiState = mutableStateOf<MainUiState?>(null)

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
        setContent {
            AppTheme {
                uiState.value?.let { state ->
                    MainScreen(
                        state = state,
                        actions =
                            MainActions(
                                onAddScreenTile = {
                                    addTile(ScreenTileService::class.java, R.string.tile_screen_label, R.drawable.ic_tile_display)
                                },
                                onAddDnsTile = {
                                    addTile(DnsTileService::class.java, R.string.tile_dns_label, R.drawable.ic_dns)
                                },
                                onAllowSystemSettings = {
                                    open(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, "package:$packageName".toUri()))
                                },
                                onCopyCommand = ::copyAdbCommand,
                                onAllowNotifications = ::allowNotifications,
                            ),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Permissions may have been granted in Settings or with adb meanwhile.
        refresh()
        quickSettingsApp.sync()
    }

    private fun refresh() {
        uiState.value =
            MainUiState(
                canAddTilesHere = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                canWriteSystem = SystemScreenSettings.canWriteSystem(this),
                canWriteSecure = SystemScreenSettings.canWriteSecure(this),
                canPostNotifications = Notifications.canPost(this),
                adbCommand = adbCommand(),
            )
    }

    private fun addTile(
        tile: Class<*>,
        label: Int,
        icon: Int,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requestTile(tile, label, icon)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestTile(
        tile: Class<*>,
        label: Int,
        icon: Int,
    ) {
        getSystemService(StatusBarManager::class.java)?.requestAddTileService(
            ComponentName(this, tile),
            getString(label),
            Icon.createWithResource(this, icon),
            mainExecutor,
        ) { }
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

    private fun adbCommand() = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"

    private fun copyAdbCommand() {
        getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("adb", adbCommand()))
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
