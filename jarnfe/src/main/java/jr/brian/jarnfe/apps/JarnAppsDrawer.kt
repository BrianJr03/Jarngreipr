package jr.brian.jarnfe.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnLoadingGate
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.core.ui.focus.JarnRevealLazyItems
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.focus.JarnFocusStyle
import jr.brian.core.ui.focus.jarnClickable
import jr.brian.core.ui.focus.jarnFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.dock.JarnDockApp
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.LocalJarnInputRouter
import jr.brian.jarnfe.input.LocalJarnScreen
import jr.brian.jarnfe.look.rememberJarnDisplay
import jr.brian.jarnfe.settings.rememberJarnLauncherPrefs
import kotlinx.coroutines.flow.first

private val AppIconSize = 48.dp

internal class JarnAppsCommands(
    val open: (String) -> Unit,
    val options: JarnAppOptionsData,
)

@Stable
private class JarnAppsFocus {
    private val requesters = HashMap<String, FocusRequester>()
    private var order = emptyList<String>()
    var pending by mutableStateOf<String?>(null)
    var current by mutableStateOf<String?>(null)
    var lastKey: String? = null
    var listHasFocus = false

    fun requester(key: String) = requesters.getOrPut(key) { FocusRequester() }

    fun onFocused(key: String) {
        current = key
        lastKey = key
    }

    fun onListFocus(hasFocus: Boolean) {
        listHasFocus = hasFocus
        if (!hasFocus) current = null
    }

    fun onOrderChanged(next: List<String>, focusedKey: String?) {
        focusedKey?.let { pending = jarnAppsFocusAfter(order, next, it) }
        order = next
    }
}

@Composable
internal fun JarnAppsRoute() {
    val context = LocalContext.current
    val data = remember(context) { context.jarnAppsDataOrNull() }
    if (data == null) {
        JarnAppsEmpty()
        return
    }
    LaunchedEffect(data) { JarnApps.observe(data) }
    val commands = rememberJarnAppsCommands(data)
    JarnLoadingGate(JarnApps.snapshot, stringResource(R.string.jarnfe_apps_loading)) {
        JarnAppsDrawer(it, commands)
    }
}

@Composable
private fun JarnAppsEmpty() {
    JarnEmptyState(
        title = stringResource(R.string.jarnfe_apps_empty_title),
        message = stringResource(R.string.jarnfe_apps_empty_message),
    )
}

@Composable
internal fun JarnAppsDrawer(snapshot: JarnAppsSnapshot, commands: JarnAppsCommands) {
    val sections = remember(snapshot) { jarnAppSections(snapshot) }
    if (sections.isEmpty) {
        JarnAppsEmpty()
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val margin = JarnTheme.space.deckMargin
        val columns = jarnAppColumns((maxWidth - margin * 2).value)
        JarnAppsContent(snapshot, sections, columns, commands)
    }
}

