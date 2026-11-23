package jr.brian.jarnfe.canvas

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnDialog
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import kotlinx.coroutines.launch

private val MovingRowOutline = 2.dp

/** A page being moved. [order] holds old page indices, and nothing is saved until the drop. */
internal data class CanvasPageMove(val origin: Int, val order: List<Int>) {
    val position: Int get() = order.indexOf(origin)

    fun stepped(delta: Int): CanvasPageMove {
        val target = (position + delta).coerceIn(order.indices)
        if (target == position) return this
        val next = order.toMutableList()
        next.add(target, next.removeAt(position))
        return copy(order = next)
    }
}

@StringRes
internal fun JarnCanvasPageKind.labelRes(): Int = when (this) {
    JarnCanvasPageKind.Canvas -> R.string.jarnfe_tab_canvas
    JarnCanvasPageKind.Apps -> R.string.jarnfe_canvas_kind_apps
    JarnCanvasPageKind.AppsAndWidgets -> R.string.jarnfe_canvas_kind_apps_widgets
    JarnCanvasPageKind.AppDrawer -> R.string.jarnfe_canvas_kind_drawer
    JarnCanvasPageKind.Rss -> R.string.jarnfe_canvas_kind_rss
}

@Composable
internal fun CanvasPagesSheet(
    data: JarnCanvasData,
    snapshot: JarnCanvasSnapshot,
    selectedPage: Int,
    onGoTo: (Int) -> Unit,
    onSelectionMoved: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pages = snapshot.pages
    var move by remember { mutableStateOf<CanvasPageMove?>(null) }
    var pendingDelete by remember { mutableStateOf<Int?>(null) }
    var focusRow by remember { mutableStateOf<Int?>(null) }
    val rowFocus = remember { List(JARN_CANVAS_MAX_PAGES + 2) { FocusRequester() } }
    val addFocus = remember { FocusRequester() }
    val order = move?.order ?: pages.indices.toList()

    LaunchedEffect(focusRow) {
        val row = focusRow ?: return@LaunchedEffect
        withFrameNanos { }
        runCatching { rowFocus.getOrNull(row)?.requestFocus() }
        focusRow = null
    }

    if (pendingDelete == null) CanvasModalSheet(
        title = stringResource(R.string.jarnfe_canvas_pages_title),
        initialFocus = if (pages.isEmpty()) {
            addFocus
        } else {
            rowFocus[(focusRow ?: selectedPage).coerceIn(pages.indices)]
        },
        onClose = onClose,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = CanvasSheetListMaxHeight)
                .verticalScroll(rememberScrollState())
                .padding(vertical = JarnTheme.space.focusEdge),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            order.forEachIndexed { position, pageIndex ->
                val page = pages.getOrNull(pageIndex) ?: return@forEachIndexed
                key(pageIndex) {
                    CanvasPageRow(
                        page = page,
                        position = position,
                        isHome = pageIndex == snapshot.homePage,
                        isCurrent = pageIndex == selectedPage,
                        isMoving = move?.origin == pageIndex,
                        canChange = pages.size > 1,
                        focusRequester = rowFocus.getOrNull(position),
                        onGoTo = { onGoTo(pageIndex) },
                        onSetHome = { data.setHomePage(pageIndex) },
                        onMove = { move = CanvasPageMove(pageIndex, pages.indices.toList()) },
                        onDelete = { pendingDelete = pageIndex },
                    )
                }
            }
        }
        JarnButton(
            label = stringResource(
                if (pages.size < JARN_CANVAS_MAX_PAGES) R.string.jarnfe_canvas_page_add
                else R.string.jarnfe_canvas_page_limit,
            ),
            onClick = {
                scope.launch {
                    data.addCanvasPage()?.let(onSelectionMoved)
                }
            },
            modifier = Modifier.fillMaxWidth().focusRequester(addFocus),
            variant = JarnButtonVariant.Secondary,
            enabled = pages.size < JARN_CANVAS_MAX_PAGES,
            focusableWhenDisabled = true,
        )
    }

    CanvasPageMoveLayer(
        move = move,
        onStep = { delta -> move = move?.stepped(delta) },
        onCancel = {
            focusRow = move?.origin
            move = null
        },
        onDrop = {
            val finished = move ?: return@CanvasPageMoveLayer
            move = null
            val target = finished.position
            focusRow = target
            if (target != finished.origin) {
                scope.launch {
                    data.movePage(finished.origin, target)
                    onSelectionMoved(finished.order.indexOf(selectedPage).coerceAtLeast(0))
                }
            }
        },
    )

    pendingDelete?.let { index ->
        CanvasPageDeleteDialog(
            pageNumber = index + 1,
            onConfirm = {
                pendingDelete = null
                focusRow = index.coerceAtMost(pages.size - 2)
                scope.launch {
                    data.deletePage(index)
                    onSelectionMoved(jarnPageAfterDelete(selectedPage, index, pages.size))
                }
            },
            onDismiss = {
                pendingDelete = null
                focusRow = index
            },
        )
    }
}

