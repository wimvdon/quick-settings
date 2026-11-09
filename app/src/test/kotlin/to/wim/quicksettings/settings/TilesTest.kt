package to.wim.quicksettings.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TilesTest {
    @Test
    fun `every popup has one tile`() {
        assertEquals(Group.entries.toList(), TileId.popups.map { (it.kind as TileKind.Popup).group })
    }

    @Test
    fun `single tiles are grouped in heading order`() {
        val categories = TileId.singlesByCategory().map { it.first }
        assertEquals(categories.sortedBy { it.ordinal }, categories)
        assertEquals(TileId.singles.size, TileId.singlesByCategory().sumOf { it.second.size })
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
