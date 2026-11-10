package to.wim.quicksettings.settings

/**
 * What the user chose as normal: a default per setting (only those they changed are stored),
 * which settings notify while they differ, the rows each popup shows, what each launcher
 * shortcut taps, and the saved Private DNS hostnames.
 *
 * Kept in the backed-up DataStore (see data/SettingsRepository.kt), so it comes along to a new
 * phone. Plain Kotlin, tested on the JVM (see DefaultsTest).
 */
data class Defaults(
    val values: Map<String, String> = emptyMap(),
    /** The per-popup choice of earlier versions: still the fallback for settings without their own. */
    val notify: Map<Group, Boolean> = emptyMap(),
    /** Setting id → whether a notification shows while it differs. */
    val notifySettings: Map<String, Boolean> = emptyMap(),
    /** Ids of the row settings a popup leaves out. */
    val hiddenRows: Set<String> = emptySet(),
    /** Slot → the tile it taps (null: nothing). Slots not stored do what [Shortcuts.defaults] says. */
    val shortcuts: Map<Int, TileId?> = emptyMap(),
    val hostnames: Set<String> = emptySet(),
    /** Whether the first start has taken the phone's values as defaults (see [Catalog.seeded]). */
    val seeded: Boolean = false,
) {
    /** The default of [setting]: the user's choice, or the app's when none (or an outdated one) is stored. */
    fun of(setting: Tracked): String = values[setting.id]?.takeIf { setting.isValid(it) } ?: setting.appDefault

    fun notifies(group: Group): Boolean = notify[group] ?: group.notifiesByDefault

    fun notifies(setting: Tracked): Boolean = notifySettings[setting.id] ?: notifies(Group.of(setting))

    /** A copy with [value] as the default of [setting]; the app's own default is not stored. */
    fun with(
        setting: Tracked,
        value: String,
    ): Defaults = copy(values = if (value == setting.appDefault) values - setting.id else values + (setting.id to value))

    fun withNotify(
        setting: Tracked,
        on: Boolean,
    ): Defaults = copy(notifySettings = notifySettings + (setting.id to on))

    fun shows(row: Row): Boolean = row.setting.id !in hiddenRows

    /** The rows [group]'s popup shows; all of them if every one was left out. */
    fun rowsOf(group: Group): List<Row> = group.rows.filter(::shows).ifEmpty { group.rows }

    /** The settings with a default that [group]'s popup shows. */
    fun trackedOf(group: Group): List<Tracked> = rowsOf(group).mapNotNull { it.setting as? Tracked }

    /** A copy that shows or leaves out [row] in its popup. The last row shown can't be left out. */
    fun withRow(
        row: Row,
        shown: Boolean,
    ): Defaults {
        val id = row.setting.id
        if (!shown && rowsOf(Group.of(row.setting)).let { it.size <= 1 && row in it }) return this
        return copy(hiddenRows = if (shown) hiddenRows - id else hiddenRows + id)
    }

    /** The tile [slot] taps, or null when it taps nothing. */
    fun shortcut(slot: Int): TileId? = if (slot in shortcuts) shortcuts[slot] else Shortcuts.defaultOf(slot)

    /** The slot that taps [tile], if any. */
    fun slotOf(tile: TileId): Int? = Shortcuts.slots.firstOrNull { shortcut(it) == tile }

    /** A copy where [slot] taps [tile] (null: no slot); a slot that tapped it before is emptied. */
    fun withShortcut(
        tile: TileId,
        slot: Int?,
    ): Defaults =
        copy(
            shortcuts =
                Shortcuts.slots.associateWith { s ->
                    when {
                        s == slot -> tile
                        shortcut(s) == tile -> null
                        else -> shortcut(s)
                    }
                },
        )

    /** A copy where [slot] taps [tile] (null: nothing); a slot that tapped [tile] before is emptied. */
    fun withSlot(
        slot: Int,
        tile: TileId?,
    ): Defaults =
        if (tile != null) {
            withShortcut(tile, slot)
        } else {
            copy(shortcuts = Shortcuts.slots.associateWith { s -> if (s == slot) null else shortcut(s) })
        }

    /** Converts to and from the flat keys DataStore stores. */
    object Keys {
        const val VALUE_PREFIX = "default."
        const val NOTIFY_PREFIX = "notify."
        const val NOTIFY_SETTING_PREFIX = "notify_setting."
        const val SHORTCUT_PREFIX = "shortcut."
        const val HIDDEN_ROWS = "hidden_rows"
        const val HOSTNAMES = "hostnames"
        const val SEEDED = "defaults_seeded"

        /** Stored for a slot that taps nothing. */
        const val NO_SHORTCUT = ""

        fun value(setting: Tracked) = VALUE_PREFIX + setting.id

        fun notify(group: Group) = NOTIFY_PREFIX + group.name

        fun notify(setting: Tracked) = NOTIFY_SETTING_PREFIX + setting.id

        fun shortcut(slot: Int) = SHORTCUT_PREFIX + slot

        /** What a slot stores: the tile's name, or [NO_SHORTCUT]. */
        fun shortcutValue(tile: TileId?) = tile?.name ?: NO_SHORTCUT

        /** Rebuilds [Defaults] from stored strings, booleans and string sets; unknown keys are ignored. */
        fun read(
            strings: Map<String, String>,
            booleans: Map<String, Boolean>,
            hostnames: Set<String>,
            hiddenRows: Set<String> = emptySet(),
        ): Defaults =
            Defaults(
                values = strings.filterKeys { it.startsWith(VALUE_PREFIX) }.mapKeys { it.key.removePrefix(VALUE_PREFIX) },
                notify =
                    booleans.mapNotNull { (key, on) ->
                        Group.entries.firstOrNull { notify(it) == key }?.let { it to on }
                    }.toMap(),
                notifySettings =
                    booleans.filterKeys { it.startsWith(NOTIFY_SETTING_PREFIX) }.mapKeys { it.key.removePrefix(NOTIFY_SETTING_PREFIX) },
                hiddenRows = hiddenRows,
                shortcuts =
                    strings.mapNotNull { (key, value) ->
                        val slot = key.removePrefix(SHORTCUT_PREFIX).takeIf { key.startsWith(SHORTCUT_PREFIX) }?.toIntOrNull()
                        val tile = Shortcuts.targets.firstOrNull { it.name == value }
                        when {
                            slot == null || slot !in Shortcuts.slots -> null
                            value == NO_SHORTCUT -> slot to null
                            tile != null -> slot to tile
                            else -> null
                        }
                    }.toMap(),
                hostnames = hostnames.filter(Hostnames::isValid).toSet(),
                seeded = booleans[SEEDED] == true,
            )
    }
}
