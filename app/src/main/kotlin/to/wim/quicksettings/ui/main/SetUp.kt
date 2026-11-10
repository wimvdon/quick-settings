package to.wim.quicksettings.ui.main

import to.wim.quicksettings.settings.Catalog
import to.wim.quicksettings.settings.Group
import to.wim.quicksettings.settings.Tracked

/** What a tune button on the main screen opens: a popup's rows, or one setting's defaults. */
sealed interface SetUp {
    data class Popup(
        val group: Group,
    ) : SetUp

    data class Setting(
        val setting: Tracked,
    ) : SetUp

    /** As text, so the open sheet survives rotation and can be asked for in an intent. */
    val key: String
        get() =
            when (this) {
                is Popup -> POPUP + group.name
                is Setting -> SETTING + setting.id
            }

    companion object {
        private const val POPUP = "popup:"
        private const val SETTING = "setting:"

        fun parse(key: String?): SetUp? =
            when {
                key == null -> null
                key.startsWith(POPUP) -> Group.entries.firstOrNull { it.name == key.removePrefix(POPUP) }?.let(::Popup)
                key.startsWith(SETTING) -> Catalog.tracked(key.removePrefix(SETTING))?.let(::Setting)
                else -> null
            }
    }
}
