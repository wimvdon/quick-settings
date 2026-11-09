package to.wim.quicksettings.settings

/** A screen timeout split into the unit it is shown in. */
data class TimeoutDuration(val amount: Int, val unit: Unit) {
    enum class Unit { Seconds, Minutes, Hours }

    companion object {
        fun of(timeoutMs: Int): TimeoutDuration {
            val seconds = timeoutMs.coerceAtLeast(0) / 1000
            return when {
                seconds >= 3600 && seconds % 3600 == 0 -> TimeoutDuration(seconds / 3600, Unit.Hours)
                seconds >= 60 -> TimeoutDuration(seconds / 60, Unit.Minutes)
                else -> TimeoutDuration(seconds, Unit.Seconds)
            }
        }
    }
}
