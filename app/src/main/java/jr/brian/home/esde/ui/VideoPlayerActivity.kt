package jr.brian.home.esde.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.AppCompatImageView
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
    private var exoPlayer: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var checkForegroundRunnable: Runnable? = null
    private var hideOverlayRunnable: Runnable? = null

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
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        )

        exoPlayer = ExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri(File(videoPath).toUri()))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            volume = if (audioEnabled) 1.0f else 0.0f
            prepare()
        }

        val density = resources.displayMetrics.density

        // -----------------------------------------------------------------------
        // PlayerView
        // -----------------------------------------------------------------------
        val playerView = PlayerView(this).apply {
            player = exoPlayer
            useController = false
            resizeMode = when (scaleMode) {
                VideoScaleMode.FitVideo -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                VideoScaleMode.FillScreen -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        // -----------------------------------------------------------------------
        // Centre play/pause overlay
        // -----------------------------------------------------------------------
        val overlaySize = (72 * density).toInt()
        val overlayPadding = (16 * density).toInt()

        val overlayIcon = AppCompatImageView(this).apply {
            setImageResource(android.R.drawable.ic_media_pause)
            setColorFilter(Color.WHITE)
            setPadding(overlayPadding, overlayPadding, overlayPadding, overlayPadding)
            alpha = 0f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x88000000.toInt())
            }
            layoutParams = FrameLayout.LayoutParams(overlaySize, overlaySize, Gravity.CENTER)
        }

        // -----------------------------------------------------------------------
        // Close button — top right
        // -----------------------------------------------------------------------
        val closeSize = (48 * density).toInt()
        val closeMargin = (16 * density).toInt()
        val closePadding = (10 * density).toInt()

        val closeButton = AppCompatImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            setPadding(closePadding, closePadding, closePadding, closePadding)
            alpha = 0f  // hidden by default, shown alongside play/pause overlay
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x88000000.toInt())
            }
            layoutParams = FrameLayout.LayoutParams(
                closeSize,
                closeSize,
                Gravity.TOP or Gravity.END
            ).apply {
                topMargin = closeMargin
                marginEnd = closeMargin
            }
            setOnClickListener { finish() }
        }

        // -----------------------------------------------------------------------
        // Shared hide runnable — fades out both overlay and close button together
        // -----------------------------------------------------------------------
        hideOverlayRunnable = Runnable {
            overlayIcon.animate().alpha(0f).setDuration(300).start()
            closeButton.animate().alpha(0f).setDuration(300).start()
        }

        // -----------------------------------------------------------------------
        // Helper to flash both overlays
        // -----------------------------------------------------------------------
        fun showOverlays(playWhenReady: Boolean) {
            overlayIcon.setImageResource(
                if (playWhenReady) android.R.drawable.ic_media_play
                else android.R.drawable.ic_media_pause
            )

            hideOverlayRunnable?.let { handler.removeCallbacks(it) }
            overlayIcon.animate().cancel()
            closeButton.animate().cancel()

            overlayIcon.alpha = 0f
            closeButton.alpha = 0f

            overlayIcon.animate().alpha(1f).setDuration(200).start()
            closeButton.animate().alpha(1f).setDuration(200).withEndAction {
                hideOverlayRunnable?.let { handler.postDelayed(it, 1500) }
            }.start()
        }

        // -----------------------------------------------------------------------
        // Tap to toggle play/pause
        // -----------------------------------------------------------------------
        playerView.setOnClickListener {
            exoPlayer?.let { player ->
                player.playWhenReady = !player.playWhenReady
                showOverlays(player.playWhenReady)
            }
        }

        // -----------------------------------------------------------------------
        // Root layout
        // -----------------------------------------------------------------------
        val rootLayout = FrameLayout(this).apply {
            addView(playerView)
            addView(overlayIcon)
            addView(closeButton)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        setContentView(rootLayout)

//        handler.postDelayed({ startForegroundMonitoring() }, 3000)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE -> return super.onKeyDown(keyCode, event)

            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                exoPlayer?.let { it.playWhenReady = !it.playWhenReady }
                return true
            }

            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                finish()
                return false
            }

            KeyEvent.KEYCODE_BUTTON_B -> {
                finish()
                return true
            }
        }

        finish()
        return true
    }

//    private fun startForegroundMonitoring() {
//        checkForegroundRunnable = object : Runnable {
//            override fun run() {
//                if (!isESDEOrSelfInForeground()) {
//                    finish()
//                } else {
//                    handler.postDelayed(this, 1000)
//                }
//            }
//        }
//        handler.post(checkForegroundRunnable!!)
//    }

//    private fun isESDEOrSelfInForeground(): Boolean {
//        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
//        val time = System.currentTimeMillis()
//        val stats = usageStatsManager.queryEvents(time - 3000, time)
//        val event = UsageEvents.Event()
//        var lastResumedPackage: String? = null
//        while (stats.hasNextEvent()) {
//            stats.getNextEvent(event)
//            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
//                lastResumedPackage = event.packageName
//            }
//        }
//        return lastResumedPackage == "jr.brian.home.esde" ||
//                lastResumedPackage == packageName
//    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlayRunnable?.let { handler.removeCallbacks(it) }
        checkForegroundRunnable?.let { handler.removeCallbacks(it) }
        exoPlayer?.release()
        exoPlayer = null
    }
}