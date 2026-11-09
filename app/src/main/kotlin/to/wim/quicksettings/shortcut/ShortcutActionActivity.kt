package to.wim.quicksettings.shortcut

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import to.wim.quicksettings.R
import to.wim.quicksettings.quickSettingsApp
import to.wim.quicksettings.screen.SystemScreenSettings
import to.wim.quicksettings.ui.main.MainActivity

/**
 * Second step of a shortcut (see [ShortcutActivity]): switches one setting, confirms with a toast
 * and finishes at once (Theme.NoDisplay). It runs in its own task, started without
 * FLAG_ACTIVITY_TASK_ON_HOME, so when it closes Android returns to the app the user was in.
 * Without the permission a shortcut needs, it opens the main screen instead.
 */
class ShortcutActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = quickSettingsApp
        val needsSecure =
            intent?.action in setOf(ShortcutActivity.ACTION_DNS, ShortcutActivity.ACTION_ALWAYS_ON, ShortcutActivity.ACTION_TAP_TO_WAKE)
        val allowed =
            if (needsSecure) SystemScreenSettings.canWriteSecure(this) else SystemScreenSettings.canWriteSystem(this)
        val message: Int? =
            when {
                intent?.action !in ShortcutActivity.ALL -> null
                !allowed -> {
                    startActivity(MainActivity.intent(this))
                    null
                }
                intent?.action == ShortcutActivity.ACTION_DNS ->
                    if (!app.dns.toggle()) {
                        R.string.main_dns_no_hostname
                    } else if (app.dns.read().tileActive) {
                        R.string.toast_dns_hostname
                    } else {
                        R.string.toast_dns_automatic
                    }
                intent?.action == ShortcutActivity.ACTION_TIMEOUT ->
                    if (app.controller.toggleTimeout()) R.string.toast_timeout_long else R.string.toast_timeout_default
                intent?.action == ShortcutActivity.ACTION_ALWAYS_ON ->
                    if (app.controller.toggleAlwaysOn()) R.string.toast_always_on_on else R.string.toast_always_on_off
                else ->
                    if (app.controller.toggleTapToWake()) R.string.toast_tap_on else R.string.toast_tap_off
            }
        if (message != null) {
            app.sync()
            Toast.makeText(app, message, Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
