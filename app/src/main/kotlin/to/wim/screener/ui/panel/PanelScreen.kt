package to.wim.screener.ui.panel

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import to.wim.screener.R
import to.wim.screener.screen.BrightnessCurve
import to.wim.screener.screen.ScreenState
import to.wim.screener.ui.theme.AppShapes
import to.wim.screener.ui.theme.ScreenerTheme
import to.wim.screener.ui.timeoutText

data class PanelUiState(
    val screen: ScreenState,
    val canWriteSystem: Boolean,
    val canWriteSecure: Boolean,
    val adbCommand: String,
)

class PanelActions(
    val onBrightness: (Float) -> Unit = {},
    val onBrightnessFinished: () -> Unit = {},
    val onAutoBrightness: () -> Unit = {},
    val onTimeoutExtended: (Boolean) -> Unit = {},
    val onAlwaysOn: (Boolean) -> Unit = {},
    val onTapToWake: (Boolean) -> Unit = {},
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

/**
 * The popup: a title and status line, one group of rows (a changed setting is highlighted, like
 * the connected network in the Internet dialog), and Settings / Done at the bottom.
 */
@Composable
fun PanelScreen(
    state: PanelUiState,
    actions: PanelActions,
    modifier: Modifier = Modifier,
) {
    val screen = state.screen
    val changes = screen.changes
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 480.dp)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        shape = DialogShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.panel_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text =
                    if (changes.isEmpty()) {
                        stringResource(R.string.panel_subtitle_default)
                    } else {
                        pluralStringResource(R.plurals.panel_subtitle_changes, changes.size, changes.size)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp),
            )

            if (!state.canWriteSystem) {
                SetupCard(
                    title = stringResource(R.string.setup_system_title),
                    text = stringResource(R.string.setup_system_text),
                    button = stringResource(R.string.setup_system_button),
                    onClick = actions.onAllowSystemSettings,
                )
            }
            if (!state.canWriteSecure) {
                SetupCard(
                    title = stringResource(R.string.setup_secure_title),
                    text = stringResource(R.string.setup_secure_text),
                    command = state.adbCommand,
                    button = stringResource(R.string.setup_secure_button),
                    onClick = actions.onCopyCommand,
                )
            }

            val rows = 4
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BrightnessRow(screen, state.canWriteSystem, rowShape(0, rows), actions)
                SwitchRow(
                    icon = R.drawable.ic_timer,
                    title = stringResource(R.string.timeout_title),
                    summary = timeoutText(LocalResources.current, screen.timeoutMs),
                    checked = screen.timeoutExtended,
                    changed = ScreenState.Change.Timeout in changes,
                    enabled = state.canWriteSystem,
                    shape = rowShape(1, rows),
                    onCheckedChange = actions.onTimeoutExtended,
                )
                SwitchRow(
                    icon = R.drawable.ic_always_on,
                    title = stringResource(R.string.always_on_title),
                    summary = null,
                    checked = screen.alwaysOn,
                    changed = ScreenState.Change.AlwaysOn in changes,
                    enabled = state.canWriteSecure,
                    shape = rowShape(2, rows),
                    onCheckedChange = actions.onAlwaysOn,
                )
                SwitchRow(
                    icon = R.drawable.ic_tap,
                    title = stringResource(R.string.tap_to_wake_title),
                    summary = null,
                    checked = screen.tapToWake,
                    changed = ScreenState.Change.TapToWake in changes,
                    enabled = state.canWriteSecure,
                    shape = rowShape(3, rows),
                    onCheckedChange = actions.onTapToWake,
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OutlinedButton(onClick = actions.onOpenSettings) { Text(stringResource(R.string.panel_settings)) }
                Button(onClick = actions.onDone) { Text(stringResource(R.string.panel_done)) }
            }
        }
    }
}

@Composable
private fun rowColors(changed: Boolean): Pair<Color, Color> =
    if (changed) {
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
    }

@Composable
private fun BrightnessRow(
    screen: ScreenState,
    enabled: Boolean,
    shape: Shape,
    actions: PanelActions,
) {
    val manual = !screen.autoBrightness
    val (container, content) = rowColors(manual)
    Surface(shape = shape, color = container, contentColor = content, modifier = Modifier.fillMaxWidth()) {
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
                                stringResource(R.string.brightness_manual, BrightnessCurve.toPercent(screen.brightness))
                            } else {
                                stringResource(R.string.brightness_auto)
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (manual) content else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (manual) {
                    FilledTonalIconButton(onClick = actions.onAutoBrightness, enabled = enabled) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.brightness_reset),
                        )
                    }
                }
            }
            Slider(
                value = BrightnessCurve.toPosition(screen.brightness),
                onValueChange = actions.onBrightness,
                onValueChangeFinished = actions.onBrightnessFinished,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().padding(end = 4.dp),
            )
        }
    }
}

@Composable
private fun SwitchRow(
    @DrawableRes icon: Int,
    title: String,
    summary: String?,
    checked: Boolean,
    changed: Boolean,
    enabled: Boolean,
    shape: Shape,
    onCheckedChange: (Boolean) -> Unit,
) {
    val (container, content) = rowColors(changed)
    Surface(shape = shape, color = container, contentColor = content, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(painterResource(icon), contentDescription = null)
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (summary != null) {
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (changed) content else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // The row handles the click, so the switch itself only shows the state.
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    }
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

@Composable
private fun PreviewPanel(state: PanelUiState) {
    ScreenerTheme {
        Column {
            Spacer(Modifier.size(1.dp))
            PanelScreen(state, PanelActions())
        }
    }
}

@Preview
@Composable
private fun PanelDefaultPreview() =
    PreviewPanel(PanelUiState(ScreenState.DEFAULTS, canWriteSystem = true, canWriteSecure = true, adbCommand = ""))

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PanelChangedPreview() =
    PreviewPanel(
        PanelUiState(
            ScreenState.DEFAULTS.copy(autoBrightness = false, timeoutMs = ScreenState.EXTENDED_TIMEOUT_MS),
            canWriteSystem = true,
            canWriteSecure = false,
            adbCommand = "adb shell pm grant to.wim.screener android.permission.WRITE_SECURE_SETTINGS",
        ),
    )
