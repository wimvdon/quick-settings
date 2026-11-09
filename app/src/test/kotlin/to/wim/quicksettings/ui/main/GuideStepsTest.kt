package to.wim.quicksettings.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideStepsTest {
    @Test
    fun `one step per line`() {
        assertEquals(listOf("Open Settings.", "Tap it."), GuideSteps.split("Open Settings.\nTap it."))
    }

    @Test
    fun `numbers and blank lines are dropped`() {
        assertEquals(listOf("First", "Second"), GuideSteps.split("1. First\n\n 2) Second \n"))
    }

    @Test
    fun `numbers inside a step stay`() {
        assertEquals(listOf("Tap Build number 7 times."), GuideSteps.split("Tap Build number 7 times."))
    }
}