@Composable
private fun JarnAppsContent(
    snapshot: JarnAppsSnapshot,
    sections: JarnAppSections,
    columns: Int,
    commands: JarnAppsCommands,
) {
    var expandedState by remember { mutableStateOf(false) }
    val expanded = expandedState && sections.hidden.isNotEmpty()
    var menuKey by remember { mutableStateOf<String?>(null) }
    var askKey by remember { mutableStateOf<String?>(null) }
    val sheetOpen = menuKey != null || askKey != null
    val focus = remember { JarnAppsFocus() }
    val listState = rememberLazyListState()
    val rows = remember(sections, expanded, columns) { jarnAppRows(sections, expanded, columns) }
    val order = remember(rows) { rows.focusOrder() }
    val focusedAtChange = remember(order) { focus.lastKey.takeIf { focus.listHasFocus } }
    val orderAtMenu = remember(menuKey) { order }
    val menuApp = menuKey?.let { key -> snapshot.apps.firstOrNull { it.packageName == key } }
    val router = LocalJarnInputRouter.current
    val isActive = router.activeScreen == LocalJarnScreen.current

    LaunchedEffect(order) { focus.onOrderChanged(order, focusedAtChange) }
    LaunchedEffect(menuKey, menuApp == null) {
        val gone = menuKey?.takeIf { menuApp == null } ?: return@LaunchedEffect
        menuKey = null
        focus.pending = jarnAppsFocusAfter(orderAtMenu, order, gone)
    }
    val tabsFocus = LocalJarnTabsFocus.current
    LaunchedEffect(isActive, order.isEmpty(), tabsFocus.handoffs) {
        if (isActive && !focus.listHasFocus && !sheetOpen) {
            focus.pending = focus.lastKey?.takeIf { it in order } ?: order.firstOrNull()
        }
    }
    LaunchedEffect(focus.pending, rows, sheetOpen, isActive) {
        val target = focus.pending ?: return@LaunchedEffect
        if (sheetOpen || !isActive) return@LaunchedEffect
        if (tabsFocus.hasFocus) {
            focus.pending = null
            return@LaunchedEffect
        }
        if (listState.revealRow(rows.rowIndexOf(target))) {
            runCatching { focus.requester(target).requestFocus() }
            focus.pending = null
        }
    }

    JarnAppsInput(
        focused = focus.current.takeIf { !sheetOpen },
        expanded = expanded,
        hiddenPackages = remember(sections) { sections.hidden.mapTo(HashSet()) { it.packageName } },
        onContext = { menuKey = it },
        onCollapse = {
            expandedState = false
            focus.pending = JARN_APPS_HIDDEN_KEY
        },
    )
    JarnAppsList(
        rows = rows,
        columns = columns,
        listState = listState,
        focus = focus,
        onOpen = { packageName -> commands.launchOrAsk(packageName) { askKey = it } },
        onToggleHidden = { expandedState = !expanded },
        onOptions = { menuKey = it },
    )
    menuApp?.let { app ->
        val moveFocusAndClose = {
            focus.pending = jarnAppsFocusAfter(order, order - app.packageName, app.packageName) ?: app.packageName
            menuKey = null
        }
        val closeOnApp = {
            focus.pending = app.packageName
            menuKey = null
        }
        JarnAppOptionsFlow(
            data = commands.options,
            target = JarnAppOptionsTarget(app.packageName, app.label, app.iconPath != null),
            extras = emptyList(),
            onClose = closeOnApp,
            onLeave = closeOnApp,
            onListChange = { moveFocusAndClose() },
        )
    }
    askKey?.let { packageName ->
        JarnAppDisplayAsk(packageName, commands) {
            focus.pending = packageName
            askKey = null
        }
    }
}

private suspend fun LazyListState.revealRow(index: Int): Boolean {
    if (index < 0) return false
    if (layoutInfo.visibleItemsInfo.none { it.index == index }) scrollToItem(index)
    snapshotFlow { layoutInfo.visibleItemsInfo.any { it.index == index } }.first { it }
    withFrameNanos { }
    return true
}

@Composable
private fun JarnAppsInput(
    focused: String?,
    expanded: Boolean,
    hiddenPackages: Set<String>,
    onContext: (String) -> Unit,
    onCollapse: () -> Unit,
) {
    JarnInputLayer(
        enabled = focused != null,
        bindings = {
            when (focused) {
                null -> Unit
                JARN_APPS_HIDDEN_KEY -> hint(
                    JarnAction.Confirm,
                    if (expanded) R.string.jarnfe_apps_hint_collapse else R.string.jarnfe_apps_hint_expand,
                )
                else -> {
                    hint(JarnAction.Confirm, R.string.jarnfe_hint_open)
                    on(JarnAction.Context, R.string.jarnfe_hint_context) { onContext(focused) }
                    if (expanded && focused in hiddenPackages) {
                        on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onCollapse)
                    }
                }
            }
        },
    )
}

