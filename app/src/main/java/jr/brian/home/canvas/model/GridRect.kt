package jr.brian.home.canvas.model

/**
 * A rectangular region of grid cells: top-left at [col]/[row], extending [colSpan]
 * cells right and [rowSpan] cells down. Edges are half-open: [right] and [bottom]
 * are exclusive.
 */
data class GridRect(
    val col: Int,
    val row: Int,
    val colSpan: Int,
    val rowSpan: Int
) {
    init {
        require(colSpan >= 1) { "colSpan must be >= 1, was $colSpan" }
        require(rowSpan >= 1) { "rowSpan must be >= 1, was $rowSpan" }
    }

    val right: Int get() = col + colSpan
    val bottom: Int get() = row + rowSpan

    fun overlaps(other: GridRect): Boolean =
        col < other.right && other.col < right &&
            row < other.bottom && other.row < bottom

    fun contains(cell: GridCell): Boolean =
        cell.col in col until right && cell.row in row until bottom

    fun withOrigin(col: Int, row: Int): GridRect = copy(col = col, row = row)

    fun withSize(colSpan: Int, rowSpan: Int): GridRect = copy(colSpan = colSpan, rowSpan = rowSpan)
}
