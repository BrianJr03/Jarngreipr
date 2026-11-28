package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.model.theme.ViewSpec
import jr.brian.core.model.theme.narrowChanges
import jr.brian.core.model.theme.narrowView
import jr.brian.core.model.theme.withNarrowEdit
import jr.brian.core.model.theme.withoutNarrowLayer
import jr.brian.core.model.theme.withoutNarrowPrimary
import jr.brian.jarnfe.library.withBox
import kotlin.math.abs

private const val LATERAL_WEIGHT = 2f

/** [target]'s rect in the current view: the navigator's box or the layer's rect. */
internal fun ThemeEditorState.rectOf(target: ThemeEditorTarget): ThemeRect? = when (target) {
    ThemeEditorTarget.Navigator -> viewSpec?.primary?.box
    is ThemeEditorTarget.Layer -> layer(target.id)?.rect
}

/** The draft with [view] replaced by [transform] of it. */
internal fun JarnThemeSpec.withView(view: ThemeView, transform: (ViewSpec) -> ViewSpec): JarnThemeSpec {
    val current = views[view] ?: return this
    val next = transform(current)
    return copy(
        views = when (view) {
            ThemeView.Systems -> views.copy(systems = next)
            ThemeView.Games -> views.copy(games = next)
        },
    )
}

/**
 * The draft with [transform] applied to the view on the canvas. With the narrow canvas on, the
 * result is written back as the view's narrow patch and the wide layout is left as it was.
 */
internal fun ThemeEditorState.withViewSpec(transform: (ViewSpec) -> ViewSpec): ThemeEditorState =
    copy(spec = spec.withView(view) { wide -> if (narrow) wide.withNarrowEdit(transform(wide.narrowView())) else transform(wide) })

/** The written view changed directly, narrow patch included, whichever canvas is showing. */
internal fun ThemeEditorState.withWideViewSpec(transform: (ViewSpec) -> ViewSpec): ThemeEditorState =
    copy(spec = spec.withView(view, transform))

/** The narrow screen shows [target] as the wide one does again. */
internal fun ThemeEditorState.withNarrowReset(target: ThemeEditorTarget): ThemeEditorState = withWideViewSpec { view ->
    when (target) {
        ThemeEditorTarget.Navigator -> view.withoutNarrowPrimary()
        is ThemeEditorTarget.Layer -> view.withoutNarrowLayer(target.id)
    }
}.let { if (it.selected in it.targets) it else it.copy(selected = ThemeEditorTarget.Navigator) }

/** The narrow screen shows the whole view as the wide one does. */
internal fun ThemeEditorState.withNarrowCleared(): ThemeEditorState =
    withWideViewSpec { it.copy(narrow = null) }.let { if (it.selected in it.targets) it else it.copy(selected = ThemeEditorTarget.Navigator) }

/** Whether the narrow screen shows [target] differently from the wide one. */
internal fun ThemeEditorState.differsOnNarrow(target: ThemeEditorTarget): Boolean {
    val view = wideViewSpec ?: return false
    return when (target) {
        ThemeEditorTarget.Navigator -> view.narrow?.primary != null
        is ThemeEditorTarget.Layer -> view.narrowChanges(target.id)
    }
}

/** The selected piece's rect replaced. */
internal fun ThemeEditorState.withSelectedRect(rect: ThemeRect): ThemeEditorState = withViewSpec { view ->
    when (val target = selected) {
        ThemeEditorTarget.Navigator -> view.copy(primary = view.primary.withBox(rect))
        is ThemeEditorTarget.Layer -> view.copy(layers = view.layers.map { if (it.id == target.id) it.withRect(rect) else it })
    }
}

internal fun ThemeEditorState.pickedUp(): ThemeEditorState = copy(held = ThemeEditorHold(spec))

internal fun ThemeEditorState.dropped(): ThemeEditorState = copy(held = null)

internal fun ThemeEditorState.cancelled(): ThemeEditorState = copy(spec = held?.before ?: spec, held = null)

internal fun ThemeEditorState.axisToggled(): ThemeEditorState =
    copy(axis = if (axis == ThemeEditorAxis.Width) ThemeEditorAxis.Height else ThemeEditorAxis.Width)

/** Moves the selected piece [steps] nudges that way, snapping to lines when [ThemeEditorState.snap] is on. */
internal fun ThemeEditorState.nudged(dx: Int, dy: Int, steps: Int = 1): ThemeEditorState {
    val rect = rectOf(selected) ?: return this
    val edges = rect.edges
    val step = (if (snap) SNAP_STEP else FINE_STEP) * steps
    val others = otherEdges()
    val left = clampedStart(snappedMove(edges.left, edges.width, dx * step, snap, snapLines(others, horizontal = true)), edges.width)
    val top = clampedStart(snappedMove(edges.top, edges.height, dy * step, snap, snapLines(others, horizontal = false)), edges.height)
    return withSelectedRect(rect.withEdges(edges.copy(left = left, top = top)))
}

/** Grows or shrinks the selected piece along [ThemeEditorState.axis], keeping its top left corner. */
internal fun ThemeEditorState.resized(direction: Int, steps: Int = 1): ThemeEditorState {
    val rect = rectOf(selected) ?: return this
    val edges = rect.edges
    val step = (if (snap) SNAP_STEP else FINE_STEP) * steps * direction
    fun sized(size: Float, start: Float): Float {
        val next = if (snap) onGrid(size + step) else size + step
        return next.coerceIn(MIN_PIECE_SIZE, 1f - start.coerceIn(0f, 1f - MIN_PIECE_SIZE))
    }
    val next = when (axis) {
        ThemeEditorAxis.Width -> edges.copy(width = sized(edges.width, edges.left))
        ThemeEditorAxis.Height -> edges.copy(height = sized(edges.height, edges.top))
    }
    return withSelectedRect(rect.withEdges(next))
}

/**
 * The nearest piece that way from the selected one, favoring pieces in line with it. A piece under
 * the selected one (like a full screen list) is skipped; L1 and R1 still reach it.
 */
internal fun ThemeEditorState.jumped(dx: Int, dy: Int): ThemeEditorState {
    val from = rectOf(selected)?.edges ?: return this
    val next = targets.filter { it != selected }
        .mapNotNull { target -> rectOf(target)?.edges?.let { target to it } }
        .filter { (_, edges) -> (edges.centerX - from.centerX) * dx + (edges.centerY - from.centerY) * dy > 0 }
        .filterNot { (_, edges) -> edges.contains(from.centerX, from.centerY) }
        .minByOrNull { (_, edges) ->
            val ahead = abs(if (dx != 0) edges.centerX - from.centerX else edges.centerY - from.centerY)
            val aside = abs(if (dx != 0) edges.centerY - from.centerY else edges.centerX - from.centerX)
            ahead + aside * LATERAL_WEIGHT
        }
    return next?.let { copy(selected = it.first) } ?: this
}

private fun ThemeEditorState.otherEdges(): List<PieceEdges> =
    targets.filter { it != selected }.mapNotNull { rectOf(it)?.edges }

internal fun ThemeLayer.withRect(rect: ThemeRect): ThemeLayer = when (this) {
    is ThemeLayer.Picture -> copy(rect = rect)
    is ThemeLayer.Text -> copy(rect = rect)
    is ThemeLayer.Rating -> copy(rect = rect)
    is ThemeLayer.Date -> copy(rect = rect)
    is ThemeLayer.Panel -> copy(rect = rect)
    is ThemeLayer.Video -> copy(rect = rect)
}
