package to.wim.quicksettings.settings

import kotlin.math.roundToInt

/**
 * Every setting the app can change, with where Android keeps it and the app's default. The keys
 * are Android's own names (see docs/architecture.md for the table and what each one needs).
 *
 * Private System settings (show taps, mono audio, smooth display) are not here: Android refuses
 * them to apps, even with the adb permission (see docs/decisions.md).
 */
object Catalog {
    // ---- Screen ----------------------------------------------------------------------------

    const val BRIGHTNESS_AUTO = "auto"
    const val BRIGHTNESS_MANUAL = "manual"
    private const val KEY_BRIGHTNESS_MODE = "screen_brightness_mode"
    private const val KEY_BRIGHTNESS = "screen_brightness"

    /** Auto or manual brightness. Android's values: 1 = automatic, 0 = manual. */
    val BrightnessMode =
        Choice(
            id = "brightness",
            access = Access.System,
            keys = listOf(Table.System to KEY_BRIGHTNESS_MODE),
            options = listOf(BRIGHTNESS_AUTO, BRIGHTNESS_MANUAL),
            appDefault = BRIGHTNESS_AUTO,
            reader = { d ->
                if ((d.int(Table.System, KEY_BRIGHTNESS_MODE) ?: 1) == 1) BRIGHTNESS_AUTO else BRIGHTNESS_MANUAL
            },
            writer = { d, v -> d.put(Table.System, KEY_BRIGHTNESS_MODE, if (v == BRIGHTNESS_AUTO) 1 else 0) },
        )

    /**
     * The brightness slider. Auto brightness is switched off first: while it is on, Android
     * ignores (or overrides) a written level.
     */
    val Brightness =
        Level(
            id = "brightness_level",
            access = Access.System,
            keys = listOf(Table.System to KEY_BRIGHTNESS),
            reader = { d -> BrightnessCurve.toPosition(d.int(Table.System, KEY_BRIGHTNESS) ?: (BrightnessCurve.MAX / 2)) },
            writer = { d, p ->
                if (BrightnessMode.read(d) == BRIGHTNESS_AUTO) BrightnessMode.write(d, BRIGHTNESS_MANUAL)
                d.put(Table.System, KEY_BRIGHTNESS, BrightnessCurve.toBrightness(p))
            },
        )

    const val TIMEOUT_1_MIN = "60000"
    const val TIMEOUT_5_MIN = "300000"
    const val TIMEOUT_30_MIN = "1800000"
    private const val KEY_TIMEOUT = "screen_off_timeout"

    /** Screen timeout in milliseconds. */
    val Timeout =
        Choice(
            id = "timeout",
            access = Access.System,
            keys = listOf(Table.System to KEY_TIMEOUT),
            options = listOf(TIMEOUT_1_MIN, TIMEOUT_5_MIN, TIMEOUT_30_MIN),
            appDefault = TIMEOUT_1_MIN,
            reader = { d -> d.int(Table.System, KEY_TIMEOUT)?.toString() },
            writer = { d, v -> v.toIntOrNull()?.let { d.put(Table.System, KEY_TIMEOUT, it) } },
        )

    val AlwaysOn = secureToggle("always_on", Table.Secure, "doze_always_on", defaultOn = false, missing = false)

    private const val KEY_TAP_GESTURE = "doze_tap_gesture"
    private const val KEY_DOUBLE_TAP = "double_tap_to_wake"

    /**
     * Pixels use "Tap to check phone" (doze_tap_gesture), AOSP-based phones "Tap to wake"
     * (double_tap_to_wake). Both are kept in step; a phone ignores the one it doesn't use.
     */
    val TapToWake =
        Toggle(
            id = "tap_to_wake",
            access = Access.Secure,
            keys = listOf(Table.Secure to KEY_TAP_GESTURE, Table.Secure to KEY_DOUBLE_TAP),
            defaultOn = true,
            reader = { d -> d.bool(Table.Secure, KEY_TAP_GESTURE, missing = true) },
            writer = { d, on ->
                d.put(Table.Secure, KEY_TAP_GESTURE, on)
                d.put(Table.Secure, KEY_DOUBLE_TAP, on)
            },
        )

