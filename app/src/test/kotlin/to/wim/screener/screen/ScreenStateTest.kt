package to.wim.screener.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenStateTest {
    @Test
    fun `defaults have no changes`() {
        assertEquals(emptyList<ScreenState.Change>(), ScreenState.DEFAULTS.changes)
    }

    @Test
    fun `defaults are auto brightness, 1 minute, always-on off and tap to wake on`() {
        val d = ScreenState.DEFAULTS
        assertTrue(d.autoBrightness)
        assertEquals(60_000, d.timeoutMs)
        assertFalse(d.alwaysOn)
        assertTrue(d.tapToWake)
    }

    @Test
    fun `every setting that differs from its default is a change, in display order`() {
        val state =
            ScreenState(autoBrightness = false, brightness = 10, timeoutMs = 1_800_000, alwaysOn = true, tapToWake = false)
        assertEquals(
            listOf(
                ScreenState.Change.Brightness,
                ScreenState.Change.Timeout,
                ScreenState.Change.AlwaysOn,
                ScreenState.Change.TapToWake,
            ),
            state.changes,
        )
    }

    @Test
    fun `brightness level alone is not a change while auto brightness is on`() {
        assertEquals(emptyList<ScreenState.Change>(), ScreenState.DEFAULTS.copy(brightness = 3).changes)
    }

    @Test
    fun `any timeout other than 1 minute counts as extended`() {
        assertTrue(ScreenState.DEFAULTS.copy(timeoutMs = 30_000).timeoutExtended)
        assertTrue(ScreenState.DEFAULTS.copy(timeoutMs = 120_000).timeoutExtended)
        assertFalse(ScreenState.DEFAULTS.timeoutExtended)
    }

    @Test
    fun `timeouts are shown in the largest whole unit`() {
        assertEquals(TimeoutDuration(30, TimeoutDuration.Unit.Minutes), TimeoutDuration.of(1_800_000))
        assertEquals(TimeoutDuration(1, TimeoutDuration.Unit.Minutes), TimeoutDuration.of(60_000))
        assertEquals(TimeoutDuration(15, TimeoutDuration.Unit.Seconds), TimeoutDuration.of(15_000))
        assertEquals(TimeoutDuration(2, TimeoutDuration.Unit.Hours), TimeoutDuration.of(7_200_000))
        assertEquals(TimeoutDuration(90, TimeoutDuration.Unit.Minutes), TimeoutDuration.of(5_400_000))
        assertEquals(TimeoutDuration(0, TimeoutDuration.Unit.Seconds), TimeoutDuration.of(-1))
    }
}
