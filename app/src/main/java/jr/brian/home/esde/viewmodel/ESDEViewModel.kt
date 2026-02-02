package jr.brian.home.esde.viewmodel

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.esde.preferences.ESDEPreferencesManager
import jr.brian.home.esde.preferences.GameImageType
import jr.brian.home.esde.preferences.SystemImageType
import jr.brian.home.esde.util.ESDEMediaConstants.ESDE_MEDIA_PATH
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_MARQUEES
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_VIDEOS
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_WHEEL_3D
import jr.brian.home.esde.util.ESDEMediaConstants.GAME_IMAGE_FALLBACKS
import jr.brian.home.esde.util.ESDEMediaConstants.IMAGE_EXTENSIONS
import jr.brian.home.esde.util.ESDEMediaConstants.IMAGE_EXTENSIONS_WITH_SVG
import jr.brian.home.esde.util.ESDEMediaConstants.MARQUEE_FALLBACK_DIRS
import jr.brian.home.esde.util.ESDEMediaConstants.SYSTEM_IMAGE_FALLBACKS
import jr.brian.home.esde.util.ESDEMediaConstants.SYSTEM_LOGOS_PATH
import jr.brian.home.esde.util.ESDEMediaConstants.VIDEO_EXTENSIONS
import jr.brian.home.esde.wallpaper.WallpaperState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ESDEViewModel @Inject constructor(
    val prefs: ESDEPreferencesManager
) : ViewModel() {
    private val systemImageCache = mutableMapOf<String, String?>()

    private val _wallpaperState = mutableStateOf(createInitialState())
    val wallpaperState: State<WallpaperState> = _wallpaperState

    private val videoDelayHandler = Handler(Looper.getMainLooper())
    private var pendingVideoRunnable: Runnable? = null
    private var currentSystem: String? = null
    private var currentGameSystem: String? = null
    private var currentGameFilename: String? = null

    init {
        prefs.state
            .onEach { prefsState ->
                _wallpaperState.value = _wallpaperState.value.copy(
                    dimmingLevel = prefsState.dimmingLevelFloat,
                    blurLevel = prefsState.blurLevel.toFloat(),
                    animationStyle = prefsState.animationStyle,
                    animationDuration = prefsState.animationDuration,
                    animationScale = prefsState.animationScale,
                    backgroundColor = Color(prefsState.backgroundColor),
                    videoAudioEnabled = prefsState.videoAudioEnabled,
                    videoDelaySeconds = prefsState.videoDelaySeconds,
                    showSystemLogo = prefsState.showSystemLogo,
                    logoAlignment = prefsState.logoAlignment,
                    hideContentOnVideo = prefsState.hideContentOnVideo
                )
            }.launchIn(viewModelScope)
    }

    fun refreshSystemImage() {
        systemImageCache.clear()

        val systemName = prefs.state.value.lastSelectedSystem ?: return
        val newImagePath = when (prefs.state.value.systemImageType) {
            SystemImageType.None -> null
            else -> getSystemImagePath(systemName)
        }
        _wallpaperState.value = _wallpaperState.value.copy(
            currentImagePath = newImagePath
        )
    }

    private fun createInitialState(): WallpaperState {
        val prefsState = prefs.state.value
        val lastSystem = prefsState.lastSelectedSystem

        val initialImagePath = when (prefsState.systemImageType) {
            SystemImageType.None -> null
            else -> lastSystem?.let { getSystemImagePath(it) }
        }

        return WallpaperState(
            currentImagePath = initialImagePath,
            dimmingLevel = prefsState.dimmingLevelFloat,
            blurLevel = prefsState.blurLevel.toFloat(),
            animationStyle = prefsState.animationStyle,
            animationDuration = prefsState.animationDuration,
            animationScale = prefsState.animationScale,
            backgroundColor = Color(prefsState.backgroundColor),
            videoAudioEnabled = prefsState.videoAudioEnabled,
            videoDelaySeconds = prefsState.videoDelaySeconds,
            marqueePath = lastSystem?.let { getSystemLogoPath(it) },
            showSystemLogo = prefsState.showSystemLogo,
            logoAlignment = prefsState.logoAlignment,
            hideContentOnVideo = prefsState.hideContentOnVideo
        )
    }

    fun updateForSystem(systemName: String) {
        if (currentSystem == systemName) return

        stopVideo()
        currentSystem = systemName
        systemImageCache.remove(systemName)

        prefs.setLastSelectedSystem(systemName)
        _wallpaperState.value = _wallpaperState.value.copy(
            currentImagePath = getSystemImagePath(systemName),
            isVideoPlaying = false,
            videoPath = null,
            marqueePath = getSystemLogoPath(systemName)
        )
    }

    fun updateForGame(
        systemName: String,
        gameFilename: String
    ) {
        cancelPendingVideo()

        currentSystem = null
        currentGameSystem = systemName
        currentGameFilename = gameFilename

        _wallpaperState.value = _wallpaperState.value.copy(
            currentImagePath = getGameImagePath(systemName, gameFilename),
            isVideoPlaying = false,
            videoPath = null,
            marqueePath = getGameMarqueePath(systemName, gameFilename)
        )

        if (prefs.state.value.videoEnabled) {
            scheduleVideoPlayback(systemName, gameFilename)
        }
    }

    private fun scheduleVideoPlayback(systemName: String, gameFilename: String) {
        val videoPath = getGameVideoPath(systemName, gameFilename)
        if (videoPath == null) {
            Log.d(TAG, "No video found for: $systemName / $gameFilename")
            return
        }

        val delayMs = prefs.state.value.videoDelaySeconds * 1000L
        Log.d(TAG, "Scheduling video playback in ${delayMs}ms")

        pendingVideoRunnable = Runnable {
            if (currentGameSystem == systemName && currentGameFilename == gameFilename) {
                _wallpaperState.value = _wallpaperState.value.copy(
                    isVideoPlaying = true,
                    videoPath = videoPath
                )
            }
        }

        videoDelayHandler.postDelayed(pendingVideoRunnable!!, delayMs)
    }

    fun cancelPendingVideo() {
        pendingVideoRunnable?.let {
            videoDelayHandler.removeCallbacks(it)
        }
        pendingVideoRunnable = null
    }

    fun stopVideo() {
        cancelPendingVideo()
        _wallpaperState.value = _wallpaperState.value.copy(
            isVideoPlaying = false,
            videoPath = null
        )
    }

    fun handleGameStarted() {
        stopVideo()
        _wallpaperState.value = _wallpaperState.value.copy(
            dimmingLevel = 1.0f,
            marqueePath = null
        )
    }

    fun handleGameEnded() {
        _wallpaperState.value = _wallpaperState.value.copy(
            isVideoPlaying = false,
            videoPath = null,
            dimmingLevel = prefs.state.value.dimmingLevelFloat
        )
    }

    fun handleScreensaverStarted() {
        stopVideo()
        _wallpaperState.value = _wallpaperState.value.copy(
            dimmingLevel = 1.0f
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
            currentImagePath = getGameImagePath(systemName, gameFilename),
            dimmingLevel = prefs.state.value.dimmingLevelFloat
        )
    }

    private fun getSystemImagePath(systemName: String): String? {
        val prefsState = prefs.state.value
        val systemImageType = prefsState.systemImageType
        val useRandom = prefsState.randomSystemImage

        if (systemImageType == SystemImageType.None) {
            return null
        }

        if (systemImageCache.containsKey(systemName)) {
            return systemImageCache[systemName]
        }

        val preferredFolder = systemImageType.folderName ?: return null
        val mediaDir = File(ESDE_MEDIA_PATH, "$systemName/$preferredFolder")
        if (mediaDir.exists() && mediaDir.isDirectory) {
            val images = mediaDir.listFiles()?.filter { it.isFile && isImageFile(it) }
            if (!images.isNullOrEmpty()) {
                val selectedPath =
                    if (useRandom) images.random().absolutePath else images.first().absolutePath
                systemImageCache[systemName] = selectedPath
                return selectedPath
            }
        }

        val fallbackTypes = SYSTEM_IMAGE_FALLBACKS
            .filter { it != preferredFolder }

        for (mediaType in fallbackTypes) {
            val fallbackDir = File(ESDE_MEDIA_PATH, "$systemName/$mediaType")
            if (fallbackDir.exists() && fallbackDir.isDirectory) {
                val images = fallbackDir.listFiles()?.filter { it.isFile && isImageFile(it) }
                if (!images.isNullOrEmpty()) {
                    val selectedPath =
                        if (useRandom) images.random().absolutePath else images.first().absolutePath
                    systemImageCache[systemName] = selectedPath
                    return selectedPath
                }
            }
        }

        systemImageCache[systemName] = null
        return null
    }

    private fun getSystemLogoPath(systemName: String): String {
        return "$SYSTEM_LOGOS_PATH/$systemName.svg"
    }

    private fun getGameImagePath(
        systemName: String,
        gameFilename: String
    ): String? {
        val preferredType = prefs.state.value.gameImageType

        if (preferredType == GameImageType.None) {
            return null
        }

        val nameOnly = File(gameFilename).nameWithoutExtension

        val preferredFolder = preferredType.folderName
        if (preferredFolder != null) {
            for (ext in IMAGE_EXTENSIONS) {
                val file = File(ESDE_MEDIA_PATH, "$systemName/$preferredFolder/$nameOnly.$ext")
                if (file.exists()) return file.absolutePath
            }
        }

        val fallbackTypes = GAME_IMAGE_FALLBACKS
            .filter { it != preferredFolder }

        for (mediaType in fallbackTypes) {
            for (ext in IMAGE_EXTENSIONS) {
                val file = File(ESDE_MEDIA_PATH, "$systemName/$mediaType/$nameOnly.$ext")
                if (file.exists()) return file.absolutePath
            }
        }

        for (ext in IMAGE_EXTENSIONS) {
            val wheelFile = File(ESDE_MEDIA_PATH, "$systemName/$FOLDER_WHEEL_3D/$nameOnly.$ext")
            if (wheelFile.exists()) return wheelFile.absolutePath
        }

        return null
    }

    private fun getGameMarqueePath(systemName: String, gameFilename: String): String? {
        val nameOnly = File(gameFilename).nameWithoutExtension

        for (ext in IMAGE_EXTENSIONS_WITH_SVG) {
            val file = File(ESDE_MEDIA_PATH, "$systemName/$FOLDER_MARQUEES/$nameOnly.$ext")
            if (file.exists()) return file.absolutePath
        }

        for (dir in MARQUEE_FALLBACK_DIRS) {
            for (ext in IMAGE_EXTENSIONS_WITH_SVG) {
                val file = File(ESDE_MEDIA_PATH, "$systemName/$dir/$nameOnly.$ext")
                if (file.exists()) return file.absolutePath
            }
        }

        return null
    }

    private fun getGameVideoPath(systemName: String, gameFilename: String): String? {
        val nameOnly = File(gameFilename).nameWithoutExtension
        val videoDir = File(ESDE_MEDIA_PATH, "$systemName/$FOLDER_VIDEOS")

        if (!videoDir.exists() || !videoDir.isDirectory) {
            Log.d(TAG, "Video directory does not exist: ${videoDir.absolutePath}")
            return null
        }

        for (ext in VIDEO_EXTENSIONS) {
            val file = File(videoDir, "$nameOnly.$ext")
            if (file.exists()) {
                return file.absolutePath
            }
        }

        val subfolderPath = extractSubfolderPath(gameFilename)
        if (subfolderPath != null) {
            val subDir = File(videoDir, subfolderPath)
            if (subDir.exists() && subDir.isDirectory) {
                for (ext in VIDEO_EXTENSIONS) {
                    val file = File(subDir, "$nameOnly.$ext")
                    if (file.exists()) {
                        return file.absolutePath
                    }
                }
            }
        }

        return null
    }

    private fun extractSubfolderPath(fullPath: String): String? {
        val beforeFilename = fullPath.substringBeforeLast("/", "")
        return beforeFilename.ifEmpty { null }
    }

    private fun isImageFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in IMAGE_EXTENSIONS
    }

    override fun onCleared() {
        super.onCleared()
        cancelPendingVideo()
    }

    companion object {
        private const val TAG = "ESDEViewModel"
    }
}