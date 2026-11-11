package to.wim.quicksettings.settings

/**
 * The launcher shortcuts: four fixed slots in res/xml/shortcuts.xml (Pixel Quick Tap only offers
 * shortcuts from the manifest, see docs/decisions.md). Each opens a popup: every popup but
 * Developer, in the order of the main screen (#24).
 */
object Shortcuts {
    /** The popup each slot opens, in slot order. */
    val popups: List<Group> = listOf(Group.Screen, Group.Colour, Group.Sound, Group.Network)

    /** Slot numbers, 1 to 4, in the order of res/xml/shortcuts.xml. */
    val slots: IntRange = 1..popups.size

    /** The popup [slot] opens, or null for a slot that doesn't exist. */
    fun popupOf(slot: Int): Group? = popups.getOrNull(slot - slots.first)
}
