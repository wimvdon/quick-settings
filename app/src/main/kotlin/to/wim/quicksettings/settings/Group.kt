package to.wim.quicksettings.settings

/** One row in a popup: a setting, with a slider under it while it is on (Night Light warmth). */
data class Row(
    val setting: Setting,
    val level: Level? = null,
)

/**
 * The popups. Each has its own tile, which lights up while one of its settings differs from its
 * default. The names are stable: they key the stored notification choice.
 */
enum class Group(
    val rows: List<Row>,
    /** Whether a notification with a Reset button shows while something is changed. */
    val notifiesByDefault: Boolean,
) {
    Screen(
        listOf(
            Row(Catalog.BrightnessMode, Catalog.Brightness),
            Row(Catalog.Timeout),
            Row(Catalog.AlwaysOn),
            Row(Catalog.TapToWake),
            Row(Catalog.Rotation),
        ),
        notifiesByDefault = true,
    ),

    // Night Light usually follows a schedule, so a notification every evening would only nag.
    Colour(
        listOf(
            Row(Catalog.NightLight, Catalog.NightLightWarmth),
            Row(Catalog.ExtraDim, Catalog.ExtraDimLevel),
            Row(Catalog.Grayscale),
            Row(Catalog.InvertColors),
        ),
        notifiesByDefault = false,
    ),
    Sound(
        listOf(
            Row(Catalog.Ringer),
            Row(Catalog.MediaVolume),
            Row(Catalog.RingVolume),
            Row(Catalog.NotificationVolume),
            Row(Catalog.AlarmVolume),
            Row(Catalog.TouchVibration),
            Row(Catalog.HeadsUp),
        ),
        notifiesByDefault = false,
    ),
    Developer(
        listOf(
            Row(Catalog.UsbDebugging),
            Row(Catalog.WirelessDebugging),
            Row(Catalog.Animations),
            Row(Catalog.StayAwake),
        ),
        notifiesByDefault = true,
    ),
    Network(
        listOf(Row(Catalog.PrivateDns)),
        notifiesByDefault = true,
    ),
    ;

    /** The settings with a default, in the order they are shown. */
    val tracked: List<Tracked> get() = rows.mapNotNull { it.setting as? Tracked }

    /** Everything this popup needs permission for. */
    val accesses: Set<Access> get() = rows.flatMap { listOfNotNull(it.setting, it.level) }.map { it.access }.toSet()

    companion object {
        /** The popup a setting is shown in. */
        fun of(setting: Setting): Group =
            entries.first { g -> g.rows.any { it.setting == setting || it.level == setting } }
    }
}
