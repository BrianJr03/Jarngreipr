package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.compositionLocalOf

val LocalWallpaperManager =
    compositionLocalOf<WallpaperManager> {
        error("WallpaperManager not provided")
    }
