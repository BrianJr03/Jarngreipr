package jr.brian.home.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import jr.brian.home.data.database.CustomIconDao
import jr.brian.home.data.database.CustomIconEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages custom app icons by storing PNG files in internal storage
 * and maintaining a database mapping of package names to icon paths.
 */
@Singleton
class CustomIconManager @Inject constructor(
    private val context: Context,
    private val customIconDao: CustomIconDao
) {
    private val customIconsDir: File by lazy {
        File(context.filesDir, "custom_icons").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Flow of all custom icons as a map of package name to file path
     */
    val customIconsMap: Flow<Map<String, String>> = customIconDao.getAllCustomIcons()
        .map { entities ->
            entities.associate { it.packageName to it.customIconPath }
        }

    /**
     * Sets a custom icon for an app by copying the selected PNG to internal storage
     */
    suspend fun setCustomIcon(packageName: String, imageUri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Read the image from the URI
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: return@withContext Result.failure(Exception("Unable to open image"))

                // Decode the bitmap to validate it's a valid image
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (bitmap == null) {
                    return@withContext Result.failure(Exception("Invalid image file"))
                }

                // Create a unique filename based on package name
                val fileName = "${packageName.replace(".", "_")}.png"
                val iconFile = File(customIconsDir, fileName)

                // Save the bitmap as PNG to internal storage
                FileOutputStream(iconFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                // Recycle bitmap to free memory
                bitmap.recycle()

                // Save to database
                val customIcon = CustomIconEntity(
                    packageName = packageName,
                    customIconPath = iconFile.absolutePath
                )
                customIconDao.insertCustomIcon(customIcon)

                Log.d(
                    "CustomIconManager",
                    "Custom icon saved for $packageName at ${iconFile.absolutePath}"
                )
                Result.success(iconFile.absolutePath)
            } catch (e: Exception) {
                Log.e("CustomIconManager", "Error setting custom icon", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Gets the custom icon path for a package, if it exists
     */
    suspend fun getCustomIconPath(packageName: String): String? {
        return withContext(Dispatchers.IO) {
            customIconDao.getCustomIcon(packageName)?.customIconPath
        }
    }

    /**
     * Checks if a custom icon exists for the given package
     */
    suspend fun hasCustomIcon(packageName: String): Boolean {
        return getCustomIconPath(packageName) != null
    }

    /**
     * Removes the custom icon for an app
     */
    suspend fun removeCustomIcon(packageName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val entity = customIconDao.getCustomIcon(packageName)
                if (entity != null) {
                    // Delete the file
                    val file = File(entity.customIconPath)
                    if (file.exists()) {
                        file.delete()
                    }
                    // Remove from database
                    customIconDao.deleteCustomIcon(packageName)
                    Log.d("CustomIconManager", "Custom icon removed for $packageName")
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e("CustomIconManager", "Error removing custom icon", e)
                false
            }
        }
    }

    /**
     * Clears all custom icons
     */
    suspend fun clearAllCustomIcons() {
        withContext(Dispatchers.IO) {
            try {
                // Delete all files in the custom icons directory
                customIconsDir.listFiles()?.forEach { it.delete() }
                // Clear database
                customIconDao.deleteAllCustomIcons()
                Log.d("CustomIconManager", "All custom icons cleared")
            } catch (e: Exception) {
                Log.e("CustomIconManager", "Error clearing custom icons", e)
            }
        }
    }

    /**
     * Loads a bitmap from the custom icon path
     */
    fun loadCustomIconBitmap(iconPath: String): Bitmap? {
        return try {
            val file = File(iconPath)
            if (file.exists()) {
                BitmapFactory.decodeFile(iconPath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("CustomIconManager", "Error loading custom icon", e)
            null
        }
    }
}
