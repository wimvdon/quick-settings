package to.wim.quicksettings.tile

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

/**
 * Long-pressing a tile opens this (QS_TILE_PREFERENCES). It shows nothing: it opens the matching
 * Android settings page and closes. Screen → Display, Private DNS → Network & internet (where
 * Android keeps Private DNS).
 */
class TilePreferencesActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION") // The typed overload needs API 33; minSdk is 31.
        val tile = intent.getParcelableExtra<ComponentName>(Intent.EXTRA_COMPONENT_NAME)
        val action =
            if (tile?.className == DnsTileService::class.java.name) {
                Settings.ACTION_WIRELESS_SETTINGS
            } else {
                Settings.ACTION_DISPLAY_SETTINGS
            }
        runCatching { startActivity(Intent(action)) }
        finish()
    }
}
