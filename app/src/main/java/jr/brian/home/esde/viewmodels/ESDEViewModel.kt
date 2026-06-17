package jr.brian.home.esde.viewmodels

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.data.BgMusicManager
import jr.brian.home.data.ESDECleanupManager
import jr.brian.home.esde.data.MusicController
import jr.brian.home.esde.data.MusicManager
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.model.GameImageType
import jr.brian.home.esde.model.OverlayMediaType
import jr.brian.home.esde.model.ScreensaverBehavior
import jr.brian.home.esde.model.SystemImageType
import jr.brian.home.esde.data.SetupPreferences
import jr.brian.home.esde.data.ESDECleanupHelper
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
import jr.brian.home.esde.util.GamelistParser
import jr.brian.home.esde.model.WallpaperState
import jr.brian.home.model.VideoLaunchEvent
import jr.brian.home.model.state.DeleteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import androidx.core.net.toUri
import jr.brian.home.esde.model.SystemLaunchTrigger
import jr.brian.home.esde.model.GameKonfettiEvent
import jr.brian.home.ui.components.konfetti.KonfettiTrigger


@HiltViewModel
class ESDEViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    val prefs: ESDEPreferencesManager,
    private val setupPreferences: SetupPreferences,
    private val cleanupHelper: ESDECleanupHelper,
    val cleanupManager: ESDECleanupManager,
    private val bgMusicManager: BgMusicManager
) : ViewModel() {
    private val systemImageCache = mutableMapOf<String, String?>()

    private val mediaPath: String
        get() = prefs.state.value.customMediaPath ?: setupPreferences.mediaPath

    private val esdeRootPath: String?
        get() {
            val scriptsPath = setupPreferences.scriptsPath
            return File(scriptsPath).parentFile?.absolutePath
        }

    private val _wallpaperState = mutableStateOf(createInitialState())
    val wallpaperState: State<WallpaperState> = _wallpaperState

    private val _videoLaunchEvent = MutableSharedFlow<VideoLaunchEvent>()
    val videoLaunchEvent: SharedFlow<VideoLaunchEvent> = _videoLaunchEvent.asSharedFlow()

    private val _scanComplete = MutableSharedFlow<Unit>()
    val scanComplete: SharedFlow<Unit> = _scanComplete.asSharedFlow()

    private val _gameKonfettiEvent = MutableSharedFlow<GameKonfettiEvent>()
    val gameKonfettiEvent: SharedFlow<GameKonfettiEvent> = _gameKonfettiEvent.asSharedFlow()

    val musicController: MusicController = MusicManager(context, prefs, bgMusicManager)

    private val videoDelayHandler = Handler(Looper.getMainLooper())
    private var pendingVideoRunnable: Runnable? = null
    var currentSystem: String? = null
        private set
    private var currentGameSystem: String? = null
    private var currentGameFilename: String? = null

    private val _deleteEmptyFoldersResult = mutableStateOf<DeleteResult?>(null)
    val deleteEmptyFoldersResult: State<DeleteResult?> = _deleteEmptyFoldersResult

    private var isViewingGame: Boolean = false
    private var isLauncherActive: Boolean = true
    private var romSearchGameLaunched: Boolean = false

    init {
        Log.d(TAG, "ESDE Media path configured: $mediaPath")
        Log.d(
            TAG,
            "Parent folder (for system_images/system_logos): ${File(mediaPath).parentFile?.absolutePath}"
        )

        prefs.state
            .onEach { prefsState ->
                val currentDimming = if (isViewingGame) {
                    prefsState.gameBackgroundDimmingFloat
                } else {
                    prefsState.systemBackgroundDimmingFloat
                }
                val currentBlur = if (isViewingGame) {
                    prefsState.gameBlurLevel.toFloat()
                } else {
                    prefsState.systemBlurLevel.toFloat()
                }
                _wallpaperState.value = _wallpaperState.value.copy(
                    dimmingLevel = currentDimming,
                    blurLevel = currentBlur,
                    animationStyle = prefsState.animationStyle,
                    animationDuration = prefsState.animationDuration,
                    animationScale = prefsState.animationScale,
                    backgroundColor = Color(prefsState.backgroundColor),
                    videoAudioEnabled = prefsState.videoAudioEnabled,
                    videoDelaySeconds = prefsState.videoDelaySeconds,
                    systemBgVideoMuted = prefsState.systemBgVideoMuted,
                    systemBgVideoLooping = prefsState.systemBgVideoLooping,
                    showSystemLogo = prefsState.showSystemLogo,
                    logoAlignment = prefsState.logoAlignment,
                    marqueeWidth = prefsState.marqueeWidth,
                    marqueeHeight = prefsState.marqueeHeight,
                    screensaverBehavior = prefsState.screensaverBehavior
                )
            }.launchIn(viewModelScope)
    }

    fun refreshSystemImage() {
        systemImageCache.clear()

        val prefsState = prefs.state.value

        val singlePath = prefsState.singleSystemImagePath
        if (singlePath != null) {
            val isVideo = isVideoPath(singlePath)
            _wallpaperState.value = _wallpaperState.value.copy(
                currentImagePath = if (isVideo) null else singlePath,
                systemBackgroundVideoPath = if (isVideo) singlePath else null
            )
            return
        }

        val systemName = prefsState.lastSelectedSystem ?: return
        val newPath = when (prefsState.systemImageType) {
            SystemImageType.None -> null
            else -> getSystemImagePath(systemName)
        }

        val isVideo = newPath != null && isVideoPath(newPath)
        _wallpaperState.value = _wallpaperState.value.copy(
            currentImagePath = if (isVideo) null else newPath,
            systemBackgroundVideoPath = if (isVideo) newPath else null
        )
    }

    private fun createInitialState(): WallpaperState {
        val prefsState = prefs.state.value
        val lastSystem = prefsState.lastSelectedSystem

        val singlePath = prefsState.singleSystemImagePath
        val initialPath = when {
            singlePath != null -> singlePath
            prefsState.systemImageType == SystemImageType.None -> null
            else -> lastSystem?.let { getSystemImagePath(it) }
        }

        val isVideo = initialPath != null && isVideoPath(initialPath)

        return WallpaperState(
            currentImagePath = if (isVideo) null else initialPath,
            systemBackgroundVideoPath = if (isVideo) initialPath else null,
            dimmingLevel = prefsState.systemBackgroundDimmingFloat,
            blurLevel = prefsState.systemBlurLevel.toFloat(),
            animationStyle = prefsState.animationStyle,
            animationDuration = prefsState.animationDuration,
            animationScale = prefsState.animationScale,
            backgroundColor = Color(prefsState.backgroundColor),
            videoAudioEnabled = prefsState.videoAudioEnabled,
            videoDelaySeconds = prefsState.videoDelaySeconds,
            systemBgVideoMuted = prefsState.systemBgVideoMuted,
            systemBgVideoLooping = prefsState.systemBgVideoLooping,
            logoPath = lastSystem?.let { getSystemLogoPath(it) },
            showSystemLogo = prefsState.showSystemLogo,
            logoAlignment = prefsState.logoAlignment,
            logoOffsetX = prefsState.logoOffsetX,
            logoOffsetY = prefsState.logoOffsetY,
            marqueeWidth = prefsState.marqueeWidth,
            marqueeHeight = prefsState.marqueeHeight,
            screensaverBehavior = prefsState.screensaverBehavior
        )
    }

    fun updateForSystem(systemName: String) {
        if (currentSystem == systemName) return

        stopVideo()
        currentSystem = systemName
        isViewingGame = false
        romSearchGameLaunched = false
        systemImageCache.remove(systemName)

        prefs.setLastSelectedSystem(systemName)

        val systemPath = getSystemImagePath(systemName)
        val isVideo = systemPath != null && isVideoPath(systemPath)

        _wallpaperState.value = _wallpaperState.value.copy(
            currentImagePath = if (isVideo) null else systemPath,
            systemBackgroundVideoPath = if (isVideo) systemPath else null,
            isVideoPlaying = false,
            videoPath = null,
            isGameRunning = false,
            logoPath = getSystemLogoPath(systemName),
            gameDescription = null,
            blurLevel = prefs.state.value.systemBlurLevel.toFloat(),
            dimmingLevel = prefs.state.value.systemBackgroundDimmingFloat,
            isShowingGameBackground = false
        )

        musicController.onSystemChanged(systemName)
    }

    fun updateForGame(
        systemName: String,
        gameFilename: String
    ) {
        if (_wallpaperState.value.isGameRunning) {
            // ES-DE is sending browse events so no game is actually running anymore.
            // Clear stale ROM-search game state so we don't block the UI update.
            romSearchGameLaunched = false
        }

        cancelPendingVideo()

        currentSystem = null
        currentGameSystem = systemName
        currentGameFilename = gameFilename
        isViewingGame = true

        val gameDescription = esdeRootPath?.let { rootPath ->
            GamelistParser.getGameDescription(
                esdeRootPath = rootPath,
                systemName = systemName,
                gameFilename = gameFilename
            )
        }

        _wallpaperState.value = _wallpaperState.value.copy(
            currentImagePath = getGameImagePath(systemName, gameFilename),
            systemBackgroundVideoPath = null,
            isVideoPlaying = false,
            videoPath = null,
            isGameRunning = false,
            logoPath = getGameMarqueePath(systemName, gameFilename),
            gameDescription = gameDescription,
            blurLevel = prefs.state.value.gameBlurLevel.toFloat(),
            dimmingLevel = prefs.state.value.gameBackgroundDimmingFloat,
            isShowingGameBackground = true
        )

        musicController.onGameSelected(systemName, gameFilename)

        viewModelScope.launch {
            _gameKonfettiEvent.emit(
                GameKonfettiEvent(gameFilename, KonfettiTrigger.GAME_SELECT)
            )
        }

        if (prefs.state.value.videoEnabled) {
            scheduleVideoPlayback(systemName, gameFilename)
        }
    }

    private fun scheduleVideoPlayback(systemName: String, gameFilename: String) {
        if (_wallpaperState.value.isGameRunning) {
            Log.d(TAG, "Not scheduling video - game is currently running")
            return
        }

        val videoPath = getGameVideoPath(systemName, gameFilename)
        if (videoPath == null) {
            Log.d(TAG, "No video found for: $systemName / $gameFilename")
            return
        }

        val delayMs = prefs.state.value.videoDelaySeconds * 1000L
        Log.d(TAG, "Scheduling video playback in ${delayMs}ms")

        pendingVideoRunnable = Runnable {
            if (currentGameSystem == systemName && currentGameFilename == gameFilename && isLauncherActive && !_wallpaperState.value.isGameRunning) {
                viewModelScope.launch {
                    _videoLaunchEvent.emit(
                        VideoLaunchEvent(
                            videoPath = videoPath,
                            audioEnabled = prefs.state.value.videoAudioEnabled,
                            scaleMode = prefs.state.value.videoScaleMode,
                            overlayEnabled = prefs.state.value.videoOverlayEnabled
                        )
                    )
                }
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

    /**
     * Called when the VideoPlayerActivity finishes (user tapped or pressed back).
     * Restores music playback state.
     */
    fun onVideoActivityFinished() {
        musicController.onVideoEnded()
    }

    /**
     * Sets whether the launcher screen is currently active.
     * When navigating away from the launcher (e.g., to settings),
     * call this with false to prevent videos from launching.
     */
    fun setLauncherActive(active: Boolean) {
        isLauncherActive = active
        if (!active) {
            cancelPendingVideo()
        }
    }

    fun handleRomSearchGameStarted() {
        romSearchGameLaunched = true
        handleGameStarted()
    }

    fun handleLauncherResumed() {
        if (romSearchGameLaunched) {
            romSearchGameLaunched = false
            handleGameEnded()
        }
    }

    fun handleGameStarted(gameFilename: String? = null) {
        if (gameFilename != null) {
            viewModelScope.launch {
                _gameKonfettiEvent.emit(
                    GameKonfettiEvent(gameFilename, KonfettiTrigger.GAME_START)
                )
            }
        }
        stopVideo()
        currentGameSystem = null
        currentGameFilename = null
        musicController.onGameStarted()
        if (prefs.state.value.persistOnGameLaunch) {
            val backgroundDimming = 1f - prefs.state.value.persistBackgroundBrightnessFloat
            _wallpaperState.value = _wallpaperState.value.copy(
                isGameRunning = true,
                dimmingLevel = backgroundDimming,
                logoBrightness = prefs.state.value.persistLogoBrightnessFloat
            )
        } else {
            _wallpaperState.value = _wallpaperState.value.copy(
                isGameRunning = true,
                dimmingLevel = 0.7f,
                logoPath = null
            )
        }
    }

    fun handleGameEnded() {
        val dimmingLevel = if (isViewingGame) {
            prefs.state.value.gameBackgroundDimmingFloat
        } else {
            prefs.state.value.systemBackgroundDimmingFloat
        }
        _wallpaperState.value = _wallpaperState.value.copy(
            isGameRunning = false,
            isVideoPlaying = false,
            videoPath = null,
            dimmingLevel = dimmingLevel,
            logoBrightness = 1f
        )
        musicController.onGameEnded()
    }

    fun handleScreensaverStarted() {
        val behavior = prefs.state.value.screensaverBehavior
        if (behavior == ScreensaverBehavior.ShowAll) return

        stopVideo()

        val baseDimming = if (isViewingGame) {
            prefs.state.value.gameBackgroundDimmingFloat
        } else {
            prefs.state.value.systemBackgroundDimmingFloat
        }

        val dimmingLevel = when (behavior) {
            ScreensaverBehavior.ShowContent -> 0.7f
            ScreensaverBehavior.PowerOff -> baseDimming
            ScreensaverBehavior.Floaty -> 0.7f
            ScreensaverBehavior.ShowAll -> baseDimming
        }

        _wallpaperState.value = _wallpaperState.value.copy(
            isScreensaverActive = true,
            dimmingLevel = dimmingLevel
        )
        musicController.onScreensaverStarted()
    }

    fun handleScreensaverEnded() {
        val dimmingLevel = if (isViewingGame) {
            prefs.state.value.gameBackgroundDimmingFloat
        } else {
            prefs.state.value.systemBackgroundDimmingFloat
        }
        // Reset currentSystem so the next onSystemSelected event forces a full wallpaper
        // refresh. Without this, if the screensaver displayed a game with no artwork
        // (setting currentImagePath = null), the guard in updateForSystem would skip the
        // update and leave a black screen until the user manually navigated away and back.
        currentSystem = null
        _wallpaperState.value = _wallpaperState.value.copy(
            isScreensaverActive = false,
            dimmingLevel = dimmingLevel
        )
        musicController.onScreensaverEnded()
    }

    fun updateForScreensaverGame(
        systemName: String,
        gameFilename: String
    ) {
        val behavior = prefs.state.value.screensaverBehavior
        val shouldShowContent = behavior != ScreensaverBehavior.PowerOff

        val baseDimming = prefs.state.value.gameBackgroundDimmingFloat

        val dimmingLevel = when (behavior) {
            ScreensaverBehavior.ShowAll -> 0f
            ScreensaverBehavior.ShowContent -> 0.7f
            ScreensaverBehavior.PowerOff -> baseDimming
            ScreensaverBehavior.Floaty -> 0.7f
        }

        _wallpaperState.value = _wallpaperState.value.copy(
            currentImagePath = getGameImagePath(systemName, gameFilename),
            systemBackgroundVideoPath = null,
            logoPath = if (shouldShowContent) getGameMarqueePath(
                systemName,
                gameFilename
            ) else null,
            dimmingLevel = dimmingLevel,
            isScreensaverActive = true,
            isShowingGameBackground = true
        )
    }

    private fun getSystemImagePath(systemName: String): String? {
        val prefsState = prefs.state.value
        val systemImageType = prefsState.systemImageType
        val useRandom = prefsState.randomSystemImage
        val mediaSystemName = getMediaSystemName(systemName)

        val singleImagePath = prefsState.singleSystemImagePath
        if (singleImagePath != null) {
            if (singleImagePath.startsWith("content://")) {
                systemImageCache[systemName] = singleImagePath
                return singleImagePath
            } else {
                val singleImageFile = File(singleImagePath)
                if (singleImageFile.exists() && singleImageFile.isFile) {
                    systemImageCache[systemName] = singleImageFile.absolutePath
                    return singleImageFile.absolutePath
                }
            }
        }

        // Check custom folder before the systemImageType gate so it always takes priority
        val customImagesPath = prefsState.customSystemImagesPath
        if (customImagesPath != null) {
            val found = findInCustomImagesFolder(customImagesPath, systemName, mediaSystemName)
            if (found != null) {
                Log.d(TAG, "Found custom system image: $found")
                systemImageCache[systemName] = found
                return found
            }
        }

        if (systemImageType == SystemImageType.None) {
            return null
        }

        if (systemImageCache.containsKey(systemName)) {
            return systemImageCache[systemName]
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

        if (systemImageType == SystemImageType.All) {
            val allImages = mutableListOf<File>()
            for (type in SystemImageType.randomizableTypes()) {
                for (name in listOf(systemName, mediaSystemName).distinct()) {
                    val mediaDir = File(mediaPath, "$name/${type.folderName}")
                    if (mediaDir.exists() && mediaDir.isDirectory) {
                        mediaDir.listFiles()?.filter { it.isFile && isImageFile(it) }
                            ?.let { allImages.addAll(it) }
                    }
                }
            }
            if (allImages.isNotEmpty()) {
                val selectedPath = allImages.random().absolutePath
                systemImageCache[systemName] = selectedPath
                return selectedPath
            }
            systemImageCache[systemName] = null
            return null
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
        val mediaSystemName = getMediaSystemName(systemName)

        val singleLogoPath = prefsState.singleSystemLogoPath
        if (singleLogoPath != null) {
            if (singleLogoPath.startsWith("content://")) {
                return singleLogoPath
            } else {
                val singleLogoFile = File(singleLogoPath)
                if (singleLogoFile.exists() && singleLogoFile.isFile) {
                    return singleLogoFile.absolutePath
                }
            }
        }

        val customLogosPath = prefsState.customSystemLogosPath
        if (customLogosPath != null) {
            val customLogosDir = File(customLogosPath)
            if (customLogosDir.exists() && customLogosDir.isDirectory) {
                // Try exact system name first, then fall back to parent system
                for (name in listOf(systemName, mediaSystemName).distinct()) {
                    for (ext in LOGO_EXTENSIONS) {
                        val customLogo = File(customLogosDir, "$name.$ext")
                        if (customLogo.exists()) {
                            Log.d(TAG, "Found custom system logo: ${customLogo.absolutePath}")
                            return customLogo.absolutePath
                        }
                    }
                }
            }
        }

        // e.g. downloaded_media/system_logos/n64.svg or n64.mp4
        val userLogosDir = File(mediaPath, "system_logos")
        if (userLogosDir.exists() && userLogosDir.isDirectory) {
            // Try exact system name first, then fall back to parent system
            for (name in listOf(systemName, mediaSystemName).distinct()) {
                for (ext in LOGO_EXTENSIONS) {
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
        val prefsState = prefs.state.value
        val preferredType = prefsState.gameImageType
        val mediaSystemName = getMediaSystemName(systemName)

        // Check for single game image first (takes priority over image type)
        val singleGameImagePath = prefsState.singleGameImagePath
        if (singleGameImagePath != null) {
            if (singleGameImagePath.startsWith("content://")) {
                return singleGameImagePath
            } else {
                val singleGameImageFile = File(singleGameImagePath)
                if (singleGameImageFile.exists() && singleGameImageFile.isFile) {
                    return singleGameImageFile.absolutePath
                }
            }
        }

        if (preferredType == GameImageType.None) {
            return null
        }

        val nameOnly = File(gameFilename).nameWithoutExtension
        val parentDir = File(gameFilename).parent
        val systemNames = listOf(systemName, mediaSystemName).distinct()

        fun candidateFiles(folder: String, ext: String): List<File> {
            val files = mutableListOf<File>()
            for (name in systemNames) {
                if (parentDir != null) {
                    files += File(mediaPath, "$name/$folder/$parentDir/$nameOnly.$ext")
                }
                files += File(mediaPath, "$name/$folder/$nameOnly.$ext")
            }
            return files
        }

        // Handle "All" type by randomly selecting from all available media types
        if (preferredType == GameImageType.All) {
            val allImages = mutableListOf<File>()
            for (type in GameImageType.randomizableTypes()) {
                val folder = type.folderName ?: continue
                for (ext in IMAGE_EXTENSIONS) {
                    allImages += candidateFiles(folder, ext).filter { it.exists() }
                }
            }
            return if (allImages.isNotEmpty()) allImages.random().absolutePath else null
        }

        val preferredFolder = preferredType.folderName
        if (preferredFolder != null) {
            for (ext in IMAGE_EXTENSIONS) {
                candidateFiles(preferredFolder, ext).firstOrNull { it.exists() }?.let { return it.absolutePath }
            }
        }

        val fallbackTypes = GAME_IMAGE_FALLBACKS
            .filter { it != preferredFolder }

        for (mediaType in fallbackTypes) {
            for (ext in IMAGE_EXTENSIONS) {
                candidateFiles(mediaType, ext).firstOrNull { it.exists() }?.let { return it.absolutePath }
            }
        }

        for (ext in IMAGE_EXTENSIONS) {
            candidateFiles(FOLDER_WHEEL_3D, ext).firstOrNull { it.exists() }?.let { return it.absolutePath }
        }

        return null
    }

    private fun getGameMarqueePath(systemName: String, gameFilename: String): String? {
        val prefsState = prefs.state.value
        val mediaSystemName = getMediaSystemName(systemName)

        val singleGameLogoPath = prefsState.singleGameLogoPath
        if (singleGameLogoPath != null) {
            if (singleGameLogoPath.startsWith("content://")) {
                return singleGameLogoPath
            } else {
                val singleGameLogoFile = File(singleGameLogoPath)
                if (singleGameLogoFile.exists() && singleGameLogoFile.isFile) {
                    return singleGameLogoFile.absolutePath
                }
            }
        }

        val nameOnly = File(gameFilename).nameWithoutExtension
        val parentDir = File(gameFilename).parent
        val systemNames = listOf(systemName, mediaSystemName).distinct()

        // Get the selected overlay media type from preferences
        val overlayMediaType = prefsState.overlayMediaType
        val primaryFolder = overlayMediaType.folderName

        // Helper to check file existence for a given folder path
        fun checkFile(path: String): String? {
            val f = File(mediaPath, path)
            return if (f.exists()) f.absolutePath else null
        }

        fun lookupMarquee(folder: String): String? {
            for (name in systemNames) {
                for (ext in LOGO_EXTENSIONS) {
                    if (parentDir != null) {
                        checkFile("$name/$folder/$parentDir/$nameOnly.$ext")?.let { return it }
                        checkFile("$name/$folder/$parentDir/$nameOnly.scummvm.$ext")?.let { return it }
                    }
                    checkFile("$name/$folder/$nameOnly.$ext")?.let { return it }
                    // Also check for .scummvm suffix (for ScummVM games)
                    checkFile("$name/$folder/$nameOnly.scummvm.$ext")?.let { return it }
                }
            }
            return null
        }

        // Try the primary selected media folder first
        lookupMarquee(primaryFolder)?.let { return it }

        // If not found and using non-default folder, fall back to marquees
        if (overlayMediaType != OverlayMediaType.Marquees) {
            lookupMarquee(FOLDER_MARQUEES)?.let { return it }
        }

        // Fall back to wheel directories
        for (dir in MARQUEE_FALLBACK_DIRS) {
            lookupMarquee(dir)?.let { return it }
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

    private fun findInCustomImagesFolder(
        customImagesPath: String,
        systemName: String,
        mediaSystemName: String
    ): String? {
        val names = listOf(systemName, mediaSystemName).distinct()
        val extensions = IMAGE_EXTENSIONS + VIDEO_EXTENSIONS
        return if (customImagesPath.startsWith("content://")) {
            val treeDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(
                context, customImagesPath.toUri()
            )
            if (treeDir?.exists() == true && treeDir.isDirectory) {
                for (name in names) {
                    for (ext in extensions) {
                        val file = treeDir.findFile("$name.$ext")
                        if (file?.exists() == true) return file.uri.toString()
                    }
                }
            }
            null
        } else {
            val dir = File(customImagesPath)
            if (dir.exists() && dir.isDirectory) {
                for (name in names) {
                    for (ext in extensions) {
                        val file = File(dir, "$name.$ext")
                        if (file.exists()) return file.absolutePath
                    }
                }
            }
            null
        }
    }

    private fun isImageFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in IMAGE_EXTENSIONS
    }

    private fun isVideoPath(path: String): Boolean {
        // Check by file extension first
        val ext = path.substringAfterLast('.', "").lowercase()
        if (ext in VIDEO_EXTENSIONS) return true

        // For content:// URIs, check the MIME type
        if (path.startsWith("content://")) {
            return try {
                val uri = path.toUri()
                val mimeType = context.contentResolver.getType(uri)
                mimeType?.startsWith("video/") == true
            } catch (_: Exception) {
                false
            }
        }

        return false
    }


    fun deleteEmptyESDEFolders() {
        viewModelScope.launch {
            val result = cleanupHelper.deleteEmptyESDEFolders()
            _deleteEmptyFoldersResult.value = result
        }
    }

    fun clearDeleteResult() {
        _deleteEmptyFoldersResult.value = null
    }

    fun scanRomsFolders() {
        val romsPaths = prefs.state.value.romsPaths
        if (romsPaths.isEmpty()) return
        viewModelScope.launch {
            val subDirs = withContext(Dispatchers.IO) {
                romsPaths.flatMap { path ->
                    File(path).listFiles()?.filter { it.isDirectory }?.map { it.name }
                        ?: emptyList()
                }.distinct()
            }
            val existing = prefs.state.value.systemAppMap.toMutableMap()
            subDirs.forEach { folderName ->
                if (!existing.containsKey(folderName)) {
                    existing[folderName] = null
                }
            }
            prefs.setSystemAppMap(existing)
            _scanComplete.emit(Unit)
        }
    }

    fun setSystemApp(systemFolderName: String, packageName: String?) {
        val updated = prefs.state.value.systemAppMap.toMutableMap()
        updated[systemFolderName] = packageName
        prefs.setSystemAppMap(updated)
    }

    fun removeSystemEntry(systemFolderName: String) {
        val updated = prefs.state.value.systemAppMap.toMutableMap()
        updated.remove(systemFolderName)
        prefs.setSystemAppMap(updated)
        // Also clean up launch trigger if it was set
        prefs.setSystemLaunchTrigger(systemFolderName, SystemLaunchTrigger.NoAction)
    }

    override fun onCleared() {
        super.onCleared()
        cancelPendingVideo()
        musicController.release()
    }

    companion object {
        private const val TAG = "ESDEViewModel"
        private val LOGO_EXTENSIONS: List<String> = IMAGE_EXTENSIONS_WITH_SVG + VIDEO_EXTENSIONS
    }
}