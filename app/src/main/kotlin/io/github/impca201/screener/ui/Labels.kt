package io.github.impca201.screener.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import io.github.impca201.screener.R
import io.github.impca201.screener.screen.ScreenState
import io.github.impca201.screener.screen.TimeoutDuration

/** Short name of a changed setting, for the tile. */
@StringRes
fun changeLabel(change: ScreenState.Change): Int =
    when (change) {
        ScreenState.Change.Brightness -> R.string.change_brightness
        ScreenState.Change.Timeout -> R.string.change_timeout
        ScreenState.Change.AlwaysOn -> R.string.change_always_on
        ScreenState.Change.TapToWake -> R.string.change_tap_to_wake
    }

/** "30 minutes", "1 minute", "15 seconds". */
fun timeoutText(
    resources: Resources,
    timeoutMs: Int,
): String {
    val duration = TimeoutDuration.of(timeoutMs)
    val plural =
        when (duration.unit) {
            TimeoutDuration.Unit.Seconds -> R.plurals.duration_seconds
            TimeoutDuration.Unit.Minutes -> R.plurals.duration_minutes
            TimeoutDuration.Unit.Hours -> R.plurals.duration_hours
        }
    return resources.getQuantityString(plural, duration.amount, duration.amount)
}
