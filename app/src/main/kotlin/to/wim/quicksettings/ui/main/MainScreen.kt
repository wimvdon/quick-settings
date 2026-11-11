package to.wim.quicksettings.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import to.wim.quicksettings.R
import to.wim.quicksettings.settings.Choice
import to.wim.quicksettings.settings.Defaults
import to.wim.quicksettings.settings.Group
import to.wim.quicksettings.settings.Permissions
import to.wim.quicksettings.settings.PrivateDnsSetting
import to.wim.quicksettings.settings.Row as SettingRow
import to.wim.quicksettings.settings.TileId
import to.wim.quicksettings.settings.TileKind
import to.wim.quicksettings.settings.Toggle
import to.wim.quicksettings.settings.Tracked
import to.wim.quicksettings.ui.iconOf
import to.wim.quicksettings.ui.labelOf
import to.wim.quicksettings.ui.theme.AppShapes
import to.wim.quicksettings.ui.theme.AppTheme
import to.wim.quicksettings.ui.titleOf
import to.wim.quicksettings.ui.valueText

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
    /** The user's choices, which the tune sheets show and change. */
    val defaults: Defaults,
    /** Private DNS: every hostname to offer as the default (saved ones and the one Android has). */
    val hostnames: List<String>,
    /** Private DNS: the hostname Android has, which the "any hostname" default means. */
    val dnsHostname: String?,
    val adbCommand: String,
)

class MainActions(
    val onAddTile: (TileId) -> Unit = {},
    val onOpenPopup: (Group) -> Unit = {},
    val onSetDefault: (Tracked, String) -> Unit = { _, _ -> },
    val onNotify: (Tracked, Boolean) -> Unit = { _, _ -> },
    val onShowRow: (SettingRow, Boolean) -> Unit = { _, _ -> },
    val onUseCurrent: (Group) -> Unit = {},
    val onAllowSystemSettings: () -> Unit = {},
    val onCopyCommand: () -> Unit = {},
    val onAllowNotifications: () -> Unit = {},
    val onOpenPlatformTools: () -> Unit = {},
)

private enum class Page { Home, Permissions, Instructions }

/**
 * The launcher screen: a section per popup, its wide tile with the single tiles below, each drawn
 * like its Quick Settings tile. A tile opens its popup; its tune button sets it up and its plus
 * adds it. Permissions and instructions are pages behind the overflow menu; a banner points to
 * the permissions while one is missing. [setUpRequest] opens a tune sheet (from a popup's own
 * tune button).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    actions: MainActions,
    modifier: Modifier = Modifier,
    setUpRequest: SetUp? = null,
    onSetUpRequestHandled: () -> Unit = {},
) {
    var page by rememberSaveable { mutableStateOf(Page.Home) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var setUpKey by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(setUpRequest) {
        if (setUpRequest != null) {
            page = Page.Home
            setUpKey = setUpRequest.key
            onSetUpRequestHandled()
        }
    }
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
                Page.Home -> HomePage(state, actions, onSetUp = { setUpKey = it.key }) { page = Page.Permissions }
                Page.Permissions -> PermissionsPage(state, actions)
                Page.Instructions -> InstructionsPage(state)
            }
        }
    }
    SetUp.parse(setUpKey)?.let { setUp ->
        ModalBottomSheet(onDismissRequest = { setUpKey = null }) {
            when (setUp) {
                is SetUp.Popup -> PopupSheet(setUp.group, state, actions, onSetUp = { setUpKey = it.key }, onDone = { setUpKey = null })
                is SetUp.Setting -> SettingSheet(setUp.setting, state, actions, onDone = { setUpKey = null })
            }
        }
    }
}

@Composable
private fun HomePage(
    state: MainUiState,
    actions: MainActions,
    onSetUp: (SetUp) -> Unit,
    onOpenPermissions: () -> Unit,
) {
    if (state.permissions.anyMissing) PermissionBanner(onOpenPermissions)
    val cards = state.tiles.associateBy { it.tile }
    TileId.sections().forEach { section ->
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(titleOf(section.category)),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp).semantics { heading() },
            )
            section.popup?.let { cards[it] }?.let { popup ->
                val group = (popup.tile.kind as TileKind.Popup).group
                WideTile(
                    card = popup,
                    contents = state.defaults.rowsOf(group).map { stringResource(titleOf(it.setting)) },
                    canAdd = state.canAddTilesHere,
                    onOpen = { actions.onOpenPopup(group) },
                    onSetUp = { onSetUp(SetUp.Popup(group)) },
                    onAdd = { actions.onAddTile(popup.tile) },
                )
            }
            section.tiles.mapNotNull { cards[it] }.chunked(2).forEach { pair ->
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { card ->
                        RegularTile(
                            card = card,
                            canAdd = state.canAddTilesHere,
                            onOpen = card.tile.popup?.let { group -> { actions.onOpenPopup(group) } },
                            onSetUp = card.tile.setting?.let { setting -> { onSetUp(SetUp.Setting(setting)) } },
                            onAdd = { actions.onAddTile(card.tile) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ---- Tiles ---------------------------------------------------------------------------------

/**
 * A popup's tile across the width: icon and name, and under the name the settings it bundles
 * ([contents]), so it reads as a group that opens a popup (#25); lit while something in it differs.
 */
