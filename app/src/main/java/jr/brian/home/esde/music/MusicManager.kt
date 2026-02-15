package jr.brian.home.esde.music

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import jr.brian.home.esde.preferences.ESDEPreferencesManager
import jr.brian.home.esde.preferences.MusicVideoBehavior
import jr.brian.home.esde.util.ESDEMediaConstants.getMediaSystemName
import java.io.File

class MusicManager(
    private val context: Context,
    private val prefsManager: ESDEPreferencesManager
) : MusicController {

    companion object {
        private const val TAG = "MusicManager"
        private const val DEFAULT_MUSIC_PATH = "/storage/emulated/0/ES-DE Jarngreipr/music"
        private val AUDIO_EXTENSIONS = listOf("mp3", "ogg", "flac", "m4a", "wav", "aac")
        private const val CROSS_FADE_DURATION = 300L
        private const val DUCK_FADE_DURATION = 300L
        private const val DUCKED_VOLUME = 0.2f
    }

    private fun getNormalVolume(): Float {
        return prefsManager.state.value.musicVolumeFloat
    }

    private var musicPlayer: MediaPlayer? = null
    private var currentMusicSource: MusicSource? = null
    private var currentPlaylist: List<File> = emptyList()
    private var currentTrackIndex: Int = 0
    private var currentVolume: Float = getNormalVolume()
    private var targetVolume: Float = getNormalVolume()
    private var isMusicDucked: Boolean = false
    private var wasMusicPausedForVideo: Boolean = false
    private val handler = Handler(Looper.getMainLooper())
    private var volumeFadeRunnable: Runnable? = null
    private var lastSystemName: String? = null
    private var isInGameBrowsing: Boolean = false
    private var isMusicPlaying: Boolean = false
    private var isActivityVisible: Boolean = true
    private var wasMusicPlayingBeforeInvisible: Boolean = false
    private var wasMusicPlayingBeforeGame: Boolean = false
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus: Boolean = false
    private var wasPausedByAudioFocusLoss: Boolean = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        android.util.Log.d(TAG, "Audio focus changed: $focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss - another app took audio focus
                android.util.Log.d(TAG, "Audio focus lost permanently (another app playing)")
                hasAudioFocus = false
                if (musicPlayer?.isPlaying == true) {
                    wasPausedByAudioFocusLoss = true
                    pauseMusic()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary loss - pause playback
                android.util.Log.d(TAG, "Audio focus lost temporarily")
                if (musicPlayer?.isPlaying == true) {
                    wasPausedByAudioFocusLoss = true
                    pauseMusic()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Can duck - lower volume temporarily
                android.util.Log.d(TAG, "Audio focus - can duck")
                if (!isMusicDucked && musicPlayer?.isPlaying == true) {
                    duckMusic()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Regained focus
                android.util.Log.d(TAG, "Audio focus gained")
                hasAudioFocus = true
                if (wasPausedByAudioFocusLoss && isMusicEnabled()) {
                    resumeMusic()
                    wasPausedByAudioFocusLoss = false
                } else if (isMusicDucked) {
                    restoreMusicVolume()
                }
            }
        }
    }

    init {
        android.util.Log.d(TAG, "MusicManager initialized")
        android.util.Log.d(TAG, "Base music path: ${getMusicPath()}")
    }

    private fun getMusicPath(): String {
        return prefsManager.state.value.musicPath ?: DEFAULT_MUSIC_PATH
    }

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

        if (!prefsManager.state.value.musicSystemEnabled) {
            android.util.Log.d(TAG, "System music disabled")
            stopMusic()
            lastSystemName = systemName
            isInGameBrowsing = false
            return
        }

        val useSystemSpecific = prefsManager.state.value.musicUseSystemSpecific
        
        val requestedSource = if (useSystemSpecific && !systemName.isNullOrEmpty()) {
            MusicSource.System(systemName)
        } else {
            MusicSource.Generic
        }

        val actualSource = resolveActualSource(requestedSource)

        if (actualSource == null) {
            android.util.Log.d(TAG, "No music files available")
            stopMusic()
            lastSystemName = systemName
            isInGameBrowsing = false
            return
        }

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

        if (!prefsManager.state.value.musicGameEnabled) {
            android.util.Log.d(TAG, "Game music disabled")
            stopMusic()
            isInGameBrowsing = true
            return
        }

        val useSystemSpecific = prefsManager.state.value.musicUseSystemSpecific
        
        val requestedSource = if (useSystemSpecific) {
            MusicSource.System(systemName)
        } else {
            MusicSource.Generic
        }
        val actualSource = resolveActualSource(requestedSource)

        if (actualSource == null) {
            android.util.Log.d(TAG, "No music files available")
            stopMusic()
            isInGameBrowsing = true
            return
        }

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
        wasMusicPlayingBeforeGame = isMusicPlaying
        stopMusic()
    }

    override fun onGameEnded() {
        android.util.Log.d(TAG, "━━━ GAME ENDED ━━━")

        if (!wasMusicPlayingBeforeGame) {
            wasMusicPlayingBeforeGame = false
            return
        }

        if (isMusicEnabled() && isActivityVisible) {
            if (isInGameBrowsing && prefsManager.state.value.musicGameEnabled) {
                lastSystemName?.let { systemName ->
                    val source = resolveActualSource(MusicSource.System(systemName))
                    source?.let { startMusic(it) }
                }
            } else if (prefsManager.state.value.musicSystemEnabled) {
                lastSystemName?.let { systemName ->
                    val source = resolveActualSource(MusicSource.System(systemName))
                    source?.let { startMusic(it) }
                }
            }
        }

        wasMusicPlayingBeforeGame = false
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
        
        // Abandon audio focus
        abandonAudioFocus()
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
                fadeVolume(currentVolume, getNormalVolume(), DUCK_FADE_DURATION)
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

    override fun isOtherAudioPlaying(): Boolean {
        // If we're already playing, no other audio is interrupting us
        if (musicPlayer?.isPlaying == true) {
            android.util.Log.d(TAG, "isOtherAudioPlaying: false (we are playing)")
            return false
        }
        
        // If we were paused because another app took audio focus, they're still playing
        if (wasPausedByAudioFocusLoss) {
            android.util.Log.d(TAG, "isOtherAudioPlaying: true (we lost focus to another app)")
            return true
        }
        
        // If we have audio focus, we own the audio stream - safe to play
        if (hasAudioFocus) {
            android.util.Log.d(TAG, "isOtherAudioPlaying: false (we have focus)")
            return false
        }
        
        // We don't have focus and weren't paused by focus loss - safe to start fresh
        android.util.Log.d(TAG, "isOtherAudioPlaying: false (no focus conflict)")
        return false
    }

    private fun startMusic(source: MusicSource) {
        android.util.Log.d(TAG, "Starting music from source: $source")

        // Check if another app is already playing audio (e.g., YouTube Music, Spotify)
        if (isOtherAudioPlaying()) {
            android.util.Log.d(TAG, "Other audio is playing - skipping music start to avoid interruption")
            return
        }

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
        targetVolume = getNormalVolume()
        isMusicDucked = false
        wasMusicPausedForVideo = false

        // Play first track
        playTrack(playlist[0])
        isMusicPlaying = true
    }

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
            targetVolume = getNormalVolume()
            abandonAudioFocus()
        }
    }

    private fun crossFadeToSource(newSource: MusicSource) {
        android.util.Log.d(TAG, "Cross-fading to source: $newSource")

        // Check if another app is already playing audio (e.g., YouTube Music, Spotify)
        // Only check if we're not currently playing (i.e., this is a fresh start, not a source change)
        if (musicPlayer?.isPlaying != true && isOtherAudioPlaying()) {
            android.util.Log.d(TAG, "Other audio is playing - skipping cross-fade to avoid interruption")
            return
        }

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
        targetVolume = getNormalVolume()
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

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) {
            return true
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener, handler)
                .build()

            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            android.util.Log.d(TAG, "Audio focus request result: $hasAudioFocus")
            hasAudioFocus
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            android.util.Log.d(TAG, "Audio focus request result: $hasAudioFocus")
            hasAudioFocus
        }
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        hasAudioFocus = false
        android.util.Log.d(TAG, "Audio focus abandoned")
    }

    private fun playTrack(file: File) {
        android.util.Log.d(TAG, "Playing track: ${file.name}")

        // Request audio focus before playing
        if (!requestAudioFocus()) {
            android.util.Log.w(TAG, "Failed to gain audio focus, not playing track")
            return
        }

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

    private fun playNextTrack() {
        if (currentPlaylist.isEmpty()) {
            android.util.Log.d(TAG, "No playlist loaded")
            return
        }

        val loopEnabled = prefsManager.state.value.musicLoopEnabled
        val nextIndex = currentTrackIndex + 1
        
        if (nextIndex >= currentPlaylist.size) {
            if (loopEnabled) {
                currentTrackIndex = 0
                android.util.Log.d(TAG, "Looping back to first track")
                playTrack(currentPlaylist[currentTrackIndex])
            } else {
                android.util.Log.d(TAG, "Playlist finished, not looping")
                stopMusic()
            }
        } else {
            currentTrackIndex = nextIndex
            android.util.Log.d(TAG, "Next track index: $currentTrackIndex / ${currentPlaylist.size}")
            playTrack(currentPlaylist[currentTrackIndex])
        }
    }

    private fun duckMusic() {
        android.util.Log.d(TAG, "Ducking music to ${DUCKED_VOLUME * 100}%")
        isMusicDucked = true
        fadeVolume(currentVolume, DUCKED_VOLUME, DUCK_FADE_DURATION)
    }

    private fun restoreMusicVolume() {
        val normalVolume = getNormalVolume()
        android.util.Log.d(TAG, "Restoring music to ${normalVolume * 100}%")
        isMusicDucked = false
        fadeVolume(currentVolume, normalVolume, DUCK_FADE_DURATION)
    }

    private fun pauseMusicForVideo() {
        android.util.Log.d(TAG, "Pausing music for video")
        wasMusicPausedForVideo = true

        // Fade out then pause
        fadeVolume(currentVolume, 0f, DUCK_FADE_DURATION) {
            musicPlayer?.pause()
        }
    }

    private fun resumeMusicFromVideo() {
        android.util.Log.d(TAG, "Resuming music after video")
        wasMusicPausedForVideo = false

        musicPlayer?.start()
        fadeVolume(0f, getNormalVolume(), DUCK_FADE_DURATION)
    }

    override fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        android.util.Log.d(TAG, "Setting volume to ${clampedVolume * 100}%")
        
        // Cancel any existing fade that would override this manual volume setting
        volumeFadeRunnable?.let { handler.removeCallbacks(it) }
        volumeFadeRunnable = null
        
        targetVolume = clampedVolume
        currentVolume = clampedVolume
        
        try {
            musicPlayer?.setVolume(clampedVolume, clampedVolume)
        } catch (e: IllegalStateException) {
            android.util.Log.w(TAG, "Could not set volume - player released")
        }
    }

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



    private fun loadPlaylist(source: MusicSource): List<File> {
        val baseMusicPath = getMusicPath()
        
        if (source is MusicSource.System) {
            val systemName = source.systemName
            val normalizedSystemName = getMediaSystemName(systemName)
            val systemNamesToTry = listOf(systemName, normalizedSystemName).distinct()
            
            for (name in systemNamesToTry) {
                val flatFiles = findFlatSystemAudioFiles(baseMusicPath, name)
                if (flatFiles.isNotEmpty()) {
                    android.util.Log.d(TAG, "Found ${flatFiles.size} flat audio files for $name")
                    return flatFiles.shuffled()
                }
            }
            
            for (name in systemNamesToTry) {
                val sourcePath = "$baseMusicPath/systems/$name"
                val sourceDir = File(sourcePath)
                
                android.util.Log.d(TAG, "Loading playlist from: $sourcePath")
                
                if (sourceDir.exists() && sourceDir.isDirectory) {
                    val audioFiles = scanAudioFilesRecursively(sourceDir, excludeSystemsFolder = false)
                    if (audioFiles.isNotEmpty()) {
                        val sortedFiles = audioFiles.sortedBy { it.absolutePath }
                        val shuffledFiles = sortedFiles.shuffled()
                        android.util.Log.d(TAG, "Found ${shuffledFiles.size} audio files in $name")
                        return shuffledFiles
                    }
                }
            }
            
            android.util.Log.d(TAG, "System music not found for: $systemName, falling back to generic")
            return loadPlaylist(MusicSource.Generic)
        }
        
        val sourcePath = source.getPath(baseMusicPath)
        val sourceDir = File(sourcePath)

        android.util.Log.d(TAG, "Loading playlist from: $sourcePath")

        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            android.util.Log.d(TAG, "Music directory does not exist: $sourcePath")
            return emptyList()
        }

        val audioFiles = scanAudioFilesRecursively(
            sourceDir,
            excludeSystemsFolder = true
        )

        if (audioFiles.isEmpty()) {
            android.util.Log.d(TAG, "No audio files found in: $sourcePath")
            return emptyList()
        }

        val sortedFiles = audioFiles.sortedBy { it.absolutePath }
        val shuffledFiles = sortedFiles.shuffled()


        return shuffledFiles
    }
    
    private fun findFlatSystemAudioFiles(basePath: String, systemName: String): List<File> {
        val baseDir = File(basePath)
        if (!baseDir.exists() || !baseDir.isDirectory) {
            return emptyList()
        }
        
        val audioFiles = mutableListOf<File>()
        
        for (ext in AUDIO_EXTENSIONS) {
            val file = File(baseDir, "$systemName.$ext")
            if (file.exists() && file.isFile) {
                audioFiles.add(file)
            }
        }
        
        return audioFiles
    }

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

    private fun isMusicEnabled(): Boolean {
        return prefsManager.state.value.musicEnabled
    }

    private fun resolveActualSource(requestedSource: MusicSource): MusicSource? {
        val baseMusicPath = getMusicPath()

        if (requestedSource is MusicSource.Generic) {
            val sourcePath = requestedSource.getPath(baseMusicPath)
            return if (hasAudioFiles(sourcePath)) requestedSource else null
        }

        if (requestedSource is MusicSource.System) {
            val systemName = requestedSource.systemName
            val normalizedSystemName = getMediaSystemName(systemName)
            val systemNamesToTry = listOf(systemName, normalizedSystemName).distinct()
            
            for (name in systemNamesToTry) {
                if (findFlatSystemAudioFiles(baseMusicPath, name).isNotEmpty()) {
                    android.util.Log.d(TAG, "Found flat audio file for system: $name")
                    return if (name == systemName) requestedSource else MusicSource.System(name)
                }
            }
            
            for (name in systemNamesToTry) {
                val sourcePath = "$baseMusicPath/systems/$name"
                if (hasAudioFiles(sourcePath)) {
                    android.util.Log.d(TAG, "Found music folder for system: $name")
                    return if (name == systemName) requestedSource else MusicSource.System(name)
                }
            }

            android.util.Log.d(TAG, "System music not found for: $systemName, will use generic fallback")
            val genericPath = MusicSource.Generic.getPath(baseMusicPath)
            return if (hasAudioFiles(genericPath)) MusicSource.Generic else null
        }

        return null
    }

    private fun hasAudioFiles(path: String): Boolean {
        val dir = File(path)

        if (!dir.exists() || !dir.isDirectory) {
            return false
        }

        val excludeSystems = !path.contains("/systems/")
        val audioFiles = scanAudioFilesRecursively(dir, excludeSystemsFolder = excludeSystems)

        return audioFiles.isNotEmpty()
    }

    private fun shouldCrossFade(
        oldSource: MusicSource?,
        newSource: MusicSource
    ): Boolean {
        if (oldSource == null) {
            return false
        }

        if (oldSource != newSource) {
            return true
        }

        // Sources are the same - do NOT cross-fade
        // This prevents restarting music when switching between systems
        // that both fall back to the same Generic source
        return false
    }
}
