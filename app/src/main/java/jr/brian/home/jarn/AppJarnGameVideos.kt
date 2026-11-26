package jr.brian.home.jarn

import android.content.Context
import android.net.Uri
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import jr.brian.jarnfe.library.JarnGameVideo
import jr.brian.jarnfe.library.JarnGameVideoState
import jr.brian.jarnfe.library.JarnGameVideos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Game previews for Jarn FE, one ExoPlayer per preview. */
@Singleton
class AppJarnGameVideos @Inject constructor() : JarnGameVideos {
    override fun create(context: Context, path: String, crop: Boolean): JarnGameVideo =
        AppJarnGameVideo(context, path, crop)
}

/** Plain paths go through [File] so names with `#`, `?` or spaces still open. */
internal fun gameVideoUri(path: String): Uri? = when {
    path.isBlank() -> null
    path.startsWith("content://") -> path.toUri()
    else -> Uri.fromFile(File(path))
}

/**
 * A [TextureView] rather than a SurfaceView, so Compose can fade and clip it over art. No audio
 * focus is taken, muted or not: a preview must never pause or duck the user's music, and Jarn FE
 * only unmutes one while its own music is held and no other media is playing.
 */
@OptIn(UnstableApi::class)
private class AppJarnGameVideo(context: Context, path: String, crop: Boolean) : JarnGameVideo {
    private val player = ExoPlayer.Builder(context.applicationContext).build().apply {
        repeatMode = Player.REPEAT_MODE_ONE
        volume = 0f
    }
    private val texture = TextureView(context)
    private val frame = AspectRatioFrameLayout(context).apply {
        resizeMode = if (crop) {
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        } else {
            AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        addView(texture, FrameLayout.LayoutParams(MATCH, MATCH))
    }
    private val _state = MutableStateFlow(JarnGameVideoState.Loading)
    private var released = false

    override val view: View = FrameLayout(context).apply {
        clipChildren = true
        addView(frame, FrameLayout.LayoutParams(MATCH, MATCH, Gravity.CENTER))
    }
    override val state: StateFlow<JarnGameVideoState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onRenderedFirstFrame() {
            if (_state.value == JarnGameVideoState.Loading) _state.value = JarnGameVideoState.Ready
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                frame.setAspectRatio(
                    videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height,
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.value = JarnGameVideoState.Failed
        }
    }

    init {
        val uri = gameVideoUri(path)
        if (uri == null) {
            _state.value = JarnGameVideoState.Failed
        } else {
            player.addListener(listener)
            player.setVideoTextureView(texture)
            player.setMediaItem(MediaItem.fromUri(uri))
            player.playWhenReady = true
            player.prepare()
        }
    }

    override fun play() {
        if (!released) player.playWhenReady = true
    }

    override fun pause() {
        if (!released) player.playWhenReady = false
    }

    override fun setMuted(muted: Boolean) {
        if (!released) player.volume = if (muted) 0f else 1f
    }

    override fun release() {
        if (released) return
        released = true
        player.removeListener(listener)
        player.clearVideoTextureView(texture)
        player.release()
    }

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    }
}
