package jr.brian.home.data

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import jr.brian.home.model.widget.WidgetCategory
import jr.brian.home.model.widget.WidgetProviderInfo
import jr.brian.home.model.widget.WidgetSizeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for fetching and managing available widget providers
 */
class WidgetProviderRepository(private val context: Context) {

    companion object {
        private const val TAG = "WidgetProviderRepo"
    }

    /**
     * Fetches all available widget providers from installed apps
     */
    suspend fun getAvailableWidgets(): List<WidgetProviderInfo> = withContext(Dispatchers.IO) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val installedProviders = appWidgetManager.installedProviders

            installedProviders.mapNotNull { providerInfo ->
                try {
                    createWidgetProviderInfo(providerInfo)
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating widget info for ${providerInfo.provider}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching available widgets", e)
            emptyList()
        }
    }

    /**
     * Gets widgets grouped by their parent application
     */
    suspend fun getWidgetCategories(): List<WidgetCategory> = withContext(Dispatchers.IO) {
        try {
            val widgets = getAvailableWidgets()
            val grouped = widgets.groupBy { it.packageName }

            grouped.map { (packageName, widgetList) ->
                val appIcon = try {
                    context.packageManager.getApplicationIcon(packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }

                val appName = widgetList.firstOrNull()?.appName ?: packageName

                WidgetCategory(
                    appName = appName,
                    packageName = packageName,
                    appIcon = appIcon,
                    widgets = widgetList.sortedBy { it.label }
                )
            }.sortedBy { it.appName }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating widget categories", e)
            emptyList()
        }
    }

    /**
     * Searches for widgets by name or app name
     */
    suspend fun searchWidgets(query: String): List<WidgetProviderInfo> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) {
                return@withContext getAvailableWidgets()
            }

            val widgets = getAvailableWidgets()
            widgets.filter { widget ->
                widget.label.contains(query, ignoreCase = true) ||
                        widget.appName.contains(query, ignoreCase = true)
            }
        }

    /**
     * Creates a WidgetProviderInfo from AppWidgetProviderInfo with all metadata
     */
    private fun createWidgetProviderInfo(providerInfo: AppWidgetProviderInfo): WidgetProviderInfo {
        val packageManager = context.packageManager
        val provider = providerInfo.provider

        val label = providerInfo.loadLabel(packageManager)
        val previewImage = getWidgetPreview(providerInfo, packageManager)

        val appInfo = try {
            packageManager.getApplicationInfo(provider.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        val appName = appInfo?.let { packageManager.getApplicationLabel(it).toString() }
            ?: provider.packageName

        val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            providerInfo.loadDescription(context)?.toString() ?: ""
        } else {
            ""
        }

        return WidgetProviderInfo(
            providerInfo = providerInfo,
            label = label,
            previewImage = previewImage,
            appName = appName,
            packageName = provider.packageName,
            description = description
        )
    }

    /**
     * Gets the widget preview image, supporting both legacy and modern APIs
     */
    private fun getWidgetPreview(
        providerInfo: AppWidgetProviderInfo,
        packageManager: PackageManager
    ): Drawable? {
        return try {
            // Try modern preview layout first (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val previewLayout = providerInfo.previewLayout
                if (previewLayout != 0) {
                    // For now, we'll fall back to the static preview
                    // In a full implementation, you could inflate the layout
                    // and render it as a bitmap
                }
            }

            // Try static preview image
            val previewImage = providerInfo.previewImage
            if (previewImage != 0) {
                return packageManager.getDrawable(
                    providerInfo.provider.packageName,
                    previewImage,
                    null
                )
            }

            // Fall back to app icon
            providerInfo.loadIcon(context, context.resources.displayMetrics.densityDpi)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading widget preview", e)
            try {
                // Last resort: try to get the app icon
                packageManager.getApplicationIcon(providerInfo.provider.packageName)
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    /**
     * Gets detailed info about the widget's size requirements
     */
    fun getWidgetSizeInfo(providerInfo: AppWidgetProviderInfo): WidgetSizeInfo {
        val cellSizeDp = 70

        val minWidthCells = (providerInfo.minWidth / cellSizeDp).coerceAtLeast(1)
        val minHeightCells = (providerInfo.minHeight / cellSizeDp).coerceAtLeast(1)

        val targetWidthCells = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            providerInfo.targetCellWidth.coerceAtLeast(minWidthCells)
        } else {
            minWidthCells
        }

        val targetHeightCells = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            providerInfo.targetCellHeight.coerceAtLeast(minHeightCells)
        } else {
            minHeightCells
        }

        val maxWidthCells = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            providerInfo.maxResizeWidth.let { if (it > 0) it / cellSizeDp else targetWidthCells }
        } else {
            targetWidthCells
        }

        val maxHeightCells = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            providerInfo.maxResizeHeight.let { if (it > 0) it / cellSizeDp else targetHeightCells }
        } else {
            targetHeightCells
        }

        val isResizable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            providerInfo.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE != 0
        } else {
            @Suppress("DEPRECATION")
            providerInfo.resizeMode != AppWidgetProviderInfo.RESIZE_NONE
        }

        return WidgetSizeInfo(
            minWidthCells = minWidthCells,
            minHeightCells = minHeightCells,
            targetWidthCells = targetWidthCells,
            targetHeightCells = targetHeightCells,
            maxWidthCells = maxWidthCells,
            maxHeightCells = maxHeightCells,
            isResizable = isResizable
        )
    }
}
