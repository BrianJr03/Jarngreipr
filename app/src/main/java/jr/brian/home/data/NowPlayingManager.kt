package jr.brian.home.data

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaMetadata as PlatformMediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.model.rss.RssItem
import jr.brian.home.service.AppNotificationListenerService
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
        val imageUrl: String? = null,
        val isPlaying: Boolean = false,
        val isBuffering: Boolean = false,
        val isSystemMedia: Boolean = false
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

    private val savedPositions = HashMap<String, Long>()
    private val playTimePrefs: SharedPreferences =
        context.getSharedPreferences(PLAY_TIME_PREFS, Context.MODE_PRIVATE)

    private val _savedPositionCount = MutableStateFlow(0)
    val savedPositionCount: StateFlow<Int> = _savedPositionCount.asStateFlow()

    private fun saveCurrentPosition() {
        val itemId = _currentItemId.value ?: return
        val pos = _currentPosition.value
        if (pos > 0L) {
            savedPositions[itemId] = pos
            playTimePrefs.edit { putLong(itemId, pos) }
            _savedPositionCount.value = savedPositions.size
        }
    }

    fun clearSavedPositions() {
        savedPositions.clear()
        playTimePrefs.edit { clear() }
        _savedPositionCount.value = 0
    }

    private var controller: MediaController? = null
    private val controllerFuture: ListenableFuture<MediaController>
    private var pendingPlay: (() -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pollingJob: Job? = null

    // Set to true while play() is setting up a new RSS item, so STATE_IDLE
    // transitions from ExoPlayer don't clobber state or re-attach system media.
    private var isInitiatingPlayback = false

    // System media monitoring
    private var isShowingSystemMedia = false
    private var systemMediaController: android.media.session.MediaController? = null
    private var systemControllerCallback: android.media.session.MediaController.Callback? = null
    private var systemSessionListener: MediaSessionManager.OnActiveSessionsChangedListener? = null

    init {
        @Suppress("UNCHECKED_CAST")
        playTimePrefs.all.forEach { (k, v) -> (v as? Long)?.let { savedPositions[k] = it } }
        _savedPositionCount.value = savedPositions.size

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
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(context))

        initSystemMediaMonitoring()
    }

    fun refreshSystemMedia() {
        if (_nowPlaying.value != null && !isShowingSystemMedia) return
        tryUpdateSystemMedia()
    }

    private fun initSystemMediaMonitoring() {
        val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE)
            as? MediaSessionManager ?: return
        val listenerComponent = ComponentName(context, AppNotificationListenerService::class.java)
        val mainHandler = Handler(Looper.getMainLooper())

        systemSessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            val rssIsActivePlaying = _nowPlaying.value != null && !isShowingSystemMedia && controller?.isPlaying == true
            if (!rssIsActivePlaying) {
                updateSystemMediaController(controllers)
            }
        }

        try {
            sessionManager.addOnActiveSessionsChangedListener(
                systemSessionListener!!,
                listenerComponent,
                mainHandler
            )
            tryUpdateSystemMedia()
        } catch (_: SecurityException) {
            // Notification listener permission not yet granted; retried from refreshSystemMedia()
        } catch (_: Exception) {}
    }

    private fun tryUpdateSystemMedia() {
        val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE)
            as? MediaSessionManager ?: return
        val listenerComponent = ComponentName(context, AppNotificationListenerService::class.java)
        try {
            updateSystemMediaController(sessionManager.getActiveSessions(listenerComponent))
        } catch (_: SecurityException) {
        } catch (_: Exception) {}
    }

    private fun updateSystemMediaController(
        controllers: List<android.media.session.MediaController>?
    ) {
        val candidate = controllers
            ?.filter { it.packageName != context.packageName }
            ?.firstOrNull { c ->
                val state = c.playbackState?.state
                state == PlaybackState.STATE_PLAYING ||
                    state == PlaybackState.STATE_PAUSED ||
                    state == PlaybackState.STATE_BUFFERING
            }

        if (candidate != null) {
            attachSystemController(candidate)
        } else if (isShowingSystemMedia) {
            detachSystemController()
        }
    }

    private fun attachSystemController(newController: android.media.session.MediaController) {
        if (systemMediaController?.sessionToken == newController.sessionToken) return
        detachCurrentSystemCallback()
        systemMediaController = newController
        isShowingSystemMedia = true

        updateFromSystemController(newController)

        val cb = object : android.media.session.MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                if (!isShowingSystemMedia) return
                val isPlaying = state?.state == PlaybackState.STATE_PLAYING
                val isBuffering = state?.state == PlaybackState.STATE_BUFFERING
                _nowPlaying.value = _nowPlaying.value?.copy(
                    isPlaying = isPlaying,
                    isBuffering = isBuffering
                )
                if (isPlaying) startPositionPolling() else stopPositionPolling()
                val s = state?.state
                if (s == PlaybackState.STATE_STOPPED ||
                    s == PlaybackState.STATE_NONE ||
                    s == PlaybackState.STATE_ERROR ||
                    state == null
                ) {
                    detachSystemController()
                }
            }

            override fun onMetadataChanged(metadata: PlatformMediaMetadata?) {
                if (!isShowingSystemMedia) return
                val title = metadata?.getString(PlatformMediaMetadata.METADATA_KEY_TITLE) ?: return
                val artist = metadata.getString(PlatformMediaMetadata.METADATA_KEY_ARTIST)
                val dur = metadata.getLong(PlatformMediaMetadata.METADATA_KEY_DURATION)
                    .takeIf { it > 0 } ?: 0L
                val artUri = metadata.getString(PlatformMediaMetadata.METADATA_KEY_ART_URI)?.takeIf { it.isNotEmpty() }
                _duration.value = dur
                _nowPlaying.value = _nowPlaying.value?.copy(
                    title = title,
                    artist = artist,
                    imageUrl = artUri ?: _nowPlaying.value?.imageUrl,
                    isSystemMedia = true
                )
            }

            override fun onSessionDestroyed() {
                if (isShowingSystemMedia) detachSystemController()
            }
        }
        systemControllerCallback = cb
        newController.registerCallback(cb, Handler(Looper.getMainLooper()))
    }

    private fun detachCurrentSystemCallback() {
        systemControllerCallback?.let { systemMediaController?.unregisterCallback(it) }
        systemControllerCallback = null
    }

    private fun detachSystemController() {
        detachCurrentSystemCallback()
        systemMediaController = null
        isShowingSystemMedia = false
        _nowPlaying.value = null
        _currentPosition.value = 0L
        _duration.value = 0L
        stopPositionPolling()
    }

    private fun updateFromSystemController(ctrl: android.media.session.MediaController) {
        val metadata = ctrl.metadata
        val title = metadata?.getString(PlatformMediaMetadata.METADATA_KEY_TITLE) ?: return
        val artist = metadata.getString(PlatformMediaMetadata.METADATA_KEY_ARTIST)
        val dur = metadata.getLong(PlatformMediaMetadata.METADATA_KEY_DURATION)
            .takeIf { it > 0 } ?: 0L
        val state = ctrl.playbackState
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val isBuffering = state?.state == PlaybackState.STATE_BUFFERING
        val position = state?.position?.coerceAtLeast(0L) ?: 0L

        _nowPlaying.value = NowPlayingInfo(
            title = title,
            artist = artist,
            imageUrl = metadata?.getString(PlatformMediaMetadata.METADATA_KEY_ART_URI)?.takeIf { it.isNotEmpty() },
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            isSystemMedia = true
        )
        _duration.value = dur
        _currentPosition.value = position
        if (isPlaying) startPositionPolling()
    }

    private fun startPositionPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            var tickCount = 0
            while (true) {
                if (isShowingSystemMedia) {
                    systemMediaController?.let { sc ->
                        _currentPosition.value =
                            sc.playbackState?.position?.coerceAtLeast(0L) ?: 0L
                    }
                } else {
                    controller?.let { ctrl ->
                        _currentPosition.value = ctrl.currentPosition.coerceAtLeast(0L)
                        _duration.value = if (ctrl.duration == C.TIME_UNSET) 0L else ctrl.duration
                    }
                    if (++tickCount >= 2) {
                        saveCurrentPosition()
                        tickCount = 0
                    }
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
            if (isPlaying) startPositionPolling() else {
                saveCurrentPosition()
                stopPositionPolling()
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val title = mediaMetadata.title?.toString() ?: return
            val idx = controller?.currentMediaItemIndex ?: -1
            _nowPlaying.value = NowPlayingInfo(
                title = title,
                artist = mediaMetadata.artist?.toString(),
                imageUrl = currentQueue.getOrNull(idx)?.imageUrl?.takeIf { it.isNotEmpty() },
                isPlaying = controller?.isPlaying == true,
                isBuffering = controller?.playbackState == Player.STATE_BUFFERING
            )
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                val completedId = _currentItemId.value
                if (completedId != null) {
                    savedPositions.remove(completedId)
                    playTimePrefs.edit { remove(completedId) }
                    _savedPositionCount.value = savedPositions.size
                }
            } else {
                saveCurrentPosition()
            }
            val ctrl = controller ?: return
            val idx = ctrl.currentMediaItemIndex
            val item = currentQueue.getOrNull(idx)
            _currentItemId.value = item?.id
            _currentFeedUrl.value = item?.feedUrl
            val savedPos = item?.id?.let { savedPositions[it] }?.takeIf { it > 0L }
            if (savedPos != null) {
                ctrl.seekTo(savedPos)
                _currentPosition.value = savedPos
            } else {
                _currentPosition.value = 0L
            }
            _duration.value = 0L
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_IDLE) isInitiatingPlayback = false
            when (playbackState) {
                Player.STATE_BUFFERING ->
                    _nowPlaying.value = _nowPlaying.value?.copy(isBuffering = true)

                Player.STATE_READY ->
                    _nowPlaying.value = _nowPlaying.value?.copy(isBuffering = false)

                else -> {}
            }
            if (playbackState == Player.STATE_ENDED) {
                val completedId = _currentItemId.value
                if (completedId != null) {
                    savedPositions.remove(completedId)
                    playTimePrefs.edit { remove(completedId) }
                    _savedPositionCount.value = savedPositions.size
                }
            }
            if (playbackState == Player.STATE_IDLE && !isInitiatingPlayback) {
                _nowPlaying.value = null
                _currentItemId.value = null
                _currentFeedUrl.value = null
                _currentPosition.value = 0L
                _duration.value = 0L
                stopPositionPolling()
                tryUpdateSystemMedia()
            }
        }
    }

    fun play(audioItems: List<RssItem>, startIndex: Int) {
        if (isShowingSystemMedia) detachSystemController()
        isInitiatingPlayback = true
        saveCurrentPosition()
        _currentPosition.value = 0L
        currentQueue = audioItems
        val item = audioItems.getOrNull(startIndex)
        _currentItemId.value = item?.id
        _currentFeedUrl.value = item?.feedUrl

        // Immediately signal buffering so UI updates before any listener fires
        _nowPlaying.value = NowPlayingInfo(
            title = item?.title ?: "",
            imageUrl = item?.imageUrl?.takeIf { it.isNotEmpty() },
            isPlaying = false,
            isBuffering = true
        )

        val startPositionMs = item?.id?.let { savedPositions[it] } ?: C.TIME_UNSET

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
            ctrl.setMediaItems(mediaItems, startIndex, startPositionMs)
            ctrl.prepare()
            ctrl.play()
        } else {
            pendingPlay = {
                controller?.setMediaItems(mediaItems, startIndex, startPositionMs)
                controller?.prepare()
                controller?.play()
            }
        }
    }

    fun togglePlayPause() {
        if (isShowingSystemMedia) {
            val sc = systemMediaController ?: return
            val state = sc.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) sc.transportControls.pause()
            else sc.transportControls.play()
        } else {
            val ctrl = controller ?: return
            if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
        }
    }

    fun skipToNext() {
        if (isShowingSystemMedia) systemMediaController?.transportControls?.skipToNext()
        else controller?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        if (isShowingSystemMedia) systemMediaController?.transportControls?.skipToPrevious()
        else controller?.seekToPreviousMediaItem()
    }

    fun setVolume(volume: Float) {
        _volume.value = volume.coerceIn(0f, 1f)
        controller?.volume = _volume.value
    }

    fun seekTo(positionMs: Long) {
        if (isShowingSystemMedia) {
            systemMediaController?.transportControls?.seekTo(positionMs)
        } else {
            controller?.seekTo(positionMs)
        }
        _currentPosition.value = positionMs
    }

    fun release() {
        stopPositionPolling()
        scope.cancel()
        detachCurrentSystemCallback()
        systemMediaController = null
        systemSessionListener?.let {
            try {
                val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE)
                    as? MediaSessionManager
                sessionManager?.removeOnActiveSessionsChangedListener(it)
            } catch (_: Exception) {}
        }
        MediaController.releaseFuture(controllerFuture)
    }

    companion object {
        const val PLAY_TIME_PREFS = "rss_playtime_prefs"
    }
}
