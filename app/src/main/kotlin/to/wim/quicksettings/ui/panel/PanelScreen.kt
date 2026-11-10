package to.wim.quicksettings.ui.panel

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import to.wim.quicksettings.R
import to.wim.quicksettings.settings.Access
import to.wim.quicksettings.settings.Catalog
import to.wim.quicksettings.settings.Choice
import to.wim.quicksettings.settings.Group
import to.wim.quicksettings.settings.Level
import to.wim.quicksettings.settings.Permissions
import to.wim.quicksettings.settings.PrivateDnsSetting
import to.wim.quicksettings.settings.Row as SettingRow
import to.wim.quicksettings.settings.Toggle
import to.wim.quicksettings.settings.Tracked
import to.wim.quicksettings.ui.iconOf
import to.wim.quicksettings.ui.theme.AppShapes
import to.wim.quicksettings.ui.theme.AppTheme
import to.wim.quicksettings.ui.titleOf
import to.wim.quicksettings.ui.valueText

/** One row as shown: its setting's current value and default, and slider positions. */
data class RowState(
    val row: SettingRow,
    val value: String? = null,
    val changed: Boolean = false,
    val position: Float? = null,
    val levelPosition: Float? = null,
)

data class PanelUiState(
    val group: Group,
    val rows: List<RowState>,
    val hasChanges: Boolean,
    /** Private DNS: every hostname to offer (saved ones and the one Android has). */
    val hostnames: List<String>,
    /** Private DNS: the saved hostnames, which can be removed. */
    val savedHostnames: Set<String>,
    val permissions: Permissions,
    val adbCommand: String,
)

class PanelActions(
    val onToggle: (Toggle, Boolean) -> Unit = { _, _ -> },
    val onSelect: (Choice, String) -> Unit = { _, _ -> },
    val onLevel: (Level, Float) -> Unit = { _, _ -> },
    val onLevelFinished: () -> Unit = {},
    val onWrite: (Tracked, String) -> Unit = { _, _ -> },
    /** Returns false when the hostname isn't valid, so the dialog stays open. */
    val onAddHostname: (String) -> Boolean = { true },
    val onRemoveHostname: (String) -> Unit = {},
    /** The tune button: the popup's setup on the main screen. */
    val onSetUp: () -> Unit = {},
    val onReset: () -> Unit = {},
    val onAllowSystemSettings: () -> Unit = {},
    val onCopyCommand: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onDone: () -> Unit = {},
)

private val DialogShape = RoundedCornerShape(28.dp)
private val Outer = 24.dp
private val Inner = 4.dp

/** Corners of a row in a group: rounded on the outside, nearly square where rows meet. */
private fun rowShape(
    index: Int,
    count: Int,
) = RoundedCornerShape(
    topStart = if (index == 0) Outer else Inner,
    topEnd = if (index == 0) Outer else Inner,
    bottomStart = if (index == count - 1) Outer else Inner,
    bottomEnd = if (index == count - 1) Outer else Inner,
)

/** A row's content, drawn in a shape the list decides. */
private typealias RowContent = @Composable (Shape) -> Unit

/**
 * The popup: a title, one group of rows (a setting that differs from its default is highlighted,
 * like the connected network in the Internet dialog), and Settings / Reset / Done at the bottom.
 * The tune button opens the popup's setup on the main screen.
 */
@Composable
fun PanelScreen(
    state: PanelUiState,
    actions: PanelActions,
    modifier: Modifier = Modifier,
) {
    var addingHostname by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 480.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        shape = DialogShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        ) {
            Header(state.group, actions)
            Spacer(Modifier.height(16.dp))

            SetupCards(state, actions)

            val rows: List<RowContent> = liveRows(state, actions) { addingHostname = true }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                rows.forEachIndexed { index, row -> row(rowShape(index, rows.size)) }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = actions.onOpenSettings) { Text(stringResource(R.string.panel_settings)) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = actions.onReset, enabled = state.hasChanges) { Text(stringResource(R.string.panel_reset)) }
                Button(onClick = actions.onDone) { Text(stringResource(R.string.panel_done)) }
            }
        }
    }
    if (addingHostname) {
        HostnameDialog(onAdd = actions.onAddHostname, onDismiss = { addingHostname = false })
    }
}

