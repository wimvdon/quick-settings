package io.github.impca201.screener.notify

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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.impca201.screener.R
import io.github.impca201.screener.screen.BrightnessCurve
import io.github.impca201.screener.screen.ScreenState
import io.github.impca201.screener.screenerApp
import io.github.impca201.screener.ui.panel.PanelActivity
import io.github.impca201.screener.ui.timeoutText

/**
 * The persistent notifications for manual brightness and the extended screen timeout, each
 * with a button that puts the setting back. Plain ongoing notifications (no service).
 */
object ScreenNotifications {
    private const val CHANNEL_ID = "changed_settings"
    private const val ID_BRIGHTNESS = 1
    private const val ID_TIMEOUT = 2

    fun createChannel(context: Context) {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                // LOW: silent, but still shows the status bar icon.
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** True when the app may post notifications (runtime permission on Android 13+). */
    fun canPost(context: Context): Boolean {
        val permitted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Shows exactly the notifications that match [state]. */
    fun update(
        context: Context,
        state: ScreenState,
    ) {
        show(
            context,
            ID_BRIGHTNESS,
            if (state.autoBrightness) {
                null
            } else {
                build(
                    context,
                    title = context.getString(R.string.notification_brightness_title),
                    text = context.getString(R.string.percent, BrightnessCurve.toPercent(state.brightness)),
                    action = context.getString(R.string.notification_brightness_action),
                    change = ScreenState.Change.Brightness,
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
                    title = context.getString(R.string.notification_timeout_title),
                    text = timeoutText(context.resources, state.timeoutMs),
                    action = context.getString(R.string.notification_timeout_action),
                    change = ScreenState.Change.Timeout,
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
        title: String,
        text: String,
        action: String,
        change: ScreenState.Change,
    ): Notification {
        val open =
            PendingIntent.getActivity(
                context,
                0,
                PanelActivity.intent(context),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val reset =
            PendingIntent.getBroadcast(
                context,
                change.ordinal,
                Intent(context, ResetReceiver::class.java).putExtra(ResetReceiver.EXTRA_CHANGE, change.name),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        return NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .addAction(R.drawable.ic_close, action, reset)
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

/** Handles the notifications' reset buttons. Not exported: only our PendingIntents reach it. */
class ResetReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val name = intent.getStringExtra(EXTRA_CHANGE) ?: return
        val change = ScreenState.Change.entries.firstOrNull { it.name == name } ?: return
        val app = context.screenerApp
        app.controller.reset(change)
        app.sync()
    }

    companion object {
        const val EXTRA_CHANGE = "change"
    }
}
