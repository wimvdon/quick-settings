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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import to.wim.quicksettings.R
import to.wim.quicksettings.quickSettingsApp
import to.wim.quicksettings.settings.Group
import to.wim.quicksettings.ui.changeText
import to.wim.quicksettings.ui.iconOf
import to.wim.quicksettings.ui.panel.PanelActivity
import to.wim.quicksettings.ui.titleOf

/**
 * One notification per popup while something in it differs from its default (for the popups
 * that notify; the user can switch each on or off in the popup's defaults). Its Reset button puts
 * everything back. Plain ongoing notifications, no service.
 */
object Notifications {
    /** Channel ids: the first two are the ones earlier versions created, kept so users' choices stay. */
    private fun channel(group: Group): String =
        when (group) {
            Group.Screen -> "display"
            Group.Network -> "private_dns"
            else -> group.name.lowercase()
        }

    private fun id(group: Group): Int =
        when (group) {
            Group.Screen -> 1
            Group.Network -> 3
            Group.Colour -> 4
            Group.Sound -> 5
            Group.Developer -> 6
        }

    /** The separate timeout notification of earlier versions. */
    private const val OLD_TIMEOUT_ID = 2

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        Group.entries.forEach { group ->
            // LOW: silent, but still shows the status bar icon.
            manager.createNotificationChannel(
                NotificationChannel(channel(group), context.getString(titleOf(group)), NotificationManager.IMPORTANCE_LOW)
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

    /** Shows exactly the notifications that match the current settings and defaults. */
    fun update(context: Context) {
        val app = context.quickSettingsApp
        val defaults = app.repository.defaults.value
        NotificationManagerCompat.from(context).cancel(OLD_TIMEOUT_ID)
        Group.entries.forEach { group ->
            val changes = if (defaults.notifies(group)) app.controller.changes(group) else emptyList()
            show(
                context,
                id(group),
                if (changes.isEmpty()) {
                    null
                } else {
                    build(
                        context,
                        group,
                        text = changes.joinToString(", ") { changeText(context.resources, it, app.controller.encoded(it)) },
                    )
                },
            )
        }
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
        group: Group,
        text: String,
    ): Notification {
        val openIntent =
            PendingIntent.getActivity(
                context,
                group.ordinal,
                PanelActivity.intent(context, group),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val resetIntent =
            PendingIntent.getBroadcast(
                context,
                group.ordinal,
                ResetReceiver.intent(context, group),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val icon = iconOf(group)
        return NotificationCompat
            .Builder(context, channel(group))
            .setSmallIcon(icon)
            .setContentTitle(context.getString(R.string.notification_changed_title, context.getString(titleOf(group))))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openIntent)
            .addAction(R.drawable.ic_restart, context.getString(R.string.panel_reset), resetIntent)
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

/** Handles the notifications' Reset buttons. Not exported: only our PendingIntents reach it. */
class ResetReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_RESET) return
        val group = Group.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_GROUP) } ?: return
        val app = context.quickSettingsApp
        app.controller.reset(group)
        app.sync()
    }

    companion object {
        private const val ACTION_RESET = "to.wim.quicksettings.RESET"
        private const val EXTRA_GROUP = "group"

        fun intent(
            context: Context,
            group: Group,
        ): Intent = Intent(context, ResetReceiver::class.java).setAction(ACTION_RESET).putExtra(EXTRA_GROUP, group.name)
    }
}
