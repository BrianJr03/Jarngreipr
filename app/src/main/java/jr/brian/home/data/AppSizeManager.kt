package jr.brian.home.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

private const val PREFS_NAME = "gaming_launcher_prefs"
private const val KEY_APP_SIZE = "app_icon_size"

class AppSizeManager(private val context: Context) {
    companion object {
        const val MIN_SIZE = 48f
        const val MAX_SIZE = 96f
        const val DEFAULT_SIZE = 64f
    }

    var appIconSize by mutableFloatStateOf(loadAppSize())
        private set

    private fun loadAppSize(): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_APP_SIZE, DEFAULT_SIZE)
    }

    fun setAppSize(size: Float) {
        val clampedSize = size.coerceIn(MIN_SIZE, MAX_SIZE)
        appIconSize = clampedSize
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putFloat(KEY_APP_SIZE, clampedSize) }
    }
}
