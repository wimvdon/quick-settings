package to.wim.quicksettings.settings

/**
 * The launcher shortcuts: four fixed slots in res/xml/shortcuts.xml (Pixel Quick Tap only offers
 * shortcuts from the manifest, see docs/decisions.md). The user picks which tile each slot taps;
 * the choice is kept in [Defaults.shortcuts].
 */
object Shortcuts {
    /** Slot numbers, as the shortcuts are labelled ("Shortcut 1" to "Shortcut 4"). */
    val slots: IntRange = 1..4

    /** What the slots did before they could be chosen, so existing shortcuts and Quick Tap keep working. */
    val defaults: List<TileId> = listOf(TileId.PrivateDns, TileId.Timeout, TileId.AlwaysOn, TileId.TapToWake)

    /** Tiles a shortcut can tap: those that change a setting. */
    val targets: List<TileId> get() = TileId.singles.filter { it.setting != null }

    fun defaultOf(slot: Int): TileId? = defaults.getOrNull(slot - slots.first)
}
