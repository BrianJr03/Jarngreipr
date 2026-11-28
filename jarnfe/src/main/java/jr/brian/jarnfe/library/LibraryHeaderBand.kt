package jr.brian.jarnfe.library

import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeViews
import jr.brian.core.model.theme.ViewSpec

/** Where a list starts when it moves out of the header band, with the small gap templates keep. */
internal const val LIBRARY_BELOW_HEADER = LIBRARY_HEADER_BAND + 0.01f

/**
 * This view with its list starting below [LIBRARY_HEADER_BAND], so the Continue playing strip
 * that spec-driven themes draw there never sits over the first row. A list already below the band
 * is unchanged; one reaching into it keeps its bottom edge and gives up the top.
 */
internal fun ViewSpec.clearingHeaderBand(): ViewSpec {
    val box = primary.box
    val top = box.y - box.originY * box.height
    if (top >= LIBRARY_HEADER_BAND) return this
    val bottom = (top + box.height).coerceAtLeast(LIBRARY_BELOW_HEADER + MIN_LIST_HEIGHT)
    val height = bottom - LIBRARY_BELOW_HEADER
    val cleared = ThemeRect(
        x = box.x,
        y = LIBRARY_BELOW_HEADER + box.originY * height,
        width = box.width,
        height = height,
        originX = box.originX,
        originY = box.originY,
    )
    return copy(primary = primary.withBox(cleared))
}

/**
 * A copy of a built-in (whose list holds its header itself) made ready to stand on its own: its
 * systems list starts below the band, like the templates, so the strip it gets has room.
 */
internal fun JarnThemeSpec.withSystemsBelowHeader(): JarnThemeSpec =
    copy(views = ThemeViews(systems = views.systems?.clearingHeaderBand(), games = views.games))

/** The same list in a different box. */
internal fun ThemePrimary.withBox(box: ThemeRect): ThemePrimary = when (this) {
    is ThemePrimary.Shelf -> copy(box = box)
    is ThemePrimary.Grid -> copy(box = box)
    is ThemePrimary.List -> copy(box = box)
}

/** The shortest a list gets when it moves out of the band. */
private const val MIN_LIST_HEIGHT = 0.2f
