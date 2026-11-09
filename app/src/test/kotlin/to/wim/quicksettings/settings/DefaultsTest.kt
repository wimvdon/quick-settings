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
    }
}