@Composable
private fun WideTile(
    card: TileCard,
    contents: List<String>,
    canAdd: Boolean,
    onOpen: () -> Unit,
    onSetUp: () -> Unit,
    onAdd: () -> Unit,
) {
    TileSurface(card, onOpen, Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 64.dp).padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Icon(painterResource(iconOf(card.tile)), contentDescription = null)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(stringResource(labelOf(card.tile)), style = MaterialTheme.typography.titleSmall)
                Text(
                    contents.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TileButtons(card, canAdd, onSetUp, onAdd)
        }
    }
}

/** A single tile, half the width: icon and buttons on top, name below; lit while it differs or is on. */
@Composable
private fun RegularTile(
    card: TileCard,
    canAdd: Boolean,
    onOpen: (() -> Unit)?,
    onSetUp: (() -> Unit)?,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TileSurface(card, onOpen, modifier) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(painterResource(iconOf(card.tile)), contentDescription = null)
                Spacer(Modifier.weight(1f))
                TileButtons(card, canAdd, onSetUp, onAdd)
            }
            Text(
                stringResource(labelOf(card.tile)),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
    }
}

/** A tile's background: lit while active; a tap opens its popup when it has one. */
@Composable
private fun TileSurface(
    card: TileCard,
    onOpen: (() -> Unit)?,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val container = if (card.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
    val onContainer = if (card.active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val addedText = stringResource(if (card.added) R.string.main_tile_added else R.string.main_tile_not_added)
    val openLabel = stringResource(R.string.main_open_popup)
    Surface(
        shape = AppShapes.Item,
        color = container,
        contentColor = onContainer,
        modifier =
            modifier
                .clip(AppShapes.Item)
                .then(
                    if (onOpen != null) Modifier.clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen) else Modifier,
                ).semantics { stateDescription = listOfNotNull(card.subtitle, addedText).joinToString(", ") },
    ) { content() }
}

/** The tune button (when the tile has something to set up) and the plus, or a check once added. */
@Composable
private fun TileButtons(
    card: TileCard,
    canAdd: Boolean,
    onSetUp: (() -> Unit)?,
    onAdd: () -> Unit,
) {
    val label = stringResource(labelOf(card.tile))
    if (onSetUp != null) {
        IconButton(onClick = onSetUp) {
            Icon(painterResource(R.drawable.ic_tune), contentDescription = stringResource(R.string.main_set_up, label))
        }
    }
    when {
        canAdd ->
            IconButton(onClick = onAdd) {
                Icon(
                    painterResource(if (card.added) R.drawable.ic_check else R.drawable.ic_add),
                    contentDescription = stringResource(if (card.added) R.string.main_tile_added else R.string.main_add_named, label),
                )
            }
        card.added ->
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.main_tile_added),
                modifier = Modifier.padding(12.dp),
            )
    }
}

// ---- Tune sheets ---------------------------------------------------------------------------

