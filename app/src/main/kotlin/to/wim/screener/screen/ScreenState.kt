package to.wim.screener.screen

/**
 * The four screen settings Screener manages, as read from the system.
 *
 * Plain Kotlin with no Android types, so it is tested on the JVM (see ScreenStateTest).
 */
data class ScreenState(
    val autoBrightness: Boolean,
    /** System brightness, [BrightnessCurve.MIN]..[BrightnessCurve.MAX]. */
    val brightness: Int,
    val timeoutMs: Int,
    val alwaysOn: Boolean,
    val tapToWake: Boolean,
) {
    /** True while the screen timeout is anything other than the default minute. */
    val timeoutExtended: Boolean get() = timeoutMs != DEFAULT_TIMEOUT_MS

    /** The settings that differ from Screener's defaults, in the order they are shown. */
    val changes: List<Change>
        get() =
            buildList {
                if (autoBrightness != DEFAULTS.autoBrightness) add(Change.Brightness)
                if (timeoutExtended) add(Change.Timeout)
                if (alwaysOn != DEFAULTS.alwaysOn) add(Change.AlwaysOn)
                if (tapToWake != DEFAULTS.tapToWake) add(Change.TapToWake)
            }

    enum class Change { Brightness, Timeout, AlwaysOn, TapToWake }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 60_000
        const val EXTENDED_TIMEOUT_MS = 30 * 60_000

        /** Auto brightness, one minute, always-on display off, tap to wake on. */
        val DEFAULTS =
            ScreenState(
                autoBrightness = true,
                brightness = BrightnessCurve.MAX / 2,
                timeoutMs = DEFAULT_TIMEOUT_MS,
                alwaysOn = false,
                tapToWake = true,
            )
    }
}

/** A screen timeout split into the unit it is shown in. */
data class TimeoutDuration(val amount: Int, val unit: Unit) {
    enum class Unit { Seconds, Minutes, Hours }

    companion object {
        fun of(timeoutMs: Int): TimeoutDuration {
            val seconds = (timeoutMs.coerceAtLeast(0) / 1000)
            return when {
                seconds >= 3600 && seconds % 3600 == 0 -> TimeoutDuration(seconds / 3600, Unit.Hours)
                seconds >= 60 -> TimeoutDuration(seconds / 60, Unit.Minutes)
                else -> TimeoutDuration(seconds, Unit.Seconds)
            }
        }
    }
}