@Composable
private fun Header(
    group: Group,
    actions: PanelActions,
) {
    val title = stringResource(titleOf(group))
    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 48.dp),
        )
        IconButton(onClick = actions.onSetUp, modifier = Modifier.align(Alignment.CenterEnd)) {
            Icon(painterResource(R.drawable.ic_tune), contentDescription = stringResource(R.string.main_set_up, title))
        }
    }
}

@Composable
private fun SetupCards(
    state: PanelUiState,
    actions: PanelActions,
) {
    val needs = state.group.accesses
    if (Access.System in needs && !state.permissions.system) {
        SetupCard(
            title = stringResource(R.string.setup_system_title),
            text = stringResource(R.string.setup_system_text),
            button = stringResource(R.string.setup_system_button),
            onClick = actions.onAllowSystemSettings,
        )
    }
    if (Access.Secure in needs && !state.permissions.secure) {
        SetupCard(
            title = stringResource(R.string.setup_secure_title),
            text = stringResource(R.string.setup_secure_text),
            command = state.adbCommand,
            button = stringResource(R.string.setup_secure_button),
            onClick = actions.onCopyCommand,
        )
    }
}

// ---- Live rows: what the settings are now --------------------------------------------------

private fun liveRows(
    state: PanelUiState,
    actions: PanelActions,
    onAddHostname: () -> Unit,
): List<RowContent> =
    state.rows.flatMap { rs ->
        val enabled = state.permissions.allows(rs.row.setting.access)
        val setting = rs.row.setting
        when {
            setting == Catalog.BrightnessMode -> listOf<RowContent> { shape -> BrightnessRow(rs, enabled, shape, actions) }
            setting == PrivateDnsSetting -> dnsRows(state, rs, enabled, actions, onAddHostname)
            setting is Toggle ->
                listOf<RowContent> { shape ->
                    ToggleRow(
                        setting = setting,
                        checked = rs.value == Toggle.ON,
                        highlighted = rs.changed,
                        enabled = enabled,
                        shape = shape,
                        onCheckedChange = { actions.onToggle(setting, it) },
                        level = rs.row.level?.takeIf { rs.value == Toggle.ON },
                        levelPosition = rs.levelPosition,
                        levelEnabled = rs.row.level?.let { state.permissions.allows(it.access) } ?: false,
                        actions = actions,
                    )
                }
            setting is Choice ->
                listOf<RowContent> { shape ->
                    ChoiceRow(setting, rs.value, rs.changed, enabled, shape) { actions.onSelect(setting, it) }
                }
            setting is Level ->
                listOf<RowContent> { shape -> LevelRow(setting, rs.position ?: 0f, enabled, shape, actions) }
            else -> emptyList()
        }
    }

private fun dnsRows(
    state: PanelUiState,
    rs: RowState,
    enabled: Boolean,
    actions: PanelActions,
    onAddHostname: () -> Unit,
): List<RowContent> {
    val options = listOf(PrivateDnsSetting.OFF, PrivateDnsSetting.AUTOMATIC) + state.hostnames.map(PrivateDnsSetting::hostnameValue)
    return options.map { option ->
        val name = PrivateDnsSetting.hostnameOf(option)
        val content: RowContent = { shape ->
            RadioRow(
                icon = if (name != null) R.drawable.ic_dns else null,
                title = valueText(LocalResources.current, PrivateDnsSetting, option),
                selected = rs.value == option,
                highlighted = rs.value == option && rs.changed,
                enabled = enabled,
                shape = shape,
                onClick = { actions.onWrite(PrivateDnsSetting, option) },
                onRemove =
                    if (name != null && name in state.savedHostnames && rs.value != option) {
                        { actions.onRemoveHostname(name) }
                    } else {
                        null
                    },
            )
        }
        content
    } +
        listOf<RowContent> { shape ->
            ActionRow(R.drawable.ic_add, stringResource(R.string.dns_add_hostname), enabled, shape, onAddHostname)
        }
}

