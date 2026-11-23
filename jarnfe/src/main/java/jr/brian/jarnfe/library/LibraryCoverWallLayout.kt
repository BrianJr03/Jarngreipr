package jr.brian.jarnfe.library

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val LIBRARY_WALL_MIN_COVERS = 3
internal const val LIBRARY_WALL_MAX_COVERS = 12
internal const val LIBRARY_WALL_MAX_EDGE_PX = 1920
internal const val LIBRARY_WALL_ROTATION_DEGREES = -7.45f

private const val DESIGN_WIDTH = 860f
private const val DESIGN_HEIGHT = 620f
private const val DESIGN_CELL_WIDTH = 150f
private const val DESIGN_CELL_HEIGHT = 200f
private const val DESIGN_GAP = 10f
private const val DESIGN_ROW_STAGGER = 70f
private const val COVER_INDEX_ROW_STEP = 2

/** A null [path] draws as a title card. */
internal data class LibraryWallCover(val path: String?, val title: String)

internal data class LibraryWallSize(val width: Int, val height: Int)

internal data class LibraryCoverWallPalette(
    val base: Int,
    val card: Int,
    val text: Int,
    val overlay: Int,
)

internal data class LibraryCoverWallKey(
    val systemId: String,
    val covers: List<LibraryWallCover>,
    val size: LibraryWallSize,
    val palette: LibraryCoverWallPalette,
)

internal data class LibraryWallCell(val centerX: Float, val centerY: Float, val coverIndex: Int)

/** Cell centers are relative to the wall's center, inside its rotated frame. */
internal data class LibraryCoverWallLayout(
    val cellWidth: Float,
    val cellHeight: Float,
    val gap: Float,
    val rotationDegrees: Float,
    val cells: List<LibraryWallCell>,
)

/**
 * Real covers come first so title cards only fill spare slots. Empty when the system has too few
 * real covers for a wall, which is the caller's cue to show the wallpaper.
 */
internal fun libraryCoverWallSelection(covers: List<LibraryWallCover>): List<LibraryWallCover> {
    val unique = covers
        .map { cover -> cover.copy(path = cover.path?.takeUnless(String::isBlank)) }
        .distinctBy { cover -> cover.path to cover.title.takeIf { cover.path == null } }
    val (withArt, withoutArt) = unique.partition { it.path != null }
    if (withArt.size < LIBRARY_WALL_MIN_COVERS) return emptyList()
    return (withArt + withoutArt).take(LIBRARY_WALL_MAX_COVERS)
}

internal fun libraryCoverWallBitmapSize(width: Int, height: Int): LibraryWallSize? {
    if (width <= 0 || height <= 0) return null
    val scale = min(1f, LIBRARY_WALL_MAX_EDGE_PX.toFloat() / max(width, height))
    return LibraryWallSize(
        width = (width * scale).roundToInt().coerceAtLeast(1),
        height = (height * scale).roundToInt().coerceAtLeast(1),
    )
}

internal fun libraryCoverWallKey(
    systemId: String,
    selection: List<LibraryWallCover>,
    displayWidth: Int,
    displayHeight: Int,
    palette: LibraryCoverWallPalette,
): LibraryCoverWallKey? {
    if (selection.size < LIBRARY_WALL_MIN_COVERS) return null
    val size = libraryCoverWallBitmapSize(displayWidth, displayHeight) ?: return null
    return LibraryCoverWallKey(systemId, selection, size, palette)
}

/** Only cells that can touch the rotated viewport are returned. */
internal fun libraryCoverWallLayout(size: LibraryWallSize, coverCount: Int): LibraryCoverWallLayout {
    val scale = min(size.width / DESIGN_WIDTH, size.height / DESIGN_HEIGHT)
    val cellWidth = DESIGN_CELL_WIDTH * scale
    val cellHeight = DESIGN_CELL_HEIGHT * scale
    val gap = DESIGN_GAP * scale
    val stagger = DESIGN_ROW_STAGGER * scale
    val radians = Math.toRadians(LIBRARY_WALL_ROTATION_DEGREES.toDouble())
    val cosine = abs(cos(radians)).toFloat()
    val sine = abs(sin(radians)).toFloat()
    val reachX = size.width / 2f * cosine + size.height / 2f * sine
    val reachY = size.width / 2f * sine + size.height / 2f * cosine
    val pitchX = cellWidth + gap
    val pitchY = cellHeight + gap
    val columns = ceil((reachX + stagger + cellWidth / 2f) / pitchX).toInt()
    val rows = ceil((reachY + cellHeight / 2f) / pitchY).toInt()
    val cells = if (coverCount <= 0) emptyList() else buildList {
        for (row in -rows..rows) {
            val rowOffset = if (row % 2 != 0) stagger else 0f
            for (column in -columns..columns) {
                val centerX = column * pitchX + rowOffset
                val centerY = row * pitchY
                val touchesViewport = abs(centerX) - pitchX / 2f < reachX &&
                    abs(centerY) - pitchY / 2f < reachY
                if (touchesViewport) {
                    add(LibraryWallCell(centerX, centerY, libraryWallCoverIndex(column, row, coverCount)))
                }
            }
        }
    }
    return LibraryCoverWallLayout(cellWidth, cellHeight, gap, LIBRARY_WALL_ROTATION_DEGREES, cells)
}

/**
 * Rows are staggered, so each cell touches six others. Axial coordinates with a row step of 2 keep
 * all six on a different cover whenever there are at least three covers.
 */
internal fun libraryWallCoverIndex(column: Int, row: Int, coverCount: Int): Int {
    val axialColumn = column - Math.floorDiv(row, 2)
    return Math.floorMod(axialColumn + COVER_INDEX_ROW_STEP * row, coverCount)
}
