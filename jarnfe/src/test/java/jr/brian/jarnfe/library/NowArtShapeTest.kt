package jr.brian.jarnfe.library

import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NowArtShapeTest {
    @Test
    fun coverUsesItsOwnShape() = assertEquals(1f, nowCoverAspect(IntSize(500, 500)), 0.001f)

    @Test
    fun unknownCoverUsesTheDefault() {
        assertEquals(0.8f, nowCoverAspect(null), 0.001f)
        assertEquals(0.8f, nowCoverAspect(IntSize(-1, -1)), 0.001f)
    }

    @Test
    fun extremeCoverIsClamped() {
        assertEquals(1.6f, nowCoverAspect(IntSize(1000, 100)), 0.001f)
        assertEquals(0.6f, nowCoverAspect(IntSize(100, 1000)), 0.001f)
    }

    @Test
    fun wideMarqueeFits() = assertTrue(nowMarqueeFits(IntSize(800, 300)))

    @Test
    fun squareMarqueeDoesNotFit() = assertFalse(nowMarqueeFits(IntSize(512, 512)))

    @Test
    fun unreadableMarqueeStillTries() = assertTrue(nowMarqueeFits(IntSize(-1, -1)))
}
