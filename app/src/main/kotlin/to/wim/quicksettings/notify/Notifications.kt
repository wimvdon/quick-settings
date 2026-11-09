package to.wim.quicksettings.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import to.wim.quicksettings.R
import to.wim.quicksettings.dns.DnsState
import to.wim.quicksettings.quickSettingsApp
import to.wim.quicksettings.screen.BrightnessCurve
import to.wim.quicksettings.screen.ScreenState
import to.wim.quicksettings.ui.main.MainActivity
import to.wim.quicksettings.ui.panel.PanelActivity
import to.wim.quicksettings.ui.timeoutText

/**
 * The persistent notifications, each with a button that puts the setting back:
 * manual brightness and the long screen timeout (channel "Display"), and Private DNS on
 * Automatic (channel "Private DNS"). Plain ongoing notifications, no service.
 */
object Notifications {
    private const val CHANNEL_DISPLAY = "display"
    private const val CHANNEL_DNS = "private_dns"
    private const val ID_BRIGHTNESS = 1
    private const val ID_TIMEOUT = 2
    private const val ID_DNS = 3

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        listOf(
            CHANNEL_DISPLAY to R.string.notification_channel_display,
            CHANNEL_DNS to R.string.notification_channel_dns,
        ).forEach { (id, name) ->
            // LOW: silent, but still shows the status bar icon.
            manager.createNotificationChannel(
                NotificationChannel(id, context.getString(name), NotificationManager.IMPORTANCE_LOW)
                    .apply { setShowBadge(false) },
            )
        }
    }

    /** True when the app may post notifications (runtime permission on Android 13+). */
    fun canPost(context: Context): Boolean {
        val permitted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Shows exactly the display notifications that match [state]. */
    fun updateDisplay(
        context: Context,
        state: ScreenState,
    ) {
        val open = PanelActivity.intent(context)
        show(
            context,
            ID_BRIGHTNESS,
            if (state.autoBrightness) {
                null
            } else {
                build(
                    context,
                    channel = CHANNEL_DISPLAY,
                    icon = R.drawable.ic_brightness,
                    title = context.getString(R.string.notification_brightness_title),
                    text = context.getString(R.string.percent, BrightnessCurve.toPercent(state.brightness)),
                    action = context.getString(R.string.notification_brightness_action),
                    reset = ResetReceiver.RESET_BRIGHTNESS,
                    open = open,
                )
            },
        )
        show(
            context,
            ID_TIMEOUT,
            if (!state.timeoutExtended) {
                null
            } else {
                build(
                    context,
                    channel = CHANNEL_DISPLAY,
                    icon = R.drawable.ic_timer,
                    title = context.getString(R.string.notification_timeout_title),
                    text = timeoutText(context.resources, state.timeoutMs),
                    action = context.getString(R.string.notification_timeout_action),
                    reset = ResetReceiver.RESET_TIMEOUT,
                    open = open,
                )
            },
        )
    }

    /** Shows the Private DNS reminder while it is on Automatic and a hostname can be used. */
    fun updateDns(
        context: Context,
        state: DnsState,
    ) {
        show(
            context,
            ID_DNS,
            if (!state.showNotification || !state.hasHostname) {
                null
            } else {
                build(
                    context,
                    channel = CHANNEL_DNS,
                    icon = R.drawable.ic_dns,
                    title = context.getString(R.string.notification_dns_title),
                    text = context.getString(R.string.notification_dns_text, state.hostname),
                    action = context.getString(R.string.notification_dns_action),
                    reset = ResetReceiver.RESET_DNS,
                    open = MainActivity.intent(context),
                )
            },
        )
    }

    @SuppressLint("MissingPermission") // Checked in canPost(); SecurityException is caught too.
    private fun show(
        context: Context,
        id: Int,
        notification: Notification?,
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (notification == null) {
            manager.cancel(id)
            return
        }
        if (!canPost(context)) return
        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }

    private fun build(
        context: Context,
        channel: String,
        @DrawableRes icon: Int,
        title: String,
        text: String,
        action: String,
        reset: String,
        open: Intent,
    ): Notification {
        val openIntent =
            PendingIntent.getActivity(
                context,
                reset.hashCode(),
                open,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val resetIntent =
            PendingIntent.getBroadcast(
                context,
                reset.hashCode(),
                Intent(context, ResetReceiver::class.java).setAction(reset),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        return NotificationCompat
            .Builder(context, channel)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openIntent)
            .addAction(icon, action, resetIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setLocalOnly(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}

/** Handles the notifications' buttons. Not exported: only our PendingIntents reach it. */
class ResetReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val app = context.quickSettingsApp
        when (intent.action) {
            RESET_BRIGHTNESS -> app.controller.reset(ScreenState.Change.Brightness)
            RESET_TIMEOUT -> app.controller.reset(ScreenState.Change.Timeout)
            RESET_DNS -> app.dns.useHostname()
            else -> return
        }
        app.sync()
    }

    companion object {
        const val RESET_BRIGHTNESS = "to.wim.quicksettings.RESET_BRIGHTNESS"
        const val RESET_TIMEOUT = "to.wim.quicksettings.RESET_TIMEOUT"
        const val RESET_DNS = "to.wim.quicksettings.RESET_DNS"
    }
}
