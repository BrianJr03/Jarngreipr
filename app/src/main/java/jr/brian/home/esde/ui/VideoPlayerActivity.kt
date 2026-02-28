package jr.brian.home.esde.ui

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import jr.brian.home.esde.model.VideoScaleMode
import java.io.File
import java.lang.ref.WeakReference

@UnstableApi
class VideoPlayerActivity : ComponentActivity() {
    private var playerRef: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var checkForegroundRunnable: Runnable? = null

    companion object {
        const val EXTRA_VIDEO_PATH = "video_path"
        const val EXTRA_AUDIO_ENABLED = "audio_enabled"
        const val EXTRA_SCALE_MODE = "scale_mode"

        private var currentInstance: WeakReference<VideoPlayerActivity>? = null

        fun finishIfRunning() {
            currentInstance?.get()?.finish()
            currentInstance = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure we don't trap touches intended for the other screen on dual-screen devices
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)

        currentInstance = WeakReference(this)

        val videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH)
        val audioEnabled = intent.getBooleanExtra(EXTRA_AUDIO_ENABLED, false)
        val scaleModeName = intent.getStringExtra(EXTRA_SCALE_MODE) ?: VideoScaleMode.FitVideo.name
        val scaleMode = VideoScaleMode.valueOf(scaleModeName)

        if (videoPath == null || !File(videoPath).exists()) {
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            VideoPlayerScreen(
                videoPath = videoPath,
                audioEnabled = audioEnabled,
                scaleMode = scaleMode,
                onPlayerReady = { playerRef = it },
                onDismiss = { finish() }
            )
        }

        startForegroundMonitoring()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            // 1. Let volume keys work normally
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE -> return super.onKeyDown(keyCode, event)

            // 2. Button A / Enter: Toggle Play/Pause
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                playerRef?.let { player ->
                    player.playWhenReady = !player.playWhenReady
                }
                return true
            }

            // 3. D-PAD / JOYSTICK: This is the fix for "Lost Control"
            // If user scrolls, we finish immediately so focus returns to ES-DE
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                finish()
                return false // Return false to let the system pass the event to the launcher
            }

            // 4. Button B / Back / Any other key: Close video
            KeyEvent.KEYCODE_BUTTON_B ->{
                finish()
                return true
            }
        }

        finish()
        return true
    }

    private fun startForegroundMonitoring() {
        checkForegroundRunnable = object : Runnable {
            override fun run() {
                if (!isESDEInForeground()) {
                    finish()
                } else {
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(checkForegroundRunnable!!, 1000)
    }

    private fun isESDEInForeground(): Boolean {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryEvents(time - 3000, time)
        val event = UsageEvents.Event()
        var lastEvent: Int = -1

        while (stats.hasNextEvent()) {
            stats.getNextEvent(event)
            if (event.packageName == "jr.brian.home.esde" || event.packageName == packageName) {
                lastEvent = event.eventType
            }
        }
        return lastEvent == UsageEvents.Event.ACTIVITY_RESUMED
    }

    override fun onDestroy() {
        super.onDestroy()
        checkForegroundRunnable?.let { handler.removeCallbacks(it) }
        playerRef?.release()
        playerRef = null
    }
}

@Composable
@UnstableApi
fun VideoPlayerScreen(
    videoPath: String,
    audioEnabled: Boolean,
    scaleMode: VideoScaleMode,
    onPlayerReady: (ExoPlayer) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(File(videoPath).toUri())
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            volume = if (audioEnabled) 1.0f else 0.0f
            prepare()
            onPlayerReady(this)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = when (scaleMode) {
                        VideoScaleMode.FitVideo -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        VideoScaleMode.FillScreen -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}