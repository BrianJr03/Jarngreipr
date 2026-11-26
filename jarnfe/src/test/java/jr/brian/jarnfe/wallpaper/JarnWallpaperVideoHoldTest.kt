package jr.brian.jarnfe.wallpaper

import jr.brian.jarnfe.input.JarnScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnWallpaperVideoHoldTest {
    private val open = JarnWallpaperConditions(
        windows = listOf(
            JarnWallpaperWindow(JarnScreen.Stage, isResumed = true, isTopResumed = true),
            JarnWallpaperWindow(JarnScreen.Deck, isResumed = true, isTopResumed = false),
        ),
        deckDisplayAvailable = true,
        isScreenOn = true,
        isBatterySaverOn = false,
        isMotionReduced = false,
        isSplashPlaying = false,
    )
    private val showing = open.copy(isVideoPreviewShowing = true)

    @Test
    fun `no preview showing by default, so nothing changes`() {
        assertFalse(open.isVideoPreviewShowing)
        assertTrue(open.canAnimate())
        assertTrue(open.canPlayVideoPreview())
    }

    @Test
    fun `wallpapers hold still while a preview shows and move again after`() {
        assertFalse(showing.canAnimate())
        assertTrue(showing.copy(isVideoPreviewShowing = false).canAnimate())
    }

    @Test
    fun `the preview itself keeps playing while it shows`() {
        assertTrue(showing.canPlayVideoPreview())
    }

    @Test
    fun `a showing preview is not a pause for the note`() {
        assertNull(showing.pause())
    }

    @Test
    fun `the preview still stops for the usual reasons`() {
        assertFalse(showing.copy(isBatterySaverOn = true).canPlayVideoPreview())
        assertFalse(showing.copy(isMotionReduced = true).canPlayVideoPreview())
        assertFalse(showing.copy(isScreenOn = false).canPlayVideoPreview())
        assertFalse(showing.copy(isSplashPlaying = true).canPlayVideoPreview())
    }

    @Test
    fun `with Battery Saver kept running the preview still holds wallpapers`() {
        val kept = showing.copy(isBatterySaverOn = true, pausesOnBatterySaver = false)
        assertTrue(kept.canPlayVideoPreview())
        assertFalse(kept.canAnimate())
    }
}
