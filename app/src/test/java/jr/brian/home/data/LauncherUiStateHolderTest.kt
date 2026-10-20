package jr.brian.home.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the defaults and mutator contract of [LauncherUiStateHolder].
 * Pinned-slot swap moves the launcher content between activities;
 * these mutators are the single source of truth for the transient
 * UI state that used to live in `MainActivity`'s remember-locals.
 */
class LauncherUiStateHolderTest {

    @Test
    fun `fresh holder has sensible defaults`() {
        val holder = LauncherUiStateHolder()

        assertFalse(holder.triggerMarqueePressShortcut.value)
        assertFalse(holder.isAnyOverlayVisible.value)
        assertEquals(0, holder.currentPageIndex.value)
        assertEquals(0f, holder.pagerScrollProgress.value, 0f)
        assertFalse(holder.hideLauncherUIForScreensaver.value)
        assertFalse(holder.hideLauncherUIForGameBrowsing.value)
        assertNull(holder.dockTopY.value)
    }

    @Test
    fun `triggerMarquee flips the one-shot, markMarqueeHandled clears it`() {
        val holder = LauncherUiStateHolder()

        holder.triggerMarquee()
        assertTrue(holder.triggerMarqueePressShortcut.value)

        holder.markMarqueeHandled()
        assertFalse(holder.triggerMarqueePressShortcut.value)
    }

    @Test
    fun `setters round-trip every field`() {
        val holder = LauncherUiStateHolder()

        holder.setAnyOverlayVisible(true)
        holder.setCurrentPageIndex(3)
        holder.setPagerScrollProgress(0.42f)
        holder.setHideLauncherUIForScreensaver(true)
        holder.setHideLauncherUIForGameBrowsing(true)
        holder.setDockTopY(128f)

        assertTrue(holder.isAnyOverlayVisible.value)
        assertEquals(3, holder.currentPageIndex.value)
        assertEquals(0.42f, holder.pagerScrollProgress.value, 0f)
        assertTrue(holder.hideLauncherUIForScreensaver.value)
        assertTrue(holder.hideLauncherUIForGameBrowsing.value)
        assertEquals(128f, holder.dockTopY.value)
    }

    @Test
    fun `dockTopY can be reset to null`() {
        val holder = LauncherUiStateHolder()

        holder.setDockTopY(42f)
        assertEquals(42f, holder.dockTopY.value)

        holder.setDockTopY(null)
        assertNull(holder.dockTopY.value)
    }
}
