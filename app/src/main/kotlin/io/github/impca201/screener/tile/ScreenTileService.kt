package io.github.impca201.screener.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import io.github.impca201.screener.R
import io.github.impca201.screener.screenerApp
import io.github.impca201.screener.ui.changeLabel
import io.github.impca201.screener.ui.panel.PanelActivity

/**
 * The Quick Settings tile. It lights up while any setting differs from its default and names
 * what is changed; tapping it opens the popup.
 */
class ScreenTileService : TileService() {
    override fun onTileAdded() {
        super.onTileAdded()
        render()
    }

    override fun onStartListening() {
        super.onStartListening()
        render()
        // Brings the notifications in step too, in case something changed while we weren't running.
        screenerApp.sync()
    }

    override fun onClick() {
        super.onClick()
        openPanel()
    }

    private fun render() {
        val tile = qsTile ?: return
        val changes = screenerApp.controller.read().changes
        tile.label = getString(R.string.tile_label)
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

    // The Intent overload is only reached below Android 14, where it is still supported.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openPanel() {
        val intent = PanelActivity.intent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
