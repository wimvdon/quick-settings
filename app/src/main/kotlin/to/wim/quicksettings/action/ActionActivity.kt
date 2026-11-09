package to.wim.quicksettings.action

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import to.wim.quicksettings.quickSettingsApp
import to.wim.quicksettings.settings.TileKind

/**
 * Invisible activity behind the Keyboard and Volume tiles. Starting it closes the shade, so
 * Android's own picker or volume panel shows on top. The keyboard picker needs a focused window,
 * so this stays (transparent) until the picker closes.
 */
class ActionActivity : Activity() {
    private var pickerShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action == ACTION_VOLUME) {
            quickSettingsApp.audio.showPanel()
            finish()
        } else if (intent?.action != ACTION_KEYBOARD) {
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || isFinishing) return
        if (pickerShown) {
            // Focus came back: the picker was closed.
            finish()
        } else {
            pickerShown = true
            getSystemService(InputMethodManager::class.java)?.showInputMethodPicker() ?: finish()
        }
    }

    companion object {
        private const val ACTION_KEYBOARD = "to.wim.quicksettings.action.KEYBOARD_PICKER"
        private const val ACTION_VOLUME = "to.wim.quicksettings.action.VOLUME_PANEL"

        fun intent(
            context: Context,
            kind: TileKind,
        ): Intent =
            Intent(context, ActionActivity::class.java)
                .setAction(if (kind == TileKind.Volume) ACTION_VOLUME else ACTION_KEYBOARD)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
