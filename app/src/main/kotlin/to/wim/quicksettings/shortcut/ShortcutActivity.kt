package to.wim.quicksettings.shortcut

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import to.wim.quicksettings.R
import to.wim.quicksettings.quickSettingsApp
import to.wim.quicksettings.system.AndroidRawSettings
import to.wim.quicksettings.tile.TileLook
import to.wim.quicksettings.ui.labelOf
import to.wim.quicksettings.ui.main.MainActivity

/**
 * Invisible activity behind the four launcher (long-press) shortcuts, which also work with Quick
 * Tap and other gesture apps. Each shortcut is a slot; it taps the tile the user chose for it
 * (see Shortcuts), confirms with a toast and finishes at once (Theme.NoDisplay). Without the
 * permission the tile needs, it opens the main screen instead.
 */
class ShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = quickSettingsApp
        val slot = ACTIONS.indexOf(intent?.action) + 1
        val tile = if (slot > 0) app.repository.defaults.value.shortcut(slot) else null
        val text =
            when {
                slot == 0 -> null
                tile == null -> getString(R.string.shortcut_empty)
                !AndroidRawSettings.permissions(this).allows(tile.access) -> {
                    startActivity(MainActivity.intent(this))
                    null
                }
                !app.controller.tap(tile) -> getString(R.string.main_dns_no_hostname)
                else -> getString(R.string.change_format, getString(labelOf(tile)), TileLook.of(this, tile).subtitle.orEmpty())
            }
        if (text != null) {
            app.sync()
            Toast.makeText(app, text, Toast.LENGTH_SHORT).show()
        }
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
