package jr.brian.home.model

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder

data class ItemRect(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

fun getAllItemRects(
    apps: List<AppInfo>,
    positions: Map<String, AppPosition>,
    folders: List<Folder>,
    density: Density,
    defaultIconSize: Float = 64f
): List<ItemRect> {
    val items = mutableListOf<ItemRect>()

    // Add all apps
    apps.forEach { app ->
        val position = positions[app.packageName]
        val iconSize = position?.iconSize ?: defaultIconSize
        val iconSizePx = with(density) { iconSize.dp.toPx() }
        val x = position?.x ?: 0f
        val y = position?.y ?: 0f
        items.add(ItemRect(app.packageName, x, y, iconSizePx, iconSizePx))
    }

    // Add all folders
    folders.forEach { folder ->
        val iconSizePx = with(density) { folder.position.iconSize.dp.toPx() }
        items.add(ItemRect(folder.id, folder.position.x, folder.position.y, iconSizePx, iconSizePx))
    }

    return items
}
