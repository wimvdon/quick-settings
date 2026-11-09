package to.wim.quicksettings.dns

/** Android's Private DNS modes, with the values Android stores for them. */
enum class DnsMode(val value: String) {
    Off("off"),
    Opportunistic("opportunistic"),
    Hostname("hostname"),
    ;

    companion object {
        /** Android treats a missing value as "Automatic" (opportunistic). */
        fun from(value: String?): DnsMode = entries.firstOrNull { it.value == value } ?: Opportunistic
    }
}

/**
 * Private DNS as Android has it: the mode and the provider hostname. Android keeps the hostname
 * when you switch to Automatic, so it can be switched back to without typing it again.
 *
 * Plain Kotlin, tested on the JVM (see PrivateDnsTest).
 */
data class DnsState(val mode: DnsMode, val hostname: String?) {
    val hasHostname: Boolean get() = !hostname.isNullOrBlank()

    /** The tile is lit while the own hostname is in use. */
    val tileActive: Boolean get() = mode == DnsMode.Hostname

    /** The reminder shows whenever the own hostname is not in use (Off or Automatic). */
    val showNotification: Boolean get() = mode != DnsMode.Hostname

    /**
     * The mode a tap on the tile switches to: Hostname ↔ Automatic. From Off it goes to Hostname.
     * Null when it would need a hostname that was never set.
     */
    fun toggled(): DnsMode? =
        when {
            mode == DnsMode.Hostname -> DnsMode.Opportunistic
            hasHostname -> DnsMode.Hostname
            else -> null
        }
}

/** Reads and writes Android's Private DNS setting. The Android version is [SystemDnsSettings]. */
interface DnsSettings {
    fun read(): DnsState

    fun setMode(mode: DnsMode)
}

class DnsController(private val settings: DnsSettings) {
    fun read(): DnsState = settings.read()

    /** Switches between hostname and Automatic. False when no hostname is set, so nothing changed. */
    fun toggle(): Boolean {
        val target = settings.read().toggled() ?: return false
        settings.setMode(target)
        return true
    }

    /** Goes back to the own hostname. False when no hostname is set. */
    fun useHostname(): Boolean {
        if (!settings.read().hasHostname) return false
        settings.setMode(DnsMode.Hostname)
        return true
    }
}
