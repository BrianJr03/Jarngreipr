package jr.brian.jarnfe.canvas

import jr.brian.core.model.GameImageType
import jr.brian.frontend.library.media.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasArtResolutionTest {
    private val marqueeA = mapOf(MediaKind.Marquee to "/a/marquee.png", MediaKind.PhysicalMedia to "/a/disc.png")

    private fun stageArt(key: String, lookup: CanvasArtLookup?, lookupRuns: Boolean = true) =
        canvasGameStageArt(key, "Game $key", "/$key/cover.png", lookup, lookupRuns)

    @Test
    fun `a pending lookup never resolves to fallback art`() {
        val pending = stageArt("b", lookup = null)
        assertTrue(pending.isPending)
        assertNull(pending.pathFor(GameImageType.Marquee))
        assertNull(pending.pathFor(GameImageType.Covers))
    }

    @Test
    fun `the previous game's media is never used for the new game`() {
        val art = stageArt("b", CanvasArtLookup("a", marqueeA))
        assertTrue(art.isPending)
        assertNull(art.pathFor(GameImageType.Marquee))
        assertTrue(art.media.isEmpty())
    }

    @Test
    fun `a resolved marquee shows the marquee`() {
        val art = stageArt("a", CanvasArtLookup("a", marqueeA))
        assertFalse(art.isPending)
        assertEquals("/a/marquee.png", art.pathFor(GameImageType.Marquee))
        assertEquals("/a/disc.png", art.pathFor(GameImageType.PhysicalMedia))
    }

    @Test
    fun `a confirmed missing marquee falls back to the game's own art`() {
        val art = stageArt("a", CanvasArtLookup("a", mapOf(MediaKind.PhysicalMedia to "/a/disc.png")))
        assertEquals("/a/cover.png", art.pathFor(GameImageType.Marquee))
    }

    @Test
    fun `without a library there is nothing to wait for`() {
        val art = stageArt("a", lookup = null, lookupRuns = false)
        assertFalse(art.isPending)
        assertEquals("/a/cover.png", art.pathFor(GameImageType.Marquee))
    }

    @Test
    fun `a tile keeps its image while the next game is pending, then swaps once`() {
        val first = canvasHeldArt(stageArt("a", CanvasArtLookup("a", marqueeA)), GameImageType.Marquee, held = null)
        assertEquals("/a/marquee.png", first?.path)
        val waiting = canvasHeldArt(stageArt("b", CanvasArtLookup("a", marqueeA)), GameImageType.Marquee, first)
        assertSame(first, waiting)
        val next = canvasHeldArt(
            stageArt("b", CanvasArtLookup("b", mapOf(MediaKind.Marquee to "/b/marquee.png"))),
            GameImageType.Marquee,
            waiting,
        )
        assertEquals("/b/marquee.png", next?.path)
        assertEquals("/a/marquee.png", next?.previousPath)
    }

    @Test
    fun `a first show that is still pending draws nothing`() {
        assertNull(canvasHeldArt(stageArt("a", lookup = null), GameImageType.Marquee, held = null))
    }

    @Test
    fun `changing the tile's media choice applies at once`() {
        val art = stageArt("a", CanvasArtLookup("a", marqueeA))
        val marquee = canvasHeldArt(art, GameImageType.Marquee, held = null)
        assertEquals("/a/disc.png", canvasHeldArt(art, GameImageType.PhysicalMedia, marquee)?.path)
    }
}
