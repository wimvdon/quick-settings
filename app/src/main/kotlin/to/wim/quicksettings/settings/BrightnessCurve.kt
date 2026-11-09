package to.wim.quicksettings.settings

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Converts between the slider position (0..1) and the system brightness (1..255) with the same
 * perceptual curve (HLG gamma) as Android's own brightness slider, so 50% here looks like 50%
 * in Quick Settings.
 */
object BrightnessCurve {
    const val MIN = 1
    const val MAX = 255

    private const val R = 0.5
    private const val A = 0.17883277
    private const val B = 0.28466892
    private const val C = 0.55991073

    /** Slider position 0..1 → system brightness [MIN]..[MAX]. */
    fun toBrightness(position: Float): Int {
        val p = position.coerceIn(0f, 1f).toDouble()
        val linear = if (p <= R) (p / R) * (p / R) else exp((p - C) / A) + B
        return (MIN + (MAX - MIN) * (linear / 12.0)).roundToInt().coerceIn(MIN, MAX)
    }

    /** System brightness → slider position 0..1. */
    fun toPosition(brightness: Int): Float {
        val normalized = (brightness.coerceIn(MIN, MAX) - MIN).toDouble() / (MAX - MIN) * 12.0
        val gamma = if (normalized <= 1.0) sqrt(normalized) * R else A * ln(normalized - B) + C
        return gamma.toFloat().coerceIn(0f, 1f)
    }

    /** System brightness as the percentage the system slider would show. */
    fun toPercent(brightness: Int): Int = (toPosition(brightness) * 100).roundToInt()
}
