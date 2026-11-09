package to.wim.quicksettings.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateDnsTest {
    private class FakeDns(var state: DnsState) : DnsSettings {
        val writes = mutableListOf<DnsMode>()

        override fun read() = state

        override fun setMode(mode: DnsMode) {
            writes += mode
            state = state.copy(mode = mode)
        }
    }

    @Test
    fun `stored values map to modes, and a missing value means Automatic`() {
        assertEquals(DnsMode.Hostname, DnsMode.from("hostname"))
        assertEquals(DnsMode.Off, DnsMode.from("off"))
        assertEquals(DnsMode.Opportunistic, DnsMode.from("opportunistic"))
        assertEquals(DnsMode.Opportunistic, DnsMode.from(null))
        assertEquals(DnsMode.Opportunistic, DnsMode.from("something new"))
    }

    @Test
    fun `the tile is active only with the hostname`() {
        assertTrue(DnsState(DnsMode.Hostname, "dns.example").tileActive)
        assertFalse(DnsState(DnsMode.Opportunistic, "dns.example").tileActive)
        assertFalse(DnsState(DnsMode.Off, "dns.example").tileActive)
    }

    @Test
    fun `the notification shows only on Automatic`() {
        assertTrue(DnsState(DnsMode.Opportunistic, "dns.example").showNotification)
        assertFalse(DnsState(DnsMode.Hostname, "dns.example").showNotification)
        assertFalse(DnsState(DnsMode.Off, null).showNotification)
    }

    @Test
    fun `toggling switches between hostname and Automatic`() {
        val dns = FakeDns(DnsState(DnsMode.Hostname, "dns.example"))
        val controller = DnsController(dns)
        assertTrue(controller.toggle())
        assertEquals(DnsMode.Opportunistic, dns.state.mode)
        assertTrue(controller.toggle())
        assertEquals(DnsMode.Hostname, dns.state.mode)
        assertEquals("dns.example", dns.state.hostname)
    }

    @Test
    fun `from Off a toggle goes to the hostname`() {
        assertEquals(DnsMode.Hostname, DnsState(DnsMode.Off, "dns.example").toggled())
    }

    @Test
    fun `without a hostname nothing is switched`() {
        val dns = FakeDns(DnsState(DnsMode.Opportunistic, " "))
        val controller = DnsController(dns)
        assertNull(dns.state.toggled())
        assertFalse(controller.toggle())
        assertFalse(controller.useHostname())
        assertEquals(emptyList<DnsMode>(), dns.writes)
    }

    @Test
    fun `use hostname goes back from Automatic`() {
        val dns = FakeDns(DnsState(DnsMode.Opportunistic, "dns.example"))
        assertTrue(DnsController(dns).useHostname())
        assertEquals(DnsMode.Hostname, dns.state.mode)
    }
}