    const val ROTATION_AUTO = "auto"
    const val ROTATION_PORTRAIT = "portrait"
    const val ROTATION_LANDSCAPE = "landscape"
    private const val ROTATION_UPSIDE_DOWN = "upside_down"
    private const val ROTATION_LANDSCAPE_REVERSED = "landscape_reversed"
    private const val KEY_AUTO_ROTATE = "accelerometer_rotation"
    private const val KEY_USER_ROTATION = "user_rotation"

    /**
     * Auto-rotate, or the screen locked in one direction. Android's user_rotation counts quarter
     * turns from the natural (portrait) direction: 0 portrait, 1 landscape, 2 upside down, 3 the
     * other landscape. Apps that fix their own direction still win.
     */
    val Rotation =
        Choice(
            id = "rotation",
            access = Access.System,
            keys = listOf(Table.System to KEY_AUTO_ROTATE, Table.System to KEY_USER_ROTATION),
            options = listOf(ROTATION_AUTO, ROTATION_PORTRAIT, ROTATION_LANDSCAPE),
            appDefault = ROTATION_PORTRAIT,
            reader = { d ->
                if (d.bool(Table.System, KEY_AUTO_ROTATE, missing = false)) {
                    ROTATION_AUTO
                } else {
                    when (d.int(Table.System, KEY_USER_ROTATION) ?: 0) {
                        0 -> ROTATION_PORTRAIT
                        1 -> ROTATION_LANDSCAPE
                        2 -> ROTATION_UPSIDE_DOWN
                        else -> ROTATION_LANDSCAPE_REVERSED
                    }
                }
            },
            writer = { d, v ->
                if (v == ROTATION_AUTO) {
                    d.put(Table.System, KEY_AUTO_ROTATE, true)
                } else {
                    d.put(Table.System, KEY_USER_ROTATION, if (v == ROTATION_LANDSCAPE) 1 else 0)
                    d.put(Table.System, KEY_AUTO_ROTATE, false)
                }
            },
        )

    // ---- Colour ----------------------------------------------------------------------------

    val NightLight = secureToggle("night_light", Table.Secure, "night_display_activated", defaultOn = false, missing = false)

    /** Android's Night Light range in kelvin (AOSP and Pixel); lower is warmer. */
    const val NIGHT_LIGHT_COOLEST = 4082
    const val NIGHT_LIGHT_WARMEST = 2596
    private const val NIGHT_LIGHT_DEFAULT = 2850
    private const val KEY_NIGHT_LIGHT_TEMPERATURE = "night_display_color_temperature"

    /** Warmth: 0 = least warm, 1 = warmest. */
    val NightLightWarmth =
        Level(
            id = "night_light_warmth",
            access = Access.Secure,
            keys = listOf(Table.Secure to KEY_NIGHT_LIGHT_TEMPERATURE),
            reader = { d ->
                val kelvin = d.int(Table.Secure, KEY_NIGHT_LIGHT_TEMPERATURE) ?: NIGHT_LIGHT_DEFAULT
                (NIGHT_LIGHT_COOLEST - kelvin).toFloat() / (NIGHT_LIGHT_COOLEST - NIGHT_LIGHT_WARMEST)
            },
            writer = { d, p ->
                val kelvin = NIGHT_LIGHT_COOLEST - (p * (NIGHT_LIGHT_COOLEST - NIGHT_LIGHT_WARMEST)).roundToInt()
                d.put(Table.Secure, KEY_NIGHT_LIGHT_TEMPERATURE, kelvin)
            },
        )

    val ExtraDim = secureToggle("extra_dim", Table.Secure, "reduce_bright_colors_activated", defaultOn = false, missing = false)

    private const val KEY_EXTRA_DIM_LEVEL = "reduce_bright_colors_level"
    private const val EXTRA_DIM_LEVEL_DEFAULT = 30

