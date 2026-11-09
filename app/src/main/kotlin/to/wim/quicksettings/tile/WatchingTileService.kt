package to.wim.quicksettings.tile

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.TileService
import to.wim.quicksettings.quickSettingsApp

/**
 * A tile that redraws the moment one of its settings changes while Quick Settings is open
 * (from a notification button, a shortcut, Android's Settings or the tile itself), instead of
 * only when the shade is opened again.
 */
abstract class WatchingTileService : TileService() {
    /** The settings this tile shows. */
    protected abstract val watchedUris: List<Uri>

    protected abstract fun render()

    private val observer =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = render()
        }
    private var watching = false

    override fun onTileAdded() {
        super.onTileAdded()
        render()
    }

    override fun onStartListening() {
        super.onStartListening()
        render()
        if (!watching) {
            watchedUris.forEach { contentResolver.registerContentObserver(it, false, observer) }
            watching = true
        }
        // Brings the notifications in step too, in case something changed while we weren't running.
        quickSettingsApp.syncNotifications()
    }

    override fun onStopListening() {
        stopWatching()
        super.onStopListening()
    }

    override fun onDestroy() {
        stopWatching()
        super.onDestroy()
    }

    private fun stopWatching() {
        if (!watching) return
        contentResolver.unregisterContentObserver(observer)
        watching = false
    }
}
