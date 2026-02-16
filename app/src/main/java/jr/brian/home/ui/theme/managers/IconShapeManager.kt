package jr.brian.home.ui.theme.managers

import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.core.content.edit

private const val PREFS_NAME = "icon_shape_prefs"
private const val KEY_ICON_SHAPE = "icon_shape"

/**
 * Defines the available icon shape styles for app icons.
 */
enum class IconShape {
    ROUNDED,
    SQUARE;
    /**
     * Returns the Compose Shape for this icon shape style.
     */
    fun toComposeShape(): Shape = when (this) {
        ROUNDED -> RoundedCornerShape(8.dp)
        SQUARE -> RectangleShape
    }
}

/**
 * Manages the user's preferred icon shape style (rounded vs square).
 * The preference is persisted to SharedPreferences.
 */
class IconShapeManager(
    private val context: Context,
) {
    var iconShape by mutableStateOf(loadIconShape())
        private set

    private fun loadIconShape(): IconShape {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val shapeName = prefs.getString(KEY_ICON_SHAPE, IconShape.ROUNDED.name)
        return try {
            IconShape.valueOf(shapeName ?: IconShape.ROUNDED.name)
        } catch (_: IllegalArgumentException) {
            IconShape.ROUNDED
        }
    }

    fun updateIconShape(shape: IconShape) {
        iconShape = shape
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_ICON_SHAPE, shape.name) }
    }

    fun toggleIconShape() {
        updateIconShape(
            if (iconShape == IconShape.ROUNDED) IconShape.SQUARE else IconShape.ROUNDED
        )
    }
}
