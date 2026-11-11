package to.wim.quicksettings.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultsTest {
    @Test
    fun `app defaults apply when nothing is stored`() {
        val d = Defaults()
        assertEquals(Catalog.TIMEOUT_1_MIN, d.of(Catalog.Timeout))
        assertEquals(Toggle.ON, d.of(Catalog.TapToWake))
        assertTrue(d.notifies(Group.Screen))
        assertFalse(d.notifies(Group.Colour))
    }

    @Test
    fun `an outdated stored default falls back to the app's`() {
        val d = Defaults(values = mapOf("timeout" to "120000"))
        assertEquals(Catalog.TIMEOUT_1_MIN, d.of(Catalog.Timeout))
    }

    @Test
    fun `setting the app default removes the stored value`() {
        val d = Defaults().with(Catalog.AlwaysOn, Toggle.ON).with(Catalog.AlwaysOn, Toggle.OFF)
        assertTrue(d.values.isEmpty())
    }

    @Test
    fun `stored keys are read back`() {
        val d =
            Defaults.Keys.read(
                strings = mapOf("default.timeout" to "300000", "something.else" to "x"),
                booleans = mapOf("notify.Colour" to true, "notify.Gone" to false, "defaults_seeded" to true),
                hostnames = setOf("dns.example.com", "not valid"),
            )
        assertEquals(mapOf("timeout" to "300000"), d.values)
        assertEquals(mapOf(Group.Colour to true), d.notify)
        assertEquals(setOf("dns.example.com"), d.hostnames)
        assertTrue(d.seeded)
    }

    @Test
    fun `key names`() {
        assertEquals("default.timeout", Defaults.Keys.value(Catalog.Timeout))
        assertEquals("notify.Screen", Defaults.Keys.notify(Group.Screen))
        assertEquals("notify_setting.timeout", Defaults.Keys.notify(Catalog.Timeout))
    }

    @Test
    fun `a setting notifies like its popup did until it has its own choice`() {
        val d = Defaults(notify = mapOf(Group.Colour to true))
        assertTrue(d.notifies(Catalog.Grayscale))
        assertTrue(d.notifies(Catalog.AlwaysOn))
        assertFalse(d.notifies(Catalog.Ringer))
        val changed = d.withNotify(Catalog.Grayscale, false)
        assertFalse(changed.notifies(Catalog.Grayscale))
        assertTrue(changed.notifies(Catalog.NightLight))
    }

    @Test
    fun `a popup shows every row until some are left out`() {
        val d = Defaults()
        assertEquals(Group.Screen.rows, d.rowsOf(Group.Screen))
        val rotation = Group.Screen.rows.first { it.setting == Catalog.Rotation }
        val hidden = d.withRow(rotation, shown = false)
        assertFalse(rotation in hidden.rowsOf(Group.Screen))
        assertFalse(Catalog.Rotation in hidden.trackedOf(Group.Screen))
        assertEquals(Group.Screen.rows, hidden.withRow(rotation, shown = true).rowsOf(Group.Screen))
    }

    @Test
    fun `the last row shown can't be left out`() {
        val dns = Group.Network.rows.single()
        assertEquals(Defaults(), Defaults().withRow(dns, shown = false))
        val sound = Group.Sound.rows
        val one = sound.drop(1).fold(Defaults()) { d, row -> d.withRow(row, shown = false) }
        assertEquals(listOf(sound.first()), one.rowsOf(Group.Sound))
        assertEquals(one, one.withRow(sound.first(), shown = false))
    }

    @Test
    fun `a popup with every row stored as hidden shows them all`() {
        val d = Defaults(hiddenRows = Group.Network.rows.map { it.setting.id }.toSet())
        assertEquals(Group.Network.rows, d.rowsOf(Group.Network))
    }

    @Test
    fun `new choices are read back`() {
        val d =
            Defaults.Keys.read(
                strings = mapOf("shortcut.1" to "", "shortcut.2" to "Grayscale", "shortcut.3" to "Gone", "shortcut.9" to "Ringer"),
                booleans = mapOf("notify_setting.grayscale" to true),
                hostnames = emptySet(),
                hiddenRows = setOf("rotation"),
            )
        // The shortcut choices of earlier versions are ignored: the shortcuts open the popups (#24).
        assertEquals(Defaults(notifySettings = mapOf("grayscale" to true), hiddenRows = setOf("rotation")), d)
        assertEquals(mapOf("grayscale" to true), d.notifySettings)
        assertEquals(setOf("rotation"), d.hiddenRows)
    }
}
