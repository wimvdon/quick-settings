package to.wim.quicksettings.shortcut

/**
 * The four long-press shortcuts: their stable ids (Quick Tap remembers a shortcut by id) and the
 * action [ShortcutActivity] handles. Plain Kotlin, tested on the JVM (see AppShortcutTest).
 */
enum class AppShortcut(val id: String, val action: String) {
    PrivateDns("toggle_private_dns", ShortcutActivity.ACTION_DNS),
    Timeout("toggle_timeout", ShortcutActivity.ACTION_TIMEOUT),
    AlwaysOn("toggle_always_on", ShortcutActivity.ACTION_ALWAYS_ON),
    TapToWake("toggle_tap_to_wake", ShortcutActivity.ACTION_TAP_TO_WAKE),
    ;

    companion object {
        /** True when the published shortcuts are exactly these, in this order. */
        fun matches(publishedIds: List<String>): Boolean = publishedIds == entries.map { it.id }
    }
}
