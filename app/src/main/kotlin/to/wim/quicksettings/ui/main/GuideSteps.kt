package to.wim.quicksettings.ui.main

/**
 * Turns a translated block of steps (one step per line) into the separate steps shown with
 * number circles. Leading "1." style numbers are dropped, so the screen numbers them itself.
 *
 * Plain Kotlin, tested on the JVM (see GuideStepsTest).
 */
object GuideSteps {
    private val leadingNumber = Regex("""^\d+[.)]\s*""")

    fun split(text: String): List<String> =
        text
            .lines()
            .map { it.trim().replace(leadingNumber, "") }
            .filter { it.isNotEmpty() }
}
