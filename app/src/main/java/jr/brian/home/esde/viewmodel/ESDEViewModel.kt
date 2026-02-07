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
import jr.brian.home.esde.music.MusicController
import jr.brian.home.esde.music.MusicManager
import jr.brian.home.esde.preferences.ESDEPreferencesManager
import jr.brian.home.esde.preferences.GameImageType
import jr.brian.home.esde.preferences.ScreensaverBehavior
import jr.brian.home.esde.preferences.SystemImageType
import jr.brian.home.esde.setup.SetupPreferences
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_MARQUEES
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_VIDEOS
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_WHEEL_3D
import jr.brian.home.esde.util.ESDEMediaConstants.GAME_IMAGE_FALLBACKS
import jr.brian.home.esde.util.ESDEMediaConstants.IMAGE_EXTENSIONS
import jr.brian.home.esde.util.ESDEMediaConstants.IMAGE_EXTENSIONS_WITH_SVG
import jr.brian.home.esde.util.ESDEMediaConstants.MARQUEE_FALLBACK_DIRS
import jr.brian.home.esde.util.ESDEMediaConstants.SYSTEM_IMAGE_FALLBACKS
import jr.brian.home.esde.util.ESDEMediaConstants.SYSTEM_LOGOS_ASSET_PATH
import jr.brian.home.esde.util.ESDEMediaConstants.VIDEO_EXTENSIONS
import jr.brian.home.esde.util.ESDEMediaConstants.getMediaSystemName
import jr.brian.home.esde.wallpaper.WallpaperState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ESDEViewModel @Inject constructor(
    val prefs: ESDEPreferencesManager,
    private val setupPreferences: SetupPreferences
) : ViewModel() {
    private val systemImageCache = mutableMapOf<String, String?>()

    private val mediaPath: String
        get() = setupPreferences.mediaPath

    private val _wallpaperState = mutableStateOf(createInitialState())
    val wallpaperState: State<WallpaperState> = _wallpaperState

    val musicController: MusicController = MusicManager(prefs)

    private val videoDelayHandler = Handler(Looper.getMainLooper())
    private var pendingVideoRunnable: Runnable? = null
    private var currentSystem: String? = null
    private var currentGameSystem: String? = null
    private var currentGameFilename: String? = null

    init {
        Log.d(TAG, "ESDE Media path configured: $mediaPath")
        Log.d(TAG, "Parent folder (for system_images/system_logos): ${File(mediaPath).parentFile?.absolutePath}")

        prefs.state
            .onEach { prefsState ->
                val currentDimming = if (_wallpaperState.value.isScreensaverActive) {
                    _wallpaperState.value.dimmingLevel
                } else {
                    prefsState.dimmingLevelFloat
                }
                _wallpaperState.value = _wallpaperState.value.copy(
                    dimmingLevel = currentDimming,
                    blurLevel = prefsState.blurLevel.toFloat(),
                    animationStyle = prefsState.animationStyle,
                    animationDuration = prefsState.animationDuration,
                    animationScale = prefsState.animationScale,
                    backgroundColor = Color(prefsState.backgroundColor),
                    videoAudioEnabled = prefsState.videoAudioEnabled,
                    videoDelaySeconds = prefsState.videoDelaySeconds,
                    showSystemLogo = prefsState.showSystemLogo,
                    logoAlignment = prefsState.logoAlignment,
                    hideContentOnVideo = prefsState.hideContentOnVideo,
                    marqueeWidth = prefsState.marqueeWidth,
                    marqueeHeight = prefsState.marqueeHeight,
                    screensaverBehavior = prefsState.screensaverBehavior
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
            hideContentOnVideo = prefsState.hideContentOnVideo,
            marqueeWidth = prefsState.marqueeWidth,
            marqueeHeight = prefsState.marqueeHeight,
            screensaverBehavior = prefsState.screensaverBehavior
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

        musicController.onSystemChanged(systemName)
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

        musicController.onGameSelected(systemName, gameFilename)

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
                musicController.onVideoStarted()
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
        musicController.onVideoEnded()
    }

    fun handleGameStarted() {
        stopVideo()
        musicController.onGameStarted()
        if (prefs.state.value.persistOnGameLaunch) {
            _wallpaperState.value = _wallpaperState.value.copy(
                dimmingLevel = prefs.state.value.dimmingLevelFloat,
                logoBrightness = prefs.state.value.logoBrightnessFloat
            )
        } else {
            _wallpaperState.value = _wallpaperState.value.copy(
                dimmingLevel = 1.0f,
                marqueePath = null
            )
        }
    }

    fun handleGameEnded() {
        _wallpaperState.value = _wallpaperState.value.copy(
            isVideoPlaying = false,
            videoPath = null,
            dimmingLevel = prefs.state.value.dimmingLevelFloat,
            logoBrightness = 1.0f
        )
        musicController.onGameEnded()
    }

    fun handleScreensaverStarted() {
        stopVideo()
        val behavior = prefs.state.value.screensaverBehavior
        
        // For ShowContent: use 80% dimming overlay
        // For PowerOff: don't change dimming - MainActivity will use powerViewModel.powerOff()
        val dimmingLevel = when (behavior) {
            ScreensaverBehavior.ShowContent -> 0.8f
            ScreensaverBehavior.PowerOff -> prefs.state.value.dimmingLevelFloat
        }
        
        _wallpaperState.value = _wallpaperState.value.copy(
            isScreensaverActive = true,
            dimmingLevel = dimmingLevel
        )
        musicController.onScreensaverStarted()
    }

    fun handleScreensaverEnded() {
        _wallpaperState.value = _wallpaperState.value.copy(
            isScreensaverActive = false,
            dimmingLevel = prefs.state.value.dimmingLevelFloat
        )
        musicController.onScreensaverEnded()
    }

    fun updateForScreensaverGame(
        systemName: String,
        gameFilename: String
    ) {
        val behavior = prefs.state.value.screensaverBehavior
        val shouldShowContent = behavior == ScreensaverBehavior.ShowContent
        
        // For ShowContent: use 50% dimming overlay with content visible
        // For PowerOff: use normal dimming - MainActivity handles powerViewModel.powerOff()
        val dimmingLevel = when (behavior) {
            ScreensaverBehavior.ShowContent -> 0.7f
            ScreensaverBehavior.PowerOff -> prefs.state.value.dimmingLevelFloat
        }
        
        _wallpaperState.value = _wallpaperState.value.copy(
            currentImagePath = getGameImagePath(systemName, gameFilename),
            marqueePath = if (shouldShowContent) getGameMarqueePath(systemName, gameFilename) else null,
            dimmingLevel = dimmingLevel,
            isScreensaverActive = true
        )
    }

    private fun getSystemImagePath(systemName: String): String? {
        val prefsState = prefs.state.value
        val systemImageType = prefsState.systemImageType
        val useRandom = prefsState.randomSystemImage
        // Use normalized system name for media lookups (e.g., snes-msu1 -> snes)
        val mediaSystemName = getMediaSystemName(systemName)

        if (systemImageType == SystemImageType.None) {
            return null
        }

        if (systemImageCache.containsKey(systemName)) {
            return systemImageCache[systemName]
        }

        // First check custom system_images path if configured
        val customImagesPath = prefsState.customSystemImagesPath
        if (customImagesPath != null) {
            val customImagesDir = File(customImagesPath)
            if (customImagesDir.exists() && customImagesDir.isDirectory) {
                // Try exact system name first, then fall back to parent system
                for (name in listOf(systemName, mediaSystemName).distinct()) {
                    for (ext in IMAGE_EXTENSIONS) {
                        val customImage = File(customImagesDir, "$name.$ext")
                        if (customImage.exists()) {
                            Log.d(TAG, "Found custom system image: ${customImage.absolutePath}")
                            systemImageCache[systemName] = customImage.absolutePath
                            return customImage.absolutePath
                        }
                    }
                }
            }
        }

        // This contains system-level background images like n64.png, snes.png, etc.
        val systemImagesDir = File(mediaPath, "system_images")
        if (systemImagesDir.exists() && systemImagesDir.isDirectory) {
            // Try exact system name first, then fall back to parent system
            for (name in listOf(systemName, mediaSystemName).distinct()) {
                for (ext in IMAGE_EXTENSIONS) {
                    val systemImage = File(systemImagesDir, "$name.$ext")
                    if (systemImage.exists()) {
                        Log.d(TAG, "Found system image: ${systemImage.absolutePath}")
                        systemImageCache[systemName] = systemImage.absolutePath
                        return systemImage.absolutePath
                    }
                }
            }
        }

        // Then check in downloaded_media/<system>/<imageType>/ folders
        val preferredFolder = systemImageType.folderName ?: return null
        // Try both exact system name and parent system for game media folders
        for (name in listOf(systemName, mediaSystemName).distinct()) {
            val mediaDir = File(mediaPath, "$name/$preferredFolder")
            if (mediaDir.exists() && mediaDir.isDirectory) {
                val images = mediaDir.listFiles()?.filter { it.isFile && isImageFile(it) }
                if (!images.isNullOrEmpty()) {
                    val selectedPath =
                        if (useRandom) images.random().absolutePath else images.first().absolutePath
                    systemImageCache[systemName] = selectedPath
                    return selectedPath
                }
            }
        }

        val fallbackTypes = SYSTEM_IMAGE_FALLBACKS
            .filter { it != preferredFolder }

        for (mediaType in fallbackTypes) {
            for (name in listOf(systemName, mediaSystemName).distinct()) {
                val fallbackDir = File(mediaPath, "$name/$mediaType")
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
        }

        systemImageCache[systemName] = null
        return null
    }

    private fun getSystemLogoPath(systemName: String): String? {
        val prefsState = prefs.state.value
        // Use normalized system name for media lookups (e.g., snes-msu1 -> snes)
        val mediaSystemName = getMediaSystemName(systemName)
        
        // First check custom system_logos path if configured
        val customLogosPath = prefsState.customSystemLogosPath
        if (customLogosPath != null) {
            val customLogosDir = File(customLogosPath)
            if (customLogosDir.exists() && customLogosDir.isDirectory) {
                // Try exact system name first, then fall back to parent system
                for (name in listOf(systemName, mediaSystemName).distinct()) {
                    for (ext in IMAGE_EXTENSIONS_WITH_SVG) {
                        val customLogo = File(customLogosDir, "$name.$ext")
                        if (customLogo.exists()) {
                            Log.d(TAG, "Found custom system logo: ${customLogo.absolutePath}")
                            return customLogo.absolutePath
                        }
                    }
                }
            }
        }
        
        // e.g. downloaded_media/system_logos/n64.svg
        val userLogosDir = File(mediaPath, "system_logos")
        if (userLogosDir.exists() && userLogosDir.isDirectory) {
            // Try exact system name first, then fall back to parent system
            for (name in listOf(systemName, mediaSystemName).distinct()) {
                for (ext in IMAGE_EXTENSIONS_WITH_SVG) {
                    val userLogo = File(userLogosDir, "$name.$ext")
                    if (userLogo.exists()) {
                        Log.d(TAG, "Found user system logo: ${userLogo.absolutePath}")
                        return userLogo.absolutePath
                    }
                }
            }
        }

        // Fall back to bundled asset logos - use parent system name if available
        // since bundled assets typically only have the main system logos (e.g., snes.svg not snes-msu1.svg)
        return "$SYSTEM_LOGOS_ASSET_PATH/$mediaSystemName.svg"
    }

    private fun getGameImagePath(
        systemName: String,
        gameFilename: String
    ): String? {
        val preferredType = prefs.state.value.gameImageType
        // Use normalized system name for media lookups (e.g., snes-msu1 -> snes)
        val mediaSystemName = getMediaSystemName(systemName)

        if (preferredType == GameImageType.None) {
            return null
        }

        val nameOnly = File(gameFilename).nameWithoutExtension

        val preferredFolder = preferredType.folderName
        if (preferredFolder != null) {
            // Try both exact system name and parent system
            for (name in listOf(systemName, mediaSystemName).distinct()) {
                for (ext in IMAGE_EXTENSIONS) {
                    val file = File(mediaPath, "$name/$preferredFolder/$nameOnly.$ext")
                    if (file.exists()) return file.absolutePath
                }
            }
        }

        val fallbackTypes = GAME_IMAGE_FALLBACKS
            .filter { it != preferredFolder }

        for (mediaType in fallbackTypes) {
            for (name in listOf(systemName, mediaSystemName).distinct()) {
                for (ext in IMAGE_EXTENSIONS) {
                    val file = File(mediaPath, "$name/$mediaType/$nameOnly.$ext")
                    if (file.exists()) return file.absolutePath
                }
            }
        }

        for (name in listOf(systemName, mediaSystemName).distinct()) {
            for (ext in IMAGE_EXTENSIONS) {
                val wheelFile = File(mediaPath, "$name/$FOLDER_WHEEL_3D/$nameOnly.$ext")
                if (wheelFile.exists()) return wheelFile.absolutePath
            }
        }

        return null
    }

    private fun getGameMarqueePath(systemName: String, gameFilename: String): String? {
        val nameOnly = File(gameFilename).nameWithoutExtension
        // Use normalized system name for media lookups (e.g., snes-msu1 -> snes)
        val mediaSystemName = getMediaSystemName(systemName)

        for (name in listOf(systemName, mediaSystemName).distinct()) {
            for (ext in IMAGE_EXTENSIONS_WITH_SVG) {
                val file = File(mediaPath, "$name/$FOLDER_MARQUEES/$nameOnly.$ext")
                if (file.exists()) return file.absolutePath
            }
        }

        for (dir in MARQUEE_FALLBACK_DIRS) {
            for (name in listOf(systemName, mediaSystemName).distinct()) {
                for (ext in IMAGE_EXTENSIONS_WITH_SVG) {
                    val file = File(mediaPath, "$name/$dir/$nameOnly.$ext")
                    if (file.exists()) return file.absolutePath
                }
            }
        }

        return null
    }

    private fun getGameVideoPath(systemName: String, gameFilename: String): String? {
        val nameOnly = File(gameFilename).nameWithoutExtension
        // Use normalized system name for media lookups (e.g., snes-msu1 -> snes)
        val mediaSystemName = getMediaSystemName(systemName)

        for (name in listOf(systemName, mediaSystemName).distinct()) {
            val videoDir = File(mediaPath, "$name/$FOLDER_VIDEOS")

            if (!videoDir.exists() || !videoDir.isDirectory) {
                Log.d(TAG, "Video directory does not exist: ${videoDir.absolutePath}")
                continue
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
        musicController.release()
    }

    companion object {
        private const val TAG = "ESDEViewModel"
    }
}