// ---- Row building blocks -------------------------------------------------------------------

@Composable
private fun rowColors(highlighted: Boolean): Pair<Color, Color> =
    if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
    }

@Composable
private fun RowSurface(
    highlighted: Boolean,
    shape: Shape,
    content: @Composable () -> Unit,
) {
    val (container, onContainer) = rowColors(highlighted)
    Surface(shape = shape, color = container, contentColor = onContainer, modifier = Modifier.fillMaxWidth()) { content() }
}

@Composable
private fun BrightnessRow(
    rs: RowState,
    enabled: Boolean,
    shape: Shape,
    actions: PanelActions,
) {
    val manual = rs.value == Catalog.BRIGHTNESS_MANUAL
    val position = rs.levelPosition ?: 0f
    RowSurface(rs.changed, shape) {
        Column(Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(
                    painterResource(if (manual) R.drawable.ic_brightness else R.drawable.ic_brightness_auto),
                    contentDescription = null,
                )
                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text(stringResource(R.string.brightness_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text =
                            if (manual) {
                                stringResource(R.string.brightness_manual, (position * 100).toInt())
                            } else {
                                stringResource(R.string.brightness_auto)
                            },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (manual) {
                    FilledTonalIconButton(onClick = { actions.onSelect(Catalog.BrightnessMode, Catalog.BRIGHTNESS_AUTO) }, enabled = enabled) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.brightness_reset),
                        )
                    }
                }
            }
            Slider(
                value = position,
                onValueChange = { actions.onLevel(Catalog.Brightness, it) },
                onValueChangeFinished = actions.onLevelFinished,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
            )
        }
    }
}

