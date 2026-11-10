package to.wim.quicksettings.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import to.wim.quicksettings.R
import to.wim.quicksettings.action.ActionActivity
import to.wim.quicksettings.quickSettingsApp
import to.wim.quicksettings.settings.Catalog
import to.wim.quicksettings.settings.Group
import to.wim.quicksettings.settings.PrivateDnsSetting
import to.wim.quicksettings.settings.TileId
import to.wim.quicksettings.settings.TileKind
import to.wim.quicksettings.settings.Toggle
import to.wim.quicksettings.system.AndroidRawSettings
import to.wim.quicksettings.ui.labelOf
import to.wim.quicksettings.ui.main.MainActivity
import to.wim.quicksettings.ui.panel.PanelActivity
import to.wim.quicksettings.ui.titleOf
import to.wim.quicksettings.ui.valueText

/**
 * Every tile is one of these, told apart by [tile] (see [TileId] for what each does). Android
 * needs a separate class per tile, hence the small subclasses below; their class names are how
 * Android remembers the tiles, so they must not be renamed.
 *
 * Tiles redraw when Quick Settings opens and after their own tap (see docs/decisions.md, "No tile
 * refresh requests").
 */
abstract class AppTileService(
    val tile: TileId,
) : TileService() {
    override fun onTileAdded() {
        super.onTileAdded()
        quickSettingsApp.repository.setAdded(tile, true)
        render()
    }

    override fun onTileRemoved() {
        quickSettingsApp.repository.setAdded(tile, false)
        super.onTileRemoved()
    }

    override fun onStartListening() {
        super.onStartListening()
        // Also marks tiles added before the app kept track.
        quickSettingsApp.repository.setAdded(tile, true)
        render()
        quickSettingsApp.sync()
    }

    override fun onClick() {
        super.onClick()
        val app = quickSettingsApp
        val kind = tile.kind
        if (!AndroidRawSettings.permissions(this).allows(tile.access)) {
            openAndCollapse(MainActivity.intent(this))
            return
        }
        when (kind) {
            is TileKind.Popup -> {
                openAndCollapse(PanelActivity.intent(this, kind.group))
                return
            }
            is TileKind.Switch, is TileKind.Cycle, TileKind.Timeout, TileKind.Dns ->
                if (!app.controller.tap(tile)) {
                    // Private DNS without any hostname: the Network popup is where one is added.
                    openAndCollapse(PanelActivity.intent(this, Group.Network))
                    return
                }
            TileKind.Keyboard, TileKind.Volume -> {
                openAndCollapse(ActionActivity.intent(this, kind))
                return
            }
        }
        render()
        app.sync()
    }

    private fun render() {
        val qs = qsTile ?: return
        val look = TileLook.of(this, tile)
        qs.label = getString(labelOf(tile))
        qs.state = if (look.active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qs.subtitle = look.subtitle
        qs.stateDescription = look.description ?: look.subtitle
        qs.updateTile()
    }
}

/** What a tile shows: lit or not, the line under its name, and a longer text for screen readers. */
data class TileLook(
    val active: Boolean,
    val subtitle: String?,
    val description: String? = null,
) {
    companion object {
        fun of(
            context: android.content.Context,
            tile: TileId,
        ): TileLook {
            val c = context.quickSettingsApp.controller
            val res = context.resources
            return when (val kind = tile.kind) {
                is TileKind.Popup -> {
                    val changes = c.changes(kind.group)
                    TileLook(
                        active = changes.isNotEmpty(),
                        subtitle =
                            when (changes.size) {
                                0 -> res.getString(R.string.tile_subtitle_default)
                                1 -> res.getString(titleOf(changes.single()))
                                else -> res.getQuantityString(R.plurals.tile_subtitle_changes, changes.size, changes.size)
                            },
                        description =
                            if (changes.isEmpty()) {
                                null
                            } else {
                                changes.joinToString(", ") { to.wim.quicksettings.ui.changeText(res, it, c.encoded(it)) }
                            },
                    )
                }
                is TileKind.Switch -> {
                    val on = c.read(kind.toggle)
                    TileLook(on, valueText(res, kind.toggle, Toggle.encode(on)))
                }
                is TileKind.Cycle ->
                    TileLook(c.isChanged(kind.choice), valueText(res, kind.choice, c.read(kind.choice), short = true))
                TileKind.Timeout ->
                    TileLook(c.isChanged(Catalog.Timeout), valueText(res, Catalog.Timeout, c.read(Catalog.Timeout)))
                TileKind.Dns -> TileLook(c.dnsOnHostname(), valueText(res, PrivateDnsSetting, c.encoded(PrivateDnsSetting)))
                TileKind.Keyboard, TileKind.Volume -> TileLook(active = false, subtitle = null)
            }
        }
    }
}

// Popups. ScreenTileService and DnsTileService keep the class names of earlier versions, so
// tiles already in Quick Settings stay.
class ScreenTileService : AppTileService(TileId.ScreenPopup)

class ColourTileService : AppTileService(TileId.ColourPopup)

class SoundTileService : AppTileService(TileId.SoundPopup)

class DeveloperTileService : AppTileService(TileId.DeveloperPopup)

class NetworkTileService : AppTileService(TileId.NetworkPopup)

// Single tiles.
class TimeoutTileService : AppTileService(TileId.Timeout)

class AlwaysOnTileService : AppTileService(TileId.AlwaysOn)

class TapToWakeTileService : AppTileService(TileId.TapToWake)

class RotationTileService : AppTileService(TileId.Rotation)

class GrayscaleTileService : AppTileService(TileId.Grayscale)

class RingerTileService : AppTileService(TileId.Ringer)

class VolumeTileService : AppTileService(TileId.Volume)

class TouchVibrationTileService : AppTileService(TileId.TouchVibration)

class HeadsUpTileService : AppTileService(TileId.HeadsUp)

class DnsTileService : AppTileService(TileId.PrivateDns)

class UsbDebuggingTileService : AppTileService(TileId.UsbDebugging)

class WirelessDebuggingTileService : AppTileService(TileId.WirelessDebugging)

class AnimationsTileService : AppTileService(TileId.Animations)

class StayAwakeTileService : AppTileService(TileId.StayAwake)

class KeyboardTileService : AppTileService(TileId.Keyboard)

/** The service class of each tile, for adding it from the main screen. */
fun serviceOf(tile: TileId): Class<out TileService> =
    when (tile) {
        TileId.ScreenPopup -> ScreenTileService::class.java
        TileId.ColourPopup -> ColourTileService::class.java
        TileId.SoundPopup -> SoundTileService::class.java
        TileId.DeveloperPopup -> DeveloperTileService::class.java
        TileId.NetworkPopup -> NetworkTileService::class.java
        TileId.Timeout -> TimeoutTileService::class.java
        TileId.AlwaysOn -> AlwaysOnTileService::class.java
        TileId.TapToWake -> TapToWakeTileService::class.java
        TileId.Rotation -> RotationTileService::class.java
        TileId.Grayscale -> GrayscaleTileService::class.java
        TileId.Ringer -> RingerTileService::class.java
        TileId.Volume -> VolumeTileService::class.java
        TileId.TouchVibration -> TouchVibrationTileService::class.java
        TileId.HeadsUp -> HeadsUpTileService::class.java
        TileId.PrivateDns -> DnsTileService::class.java
        TileId.UsbDebugging -> UsbDebuggingTileService::class.java
        TileId.WirelessDebugging -> WirelessDebuggingTileService::class.java
        TileId.Animations -> AnimationsTileService::class.java
        TileId.StayAwake -> StayAwakeTileService::class.java
        TileId.Keyboard -> KeyboardTileService::class.java
    }
