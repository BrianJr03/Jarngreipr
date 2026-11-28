package jr.brian.jarnfe.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemePrimaryPatch
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeViewPatch
import jr.brian.core.model.theme.ThemeVisibleWhen
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.JarnLibrary

private val ItemsRange = 0..6
private val ColumnsRange = 1..12
private val RowsRange = 2..20
private val GrowRange = 1f..2f
private val AspectRange = 0.5f..2f
private val SpacingRange = 0f..0.1f

/** Everything about the selected piece: where it sits, then what it is and how it looks. */
@Composable
internal fun ThemeEditorInspectTab(editor: ThemeEditorState) {
    val target = editor.selected
    val rect = editor.rectOf(target)
    if (rect == null) {
        JarnText(stringResource(R.string.jarnfe_editor_nothing_selected), JarnTheme.type.body, JarnTheme.colors.textMid)
        return
    }
    JarnText(editor.targetLabel(target), JarnTheme.type.heading, JarnTheme.colors.textHigh, maxLines = 1)
    if (editor.narrow) NarrowEditRows(editor, target)
    PlacementRows(rect) { next -> JarnThemeEditor.edit { withSelectedRect(next) } }
    when (target) {
        ThemeEditorTarget.Navigator -> editor.viewSpec?.primary?.let { NavigatorRows(it) }
        is ThemeEditorTarget.Layer -> editor.layer(target.id)?.let { LayerRows(it) }
    }
}

@Composable
private fun PlacementRows(rect: ThemeRect, onChange: (ThemeRect) -> Unit) {
    val edges = rect.edges
    InspectFractionRow(R.string.jarnfe_editor_x, edges.left) { onChange(rect.withEdges(edges.copy(left = it))) }
    InspectFractionRow(R.string.jarnfe_editor_y, edges.top) { onChange(rect.withEdges(edges.copy(top = it))) }
    InspectFractionRow(R.string.jarnfe_editor_width, edges.width, MIN_PIECE_SIZE..1f) { onChange(rect.withEdges(edges.copy(width = it))) }
    InspectFractionRow(R.string.jarnfe_editor_height, edges.height, MIN_PIECE_SIZE..1f) { onChange(rect.withEdges(edges.copy(height = it))) }
}

@Composable
private fun NavigatorRows(primary: ThemePrimary) {
    InspectStepRow(R.string.jarnfe_editor_list_type, primary.typeRes) { JarnThemeEditor.edit { withPrimary { it.nextType() } } }
    when (primary) {
        is ThemePrimary.Shelf -> ShelfRows(primary)
        is ThemePrimary.Grid -> GridRows(primary)
        is ThemePrimary.List -> InspectCountRow(R.string.jarnfe_editor_rows, primary.rows, RowsRange) { rows ->
            JarnThemeEditor.edit { withPrimary { (it as? ThemePrimary.List)?.copy(rows = rows) ?: it } }
        }
    }
    if (!(JarnThemeEditor.state?.narrow ?: false)) NarrowRows()
}

/** While the narrow canvas is on: whether this piece differs there, and putting it back to wide. */
@Composable
private fun NarrowEditRows(editor: ThemeEditorState, target: ThemeEditorTarget) {
    if (editor.differsOnNarrow(target)) {
        JarnText(stringResource(R.string.jarnfe_editor_narrow_only), JarnTheme.type.captionStrong, JarnTheme.colors.accent, maxLines = 1)
        InspectActionRow(stringResource(R.string.jarnfe_editor_narrow_reset_piece), stringResource(R.string.jarnfe_editor_narrow_reset_piece_hint)) {
            JarnThemeEditor.edit { withNarrowReset(target) }
        }
    }
    if (editor.wideViewSpec?.narrow != null) {
        InspectActionRow(stringResource(R.string.jarnfe_editor_narrow_reset_view), stringResource(R.string.jarnfe_editor_narrow_reset_view_hint)) {
            JarnThemeEditor.edit { withNarrowCleared() }
        }
    }
}

@Composable
private fun ShelfRows(shelf: ThemePrimary.Shelf) {
    fun edit(transform: (ThemePrimary.Shelf) -> ThemePrimary.Shelf) =
        JarnThemeEditor.edit { withPrimary { (it as? ThemePrimary.Shelf)?.let(transform) ?: it } }
    InspectCountRow(R.string.jarnfe_editor_before, shelf.itemsBefore, ItemsRange) { n -> edit { it.copy(itemsBefore = n) } }
    InspectCountRow(R.string.jarnfe_editor_after, shelf.itemsAfter, ItemsRange) { n -> edit { it.copy(itemsAfter = n) } }
    InspectFractionRow(R.string.jarnfe_editor_focus_grow, shelf.focusedScale, GrowRange) { s -> edit { it.copy(focusedScale = s) } }
    InspectToggleRow(R.string.jarnfe_editor_show_names, shelf.showTitle) { on -> edit { it.copy(showTitle = on) } }
}

