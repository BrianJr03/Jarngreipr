package jr.brian.home.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import jr.brian.home.model.IconPack
import jr.brian.home.model.IconPackResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

private val Context.iconPackDataStore by preferencesDataStore(name = "icon_pack_prefs")

/**
 * Manages icon pack detection, loading, and application
 */
class IconPackManager(private val context: Context) {

    private val selectedIconPackKey = stringPreferencesKey("selected_icon_pack")

    // Known icon pack intent actions
    private val iconPackIntents = listOf(
        "org.adw.launcher.THEMES",
        "com.gau.go.launcherex.theme",
        "com.fede.launcher.THEME_ICONPACK",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "com.novalauncher.THEME"
    )

    private var cachedMappings: Map<String, String>? = null
    private var cachedPackageName: String? = null
    private var cachedResources: IconPackResources? = null

    /**
     * Get the currently selected icon pack package name
     */
    val selectedIconPack: Flow<String?> = context.iconPackDataStore.data.map { preferences ->
        preferences[selectedIconPackKey]
    }

    /**
     * Set the selected icon pack
     */
    suspend fun setSelectedIconPack(packageName: String?) {
        context.iconPackDataStore.edit { preferences ->
            if (packageName != null) {
                preferences[selectedIconPackKey] = packageName
            } else {
                preferences.remove(selectedIconPackKey)
            }
        }
        // Clear cache when pack changes
        cachedMappings = null
        cachedPackageName = null
        cachedResources = null
    }