    /** Extra dim intensity, which Android keeps as 0..100. */
    val ExtraDimLevel =
        Level(
            id = "extra_dim_level",
            access = Access.Secure,
            keys = listOf(Table.Secure to KEY_EXTRA_DIM_LEVEL),
            reader = { d -> (d.int(Table.Secure, KEY_EXTRA_DIM_LEVEL) ?: EXTRA_DIM_LEVEL_DEFAULT) / 100f },
            writer = { d, p -> d.put(Table.Secure, KEY_EXTRA_DIM_LEVEL, (p * 100).roundToInt()) },
        )

    private const val KEY_DALTONIZER_ON = "accessibility_display_daltonizer_enabled"
    private const val KEY_DALTONIZER = "accessibility_display_daltonizer"
    private const val DALTONIZER_GRAYSCALE = 0

    /** Android's default colour correction mode (deuteranomaly), put back when grayscale ends. */
    private const val DALTONIZER_DEFAULT = 12

    /**
     * Grayscale is colour correction in its "monochromacy" mode (0), as Bedtime mode uses it.
     * Switching it off puts the correction mode back to Android's default, so colour correction
     * isn't left on grayscale.
     */
    val Grayscale =
        Toggle(
            id = "grayscale",
            access = Access.Secure,
            keys = listOf(Table.Secure to KEY_DALTONIZER_ON, Table.Secure to KEY_DALTONIZER),
            defaultOn = false,
            reader = { d ->
                d.bool(Table.Secure, KEY_DALTONIZER_ON, missing = false) &&
                    d.int(Table.Secure, KEY_DALTONIZER) == DALTONIZER_GRAYSCALE
            },
            writer = { d, on ->
                if (on) {
                    d.put(Table.Secure, KEY_DALTONIZER, DALTONIZER_GRAYSCALE)
                    d.put(Table.Secure, KEY_DALTONIZER_ON, true)
                } else {
                    d.put(Table.Secure, KEY_DALTONIZER_ON, false)
                    if (d.int(Table.Secure, KEY_DALTONIZER) == DALTONIZER_GRAYSCALE) {
                        d.put(Table.Secure, KEY_DALTONIZER, DALTONIZER_DEFAULT)
                    }
                }
            },
        )

    val InvertColors =
        secureToggle("invert_colors", Table.Secure, "accessibility_display_inversion_enabled", defaultOn = false, missing = false)

    // ---- Sound -----------------------------------------------------------------------------

    const val RINGER_SOUND = "sound"
    const val RINGER_VIBRATE = "vibrate"
    const val RINGER_SILENT = "silent"

    /**
     * Sound or vibrate. Silent isn't offered: Android ties it to Do Not Disturb, which needs a
     * permission the app doesn't ask for. Android mirrors the mode in Global "mode_ringer".
     */
    val Ringer =
        Choice(
            id = "ringer",
            access = Access.None,
            keys = listOf(Table.Global to "mode_ringer"),
            options = listOf(RINGER_SOUND, RINGER_VIBRATE),
            appDefault = RINGER_SOUND,
            reader = { d ->
                when (d.audio.ringer()) {
                    RingerMode.Normal -> RINGER_SOUND
                    RingerMode.Vibrate -> RINGER_VIBRATE
                    RingerMode.Silent -> RINGER_SILENT
                }
            },
            writer = { d, v ->
                d.audio.setRinger(
                    if (v == RINGER_VIBRATE) RingerMode.Vibrate else RingerMode.Normal,
                )
            },
        )

    val MediaVolume = volume("volume_media", Stream.Media)
    val RingVolume = volume("volume_ring", Stream.Ring)
    val NotificationVolume = volume("volume_notification", Stream.Notification)
    val AlarmVolume = volume("volume_alarm", Stream.Alarm)

    /** "Touch feedback" (vibration on taps). A public System setting. */
    val TouchVibration =
        Toggle(
            id = "touch_vibration",
            access = Access.System,
            keys = listOf(Table.System to "haptic_feedback_enabled"),
            defaultOn = true,
            reader = { d -> d.bool(Table.System, "haptic_feedback_enabled", missing = true) },
            writer = { d, on -> d.put(Table.System, "haptic_feedback_enabled", on) },
        )

