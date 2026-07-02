package jr.brian.home.esde.ui.video

import android.R as AndroidR
import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import jr.brian.home.esde.model.VideoScaleMode
import java.io.File

@UnstableApi
class VideoPresentation(
    context: Context,
    display: Display,
    private val videoPath: String,
    private val audioEnabled: Boolean,
    private val scaleMode: VideoScaleMode,
    private val overlayEnabled: Boolean
) : Presentation(context, display) {

    private var exoPlayer: ExoPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var hideOverlayRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!File(videoPath).exists()) {
            dismiss()
            return
        }

        exoPlayer = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(File(videoPath).toUri()))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            volume = if (audioEnabled) 1.0f else 0.0f
            prepare()
        }

        val density = context.resources.displayMetrics.density

        val playerView = PlayerView(context).apply {
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

        val overlaySize = (72 * density).toInt()
        val overlayPadding = (16 * density).toInt()

        val overlayIcon = AppCompatImageView(context).apply {
            setImageResource(AndroidR.drawable.ic_media_pause)
            setColorFilter(Color.WHITE)
            setPadding(overlayPadding, overlayPadding, overlayPadding, overlayPadding)
            alpha = 0f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x88000000.toInt())
            }
            layoutParams = FrameLayout.LayoutParams(overlaySize, overlaySize, Gravity.CENTER)
        }

        val closeSize = (48 * density).toInt()
        val closeMargin = (16 * density).toInt()
        val closePadding = (10 * density).toInt()

        val closeButton = AppCompatImageView(context).apply {
            setImageResource(AndroidR.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            setPadding(closePadding, closePadding, closePadding, closePadding)
            alpha = 0f
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
            setOnClickListener { dismiss() }
        }

        hideOverlayRunnable = Runnable {
            overlayIcon.animate().alpha(0f).setDuration(300).start()
            closeButton.animate().alpha(0f).setDuration(300).start()
        }

        fun showOverlays(playWhenReady: Boolean) {
            if (!overlayEnabled) return

            overlayIcon.setImageResource(
                if (playWhenReady) AndroidR.drawable.ic_media_play
                else AndroidR.drawable.ic_media_pause
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

        playerView.setOnClickListener {
            exoPlayer?.let { player ->
                if (overlayEnabled) {
                    player.playWhenReady = !player.playWhenReady
                    showOverlays(player.playWhenReady)
                } else {
                    dismiss()
                }
            }
        }

        val rootLayout = FrameLayout(context).apply {
            addView(playerView)
            if (overlayEnabled) {
                addView(overlayIcon)
                addView(closeButton)
            }
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        setContentView(rootLayout)
    }

    fun pauseVideo() {
        exoPlayer?.playWhenReady = false
    }

    fun resumeVideo() {
        exoPlayer?.playWhenReady = true
    }

    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying == true
    }

    fun setMuted(muted: Boolean) {
        exoPlayer?.volume = if (muted) 0f else 1f
    }

    override fun dismiss() {
        hideOverlayRunnable?.let { handler.removeCallbacks(it) }
        exoPlayer?.release()
        exoPlayer = null
        super.dismiss()
    }
}