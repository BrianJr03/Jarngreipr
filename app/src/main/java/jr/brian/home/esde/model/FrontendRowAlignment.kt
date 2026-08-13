package jr.brian.home.esde.model

enum class FrontendRowAlignment { Top, Center, Bottom }

const val FRONTEND_TILE_SCALE_MIN = 1.0f
const val FRONTEND_TILE_SCALE_MAX = 1.3f
const val FRONTEND_TILE_SCALE_STEP = 0.05f

const val FRONTEND_GRID_COLUMNS_DEFAULT = 4
private const val FRONTEND_GRID_COLUMNS_LARGE = 3

/**
 * Scale ≥ this threshold drops the grid to fewer columns. Kept in one place so
 * the two grid call sites stay in sync.
 */
private const val LARGE_TILE_SCALE_THRESHOLD = 1.15f

fun gridColumnsForScale(scale: Float): Int =
    if (scale >= LARGE_TILE_SCALE_THRESHOLD) FRONTEND_GRID_COLUMNS_LARGE
    else FRONTEND_GRID_COLUMNS_DEFAULT
