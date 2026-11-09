package to.wim.quicksettings.ui.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import to.wim.quicksettings.R
import to.wim.quicksettings.ui.theme.AppShapes
import to.wim.quicksettings.ui.theme.AppTheme

data class MainUiState(
    /** Android 13+ can add a tile with one tap; older versions only by editing Quick Settings. */
    val canAddTilesHere: Boolean,
    val canWriteSystem: Boolean,
    val canWriteSecure: Boolean,
    val canPostNotifications: Boolean,
    val adbCommand: String,
)

class MainActions(
    val onAddScreenTile: () -> Unit = {},
    val onAddDnsTile: () -> Unit = {},
    val onAllowSystemSettings: () -> Unit = {},
    val onCopyCommand: () -> Unit = {},
    val onAllowNotifications: () -> Unit = {},
)

/** Permissions first (with the adb steps), then instructions: tiles, shortcuts, Quick Tap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    actions: MainActions,
    modifier: Modifier = Modifier,
) {
    val appName = stringResource(R.string.app_name)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(appName) }) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 640.dp)
                .padding(bottom = 32.dp),
        ) {
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

            Section(stringResource(R.string.main_section_instructions))
            Guide(stringResource(R.string.main_section_tiles))
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

            Guide(stringResource(R.string.main_shortcut_title))
            Body(stringResource(R.string.main_shortcuts_intro))
            NumberedSteps(GuideSteps.split(stringResource(R.string.main_shortcut_steps, appName)), StepsPadding)

            Guide(stringResource(R.string.main_gesture_title))
            Body(stringResource(R.string.main_gesture_pixel_only))
            NumberedSteps(GuideSteps.split(stringResource(R.string.main_gesture_steps, appName)), StepsPadding)
            Body(stringResource(R.string.main_gesture_other))
        }
    }
}

private val StepsPadding = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                .semantics { heading() },
    )
}

/** A guide's own heading inside the Instructions section. */
@Composable
private fun Guide(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier =
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                .semantics { heading() },
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

/** Steps with a number in a primary-coloured circle, like Snoozer's guides. */
@Composable
private fun NumberedSteps(
    steps: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        steps.forEachIndexed { index, step ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                )
            }
        }
    }
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
            Text(
                stringResource(R.string.main_adb_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp).semantics { heading() },
            )
            NumberedSteps(
                GuideSteps.split(stringResource(R.string.main_adb_steps)),
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                command,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 48.dp, top = 8.dp, end = 8.dp),
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
                adbCommand = "adb shell pm grant to.wim.quicksettings android.permission.WRITE_SECURE_SETTINGS",
            ),
            MainActions(),
        )
    }
}
