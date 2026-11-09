package to.wim.quicksettings.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateDnsTest {
    private val hostname = "global/private_dns_specifier" to "dns.example.com"

    @Test
    fun `mode values map to Android's`() {
        assertEquals(DnsMode.Hostname, DnsMode.from("hostname"))
        assertEquals(DnsMode.Off, DnsMode.from("off"))
        assertEquals(DnsMode.Automatic, DnsMode.from("opportunistic"))
    }

    @Test
    fun `a missing mode is Automatic`() {
        assertEquals(DnsMode.Automatic, DnsMode.from(null))
        assertEquals(PrivateDnsSetting.AUTOMATIC, PrivateDnsSetting.encoded(device().first))
    }

    @Test
    fun `a hostname in use reads with its name`() {
        val (d, _) = device("global/private_dns_mode" to "hostname", hostname)
        assertEquals("hostname:dns.example.com", PrivateDnsSetting.encoded(d))
    }

    @Test
    fun `writing a named hostname sets it before the mode`() {
        val (d, raw) = device()
        PrivateDnsSetting.writeEncoded(d, "hostname:dns.other.org")
        assertEquals(listOf("global/private_dns_specifier=dns.other.org", "global/private_dns_mode=hostname"), raw.writes)
    }

    @Test
    fun `any hostname needs one set in Android`() {
        val (d, raw) = device("global/private_dns_mode" to "opportunistic")
        PrivateDnsSetting.writeEncoded(d, PrivateDnsSetting.ANY_HOSTNAME)
        assertTrue(raw.writes.isEmpty())
    }

    @Test
    fun `the any-hostname default matches every hostname`() {
        assertTrue(PrivateDnsSetting.matches("hostname:a.b", PrivateDnsSetting.ANY_HOSTNAME))
        assertFalse(PrivateDnsSetting.matches(PrivateDnsSetting.AUTOMATIC, PrivateDnsSetting.ANY_HOSTNAME))
        assertFalse(PrivateDnsSetting.matches("hostname:a.b", "hostname:c.d"))
    }

    @Test
    fun `hostname of a value`() {
        assertEquals("a.b", PrivateDnsSetting.hostnameOf("hostname:a.b"))
        assertNull(PrivateDnsSetting.hostnameOf(PrivateDnsSetting.OFF))
    }

    @Test
    fun `hostnames are cleaned and checked`() {
        assertEquals("dns.example.com", Hostnames.clean("  HTTPS://DNS.Example.com/ "))
        assertTrue(Hostnames.isValid("dns.example.com"))
        assertTrue(Hostnames.isValid("1dot1dot1dot1.cloudflare-dns.com"))
        assertFalse(Hostnames.isValid("localhost"))
        assertFalse(Hostnames.isValid("bad_name.com"))
        assertFalse(Hostnames.isValid("-dash.com"))
        assertFalse(Hostnames.isValid(""))
    }

    @Test
    fun `merged hostnames include Android's, once, sorted`() {
        assertEquals(listOf("a.com", "b.com"), Hostnames.merge(setOf("b.com", "a.com"), "A.com"))
        assertEquals(listOf("b.com"), Hostnames.merge(setOf("b.com"), null))
    }

    @Test
    fun `valid defaults`() {
        assertTrue(PrivateDnsSetting.isValid("hostname:dns.example.com"))
        assertFalse(PrivateDnsSetting.isValid("hostname:nope"))
        assertFalse(PrivateDnsSetting.isValid("something"))
    }
}
