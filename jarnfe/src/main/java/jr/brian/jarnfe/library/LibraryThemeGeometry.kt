package jr.brian.jarnfe.library

import androidx.compose.ui.unit.Dp
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeView

/**
 * Values a built-in theme pins where the theme format can't state today's layout exactly:
 * [shelf] replaces the shelf geometry the spec would derive, [grid] keeps adaptive columns and
 * fixed dp gaps. A theme without pins takes everything from its spec.
 */
internal data class LibraryPrimaryPins(
    val shelf: LibraryShelfGeometry? = null,
    val grid: LibraryGridPins? = null,
)

internal data class LibraryGridPins(val rowSpacing: Dp, val columnSpacing: Dp = LibraryGridColumnSpacing)

internal val LibraryShelfKind.themeView: ThemeView
    get() = when (this) {
        LibraryShelfKind.Systems -> ThemeView.Systems
        LibraryShelfKind.Games -> ThemeView.Games
    }

/**
 * The shelf geometry for a theme's shelf. Neighbor counts and captions always come from [primary];
 * the rest comes from [pinned] when set, otherwise from [kind]'s own geometry with neighbors
 * scaled to one over [ThemePrimary.Shelf.focusedScale].
 */
internal fun libraryShelfGeometry(
    primary: ThemePrimary.Shelf,
    kind: LibraryShelfKind,
    pinned: LibraryShelfGeometry? = null,
): LibraryShelfGeometry {
    val base = pinned ?: kind.geometry.copy(neighborScale = 1f / primary.focusedScale)
    return base.copy(
        neighborsBefore = primary.itemsBefore,
        neighborsAfter = primary.itemsAfter,
        showsCaption = primary.showTitle,
    )
}

/**
 * The grid geometry for a theme's grid at [cellMinSize]. With [pinned] the columns stay adaptive,
 * the gaps are its dp values and tiles keep their own shape and names. Otherwise
 * [ThemePrimary.Grid.columns] fixes the count, [ThemePrimary.Grid.spacing] of [boxWidth] sets both
 * gaps, and the item shape and names follow the spec.
 */
internal fun libraryGridGeometry(
    primary: ThemePrimary.Grid,
    cellMinSize: Dp,
    boxWidth: Dp,
    pinned: LibraryGridPins? = null,
): LibraryGridGeometry {
    if (pinned != null) return LibraryGridGeometry(cellMinSize, pinned.rowSpacing, pinned.columnSpacing)
    val gap = boxWidth * primary.spacing
    return LibraryGridGeometry(
        cellMinSize = cellMinSize,
        rowSpacing = gap,
        columnSpacing = gap,
        columns = primary.columns,
        itemAspect = primary.itemAspect.takeIf { it.isFinite() && it > 0f },
        showTitle = primary.showTitle,
    )
}
