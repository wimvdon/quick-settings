package to.wim.quicksettings.ui.panel

import android.content.Intent
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import to.wim.quicksettings.R
import to.wim.quicksettings.notify.Notifications
import to.wim.quicksettings.tile.ScreenTileService
import to.wim.quicksettings.tile.openAndCollapse
import to.wim.quicksettings.ui.main.MainActivity
import to.wim.quicksettings.ui.theme.AppTheme

/**
 * Shows the popup as a dialog on top of the open Quick Settings shade, the way Android's own
 * Internet dialog does, instead of closing the shade and starting a separate screen.
 * [ComponentDialog] gives Compose the lifecycle it needs inside a service.
 */
fun ScreenTileService.showPanelDialog() {
    val tile = this
    val dialog = ComponentDialog(this, R.style.Theme_QuickSettings_Panel)
    val model =
        PanelModel(
            this,
            object : PanelModel.Host {
                override fun open(intent: Intent) {
                    dialog.dismiss()
                    tile.openAndCollapse(intent)
                }

                override fun close() = dialog.dismiss()

                // A service can't ask for permissions; the main screen can, so send the user
                // there only when notifications are actually off.
                override fun notificationsUseful() {
                    if (!Notifications.canPost(tile)) open(MainActivity.intent(tile))
                }
            },
        )
    dialog.setContentView(
        ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent { AppTheme { PanelScreen(state = model.state.value, actions = model.actions) } }
        },
    )
    dialog.window?.apply {
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setGravity(Gravity.CENTER)
    }
    dialog.setOnShowListener { model.start() }
    dialog.setOnDismissListener { model.stop() }
    showDialog(dialog)
}
