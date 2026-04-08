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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private val _currentItemId = MutableStateFlow<String?>(null)
    val currentItemId: StateFlow<String?> = _currentItemId.asStateFlow()

    private val _currentFeedUrl = MutableStateFlow<String?>(null)
    val currentFeedUrl: StateFlow<String?> = _currentFeedUrl.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private var currentQueue: List<RssItem> = emptyList()

    private var controller: MediaController? = null
    private val controllerFuture: ListenableFuture<MediaController>
    private var pendingPlay: (() -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pollingJob: Job? = null

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

    private fun startPositionPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (true) {
                controller?.let { ctrl ->
                    _currentPosition.value = ctrl.currentPosition.coerceAtLeast(0L)
                    _duration.value = if (ctrl.duration == C.TIME_UNSET) 0L else ctrl.duration
                }
                delay(500L)
            }
        }
    }

    private fun stopPositionPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _nowPlaying.value = _nowPlaying.value?.copy(isPlaying = isPlaying)
            if (isPlaying) startPositionPolling() else stopPositionPolling()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val title = mediaMetadata.title?.toString() ?: return
            _nowPlaying.value = NowPlayingInfo(
                title = title,
                artist = mediaMetadata.artist?.toString(),
                isPlaying = controller?.isPlaying == true
            )
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val idx = controller?.currentMediaItemIndex ?: return
            val item = currentQueue.getOrNull(idx)
            _currentItemId.value = item?.id
            _currentFeedUrl.value = item?.feedUrl
            _currentPosition.value = 0L
            _duration.value = 0L
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_IDLE) {
                _nowPlaying.value = null
                _currentItemId.value = null
                _currentFeedUrl.value = null
                _currentPosition.value = 0L
                _duration.value = 0L
                stopPositionPolling()
            }
        }
    }

    fun play(audioItems: List<RssItem>, startIndex: Int) {
        currentQueue = audioItems
        val item = audioItems.getOrNull(startIndex)
        _currentItemId.value = item?.id
        _currentFeedUrl.value = item?.feedUrl

        val mediaItems = audioItems.map { rssItem ->
            MediaItem.Builder()
                .setUri(rssItem.audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(rssItem.title)
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

    fun setVolume(volume: Float) {
        _volume.value = volume.coerceIn(0f, 1f)
        controller?.volume = _volume.value
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun release() {
        stopPositionPolling()
        scope.cancel()
        MediaController.releaseFuture(controllerFuture)
    }
}