/** A popup's setup: which rows it shows, each with its own tune button. */
@Composable
private fun PopupSheet(
    group: Group,
    state: MainUiState,
    actions: MainActions,
    onSetUp: (SetUp) -> Unit,
    onDone: () -> Unit,
) {
    val shown = state.defaults.rowsOf(group)
    SheetColumn(stringResource(titleOf(group))) {
        SheetLabel(stringResource(R.string.preset_rows))
        group.rows.forEach { row ->
            val checked = row in shown
            val canChange = !checked || shown.size > 1
            val tracked = row.setting as? Tracked
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .toggleable(value = checked, enabled = canChange, role = Role.Checkbox) { actions.onShowRow(row, it) },
            ) {
                Checkbox(checked = checked, onCheckedChange = null, enabled = canChange)
                Icon(painterResource(iconOf(row.setting)), contentDescription = null, modifier = Modifier.padding(horizontal = 16.dp))
                Text(stringResource(titleOf(row.setting)), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (tracked != null) {
                    IconButton(onClick = { onSetUp(SetUp.Setting(tracked)) }) {
                        Icon(
                            painterResource(R.drawable.ic_tune),
                            contentDescription = stringResource(R.string.main_set_up, stringResource(titleOf(tracked))),
                        )
                    }
                }
            }
        }
        SheetButtons(onDone) { OutlinedButton(onClick = { actions.onUseCurrent(group) }) { Text(stringResource(R.string.panel_use_current)) } }
    }
}

/** One setting's setup: its default and whether it notifies. */
@Composable
private fun SettingSheet(
    setting: Tracked,
    state: MainUiState,
    actions: MainActions,
    onDone: () -> Unit,
) {
    val resources = LocalResources.current
    val default = state.defaults.of(setting)
    SheetColumn(stringResource(titleOf(setting))) {
        SheetLabel(stringResource(R.string.preset_default))
        when (setting) {
            PrivateDnsSetting -> {
                val options = listOf(PrivateDnsSetting.OFF, PrivateDnsSetting.AUTOMATIC) + state.hostnames.map(PrivateDnsSetting::hostnameValue)
                val selected =
                    options.firstOrNull { option ->
                        default == option ||
                            (default == PrivateDnsSetting.ANY_HOSTNAME && PrivateDnsSetting.hostnameOf(option) == state.dnsHostname)
                    }
                Dropdown(
                    label = stringResource(R.string.preset_default),
                    selected = valueText(resources, setting, selected ?: default),
                    options = options.map { valueText(resources, setting, it) to null },
                    onSelect = { actions.onSetDefault(setting, options[it]) },
                )
            }
            is Toggle -> Segmented(listOf(Toggle.OFF, Toggle.ON), default, { valueText(resources, setting, it) }) { actions.onSetDefault(setting, it) }
            is Choice ->
                Segmented(setting.options, default, { valueText(resources, setting, it, short = true) }) { actions.onSetDefault(setting, it) }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(min = 56.dp)
                    .toggleable(value = state.defaults.notifies(setting), role = Role.Switch) { actions.onNotify(setting, it) },
        ) {
            Text(stringResource(R.string.panel_notify), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = state.defaults.notifies(setting), onCheckedChange = null)
        }
        SheetButtons(onDone)
    }
}

@Composable
private fun SheetColumn(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp).semantics { heading() })
        content()
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp).semantics { heading() },
    )
}

@Composable
private fun SheetButtons(
    onDone: () -> Unit,
    start: @Composable () -> Unit = {},
) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        start()
        Spacer(Modifier.weight(1f))
        Button(onClick = onDone) { Text(stringResource(R.string.panel_done)) }
    }
}

/** Two to four options as a segmented button, as in the popups. */
@Composable
private fun Segmented(
    options: List<String>,
    selected: String,
    text: (String) -> String,
    onSelect: (String) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(text(option), maxLines = 1) }
        }
    }
}

/** A choice from a menu: each option's text, with an optional second line. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Dropdown(
    label: String,
    selected: String,
    options: List<Pair<String, String?>>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, (text, supporting) ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(text)
                            supporting?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
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
    Guide(stringResource(R.string.main_guide_tiles_title))
    NumberedSteps(
        GuideSteps.split(stringResource(if (state.canAddTilesHere) R.string.main_guide_tiles_steps else R.string.main_guide_tiles_steps_edit)),
        StepsPadding,
    )
    Guide(stringResource(R.string.main_guide_defaults_title))
    NumberedSteps(GuideSteps.split(stringResource(R.string.main_guide_defaults_steps)), StepsPadding)
}

// ---- Shared pieces -------------------------------------------------------------------------

private val StepsPadding = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)

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
                defaults = Defaults(),
                hostnames = listOf("dns.example.com"),
                dnsHostname = "dns.example.com",
                adbCommand = "adb shell pm grant to.wim.quicksettings android.permission.WRITE_SECURE_SETTINGS",
            ),
            MainActions(),
        )
    }
}
