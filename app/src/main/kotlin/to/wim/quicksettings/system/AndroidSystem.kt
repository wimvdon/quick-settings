package to.wim.quicksettings.system

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import to.wim.quicksettings.notify.Notifications
import to.wim.quicksettings.settings.Audio
import to.wim.quicksettings.settings.Permissions
import to.wim.quicksettings.settings.RawSettings
import to.wim.quicksettings.settings.RingerMode
import to.wim.quicksettings.settings.Stream
import to.wim.quicksettings.settings.Table
import kotlin.math.roundToInt

private const val TAG = "QuickSettings"

/**
 * [RawSettings] backed by Settings.System, Settings.Secure and Settings.Global. System needs
 * "Modify system settings"; Secure and Global need WRITE_SECURE_SETTINGS (adb). Writes without
 * permission are skipped; a refused write is logged, not thrown.
 */
class AndroidRawSettings(private val context: Context) : RawSettings {
    private val resolver: ContentResolver get() = context.contentResolver

    override fun get(
        table: Table,
        key: String,
    ): String? =
        runCatching {
            when (table) {
                Table.System -> Settings.System.getString(resolver, key)
                Table.Secure -> Settings.Secure.getString(resolver, key)
                Table.Global -> Settings.Global.getString(resolver, key)
            }
        }.getOrNull()

    override fun put(
        table: Table,
        key: String,
        value: String,
    ) {
        val allowed = if (table == Table.System) canWriteSystem(context) else canWriteSecure(context)
        if (!allowed) return
        runCatching {
            when (table) {
                Table.System -> Settings.System.putString(resolver, key, value)
                Table.Secure -> Settings.Secure.putString(resolver, key, value)
                Table.Global -> Settings.Global.putString(resolver, key, value)
            }
        }.onFailure { Log.w(TAG, "Could not write $key", it) }
    }

    companion object {
        fun uri(
            table: Table,
            key: String,
        ): Uri =
            when (table) {
                Table.System -> Settings.System.getUriFor(key)
                Table.Secure -> Settings.Secure.getUriFor(key)
                Table.Global -> Settings.Global.getUriFor(key)
            }

        fun canWriteSystem(context: Context): Boolean = Settings.System.canWrite(context)

        fun canWriteSecure(context: Context): Boolean =
            context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

        fun permissions(context: Context) =
            Permissions(
                system = canWriteSystem(context),
                secure = canWriteSecure(context),
                notifications = Notifications.canPost(context),
            )
    }
}

/**
 * [Audio] backed by AudioManager. No permission needed, but Android refuses some changes while
 * Do Not Disturb is on (SecurityException); those are skipped.
 */
class AndroidAudio(context: Context) : Audio {
    private val audio = context.getSystemService(AudioManager::class.java)

    override fun ringer(): RingerMode =
        when (audio?.ringerMode) {
            AudioManager.RINGER_MODE_VIBRATE -> RingerMode.Vibrate
            AudioManager.RINGER_MODE_SILENT -> RingerMode.Silent
            else -> RingerMode.Normal
        }

    override fun setRinger(ringer: RingerMode) {
        val mode =
            when (ringer) {
                RingerMode.Normal -> AudioManager.RINGER_MODE_NORMAL
                RingerMode.Vibrate -> AudioManager.RINGER_MODE_VIBRATE
                RingerMode.Silent -> AudioManager.RINGER_MODE_SILENT
            }
        runCatching { audio?.ringerMode = mode }.onFailure { Log.w(TAG, "Could not set the ringer", it) }
    }

    override fun volume(stream: Stream): Float {
        val am = audio ?: return 0f
        val type = type(stream)
        val min = am.getStreamMinVolume(type)
        val max = am.getStreamMaxVolume(type)
        if (max <= min) return 0f
        return (am.getStreamVolume(type) - min).toFloat() / (max - min)
    }

    override fun setVolume(
        stream: Stream,
        position: Float,
    ) {
        val am = audio ?: return
        val type = type(stream)
        val min = am.getStreamMinVolume(type)
        val max = am.getStreamMaxVolume(type)
        val index = min + (position * (max - min)).roundToInt()
        runCatching { am.setStreamVolume(type, index, 0) }.onFailure { Log.w(TAG, "Could not set a volume", it) }
    }

    /** Shows Android's own volume panel. */
    fun showPanel() {
        runCatching { audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI) }
    }

    private fun type(stream: Stream): Int =
        when (stream) {
            Stream.Media -> AudioManager.STREAM_MUSIC
            Stream.Ring -> AudioManager.STREAM_RING
            Stream.Notification -> AudioManager.STREAM_NOTIFICATION
            Stream.Alarm -> AudioManager.STREAM_ALARM
        }
}
