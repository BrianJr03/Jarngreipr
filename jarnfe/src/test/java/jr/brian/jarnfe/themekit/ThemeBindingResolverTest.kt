package jr.brian.jarnfe.themekit

import jr.brian.core.model.MediaKind
import jr.brian.core.model.theme.GameTextField
import jr.brian.core.model.theme.SystemArtKind
import jr.brian.core.model.theme.SystemTextField
import jr.brian.core.model.theme.ThemeDateField
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemeText
import jr.brian.core.model.theme.ThemeVisibleWhen
import jr.brian.jarnfe.library.JarnGameMetadata
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.library.LibrarySelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.ZoneOffset

class ThemeBindingResolverTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val snes = JarnLibrarySystem(
        id = "snes",
        title = "Super Nintendo",
        gameCount = 12,
        artPath = "/art/snes.png",
        logoPath = "/art/snes-logo.png",
        favoriteCount = 0,
    )
    private val game = JarnLibraryGame(
        id = "g1",
        systemId = "snes",
        title = "Chrono Trigger",
        artPath = "/covers/ct.png",
        metadata = JarnGameMetadata(
            developer = "Square",
            publisher = "  ",
            releaseDate = "19950311T000000",
            rating = "0.9",
            playCount = 3,
            playtimeMinutes = 0,
        ),
    )
    private val focusedGame = FocusedItem.Game(game, snes)
    private val media = ThemeFocusMedia("g1", mapOf(MediaKind.Screenshot to "/shots/ct.png"))

    @Test
    fun `game text fields resolve and blank or zero values hide`() {
        assertEquals(ThemeTextValue.Plain("Chrono Trigger"), resolveThemeText(ThemeText.Game(GameTextField.Title), focusedGame))
        assertEquals(ThemeTextValue.Plain("Super Nintendo"), resolveThemeText(ThemeText.Game(GameTextField.SystemTitle), focusedGame))
        assertEquals(ThemeTextValue.PlayCount(3), resolveThemeText(ThemeText.Game(GameTextField.PlayCount), focusedGame))
        assertNull(resolveThemeText(ThemeText.Game(GameTextField.Publisher), focusedGame))
        assertNull(resolveThemeText(ThemeText.Game(GameTextField.Playtime), focusedGame))
        assertNull(resolveThemeText(ThemeText.Game(GameTextField.Description), focusedGame))
    }

    @Test
    fun `fields of the wrong kind hide`() {
        assertNull(resolveThemeText(ThemeText.Game(GameTextField.Title), FocusedItem.System(snes)))
        assertNull(resolveThemeText(ThemeText.System(SystemTextField.Title), focusedGame))
        assertNull(resolveThemeText(ThemeText.Game(GameTextField.Title), FocusedItem.None))
    }

    @Test
    fun `system fields resolve and an empty favorites count hides`() {
        val system = FocusedItem.System(snes)
        assertEquals(ThemeTextValue.GameCount(12), resolveThemeText(ThemeText.System(SystemTextField.GameCount), system))
        assertNull(resolveThemeText(ThemeText.System(SystemTextField.FavoriteCount), system))
    }

    @Test
    fun `static text shows with nothing focused unless blank`() {
        assertEquals(ThemeTextValue.Plain("Library"), resolveThemeText(ThemeText.Static("Library"), FocusedItem.None))
        assertNull(resolveThemeText(ThemeText.Static(" "), FocusedItem.None))
    }

    @Test
    fun `release dates read ES-DE and hand written years`() {
        val esde = releaseDateValue("19950311T000000", ZoneOffset.UTC)
        assertEquals(ThemeDateValue(1995, 794880000000L), esde)
        assertEquals(ThemeDateValue(1995), releaseDateValue("March 1995", ZoneOffset.UTC))
        assertNull(releaseDateValue("unknown"))
        assertNull(resolveThemeDate(ThemeDateField.LastPlayed, focusedGame))
        assertNull(resolveThemeDate(ThemeDateField.ReleaseDate, FocusedItem.System(snes)))
    }

    @Test
    fun `ratings normalise their scale and unrated hides`() {
        assertEquals(0.9f, resolveThemeRating(focusedGame)!!, 0.0001f)
        assertEquals(0.8f, resolveThemeRating(withRating("4"))!!, 0.0001f)
        assertEquals(0.7f, resolveThemeRating(withRating("7"))!!, 0.0001f)
        assertNull(resolveThemeRating(withRating("0")))
        assertNull(resolveThemeRating(withRating("great")))
        assertNull(resolveThemeRating(withRating("42")))
    }

    @Test
    fun `game media comes from the focused game only, with the cover from its tile art`() {
        val screenshot = ThemePicture.GameMedia(MediaKind.Screenshot)
        assertEquals("/shots/ct.png", resolveThemePicture(screenshot, focusedGame, media, null))
        assertEquals("/covers/ct.png", resolveThemePicture(ThemePicture.GameMedia(MediaKind.Cover), focusedGame, media, null))
        assertNull(resolveThemePicture(ThemePicture.GameMedia(MediaKind.Fanart), focusedGame, media, null))
        val otherGame = FocusedItem.Game(game.copy(id = "g2"), snes)
        assertNull(resolveThemePicture(screenshot, otherGame, media, null))
    }

    @Test
    fun `system art binds for a system and beside a game`() {
        val logo = ThemePicture.SystemArt(SystemArtKind.Logo)
        assertEquals("/art/snes-logo.png", resolveThemePicture(logo, FocusedItem.System(snes), media, null))
        assertEquals("/art/snes-logo.png", resolveThemePicture(logo, focusedGame, media, null))
        assertEquals("/art/snes.png", resolveThemePicture(ThemePicture.SystemArt(SystemArtKind.Background), focusedGame, media, null))
        assertNull(resolveThemePicture(logo, FocusedItem.None, media, null))
    }

    @Test
    fun `theme files stay inside the theme folder`() {
        val dir = temp.newFolder("theme")
        val inside = resolveThemePicture(ThemePicture.File("art/bg.png"), FocusedItem.None, media, dir)
        assertEquals(dir.canonicalFile.resolve("art/bg.png").path, inside)
        assertNull(resolveThemePicture(ThemePicture.File("../secret.png"), FocusedItem.None, media, dir))
        assertNull(resolveThemePicture(ThemePicture.File("bg.png"), FocusedItem.None, media, null))
    }

    @Test
    fun `visibleWhen checks media, text and systems together`() {
        assertTrue(themeLayerVisible(null, FocusedItem.None, ThemeFocusMedia()))
        assertTrue(themeLayerVisible(ThemeVisibleWhen(hasMedia = MediaKind.Screenshot), focusedGame, media))
        assertFalse(themeLayerVisible(ThemeVisibleWhen(hasMedia = MediaKind.Video), focusedGame, media))
        assertFalse(themeLayerVisible(ThemeVisibleWhen(hasText = GameTextField.Genre), focusedGame, media))
        assertTrue(themeLayerVisible(ThemeVisibleWhen(systems = listOf("snes")), FocusedItem.Game(game), media))
        assertFalse(themeLayerVisible(ThemeVisibleWhen(systems = listOf("nes")), focusedGame, media))
    }

    @Test
    fun `browse selections map to focused items`() {
        val systems = mapOf("snes" to snes)
        assertEquals(focusedGame, LibrarySelection.Game(game).toFocusedItem(systems::get))
        assertEquals(FocusedItem.System(snes), LibrarySelection.System(snes).toFocusedItem())
        assertEquals(FocusedItem.None, LibrarySelection.None.toFocusedItem())
    }

    private fun withRating(rating: String) =
        FocusedItem.Game(game.copy(metadata = game.metadata.copy(rating = rating)), snes)
}
