package to.wim.quicksettings.ui

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import to.wim.quicksettings.R
import to.wim.quicksettings.settings.Catalog
import to.wim.quicksettings.settings.Category
import to.wim.quicksettings.settings.Choice
import to.wim.quicksettings.settings.Group
import to.wim.quicksettings.settings.PrivateDnsSetting
import to.wim.quicksettings.settings.Setting
import to.wim.quicksettings.settings.TileId
import to.wim.quicksettings.settings.TileKind
import to.wim.quicksettings.settings.TimeoutDuration
import to.wim.quicksettings.settings.Toggle
import to.wim.quicksettings.settings.Tracked

/** The name of a setting, as in Android's own Settings. */
@StringRes
fun titleOf(setting: Setting): Int =
    when (setting) {
        Catalog.BrightnessMode, Catalog.Brightness -> R.string.brightness_title
        Catalog.Timeout -> R.string.timeout_title
        Catalog.AlwaysOn -> R.string.always_on_title
        Catalog.TapToWake -> R.string.tap_to_wake_title
        Catalog.Rotation -> R.string.rotation_title
        Catalog.NightLight -> R.string.night_light_title
        Catalog.NightLightWarmth -> R.string.night_light_warmth
        Catalog.ExtraDim -> R.string.extra_dim_title
        Catalog.ExtraDimLevel -> R.string.extra_dim_level
        Catalog.Grayscale -> R.string.grayscale_title
        Catalog.InvertColors -> R.string.invert_title
        Catalog.Ringer -> R.string.ringer_title
        Catalog.MediaVolume -> R.string.volume_media
        Catalog.RingVolume -> R.string.volume_ring
        Catalog.NotificationVolume -> R.string.volume_notification
        Catalog.AlarmVolume -> R.string.volume_alarm
        Catalog.TouchVibration -> R.string.touch_vibration_title
        Catalog.HeadsUp -> R.string.heads_up_title
        Catalog.UsbDebugging -> R.string.usb_debugging_title
        Catalog.WirelessDebugging -> R.string.wireless_debugging_title
        Catalog.Animations -> R.string.animations_title
        Catalog.StayAwake -> R.string.stay_awake_title
        Catalog.PrivateDns -> R.string.tile_dns_label
        else -> R.string.app_name
    }

@DrawableRes
fun iconOf(setting: Setting): Int =
    when (setting) {
        Catalog.BrightnessMode, Catalog.Brightness -> R.drawable.ic_brightness
        Catalog.Timeout -> R.drawable.ic_timer
        Catalog.AlwaysOn -> R.drawable.ic_always_on
        Catalog.TapToWake -> R.drawable.ic_tap
        Catalog.Rotation -> R.drawable.ic_rotation
        Catalog.NightLight, Catalog.NightLightWarmth -> R.drawable.ic_night_light
        Catalog.ExtraDim, Catalog.ExtraDimLevel -> R.drawable.ic_extra_dim
        Catalog.Grayscale -> R.drawable.ic_grayscale
        Catalog.InvertColors -> R.drawable.ic_invert
        Catalog.Ringer, Catalog.RingVolume -> R.drawable.ic_ring
        Catalog.MediaVolume -> R.drawable.ic_volume
        Catalog.NotificationVolume -> R.drawable.ic_notifications
        Catalog.AlarmVolume -> R.drawable.ic_alarm
        Catalog.TouchVibration -> R.drawable.ic_vibration
        Catalog.HeadsUp -> R.drawable.ic_heads_up
        Catalog.UsbDebugging -> R.drawable.ic_usb
        Catalog.WirelessDebugging -> R.drawable.ic_wifi
        Catalog.Animations -> R.drawable.ic_animation
        Catalog.StayAwake -> R.drawable.ic_stay_awake
        Catalog.PrivateDns -> R.drawable.ic_dns
        else -> R.drawable.ic_tile_display
    }

@StringRes
fun titleOf(group: Group): Int =
    when (group) {
        Group.Screen -> R.string.tile_screen_label
        Group.Colour -> R.string.group_colour
        Group.Sound -> R.string.group_sound
        Group.Developer -> R.string.group_developer
        Group.Network -> R.string.group_network
    }

@DrawableRes
fun iconOf(group: Group): Int =
    when (group) {
        Group.Screen -> R.drawable.ic_tile_display
        Group.Colour -> R.drawable.ic_palette
        Group.Sound -> R.drawable.ic_sound
        Group.Developer -> R.drawable.ic_code
        Group.Network -> R.drawable.ic_network
    }

