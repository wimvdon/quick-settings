package to.wim.quicksettings.settings

/**
 * One setting the app shows. [id] is stable: it names the stored default (DataStore) and must
 * not change once released.
 *
 * Switches ([Toggle]) and choices ([Choice]) have a default the user can change; a popup's tile
 * lights up while one of them differs from it. Sliders ([Level]) have no default.
 */
sealed class Setting(
    val id: String,
    val access: Access,
    /** Android settings to watch for changes made elsewhere. */
    val keys: List<Pair<Table, String>>,
)

/** A setting with a default: its value is written as text ("on"/"off" or a choice's option). */
sealed class Tracked(
    id: String,
    access: Access,
    keys: List<Pair<Table, String>>,
) : Setting(id, access, keys) {
    abstract val appDefault: String

    /** The current value as text, or null when Android has a value the app doesn't know. */
    abstract fun encoded(device: Device): String?

    abstract fun writeEncoded(
        device: Device,
        value: String,
    )

    /** Whether [value] can be stored as this setting's default. */
    abstract fun isValid(value: String): Boolean

    /** Whether [current] counts as being at [default]. */
    open fun matches(
        current: String?,
        default: String,
    ): Boolean = current == default
}

class Toggle(
    id: String,
    access: Access,
    keys: List<Pair<Table, String>>,
    val defaultOn: Boolean,
    private val reader: (Device) -> Boolean,
    private val writer: (Device, Boolean) -> Unit,
) : Tracked(id, access, keys) {
    override val appDefault: String get() = encode(defaultOn)

    fun read(device: Device): Boolean = reader(device)

    fun write(
        device: Device,
        on: Boolean,
    ) = writer(device, on)

    override fun encoded(device: Device): String = encode(read(device))

    override fun writeEncoded(
        device: Device,
        value: String,
    ) = write(device, value == ON)

    override fun isValid(value: String): Boolean = value == ON || value == OFF

    companion object {
        const val ON = "on"
        const val OFF = "off"

        fun encode(on: Boolean): String = if (on) ON else OFF
    }
}

class Choice(
    id: String,
    access: Access,
    keys: List<Pair<Table, String>>,
    /** The values the popup offers, in order. */
    val options: List<String>,
    override val appDefault: String,
    private val reader: (Device) -> String?,
    private val writer: (Device, String) -> Unit,
) : Tracked(id, access, keys) {
    /** The current value: one of [options], or something else set elsewhere (null if unreadable). */
    fun read(device: Device): String? = reader(device)

    fun write(
        device: Device,
        value: String,
    ) = writer(device, value)

    override fun encoded(device: Device): String? = read(device)

    override fun writeEncoded(
        device: Device,
        value: String,
    ) = write(device, value)

    override fun isValid(value: String): Boolean = value in options
}

/** A slider, 0..1. */
class Level(
    id: String,
    access: Access,
    keys: List<Pair<Table, String>>,
    private val reader: (Device) -> Float,
    private val writer: (Device, Float) -> Unit,
) : Setting(id, access, keys) {
    fun read(device: Device): Float = reader(device).coerceIn(0f, 1f)

    fun write(
        device: Device,
        position: Float,
    ) = writer(device, position.coerceIn(0f, 1f))
}
