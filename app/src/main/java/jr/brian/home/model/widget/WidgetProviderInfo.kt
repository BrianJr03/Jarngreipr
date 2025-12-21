package jr.brian.home.model.widget

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable

/**
 * Wrapper for widget provider information with additional display metadata
 */
data class WidgetProviderInfo(
    val providerInfo: AppWidgetProviderInfo,
    val label: String,
    val previewImage: Drawable?,
    val appName: String,
    val packageName: String,
    val description: String = ""
)

/**
 * Groups widgets by their parent application
 */
data class WidgetCategory(
    val appName: String,
    val packageName: String,
    val appIcon: Drawable?,
    val widgets: List<WidgetProviderInfo>
)
