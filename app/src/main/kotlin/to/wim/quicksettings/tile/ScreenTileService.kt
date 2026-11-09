package to.wim.quicksettings.tile

import android.service.quicksettings.Tile
import to.wim.quicksettings.R
import to.wim.quicksettings.quickSettingsApp
import to.wim.quicksettings.screen.SystemScreenSettings
import to.wim.quicksettings.ui.changeLabel
import to.wim.quicksettings.ui.panel.showPanelDialog

/**
 * The Screen tile. It lights up while any setting differs from its default and names
 * what is changed; tapping it shows the popup over the open shade.
 */
class ScreenTileService : WatchingTileService() {
    override val watchedUris get() = SystemScreenSettings.watchedUris

    override fun onClick() {
        super.onClick()
        // Over the open shade, like Android's own Internet dialog.
        showPanelDialog()
    }

    override fun render() {
        val tile = qsTile ?: return
        val changes = quickSettingsApp.controller.read().changes
        tile.label = getString(R.string.tile_screen_label)
        tile.state = if (changes.isEmpty()) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.subtitle =
            when (changes.size) {
                0 -> getString(R.string.tile_subtitle_default)
                1 -> getString(changeLabel(changes.single()))
                else -> resources.getQuantityString(R.plurals.tile_subtitle_changes, changes.size, changes.size)
            }
        tile.stateDescription =
            if (changes.isEmpty()) {
                getString(R.string.tile_subtitle_default)
            } else {
                changes.joinToString(", ") { getString(changeLabel(it)) }
            }
        tile.updateTile()
    }
}
