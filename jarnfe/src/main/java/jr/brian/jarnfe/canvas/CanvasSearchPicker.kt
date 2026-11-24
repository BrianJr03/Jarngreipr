package jr.brian.jarnfe.canvas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.focus.JarnRevealLazyItems
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.search.parseJarnSearchQuery
import jr.brian.jarnfe.search.rankByTitle
import jr.brian.jarnfe.shell.JarnNotices

/** A blank query keeps the list's own order. */
internal fun <T> canvasPickerMatches(items: List<T>, query: String, title: (T) -> String): List<T> =
    if (query.isBlank()) items else rankByTitle(items, parseJarnSearchQuery(query).terms, title)

/** A lazy list under a search row. Y also opens search, and B leaves the keyboard first. */
@Composable
internal fun <T> CanvasSearchPicker(
    title: String,
    rows: List<T>?,
    rowKey: (T) -> String,
    query: String,
    onQuery: (String) -> Unit,
    onClose: () -> Unit,
    bindings: JarnBindingsBuilder.() -> Unit = {},
    note: String? = null,
    header: LazyListScope.() -> Unit = {},
    footer: @Composable () -> Unit = {},
    row: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    var searching by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val first = remember { FocusRequester() }
    LaunchedEffect(query) { listState.scrollToItem(0) }
    if (searching) {
        CanvasKeyboardSheet(
            label = stringResource(R.string.jarnfe_canvas_pick_search),
            onDone = {
                onQuery(it.trim())
                searching = false
            },
            onCancel = { searching = false },
            initialText = query,
        )
        return
    }
    CanvasModalSheet(
        title = title,
        initialFocus = first,
        onClose = onClose,
        extraBindings = {
            on(JarnAction.Search, R.string.jarnfe_hint_search) { searching = true }
            bindings()
        },
    ) {
        JarnNavigateRow(
            label = stringResource(R.string.jarnfe_canvas_pick_search),
            supporting = query.ifEmpty { null },
            onClick = { searching = true },
            modifier = Modifier.fillMaxWidth().focusRequester(first),
        )
        note?.let { JarnText(it, JarnTheme.type.body, JarnTheme.colors.textMid) }
        JarnRevealLazyItems(listState) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .heightIn(max = CanvasSheetListMaxHeight),
                contentPadding = PaddingValues(vertical = JarnTheme.space.focusEdge),
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            ) {
                header()
                if (rows.isNullOrEmpty()) {
                    item(key = CANVAS_PICKER_STATUS_KEY) { CanvasPickerStatus(rows == null, query.isNotEmpty()) }
                } else {
                    items(rows, key = rowKey) { item -> row(item, Modifier.fillMaxWidth()) }
                }
            }
        }
        footer()
    }
}

private const val CANVAS_PICKER_STATUS_KEY = "canvas-picker-status"
private const val CANVAS_PICKER_SELECT_KEY = "canvas-picker-select"
private const val CANVAS_PICKER_CLEAR_KEY = "canvas-picker-clear"

@Composable
private fun CanvasPickerStatus(loading: Boolean, searched: Boolean) {
    val message = when {
        loading -> R.string.jarnfe_canvas_pick_loading
        searched -> R.string.jarnfe_canvas_pick_no_matches
        else -> R.string.jarnfe_canvas_pick_empty
    }
    JarnText(stringResource(message), JarnTheme.type.body, JarnTheme.colors.textMid)
}

/**
 * A press of A toggles a row. X confirms the picks, or just the focused row when nothing is picked,
 * so adding one item stays a single press. [isLocked] rows show as picked and can't change.
 */
@Composable
internal fun <T> CanvasMultiPicker(
    title: String,
    rows: List<T>?,
    rowKey: (T) -> String,
    rowLabel: (T) -> String,
    selection: CanvasPickSelection<T>,
    onSelection: (CanvasPickSelection<T>) -> Unit,
    query: String,
    onQuery: (String) -> Unit,
    confirmLabel: String,
    onConfirm: (List<T>) -> Unit,
    onClose: () -> Unit,
    rowDetail: (T) -> String? = { null },
    isLocked: (T) -> Boolean = { false },
    note: String? = null,
    header: LazyListScope.() -> Unit = {},
) {
    var focused by remember { mutableStateOf<T?>(null) }
    val warnIfFull = {
        selection.capacity?.let { JarnNotices.showCount(R.plurals.jarnfe_canvas_notice_room_left, it) }
    }
    val confirm = {
        val picks = selection.picked.values.toList().ifEmpty { listOfNotNull(focused) }
        if (picks.isNotEmpty()) onConfirm(picks)
    }
    CanvasSearchPicker(
        title = title,
        rows = rows,
        rowKey = rowKey,
        query = query,
        onQuery = onQuery,
        onClose = onClose,
        note = note,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_canvas_hint_select)
            on(JarnAction.Context, R.string.jarnfe_canvas_hint_add, onPress = confirm)
        },
        header = {
            header()
            if (!rows.isNullOrEmpty()) {
                item(key = CANVAS_PICKER_SELECT_KEY) {
                    JarnNavigateRow(
                        label = stringResource(R.string.jarnfe_canvas_pick_select_shown),
                        onClick = {
                            val open = rows.filterNot(isLocked)
                            val next = selection.withAll(open, rowKey)
                            onSelection(next)
                            if (open.any { rowKey(it) !in next }) warnIfFull()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item(key = CANVAS_PICKER_CLEAR_KEY) {
                    JarnNavigateRow(
                        label = stringResource(R.string.jarnfe_canvas_pick_clear),
                        onClick = { onSelection(selection.cleared()) },
                        enabled = selection.count > 0,
                        focusableWhenDisabled = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        footer = {
            JarnButton(
                label = confirmLabel,
                onClick = confirm,
                enabled = selection.count > 0,
                focusableWhenDisabled = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) { item, modifier ->
        val key = rowKey(item)
        val locked = isLocked(item)
        JarnToggleRow(
            label = rowLabel(item),
            supporting = rowDetail(item),
            checked = locked || key in selection,
            enabled = !locked,
            focusableWhenDisabled = true,
            onCheckedChange = {
                if (key !in selection && selection.isFull) warnIfFull()
                onSelection(selection.toggled(key, item))
            },
            modifier = modifier.onFocusChanged { state ->
                when {
                    state.hasFocus -> focused = item.takeUnless { locked }
                    focused == item -> focused = null
                }
            },
        )
    }
}

@Composable
internal fun CanvasPageFullSheet(onClose: () -> Unit) {
    CanvasModalSheet(stringResource(R.string.jarnfe_canvas_page_full_title), null, onClose) {
        JarnText(
            text = stringResource(R.string.jarnfe_canvas_page_full_message),
            style = JarnTheme.type.body,
            color = JarnTheme.colors.textMid,
        )
    }
}
