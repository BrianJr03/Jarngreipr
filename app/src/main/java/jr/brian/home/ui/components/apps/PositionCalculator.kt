package jr.brian.home.ui.components.apps

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import jr.brian.home.model.ItemRect
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import jr.brian.home.model.getAllItemRects
import jr.brian.home.util.findNonOverlappingPosition
import kotlin.math.max

/**
 * Handles position calculations for apps in a free-positioned layout.
 * Calculates default grid positions and ensures no overlaps with existing items.
 */
class PositionCalculator(
    private val density: Density,
    private val borderPadding: Float
) {
    companion object {
        private const val GRID_COLUMNS = 4
        private val ITEM_WIDTH = 80.dp
        private val ITEM_HEIGHT = 100.dp
        private val HORIZONTAL_SPACING = 32.dp
        private val VERTICAL_SPACING = 24.dp
        private val DEFAULT_PADDING = 8.dp
    }

    /**
     * Calculate the default position for an app in a grid layout,
     * ensuring it doesn't overlap with existing positioned items.
     *
     * @param index The index of the app in the list
     * @param iconSizePx The size of the app icon in pixels
     * @param existingItems Rectangles of all existing positioned items
     * @param containerWidth Width of the container
     * @param contentHeight Height of the content area
     * @return Pair of (x, y) coordinates for the app position
     */
    fun calculateDefaultPosition(
        index: Int,
        iconSizePx: Float,
        existingItems: List<ItemRect>,
        containerWidth: Float,
        contentHeight: Float
    ): Pair<Float, Float> {
        // Calculate default grid position
        val defaultX = with(density) {
            val column = index % GRID_COLUMNS
            val startPadding = max(DEFAULT_PADDING.toPx(), borderPadding)
            (startPadding + column * (ITEM_WIDTH.toPx() + HORIZONTAL_SPACING.toPx()))
        }

        val defaultY = with(density) {
            val row = index / GRID_COLUMNS
            val topPadding = max(DEFAULT_PADDING.toPx(), borderPadding)
            (topPadding + row * (ITEM_HEIGHT.toPx() + VERTICAL_SPACING.toPx()))
        }

        // Check if default position overlaps with existing positioned items
        return findNonOverlappingPosition(
            targetX = defaultX,
            targetY = defaultY,
            targetSize = iconSizePx,
            excludeId = "", // New app has no ID yet
            allItems = existingItems,
            containerWidth = containerWidth.takeIf { it > 0 } ?: 1080f,
            containerHeight = contentHeight.takeIf { it > 0 } ?: 1920f,
            borderPadding = borderPadding
        )
    }

    /**
     * Calculate the default Y position for an app based on its index.
     * Used for height calculations before full position is determined.
     */
    fun calculateDefaultY(index: Int): Float {
        return with(density) {
            val row = index / GRID_COLUMNS
            val topPadding = max(DEFAULT_PADDING.toPx(), borderPadding)
            (topPadding + row * (ITEM_HEIGHT.toPx() + VERTICAL_SPACING.toPx()))
        }
    }

    /**
     * Get rectangles for all existing positioned items (apps with saved positions + folders).
     */
    fun getExistingPositionedItems(
        apps: List<AppInfo>,
        positions: Map<String, AppPosition>,
        folders: List<Folder>
    ): List<ItemRect> {
        return getAllItemRects(
            apps = apps.filter { positions[it.packageName] != null },
            positions = positions,
            folders = folders,
            density = density
        )
    }
}
