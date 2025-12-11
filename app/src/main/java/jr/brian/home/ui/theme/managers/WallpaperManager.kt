package jr.brian.home.ui.theme.managers

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.core.net.toUri

private const val PREFS_NAME = "launcher_prefs"
private const val KEY_WALLPAPER = "selected_wallpaper"
private const val KEY_WALLPAPER_TYPE = "wallpaper_type"
const val WALLPAPER_TRANSPARENT = "TRANSPARENT"

enum class WallpaperType {
    NONE,
    IMAGE,
    GIF,
    VIDEO,
    TRANSPARENT
}

data class WallpaperInfo(
    val uri: String?,
    val type: WallpaperType
)

class WallpaperManager(
    private val context: Context,
) {
    var currentWallpaper by mutableStateOf(loadWallpaper())
        private set

    private fun loadWallpaper(): WallpaperInfo {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uri = prefs.getString(KEY_WALLPAPER, null)
        val typeString = prefs.getString(KEY_WALLPAPER_TYPE, WallpaperType.NONE.name)
        val type = try {
            WallpaperType.valueOf(typeString ?: WallpaperType.NONE.name)
        } catch (_: IllegalArgumentException) {
            WallpaperType.NONE
        }

        if (uri != null && type != WallpaperType.NONE && type != WallpaperType.TRANSPARENT) {
            if (!isUriAccessible(uri)) {
                clearWallpaper()
                return WallpaperInfo(null, WallpaperType.NONE)
            }
        }

        return WallpaperInfo(uri, type)
    }

    private fun isUriAccessible(uriString: String): Boolean {
        return try {
            val uri = uriString.toUri()
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun setWallpaper(uri: String?, type: WallpaperType) {
        currentWallpaper = WallpaperInfo(uri, type)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            if (uri != null) {
                putString(KEY_WALLPAPER, uri)
                putString(KEY_WALLPAPER_TYPE, type.name)
            } else {
                remove(KEY_WALLPAPER)
                remove(KEY_WALLPAPER_TYPE)
            }
        }
    }

    fun setTransparent() {
        setWallpaper(WALLPAPER_TRANSPARENT, WallpaperType.TRANSPARENT)
    }

    fun clearWallpaper() {
        try {
            val wallpaperDir = java.io.File(context.filesDir, "wallpapers")
            if (wallpaperDir.exists()) {
                wallpaperDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        setWallpaper(null, WallpaperType.NONE)
    }

    fun isTransparent(): Boolean {
        return currentWallpaper.type == WallpaperType.TRANSPARENT
    }

    fun getWallpaperType(): WallpaperType {
        return currentWallpaper.type
    }

    fun getWallpaperUri(): String? {
        return currentWallpaper.uri
    }
}
