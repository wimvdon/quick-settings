package to.wim.quicksettings.ui.panel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import to.wim.quicksettings.quickSettingsApp
import to.wim.quicksettings.ui.theme.AppTheme

/**
 * The popup, shaped like Android's Quick Settings Internet dialog, in its own floating window.
 * Opened from the Screen tile and the notifications. Tapping outside it closes it.
 */
class PanelActivity :
    ComponentActivity(),
    PanelModel.Host {
    private lateinit var model: PanelModel
    private var askedForNotifications = false

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { quickSettingsApp.sync() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = PanelModel(this, this)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
        setContent {
            AppTheme { PanelScreen(state = model.state.value, actions = model.actions) }
        }
    }

    override fun onResume() {
        super.onResume()
        // Permissions may have been granted in Settings or with adb meanwhile.
        model.start()
    }

    override fun onPause() {
        model.stop()
        super.onPause()
    }

    override fun open(intent: Intent) {
        runCatching { startActivity(intent) }
    }

    override fun close() = finish()

    /** The notifications need permission on Android 13+; ask the first time they're useful. */
    override fun notificationsUseful() {
        if (askedForNotifications || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        askedForNotifications = true
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, PanelActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