    /** Notifications that pop up over the screen (heads-up). */
    val HeadsUp = secureToggle("heads_up", Table.Global, "heads_up_notifications_enabled", defaultOn = true, missing = true)

    // ---- Developer -------------------------------------------------------------------------

    val UsbDebugging = secureToggle("usb_debugging", Table.Global, "adb_enabled", defaultOn = false, missing = false)

    val WirelessDebugging = secureToggle("wireless_debugging", Table.Global, "adb_wifi_enabled", defaultOn = false, missing = false)

    const val ANIMATIONS_OFF = "0"
    const val ANIMATIONS_HALF = "0.5"
    const val ANIMATIONS_NORMAL = "1"
    private val ANIMATION_KEYS = listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")

    /** The three animation scales of Developer options, set together. Read from the first. */
    val Animations =
        Choice(
            id = "animations",
            access = Access.Secure,
            keys = ANIMATION_KEYS.map { Table.Global to it },
            options = listOf(ANIMATIONS_OFF, ANIMATIONS_HALF, ANIMATIONS_NORMAL),
            appDefault = ANIMATIONS_NORMAL,
            reader = { d -> formatScale(d.float(Table.Global, ANIMATION_KEYS.first()) ?: 1f) },
            writer = { d, v -> ANIMATION_KEYS.forEach { d.put(Table.Global, it, v) } },
        )

    /** Android stores which chargers keep the screen on as bits: AC 1, USB 2, wireless 4. */
    private const val STAY_AWAKE_ALL_CHARGERS = 7

    val StayAwake =
        Toggle(
            id = "stay_awake",
            access = Access.Secure,
            keys = listOf(Table.Global to "stay_on_while_plugged_in"),
            defaultOn = false,
            reader = { d -> (d.int(Table.Global, "stay_on_while_plugged_in") ?: 0) != 0 },
            writer = { d, on -> d.put(Table.Global, "stay_on_while_plugged_in", if (on) STAY_AWAKE_ALL_CHARGERS else 0) },
        )

    // ---- Network ---------------------------------------------------------------------------

    val PrivateDns = PrivateDnsSetting

    /** Every setting, for watching changes. */
    val all: List<Setting> by lazy { Group.entries.flatMap { g -> g.rows.flatMap { listOfNotNull(it.setting, it.level) } } }

    /**
     * The settings whose default the first start takes from the phone, so an update doesn't light
     * up tiles for how the owner already had things. The four screen settings and Private DNS
     * keep the defaults earlier versions had.
     */
    val seeded: List<Tracked> by lazy {
        all.filterIsInstance<Tracked>() - setOf(BrightnessMode, Timeout, AlwaysOn, TapToWake, PrivateDns)
    }

    /** The tracked setting with this [id], if any. */
    fun tracked(id: String): Tracked? = all.filterIsInstance<Tracked>().firstOrNull { it.id == id }

    /** 1 → "1", 0.5 → "0.5", 1.5 → "1.5". */
    fun formatScale(scale: Float): String {
        val rounded = (scale * 100).roundToInt() / 100f
        return if (rounded == rounded.toInt().toFloat()) rounded.toInt().toString() else rounded.toString()
    }

    private fun secureToggle(
        id: String,
        table: Table,
        key: String,
        defaultOn: Boolean,
        missing: Boolean,
    ) = Toggle(
        id = id,
        access = Access.Secure,
        keys = listOf(table to key),
        defaultOn = defaultOn,
        reader = { d -> d.bool(table, key, missing) },
        writer = { d, on -> d.put(table, key, on) },
    )

    private fun volume(
        id: String,
        stream: Stream,
    ) = Level(
        id = id,
        access = Access.None,
        keys = emptyList(),
        reader = { d -> d.audio.volume(stream) },
        writer = { d, p -> d.audio.setVolume(stream, p) },
    )
}
