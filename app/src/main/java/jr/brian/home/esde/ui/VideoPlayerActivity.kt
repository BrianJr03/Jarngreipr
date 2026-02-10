package jr.brian.home.esde.ui

import android.os.Bundle
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import java.lang.ref.WeakReference
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import jr.brian.home.esde.preferences.VideoScaleMode
import java.io.File

/**
 * Standalone Activity for fullscreen video playback.
 * 
 * This activity is launched when a game video should be played.
 * It finishes and returns to the launcher when:
 * - The user touches the screen
 * - The user presses the back button
 * 
 * Intent extras:
 * - EXTRA_VIDEO_PATH: String - The path to the video file to play
 * - EXTRA_AUDIO_ENABLED: Boolean - Whether to enable audio (default: false)
 */
class VideoPlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_VIDEO_PATH = "extra_video_path"
        const val EXTRA_AUDIO_ENABLED = "extra_audio_enabled"
        const val EXTRA_SCALE_MODE = "extra_scale_mode"
        
        private var currentInstance: WeakReference<VideoPlayerActivity>? = null
        
        /**
         * Finishes any currently running VideoPlayerActivity instance.
         * Called when browsing games or systems to dismiss the video.
         */
        fun finishIfRunning() {
            currentInstance?.get()?.finish()
            currentInstance = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentInstance = WeakReference(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH)
        val audioEnabled = intent.getBooleanExtra(EXTRA_AUDIO_ENABLED, false)
        val scaleModeName = intent.getStringExtra(EXTRA_SCALE_MODE)
        val scaleMode = try {
            scaleModeName?.let { VideoScaleMode.valueOf(it) } ?: VideoScaleMode.FillScreen
        } catch (_: IllegalArgumentException) {
            VideoScaleMode.FillScreen
        }

        if (videoPath == null) {
            finish()
            return
        }

        setContent {
            VideoPlayerScreen(
                videoPath = videoPath,
                audioEnabled = audioEnabled,
                scaleMode = scaleMode,
                onDismiss = { finish() }
            )
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            finish()
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentInstance?.get() == this) {
            currentInstance = null
        }
    }
}

@UnstableApi
@Composable
private fun VideoPlayerScreen(
    videoPath: String,
    audioEnabled: Boolean,
    scaleMode: VideoScaleMode,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    val exoPlayer = remember(videoPath) {
        ExoPlayer.Builder(context).build().apply {
            try {
                val file = File(videoPath)
                if (file.exists()) {
                    setMediaItem(MediaItem.fromUri(file.toUri()))
                    repeatMode = Player.REPEAT_MODE_ALL
                    volume = if (audioEnabled) 1f else 0f
                    prepare()
                    playWhenReady = true
                }
            } catch (_: Exception) {
                // Video failed to load, dismiss
            }
        }
    }

    LaunchedEffect(audioEnabled) {
        exoPlayer.volume = if (audioEnabled) 1f else 0f
    }

    DisposableEffect(videoPath) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onDismiss
            )
    ) {
        val resizeMode = when (scaleMode) {
            VideoScaleMode.FillScreen -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            VideoScaleMode.FitVideo -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
