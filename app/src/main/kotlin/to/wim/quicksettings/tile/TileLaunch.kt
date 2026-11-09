package to.wim.quicksettings.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService

/** Opens [intent] from a tile and closes the shade. */
// The Intent overload is only reached below Android 14, where it is still supported.
@SuppressLint("StartActivityAndCollapseDeprecated")
fun TileService.openAndCollapse(intent: Intent) {
    val launch = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startActivityAndCollapse(
            PendingIntent.getActivity(
                this,
                launch.component?.className.hashCode(),
                launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
    } else {
        @Suppress("DEPRECATION")
        startActivityAndCollapse(launch)
    }
}
