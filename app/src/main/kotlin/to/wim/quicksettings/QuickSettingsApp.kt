package to.wim.quicksettings

import android.app.Application
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import to.wim.quicksettings.dns.DnsController
import to.wim.quicksettings.dns.SystemDnsSettings
import to.wim.quicksettings.notify.Notifications
import to.wim.quicksettings.screen.ScreenController
import to.wim.quicksettings.screen.SystemScreenSettings

class QuickSettingsApp : Application() {
    val controller: ScreenController by lazy { ScreenController(SystemScreenSettings(this)) }
    val dns: DnsController by lazy { DnsController(SystemDnsSettings(this)) }

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
        Notifications.createChannels(this)
        // Follows changes made anywhere (Settings, the system slider) while the process lives.
        (SystemScreenSettings.watchedUris + SystemDnsSettings.watchedUris).forEach {
            contentResolver.registerContentObserver(it, false, observer)
        }
    }

    /**
     * Makes the notifications match the current settings. Safe to call often; called after every
     * change and whenever a tile, the popup or the main screen is shown. The tiles redraw
     * themselves when Quick Settings opens and when they are tapped.
     */
    fun sync() {
        handler.removeCallbacks(syncLater)
        Notifications.updateDisplay(this, controller.read())
        Notifications.updateDns(this, dns.read())
    }
}

private const val SYNC_DELAY_MS = 300L

val Context.quickSettingsApp: QuickSettingsApp
    get() = applicationContext as QuickSettingsApp
