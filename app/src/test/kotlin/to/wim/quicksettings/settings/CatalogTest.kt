package to.wim.quicksettings.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    @Test
    fun `manual brightness switches auto brightness off first`() {
        val (d, raw) = device("system/screen_brightness_mode" to "1")
        Catalog.Brightness.write(d, 1f)
        assertEquals(listOf("system/screen_brightness_mode=0", "system/screen_brightness=255"), raw.writes)
    }

    @Test
    fun `brightness does not rewrite the mode when already manual`() {
        val (d, raw) = device("system/screen_brightness_mode" to "0")
        Catalog.Brightness.write(d, 0f)
        assertEquals(listOf("system/screen_brightness=1"), raw.writes)
    }

    @Test
    fun `missing brightness mode reads as auto`() {
        val (d, _) = device()
        assertEquals(Catalog.BRIGHTNESS_AUTO, Catalog.BrightnessMode.read(d))
    }

    @Test
    fun `timeout reads any value Android has`() {
        val (d, _) = device("system/screen_off_timeout" to "120000")
        assertEquals("120000", Catalog.Timeout.read(d))
        assertFalse(Catalog.Timeout.isValid("120000"))
    }

    @Test
    fun `tap to wake keeps both of Android's keys in step`() {
        val (d, raw) = device()
        Catalog.TapToWake.write(d, false)
        assertEquals(listOf("secure/doze_tap_gesture=0", "secure/double_tap_to_wake=0"), raw.writes)
        assertFalse(Catalog.TapToWake.read(d))
    }

    @Test
    fun `missing tap to wake reads as on`() {
        assertTrue(Catalog.TapToWake.read(device().first))
    }

    @Test
    fun `rotation reads auto and the locked directions`() {
        assertEquals(Catalog.ROTATION_AUTO, Catalog.Rotation.read(device("system/accelerometer_rotation" to "1").first))
        assertEquals(Catalog.ROTATION_PORTRAIT, Catalog.Rotation.read(device().first))
        assertEquals(
            Catalog.ROTATION_LANDSCAPE,
            Catalog.Rotation.read(device("system/accelerometer_rotation" to "0", "system/user_rotation" to "1").first),
        )
        assertEquals("upside_down", Catalog.Rotation.read(device("system/user_rotation" to "2").first))
    }

    @Test
    fun `locking rotation sets the direction before switching auto-rotate off`() {
        val (d, raw) = device("system/accelerometer_rotation" to "1")
        Catalog.Rotation.write(d, Catalog.ROTATION_LANDSCAPE)
        assertEquals(listOf("system/user_rotation=1", "system/accelerometer_rotation=0"), raw.writes)
    }

    @Test
    fun `grayscale is colour correction in monochrome mode`() {
        val (d, raw) = device()
        Catalog.Grayscale.write(d, true)
        assertEquals(
            listOf("secure/accessibility_display_daltonizer=0", "secure/accessibility_display_daltonizer_enabled=1"),
            raw.writes,
        )
        assertTrue(Catalog.Grayscale.read(d))
    }

    @Test
    fun `switching grayscale off restores the colour correction mode`() {
        val (d, raw) =
            device("secure/accessibility_display_daltonizer_enabled" to "1", "secure/accessibility_display_daltonizer" to "0")
        Catalog.Grayscale.write(d, false)
        assertEquals("12", raw.values["secure/accessibility_display_daltonizer"])
        assertFalse(Catalog.Grayscale.read(d))
    }

    @Test
    fun `colour correction in another mode is not grayscale`() {
        val (d, _) =
            device("secure/accessibility_display_daltonizer_enabled" to "1", "secure/accessibility_display_daltonizer" to "12")
        assertFalse(Catalog.Grayscale.read(d))
    }

    @Test
    fun `night light warmth maps the kelvin range`() {
        val (d, raw) = device()
        Catalog.NightLightWarmth.write(d, 1f)
        assertEquals("2596", raw.values["secure/night_display_color_temperature"])
        Catalog.NightLightWarmth.write(d, 0f)
        assertEquals("4082", raw.values["secure/night_display_color_temperature"])
        assertEquals(0f, Catalog.NightLightWarmth.read(d))
    }

    @Test
    fun `extra dim level is a percentage`() {
        val (d, raw) = device()
        Catalog.ExtraDimLevel.write(d, 0.42f)
        assertEquals("42", raw.values["secure/reduce_bright_colors_level"])
    }

    @Test
    fun `animations set all three scales`() {
        val (d, raw) = device()
        Catalog.Animations.write(d, Catalog.ANIMATIONS_OFF)
        assertEquals(
            listOf(
                "global/window_animation_scale=0",
                "global/transition_animation_scale=0",
                "global/animator_duration_scale=0",
            ),
            raw.writes,
        )
        assertEquals(Catalog.ANIMATIONS_OFF, Catalog.Animations.read(d))
    }

    @Test
    fun `animation scales read as short numbers`() {
        assertEquals("1", Catalog.Animations.read(device().first))
        assertEquals("0.5", Catalog.Animations.read(device("global/window_animation_scale" to "0.5").first))
        assertEquals("1.5", Catalog.Animations.read(device("global/window_animation_scale" to "1.5").first))
        assertEquals("0", Catalog.Animations.read(device("global/window_animation_scale" to "0.0").first))
    }

    @Test
    fun `stay awake covers every charger`() {
        val (d, raw) = device()
        Catalog.StayAwake.write(d, true)
        assertEquals("7", raw.values["global/stay_on_while_plugged_in"])
        assertTrue(Catalog.StayAwake.read(device("global/stay_on_while_plugged_in" to "2").first))
    }

    @Test
    fun `ringer uses the audio service`() {
        val audio = FakeAudio(RingerMode.Silent)
        val (d, _) = device(audio = audio)
        assertEquals(Catalog.RINGER_SILENT, Catalog.Ringer.read(d))
        Catalog.Ringer.write(d, Catalog.RINGER_VIBRATE)
        assertEquals(RingerMode.Vibrate, audio.ringerMode)
    }

    @Test
    fun `heads-up missing reads as on`() {
        assertTrue(Catalog.HeadsUp.read(device().first))
    }

    @Test
    fun `every setting id is unique`() {
        val ids = Catalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every tracked default is valid`() {
        Catalog.all.filterIsInstance<Tracked>().forEach { assertTrue(it.id, it.isValid(it.appDefault)) }
    }
}
