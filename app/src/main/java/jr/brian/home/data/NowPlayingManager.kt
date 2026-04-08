package jr.brian.home.data

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.model.rss.RssItem
import jr.brian.home.service.RssPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NowPlayingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class NowPlayingInfo(
        val title: String,
        val artist: String? = null,
        val isPlaying: Boolean = false
    )

    private val _nowPlaying = MutableStateFlow<NowPlayingInfo?>(null)
    val nowPlaying: StateFlow<NowPlayingInfo?> = _nowPlaying.asStateFlow()

    private var controller: MediaController? = null
    private val controllerFuture: ListenableFuture<MediaController>
    private var pendingPlay: (() -> Unit)? = null

    init {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, RssPlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                controller = controllerFuture.get().also { ctrl ->
                    ctrl.addListener(playerListener)
                    pendingPlay?.invoke()
                    pendingPlay = null
                }
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _nowPlaying.value = _nowPlaying.value?.copy(isPlaying = isPlaying)
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val title = mediaMetadata.title?.toString() ?: return
            _nowPlaying.value = NowPlayingInfo(
                title = title,
                artist = mediaMetadata.artist?.toString(),
                isPlaying = controller?.isPlaying == true
            )
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_IDLE) {
                _nowPlaying.value = null
            }
        }
    }

    fun play(audioItems: List<RssItem>, startIndex: Int) {
        val mediaItems = audioItems.map { item ->
            MediaItem.Builder()
                .setUri(item.audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .build()
                )
                .build()
        }
        val ctrl = controller
        if (ctrl != null) {
            ctrl.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
            ctrl.prepare()
            ctrl.play()
        } else {
            pendingPlay = {
                controller?.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
                controller?.prepare()
                controller?.play()
            }
        }
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun skipToNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun release() {
        MediaController.releaseFuture(controllerFuture)
    }
}
