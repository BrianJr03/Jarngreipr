package jr.brian.home.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.model.IconPack
import jr.brian.home.model.IconPackDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages icon pack loading and caching
 */
@Singleton
class IconPackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val iconPackPreferences: IconPackPreferences
) {
    private val iconCache = mutableMapOf<String, Drawable>()
    private var currentIconPackResources: Resources? = null
    private var componentToDrawableMap = mutableMapOf<ComponentName, String>()

    /**
     * Get all installed icon packs
     */
    suspend fun getInstalledIconPacks(): List<IconPack> = withContext(Dispatchers.IO) {
        val iconPacks = mutableListOf<IconPack>()
        val pm = context.packageManager

        // Query for icon packs that declare the "com.android.launcher3.permission.READ_SETTINGS" permission
        // or have the category "com.novalauncher.category.CUSTOM_ICON_PICKER"
        val allPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA)

        for (packageInfo in allPackages) {
            val packageName = packageInfo.packageName

            try {
                val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                val metaData = appInfo.metaData

                // Check if package is an icon pack
                if (isIconPack(packageName, metaData)) {
                    val name = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)

                    iconPacks.add(
                        IconPack(
                            packageName = packageName,
                            name = name,
                            icon = icon
                        )
                    )
                }
            } catch (e: Exception) {
                // Package not found or other error, skip
                continue
            }
        }

        iconPacks.sortedBy { it.name }
    }

    /**
     * Check if a package is an icon pack
     */
    private fun isIconPack(packageName: String, metaData: android.os.Bundle?): Boolean {
        if (metaData == null) return false

        // Common icon pack metadata keys
        val iconPackKeys = listOf(
            "com.novalauncher.theme",
            "com.teslacoilsw.launcher.iconpack",
            "com.anddoes.launcher.theme",
            "ADW.THEMES",
            "org.adw.launcher.icons.ACTION_PICK_ICON"
        )

        return iconPackKeys.any { metaData.containsKey(it) }
    }

    /**
     * Load icon pack and parse appfilter.xml
     */
    suspend fun loadIconPack(packageName: String?) = withContext(Dispatchers.IO) {
        if (packageName.isNullOrEmpty()) {
            clearIconPack()
            return@withContext
        }

        try {
            val pm = context.packageManager
            currentIconPackResources = pm.getResourcesForApplication(packageName)
            componentToDrawableMap.clear()
            iconCache.clear()

            // Parse appfilter.xml
            parseAppFilter(packageName, currentIconPackResources!!)

        } catch (e: Exception) {
            e.printStackTrace()
            clearIconPack()
        }
    }

    /**
     * Parse appfilter.xml to build component to drawable name mapping
     */
    private fun parseAppFilter(packageName: String, resources: Resources) {
        try {
            val resId = resources.getIdentifier("appfilter", "xml", packageName)
            if (resId == 0) return

            val parser = resources.getXml(resId)
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")

                    if (component != null && drawable != null) {
                        // Parse component name (format: ComponentInfo{package/class})
                        val componentInfo = parseComponentName(component)
                        if (componentInfo != null) {
                            componentToDrawableMap[componentInfo] = drawable
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Parse component string to ComponentName
     * Format: ComponentInfo{package/class}
     */
    private fun parseComponentName(component: String): ComponentName? {
        try {
            val cleaned = component.replace("ComponentInfo{", "").replace("}", "")
            val parts = cleaned.split("/")
            if (parts.size == 2) {
                return ComponentName(parts[0], parts[1])
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Get icon for a specific app package
     */
    suspend fun getIconForPackage(
        packageName: String,
        defaultIcon: Drawable
    ): Drawable = withContext(Dispatchers.IO) {
        val selectedPackageName = iconPackPreferences.getSelectedIconPack()

        if (selectedPackageName.isNullOrEmpty() || currentIconPackResources == null) {
            return@withContext defaultIcon
        }

        // Check cache first
        iconCache[packageName]?.let { return@withContext it }

        // Try to find icon in icon pack
        val customIcon = findIconInPack(packageName)

        if (customIcon != null) {
            iconCache[packageName] = customIcon
            return@withContext customIcon
        }

        defaultIcon
    }

    /**
     * Find icon in the loaded icon pack
     */
    private fun findIconInPack(packageName: String): Drawable? {
        val resources = currentIconPackResources ?: return null
        val selectedPackageName = iconPackPreferences.getSelectedIconPack() ?: return null

        try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)

            if (launchIntent != null) {
                val componentName = launchIntent.component

                if (componentName != null) {
                    // Look up component in the mapping
                    val drawableName = componentToDrawableMap[componentName]

                    if (drawableName != null) {
                        val resId = resources.getIdentifier(
                            drawableName,
                            "drawable",
                            selectedPackageName
                        )

                        if (resId != 0) {
                            return ContextCompat.getDrawable(
                                context.createPackageContext(selectedPackageName, 0),
                                resId
                            )
                        }
                    }
                }
            }

            // Fallback: try common naming conventions
            val drawableName = packageName.replace(".", "_")
            val resId = resources.getIdentifier(drawableName, "drawable", selectedPackageName)

            if (resId != 0) {
                return ContextCompat.getDrawable(
                    context.createPackageContext(selectedPackageName, 0),
                    resId
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * Clear current icon pack
     */
    private fun clearIconPack() {
        currentIconPackResources = null
        componentToDrawableMap.clear()
        iconCache.clear()
    }

    /**
     * Get the currently selected icon pack
     */
    fun getSelectedIconPack(): String? {
        return iconPackPreferences.getSelectedIconPack()
    }

    /**
     * Set the selected icon pack
     */
    suspend fun setSelectedIconPack(packageName: String?) {
        iconPackPreferences.setSelectedIconPack(packageName)
        loadIconPack(packageName)
    }
}
