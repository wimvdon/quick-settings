package io.github.impca201.screener.ui.panel

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
import io.github.impca201.screener.screen.ScreenController
import io.github.impca201.screener.screen.SystemScreenSettings
import io.github.impca201.screener.screenerApp
import io.github.impca201.screener.ui.theme.ScreenerTheme

/**
 * The popup, shaped like Android's Quick Settings Internet dialog. Opened from the tile, the
 * launcher icon and the notifications. A floating window: tapping outside it closes it.
 */
class PanelActivity : ComponentActivity() {
    private val controller: ScreenController get() = screenerApp.controller
    private val uiState = mutableStateOf<PanelUiState?>(null)
    private var askedForNotifications = false

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { screenerApp.sync() }

    private val observer =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.BOTTOM)
        setContent {
            ScreenerTheme {
                uiState.value?.let { state ->
                    PanelScreen(
                        state = state,
                        actions =
                            PanelActions(
                                onBrightness = { position ->
                                    // Called for every step of a drag: the notification and tile
                                    // follow once the drag ends.
                                    change(sync = false) { controller.setBrightnessPosition(position) }
                                },
                                onBrightnessFinished = {
                                    screenerApp.sync()
                                    askForNotifications()
                                },
                                onAutoBrightness = { change { controller.resetBrightness() } },
                                onTimeoutExtended = { extended ->
                                    change {
                                        controller.setTimeoutExtended(extended)
                                        if (extended) askForNotifications()
                                    }
                                },
                                onAlwaysOn = { change { controller.setAlwaysOn(it) } },
                                onTapToWake = { change { controller.setTapToWake(it) } },
                                onAllowSystemSettings = ::openWriteSettings,
                                onCopyCommand = ::copyAdbCommand,
                                onOpenSettings = ::openDisplaySettings,
                                onDone = ::finish,
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
        SystemScreenSettings.watchedUris.forEach { contentResolver.registerContentObserver(it, false, observer) }
        screenerApp.sync()
    }

    override fun onPause() {
        contentResolver.unregisterContentObserver(observer)
        super.onPause()
    }

    private fun refresh() {
        uiState.value =
            PanelUiState(
                screen = controller.read(),
                canWriteSystem = SystemScreenSettings.canWriteSystem(this),
                canWriteSecure = SystemScreenSettings.canWriteSecure(this),
                adbCommand = adbCommand(),
            )
    }

    private fun change(
        sync: Boolean = true,
        block: () -> Unit,
    ) {
        block()
        refresh()
        if (sync) screenerApp.sync()
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

    private fun openDisplaySettings() {
        runCatching { startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
    }

    private fun adbCommand() = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"

    private fun copyAdbCommand() {
        getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("adb", adbCommand()))
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, PanelActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
