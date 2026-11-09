package to.wim.quicksettings.settings

/**
 * What the user chose as normal: a default per setting (only those they changed are stored),
 * whether each popup notifies while something differs, and the saved Private DNS hostnames.
 *
 * Kept in the backed-up DataStore (see data/SettingsRepository.kt), so it comes along to a new
 * phone. Plain Kotlin, tested on the JVM (see DefaultsTest).
 */
data class Defaults(
    val values: Map<String, String> = emptyMap(),
    val notify: Map<Group, Boolean> = emptyMap(),
    val hostnames: Set<String> = emptySet(),
    /** Whether the first start has taken the phone's values as defaults (see [Catalog.seeded]). */
    val seeded: Boolean = false,
) {
    /** The default of [setting]: the user's choice, or the app's when none (or an outdated one) is stored. */
    fun of(setting: Tracked): String = values[setting.id]?.takeIf { setting.isValid(it) } ?: setting.appDefault

    fun notifies(group: Group): Boolean = notify[group] ?: group.notifiesByDefault

    /** A copy with [value] as the default of [setting]; the app's own default is not stored. */
    fun with(
        setting: Tracked,
        value: String,
    ): Defaults = copy(values = if (value == setting.appDefault) values - setting.id else values + (setting.id to value))

    /** Converts to and from the flat keys DataStore stores. */
    object Keys {
        const val VALUE_PREFIX = "default."
        const val NOTIFY_PREFIX = "notify."
        const val HOSTNAMES = "hostnames"
        const val SEEDED = "defaults_seeded"

        fun value(setting: Tracked) = VALUE_PREFIX + setting.id

        fun notify(group: Group) = NOTIFY_PREFIX + group.name

        /** Rebuilds [Defaults] from stored strings, booleans and the hostname set; unknown keys are ignored. */
        fun read(
            strings: Map<String, String>,
            booleans: Map<String, Boolean>,
            hostnames: Set<String>,
        ): Defaults =
            Defaults(
                values = strings.filterKeys { it.startsWith(VALUE_PREFIX) }.mapKeys { it.key.removePrefix(VALUE_PREFIX) },
                notify =
                    booleans.mapNotNull { (key, on) ->
                        Group.entries.firstOrNull { notify(it) == key }?.let { it to on }
                    }.toMap(),
                hostnames = hostnames.filter(Hostnames::isValid).toSet(),
                seeded = booleans[SEEDED] == true,
            )
    }
}
