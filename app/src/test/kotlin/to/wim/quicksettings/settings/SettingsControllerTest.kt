package to.wim.quicksettings.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsControllerTest {
    private var defaults = Defaults()

    /** A phone with Android's 1-minute timeout (always set on a real phone), plus [values]. */
    private fun controller(vararg values: Pair<String, String>): Pair<SettingsController, FakeRaw> {
        val (d, raw) = device("system/screen_off_timeout" to "60000", *values)
        return SettingsController(d) { defaults } to raw
    }

    @Test
    fun `nothing changed at Android's defaults`() {
        val (c, _) = controller("global/private_dns_mode" to "hostname", "global/private_dns_specifier" to "a.com")
        Group.entries.forEach { assertEquals(it.name, emptyList<Tracked>(), c.changes(it)) }
    }

    @Test
    fun `changes are listed in popup order`() {
        val (c, _) = controller("secure/doze_always_on" to "1", "system/screen_brightness_mode" to "0")
        assertEquals(listOf(Catalog.BrightnessMode, Catalog.AlwaysOn), c.changes(Group.Screen))
    }

    @Test
    fun `a user default turns a change into the normal state`() {
        val (c, _) = controller("secure/doze_always_on" to "1")
        defaults = defaults.with(Catalog.AlwaysOn, Toggle.ON)
        assertTrue(c.changes(Group.Screen).isEmpty())
    }

    @Test
    fun `a timeout set elsewhere counts as changed`() {
        val (c, _) = controller("system/screen_off_timeout" to "120000")
        assertEquals(listOf(Catalog.Timeout), c.changes(Group.Screen))
    }

    @Test
    fun `reset writes the defaults of only what changed`() {
        val (c, raw) = controller("secure/doze_always_on" to "1", "system/screen_brightness_mode" to "0")
        c.reset(Group.Screen)
        assertEquals(listOf("system/screen_brightness_mode=1", "secure/doze_always_on=0"), raw.writes)
        assertTrue(c.changes(Group.Screen).isEmpty())
    }

    @Test
    fun `reset uses the user's defaults`() {
        defaults = defaults.with(Catalog.Timeout, Catalog.TIMEOUT_5_MIN)
        val (c, raw) = controller("system/screen_off_timeout" to "60000")
        c.reset(Group.Screen)
        assertEquals(listOf("system/screen_off_timeout=300000"), raw.writes)
    }

    @Test
    fun `toggle returns the new state`() {
        val (c, _) = controller()
        assertTrue(c.toggle(Catalog.AlwaysOn))
        assertFalse(c.toggle(Catalog.AlwaysOn))
    }

    @Test
    fun `cycle steps through the options and wraps`() {
        val (c, _) = controller("global/window_animation_scale" to "1")
        assertEquals(Catalog.ANIMATIONS_OFF, c.cycle(Catalog.Animations))
        assertEquals(Catalog.ANIMATIONS_HALF, c.cycle(Catalog.Animations))
        assertEquals(Catalog.ANIMATIONS_NORMAL, c.cycle(Catalog.Animations))
    }

    @Test
    fun `cycle from an unknown value starts at the first option`() {
        val (c, _) = controller("system/user_rotation" to "2")
        assertEquals(Catalog.ROTATION_AUTO, c.cycle(Catalog.Rotation))
    }

    @Test
    fun `timeout toggles between the default and 30 minutes`() {
        val (c, raw) = controller("system/screen_off_timeout" to "60000")
        assertTrue(c.toggleTimeout())
        assertEquals("1800000", raw.values["system/screen_off_timeout"])
        assertFalse(c.toggleTimeout())
        assertEquals("60000", raw.values["system/screen_off_timeout"])
    }

    @Test
    fun `with 30 minutes as default the timeout toggles to 1 minute`() {
        defaults = defaults.with(Catalog.Timeout, Catalog.TIMEOUT_30_MIN)
        val (c, raw) = controller("system/screen_off_timeout" to "1800000")
        assertTrue(c.toggleTimeout())
        assertEquals("60000", raw.values["system/screen_off_timeout"])
    }

    @Test
    fun `a timeout set elsewhere toggles back to the default`() {
        val (c, raw) = controller("system/screen_off_timeout" to "120000")
        assertFalse(c.toggleTimeout())
        assertEquals("60000", raw.values["system/screen_off_timeout"])
    }

    @Test
    fun `dns toggles from a hostname to Automatic and back`() {
        val (c, raw) = controller("global/private_dns_mode" to "hostname", "global/private_dns_specifier" to "a.com")
        assertTrue(c.toggleDns())
        assertEquals("opportunistic", raw.values["global/private_dns_mode"])
        assertFalse(c.dnsOnHostname())
        assertTrue(c.toggleDns())
        assertEquals("hostname", raw.values["global/private_dns_mode"])
        assertTrue(c.dnsOnHostname())
    }

    @Test
    fun `dns toggles to the default's hostname`() {
        defaults = defaults.with(Catalog.PrivateDns, "hostname:b.com")
        val (c, raw) = controller("global/private_dns_mode" to "off", "global/private_dns_specifier" to "a.com")
        assertTrue(c.toggleDns())
        assertEquals("b.com", raw.values["global/private_dns_specifier"])
    }

    @Test
    fun `dns without any hostname changes nothing`() {
        val (c, raw) = controller("global/private_dns_mode" to "opportunistic")
        assertFalse(c.toggleDns())
        assertTrue(raw.writes.isEmpty())
    }

    @Test
    fun `current values become defaults, app defaults are not stored`() {
        val (c, _) = controller("secure/doze_always_on" to "1", "system/screen_off_timeout" to "120000")
        val saved = c.currentAsDefaults(Group.Screen)
        assertEquals(mapOf("always_on" to "on"), saved.values)
    }

    @Test
    fun `the first start takes new settings from the phone, not the screen settings`() {
        val (c, _) =
            controller(
                "global/adb_enabled" to "1",
                "system/accelerometer_rotation" to "1",
                "secure/doze_always_on" to "1",
                "system/screen_brightness_mode" to "0",
            )
        val seeded = c.seedDefaults()
        assertTrue(seeded.seeded)
        assertEquals(mapOf("usb_debugging" to "on", "rotation" to "auto"), seeded.values)
        defaults = seeded
        assertEquals(listOf(Catalog.BrightnessMode, Catalog.AlwaysOn), c.changes(Group.Screen))
        assertTrue(c.changes(Group.Developer).isEmpty())
    }
}
