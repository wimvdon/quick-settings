package to.wim.quicksettings.ui.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import to.wim.quicksettings.R
import to.wim.quicksettings.dns.DnsMode
import to.wim.quicksettings.dns.DnsState
import to.wim.quicksettings.ui.theme.AppShapes
import to.wim.quicksettings.ui.theme.AppTheme

data class MainUiState(
    /** Android 13+ can add a tile with one tap; older versions only by editing Quick Settings. */
    val canAddTilesHere: Boolean,
    val canWriteSystem: Boolean,
    val canWriteSecure: Boolean,
    val canPostNotifications: Boolean,
    val dns: DnsState,
    val adbCommand: String,
)

class MainActions(
    val onAddScreenTile: () -> Unit = {},
    val onAddDnsTile: () -> Unit = {},
    val onAllowSystemSettings: () -> Unit = {},
    val onCopyCommand: () -> Unit = {},
    val onAllowNotifications: () -> Unit = {},
    val onOpenNetworkSettings: () -> Unit = {},
    val onOpenScreenPanel: () -> Unit = {},
)

/** How to add the tiles, the permissions they need (with the adb steps) and the DNS hostname. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    actions: MainActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 640.dp)
                .padding(bottom = 24.dp),
        ) {
            Section(stringResource(R.string.main_section_tiles))
            TileRow(
                icon = R.drawable.ic_tile_display,
                title = stringResource(R.string.tile_screen_label),
                text = stringResource(R.string.main_tile_screen_text),
                showAdd = state.canAddTilesHere,
                onAdd = actions.onAddScreenTile,
            )
            TileRow(
                icon = R.drawable.ic_dns,
                title = stringResource(R.string.tile_dns_label),
                text = stringResource(R.string.main_tile_dns_text),
                showAdd = state.canAddTilesHere,
                onAdd = actions.onAddDnsTile,
            )
            Body(stringResource(R.string.main_tiles_manual))
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedButton(onClick = actions.onOpenScreenPanel) {
                    Text(stringResource(R.string.main_open_screen))
                }
            }

            Section(stringResource(R.string.main_section_permissions))
            PermissionRow(
                title = stringResource(R.string.main_permission_system),
                text = stringResource(R.string.main_permission_system_text),
                granted = state.canWriteSystem,
                onAllow = actions.onAllowSystemSettings,
            )
            PermissionRow(
                title = stringResource(R.string.main_permission_notifications),
                text = stringResource(R.string.main_permission_notifications_text),
                granted = state.canPostNotifications,
                onAllow = actions.onAllowNotifications,
            )
            PermissionRow(
                title = stringResource(R.string.main_permission_secure),
                text = stringResource(R.string.main_permission_secure_text),
                granted = state.canWriteSecure,
                onAllow = null,
            )
            if (!state.canWriteSecure) AdbSteps(state.adbCommand, actions.onCopyCommand)

            Section(stringResource(R.string.tile_dns_label))
            ListItem(
                headlineContent = {
                    Text(
                        if (state.dns.hasHostname) {
                            state.dns.hostname.orEmpty()
                        } else {
                            stringResource(R.string.main_dns_no_hostname)
                        },
                    )
                },
                supportingContent = {
                    Text(
                        if (state.dns.hasHostname) {
                            stringResource(R.string.main_dns_current, stringResource(modeLabel(state.dns.mode)))
                        } else {
                            stringResource(R.string.main_dns_no_hostname_text)
                        },
                    )
                },
                leadingContent = { Icon(painterResource(R.drawable.ic_dns), contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            Row(Modifier.padding(horizontal = 16.dp)) {
                OutlinedButton(onClick = actions.onOpenNetworkSettings) {
                    Text(stringResource(R.string.main_open_settings))
                }
            }

            val appName = stringResource(R.string.app_name)
            Section(stringResource(R.string.main_section_shortcuts))
            Body(stringResource(R.string.main_shortcuts_intro))
            Steps(stringResource(R.string.main_shortcut_title), stringResource(R.string.main_shortcut_steps, appName))
            Steps(stringResource(R.string.main_gesture_title), stringResource(R.string.main_gesture_steps, appName))
            Body(stringResource(R.string.main_gesture_other))
        }
    }
}

@Composable
private fun Steps(
    title: String,
    steps: String,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(steps, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun modeLabel(mode: DnsMode): Int =
    when (mode) {
        DnsMode.Hostname -> R.string.dns_hostname
        DnsMode.Opportunistic -> R.string.dns_automatic
        DnsMode.Off -> R.string.dns_off
    }

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun Body(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun TileRow(
    @DrawableRes icon: Int,
    title: String,
    text: String,
    showAdd: Boolean,
    onAdd: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(text) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        trailingContent =
            if (showAdd) {
                { FilledTonalButton(onClick = onAdd) { Text(stringResource(R.string.main_add_tile)) } }
            } else {
                null
            },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun PermissionRow(
    title: String,
    text: String,
    granted: Boolean,
    onAllow: (() -> Unit)?,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(text) },
        trailingContent = {
            when {
                granted ->
                    Icon(
                        painterResource(R.drawable.ic_check_circle),
                        contentDescription = stringResource(R.string.main_granted),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                onAllow != null -> FilledTonalButton(onClick = onAllow) { Text(stringResource(R.string.setup_system_button)) }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun AdbSteps(
    command: String,
    onCopy: () -> Unit,
) {
    Surface(
        shape = AppShapes.Group,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp)) {
            Text(stringResource(R.string.main_adb_title), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.main_adb_steps),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                command,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(end = 8.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCopy) { Text(stringResource(R.string.setup_secure_button)) }
            }
        }
    }
}

@Preview
@Composable
private fun MainScreenPreview() {
    AppTheme {
        MainScreen(
            MainUiState(
                canAddTilesHere = true,
                canWriteSystem = true,
                canWriteSecure = false,
                canPostNotifications = false,
                dns = DnsState(DnsMode.Hostname, "dns.example.com"),
                adbCommand = "adb shell pm grant to.wim.quicksettings android.permission.WRITE_SECURE_SETTINGS",
            ),
            MainActions(),
        )
    }
}
