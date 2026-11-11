package to.wim.quicksettings.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ShortcutsTest {
    @Test
    fun `the four shortcuts open every popup but Developer, in order`() {
        assertEquals(listOf(Group.Screen, Group.Colour, Group.Sound, Group.Network), Shortcuts.slots.map(Shortcuts::popupOf))
        assertFalse(Group.Developer in Shortcuts.popups)
        assertEquals(Group.entries - Group.Developer, Shortcuts.popups)
    }

    @Test
    fun `a slot that doesn't exist opens nothing`() {
        assertNull(Shortcuts.popupOf(0))
        assertNull(Shortcuts.popupOf(5))
    }
}
