package to.wim.quicksettings.dns

import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.util.Log
import to.wim.quicksettings.screen.SystemScreenSettings

/**
 * [DnsSettings] backed by Settings.Global. Writing needs WRITE_SECURE_SETTINGS (adb), the same
 * permission as always-on display and tap to wake. Writes without it are skipped.
 */
class SystemDnsSettings(private val context: Context) : DnsSettings {
    override fun read(): DnsState =
        DnsState(
            mode = DnsMode.from(Settings.Global.getString(context.contentResolver, KEY_MODE)),
            hostname = Settings.Global.getString(context.contentResolver, KEY_HOSTNAME),
        )

    override fun setMode(mode: DnsMode) {
        if (!SystemScreenSettings.canWriteSecure(context)) return
        runCatching { Settings.Global.putString(context.contentResolver, KEY_MODE, mode.value) }
            .onFailure { Log.w("QuickSettings", "Could not write $KEY_MODE", it) }
    }

    companion object {
        const val KEY_MODE = "private_dns_mode"
        const val KEY_HOSTNAME = "private_dns_specifier"

        val watchedUris: List<Uri>
            get() = listOf(Settings.Global.getUriFor(KEY_MODE), Settings.Global.getUriFor(KEY_HOSTNAME))
    }
}
