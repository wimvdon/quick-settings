package to.wim.quicksettings.settings

/** The three tables Android keeps its settings in. */
enum class Table { System, Secure, Global }

/** What the app needs before it may change a setting. */
enum class Access {
    /** Nothing: volumes and the ringer go through Android's audio service. */
    None,

    /** "Modify system settings", granted in Android's Settings. */
    System,

    /** WRITE_SECURE_SETTINGS, which only adb can grant. */
    Secure,
}

/** Android's settings as text, the way it stores them. The Android version is AndroidRawSettings. */
interface RawSettings {
    fun get(
        table: Table,
        key: String,
    ): String?

    /** Writes [value]. Skipped quietly when the app may not write [table]. */
    fun put(
        table: Table,
        key: String,
        value: String,
    )
}

/** The volumes the Sound popup shows. */
enum class Stream { Media, Ring, Notification, Alarm }

enum class RingerMode { Normal, Vibrate, Silent }

/** Android's audio service. The Android version is AndroidAudio. */
interface Audio {
    fun ringer(): RingerMode

    fun setRinger(ringer: RingerMode)

    /** 0..1. */
    fun volume(stream: Stream): Float

    fun setVolume(
        stream: Stream,
        position: Float,
    )
}

/** Everything a setting reads and writes. Faked in the tests. */
class Device(
    val raw: RawSettings,
    val audio: Audio,
) {
    fun string(
        table: Table,
        key: String,
    ): String? = raw.get(table, key)?.trim()?.takeIf { it.isNotEmpty() }

    fun float(
        table: Table,
        key: String,
    ): Float? = string(table, key)?.toFloatOrNull()

    fun int(
        table: Table,
        key: String,
    ): Int? = string(table, key)?.let { it.toIntOrNull() ?: it.toFloatOrNull()?.toInt() }

    /** Android stores switches as 1 and 0; a missing value means Android's own default. */
    fun bool(
        table: Table,
        key: String,
        missing: Boolean,
    ): Boolean = int(table, key)?.let { it != 0 } ?: missing

    fun put(
        table: Table,
        key: String,
        value: String,
    ) = raw.put(table, key, value)

    fun put(
        table: Table,
        key: String,
        value: Int,
    ) = raw.put(table, key, value.toString())

    fun put(
        table: Table,
        key: String,
        on: Boolean,
    ) = raw.put(table, key, if (on) "1" else "0")
}
