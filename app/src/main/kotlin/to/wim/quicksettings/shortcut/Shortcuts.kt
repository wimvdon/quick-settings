package to.wim.quicksettings.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.util.Log
import to.wim.quicksettings.R

/**
 * Publishes the long-press shortcuts from code (dynamic shortcuts) instead of res/xml.
 *
 * Android adds FLAG_ACTIVITY_TASK_ON_HOME to every shortcut declared in the manifest, which puts
 * the home screen behind it: after a Quick Tap the app you were in was gone. Shortcuts published
 * here keep only the flags given below, so you stay in the app you were using.
 */
object Shortcuts {
    /**
     * [force] re-publishes (for new labels after a language change); without it, only when the
     * set differs, because Android limits how often an app in the background may do this.
     */
    fun publish(
        context: Context,
        force: Boolean = false,
    ) {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        if (!force && AppShortcut.matches(manager.dynamicShortcuts.map { it.id })) return
        val shortcuts =
            AppShortcut.entries.mapIndexed { rank, shortcut ->
                ShortcutInfo
                    .Builder(context, shortcut.id)
                    .setShortLabel(context.getString(shortLabel(shortcut)))
                    .setLongLabel(context.getString(longLabel(shortcut)))
                    .setIcon(Icon.createWithResource(context, icon(shortcut)))
                    .setIntent(
                        Intent(shortcut.action)
                            .setClass(context, ShortcutActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    ).setRank(rank)
                    .build()
            }
        runCatching { manager.dynamicShortcuts = shortcuts }
            .onFailure { Log.w("QuickSettings", "Could not publish shortcuts", it) }
    }

    private fun shortLabel(shortcut: AppShortcut) =
        when (shortcut) {
            AppShortcut.PrivateDns -> R.string.shortcut_dns_short
            AppShortcut.Timeout -> R.string.shortcut_timeout_short
            AppShortcut.AlwaysOn -> R.string.shortcut_always_on_short
            AppShortcut.TapToWake -> R.string.shortcut_tap_short
        }

    private fun longLabel(shortcut: AppShortcut) =
        when (shortcut) {
            AppShortcut.PrivateDns -> R.string.shortcut_dns_long
            AppShortcut.Timeout -> R.string.shortcut_timeout_long
            AppShortcut.AlwaysOn -> R.string.shortcut_always_on_long
            AppShortcut.TapToWake -> R.string.shortcut_tap_long
        }

    private fun icon(shortcut: AppShortcut) =
        when (shortcut) {
            AppShortcut.PrivateDns -> R.drawable.ic_shortcut_dns
            AppShortcut.Timeout -> R.drawable.ic_shortcut_timer
            AppShortcut.AlwaysOn -> R.drawable.ic_shortcut_always_on
            AppShortcut.TapToWake -> R.drawable.ic_shortcut_tap
        }
}
