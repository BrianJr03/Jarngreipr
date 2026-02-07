package jr.brian.home.esde.music

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import jr.brian.home.esde.preferences.ESDEPreferencesManager
import jr.brian.home.esde.preferences.MusicVideoBehavior
import java.io.File

/**
 * ═══════════════════════════════════════════════════════════
 * MUSIC MANAGER
 * ═══════════════════════════════════════════════════════════
 * Manages background music playback for ES-DE integration.
 *
 * FEATURES:
 * - State-based music playback (system/game/screensaver)
 * - System-specific music support with fallback to generic
 * - Cross-fade transitions between sources
 * - Video ducking/pausing behavior
 * - Playlist management with shuffle
 * - Lifecycle-aware playback (activity visibility)
 *
 * MUSIC FOLDER STRUCTURE:
 * /storage/emulated/0/ES-DE Jarngreipr/music/
 *   ├── song1.mp3 (generic music)
 *   ├── song2.ogg
 *   └── systems/
 *       ├── snes/
 *       │   ├── snes-theme.mp3
 *       │   └── snes-battle.ogg
 *       └── arcade/
 *           └── arcade-music.mp3
 * ═══════════════════════════════════════════════════════════
 */
class MusicManager(
    private val prefsManager: ESDEPreferencesManager
) : MusicController {

    companion object {
        private const val TAG = "MusicManager"

        private const val DEFAULT_MUSIC_PATH = "/storage/emulated/0/ES-DE Jarngreipr/music"
        private val AUDIO_EXTENSIONS = listOf("mp3", "ogg", "flac", "m4a", "wav", "aac")

        private const val CROSS_FADE_DURATION = 300L
        private const val DUCK_FADE_DURATION = 300L

        private const val NORMAL_VOLUME = 1.0f
        private const val DUCKED_VOLUME = 0.2f
    }

    // ========== PLAYBACK STATE ==========

    private var musicPlayer: MediaPlayer? = null
    private var currentMusicSource: MusicSource? = null
    private var currentPlaylist: List<File> = emptyList()
    private var currentTrackIndex: Int = 0
    private var currentVolume: Float = NORMAL_VOLUME
    private var targetVolume: Float = NORMAL_VOLUME

    // Video interaction state
    private var isMusicDucked: Boolean = false
    private var wasMusicPausedForVideo: Boolean = false

    // Handler for volume fades and track transitions
    private val handler = Handler(Looper.getMainLooper())
    private var volumeFadeRunnable: Runnable? = null

    // Track the last system state to detect source changes
    private var lastSystemName: String? = null
    private var isInGameBrowsing: Boolean = false

    // Track if music is actually playing (stopped during GamePlaying)
    private var isMusicPlaying: Boolean = false

    // Track if activity is visible (onStart/onStop lifecycle)
    private var isActivityVisible: Boolean = true

    // Track if music was playing before becoming invisible
    private var wasMusicPlayingBeforeInvisible: Boolean = false

    init {
        android.util.Log.d(TAG, "MusicManager initialized")
        android.util.Log.d(TAG, "Base music path: ${getMusicPath()}")
    }

    // ========== PATH MANAGEMENT ==========

    /**
     * Get the music path from preferences, or use default.
     */
    private fun getMusicPath(): String {
        return prefsManager.state.value.musicPath ?: DEFAULT_MUSIC_PATH
    }

    // ========== MUSICCONTROLLER INTERFACE IMPLEMENTATION ==========

    override fun onSystemChanged(systemName: String?) {
        android.util.Log.d(TAG, "━━━ SYSTEM CHANGED: $systemName ━━━")

        if (!isActivityVisible) {
            android.util.Log.d(TAG, "Music blocked - activity not visible")
            lastSystemName = systemName
            isInGameBrowsing = false
            return
        }

        if (!isMusicEnabled()) {
            android.util.Log.d(TAG, "Music disabled globally")
            stopMusic()
            lastSystemName = systemName
            isInGameBrowsing = false
            return
        }

        // Check if system music is enabled
        if (!prefsManager.state.value.musicSystemEnabled) {
            android.util.Log.d(TAG, "System music disabled")
            stopMusic()
            lastSystemName = systemName
            isInGameBrowsing = false
            return
        }

        val requestedSource = if (!systemName.isNullOrEmpty()) {
            MusicSource.System(systemName)
        } else {
            MusicSource.Generic
        }

        // Resolve actual source after fallback
        val actualSource = resolveActualSource(requestedSource)

        if (actualSource == null) {
            android.util.Log.d(TAG, "No music files available")
            stopMusic()
            lastSystemName = systemName
            isInGameBrowsing = false
            return
        }

        // Determine if cross-fade needed
        val needsCrossFade = shouldCrossFade(
            currentMusicSource,
            actualSource
        )

        if (needsCrossFade) {
            crossFadeToSource(actualSource)
        } else if (!isMusicPlaying || currentMusicSource == null) {
            startMusic(actualSource)
        } else {
            android.util.Log.d(TAG, "Continuing current music (same source)")
        }

        lastSystemName = systemName
        isInGameBrowsing = false
    }

    override fun onGameSelected(systemName: String, gameFilename: String) {
        android.util.Log.d(TAG, "━━━ GAME SELECTED: $gameFilename in $systemName ━━━")

        if (!isActivityVisible) {
            android.util.Log.d(TAG, "Music blocked - activity not visible")
            isInGameBrowsing = true
            return
        }

        if (!isMusicEnabled()) {
            android.util.Log.d(TAG, "Music disabled globally")
            stopMusic()
            isInGameBrowsing = true
            return
        }

        // Check if game music is enabled
        if (!prefsManager.state.value.musicGameEnabled) {
            android.util.Log.d(TAG, "Game music disabled")
            stopMusic()
            isInGameBrowsing = true
            return
        }

        // Use system-specific music for game browsing (same as system browsing)
        val requestedSource = MusicSource.System(systemName)
        val actualSource = resolveActualSource(requestedSource)

        if (actualSource == null) {
            android.util.Log.d(TAG, "No music files available")
            stopMusic()
            isInGameBrowsing = true
            return
        }

        // Don't cross-fade if we're already playing from this source
        val needsCrossFade = currentMusicSource != actualSource

        if (needsCrossFade) {
            crossFadeToSource(actualSource)
        } else if (!isMusicPlaying) {
            startMusic(actualSource)
        }

        isInGameBrowsing = true
    }

    override fun onGameStarted() {
        android.util.Log.d(TAG, "━━━ GAME STARTED ━━━")
        // Always stop music during gameplay regardless of settings
        stopMusic()
        isInGameBrowsing = false
    }

    override fun onGameEnded() {
        android.util.Log.d(TAG, "━━━ GAME ENDED ━━━")

        // Resume music based on last known state
        if (isMusicEnabled() && isActivityVisible) {
            if (isInGameBrowsing && prefsManager.state.value.musicGameEnabled) {
                // Resume game browsing music
                lastSystemName?.let { systemName ->
                    val source = resolveActualSource(MusicSource.System(systemName))
                    source?.let { startMusic(it) }
                }
            } else if (prefsManager.state.value.musicSystemEnabled) {
                // Resume system browsing music
                lastSystemName?.let { systemName ->
                    val source = resolveActualSource(MusicSource.System(systemName))
                    source?.let { startMusic(it) }
                }
            }
        }
    }

    override fun onScreensaverStarted() {
        android.util.Log.d(TAG, "━━━ SCREENSAVER STARTED ━━━")

        if (!isMusicEnabled()) {
            android.util.Log.d(TAG, "Music disabled globally")
            return
        }

        if (!prefsManager.state.value.musicScreensaverEnabled) {
            android.util.Log.d(TAG, "Screensaver music disabled")
            return
        }

        // Screensaver always uses generic music
        val source = resolveActualSource(MusicSource.Generic)
        source?.let {
            // Always cross-fade to screensaver music
            crossFadeToSource(it)
        }
    }

    override fun onScreensaverEnded() {
        android.util.Log.d(TAG, "━━━ SCREENSAVER ENDED ━━━")

        // Resume normal music based on current state
        if (isMusicEnabled() && isActivityVisible) {
            if (isInGameBrowsing && prefsManager.state.value.musicGameEnabled && lastSystemName != null) {
                val source = resolveActualSource(MusicSource.System(lastSystemName!!))
                source?.let { crossFadeToSource(it) }
            } else if (prefsManager.state.value.musicSystemEnabled && lastSystemName != null) {
                val source = resolveActualSource(MusicSource.System(lastSystemName!!))
                source?.let { crossFadeToSource(it) }
            }
        }
    }

    override fun onVideoStarted() {
        if (musicPlayer == null || musicPlayer?.isPlaying != true) {
            return
        }

        val behavior = prefsManager.state.value.musicVideoBehavior
        android.util.Log.d(TAG, "Video started - behavior: $behavior")

        when (behavior) {
            MusicVideoBehavior.Continue -> {
                // Do nothing - music stays at 100%
                android.util.Log.d(TAG, "Continuing music at full volume")
            }

            MusicVideoBehavior.Duck -> {
                duckMusic()
            }

            MusicVideoBehavior.Pause -> {
                pauseMusicForVideo()
            }
        }
    }

    override fun onVideoEnded() {
        android.util.Log.d(TAG, "Video ended")

        if (isMusicDucked) {
            restoreMusicVolume()
        } else if (wasMusicPausedForVideo) {
            resumeMusicFromVideo()
        }
    }

    override fun onActivityVisible() {
        android.util.Log.d(TAG, "━━━ ACTIVITY VISIBLE ━━━")
        isActivityVisible = true

        // Check if music is enabled before resuming
        val musicEnabled = prefsManager.state.value.musicEnabled
        if (!musicEnabled) {
            android.util.Log.d(TAG, "Music disabled - not resuming")
            wasMusicPlayingBeforeInvisible = false
            return
        }

        // Resume music if it was playing before visibility was lost
        if (wasMusicPlayingBeforeInvisible) {
            android.util.Log.d(TAG, "Resuming music (was playing before invisible)")

            // Restart the music source that was playing
            if (currentMusicSource != null) {
                android.util.Log.d(TAG, "Restarting music from source: $currentMusicSource")
                startMusic(currentMusicSource!!)
            } else {
                android.util.Log.d(TAG, "No music source to resume")
                isMusicPlaying = false
            }
        }
    }

    override fun onActivityInvisible() {
        android.util.Log.d(TAG, "━━━ ACTIVITY INVISIBLE ━━━")
        isActivityVisible = false

        // Pause music if currently playing
        if (musicPlayer?.isPlaying == true) {
            android.util.Log.d(TAG, "Pausing music (activity not visible)")
            wasMusicPlayingBeforeInvisible = true

            // Fade out then pause
            fadeVolume(currentVolume, 0f, CROSS_FADE_DURATION) {
                musicPlayer?.pause()
            }
        } else {
            android.util.Log.d(TAG, "Music not playing - no pause needed")
            wasMusicPlayingBeforeInvisible = false
        }
    }

    override fun release() {
        android.util.Log.d(TAG, "Releasing music resources")

        // Cancel any pending operations
        volumeFadeRunnable?.let { handler.removeCallbacks(it) }

        // Release player
        musicPlayer?.release()
        musicPlayer = null

        currentMusicSource = null
        currentPlaylist = emptyList()
        isMusicPlaying = false
    }

    override fun pauseMusic() {
        musicPlayer?.let { player ->
            if (player.isPlaying) {
                android.util.Log.d(TAG, "Pausing music via user control")
                player.pause()
            }
        }
    }

    override fun resumeMusic() {
        musicPlayer?.let { player ->
            if (!player.isPlaying) {
                android.util.Log.d(TAG, "Resuming music via user control")
                player.start()
                // Ensure volume is at normal level
                fadeVolume(currentVolume, NORMAL_VOLUME, DUCK_FADE_DURATION)
            }
        }
    }

    override fun skipToNextTrack() {
        android.util.Log.d(TAG, "Skipping to next track via user control")
        playNextTrack()
    }

    override fun isPlaying(): Boolean {
        return musicPlayer?.isPlaying ?: false
    }

    override fun isPaused(): Boolean {
        val player = musicPlayer ?: return false
        return try {
            // If player exists and is NOT playing, it's paused
            !player.isPlaying
        } catch (e: IllegalStateException) {
            // Player was released
            false
        }
    }

    // ========== PRIVATE PLAYBACK METHODS ==========

    /**
     * Start playing music from a new source.
     */
    private fun startMusic(source: MusicSource) {
        android.util.Log.d(TAG, "Starting music from source: $source")

        // Load playlist for this source
        val playlist = loadPlaylist(source)
        if (playlist.isEmpty()) {
            android.util.Log.d(TAG, "No music files found for source: $source")
            isMusicPlaying = false
            return
        }

        android.util.Log.d(TAG, "Loaded playlist with ${playlist.size} tracks")

        // Store new state
        currentMusicSource = source
        currentPlaylist = playlist
        currentTrackIndex = 0

        // Reset volume state for new playback
        targetVolume = NORMAL_VOLUME
        isMusicDucked = false
        wasMusicPausedForVideo = false

        // Play first track
        playTrack(playlist[0])
        isMusicPlaying = true
    }

    /**
     * Stop all music playback.
     */
    private fun stopMusic() {
        if (musicPlayer == null) {
            return
        }

        android.util.Log.d(TAG, "Stopping music")

        // Fade out then stop
        fadeVolume(currentVolume, 0f, CROSS_FADE_DURATION) {
            musicPlayer?.stop()
            musicPlayer?.release()
            musicPlayer = null
            currentMusicSource = null
            currentPlaylist = emptyList()
            isMusicDucked = false
            wasMusicPausedForVideo = false
            isMusicPlaying = false
            targetVolume = NORMAL_VOLUME
        }
    }

    /**
     * Cross-fade from current source to a new source.
     */
    private fun crossFadeToSource(newSource: MusicSource) {
        android.util.Log.d(TAG, "Cross-fading to source: $newSource")

        // Save reference to old player BEFORE changing musicPlayer
        val oldPlayer = musicPlayer
        
        // IMPORTANT: Clear musicPlayer reference so playTrack() doesn't release it
        // The old player will be faded out and released independently below
        musicPlayer = null

        // Load new playlist
        val newPlaylist = loadPlaylist(newSource)
        if (newPlaylist.isEmpty()) {
            android.util.Log.d(TAG, "No music files found for new source")
            stopMusic()
            return
        }

        // Update state
        currentMusicSource = newSource
        currentPlaylist = newPlaylist
        currentTrackIndex = 0

        // Reset volume state for new playback - start from silence
        currentVolume = 0f
        targetVolume = NORMAL_VOLUME
        isMusicDucked = false
        wasMusicPausedForVideo = false

        // Fade out and release old player independently
        if (oldPlayer != null && oldPlayer.isPlaying) {
            android.util.Log.d(TAG, "Fading out old player independently")

            // Create independent fade for old player
            var oldPlayerVolume = 1.0f
            val fadeSteps = (CROSS_FADE_DURATION / 50).toInt()
            val volumeStep = oldPlayerVolume / fadeSteps
            var currentStep = 0

            val oldPlayerFadeRunnable = object : Runnable {
                override fun run() {
                    if (currentStep < fadeSteps) {
                        oldPlayerVolume -= volumeStep
                        if (oldPlayerVolume < 0f) oldPlayerVolume = 0f

                        try {
                            oldPlayer.setVolume(oldPlayerVolume, oldPlayerVolume)
                            currentStep++
                            handler.postDelayed(this, 50)
                        } catch (e: Exception) {
                            android.util.Log.d(TAG, "Old player already released")
                        }
                    } else {
                        // Fade complete - release old player
                        try {
                            oldPlayer.stop()
                            oldPlayer.release()
                            android.util.Log.d(TAG, "Old player released after fade")
                        } catch (e: Exception) {
                            android.util.Log.d(TAG, "Error releasing old player: ${e.message}")
                        }
                    }
                }
            }
            handler.post(oldPlayerFadeRunnable)
        } else if (oldPlayer != null) {
            // Old player exists but isn't playing - release it immediately
            android.util.Log.d(TAG, "Releasing old player (not playing)")
            try {
                oldPlayer.release()
            } catch (e: Exception) {
                android.util.Log.d(TAG, "Error releasing old player: ${e.message}")
            }
        }

        // Start new player immediately (it will fade in from 0)
        playTrack(newPlaylist[0])
        isMusicPlaying = true
    }

    /**
     * Play a specific audio track.
     */
    private fun playTrack(file: File) {
        android.util.Log.d(TAG, "Playing track: ${file.name}")

        try {
            // Release old player
            musicPlayer?.release()

            // Create new player
            musicPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    android.util.Log.d(TAG, "Track prepared, starting playback")
                    mp.start()
                    // Fade in from 0 to target volume
                    fadeVolume(0f, targetVolume, CROSS_FADE_DURATION)
                }
                setOnCompletionListener {
                    android.util.Log.d(TAG, "Track completed, playing next")
                    // IMPORTANT: Post to handler to avoid calling release() from within callback
                    // Releasing a MediaPlayer from its own callback can cause undefined behavior
                    handler.post { playNextTrack() }
                }
                setOnErrorListener { _, what, extra ->
                    android.util.Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    // IMPORTANT: Post to handler to avoid calling release() from within callback
                    handler.post { playNextTrack() }
                    true
                }
                prepareAsync()
            }

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error playing track: ${file.name}", e)
            // Post to handler to avoid potential recursive issues
            handler.post { playNextTrack() }
        }
    }

    /**
     * Play the next track in the playlist.
     */
    private fun playNextTrack() {
        if (currentPlaylist.isEmpty()) {
            android.util.Log.d(TAG, "No playlist loaded")
            return
        }

        // Move to next track (loop back to start if at end)
        currentTrackIndex = (currentTrackIndex + 1) % currentPlaylist.size
        android.util.Log.d(TAG, "Next track index: $currentTrackIndex / ${currentPlaylist.size}")

        playTrack(currentPlaylist[currentTrackIndex])
    }

    // ========== VOLUME CONTROL ==========

    /**
     * Duck music volume for video playback.
     */
    private fun duckMusic() {
        android.util.Log.d(TAG, "Ducking music to ${DUCKED_VOLUME * 100}%")
        isMusicDucked = true
        fadeVolume(currentVolume, DUCKED_VOLUME, DUCK_FADE_DURATION)
    }

    /**
     * Restore music volume after video ends.
     */
    private fun restoreMusicVolume() {
        android.util.Log.d(TAG, "Restoring music to ${NORMAL_VOLUME * 100}%")
        isMusicDucked = false
        fadeVolume(currentVolume, NORMAL_VOLUME, DUCK_FADE_DURATION)
    }

    /**
     * Pause music for video playback.
     */
    private fun pauseMusicForVideo() {
        android.util.Log.d(TAG, "Pausing music for video")
        wasMusicPausedForVideo = true

        // Fade out then pause
        fadeVolume(currentVolume, 0f, DUCK_FADE_DURATION) {
            musicPlayer?.pause()
        }
    }

    /**
     * Resume music after video ends.
     */
    private fun resumeMusicFromVideo() {
        android.util.Log.d(TAG, "Resuming music after video")
        wasMusicPausedForVideo = false

        musicPlayer?.start()
        fadeVolume(0f, NORMAL_VOLUME, DUCK_FADE_DURATION)
    }

    /**
     * Smoothly fade volume from one level to another.
     */
    private fun fadeVolume(
        fromVolume: Float,
        toVolume: Float,
        duration: Long,
        onComplete: (() -> Unit)? = null
    ) {
        // Cancel any existing fade
        volumeFadeRunnable?.let { handler.removeCallbacks(it) }

        val player = musicPlayer ?: return

        currentVolume = fromVolume
        targetVolume = toVolume

        // SAFETY: Check if player is valid before setting volume
        try {
            if (player.isPlaying || !player.isPlaying) { // Triggers IllegalStateException if released
                player.setVolume(fromVolume, fromVolume)
            }
        } catch (e: IllegalStateException) {
            android.util.Log.w(TAG, "Player already released, canceling fade")
            return
        }

        val startTime = System.currentTimeMillis()
        val volumeDelta = toVolume - fromVolume

        volumeFadeRunnable = object : Runnable {
            override fun run() {
                // SAFETY: Check if player still exists and is valid
                try {
                    // Verify player is still the same instance and not released
                    if (player.isPlaying || !player.isPlaying) { // Triggers IllegalStateException if released
                        val elapsed = System.currentTimeMillis() - startTime
                        val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

                        val newVolume = fromVolume + (volumeDelta * progress)
                        currentVolume = newVolume
                        player.setVolume(newVolume, newVolume)

                        if (progress < 1f) {
                            handler.postDelayed(this, 16) // ~60fps
                        } else {
                            onComplete?.invoke()
                        }
                    }
                } catch (e: IllegalStateException) {
                    // Player was released during fade - this is normal during cross-fades
                    android.util.Log.d(TAG, "Fade canceled: player was released")
                }
            }
        }

        handler.post(volumeFadeRunnable!!)
    }

    // ========== PLAYLIST MANAGEMENT ==========

    /**
     * Load all audio files from a music source.
     * Scans recursively through all subdirectories.
     */
    private fun loadPlaylist(source: MusicSource): List<File> {
        val sourcePath = source.getPath(getMusicPath())
        val sourceDir = File(sourcePath)

        android.util.Log.d(TAG, "Loading playlist from: $sourcePath")

        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            android.util.Log.d(TAG, "Music directory does not exist: $sourcePath")

            // If system-specific folder doesn't exist, try generic as fallback
            if (source is MusicSource.System) {
                android.util.Log.d(TAG, "System folder not found, falling back to generic")
                return loadPlaylist(MusicSource.Generic)
            }

            return emptyList()
        }

        // Find all audio files recursively, excluding "systems" subfolder for generic source
        val audioFiles = scanAudioFilesRecursively(
            sourceDir,
            excludeSystemsFolder = (source is MusicSource.Generic)
        )

        if (audioFiles.isEmpty()) {
            android.util.Log.d(TAG, "No audio files found in: $sourcePath")

            // If system-specific folder is empty, try generic as fallback
            if (source is MusicSource.System) {
                android.util.Log.d(TAG, "System folder empty, falling back to generic")
                return loadPlaylist(MusicSource.Generic)
            }

            return emptyList()
        }

        // Sort alphabetically then shuffle
        val sortedFiles = audioFiles.sortedBy { it.absolutePath }
        val shuffledFiles = sortedFiles.shuffled()

        android.util.Log.d(TAG, "Found ${shuffledFiles.size} audio files (shuffled)")

        return shuffledFiles
    }

    /**
     * Recursively scan a directory for audio files.
     */
    private fun scanAudioFilesRecursively(
        directory: File,
        excludeSystemsFolder: Boolean = false
    ): List<File> {
        val audioFiles = mutableListOf<File>()
        val filesToProcess = mutableListOf(directory)

        while (filesToProcess.isNotEmpty()) {
            val currentDir = filesToProcess.removeAt(0)

            // Skip if not a directory
            if (!currentDir.isDirectory) continue

            // Skip "systems" folder if this is generic music scan
            if (excludeSystemsFolder && currentDir.name == "systems" && currentDir.parentFile == directory) {
                android.util.Log.d(
                    TAG,
                    "Skipping systems folder in generic scan: ${currentDir.absolutePath}"
                )
                continue
            }

            currentDir.listFiles()?.forEach { file ->
                when {
                    file.isFile && AUDIO_EXTENSIONS.any { ext ->
                        file.name.endsWith(".$ext", ignoreCase = true)
                    } -> {
                        // Audio file found - add to list
                        audioFiles.add(file)
                    }

                    file.isDirectory -> {
                        // Subdirectory found - add to processing queue
                        filesToProcess.add(file)
                    }
                }
            }
        }

        return audioFiles
    }

    // ========== STATE LOGIC ==========

    /**
     * Check if music is globally enabled.
     */
    private fun isMusicEnabled(): Boolean {
        return prefsManager.state.value.musicEnabled
    }

    /**
     * Resolve the actual music source after fallback logic.
     */
    private fun resolveActualSource(requestedSource: MusicSource): MusicSource? {
        val baseMusicPath = getMusicPath()

        // For Generic source, check if it exists
        if (requestedSource is MusicSource.Generic) {
            val sourcePath = requestedSource.getPath(baseMusicPath)
            return if (hasAudioFiles(sourcePath)) requestedSource else null
        }

        // For System source, check if system folder exists with audio files
        if (requestedSource is MusicSource.System) {
            val sourcePath = requestedSource.getPath(baseMusicPath)

            // If system folder has audio files, use it
            if (hasAudioFiles(sourcePath)) {
                return requestedSource
            }

            // System folder doesn't exist/is empty - will fall back to Generic
            android.util.Log.d(TAG, "System folder not found/empty, will use generic fallback")
            val genericPath = MusicSource.Generic.getPath(baseMusicPath)
            return if (hasAudioFiles(genericPath)) MusicSource.Generic else null
        }

        return null
    }

    /**
     * Check if a directory exists and contains audio files (recursively).
     */
    private fun hasAudioFiles(path: String): Boolean {
        val dir = File(path)

        if (!dir.exists() || !dir.isDirectory) {
            return false
        }

        // Use recursive scan to check for audio files
        val excludeSystems = !path.contains("/systems/")
        val audioFiles = scanAudioFilesRecursively(dir, excludeSystemsFolder = excludeSystems)

        return audioFiles.isNotEmpty()
    }

    /**
     * Determine if a state transition requires cross-fading.
     *
     * Cross-fade when:
     * - Source changes (e.g., Generic → System("snes"))
     * - System changes AND the system has its own music folder
     *
     * Continue without cross-fade when:
     * - SystemBrowsing → GameBrowsing (same system)
     * - GameBrowsing → SystemBrowsing (same system)
     * - Two different systems both fallback to Generic music
     */
    private fun shouldCrossFade(
        oldSource: MusicSource?,
        newSource: MusicSource
    ): Boolean {
        // If no old source, we're starting fresh (not a cross-fade)
        if (oldSource == null) {
            return false
        }

        // If sources are different, cross-fade
        if (oldSource != newSource) {
            return true
        }

        // Sources are the same - do NOT cross-fade
        // This prevents restarting music when switching between systems
        // that both fall back to the same Generic source
        return false
    }
}
