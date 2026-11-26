package jr.brian.jarnfe.wallpaper

import jr.brian.jarnfe.input.JarnScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnBatterySaverGateTest {
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

    private fun conditions(saverOn: Boolean, pausesOnSaver: Boolean) =
        open.copy(isBatterySaverOn = saverOn, pausesOnBatterySaver = pausesOnSaver)

    @Test
    fun `the setting is on by default and pauses on Battery Saver`() {
        assertTrue(open.pausesOnBatterySaver)
        assertFalse(conditions(saverOn = true, pausesOnSaver = true).canAnimate())
        assertEquals(JarnWallpaperPause.BatterySaver, conditions(saverOn = true, pausesOnSaver = true).pause())
    }

    @Test
    fun `with the setting off Battery Saver no longer pauses`() {
        val kept = conditions(saverOn = true, pausesOnSaver = false)
        assertTrue(kept.canAnimate())
        assertNull(kept.pause())
    }

    @Test
    fun `without Battery Saver the setting changes nothing`() {
        assertTrue(conditions(saverOn = false, pausesOnSaver = true).canAnimate())
        assertTrue(conditions(saverOn = false, pausesOnSaver = false).canAnimate())
    }

    @Test
    fun `reduce motion still pauses with the setting off`() {
        val reduced = conditions(saverOn = true, pausesOnSaver = false).copy(isMotionReduced = true)
        assertFalse(reduced.canAnimate())
        assertEquals(JarnWallpaperPause.ReduceMotion, reduced.pause())
    }

    @Test
    fun `a game, a dark screen and the splash still pause with the setting off`() {
        val kept = conditions(saverOn = true, pausesOnSaver = false)
        assertFalse(kept.copy(isScreenOn = false).canAnimate())
        assertFalse(kept.copy(isSplashPlaying = true).canAnimate())
        val covered = kept.windows.map { it.copy(isResumed = it.screen == JarnScreen.Deck) }
        assertFalse(kept.copy(windows = covered, isGameRunning = true).canAnimate())
    }
}
