package io.github.impca201.screener

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.TileService
import io.github.impca201.screener.notify.ScreenNotifications
import io.github.impca201.screener.screen.ScreenController
import io.github.impca201.screener.screen.SystemScreenSettings
import io.github.impca201.screener.tile.ScreenTileService

class ScreenerApp : Application() {
    val controller: ScreenController by lazy { ScreenController(SystemScreenSettings(this)) }

    private val handler = Handler(Looper.getMainLooper())
    private val syncLater = Runnable { sync() }

    private val observer =
        object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                // A drag on a brightness slider changes the value many times a second.
                handler.removeCallbacks(syncLater)
                handler.postDelayed(syncLater, SYNC_DELAY_MS)
            }
        }

    override fun onCreate() {
        super.onCreate()
        ScreenNotifications.createChannel(this)
        // Follows changes made anywhere (Settings, the system slider) while the process lives.
        SystemScreenSettings.watchedUris.forEach {
            contentResolver.registerContentObserver(it, false, observer)
        }
    }

    /**
     * Makes the notifications and the tile match the current settings. Safe to call often;
     * called after every change and whenever the tile or the popup becomes visible.
     */
    fun sync() {
        handler.removeCallbacks(syncLater)
        ScreenNotifications.update(this, controller.read())
        runCatching {
            TileService.requestListeningState(this, ComponentName(this, ScreenTileService::class.java))
        }
    }
}

private const val SYNC_DELAY_MS = 300L

val Context.screenerApp: ScreenerApp
    get() = applicationContext as ScreenerApp