@Composable
private fun CanvasPageMoveLayer(
    move: CanvasPageMove?,
    onStep: (Int) -> Unit,
    onCancel: () -> Unit,
    onDrop: () -> Unit,
) {
    JarnInputLayer(
        mode = JarnLayerMode.Capture,
        enabled = move != null,
        bindings = {
            on(JarnAction.MoveUp, R.string.jarnfe_hint_move) { onStep(-1) }
            on(JarnAction.MoveDown) { onStep(1) }
            on(JarnAction.Confirm, R.string.jarnfe_canvas_hint_drop, onPress = onDrop)
            on(JarnAction.Back, R.string.jarnfe_canvas_hint_cancel, onPress = onCancel)
        },
    )
}

@Composable
private fun CanvasPageDeleteDialog(pageNumber: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onDismiss)
        },
    ) {
        JarnDialog(
            visible = true,
            title = stringResource(R.string.jarnfe_canvas_delete_title, pageNumber),
            message = stringResource(R.string.jarnfe_canvas_delete_message),
            confirmLabel = stringResource(R.string.jarnfe_canvas_page_delete),
            dismissLabel = stringResource(R.string.jarnfe_canvas_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
        )
    }
}

@Composable
private fun CanvasPageRow(
    page: JarnCanvasPage,
    position: Int,
    isHome: Boolean,
    isCurrent: Boolean,
    isMoving: Boolean,
    canChange: Boolean,
    focusRequester: FocusRequester?,
    onGoTo: () -> Unit,
    onSetHome: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    val outline = if (isMoving) {
        Modifier.border(MovingRowOutline, JarnTheme.colors.accent, JarnTheme.radius.mdShape)
    } else {
        Modifier
    }
    val focus = focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
    Row(
        modifier = Modifier.fillMaxWidth().then(outline),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_canvas_page_number, position + 1),
            supporting = pageRowDetail(page, isHome, isCurrent),
            onClick = onGoTo,
            modifier = Modifier.weight(1f).then(focus),
        )
        CanvasPageAction(R.string.jarnfe_canvas_page_set_home, !isHome, JarnButtonVariant.Secondary, onSetHome)
        CanvasPageAction(R.string.jarnfe_canvas_page_move, canChange, JarnButtonVariant.Secondary, onMove)
        CanvasPageAction(R.string.jarnfe_canvas_page_delete, canChange, JarnButtonVariant.Danger, onDelete)
    }
}

@Composable
private fun pageRowDetail(page: JarnCanvasPage, isHome: Boolean, isCurrent: Boolean): String =
    listOfNotNull(
        stringResource(page.kind.labelRes()),
        stringResource(R.string.jarnfe_canvas_home).takeIf { isHome },
        stringResource(R.string.jarnfe_canvas_page_current).takeIf { isCurrent },
    ).joinToString(", ")

@Composable
private fun CanvasPageAction(
    @StringRes labelRes: Int,
    enabled: Boolean,
    variant: JarnButtonVariant,
    onClick: () -> Unit,
) {
    JarnButton(
        label = stringResource(labelRes),
        onClick = onClick,
        variant = variant,
        enabled = enabled,
        focusableWhenDisabled = true,
    )
}
