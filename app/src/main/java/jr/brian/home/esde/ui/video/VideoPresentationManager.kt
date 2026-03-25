package jr.brian.home.esde.ui.video

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.media3.common.util.UnstableApi
import jr.brian.home.esde.model.VideoScaleMode

@UnstableApi
object VideoPresentationManager {
    private var activePresentation: VideoPresentation? = null

    /**
     * Shows the video on the first available external/presentation display.
     * Falls back to the default display if none found.
     *
     * @param context       Application or Activity context
     * @param videoPath     Absolute path to the video file
     * @param audioEnabled  Whether to play audio
     * @param scaleMode     FitVideo or FillScreen
     * @param overlayEnabled Whether to show play/pause and close overlays on tap
     */
    fun show(
        context: Context,
        videoPath: String,
        audioEnabled: Boolean = false,
        scaleMode: VideoScaleMode = VideoScaleMode.FitVideo,
        overlayEnabled: Boolean = true
    ) {
        dismiss() // dismiss any existing presentation first

        val display = getPresentationDisplay(context) ?: return

        activePresentation = VideoPresentation(
            context = context.applicationContext,
            display = display,
            videoPath = videoPath,
            audioEnabled = audioEnabled,
            scaleMode = scaleMode,
            overlayEnabled = overlayEnabled
        ).also { it.show() }
    }

    /**
     * Dismisses the active presentation and releases the player.
     */
    fun dismiss() {
        activePresentation?.dismiss()
        activePresentation = null
    }

    @Suppress("unused")
    fun pauseVideo() {
        activePresentation?.pauseVideo()
    }

    @Suppress("unused")
    fun resumeVideo() {
        activePresentation?.resumeVideo()
    }

    @Suppress("unused")
    fun isShowing(): Boolean {
        return activePresentation?.isShowing == true
    }

    @Suppress("unused")
    fun isPlaying(): Boolean {
        return activePresentation?.isPlaying() == true
    }

    fun setMuted(muted: Boolean) {
        activePresentation?.setMuted(muted)
    }

    /**
     * Returns the best display for the Presentation:
     * Prefers DISPLAY_CATEGORY_PRESENTATION displays (external/secondary),
     * falls back to the default display if none are available.
     */
    private fun getPresentationDisplay(context: Context): Display? {
        val displayManager =
            context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

        // Prefer dedicated presentation displays (external HDMI, cast, etc.)
        val presentationDisplays =
            displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)

        if (presentationDisplays.isNotEmpty()) {
            return presentationDisplays[0]
        }

        // Fallback: all displays except the default one
        val allDisplays = displayManager.displays
        val external = allDisplays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        if (external != null) return external

        // Last resort: default display (won't steal focus issue but at least plays)
        return displayManager.getDisplay(Display.DEFAULT_DISPLAY)
    }
}