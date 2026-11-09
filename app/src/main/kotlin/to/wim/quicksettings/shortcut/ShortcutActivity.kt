package to.wim.quicksettings.shortcut

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import to.wim.quicksettings.R
import to.wim.quicksettings.quickSettingsApp
import to.wim.quicksettings.screen.SystemScreenSettings
import to.wim.quicksettings.ui.main.MainActivity

/**
 * Invisible activity behind the launcher (long-press) shortcuts, which also work with Quick Tap
 * and other gesture apps. It switches one setting, confirms with a toast and finishes at once
 * (Theme.NoDisplay). Without the permission a shortcut needs, it opens the main screen instead.
 */
class ShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = quickSettingsApp
        val needsSecure = intent?.action in setOf(ACTION_DNS, ACTION_ALWAYS_ON, ACTION_TAP_TO_WAKE)
        val allowed =
            if (needsSecure) SystemScreenSettings.canWriteSecure(this) else SystemScreenSettings.canWriteSystem(this)
        val message: Int? =
            when {
                intent?.action !in ALL -> null
                !allowed -> {
                    startActivity(MainActivity.intent(this))
                    null
                }
                intent?.action == ACTION_DNS ->
                    if (!app.dns.toggle()) {
                        R.string.main_dns_no_hostname
                    } else if (app.dns.read().tileActive) {
                        R.string.toast_dns_hostname
                    } else {
                        R.string.toast_dns_automatic
                    }
                intent?.action == ACTION_TIMEOUT ->
                    if (app.controller.toggleTimeout()) R.string.toast_timeout_long else R.string.toast_timeout_default
                intent?.action == ACTION_ALWAYS_ON ->
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

    companion object {
        // Also named in res/xml/shortcuts.xml (and the debug copy).
        const val ACTION_DNS = "to.wim.quicksettings.action.TOGGLE_PRIVATE_DNS"
        const val ACTION_TIMEOUT = "to.wim.quicksettings.action.TOGGLE_TIMEOUT"
        const val ACTION_ALWAYS_ON = "to.wim.quicksettings.action.TOGGLE_ALWAYS_ON"
        const val ACTION_TAP_TO_WAKE = "to.wim.quicksettings.action.TOGGLE_TAP_TO_WAKE"
        private val ALL = setOf(ACTION_DNS, ACTION_TIMEOUT, ACTION_ALWAYS_ON, ACTION_TAP_TO_WAKE)
    }
}
