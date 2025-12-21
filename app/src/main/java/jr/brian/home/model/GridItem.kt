package jr.brian.home.model

import androidx.compose.ui.graphics.vector.ImageVector

sealed class GridItem {
    data class IconItem(
        val icon: ImageVector,
        val label: String,
        val onClick: () -> Unit,
        val index: Int
    ) : GridItem()

    data class TextItem(
        val text: String,
        val onClick: () -> Unit,
        val isSelected: Boolean,
        val index: Int
    ) : GridItem()
}