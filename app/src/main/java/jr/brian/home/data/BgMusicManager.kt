package jr.brian.home.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class BgMusicManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "bg_music_prefs"
        private const val KEY_FOLDER_URI = "bg_music_folder_uri"
        private const val KEY_SINGLE_FILE_URI = "bg_music_file_uri"
        private const val KEY_VOLUME = "bg_music_volume"
        private const val KEY_MODE = "bg_music_mode"
        private const val DUCK_FACTOR = 0.2f
        private val AUDIO_EXTENSIONS = setOf("mp3", "ogg", "flac", "m4a", "wav", "aac", "opus")
    }

    enum class Mode { FOLDER, SINGLE_FILE }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var vol by mutableFloatStateOf(prefs.getFloat(KEY_VOLUME, 0.5f))
        private set

    private var isDucked = false

    // Whether the player was paused specifically because mute was enabled.
    // Guards against resumePlayback(), screen-on, or audio focus gain
    // accidentally restarting audio while the user still has mute on.
    private var isMuted = false
    private var isMutePaused = false

    private fun effectiveVolume() = when {
        isDucked -> vol * DUCK_FACTOR
        else -> vol
    }

    fun duck() {
        if (isDucked) return
        isDucked = true
        // Only apply volume change if not muted (players are paused when muted)
        if (!isMuted) {
            val ev = effectiveVolume()
            mediaPlayer?.setVolume(ev, ev)
            nextMediaPlayer?.setVolume(ev, ev)
        }
    }

    fun unDuck() {
        if (!isDucked) return
        isDucked = false
        // Only apply volume change if not muted
        if (!isMuted) {
            val ev = effectiveVolume()
            mediaPlayer?.setVolume(ev, ev)
            nextMediaPlayer?.setVolume(ev, ev)
        }
    }

    var folderUri by mutableStateOf(prefs.getString(KEY_FOLDER_URI, null))
        private set

    var singleFileUri by mutableStateOf(prefs.getString(KEY_SINGLE_FILE_URI, null))
        private set

    var mode by mutableStateOf(
        prefs.getString(KEY_MODE, Mode.SINGLE_FILE.name)
            ?.let { runCatching { Mode.valueOf(it) }.getOrNull() }
            ?: Mode.SINGLE_FILE
    )
        private set

    private var mediaPlayer: MediaPlayer? = null
    private var nextMediaPlayer: MediaPlayer? = null
    private var playlist: List<Uri> = emptyList()
    private var currentIndex = 0

    // true from startPlayback() until stopPlayback(); unaffected by pause/focus events
    private var isIntendedToPlay = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pausePlayback()

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> duck()
            AudioManager.AUDIOFOCUS_GAIN -> {
                unDuck()
                // Don't resume if muted — isMutePaused will gate it
                if (isIntendedToPlay && !isMuted) resumePlayback()
            }
        }
    }

    private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setOnAudioFocusChangeListener(audioFocusListener)
        .build()

    private fun requestAudioFocus(): Boolean =
        audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    private fun abandonAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> pausePlayback()
                // Don't resume on screen-on if muted
                Intent.ACTION_SCREEN_ON -> if (isIntendedToPlay && !isMuted) resumePlayback()
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        context.registerReceiver(screenReceiver, filter)
    }

    fun setFolderUri(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val uriString = uri.toString()
        folderUri = uriString
        mode = Mode.FOLDER
        prefs.edit {
            putString(KEY_FOLDER_URI, uriString)
            putString(KEY_MODE, Mode.FOLDER.name)
        }
        startPlayback()
    }

    fun setSingleFileUri(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val uriString = uri.toString()
        singleFileUri = uriString
        mode = Mode.SINGLE_FILE
        prefs.edit {
            putString(KEY_SINGLE_FILE_URI, uriString)
            putString(KEY_MODE, Mode.SINGLE_FILE.name)
        }
        startPlayback()
    }

    fun setVolume(v: Float) {
        vol = v.coerceIn(0f, 1f)
        prefs.edit { putFloat(KEY_VOLUME, vol) }
        if (isMuted) return
        if (vol == 0f) {
            val wasPlaying = mediaPlayer?.isPlaying == true
            mediaPlayer?.runCatching { if (isPlaying) pause() }
            if (wasPlaying) isMutePaused = true
        } else {
            if (isMutePaused) {
                isMutePaused = false
                mediaPlayer?.runCatching { start() }
            }
            val ev = effectiveVolume()
            mediaPlayer?.setVolume(ev, ev)
            nextMediaPlayer?.setVolume(ev, ev)
        }
    }

    fun startPlayback() {
        stopPlayback()
        isIntendedToPlay = true
        // If muted, set up the players but don't actually start them
        when (mode) {
            Mode.FOLDER -> startFolderPlayback()
            Mode.SINGLE_FILE -> startSingleFilePlayback()
        }
    }

    fun stopPlayback() {
        isIntendedToPlay = false
        isMutePaused = false
        mediaPlayer?.runCatching { if (isPlaying) stop(); release() }
        nextMediaPlayer?.runCatching { release() }
        mediaPlayer = null
        nextMediaPlayer = null
        abandonAudioFocus()
    }

    /**
     * Mutes or unmutes playback by pausing/resuming the player rather than
     * zeroing the software gain. This ensures screen recorders capture silence
     * instead of the raw audio stream at zero gain.
     */
    fun setMuted(muted: Boolean) {
        if (isMuted == muted) return
        isMuted = muted

        if (muted) {
            // Pause the active player so no audio reaches the mixer
            val wasPlaying = mediaPlayer?.isPlaying == true
            mediaPlayer?.runCatching { if (isPlaying) pause() }
            if (wasPlaying) {
                isMutePaused = true
            }
        } else {
            // Only resume if we were the ones who paused it for mute
            if (isMutePaused && isIntendedToPlay) {
                isMutePaused = false
                mediaPlayer?.runCatching { start() }
                val ev = effectiveVolume()
                mediaPlayer?.setVolume(ev, ev)
                nextMediaPlayer?.setVolume(ev, ev)
            } else {
                isMutePaused = false
            }
        }
    }

    fun restoreSettings(
        folderUri: String?,
        fileUri: String?,
        mode: Mode,
        volume: Float
    ) {
        this.folderUri = folderUri
        this.singleFileUri = fileUri
        this.mode = mode
        this.vol = volume.coerceIn(0f, 1f)
        prefs.edit {
            if (folderUri != null) putString(KEY_FOLDER_URI, folderUri) else remove(KEY_FOLDER_URI)
            if (fileUri != null) putString(KEY_SINGLE_FILE_URI, fileUri) else remove(KEY_SINGLE_FILE_URI)
            putString(KEY_MODE, mode.name)
            putFloat(KEY_VOLUME, this@BgMusicManager.vol)
        }
    }

    fun release() {
        runCatching { context.unregisterReceiver(screenReceiver) }
        stopPlayback()
    }

    fun pausePlayback() {
        mediaPlayer?.runCatching { if (isPlaying) pause() }
    }

    fun resumePlayback() {
        // Don't resume if the player is paused because of mute
        if (isMutePaused) return

        if (mediaPlayer != null) {
            mediaPlayer?.runCatching { start() }
        } else if (isIntendedToPlay) {
            resumeIfConfigured()
        }
    }

    /** Called on app start to resume playback if a source is configured. */
    fun resumeIfConfigured() {
        // Don't resume if muted
        if (isMuted) return

        val hasSource = when (mode) {
            Mode.FOLDER -> folderUri != null
            Mode.SINGLE_FILE -> singleFileUri != null
        }
        if (hasSource) startPlayback()
    }

    private fun startFolderPlayback() {
        val uri = folderUri?.toUri() ?: return
        val docFile = DocumentFile.fromTreeUri(context, uri) ?: return
        playlist = docFile.listFiles()
            .filter { file ->
                file.isFile &&
                        file.name?.substringAfterLast('.', "")?.lowercase() in AUDIO_EXTENSIONS
            }
            .sortedBy { it.name?.lowercase() }
            .mapNotNull { it.uri }
        if (playlist.isEmpty()) return
        currentIndex = 0
        playFolderTrack(playlist[currentIndex])
    }

    /**
     * Builds the primary [MediaPlayer] for [uri]. Once it is prepared and
     * started, we immediately begin buffering the next track so it is ready
     * to be chained via [MediaPlayer.setNextMediaPlayer].
     */
    private fun playFolderTrack(uri: Uri) {
        mediaPlayer?.runCatching { release() }
        nextMediaPlayer?.runCatching { release() }
        nextMediaPlayer = null

        val player = buildFolderPlayer(uri) ?: return
        player.setOnPreparedListener { mp ->
            if (!requestAudioFocus()) return@setOnPreparedListener
            if (isMuted) {
                mp.start()
                mp.pause()
                isMutePaused = true
            } else {
                mp.start()
                prebufferNextFolderTrack()
            }
        }
        mediaPlayer = player
        if (!player.prepareAsyncSafely()) {
            mediaPlayer = null
            player.runCatching { release() }
        }
    }

    /**
     * Prepares the track after [currentIndex] in the background.
     * Once ready, chains it to the current player via [MediaPlayer.setNextMediaPlayer]
     * so the OS can hand off without any gap.
     */
    private fun prebufferNextFolderTrack() {
        if (playlist.isEmpty()) return
        val nextIndex = (currentIndex + 1) % playlist.size
        val nextUri = playlist[nextIndex]

        nextMediaPlayer?.runCatching { release() }
        val next = buildFolderPlayer(nextUri) ?: run {
            nextMediaPlayer = null
            return
        }
        next.setOnPreparedListener { preparedNext ->
            mediaPlayer?.setNextMediaPlayer(preparedNext)
        }
        nextMediaPlayer = next
        if (!next.prepareAsyncSafely()) {
            nextMediaPlayer = null
            next.runCatching { release() }
        }
    }

    /**
     * Builds a [MediaPlayer] for folder playback.
     * The completion listener advances state and pre-buffers the track after next.
     */
    private fun buildFolderPlayer(uri: Uri): MediaPlayer? {
        val player = MediaPlayer()
        val configured = player.runCatching {
            setDataSource(context, uri)
            setVolume(effectiveVolume(), effectiveVolume())

            // onCompletion fires after the OS transitions to the next player,
            // so at this point nextMediaPlayer IS the current player.
            setOnCompletionListener {
                mediaPlayer?.runCatching { release() }
                mediaPlayer = nextMediaPlayer
                nextMediaPlayer = null
                if (playlist.isNotEmpty()) {
                    currentIndex = (currentIndex + 1) % playlist.size
                    if (!isMuted) prebufferNextFolderTrack()
                }
            }

            setOnErrorListener { _, _, _ ->
                if (playlist.isNotEmpty()) {
                    currentIndex = (currentIndex + 1) % playlist.size
                    playFolderTrack(playlist[currentIndex])
                }
                true
            }
        }.isSuccess

        if (!configured) {
            player.runCatching { release() }
            return null
        }
        return player
    }

    /**
     * Creates two [MediaPlayer] instances pointing at the same file and
     * chains them via [MediaPlayer.setNextMediaPlayer]. When one finishes
     * the OS immediately continues with the other, and we rebuild the
     * just-finished player in the background ready for the next handoff.
     */
    private fun startSingleFilePlayback() {
        val uri = singleFileUri?.toUri() ?: return

        mediaPlayer?.runCatching { release() }
        nextMediaPlayer?.runCatching { release() }
        nextMediaPlayer = null

        val primary = buildSingleFilePlayer(uri) ?: return
        primary.setOnPreparedListener { mp ->
            if (!requestAudioFocus()) return@setOnPreparedListener

            if (isMuted) {
                mp.start()
                mp.pause()
                isMutePaused = true
            } else {
                // Build and prepare the looper before primary starts so it
                // is ready to be chained immediately after prepare.
                val looper = buildSingleFilePlayer(uri)
                if (looper != null) {
                    looper.setOnPreparedListener { preparedNext ->
                        mp.setNextMediaPlayer(preparedNext)
                    }
                    nextMediaPlayer = looper
                    if (!looper.prepareAsyncSafely()) {
                        nextMediaPlayer = null
                        looper.runCatching { release() }
                    }
                }
                mp.start()
            }
        }
        mediaPlayer = primary
        if (!primary.prepareAsyncSafely()) {
            mediaPlayer = null
            primary.runCatching { release() }
        }
    }

    /**
     * Builds a [MediaPlayer] for single-file loop playback.
     * On completion, the active players are rotated and a fresh player
     * is buffered for the next loop iteration.
     */
    private fun buildSingleFilePlayer(uri: Uri): MediaPlayer? {
        val player = MediaPlayer()
        val configured = player.runCatching {
            setDataSource(context, uri)
            setVolume(effectiveVolume(), effectiveVolume())

            setOnCompletionListener {
                mediaPlayer?.runCatching { release() }
                mediaPlayer = nextMediaPlayer
                nextMediaPlayer = null

                if (!isMuted) {
                    val fresh = buildSingleFilePlayer(uri)
                    if (fresh != null) {
                        fresh.setOnPreparedListener { preparedFresh ->
                            mediaPlayer?.setNextMediaPlayer(preparedFresh)
                        }
                        nextMediaPlayer = fresh
                        if (!fresh.prepareAsyncSafely()) {
                            nextMediaPlayer = null
                            fresh.runCatching { release() }
                        }
                    }
                }
            }

            setOnErrorListener { _, _, _ -> true }
        }.isSuccess

        if (!configured) {
            player.runCatching { release() }
            return null
        }
        return player
    }

    private fun MediaPlayer.prepareAsyncSafely(): Boolean =
        runCatching { prepareAsync() }.isSuccess
}