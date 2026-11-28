package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.model.theme.ViewSpec
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

internal fun ThemeEditorState.withViewSpec(transform: (ViewSpec) -> ViewSpec): ThemeEditorState =
    copy(spec = spec.withView(view, transform))

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

internal fun ThemePrimary.withBox(box: ThemeRect): ThemePrimary = when (this) {
    is ThemePrimary.Shelf -> copy(box = box)
    is ThemePrimary.Grid -> copy(box = box)
    is ThemePrimary.List -> copy(box = box)
}

internal fun ThemeLayer.withRect(rect: ThemeRect): ThemeLayer = when (this) {
    is ThemeLayer.Picture -> copy(rect = rect)
    is ThemeLayer.Text -> copy(rect = rect)
    is ThemeLayer.Rating -> copy(rect = rect)
    is ThemeLayer.Date -> copy(rect = rect)
    is ThemeLayer.Panel -> copy(rect = rect)
    is ThemeLayer.Video -> copy(rect = rect)
}
