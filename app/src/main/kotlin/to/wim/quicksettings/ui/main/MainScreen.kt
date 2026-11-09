package to.wim.quicksettings.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import to.wim.quicksettings.R
import to.wim.quicksettings.settings.Group
import to.wim.quicksettings.settings.Permissions
import to.wim.quicksettings.settings.TileId
import to.wim.quicksettings.settings.TileKind
import to.wim.quicksettings.ui.iconOf
import to.wim.quicksettings.ui.labelOf
import to.wim.quicksettings.ui.summaryOf
import to.wim.quicksettings.ui.theme.AppShapes
import to.wim.quicksettings.ui.theme.AppTheme
import to.wim.quicksettings.ui.titleOf

/** A tile as the main screen shows it: lit like in Quick Settings, and whether it's added. */
data class TileCard(
    val tile: TileId,
    val active: Boolean,
    val subtitle: String?,
    val added: Boolean,
)

data class MainUiState(
    /** Android 13+ can add a tile with one tap; older versions only by editing Quick Settings. */
    val canAddTilesHere: Boolean,
    val permissions: Permissions,
    val tiles: List<TileCard>,
    val adbCommand: String,
)

class MainActions(
    val onAddTile: (TileId) -> Unit = {},
    val onOpenPopup: (Group) -> Unit = {},
    val onAllowSystemSettings: () -> Unit = {},
    val onCopyCommand: () -> Unit = {},
    val onAllowNotifications: () -> Unit = {},
    val onOpenPlatformTools: () -> Unit = {},
)

private enum class Page { Home, Permissions, Instructions }

