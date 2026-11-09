package io.github.impca201.screener.screen

/** Reads and writes the system settings. The Android version is [SystemScreenSettings]. */
interface ScreenSettings {
    fun read(): ScreenState

    fun setAutoBrightness(auto: Boolean)

    fun setBrightness(level: Int)

    fun setTimeout(timeoutMs: Int)

    fun setAlwaysOn(on: Boolean)

    fun setTapToWake(on: Boolean)
}

/**
 * Every change Screener makes goes through here, so the rules live in one tested place.
 */
class ScreenController(private val settings: ScreenSettings) {
    fun read(): ScreenState = settings.read()

    /**
     * Applies a manual brightness. Auto brightness is switched off first: while it is on, the
     * system ignores (or overrides) a written brightness.
     */
    fun setBrightness(level: Int) {
        if (settings.read().autoBrightness) settings.setAutoBrightness(false)
        settings.setBrightness(level.coerceIn(BrightnessCurve.MIN, BrightnessCurve.MAX))
    }

    fun setBrightnessPosition(position: Float) = setBrightness(BrightnessCurve.toBrightness(position))

    fun resetBrightness() = settings.setAutoBrightness(true)

    fun setTimeoutExtended(extended: Boolean) =
        settings.setTimeout(if (extended) ScreenState.EXTENDED_TIMEOUT_MS else ScreenState.DEFAULT_TIMEOUT_MS)

    fun setAlwaysOn(on: Boolean) = settings.setAlwaysOn(on)

    fun setTapToWake(on: Boolean) = settings.setTapToWake(on)

    /** Puts one setting back to its default. */
    fun reset(change: ScreenState.Change) =
        when (change) {
            ScreenState.Change.Brightness -> resetBrightness()
            ScreenState.Change.Timeout -> setTimeoutExtended(false)
            ScreenState.Change.AlwaysOn -> setAlwaysOn(ScreenState.DEFAULTS.alwaysOn)
            ScreenState.Change.TapToWake -> setTapToWake(ScreenState.DEFAULTS.tapToWake)
        }
}
