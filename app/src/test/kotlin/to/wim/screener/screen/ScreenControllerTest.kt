package to.wim.screener.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenControllerTest {
    /** Records every write, in order, and applies it to an in-memory state. */
    private class FakeSettings(var state: ScreenState = ScreenState.DEFAULTS) : ScreenSettings {
        val writes = mutableListOf<String>()

        override fun read() = state

        override fun setAutoBrightness(auto: Boolean) {
            writes += "auto=$auto"
            state = state.copy(autoBrightness = auto)
        }

        override fun setBrightness(level: Int) {
            writes += "brightness=$level"
            // Like Android: a brightness written while auto brightness is on doesn't stick.
            if (!state.autoBrightness) state = state.copy(brightness = level)
        }

        override fun setTimeout(timeoutMs: Int) {
            writes += "timeout=$timeoutMs"
            state = state.copy(timeoutMs = timeoutMs)
        }

        override fun setAlwaysOn(on: Boolean) {
            writes += "aod=$on"
            state = state.copy(alwaysOn = on)
        }

        override fun setTapToWake(on: Boolean) {
            writes += "tap=$on"
            state = state.copy(tapToWake = on)
        }
    }

    private val settings = FakeSettings()
    private val controller = ScreenController(settings)

    @Test
    fun `manual brightness switches auto brightness off first`() {
        controller.setBrightness(100)
        assertEquals(listOf("auto=false", "brightness=100"), settings.writes)
        assertEquals(100, settings.state.brightness)
        assertEquals(false, settings.state.autoBrightness)
    }

    @Test
    fun `manual brightness does not rewrite the mode when already manual`() {
        settings.state = settings.state.copy(autoBrightness = false)
        controller.setBrightness(80)
        assertEquals(listOf("brightness=80"), settings.writes)
    }

    @Test
    fun `brightness is kept within the system range`() {
        controller.setBrightness(999)
        controller.setBrightness(-5)
        assertEquals(listOf("auto=false", "brightness=255", "brightness=1"), settings.writes)
    }

    @Test
    fun `slider position goes through the brightness curve`() {
        controller.setBrightnessPosition(1f)
        assertEquals(BrightnessCurve.MAX, settings.state.brightness)
    }

    @Test
    fun `reset brightness turns auto brightness back on`() {
        controller.setBrightness(50)
        controller.resetBrightness()
        assertEquals(true, settings.state.autoBrightness)
    }

    @Test
    fun `extending the timeout sets 30 minutes and resetting sets 1 minute`() {
        controller.setTimeoutExtended(true)
        assertEquals(30 * 60_000, settings.state.timeoutMs)
        controller.setTimeoutExtended(false)
        assertEquals(60_000, settings.state.timeoutMs)
    }

    @Test
    fun `reset puts every setting back to its default`() {
        settings.state =
            ScreenState(autoBrightness = false, brightness = 10, timeoutMs = 1_800_000, alwaysOn = true, tapToWake = false)
        ScreenState.Change.entries.forEach(controller::reset)
        assertEquals(emptyList<ScreenState.Change>(), settings.state.changes)
    }
}
