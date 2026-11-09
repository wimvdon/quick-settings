package to.wim.quicksettings.ui.panel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import to.wim.quicksettings.quickSettingsApp
import to.wim.quicksettings.screen.SystemScreenSettings

/**
 * The popup's state and what its controls do, kept apart from [PanelActivity] so the window
 * around it stays thin.
 */
class PanelModel(
    private val context: Context,
    private val host: Host,
) {
    /** What the window around the popup does. */
    interface Host {
        /** Opens another screen (a Settings page); the popup closes. */
        fun open(intent: Intent)

        fun close()

        /** Called when a change will show a notification, so its permission can be asked for. */
        fun notificationsUseful() {}
    }

    private val app = context.quickSettingsApp
    val state = mutableStateOf(read())

    private val observer =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }

    /** Follow changes made elsewhere while the popup is visible. */
    fun start() {
        refresh()
        SystemScreenSettings.watchedUris.forEach { context.contentResolver.registerContentObserver(it, false, observer) }
        app.sync()
    }

    fun stop() = context.contentResolver.unregisterContentObserver(observer)

    fun refresh() {
        state.value = read()
    }

    private fun read() =
        PanelUiState(
            screen = app.controller.read(),
            canWriteSystem = SystemScreenSettings.canWriteSystem(context),
            canWriteSecure = SystemScreenSettings.canWriteSecure(context),
            adbCommand = adbCommand(),
        )

    val actions =
        PanelActions(
            onBrightness = { position ->
                // Called for every step of a drag: the notification and tile follow once it ends.
                change(sync = false) { app.controller.setBrightnessPosition(position) }
            },
            onBrightnessFinished = {
                app.sync()
                host.notificationsUseful()
            },
            onAutoBrightness = { change { app.controller.resetBrightness() } },
            onTimeoutExtended = { extended ->
                change { app.controller.setTimeoutExtended(extended) }
                if (extended) host.notificationsUseful()
            },
            onAlwaysOn = { change { app.controller.setAlwaysOn(it) } },
            onTapToWake = { change { app.controller.setTapToWake(it) } },
            onAllowSystemSettings = {
                host.open(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, "package:${context.packageName}".toUri()))
            },
            onCopyCommand = ::copyAdbCommand,
            onOpenSettings = { host.open(Intent(Settings.ACTION_DISPLAY_SETTINGS)) },
            onDone = { host.close() },
        )

    private fun change(
        sync: Boolean = true,
        block: () -> Unit,
    ) {
        block()
        refresh()
        if (sync) app.sync()
    }

    private fun adbCommand() = "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"

    private fun copyAdbCommand() {
        context
            .getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("adb", adbCommand()))
    }
}
