package to.wim.quicksettings.settings

/**
 * Every change the app makes goes through here, so the rules live in one tested place.
 * [defaults] is read each time, so a default changed in a popup applies at once.
 */
class SettingsController(
    private val device: Device,
    private val defaults: () -> Defaults,
) {
    fun read(toggle: Toggle): Boolean = toggle.read(device)

    fun read(choice: Choice): String? = choice.read(device)

    fun read(level: Level): Float = level.read(device)

    fun encoded(setting: Tracked): String? = setting.encoded(device)

    fun defaultOf(setting: Tracked): String = defaults().of(setting)

    fun isChanged(setting: Tracked): Boolean = !setting.matches(setting.encoded(device), defaults().of(setting))

    /** The settings of [group] that differ from their default, in popup order. */
    fun changes(group: Group): List<Tracked> = group.tracked.filter(::isChanged)

    fun set(
        toggle: Toggle,
        on: Boolean,
    ) = toggle.write(device, on)

    fun select(
        choice: Choice,
        value: String,
    ) = choice.write(device, value)

    fun setLevel(
        level: Level,
        position: Float,
    ) = level.write(device, position)

    fun write(
        setting: Tracked,
        value: String,
    ) = setting.writeEncoded(device, value)

    /** Puts one setting back to its default. */
    fun reset(setting: Tracked) = setting.writeEncoded(device, defaults().of(setting))

    /** Puts everything changed in [group] back to its default. */
    fun reset(group: Group) = changes(group).forEach(::reset)

    /** Switches [toggle] and returns the new state. */
    fun toggle(toggle: Toggle): Boolean {
        val on = !toggle.read(device)
        toggle.write(device, on)
        return on
    }

    /** Moves [choice] to its next option (after the last, back to the first) and returns it. */
    fun cycle(choice: Choice): String {
        val index = choice.options.indexOf(choice.read(device))
        val next = choice.options[(index + 1) % choice.options.size]
        choice.write(device, next)
        return next
    }

    /**
     * Switches the screen timeout between its default and a long one: 30 minutes, or 1 minute
     * when 30 minutes is the default. Returns true when it now differs from the default.
     */
    fun toggleTimeout(): Boolean {
        val default = defaults().of(Catalog.Timeout)
        val long = if (default == Catalog.TIMEOUT_30_MIN) Catalog.TIMEOUT_1_MIN else Catalog.TIMEOUT_30_MIN
        val target = if (Catalog.Timeout.read(device) == default) long else default
        Catalog.Timeout.write(device, target)
        return target != default
    }

    /** The hostname Android has, also while Private DNS is on Automatic or Off. */
    fun dnsHostname(): String? = PrivateDnsSetting.hostname(device)

    /** Whether the Private DNS tile is lit: a hostname is in use. */
    fun dnsOnHostname(): Boolean = PrivateDnsSetting.mode(device) == DnsMode.Hostname

    /**
     * Switches Private DNS between a hostname and Automatic. The hostname is the default's when
     * the default names one, otherwise the one Android has. False when there is no hostname to
     * switch to, so nothing changed.
     */
    fun toggleDns(): Boolean {
        if (dnsOnHostname()) {
            PrivateDnsSetting.writeEncoded(device, PrivateDnsSetting.AUTOMATIC)
            return true
        }
        val named = PrivateDnsSetting.hostnameOf(defaults().of(PrivateDnsSetting))?.takeIf { it.isNotBlank() }
        return when {
            named != null -> {
                PrivateDnsSetting.writeEncoded(device, PrivateDnsSetting.hostnameValue(named))
                true
            }
            !PrivateDnsSetting.hostname(device).isNullOrBlank() -> {
                PrivateDnsSetting.writeEncoded(device, PrivateDnsSetting.ANY_HOSTNAME)
                true
            }
            else -> false
        }
    }

    /** The first start: the phone's current values become the defaults of [Catalog.seeded]. */
    fun seedDefaults(base: Defaults = defaults()): Defaults = currentAs(Catalog.seeded, base).copy(seeded = true)

    /** The current values of [group] as defaults: what "Use current" stores. Unknown values are skipped. */
    fun currentAsDefaults(
        group: Group,
        base: Defaults = defaults(),
    ): Defaults = currentAs(group.tracked, base)

    private fun currentAs(
        settings: List<Tracked>,
        base: Defaults,
    ): Defaults =
        settings.fold(base) { acc, setting ->
            val value = setting.encoded(device)
            if (value != null && setting.isValid(value)) acc.with(setting, value) else acc
        }
}
