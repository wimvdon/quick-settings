package to.wim.quicksettings.tile

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import to.wim.quicksettings.settings.Group
import to.wim.quicksettings.settings.TileId
import to.wim.quicksettings.settings.TileKind
import to.wim.quicksettings.ui.panel.PanelActivity

/**
 * Long-pressing a tile opens this (QS_TILE_PREFERENCES). It shows nothing: a popup's tile opens
 * the matching Android settings page; a single tile opens the popup its setting is in (which has a
 * Settings button too). The keyboard tile opens Android's keyboard settings.
 */
class TilePreferencesActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION") // The typed overload needs API 33; minSdk is 31.
        val component = intent.getParcelableExtra<ComponentName>(Intent.EXTRA_COMPONENT_NAME)
        val tile = TileId.entries.firstOrNull { serviceOf(it).name == component?.className }
        val kind = tile?.kind
        val popup = tile?.popup
        val target =
            when {
                kind is TileKind.Popup -> Intent(settingsAction(kind.group))
                popup != null -> PanelActivity.intent(this, popup)
                kind == TileKind.Keyboard -> Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                else -> Intent(Settings.ACTION_SETTINGS)
            }
        runCatching { startActivity(target) }
        finish()
    }

    companion object {
        /** The Android settings page that belongs to a popup. */
        fun settingsAction(group: Group): String =
            when (group) {
                Group.Screen -> Settings.ACTION_DISPLAY_SETTINGS
                Group.Colour -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                Group.Sound -> Settings.ACTION_SOUND_SETTINGS
                Group.Developer -> Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
                Group.Network -> Settings.ACTION_WIRELESS_SETTINGS
            }
    }
}
