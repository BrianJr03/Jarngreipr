package jr.brian.home.esde.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.esde.preferences.ESDEPreferencesManager
import jr.brian.home.esde.wallpaper.WallpaperState
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ESDEViewModel @Inject constructor(
    private val prefs: ESDEPreferencesManager
) : ViewModel() {

    private val _wallpaperState = mutableStateOf(createInitialState())
    val wallpaperState: State<WallpaperState> = _wallpaperState

    private fun createInitialState(): WallpaperState {
        val prefsState = prefs.state.value
        val lastSystem = prefsState.lastSelectedSystem
        return WallpaperState(
            dimmingLevel = prefsState.dimmingLevelFloat,
            blurLevel = prefsState.blurLevel.toFloat(),
            animationStyle = prefsState.animationStyle,
            animationDuration = prefsState.animationDuration,
            animationScale = prefsState.animationScale,
            backgroundColor = Color(prefsState.backgroundColor),
            marqueePath = lastSystem?.let { getSystemLogoPath(it) }
        )
    }

    fun updateForSystem(systemName: String) {
        prefs.setLastSelectedSystem(systemName)
        _wallpaperState.value = _wallpaperState.value.copy(
//            currentImagePath = getSystemImagePath(systemName),
            isVideoPlaying = false,
            marqueePath = getSystemLogoPath(systemName)
        )
    }

    fun updateForGame(
        systemName: String,
        gameFilename: String
    ) {
        _wallpaperState.value = _wallpaperState.value.copy(
//            currentImagePath = getGameImagePath(systemName, gameFilename),
            isVideoPlaying = false,
            marqueePath = getGameMarqueePath(systemName, gameFilename)
        )
    }

    fun handleGameStarted() {
        if (prefs.state.value.videoEnabled) {
            _wallpaperState.value = _wallpaperState.value.copy(
                isVideoPlaying = true,
                currentImagePath = null,
                marqueePath = null
            )
        } else {
            _wallpaperState.value = _wallpaperState.value.copy(
                dimmingLevel = 1.0f, // Full dim (black screen)
                marqueePath = null
            )
        }
    }

    fun handleGameEnded() {
        _wallpaperState.value = _wallpaperState.value.copy(
            isVideoPlaying = false,
            dimmingLevel = prefs.state.value.dimmingLevelFloat
        )
    }

    fun handleScreensaverStarted() {
        _wallpaperState.value = _wallpaperState.value.copy(
            dimmingLevel = 1.0f // Full dim (black screen)
        )
    }

    fun handleScreensaverEnded() {
        _wallpaperState.value = _wallpaperState.value.copy(
            dimmingLevel = prefs.state.value.dimmingLevelFloat
        )
    }

    fun updateForScreensaverGame(
        systemName: String,
        gameFilename: String
    ) {
        _wallpaperState.value = _wallpaperState.value.copy(
//            currentImagePath = getGameImagePath(systemName, gameFilename),
            dimmingLevel = prefs.state.value.dimmingLevelFloat
        )
    }

    @Suppress("unused")
    fun refreshFromPreferences() {
        val prefsState = prefs.state.value
        _wallpaperState.value = _wallpaperState.value.copy(
            dimmingLevel = prefsState.dimmingLevelFloat,
            blurLevel = prefsState.blurLevel.toFloat(),
            animationStyle = prefsState.animationStyle,
            animationDuration = prefsState.animationDuration,
            animationScale = prefsState.animationScale,
            backgroundColor = Color(prefsState.backgroundColor)
        )
    }

    private fun getSystemImagePath(systemName: String): String? {
        // First check for custom system images
        val customSystemImages = File("/storage/emulated/0/ES-DE Companion/system_images")
        if (customSystemImages.exists()) {
            val extensions = listOf("png", "jpg", "jpeg", "webp")
            for (ext in extensions) {
                val customImage = File(customSystemImages, "$systemName.$ext")
                if (customImage.exists()) return customImage.absolutePath
            }
        }

        // ES-DE downloaded_media has per-game fanart, not system-level
        // So pick a random game's fanart/screenshot from this system
        val mediaTypes = listOf("fanart", "screenshots", "titlescreens")
        for (mediaType in mediaTypes) {
            val mediaDir = File(ESDE_MEDIA_PATH, "$systemName/$mediaType")
            if (mediaDir.exists() && mediaDir.isDirectory) {
                val images = mediaDir.listFiles()?.filter { it.isFile && isImageFile(it) }
                if (!images.isNullOrEmpty()) {
                    return (images.randomOrNull() ?: images.first()).absolutePath
                }
            }
        }

        return null
    }

    private fun getSystemLogoPath(systemName: String): String {
        return "file:///android_asset/system_logos/$systemName.svg"
    }

    private fun getGameImagePath(systemName: String, gameFilename: String): String? {
        val nameOnly = File(gameFilename).nameWithoutExtension
        val mediaTypes = listOf("screenshots", "fanart", "titlescreens", "covers", "miximages")
        val extensions = listOf("png", "jpg", "jpeg", "webp")

        for (mediaType in mediaTypes) {
            for (ext in extensions) {
                val file = File(ESDE_MEDIA_PATH, "$systemName/$mediaType/$nameOnly.$ext")
                if (file.exists()) return file.absolutePath
            }
        }

        for (ext in extensions) {
            val wheelFile = File(ESDE_MEDIA_PATH, "$systemName/images/wheel-3d/$nameOnly.$ext")
            if (wheelFile.exists()) return wheelFile.absolutePath
        }

        return null
    }

    private fun getGameMarqueePath(systemName: String, gameFilename: String): String? {
        val nameOnly = File(gameFilename).nameWithoutExtension
        val extensions = listOf("png", "jpg", "jpeg", "webp", "svg")

        for (ext in extensions) {
            val file = File(ESDE_MEDIA_PATH, "$systemName/marquees/$nameOnly.$ext")
            if (file.exists()) return file.absolutePath
        }

        val fallbackDirs = listOf("images/wheel-2d", "images/wheel-3d")
        for (dir in fallbackDirs) {
            for (ext in extensions) {
                val file = File(ESDE_MEDIA_PATH, "$systemName/$dir/$nameOnly.$ext")
                if (file.exists()) return file.absolutePath
            }
        }

        return null
    }

    private fun isImageFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("png", "jpg", "jpeg", "webp")
    }

    companion object {
        private const val ESDE_MEDIA_PATH = "/storage/emulated/0/ES-DE/downloaded_media"
    }
}