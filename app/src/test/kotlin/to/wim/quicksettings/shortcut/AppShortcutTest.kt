package to.wim.quicksettings.shortcut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShortcutTest {
    @Test
    fun `ids stay the same, so Quick Tap keeps finding the chosen shortcut`() {
        assertEquals(
            listOf("toggle_private_dns", "toggle_timeout", "toggle_always_on", "toggle_tap_to_wake"),
            AppShortcut.entries.map { it.id },
        )
    }

    @Test
    fun `every shortcut has its own action`() {
        val actions = AppShortcut.entries.map { it.action }
        assertEquals(actions.size, actions.toSet().size)
    }

    @Test
    fun `published shortcuts match only when they are exactly these, in order`() {
        assertTrue(AppShortcut.matches(AppShortcut.entries.map { it.id }))
        assertFalse(AppShortcut.matches(emptyList()))
        assertFalse(AppShortcut.matches(AppShortcut.entries.map { it.id }.reversed()))
    }
}
