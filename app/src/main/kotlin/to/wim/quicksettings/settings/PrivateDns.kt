package to.wim.quicksettings.settings

/** Android's Private DNS modes, with the values Android stores for them. */
enum class DnsMode(val value: String) {
    Off("off"),
    Automatic("opportunistic"),
    Hostname("hostname"),
    ;

    companion object {
        /** Android treats a missing value as Automatic. */
        fun from(value: String?): DnsMode = entries.firstOrNull { it.value == value } ?: Automatic
    }
}

/**
 * Private DNS as one value: "off", "opportunistic", or "hostname:<name>" while a hostname is in
 * use. As a default, "hostname" (without a name) means "whichever hostname is set in Android".
 *
 * Android keeps the hostname (private_dns_specifier) when you switch to Automatic, so a tap can
 * go back to it without typing it again.
 */
object PrivateDnsSetting : Tracked(
    id = "private_dns",
    access = Access.Secure,
    keys = listOf(Table.Global to "private_dns_mode", Table.Global to "private_dns_specifier"),
) {
    const val KEY_MODE = "private_dns_mode"
    const val KEY_HOSTNAME = "private_dns_specifier"
    const val OFF = "off"
    const val AUTOMATIC = "opportunistic"
    const val ANY_HOSTNAME = "hostname"
    private const val PREFIX = "hostname:"

    override val appDefault: String = ANY_HOSTNAME

    fun mode(device: Device): DnsMode = DnsMode.from(device.string(Table.Global, KEY_MODE))

    /** The hostname Android has, also while Private DNS is on Automatic or Off. */
    fun hostname(device: Device): String? = device.string(Table.Global, KEY_HOSTNAME)

    override fun encoded(device: Device): String =
        when (mode(device)) {
            DnsMode.Off -> OFF
            DnsMode.Automatic -> AUTOMATIC
            DnsMode.Hostname -> hostnameValue(hostname(device).orEmpty())
        }

    /** Writes a mode, or a hostname and then the hostname mode. A missing hostname writes nothing. */
    override fun writeEncoded(
        device: Device,
        value: String,
    ) {
        when {
            value == OFF -> device.put(Table.Global, KEY_MODE, DnsMode.Off.value)
            value == AUTOMATIC -> device.put(Table.Global, KEY_MODE, DnsMode.Automatic.value)
            value == ANY_HOSTNAME -> {
                if (!hostname(device).isNullOrBlank()) device.put(Table.Global, KEY_MODE, DnsMode.Hostname.value)
            }
            value.startsWith(PREFIX) -> {
                val name = value.removePrefix(PREFIX)
                if (name.isBlank()) return
                device.put(Table.Global, KEY_HOSTNAME, name)
                device.put(Table.Global, KEY_MODE, DnsMode.Hostname.value)
            }
        }
    }

    override fun isValid(value: String): Boolean =
        value == OFF || value == AUTOMATIC || value == ANY_HOSTNAME ||
            (value.startsWith(PREFIX) && Hostnames.isValid(value.removePrefix(PREFIX)))

    override fun matches(
        current: String?,
        default: String,
    ): Boolean = if (default == ANY_HOSTNAME) current?.startsWith(PREFIX) == true else current == default

    fun hostnameValue(name: String): String = PREFIX + name

    /** The hostname in a value, or null for Off and Automatic. */
    fun hostnameOf(value: String?): String? = value?.takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX)
}

/** Checks and orders the Private DNS hostnames the user saves. */
object Hostnames {
    private val label = Regex("""[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?""")

    /** Lower case, without spaces or a scheme someone pasted ("https://dns.example/"). */
    fun clean(input: String): String =
        input
            .trim()
            .lowercase()
            .removePrefix("https://")
            .removePrefix("tls://")
            .trimEnd('/')

    /** A hostname with at least one dot, like Android's own field accepts. */
    fun isValid(name: String): Boolean {
        if (name.length > 253) return false
        val labels = name.split('.')
        return labels.size >= 2 && labels.all { label.matches(it) }
    }

    /** The saved hostnames plus the one Android has, without duplicates, in alphabetical order. */
    fun merge(
        saved: Collection<String>,
        current: String?,
    ): List<String> = (saved + listOfNotNull(current?.takeIf { it.isNotBlank() })).map(::clean).distinct().sorted()
}