@StringRes
fun titleOf(category: Category): Int =
    when (category) {
        Category.Display -> R.string.category_display
        Category.Colour -> R.string.group_colour
        Category.Sound -> R.string.group_sound
        Category.Network -> R.string.group_network
        Category.Developer -> R.string.group_developer
        Category.Other -> R.string.category_other
    }

@StringRes
fun labelOf(tile: TileId): Int =
    when (val kind = tile.kind) {
        is TileKind.Popup -> titleOf(kind.group)
        is TileKind.Switch -> titleOf(kind.toggle)
        is TileKind.Cycle -> titleOf(kind.choice)
        TileKind.Timeout -> R.string.timeout_title
        TileKind.Dns -> R.string.tile_dns_label
        TileKind.Keyboard -> R.string.tile_keyboard
        TileKind.Volume -> R.string.tile_volume
    }

@DrawableRes
fun iconOf(tile: TileId): Int =
    when (val kind = tile.kind) {
        is TileKind.Popup -> iconOf(kind.group)
        is TileKind.Switch -> iconOf(kind.toggle)
        is TileKind.Cycle -> iconOf(kind.choice)
        TileKind.Timeout -> R.drawable.ic_timer
        TileKind.Dns -> R.drawable.ic_dns
        TileKind.Keyboard -> R.drawable.ic_keyboard
        TileKind.Volume -> R.drawable.ic_volume
    }

/** A setting's value in words: "On", "Auto", "30 minutes", "dns.example.com". */
fun valueText(
    resources: Resources,
    setting: Tracked,
    value: String?,
    short: Boolean = false,
): String =
    when (setting) {
        is Toggle -> resources.getString(if (value == Toggle.ON) R.string.value_on else R.string.value_off)
        PrivateDnsSetting ->
            when (value) {
                PrivateDnsSetting.OFF -> resources.getString(R.string.dns_off)
                PrivateDnsSetting.AUTOMATIC -> resources.getString(R.string.dns_automatic)
                PrivateDnsSetting.ANY_HOSTNAME -> resources.getString(R.string.dns_hostname)
                else -> PrivateDnsSetting.hostnameOf(value)?.ifBlank { null } ?: resources.getString(R.string.dns_hostname)
            }
        is Choice -> choiceText(resources, setting, value, short)
        else -> value.orEmpty()
    }

private fun choiceText(
    resources: Resources,
    choice: Choice,
    value: String?,
    short: Boolean,
): String =
    when (choice) {
        Catalog.BrightnessMode ->
            resources.getString(if (value == Catalog.BRIGHTNESS_AUTO) R.string.brightness_auto else R.string.brightness_manual_short)
        Catalog.Timeout -> {
            val ms = value?.toIntOrNull() ?: 0
            if (short && ms % 60_000 == 0) {
                resources.getString(R.string.minutes_short, ms / 60_000)
            } else {
                timeoutText(resources, ms)
            }
        }
        Catalog.Rotation ->
            resources.getString(
                when (value) {
                    Catalog.ROTATION_AUTO -> R.string.rotation_auto
                    Catalog.ROTATION_PORTRAIT -> R.string.rotation_portrait
                    Catalog.ROTATION_LANDSCAPE -> R.string.rotation_landscape
                    "upside_down" -> R.string.rotation_upside_down
                    else -> R.string.rotation_landscape_reversed
                },
            )
        Catalog.Ringer ->
            resources.getString(
                when (value) {
                    Catalog.RINGER_VIBRATE -> R.string.ringer_vibrate
                    Catalog.RINGER_SILENT -> R.string.ringer_silent
                    else -> R.string.ringer_sound
                },
            )
        Catalog.Animations ->
            if (value == Catalog.ANIMATIONS_OFF) resources.getString(R.string.value_off) else resources.getString(R.string.scale, value)
        else -> value.orEmpty()
    }

/** "Always-on display: On", for notifications and screen readers. */
fun changeText(
    resources: Resources,
    setting: Tracked,
    value: String?,
): String = resources.getString(R.string.change_format, resources.getString(titleOf(setting)), valueText(resources, setting, value))

/** "30 minutes", "1 minute", "15 seconds". */
fun timeoutText(
    resources: Resources,
    timeoutMs: Int,
): String {
    val duration = TimeoutDuration.of(timeoutMs)
    val plural =
        when (duration.unit) {
            TimeoutDuration.Unit.Seconds -> R.plurals.duration_seconds
            TimeoutDuration.Unit.Minutes -> R.plurals.duration_minutes
            TimeoutDuration.Unit.Hours -> R.plurals.duration_hours
        }
    return resources.getQuantityString(plural, duration.amount, duration.amount)
}
