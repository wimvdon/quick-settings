package to.wim.quicksettings.settings

/** The headings tiles are grouped under on the main screen. */
enum class Category { Display, Colour, Sound, Network, Developer, Other }

/** What a tile does when tapped. */
sealed interface TileKind {
    /** Opens a popup. Lit while something in it differs from its default. */
    data class Popup(val group: Group) : TileKind

    /** Switches a setting on or off. Lit while on. */
    data class Switch(val toggle: Toggle) : TileKind

    /** Steps through a choice's options. Lit while not at its default. */
    data class Cycle(val choice: Choice) : TileKind

    /** Screen timeout: its default ↔ a long one. Lit while long. */
    data object Timeout : TileKind

    /** Private DNS: a hostname ↔ Automatic. Lit while on a hostname. */
    data object Dns : TileKind

    /** Opens Android's keyboard picker. */
    data object Keyboard : TileKind

    /** Shows Android's volume panel. */
    data object Volume : TileKind
}

/**
 * Every tile, in main-screen order. The names are stable: they key which tiles were added.
 * Each has its own TileService class (see tile/TileServices.kt and the manifest).
 */
enum class TileId(
    val kind: TileKind,
    val category: Category,
) {
    // Popups.
    ScreenPopup(TileKind.Popup(Group.Screen), Category.Display),
    ColourPopup(TileKind.Popup(Group.Colour), Category.Colour),
    SoundPopup(TileKind.Popup(Group.Sound), Category.Sound),
    DeveloperPopup(TileKind.Popup(Group.Developer), Category.Developer),
    NetworkPopup(TileKind.Popup(Group.Network), Category.Network),

    // Single tiles.
    Timeout(TileKind.Timeout, Category.Display),
    AlwaysOn(TileKind.Switch(Catalog.AlwaysOn), Category.Display),
    TapToWake(TileKind.Switch(Catalog.TapToWake), Category.Display),
    Rotation(TileKind.Cycle(Catalog.Rotation), Category.Display),
    Grayscale(TileKind.Switch(Catalog.Grayscale), Category.Colour),
    Ringer(TileKind.Cycle(Catalog.Ringer), Category.Sound),
    Volume(TileKind.Volume, Category.Sound),
    TouchVibration(TileKind.Switch(Catalog.TouchVibration), Category.Sound),
    HeadsUp(TileKind.Switch(Catalog.HeadsUp), Category.Sound),
    PrivateDns(TileKind.Dns, Category.Network),
    UsbDebugging(TileKind.Switch(Catalog.UsbDebugging), Category.Developer),
    WirelessDebugging(TileKind.Switch(Catalog.WirelessDebugging), Category.Developer),
    Animations(TileKind.Cycle(Catalog.Animations), Category.Developer),
    StayAwake(TileKind.Switch(Catalog.StayAwake), Category.Developer),
    Keyboard(TileKind.Keyboard, Category.Other),
    ;

    val isPopup: Boolean get() = kind is TileKind.Popup

    /** The setting a one-tap tile changes, which its tune button sets up; null for popups, Keyboard and Volume. */
    val setting: Tracked?
        get() =
            when (val k = kind) {
                is TileKind.Switch -> k.toggle
                is TileKind.Cycle -> k.choice
                TileKind.Timeout -> Catalog.Timeout
                TileKind.Dns -> PrivateDnsSetting
                is TileKind.Popup, TileKind.Keyboard, TileKind.Volume -> null
            }

    /** What the tile needs permission for. */
    val access: Access
        get() =
            when (val k = kind) {
                is TileKind.Popup -> Access.None // The popup itself explains what's missing.
                is TileKind.Switch -> k.toggle.access
                is TileKind.Cycle -> k.choice.access
                TileKind.Timeout -> Catalog.Timeout.access
                TileKind.Dns -> PrivateDnsSetting.access
                TileKind.Keyboard, TileKind.Volume -> Access.None
            }

    /** The popup a long press opens, for single tiles that belong to one. */
    val popup: Group?
        get() =
            when (val k = kind) {
                is TileKind.Popup -> null
                is TileKind.Switch -> Group.of(k.toggle)
                is TileKind.Cycle -> Group.of(k.choice)
                TileKind.Timeout -> Group.Screen
                TileKind.Dns -> Group.Network
                TileKind.Volume -> Group.Sound
                TileKind.Keyboard -> null
            }

    companion object {
        val popups: List<TileId> get() = entries.filter { it.isPopup }
        val singles: List<TileId> get() = entries.filterNot { it.isPopup }

        /** The one-tap tile that changes [setting], if there is one. */
        fun of(setting: Tracked): TileId? = singles.firstOrNull { it.setting == setting }

        /** The main screen's sections, in heading order: each popup's tile with the single tiles below it. */
        fun sections(): List<Section> =
            Category.entries
                .map { c -> Section(c, popups.firstOrNull { it.category == c }, singles.filter { it.category == c }) }
                .filter { it.popup != null || it.tiles.isNotEmpty() }
    }
}

/** One section of the main screen: a popup's wide tile (none for Other) and the tiles below it. */
data class Section(
    val category: Category,
    val popup: TileId?,
    val tiles: List<TileId>,
)

/** What the app may do right now. */
data class Permissions(
    val system: Boolean,
    val secure: Boolean,
    val notifications: Boolean,
) {
    fun allows(access: Access): Boolean =
        when (access) {
            Access.None -> true
            Access.System -> system
            Access.Secure -> secure
        }

    /** True when something the tiles need is missing (shown as a banner). */
    val anyMissing: Boolean get() = !system || !secure || !notifications
}
