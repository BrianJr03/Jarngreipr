package jr.brian.home.esde.music

/**
 * Defines the contract for music playback control in ES-DE.
 *
 * This interface allows for:
 * - Clean separation between music logic and UI
 * - Easy testing with mock implementations
 * - Safe null handling when music is disabled
 * - Clear API surface for music features
 */
interface MusicController {

    /**
     * Called when the current system changes during browsing.
     *
     * The music system will automatically:
     * - Start/stop music based on state and settings
     * - Cross-fade between different music sources
     * - Continue same track when appropriate
     *
     * @param systemName The ES-DE system name (e.g., "snes", "arcade")
     */
    fun onSystemChanged(systemName: String?)

    /**
     * Called when a game is selected for browsing.
     *
     * @param systemName The system the game belongs to
     * @param gameFilename The game filename
     */
    fun onGameSelected(systemName: String, gameFilename: String)

    /**
     * Called when a game is launched.
     * Music should stop during gameplay.
     */
    fun onGameStarted()

    /**
     * Called when a game ends (user exits back to ES-DE).
     * Music can resume based on current browsing state.
     */
    fun onGameEnded()

    /**
     * Called when ES-DE screensaver starts.
     */
    fun onScreensaverStarted()

    /**
     * Called when ES-DE screensaver ends.
     */
    fun onScreensaverEnded()

    /**
     * Called when a video starts playing.
     *
     * Behavior depends on "music_video_behavior" setting:
     * - "continue": No change (music stays at 100%)
     * - "duck": Fade music to 20% volume
     * - "pause": Pause music playback
     */
    fun onVideoStarted()

    /**
     * Called when a video stops playing.
     *
     * Restores music to previous state:
     * - If ducked: Fade back to 100% volume
     * - If paused: Resume playback
     * - If at 100%: No change
     */
    fun onVideoEnded()

    /**
     * Called when activity becomes visible (onStart).
     *
     * Music will resume if it was playing before visibility was lost.
     */
    fun onActivityVisible()

    /**
     * Called when activity becomes invisible (onStop, device sleep).
     *
     * Music will pause and track that it should resume when visible again.
     */
    fun onActivityInvisible()

    /**
     * Release all music resources.
     *
     * Called when the music system is destroyed to clean up:
     * - MediaPlayer instances
     * - Handler callbacks
     * - Any pending operations
     */
    fun release()

    /**
     * Pause music playback (called by UI controls).
     */
    fun pauseMusic()

    /**
     * Resume music playback (called by UI controls).
     */
    fun resumeMusic()

    /**
     * Skip to next track in playlist (called by UI controls).
     */
    fun skipToNextTrack()

    /**
     * Check if music is currently playing.
     */
    fun isPlaying(): Boolean

    /**
     * Check if music player exists and is paused (not playing, but not released).
     */
    fun isPaused(): Boolean
}
