package jr.brian.frontend.library.art

import jr.brian.frontend.library.media.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class GameArtChoicesTest {
    private val game = "1:snes:Game.sfc"
    private val other = "1:gba:Other.gba"

    @Test
    fun `game beats system beats default`() {
        val none = GameArtChoices()
        assertEquals(ArtChoice.Default, none.effective(ArtScreen.Top, game, "snes"))
        val system = none.withSystem(ArtScreen.Top, "snes", ArtChoice.Fanart)
        assertEquals(ArtChoice.Fanart, system.effective(ArtScreen.Top, game, "snes"))
        val own = system.withGame(ArtScreen.Top, game, "snes", ArtChoice.Marquee)
        assertEquals(ArtChoice.Marquee, own.effective(ArtScreen.Top, game, "snes"))
    }

    @Test
    fun `default on a game overrides its system choice`() {
        val choices = GameArtChoices()
            .withSystem(ArtScreen.Top, "snes", ArtChoice.Fanart)
            .withGame(ArtScreen.Top, game, "snes", ArtChoice.Default)
        assertEquals(ArtChoice.Default, choices.effective(ArtScreen.Top, game, "snes"))
    }

    @Test
    fun `all games clears only that screen and system`() {
        val choices = GameArtChoices()
            .withGame(ArtScreen.Top, game, "snes", ArtChoice.Marquee)
            .withGame(ArtScreen.Bottom, game, "snes", ArtChoice.Cover)
            .withGame(ArtScreen.Top, other, "gba", ArtChoice.Cover)
        assertEquals(1, choices.gameOverrideCount(ArtScreen.Top, "snes"))
        val applied = choices.withSystem(ArtScreen.Top, "snes", ArtChoice.Fanart)
        assertEquals(ArtChoice.Fanart, applied.effective(ArtScreen.Top, game, "snes"))
        assertEquals(ArtChoice.Cover, applied.effective(ArtScreen.Bottom, game, "snes"))
        assertEquals(ArtChoice.Cover, applied.effective(ArtScreen.Top, other, "gba"))
    }

    @Test
    fun `defaults match what shipped and a missing kind falls through them`() {
        assertEquals(MediaKind.Cover, artChain(ArtScreen.Top, ArtChoice.Default).first())
        assertEquals(
            listOf(MediaKind.Marquee, MediaKind.Cover),
            artChain(ArtScreen.Bottom, ArtChoice.Default).take(2),
        )
        assertEquals(
            listOf(MediaKind.PhysicalMedia, MediaKind.Cover),
            artChain(ArtScreen.Top, ArtChoice.PhysicalMedia).take(2),
        )
    }

    @Test
    fun `video is never offered`() {
        assertFalse(ArtChoice.entries.any { it.kind == MediaKind.Video })
    }

    @Test
    fun `stored choices survive encoding and damaged data reads empty`() {
        val choices = GameArtChoices(
            topBySystem = mapOf("snes" to ArtChoice.Miximage),
            bottomByGame = mapOf(game to ArtChoice.Default),
        )
        assertEquals(choices, decodeArtChoices(encodeArtChoices(choices)))
        assertEquals(GameArtChoices(), decodeArtChoices("not json"))
    }
}
