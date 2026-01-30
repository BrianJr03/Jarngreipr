package jr.brian.home.data

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class DockSize(
    val displayName: String,
    val iconSize: Dp,
    val containerSize: Dp,
    val padding: Dp,
    val spacing: Dp
) {
    MINI(
        displayName = "Mini",
        iconSize = 36.dp,
        containerSize = 44.dp,
        padding = 8.dp,
        spacing = 6.dp
    ),
    COMPACT(
        displayName = "Compact",
        iconSize = 44.dp,
        containerSize = 56.dp,
        padding = 10.dp,
        spacing = 8.dp
    ),
    STANDARD(
        displayName = "Standard",
        iconSize = 52.dp,
        containerSize = 64.dp,
        padding = 12.dp,
        spacing = 10.dp
    );
    companion object {
        fun fromOrdinal(ordinal: Int): DockSize {
            return entries.getOrNull(ordinal) ?: COMPACT
        }
    }
}
