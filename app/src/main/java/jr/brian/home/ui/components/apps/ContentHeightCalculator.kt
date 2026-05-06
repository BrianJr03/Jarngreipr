package jr.brian.home.ui.components.apps

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import jr.brian.home.model.rom.PinnedRomInfo
import kotlin.math.max

/**
 * Calculates the content height needed for a free-positioned apps layout,
 * ensuring all apps and folders fit within the scrollable area.
 */
object ContentHeightCalculator {
    /**
     * Calculates the required content height based on app and folder positions.
     *
     * @param apps The list of apps to display
     * @param folders The list of folders to display
     * @param positions Map of saved app positions
     * @param containerSize The size of the container
     * @param density Density for dp/px conversions
     * @param positionCalculator Calculator for default positions
     * @return The calculated content height in pixels
     */
    fun calculateContentHeight(
        apps: List<AppInfo>,
        folders: List<Folder>,
        positions: Map<String, AppPosition>,
        containerSize: IntSize,
        density: Density,
        positionCalculator: PositionCalculator,
        pinnedRoms: List<PinnedRomInfo> = emptyList()
    ): Float {
        var calculatedMaxY = 0f

        // Calculate max Y from folders
        folders.forEach { folder ->
            val position = folder.position
            val iconSizePx = with(density) { position.iconSize.dp.toPx() }
            val bottom = position.y + iconSizePx
            if (bottom > calculatedMaxY) calculatedMaxY = bottom
        }

        // Calculate max Y from apps
        apps.forEachIndexed { index, app ->
            val position = positions[app.packageName]
            val defaultY = positionCalculator.calculateDefaultY(index)
            val iconSize = position?.iconSize ?: 64f
            val iconSizePx = with(density) { iconSize.dp.toPx() }
            val y = position?.y ?: defaultY
            val bottom = y + iconSizePx
            if (bottom > calculatedMaxY) calculatedMaxY = bottom
        }

        // Calculate max Y from pinned ROMs
        pinnedRoms.forEach { rom ->
            val position = positions[rom.key]
            val iconSize = position?.iconSize ?: ROM_DEFAULT_ICON_SIZE
            val iconSizePx = with(density) { iconSize.dp.toPx() }
            val y = position?.y ?: 0f
            val bottom = y + iconSizePx
            if (bottom > calculatedMaxY) calculatedMaxY = bottom
        }

        // Add bottom padding and ensure it's at least as tall as the container
        return with(density) {
            val bottomPadding = 8.dp.toPx()
            max(
                containerSize.height.toFloat(),
                calculatedMaxY + bottomPadding
            )
        }
    }
}
