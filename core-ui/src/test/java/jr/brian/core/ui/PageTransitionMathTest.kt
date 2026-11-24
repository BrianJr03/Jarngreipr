package jr.brian.core.ui

import jr.brian.core.ui.motion.JarnPageTransition
import jr.brian.core.ui.motion.PageLayer
import jr.brian.core.ui.motion.PageTransitionMath
import jr.brian.core.ui.motion.arrivingLayer
import jr.brian.core.ui.theme.JarnMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Golden values from Classic's pager math before it moved here, so Classic can't drift. */
class PageTransitionMathTest {
    private val eps = 1e-4f

    private fun assertLayer(expected: PageLayer, actual: PageLayer?) {
        requireNotNull(actual)
        assertEquals(expected.translationX, actual.translationX, eps)
        assertEquals(expected.translationY, actual.translationY, eps)
        assertEquals(expected.scaleX, actual.scaleX, eps)
        assertEquals(expected.scaleY, actual.scaleY, eps)
        assertEquals(expected.alpha, actual.alpha, eps)
    }

    @Test
    fun `slide matches Classic`() {
        assertLayer(PageLayer(translationX = -50f, alpha = 0.8f), PageTransitionMath.slide(-0.5f, 100f))
        assertLayer(PageLayer(translationX = 25f, alpha = 0.9f), PageTransitionMath.slide(0.25f, 100f))
    }

    @Test
    fun `fade matches Classic`() {
        assertLayer(PageLayer(alpha = 0.5f), PageTransitionMath.fade(0.5f))
        assertLayer(PageLayer(alpha = 0f), PageTransitionMath.fade(-1.5f))
    }

    @Test
    fun `depth matches Classic`() {
        assertLayer(PageLayer(scaleX = 0.875f, scaleY = 0.875f, alpha = 0.5f), PageTransitionMath.depth(-0.5f, 100f))
        assertLayer(
            PageLayer(translationX = 50f, scaleX = 0.875f, scaleY = 0.875f, alpha = 0.5f),
            PageTransitionMath.depth(0.5f, 100f),
        )
    }

    @Test
    fun `stack matches Classic`() {
        assertLayer(PageLayer(translationX = -50f, alpha = 0.5f), PageTransitionMath.stack(-0.5f, 100f, 2f))
        assertLayer(
            PageLayer(translationY = 24f, scaleX = 0.925f, scaleY = 0.925f, alpha = 0.625f),
            PageTransitionMath.stack(0.5f, 100f, 2f),
        )
    }

    @Test
    fun `glide up matches Classic`() {
        assertLayer(PageLayer(translationY = -100f, alpha = 0.5f), PageTransitionMath.glideUp(-0.5f, 200f))
        assertLayer(PageLayer(translationY = 100f, alpha = 0.5f), PageTransitionMath.glideUp(0.5f, 200f))
    }

    @Test
    fun `carousel matches Classic`() {
        assertLayer(
            PageLayer(translationX = 37.5f, scaleX = 0.85f, scaleY = 0.85f, alpha = 0.7f),
            PageTransitionMath.carousel(0.5f, 100f),
        )
    }

    @Test
    fun `an arriving page starts in its own slot and settles in place`() {
        assertLayer(
            PageLayer(translationX = 100f, scaleX = 0.75f, scaleY = 0.75f, alpha = 0f),
            JarnPageTransition.Depth.arrivingLayer(-1f, 100f, 50f, 1f),
        )
        JarnPageTransition.entries.filter { it != JarnPageTransition.JarnSlide }.forEach { preset ->
            assertLayer(PageLayer(), preset.arrivingLayer(0f, 100f, 50f, 1f))
        }
    }

    @Test
    fun `jarn slide keeps its own shift`() {
        assertNull(JarnPageTransition.JarnSlide.arrivingLayer(-1f, 100f, 50f, 1f))
    }

    @Test
    fun `unknown ids fall back to the jarn slide`() {
        assertEquals(JarnPageTransition.JarnSlide, JarnPageTransition.fromId("cube"))
        assertEquals(JarnPageTransition.JarnSlide, JarnPageTransition.fromId(null))
        assertEquals(JarnPageTransition.GlideUp, JarnPageTransition.fromId("glide_up"))
    }

    @Test
    fun `only the gentle presets are offered`() {
        assertEquals(
            listOf("jarn_slide", "fade", "slide", "depth", "stack", "glide_up", "carousel"),
            JarnPageTransition.entries.map { it.id },
        )
    }

    @Test
    fun `tile press is firmer and off with reduce motion`() {
        val tile = JarnMotion(tilePress = true)
        assertEquals(0.96f, tile.pressScale, 0f)
        assertEquals(100, tile.press.durationMillis)
        assertEquals(0.97f, JarnMotion().pressScale, 0f)
        assertEquals(80, JarnMotion().press.durationMillis)
        val reduced = JarnMotion(reduced = true, tilePress = true)
        assertEquals(1f, reduced.pressScale, 0f)
        assertEquals(0, reduced.press.durationMillis)
    }
}