@Composable
private fun JarnAppsList(
    rows: List<JarnAppsRow>,
    columns: Int,
    listState: LazyListState,
    focus: JarnAppsFocus,
    onOpen: (String) -> Unit,
    onToggleHidden: () -> Unit,
    onOptions: (String) -> Unit,
) {
    val labelSize = rememberJarnLauncherPrefs().appLabelSize
    val namesVisible = rememberJarnDisplay().appNamesVisible
    JarnRevealLazyItems(listState) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag(JARN_APPS_LIST_TAG)
                .onFocusChanged { focus.onListFocus(it.hasFocus) },
            contentPadding = PaddingValues(
                horizontal = JarnTheme.space.deckMargin,
                vertical = JarnTheme.space.focusEdge,
            ),
            verticalArrangement = Arrangement.spacedBy(AppTileRowGap.dp),
        ) {
            items(rows, key = { it.key }, contentType = { it::class }) { row ->
                when (row) {
                    is JarnAppsRow.Apps ->
                        JarnAppsRowContent(row, columns, labelSize, namesVisible, focus, onOpen, onOptions)
                    is JarnAppsRow.HiddenHeader -> JarnHiddenHeader(row, focus, onToggleHidden)
                }
            }
        }
    }
}

@Composable
private fun JarnAppsRowContent(
    row: JarnAppsRow.Apps,
    columns: Int,
    labelSize: Int,
    namesVisible: Boolean,
    focus: JarnAppsFocus,
    onOpen: (String) -> Unit,
    onOptions: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppTileRowGap.dp)) {
        if (row.showsTitle) JarnAppsSectionTitle(row.section)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppTileColumnGap.dp)) {
            row.apps.forEach { app ->
                key(app.packageName) {
                    JarnAppTile(
                        app = app,
                        labelSize = labelSize,
                        nameVisible = namesVisible,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focus.requester(app.packageName))
                            .onFocusChanged { if (it.isFocused) focus.onFocused(app.packageName) },
                        onClick = { onOpen(app.packageName) },
                        onLongClick = { onOptions(app.packageName) },
                    )
                }
            }
            EmptyCells(columns - row.apps.size)
        }
    }
}

@Composable
private fun RowScope.EmptyCells(count: Int) {
    repeat(count.coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
}

@Composable
private fun JarnAppsSectionTitle(section: JarnAppsSection) {
    val title = when (section) {
        JarnAppsSection.Pinned -> R.string.jarnfe_apps_section_pinned
        JarnAppsSection.All -> R.string.jarnfe_apps_section_all
        JarnAppsSection.Hidden -> R.string.jarnfe_apps_section_hidden
    }
    JarnText(stringResource(title), JarnTheme.type.label, JarnTheme.colors.textMid, maxLines = 1)
}

/**
 * The focus look sits inside the clickable, so D-pad search and scrolling use the tile's slot.
 * Applied outside, the floating tile would look like it sits above its row neighbors.
 */
@Composable
internal fun JarnAppTile(
    app: JarnDockApp,
    labelSize: Int,
    nameVisible: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val shape = JarnTheme.radius.mdShape
    val iconSize = jarnAppIconSize(AppIconSize, nameVisible)
    val source = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .testTag(app.packageName)
            .semantics { contentDescription = app.label }
            .jarnClickable(source, onLongClick = onLongClick, onClick = onClick)
            .jarnFocus(shape, source, style = JarnFocusStyle.FloatBorder)
            .background(JarnTheme.colors.raised, shape)
            .padding(JarnTheme.space.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        Box(Modifier.size(iconSize), contentAlignment = Alignment.Center) {
            JarnAppIcon(app.icon, app.iconPath, app.iconVersion, Modifier.size(iconSize))
        }
        if (nameVisible) JarnAppNameLabel(app.label, labelSize)
    }
}

@Composable
private fun JarnHiddenHeader(row: JarnAppsRow.HiddenHeader, focus: JarnAppsFocus, onToggle: () -> Unit) {
    JarnValueRow(
        label = stringResource(R.string.jarnfe_apps_section_hidden),
        value = row.count.toString(),
        supporting = stringResource(
            if (row.expanded) R.string.jarnfe_apps_expanded else R.string.jarnfe_apps_collapsed,
        ),
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(JARN_APPS_HIDDEN_KEY)
            .focusRequester(focus.requester(JARN_APPS_HIDDEN_KEY))
            .onFocusChanged { if (it.isFocused) focus.onFocused(JARN_APPS_HIDDEN_KEY) },
    )
}

internal const val JARN_APPS_LIST_TAG = "jarnfe-apps-list"
