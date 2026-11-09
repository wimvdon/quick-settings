package to.wim.quicksettings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import to.wim.quicksettings.settings.Defaults
import to.wim.quicksettings.settings.Group
import to.wim.quicksettings.settings.TileId
import to.wim.quicksettings.settings.Tracked
import java.io.IOException

/** The user's defaults, notification choices and hostnames: backed up (see docs/architecture.md, "Backup"). */
private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "backed_up_settings")

/** Which tiles are in Quick Settings: only true on this phone, so never backed up. */
private val Context.deviceStore: DataStore<Preferences> by preferencesDataStore(name = "device_state")

private val HOSTNAMES = stringSetPreferencesKey(Defaults.Keys.HOSTNAMES)
private val ADDED_TILES = stringSetPreferencesKey("added_tiles")

private fun Flow<Preferences>.ignoringReadErrors(): Flow<Preferences> =
    catch { if (it is IOException) emit(emptyPreferences()) else throw it }

/**
 * DataStore behind [Defaults] and the added tiles. Both are kept as [StateFlow]s so tiles,
 * notifications and the controller can read them at once; the first value is read when the app
 * starts (a small file).
 */
class SettingsRepository(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val settings = context.applicationContext.settingsStore
    private val device = context.applicationContext.deviceStore

    val defaults: StateFlow<Defaults> = settings.data.ignoringReadErrors().map(::toDefaults).stateInFirst(Defaults())

    val addedTiles: StateFlow<Set<TileId>> =
        device.data
            .ignoringReadErrors()
            .map { prefs -> prefs[ADDED_TILES].orEmpty().mapNotNull { name -> TileId.entries.firstOrNull { it.name == name } }.toSet() }
            .stateInFirst(emptySet())

    fun setDefault(
        setting: Tracked,
        value: String,
    ) = edit { it.update(defaults.value.with(setting, value)) }

    fun setDefaults(updated: Defaults) = edit { it.update(updated) }

    fun setNotify(
        group: Group,
        on: Boolean,
    ) = edit { it[booleanPreferencesKey(Defaults.Keys.notify(group))] = on }

    fun addHostname(name: String) = edit { it[HOSTNAMES] = it[HOSTNAMES].orEmpty() + name }

    fun removeHostname(name: String) = edit { it[HOSTNAMES] = it[HOSTNAMES].orEmpty() - name }

    fun setAdded(
        tile: TileId,
        added: Boolean,
    ) {
        if ((tile in addedTiles.value) == added) return
        scope.launch {
            device.edit { prefs ->
                val now = prefs[ADDED_TILES].orEmpty()
                prefs[ADDED_TILES] = if (added) now + tile.name else now - tile.name
            }
        }
    }

    private fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        scope.launch { settings.edit(block) }
    }

    /** Writes every default of [updated]: stored values replace the old ones. */
    private fun androidx.datastore.preferences.core.MutablePreferences.update(updated: Defaults) {
        if (updated.seeded) this[booleanPreferencesKey(Defaults.Keys.SEEDED)] = true
        asMap().keys.filter { it.name.startsWith(Defaults.Keys.VALUE_PREFIX) }.forEach { remove(it) }
        updated.values.forEach { (id, value) -> this[stringPreferencesKey(Defaults.Keys.VALUE_PREFIX + id)] = value }
    }

    private fun <T> Flow<T>.stateInFirst(fallback: T): StateFlow<T> {
        val first = runBlocking { withTimeoutOrNull(FIRST_READ_TIMEOUT_MS) { this@stateInFirst.first() } } ?: fallback
        return stateIn(scope, SharingStarted.Eagerly, first)
    }

    private fun toDefaults(prefs: Preferences): Defaults {
        val all = prefs.asMap()
        return Defaults.Keys.read(
            strings = all.entries.mapNotNull { (k, v) -> (v as? String)?.let { k.name to it } }.toMap(),
            booleans = all.entries.mapNotNull { (k, v) -> (v as? Boolean)?.let { k.name to it } }.toMap(),
            hostnames = prefs[HOSTNAMES].orEmpty(),
        )
    }

    private companion object {
        const val FIRST_READ_TIMEOUT_MS = 1_000L
    }
}
