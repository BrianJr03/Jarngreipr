package jr.brian.home.esde.model

data class GameInfo(
    val path: String,
    val name: String,
    val description: String? = null,
    val rating: Float = 0f,
    val releaseDate: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val genre: String? = null,
    val players: String? = null,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val playTimeMinutes: Int = 0,
    val lastPlayed: String? = null,
    val systemName: String,
    val artworkPath: String? = null,
    val physicalMediaPath: String? = null,
    val marqueeImagePath: String? = null,
    val screenshotPath: String? = null,
    val fanartPath: String? = null,
    val titlescreenPath: String? = null,
    val miximagePath: String? = null,
    val emulatorPackage: String? = null,
    val romAbsolutePath: String? = null,
    val launchCommand: String? = null
)
