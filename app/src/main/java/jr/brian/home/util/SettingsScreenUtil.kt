package jr.brian.home.util

import androidx.annotation.StringRes
import jr.brian.home.R

object SettingsScreenUtil {
    const val DEFAULT_VERSION_NAME = "v1.0"
    const val EXPANDED_THEME = "theme"
    const val EXPANDED_WALLPAPER = "wallpaper"
    const val EXPANDED_ICON_PACK = "icon_pack"
    const val EXPANDED_GRID = "grid"
    const val EXPANDED_THOR = "thor"
    const val EXPANDED_BACK_BUTTON = "back_button"
    const val EXPANDED_VISIBILITY = "visibility"
    const val EXPANDED_DOCK = "dock"
    const val EXPANDED_ESDE = "esde"
}

enum class SettingsTag(@StringRes val stringRes: Int) {
    EXPERIMENTAL(R.string.settings_tag_experimental)
}