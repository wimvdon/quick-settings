package to.wim.screener.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BrightnessCurveTest {
    @Test
    fun `the ends of the slider are the ends of the brightness range`() {
        assertEquals(BrightnessCurve.MIN, BrightnessCurve.toBrightness(0f))
        assertEquals(BrightnessCurve.MAX, BrightnessCurve.toBrightness(1f))
        assertEquals(0f, BrightnessCurve.toPosition(BrightnessCurve.MIN), 0.001f)
        assertEquals(1f, BrightnessCurve.toPosition(BrightnessCurve.MAX), 0.001f)
    }

    @Test
    fun `positions outside the slider are clamped`() {
        assertEquals(BrightnessCurve.MIN, BrightnessCurve.toBrightness(-1f))
        assertEquals(BrightnessCurve.MAX, BrightnessCurve.toBrightness(2f))
    }

    @Test
    fun `half way on the slider is much dimmer than half the range, like Android`() {
        // HLG gamma: 50% on the system slider is about 1/12 of full brightness.
        val half = BrightnessCurve.toBrightness(0.5f)
        assertTrue("was $half", half in 20..25)
    }

    @Test
    fun `brightness only goes up as the slider moves right`() {
        val levels = (0..100).map { BrightnessCurve.toBrightness(it / 100f) }
        assertEquals(levels.sorted(), levels)
    }

    @Test
    fun `position and brightness convert back and forth`() {
        for (level in BrightnessCurve.MIN..BrightnessCurve.MAX) {
            val back = BrightnessCurve.toBrightness(BrightnessCurve.toPosition(level))
            assertTrue("level $level came back as $back", abs(back - level) <= 1)
        }
    }

    @Test
    fun `percent matches the slider position`() {
        assertEquals(100, BrightnessCurve.toPercent(BrightnessCurve.MAX))
        assertEquals(0, BrightnessCurve.toPercent(BrightnessCurve.MIN))
    }
}
