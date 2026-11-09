package to.wim.quicksettings.shortcut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShortcutTest {
    @Test
    fun `ids don't reuse the old manifest shortcut ids, which Android may keep locked`() {
        val old = setOf("toggle_private_dns", "toggle_timeout", "toggle_always_on", "toggle_tap_to_wake")
        assertTrue(AppShortcut.entries.none { it.id in old })
        val ids = AppShortcut.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
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
