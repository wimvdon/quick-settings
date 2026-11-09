package to.wim.quicksettings.shortcut

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Target of the launcher shortcuts (res/xml/shortcuts.xml), which also work with Quick Tap.
 *
 * Android starts every manifest shortcut with FLAG_ACTIVITY_TASK_ON_HOME, so when its task closes
 * the home screen shows instead of the app the user was in. This invisible relay therefore does no
 * work itself: it starts [ShortcutActionActivity] in a task of its own, without that flag, and
 * closes. When the action closes, Android returns to the app the user was in.
 */
class ShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action in ALL) {
            startActivity(
                Intent(intent.action)
                    .setClass(this, ShortcutActionActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        finish()
    }

    companion object {
        // Also named in res/xml/shortcuts.xml (and the debug copy).
        const val ACTION_DNS = "to.wim.quicksettings.action.TOGGLE_PRIVATE_DNS"
        const val ACTION_TIMEOUT = "to.wim.quicksettings.action.TOGGLE_TIMEOUT"
        const val ACTION_ALWAYS_ON = "to.wim.quicksettings.action.TOGGLE_ALWAYS_ON"
        const val ACTION_TAP_TO_WAKE = "to.wim.quicksettings.action.TOGGLE_TAP_TO_WAKE"
        val ALL = setOf(ACTION_DNS, ACTION_TIMEOUT, ACTION_ALWAYS_ON, ACTION_TAP_TO_WAKE)
    }
}
