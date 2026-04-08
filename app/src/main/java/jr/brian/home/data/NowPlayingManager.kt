package jr.brian.home.data

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NowPlayingManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    data class NowPlayingInfo(
        val title: String,
        val artist: String? = null,
        val isPlaying: Boolean = false
    )

    private val _nowPlaying = MutableStateFlow<NowPlayingInfo?>(null)
    val nowPlaying: StateFlow<NowPlayingInfo?> = _nowPlaying.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentController: MediaController? = null
    private var currentCallback: MediaController.Callback? = null

    fun onMediaSession(token: MediaSession.Token) {
        mainHandler.post {
            val controller = MediaController(context, token)
            val metadata = controller.metadata
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?: return@post
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING

            _nowPlaying.value = NowPlayingInfo(title = title, artist = artist, isPlaying = isPlaying)

            if (currentController?.sessionToken != token) {
                detachCallback()
                attachCallback(controller)
            }
        }
    }

    fun onMediaSessionRemoved(hasRemainingMediaSessions: Boolean) {
        if (!hasRemainingMediaSessions) {
            mainHandler.post {
                _nowPlaying.value = null
                detachCallback()
            }
        }
    }

    fun togglePlayPause() {
        val controller = currentController ?: return
        if (_nowPlaying.value?.isPlaying == true) controller.transportControls.pause()
        else controller.transportControls.play()
    }

    private fun attachCallback(controller: MediaController) {
        val cb = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                _nowPlaying.value = _nowPlaying.value?.copy(
                    isPlaying = state?.state == PlaybackState.STATE_PLAYING
                )
            }
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                    ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                    ?: return
                val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                _nowPlaying.value = _nowPlaying.value?.copy(title = title, artist = artist)
            }
            override fun onSessionDestroyed() {
                _nowPlaying.value = null
                currentController = null
                currentCallback = null
            }
        }
        controller.registerCallback(cb, mainHandler)
        currentController = controller
        currentCallback = cb
    }

    private fun detachCallback() {
        val cb = currentCallback ?: return
        try { currentController?.unregisterCallback(cb) } catch (_: Exception) {}
        currentController = null
        currentCallback = null
    }
}
