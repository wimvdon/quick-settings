package to.wim.quicksettings.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeoutDurationTest {
    @Test
    fun `whole hours are shown in hours`() {
        assertEquals(TimeoutDuration(2, TimeoutDuration.Unit.Hours), TimeoutDuration.of(7_200_000))
    }

    @Test
    fun `minutes`() {
        assertEquals(TimeoutDuration(30, TimeoutDuration.Unit.Minutes), TimeoutDuration.of(1_800_000))
        assertEquals(TimeoutDuration(90, TimeoutDuration.Unit.Minutes), TimeoutDuration.of(5_400_000))
    }

    @Test
    fun `under a minute in seconds`() {
        assertEquals(TimeoutDuration(15, TimeoutDuration.Unit.Seconds), TimeoutDuration.of(15_000))
    }

    @Test
    fun `negative values count as zero`() {
        assertEquals(TimeoutDuration(0, TimeoutDuration.Unit.Seconds), TimeoutDuration.of(-5))
    }
}
