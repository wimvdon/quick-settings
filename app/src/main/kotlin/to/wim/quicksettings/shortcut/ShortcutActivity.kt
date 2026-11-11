package to.wim.quicksettings.shortcut

import android.app.Activity
import android.os.Bundle
import to.wim.quicksettings.settings.Shortcuts
import to.wim.quicksettings.ui.panel.PanelActivity

/**
 * Invisible activity behind the four launcher (long-press) shortcuts, which also work with Quick
 * Tap and other gesture apps. Each shortcut is a slot that opens a popup (see Shortcuts) and
 * finishes at once (Theme.NoDisplay). The popup itself points to a missing permission.
 */
class ShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shortcuts.popupOf(ACTIONS.indexOf(intent?.action) + 1)?.let { startActivity(PanelActivity.intent(this, it)) }
        finish()
    }

    companion object {
        /**
         * The slots' actions, in slot order. Also named in res/xml/shortcuts.xml (and the debug
         * copy); they keep the names from when each slot had a fixed setting, so shortcuts already
         * on a home screen or set as Quick Tap keep working.
         */
        private val ACTIONS =
            listOf(
                "to.wim.quicksettings.action.TOGGLE_PRIVATE_DNS",
                "to.wim.quicksettings.action.TOGGLE_TIMEOUT",
                "to.wim.quicksettings.action.TOGGLE_ALWAYS_ON",
                "to.wim.quicksettings.action.TOGGLE_TAP_TO_WAKE",
            )
    }
}
