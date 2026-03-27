package jr.brian.home.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
        private val AUDIO_EXTENSIONS = setOf("mp3", "ogg", "flac", "m4a", "wav", "aac", "opus")
    }

    enum class Mode { FOLDER, SINGLE_FILE }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var vol by mutableFloatStateOf(prefs.getFloat(KEY_VOLUME, 0.5f))
        private set

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
    private var playlist: List<Uri> = emptyList()
    private var currentIndex = 0

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
        mediaPlayer?.setVolume(vol, vol)
    }

    fun startPlayback() {
        stopPlayback()
        when (mode) {
            Mode.FOLDER -> startFolderPlayback()
            Mode.SINGLE_FILE -> startSingleFilePlayback()
        }
    }

    fun stopPlayback() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    /** Called on app start to resume playback if a source is configured. */
    fun resumeIfConfigured() {
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
        playTrack(playlist[currentIndex])
    }

    private fun playTrack(uri: Uri) {
        mediaPlayer?.runCatching { release() }
        mediaPlayer = MediaPlayer().apply {
            runCatching {
                setDataSource(context, uri)
                setVolume(vol, vol)
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    if (playlist.isNotEmpty()) {
                        currentIndex = (currentIndex + 1) % playlist.size
                        playTrack(playlist[currentIndex])
                    }
                }
                setOnErrorListener { _, _, _ ->
                    if (playlist.isNotEmpty()) {
                        currentIndex = (currentIndex + 1) % playlist.size
                        playTrack(playlist[currentIndex])
                    }
                    true
                }
                prepareAsync()
            }
        }
    }

    private fun startSingleFilePlayback() {
        val uri = singleFileUri?.toUri() ?: return
        mediaPlayer?.runCatching { release() }
        mediaPlayer = MediaPlayer().apply {
            runCatching {
                setDataSource(context, uri)
                setVolume(vol, vol)
                isLooping = true
                setOnPreparedListener { start() }
                setOnErrorListener { _, _, _ -> true }
                prepareAsync()
            }
        }
    }
}
