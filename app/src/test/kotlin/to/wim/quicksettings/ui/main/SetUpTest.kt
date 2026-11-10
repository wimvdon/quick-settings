package to.wim.quicksettings.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import to.wim.quicksettings.settings.Catalog
import to.wim.quicksettings.settings.Group

class SetUpTest {
    @Test
    fun `a sheet survives as text`() {
        listOf(SetUp.Popup(Group.Sound), SetUp.Setting(Catalog.Timeout), SetUp.Setting(Catalog.PrivateDns)).forEach {
            assertEquals(it, SetUp.parse(it.key))
        }
    }

    @Test
    fun `unknown text opens nothing`() {
        assertNull(SetUp.parse(null))
        assertNull(SetUp.parse("popup:Gone"))
        assertNull(SetUp.parse("setting:brightness_level"))
        assertNull(SetUp.parse("something"))
    }
}
