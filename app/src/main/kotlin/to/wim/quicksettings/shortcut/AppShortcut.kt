package to.wim.quicksettings.shortcut

/**
 * The four long-press shortcuts: their ids (Quick Tap remembers a shortcut by id) and the action
 * [ShortcutActivity] handles. The ids differ from the old manifest shortcuts on purpose: Android
 * keeps a pinned manifest shortcut locked under its id, and publishing that id again would fail. Plain Kotlin, tested on the JVM (see AppShortcutTest).
 */
enum class AppShortcut(val id: String, val action: String) {
    PrivateDns("private_dns", ShortcutActivity.ACTION_DNS),
    Timeout("screen_timeout", ShortcutActivity.ACTION_TIMEOUT),
    AlwaysOn("always_on_display", ShortcutActivity.ACTION_ALWAYS_ON),
    TapToWake("tap_to_wake", ShortcutActivity.ACTION_TAP_TO_WAKE),
    ;

    companion object {
        /** True when the published shortcuts are exactly these, in this order. */
        fun matches(publishedIds: List<String>): Boolean = publishedIds == entries.map { it.id }
    }
}
