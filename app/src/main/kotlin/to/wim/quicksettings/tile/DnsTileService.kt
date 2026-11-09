package to.wim.quicksettings.tile

import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import to.wim.quicksettings.R
import to.wim.quicksettings.dns.DnsMode
import to.wim.quicksettings.dns.DnsState
import to.wim.quicksettings.dns.SystemDnsSettings
import to.wim.quicksettings.quickSettingsApp
import to.wim.quicksettings.screen.SystemScreenSettings
import to.wim.quicksettings.ui.main.MainActivity

/**
 * Private DNS tile: tapping switches between the own hostname (tile on) and Automatic (tile off).
 * Without the adb permission or a hostname, tapping opens the main screen, which explains why.
 */
class DnsTileService : WatchingTileService() {
    override val watchedUris get() = SystemDnsSettings.watchedUris

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onClick() {
        super.onClick()
        val app = quickSettingsApp
        val current = app.dns.read()
        val target = current.toggled()
        if (!SystemScreenSettings.canWriteSecure(this) || target == null) {
            openAndCollapse(MainActivity.intent(this))
            return
        }
        // Show the new state first, so the tile reacts the moment it is tapped; then write it.
        render(current.copy(mode = target))
        app.dns.toggle()
        // The notification follows right after, without making Android rebind the tiles.
        mainHandler.post { app.syncNotifications() }
    }

    override fun render() = render(quickSettingsApp.dns.read())

    private fun render(state: DnsState) {
        val tile = qsTile ?: return
        tile.label = getString(R.string.tile_dns_label)
        tile.state = if (state.tileActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        val status =
            when (state.mode) {
                DnsMode.Hostname -> state.hostname?.takeIf { it.isNotBlank() } ?: getString(R.string.dns_hostname)
                DnsMode.Opportunistic -> getString(R.string.dns_automatic)
                DnsMode.Off -> getString(R.string.dns_off)
            }
        tile.subtitle = status
        tile.stateDescription = status
        tile.updateTile()
    }
}
