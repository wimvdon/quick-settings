package to.wim.quicksettings.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import to.wim.quicksettings.R
import to.wim.quicksettings.dns.DnsMode
import to.wim.quicksettings.quickSettingsApp
import to.wim.quicksettings.screen.SystemScreenSettings
import to.wim.quicksettings.ui.main.MainActivity

/**
 * Private DNS tile: tapping switches between the own hostname (tile on) and Automatic (tile off).
 * Without the adb permission or a hostname, tapping opens the main screen, which explains why.
 */
class DnsTileService : TileService() {
    override fun onTileAdded() {
        super.onTileAdded()
        render()
    }

    override fun onStartListening() {
        super.onStartListening()
        render()
        quickSettingsApp.syncNotifications()
    }

    override fun onClick() {
        super.onClick()
        val app = quickSettingsApp
        if (!SystemScreenSettings.canWriteSecure(this) || !app.dns.toggle()) {
            openAndCollapse(MainActivity.intent(this))
            return
        }
        render()
        app.sync()
    }

    private fun render() {
        val tile = qsTile ?: return
        val state = quickSettingsApp.dns.read()
        tile.label = getString(R.string.tile_dns_label)
        tile.state = if (state.tileActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        val status =
            when (state.mode) {
                DnsMode.Hostname -> state.hostname.orEmpty()
                DnsMode.Opportunistic -> getString(R.string.dns_automatic)
                DnsMode.Off -> getString(R.string.dns_off)
            }
        tile.subtitle = status
        tile.stateDescription = status
        tile.updateTile()
    }
}
