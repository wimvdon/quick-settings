package to.wim.screener.tile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

/**
 * Long-pressing the tile opens this (QS_TILE_PREFERENCES). It shows nothing: it opens Android's
 * Display settings and closes.
 */
class DisplaySettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
        finish()
    }
}