@Composable
private fun GridRows(grid: ThemePrimary.Grid) {
    fun edit(transform: (ThemePrimary.Grid) -> ThemePrimary.Grid) =
        JarnThemeEditor.edit { withPrimary { (it as? ThemePrimary.Grid)?.let(transform) ?: it } }
    InspectCountRow(R.string.jarnfe_editor_columns, grid.columns, ColumnsRange) { n -> edit { it.copy(columns = n) } }
    InspectFractionRow(R.string.jarnfe_editor_item_shape, grid.itemAspect, AspectRange) { a -> edit { it.copy(itemAspect = a) } }
    InspectFractionRow(R.string.jarnfe_editor_spacing, grid.spacing, SpacingRange) { s -> edit { it.copy(spacing = s) } }
    InspectToggleRow(R.string.jarnfe_editor_show_names, grid.showTitle) { on -> edit { it.copy(showTitle = on) } }
}

/** The narrow screen's list counts, stored as the view's narrow patch. */
@Composable
private fun NarrowRows() {
    val editor = JarnThemeEditor.state ?: return
    val view = editor.viewSpec ?: return
    val patch = view.narrow
    InspectToggleRow(R.string.jarnfe_editor_narrow, patch != null, stringResource(R.string.jarnfe_editor_narrow_hint)) { on ->
        JarnThemeEditor.edit { withViewSpec { it.copy(narrow = if (on) ThemeViewPatch(primary = ThemePrimaryPatch()) else null) } }
    }
    val primary = patch?.primary ?: return
    fun edit(transform: (ThemePrimaryPatch) -> ThemePrimaryPatch) = JarnThemeEditor.edit {
        withViewSpec { it.copy(narrow = it.narrow?.copy(primary = transform(it.narrow?.primary ?: ThemePrimaryPatch()))) }
    }
    when (val main = view.primary) {
        is ThemePrimary.Shelf -> {
            InspectCountRow(R.string.jarnfe_editor_before, primary.itemsBefore ?: main.itemsBefore, ItemsRange) { n -> edit { it.copy(itemsBefore = n) } }
            InspectCountRow(R.string.jarnfe_editor_after, primary.itemsAfter ?: main.itemsAfter, ItemsRange) { n -> edit { it.copy(itemsAfter = n) } }
        }
        is ThemePrimary.Grid ->
            InspectCountRow(R.string.jarnfe_editor_columns, primary.columns ?: main.columns, ColumnsRange) { n -> edit { it.copy(columns = n) } }
        is ThemePrimary.List ->
            InspectCountRow(R.string.jarnfe_editor_rows, primary.rows ?: main.rows, RowsRange) { n -> edit { it.copy(rows = n) } }
    }
}

@Composable
private fun LayerRows(layer: ThemeLayer) {
    InspectFractionRow(R.string.jarnfe_systems_color_opacity, layer.opacity) { o -> JarnThemeEditor.edit { withLayer(layer.id) { it.withOpacity(o) } } }
    LayerKindRows(layer)
    ThisSystemRow(layer)
    LayerOptionRows(layer.id)
}

/** Limits the piece to the system the library has focused, which is the one the canvas shows. */
@Composable
private fun ThisSystemRow(layer: ThemeLayer) {
    val system = JarnLibrary.state.focusedSystem ?: return
    val only = layer.visibleWhen?.systems == listOf(system.id)
    InspectToggleRow(
        labelRes = R.string.jarnfe_editor_this_system,
        checked = only,
        supporting = stringResource(R.string.jarnfe_editor_this_system_hint),
        label = stringResource(R.string.jarnfe_editor_this_system, system.title),
    ) { on ->
        val rule = (layer.visibleWhen ?: ThemeVisibleWhen()).copy(systems = if (on) listOf(system.id) else emptyList())
        JarnThemeEditor.edit { withLayer(layer.id) { it.withVisibleWhen(rule.takeUnless { r -> r == ThemeVisibleWhen() }) } }
    }
}

/** Duplicate, delete and order, here and in the canvas's Select sheet. */
@Composable
internal fun LayerOptionRows(id: String, onDone: () -> Unit = {}) {
    InspectActionRow(stringResource(R.string.jarnfe_editor_duplicate)) { JarnThemeEditor.edit { withDuplicate(id) }; onDone() }
    InspectActionRow(stringResource(R.string.jarnfe_editor_forward)) { JarnThemeEditor.edit { withLayerOrder(id, 1) } }
    InspectActionRow(stringResource(R.string.jarnfe_editor_backward)) { JarnThemeEditor.edit { withLayerOrder(id, -1) } }
    InspectActionRow(stringResource(R.string.jarnfe_editor_delete)) { JarnThemeEditor.edit { withoutLayer(id) }; onDone() }
}
