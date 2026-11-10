package to.wim.quicksettings.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TilesTest {
    @Test
    fun `every popup has one tile`() {
        assertEquals(Group.entries.toList(), TileId.popups.map { (it.kind as TileKind.Popup).group })
    }

    @Test
    fun `sections hold every tile once, in heading order`() {
        val sections = TileId.sections()
        val categories = sections.map { it.category }
        assertEquals(categories.sortedBy { it.ordinal }, categories)
        assertEquals(TileId.entries.toSet(), sections.flatMap { listOfNotNull(it.popup) + it.tiles }.toSet())
        assertEquals(TileId.entries.size, sections.sumOf { it.tiles.size + if (it.popup != null) 1 else 0 })
    }

    @Test
    fun `each popup heads the section of its own tiles`() {
        val display = TileId.sections().first { it.category == Category.Display }
        assertEquals(TileId.ScreenPopup, display.popup)
        assertTrue(TileId.AlwaysOn in display.tiles)
        assertNull(TileId.sections().first { it.category == Category.Other }.popup)
    }

    @Test
    fun `single tiles name the setting they set up`() {
        assertEquals(Catalog.Timeout, TileId.Timeout.setting)
        assertEquals(PrivateDnsSetting, TileId.PrivateDns.setting)
        assertNull(TileId.Keyboard.setting)
        assertNull(TileId.ScreenPopup.setting)
        assertEquals(TileId.Grayscale, TileId.of(Catalog.Grayscale))
        assertNull(TileId.of(Catalog.NightLight))
    }

    @Test
    fun `long press opens the popup a setting is in`() {
        assertEquals(Group.Screen, TileId.AlwaysOn.popup)
        assertEquals(Group.Developer, TileId.Animations.popup)
        assertEquals(Group.Network, TileId.PrivateDns.popup)
    }

    @Test
    fun `tiles name what they need`() {
        assertEquals(Access.Secure, TileId.UsbDebugging.access)
        assertEquals(Access.System, TileId.Timeout.access)
        assertEquals(Access.None, TileId.Ringer.access)
    }

    @Test
    fun `permissions`() {
        val p = Permissions(system = true, secure = false, notifications = true)
        assertTrue(p.allows(Access.None))
        assertTrue(p.allows(Access.System))
        assertFalse(p.allows(Access.Secure))
        assertTrue(p.anyMissing)
        assertFalse(Permissions(system = true, secure = true, notifications = true).anyMissing)
    }

    @Test
    fun `every setting belongs to exactly one popup`() {
        Catalog.all.forEach { s -> assertEquals(s.id, 1, Group.entries.count { g -> g.rows.any { it.setting == s || it.level == s } }) }
    }
}