    /**
     * Detect all installed icon packs
     */
    suspend fun getInstalledIconPacks(): List<IconPack> = withContext(Dispatchers.IO) {
        val iconPacks = mutableListOf<IconPack>()
        val pm = context.packageManager

        for (action in iconPackIntents) {
            try {
                val intent = Intent(action)
                val resolveInfos = pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0)
                )

                for (resolveInfo in resolveInfos) {
                    val packageName = resolveInfo.activityInfo.packageName

                    // Avoid duplicates
                    if (iconPacks.none { it.packageName == packageName }) {
                        try {
                            val appInfo = pm.getApplicationInfo(
                                packageName,
                                PackageManager.ApplicationInfoFlags.of(0)
                            )
                            val name = pm.getApplicationLabel(appInfo).toString()
                            val icon = pm.getApplicationIcon(packageName)

                            iconPacks.add(IconPack(packageName, name, icon))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        iconPacks.sortedBy { it.name }
    }

    /**
     * Load icon for a specific app from the selected icon pack
     * @param packageName The app's package name
     * @param activityName The app's main activity name (optional for better matching)
     * @return Drawable from icon pack, or null if not found
     */
    suspend fun getIconForApp(
        packageName: String,
        activityName: String? = null
    ): Drawable? = withContext(Dispatchers.IO) {
        val selectedPack = selectedIconPack.first() ?: return@withContext null

        try {
            // Load mappings if not cached or if pack changed
            if (cachedMappings == null || cachedPackageName != selectedPack) {
                cachedMappings = loadAppFilter(selectedPack)
                cachedPackageName = selectedPack
            }

            val mappings = cachedMappings ?: return@withContext null

            // Try to find mapping with activity name first
            val componentName = if (activityName != null) {
                "ComponentInfo{$packageName/$activityName}"
            } else {
                null
            }

            var drawableName = componentName?.let { mappings[it] }

            // If not found, try with just package name
            if (drawableName == null) {
                drawableName = mappings.entries.find {
                    it.key.contains(packageName)
                }?.value
            }

            if (drawableName != null) {
                return@withContext loadDrawableFromIconPack(selectedPack, drawableName)
            }

            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Apply icon pack masking to a default icon
     * This is used when an app doesn't have a custom icon in the pack
     */
    suspend fun applyIconMask(originalIcon: Drawable): Drawable? = withContext(Dispatchers.IO) {
        val selectedPack = selectedIconPack.first() ?: return@withContext null

        try {
            // Load resources if not cached
            if (cachedResources == null || cachedPackageName != selectedPack) {
                cachedResources = loadIconPackResources(selectedPack)
            }

            val resources = cachedResources ?: return@withContext null

            // If no masking resources available, return original
            if (resources.backIcons.isEmpty()) {
                return@withContext null
            }

            // Create masked icon
            val back = resources.backIcons.random()
            val mask = resources.maskIcons.randomOrNull()
            val front = resources.frontIcon

            return@withContext createMaskedIcon(originalIcon, back, mask, front, resources.scale)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Parse appfilter.xml from icon pack
     */
    private fun loadAppFilter(packageName: String): Map<String, String> {
        val mappings = mutableMapOf<String, String>()

        try {
            val iconPackContext =
                context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
            val resources = iconPackContext.resources

            // Try to find appfilter.xml in different locations
            var parser: XmlPullParser? = null

            // Try res/xml/appfilter.xml
            try {
                val resId = resources.getIdentifier("appfilter", "xml", packageName)
                if (resId != 0) {
                    parser = resources.getXml(resId)
                }
            } catch (_: Exception) {
                // Continue to try other locations
            }

            // Try assets/appfilter.xml
            if (parser == null) {
                try {
                    val inputStream = iconPackContext.assets.open("appfilter.xml")
                    val factory = XmlPullParserFactory.newInstance()
                    parser = factory.newPullParser()
                    parser.setInput(inputStream, "UTF-8")
                } catch (_: Exception) {
                    // File not found in assets
                }
            }

            parser?.let { xmlParser ->
                var eventType = xmlParser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && xmlParser.name == "item") {
                        val component = xmlParser.getAttributeValue(null, "component")
                        val drawable = xmlParser.getAttributeValue(null, "drawable")

                        if (component != null && drawable != null) {
                            mappings[component] = drawable
                        }
                    }
                    eventType = xmlParser.next()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return mappings
    }

    /**
     * Load a drawable from the icon pack by name
     */
    private fun loadDrawableFromIconPack(packageName: String, drawableName: String): Drawable? {
        try {
            val iconPackContext =
                context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
            val resources = iconPackContext.resources
            val resId = resources.getIdentifier(drawableName, "drawable", packageName)

            if (resId != 0) {
                return resources.getDrawable(resId, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * Load icon pack resources (masks, backs, fronts, scale)
     */
    private fun loadIconPackResources(packageName: String): IconPackResources {
        try {
            val iconPackContext =
                context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
            val resources = iconPackContext.resources

            val masks = mutableListOf<Drawable>()
            val backs = mutableListOf<Drawable>()
            var front: Drawable? = null
            var scale = 1.0f

            // Try to load from appfilter.xml
            try {
                var parser: XmlPullParser? = null

                // Try res/xml/appfilter.xml
                try {
                    val resId = resources.getIdentifier("appfilter", "xml", packageName)
                    if (resId != 0) {
                        parser = resources.getXml(resId)
                    }
                } catch (_: Exception) {
                    // Continue
                }

                // Try assets/appfilter.xml
                if (parser == null) {
                    try {
                        val inputStream = iconPackContext.assets.open("appfilter.xml")
                        val factory = XmlPullParserFactory.newInstance()
                        parser = factory.newPullParser()
                        parser.setInput(inputStream, "UTF-8")
                    } catch (_: Exception) {
                        // File not found
                    }
                }

                parser?.let { xmlParser ->
                    var eventType = xmlParser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            when (xmlParser.name) {
                                "iconback" -> {
                                    for (i in 0 until xmlParser.attributeCount) {
                                        val drawableName = xmlParser.getAttributeValue(i)
                                        loadDrawableFromIconPack(packageName, drawableName)?.let {
                                            backs.add(it)
                                        }
                                    }
                                }

                                "iconmask" -> {
                                    for (i in 0 until xmlParser.attributeCount) {
                                        val drawableName = xmlParser.getAttributeValue(i)
                                        loadDrawableFromIconPack(packageName, drawableName)?.let {
                                            masks.add(it)
                                        }
                                    }
                                }

                                "iconupon" -> {
                                    val drawableName = xmlParser.getAttributeValue(0)
                                    front = loadDrawableFromIconPack(packageName, drawableName)
                                }

                                "scale" -> {
                                    val factor = xmlParser.getAttributeValue(null, "factor")
                                    factor?.toFloatOrNull()?.let {
                                        scale = it
                                    }
                                }
                            }
                        }
                        eventType = xmlParser.next()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return IconPackResources(masks, backs, front, scale)
        } catch (e: Exception) {
            e.printStackTrace()
            return IconPackResources()
        }
    }

    /**
     * Create a masked icon by combining background, original icon (scaled), mask, and front overlay
     */
    private fun createMaskedIcon(
        original: Drawable,
        back: Drawable,
        mask: Drawable?,
        front: Drawable?,
        scale: Float
    ): Drawable {
        val width = 192
        val height = 192

        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)

        // Draw background
        back.setBounds(0, 0, width, height)
        back.draw(canvas)

        // Draw scaled original icon
        val scaledSize = (width * scale).toInt()
        val offset = (width - scaledSize) / 2
        val originalBitmap = original.toBitmap(scaledSize, scaledSize)
        canvas.drawBitmap(originalBitmap, offset.toFloat(), offset.toFloat(), null)

        // Apply mask if available
        mask?.let {
            it.setBounds(0, 0, width, height)
            it.draw(canvas)
        }

        // Draw front overlay if available
        front?.let {
            it.setBounds(0, 0, width, height)
            it.draw(canvas)
        }

        return bitmap.toDrawable(context.resources)
    }
}
