package jr.brian.home.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

private const val PREFS_NAME = "launcher_prefs"
private const val KEY_WALLPAPER = "selected_wallpaper"
const val WALLPAPER_TRANSPARENT = "TRANSPARENT"

class WallpaperManager(
    private val context: Context,
) {
    var currentWallpaper by mutableStateOf(loadWallpaper())
        private set

    private fun loadWallpaper(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_WALLPAPER, null)
    }

    fun setWallpaper(uri: String?) {
        currentWallpaper = uri
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            if (uri != null) {
                putString(KEY_WALLPAPER, uri)
            } else {
                remove(KEY_WALLPAPER)
            }
        }
    }

    fun setTransparent() {
        setWallpaper(WALLPAPER_TRANSPARENT)
    }

    fun clearWallpaper() {
        setWallpaper(null)
    }

    fun isTransparent(): Boolean {
        return currentWallpaper == WALLPAPER_TRANSPARENT
    }
}
