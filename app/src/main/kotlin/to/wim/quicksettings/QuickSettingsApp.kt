package to.wim.quicksettings

import android.app.Application
import android.content.Context
import android.content.pm.ShortcutManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import to.wim.quicksettings.data.SettingsRepository
import to.wim.quicksettings.notify.Notifications
import to.wim.quicksettings.settings.Catalog
import to.wim.quicksettings.settings.Device
import to.wim.quicksettings.settings.SettingsController
import to.wim.quicksettings.system.AndroidAudio
import to.wim.quicksettings.system.AndroidRawSettings

class QuickSettingsApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val repository: SettingsRepository by lazy { SettingsRepository(this, scope) }
    val audio: AndroidAudio by lazy { AndroidAudio(this) }
    val controller: SettingsController by lazy {
        SettingsController(Device(AndroidRawSettings(this), audio)) { repository.defaults.value }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val syncLater = Runnable { sync() }

    private val observer =
        object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                // A drag on a slider changes the value many times a second.
                handler.removeCallbacks(syncLater)
                handler.postDelayed(syncLater, SYNC_DELAY_MS)
            }
        }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        // v0.0.15 published its shortcuts from code; without this they would show twice.
        getSystemService(ShortcutManager::class.java)?.removeAllDynamicShortcuts()
        // Follows changes made anywhere (Settings, the system slider) while the process lives.
        Catalog.all.flatMap { it.keys }.distinct().forEach { (table, key) ->
            contentResolver.registerContentObserver(AndroidRawSettings.uri(table, key), false, observer)
        }
        // Once: settings new in this version start out at how the phone has them now.
        if (!repository.defaults.value.seeded) repository.setDefaults(controller.seedDefaults())
        // A changed default can light up or clear a notification.
        scope.launch { repository.defaults.drop(1).collect { sync() } }
    }

    /**
     * Makes the notifications match the current settings. Safe to call often; called after every
     * change and whenever a tile, a popup or the main screen is shown. The tiles redraw
     * themselves when Quick Settings opens and when they are tapped.
     */
    fun sync() {
        handler.removeCallbacks(syncLater)
        Notifications.update(this)
    }
}

private const val SYNC_DELAY_MS = 300L

val Context.quickSettingsApp: QuickSettingsApp
    get() = applicationContext as QuickSettingsApp