/**
 * The launcher screen: the tiles and the popups, each drawn like its Quick Settings tile; a tap
 * adds it. Permissions and instructions are pages behind the overflow menu; a banner points to the
 * permissions while one is missing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    actions: MainActions,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableStateOf(Page.Home) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = page != Page.Home) { page = Page.Home }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            when (page) {
                                Page.Home -> R.string.app_name
                                Page.Permissions -> R.string.main_section_permissions
                                Page.Instructions -> R.string.main_section_instructions
                            },
                        ),
                    )
                },
                navigationIcon = {
                    if (page != Page.Home) {
                        IconButton(onClick = { page = Page.Home }) {
                            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (page == Page.Home) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.main_more))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.main_section_permissions)) },
                                    onClick = {
                                        menuOpen = false
                                        page = Page.Permissions
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.main_section_instructions)) },
                                    onClick = {
                                        menuOpen = false
                                        page = Page.Instructions
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
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
            when (page) {
                Page.Home -> HomePage(state, actions) { page = Page.Permissions }
                Page.Permissions -> PermissionsPage(state, actions)
                Page.Instructions -> InstructionsPage(state)
            }
        }
    }
}

@Composable
private fun HomePage(
    state: MainUiState,
    actions: MainActions,
    onOpenPermissions: () -> Unit,
) {
    if (state.permissions.anyMissing) PermissionBanner(onOpenPermissions)

    Section(stringResource(R.string.main_section_tiles))
    Body(stringResource(if (state.canAddTilesHere) R.string.main_tiles_hint else R.string.main_tiles_hint_edit))
    val cards = state.tiles.associateBy { it.tile }
    TileId.singlesByCategory().forEach { (category, tiles) ->
        Guide(stringResource(titleOf(category)))
        TileGrid(tiles.mapNotNull { cards[it] }, state.canAddTilesHere, actions)
    }

    Section(stringResource(R.string.main_section_popups))
    Body(stringResource(R.string.main_popups_hint))
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TileId.popups.mapNotNull { cards[it] }.forEach { card ->
            val group = (card.tile.kind as TileKind.Popup).group
            QsTile(
                card = card,
                canAdd = state.canAddTilesHere,
                summary = stringResource(summaryOf(group)),
                onClick = { actions.onAddTile(card.tile) },
                onLongClick = { actions.onOpenPopup(group) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TileGrid(
    cards: List<TileCard>,
    canAdd: Boolean,
    actions: MainActions,
) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cards.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { card ->
                    QsTile(
                        card = card,
                        canAdd = canAdd,
                        onClick = { actions.onAddTile(card.tile) },
                        onLongClick = card.tile.popup?.let { group -> { actions.onOpenPopup(group) } },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * A tile shaped like Android's large Quick Settings tile: icon, name and state, lit while
 * active. A plus shows it can be added, a check that it is in Quick Settings already.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QsTile(
    card: TileCard,
    canAdd: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    val container = if (card.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    val content = if (card.active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val label = stringResource(labelOf(card.tile))
    val addedText = stringResource(if (card.added) R.string.main_tile_added else R.string.main_tile_not_added)
    val openLabel = stringResource(R.string.main_open_popup)
    Surface(
        shape = AppShapes.Item,
        color = container,
        contentColor = content,
        modifier =
            modifier
                .clip(AppShapes.Item)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onClickLabel = if (canAdd) stringResource(R.string.main_add_tile) else null,
                    onLongClickLabel = if (onLongClick != null) openLabel else null,
                    role = Role.Button,
                ).semantics(mergeDescendants = true) {
                    stateDescription = listOfNotNull(card.subtitle, addedText).joinToString(", ")
                    if (onLongClick != null) {
                        onLongClick(label = openLabel) {
                            onLongClick()
                            true
                        }
                    }
                },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 72.dp).padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Icon(painterResource(iconOf(card.tile)), contentDescription = null)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                card.subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                summary?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (card.active) content else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (canAdd || card.added) {
                Icon(
                    painterResource(if (card.added) R.drawable.ic_check else R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun PermissionBanner(onOpen: () -> Unit) {
    Surface(
        shape = AppShapes.Group,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Icon(painterResource(R.drawable.ic_info), contentDescription = null)
            Text(
                stringResource(R.string.main_banner_permissions),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            )
            TextButton(onClick = onOpen) { Text(stringResource(R.string.main_banner_open)) }
        }
    }
}

// ---- Permissions page ----------------------------------------------------------------------

@Composable
private fun PermissionsPage(
    state: MainUiState,
    actions: MainActions,
) {
    PermissionRow(
        title = stringResource(R.string.main_permission_system),
        text = stringResource(R.string.main_permission_system_text),
        granted = state.permissions.system,
        onAllow = actions.onAllowSystemSettings,
    )
    PermissionRow(
        title = stringResource(R.string.main_permission_notifications),
        text = stringResource(R.string.main_permission_notifications_text),
        granted = state.permissions.notifications,
        onAllow = actions.onAllowNotifications,
    )
    PermissionRow(
        title = stringResource(R.string.main_permission_secure),
        text = stringResource(R.string.main_permission_secure_text),
        granted = state.permissions.secure,
        onAllow = null,
    )
    AdbGuide(state.adbCommand, actions)
}

/** What adb is and how to run the command, in Snoozer's words. */
@Composable
private fun AdbGuide(
    command: String,
    actions: MainActions,
) {
    Surface(
        shape = AppShapes.Group,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.main_adb_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.main_adb_what), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = actions.onOpenPlatformTools) {
                Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.main_adb_download), modifier = Modifier.padding(start = 8.dp))
            }
            NumberedSteps(GuideSteps.split(stringResource(R.string.main_adb_steps)))
            SelectionContainer {
                Text(
                    command,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.medium)
                            .padding(12.dp),
                )
            }
            FilledTonalButton(onClick = actions.onCopyCommand, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.setup_secure_button))
            }
            Text(
                stringResource(R.string.main_adb_keep),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- Instructions page ---------------------------------------------------------------------

@Composable
private fun InstructionsPage(state: MainUiState) {
    val appName = stringResource(R.string.app_name)
    Guide(stringResource(R.string.main_guide_tiles_title))
    NumberedSteps(
        GuideSteps.split(stringResource(if (state.canAddTilesHere) R.string.main_guide_tiles_steps else R.string.main_guide_tiles_steps_edit)),
        StepsPadding,
    )
    Guide(stringResource(R.string.main_guide_defaults_title))
    NumberedSteps(GuideSteps.split(stringResource(R.string.main_guide_defaults_steps)), StepsPadding)
    Guide(stringResource(R.string.main_shortcut_title))
    Body(stringResource(R.string.main_shortcuts_intro))
    NumberedSteps(GuideSteps.split(stringResource(R.string.main_shortcut_steps, appName)), StepsPadding)
}

// ---- Shared pieces -------------------------------------------------------------------------

private val StepsPadding = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp)
                .semantics { heading() },
    )
}

/** A heading inside a section. */
@Composable
private fun Guide(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier =
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                .semantics { heading() },
    )
}

@Composable
private fun Body(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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

@Preview
@Composable
private fun MainScreenPreview() {
    AppTheme {
        MainScreen(
            MainUiState(
                canAddTilesHere = true,
                permissions = Permissions(system = true, secure = false, notifications = false),
                tiles = TileId.entries.map { TileCard(it, active = it == TileId.AlwaysOn, subtitle = "Default", added = it == TileId.ScreenPopup) },
                adbCommand = "adb shell pm grant to.wim.quicksettings android.permission.WRITE_SECURE_SETTINGS",
            ),
            MainActions(),
        )
    }
}
