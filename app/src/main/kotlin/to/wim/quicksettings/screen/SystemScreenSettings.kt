package to.wim.quicksettings.screen

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log

/**
 * [ScreenSettings] backed by Android's system settings.
 *
 * Brightness and timeout are in Settings.System and need "Modify system settings" (granted in
 * Settings). Always-on display and tap to wake are in Settings.Secure and need
 * WRITE_SECURE_SETTINGS, which only adb can grant. Writes without permission are skipped.
 */
class SystemScreenSettings(private val context: Context) : ScreenSettings {
    private val resolver: ContentResolver get() = context.contentResolver

    override fun read(): ScreenState {
        val d = ScreenState.DEFAULTS
        return ScreenState(
            autoBrightness =
                Settings.System.getInt(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
                ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
            brightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, d.brightness),
            timeoutMs = Settings.System.getInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT, d.timeoutMs),
            alwaysOn = Settings.Secure.getInt(resolver, KEY_ALWAYS_ON, if (d.alwaysOn) 1 else 0) != 0,
            tapToWake = Settings.Secure.getInt(resolver, KEY_TAP_GESTURE, if (d.tapToWake) 1 else 0) != 0,
        )
    }

    override fun setAutoBrightness(auto: Boolean) {
        val mode =
            if (auto) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        putSystem(Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
    }

    override fun setBrightness(level: Int) = putSystem(Settings.System.SCREEN_BRIGHTNESS, level)

    override fun setTimeout(timeoutMs: Int) = putSystem(Settings.System.SCREEN_OFF_TIMEOUT, timeoutMs)

    override fun setAlwaysOn(on: Boolean) = putSecure(KEY_ALWAYS_ON, on)

    override fun setTapToWake(on: Boolean) {
        // Pixels use "Tap to check phone" (doze_tap_gesture); AOSP-based phones "Tap to wake"
        // (double_tap_to_wake). Keep both in step; a phone ignores the one it doesn't use.
        putSecure(KEY_TAP_GESTURE, on)
        putSecure(KEY_DOUBLE_TAP_TO_WAKE, on)
    }

    private fun putSystem(
        key: String,
        value: Int,
    ) {
        if (!canWriteSystem(context)) return
        runCatching { Settings.System.putInt(resolver, key, value) }
            .onFailure { Log.w(TAG, "Could not write $key", it) }
    }

    private fun putSecure(
        key: String,
        on: Boolean,
    ) {
        if (!canWriteSecure(context)) return
        runCatching { Settings.Secure.putInt(resolver, key, if (on) 1 else 0) }
            .onFailure { Log.w(TAG, "Could not write $key", it) }
    }

    companion object {
        private const val TAG = "QuickSettings"
        const val KEY_ALWAYS_ON = "doze_always_on"
        const val KEY_TAP_GESTURE = "doze_tap_gesture"
        const val KEY_DOUBLE_TAP_TO_WAKE = "double_tap_to_wake"

        /** Every setting the app shows, to watch for changes made elsewhere. */
        val watchedUris: List<Uri>
            get() =
                listOf(
                    Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
                    Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                    Settings.System.getUriFor(Settings.System.SCREEN_OFF_TIMEOUT),
                    Settings.Secure.getUriFor(KEY_ALWAYS_ON),
                    Settings.Secure.getUriFor(KEY_TAP_GESTURE),
                )

        fun canWriteSystem(context: Context): Boolean = Settings.System.canWrite(context)

        fun canWriteSecure(context: Context): Boolean =
            context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }
}
