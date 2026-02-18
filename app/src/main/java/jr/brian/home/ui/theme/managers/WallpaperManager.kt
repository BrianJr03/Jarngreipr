package jr.brian.home.ui.theme.managers

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.core.net.toUri
import java.io.File

private const val PREFS_NAME = "launcher_prefs"
private const val KEY_WALLPAPER = "selected_wallpaper"
private const val KEY_WALLPAPER_TYPE = "wallpaper_type"
private const val KEY_SAVED_IMAGE_URI = "saved_image_uri"
private const val KEY_SAVED_GIF_URI = "saved_gif_uri"
private const val KEY_SAVED_VIDEO_URI = "saved_video_uri"
const val WALLPAPER_TRANSPARENT = "TRANSPARENT"
const val WALLPAPER_ESDE = "ESDE"

class WallpaperManager(
    private val context: Context,
) {
    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var savedImageUri by mutableStateOf(prefs.getString(KEY_SAVED_IMAGE_URI, null))
        private set

    var savedGifUri by mutableStateOf(prefs.getString(KEY_SAVED_GIF_URI, null))
        private set

    var savedVideoUri by mutableStateOf(prefs.getString(KEY_SAVED_VIDEO_URI, null))
        private set

    var currentWallpaper by mutableStateOf(loadWallpaper())
        private set

    private fun loadWallpaper(): WallpaperInfo {
        val uri = prefs.getString(KEY_WALLPAPER, null)
        val typeString = prefs.getString(KEY_WALLPAPER_TYPE, WallpaperType.NONE.name)
        val type = try {
            WallpaperType.valueOf(typeString ?: WallpaperType.NONE.name)
        } catch (_: IllegalArgumentException) {
            WallpaperType.NONE
        }

        if (uri != null && type != WallpaperType.NONE && type != WallpaperType.TRANSPARENT && type != WallpaperType.ESDE) {
            if (!isUriAccessible(uri)) {
                prefs.edit {
                    remove(KEY_WALLPAPER)
                    remove(KEY_WALLPAPER_TYPE)
                }
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

    fun setESDE() {
        setWallpaper(WALLPAPER_ESDE, WallpaperType.ESDE)
    }

    fun setDefault() {
        setWallpaper(null, WallpaperType.NONE)
    }

    fun updateSavedImageUri(uri: String?) {
        savedImageUri = uri
        prefs.edit {
            if (uri != null) putString(KEY_SAVED_IMAGE_URI, uri) else remove(KEY_SAVED_IMAGE_URI)
        }
    }

    fun updateSavedGifUri(uri: String?) {
        savedGifUri = uri
        prefs.edit {
            if (uri != null) putString(KEY_SAVED_GIF_URI, uri) else remove(KEY_SAVED_GIF_URI)
        }
    }

    fun updateSavedVideoUri(uri: String?) {
        savedVideoUri = uri
        prefs.edit {
            if (uri != null) putString(KEY_SAVED_VIDEO_URI, uri) else remove(KEY_SAVED_VIDEO_URI)
        }
    }

    fun getSavedUriForType(type: WallpaperType): String? {
        return when (type) {
            WallpaperType.IMAGE -> savedImageUri
            WallpaperType.GIF -> savedGifUri
            WallpaperType.VIDEO -> savedVideoUri
            else -> null
        }
    }

    fun clearWallpaper() {
        try {
            val wallpaperDir = File(context.filesDir, "wallpapers")
            if (wallpaperDir.exists()) {
                wallpaperDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        prefs.edit {
            remove(KEY_SAVED_IMAGE_URI)
            remove(KEY_SAVED_GIF_URI)
            remove(KEY_SAVED_VIDEO_URI)
        }
        savedImageUri = null
        savedGifUri = null
        savedVideoUri = null

        setWallpaper(null, WallpaperType.NONE)
    }

    fun isTransparent(): Boolean {
        return currentWallpaper.type == WallpaperType.TRANSPARENT ||
               currentWallpaper.type == WallpaperType.ESDE
    }

    fun getWallpaperType(): WallpaperType {
        return currentWallpaper.type
    }

    fun getWallpaperUri(): String? {
        return currentWallpaper.uri
    }
}
