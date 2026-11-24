package jr.brian.jarnfe.apps

import jr.brian.core.ui.focus.JarnFocusReach
import jr.brian.core.ui.focus.jarnFocusBorderReach
import kotlin.math.ceil

internal const val APP_TILE_MIN_WIDTH = 104f

/** Where the scale first reaches its cap: growth is at its most and the border is not yet scaled down. */
private const val APP_TILE_REACH_SIDE = 150f
private const val APP_BORDER_CLEARANCE = 2f

/** The furthest a focused app tile's border can reach, in dp. */
internal val AppTileFocusReach: JarnFocusReach = jarnFocusBorderReach(APP_TILE_REACH_SIDE, APP_TILE_REACH_SIDE)

/** Spacing between tiles in a row, wide enough that a focused border clears its neighbors. */
internal val AppTileColumnGap: Float = ceil(AppTileFocusReach.sides + APP_BORDER_CLEARANCE)

/** Spacing between rows, and under a section title, wide enough for the border as the tile floats up. */
internal val AppTileRowGap: Float = ceil(maxOf(AppTileFocusReach.top, AppTileFocusReach.bottom) + APP_BORDER_CLEARANCE)

internal fun jarnAppColumns(widthDp: Float, gapDp: Float = AppTileColumnGap): Int =
    ((widthDp + gapDp) / (APP_TILE_MIN_WIDTH + gapDp)).toInt().coerceAtLeast(1)

internal fun jarnAppCellWidth(widthDp: Float, columns: Int, gapDp: Float = AppTileColumnGap): Float =
    (widthDp - gapDp * (columns - 1)) / columns
