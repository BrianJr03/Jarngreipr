package jr.brian.home.ui.theme.managers

enum class WallpaperType {
    NONE,
    IMAGE,
    GIF,
    VIDEO,
    TRANSPARENT,
    ESDE,
    /** Frontend-only: mirror the main launcher's current wallpaper. */
    LAUNCHER
}

data class WallpaperInfo(
    val uri: String?,
    val type: WallpaperType
)