@Composable
private fun ToggleRow(
    setting: Toggle,
    checked: Boolean,
    highlighted: Boolean,
    enabled: Boolean,
    shape: Shape,
    onCheckedChange: (Boolean) -> Unit,
    actions: PanelActions,
    level: Level? = null,
    levelPosition: Float? = null,
    levelEnabled: Boolean = false,
) {
    RowSurface(highlighted, shape) {
        Column {
            SwitchContent(iconOf(setting), stringResource(titleOf(setting)), checked, enabled, onCheckedChange)
            if (level != null) {
                Column(Modifier.padding(start = 56.dp, end = 16.dp, bottom = 8.dp)) {
                    Text(stringResource(titleOf(level)), style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = levelPosition ?: 0f,
                        onValueChange = { actions.onLevel(level, it) },
                        onValueChangeFinished = actions.onLevelFinished,
                        enabled = levelEnabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchLine(
    @DrawableRes icon: Int,
    title: String,
    checked: Boolean,
    highlighted: Boolean,
    enabled: Boolean,
    shape: Shape,
    onCheckedChange: (Boolean) -> Unit,
) {
    RowSurface(highlighted, shape) { SwitchContent(icon, title, checked, enabled, onCheckedChange) }
}

@Composable
private fun SwitchContent(
    @DrawableRes icon: Int,
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null)
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(horizontal = 16.dp))
        // The row handles the click, so the switch itself only shows the state.
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/**
 * A choice as a segmented button. A value set elsewhere (say 2 minutes) is shown as text, with
 * no option selected.
 */
@Composable
private fun ChoiceRow(
    choice: Choice,
    value: String?,
    highlighted: Boolean,
    enabled: Boolean,
    shape: Shape,
    onSelect: (String) -> Unit,
) {
    val resources = LocalResources.current
    RowSurface(highlighted, shape) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 24.dp)) {
                Icon(painterResource(iconOf(choice)), contentDescription = null)
                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text(stringResource(titleOf(choice)), style = MaterialTheme.typography.titleMedium)
                    if (value !in choice.options) {
                        Text(valueText(resources, choice, value), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = 40.dp, top = 8.dp)) {
                choice.options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == value,
                        onClick = { onSelect(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, choice.options.size),
                        enabled = enabled,
                    ) {
                        Text(valueText(resources, choice, option, short = true), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelRow(
    level: Level,
    position: Float,
    enabled: Boolean,
    shape: Shape,
    actions: PanelActions,
) {
    RowSurface(highlighted = false, shape = shape) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Icon(painterResource(iconOf(level)), contentDescription = null)
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(stringResource(titleOf(level)), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                Slider(
                    value = position,
                    onValueChange = { actions.onLevel(level, it) },
                    onValueChangeFinished = actions.onLevelFinished,
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
private fun RadioRow(
    @DrawableRes icon: Int?,
    title: String,
    selected: Boolean,
    highlighted: Boolean,
    enabled: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    RowSurface(highlighted, shape) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
                    .heightIn(min = 56.dp)
                    .padding(start = 4.dp, end = 4.dp),
        ) {
            RadioButton(selected = selected, onClick = null, enabled = enabled, modifier = Modifier.padding(12.dp))
            if (icon != null) Icon(painterResource(icon), contentDescription = null, modifier = Modifier.padding(end = 12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (onRemove != null) {
                IconButton(onClick = onRemove, enabled = enabled) {
                    Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.dns_remove_hostname, title))
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    @DrawableRes icon: Int,
    title: String,
    enabled: Boolean,
    shape: Shape,
    onClick: () -> Unit,
) {
    RowSurface(highlighted = false, shape = shape) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.padding(start = 2.dp, end = 18.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun HostnameDialog(
    onAdd: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    val submit = {
        if (onAdd(text)) onDismiss() else invalid = true
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dns_add_hostname)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    invalid = false
                },
                label = { Text(stringResource(R.string.dns_hostname_field)) },
                placeholder = { Text("dns.example.com") },
                singleLine = true,
                isError = invalid,
                supportingText = if (invalid) ({ Text(stringResource(R.string.dns_hostname_invalid)) }) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { submit() }),
            )
        },
        confirmButton = { TextButton(onClick = submit, enabled = text.isNotBlank()) { Text(stringResource(R.string.dialog_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

@Composable
private fun SetupCard(
    title: String,
    text: String,
    button: String,
    onClick: () -> Unit,
    command: String? = null,
) {
    Surface(
        shape = AppShapes.Group,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp, end = 8.dp))
            if (command != null) {
                Text(
                    command,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp, end = 8.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClick) { Text(button) }
            }
        }
    }
}

private fun previewState(
    group: Group,
    changed: Boolean,
) = PanelUiState(
    group = group,
    rows =
        group.rows.map {
            val tracked = it.setting as? Tracked
            RowState(
                row = it,
                value = tracked?.appDefault,
                changed = changed && it.setting == Catalog.AlwaysOn,
                position = 0.5f,
                levelPosition = 0.4f,
            )
        },
    hasChanges = changed,
    hostnames = listOf("dns.example.com"),
    savedHostnames = setOf("dns.example.com"),
    permissions = Permissions(system = true, secure = !changed, notifications = true),
    adbCommand = "adb shell pm grant to.wim.quicksettings android.permission.WRITE_SECURE_SETTINGS",
)

@Preview
@Composable
private fun PanelScreenPreview() =
    AppTheme {
        Column {
            Spacer(Modifier.size(1.dp))
            PanelScreen(previewState(Group.Screen, changed = true), actions = PanelActions())
        }
    }

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PanelNetworkPreview() =
    AppTheme { PanelScreen(previewState(Group.Network, changed = false), actions = PanelActions()) }
