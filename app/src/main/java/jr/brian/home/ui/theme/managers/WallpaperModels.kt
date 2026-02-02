package jr.brian.home.ui.theme.managers

enum class WallpaperType {
    NONE,
    IMAGE,
    GIF,
    VIDEO,
    TRANSPARENT,
    ESDE
}

data class WallpaperInfo(
    val uri: String?,
    val type: WallpaperType
)
