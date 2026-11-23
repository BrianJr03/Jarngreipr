package jr.brian.jarnfe.launch

import jr.brian.frontend.library.art.LibraryGame
import jr.brian.frontend.library.art.LibraryGameWithArt
import jr.brian.frontend.scrape.ScrapeState
import jr.brian.jarnfe.R
import jr.brian.frontend.library.setup.LibrarySetupState
import jr.brian.jarnfe.library.JarnLibraryFeed
import jr.brian.jarnfe.library.JarnLibraryStatus
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.library.librarySetupPrompt
import jr.brian.jarnfe.library.ratingText
import jr.brian.jarnfe.library.toFeedStatus
import jr.brian.jarnfe.library.toJarnGame
import jr.brian.jarnfe.settings.scrapeBlockedReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class JarnPlayMappingTest {
    private val game = LibraryGame(
        id = 7L,
        key = "1:gba:Sonic.gba",
        systemId = "gba",
        title = "Sonic",
        relativePath = "Sonic.gba",
        absolutePath = "/roms/gba/Sonic.gba",
        isFavorite = false,
        playCount = 0,
        lastPlayed = null,
        emulatorPackage = "emu",
        launchCommand = "cmd",
    )

    @Test
    fun `a launched game shows no message`() {
        assertNull(launchNoticeRes(JarnLaunchResult.Launched("emu")))
    }

    @Test
    fun `every other result has a message`() {
        val results = buildList<JarnLaunchResult> {
            JarnEmulatorChoiceKind.entries.forEach { add(JarnLaunchResult.NeedsEmulatorChoice(it)) }
            add(JarnLaunchResult.NeedsFileAccess("gba", null))
            add(JarnLaunchResult.MissingRom)
            JarnLaunchFailure.entries.forEach { add(JarnLaunchResult.Failed(it)) }
        }
        results.forEach { assertNotNull(it.toString(), launchNoticeRes(it)) }
    }

    @Test
    fun `launch ref uses the same path classic keys games by`() {
        val ref = game.toGameRef("/art.png")
        assertEquals("/roms/gba/Sonic.gba", ref.path)
        assertEquals("gba", ref.systemName)
        assertEquals("emu", ref.emulatorPackage)
        assertEquals("cmd", ref.launchCommand)
        assertEquals("Sonic.gba", game.copy(absolutePath = null).toGameRef(null).path)
    }

    @Test
    fun `library game id is the stable key`() {
        val mapped = LibraryGameWithArt(game, "/art.png").toJarnGame()
        assertEquals("1:gba:Sonic.gba", mapped.id)
        assertEquals("/art.png", mapped.artPath)
    }

    @Test
    fun `unrated and out of range ratings are not shown`() {
        assertNull(ratingText(null))
        assertNull(ratingText(0f))
        assertNull(ratingText(4f))
        assertNotNull(ratingText(0.8f))
    }

    @Test
    fun `setup prompts only appear on a settled empty library`() {
        val empty = JarnLibraryFeed.ready(emptyList<JarnLibrarySystem>())
        val reconnect = LibrarySetupState.NeedsReconnect("ROMs", "content://tree", 2)

        assertEquals(
            R.string.jarnfe_setup_add_folder,
            librarySetupPrompt(LibrarySetupState.NoSources, empty)?.actionRes,
        )
        assertEquals(R.string.jarnfe_setup_reconnect, librarySetupPrompt(reconnect, empty)?.actionRes)
        assertEquals("content://tree", librarySetupPrompt(reconnect, empty)?.pickerHint)
        assertNull(librarySetupPrompt(LibrarySetupState.Ready, empty))
        assertNull(librarySetupPrompt(reconnect, JarnLibraryFeed()))
        assertNull(
            librarySetupPrompt(
                reconnect,
                JarnLibraryFeed.ready(listOf(JarnLibrarySystem("a", "A", 1))),
            ),
        )
    }

    @Test
    fun `scan progress and failure reach the library feed`() {
        assertEquals(
            JarnLibraryStatus.Loading(1, 4),
            LibrarySetupState.Scanning(1, 4).toFeedStatus(),
        )
        assertEquals(JarnLibraryStatus.Loading(), LibrarySetupState.Scanning(0, 0).toFeedStatus())
        assertEquals(JarnLibraryStatus.Unavailable, LibrarySetupState.ScanFailed.toFeedStatus())
        assertEquals(JarnLibraryStatus.Ready, LibrarySetupState.NoSources.toFeedStatus())
    }

    @Test
    fun `scrape is blocked with a reason`() {
        assertEquals(
            R.string.jarnfe_art_scrape_no_credentials,
            scrapeBlockedReason(canScrape = false, ScrapeState.Idle),
        )
        assertEquals(
            R.string.jarnfe_art_scrape_running,
            scrapeBlockedReason(true, ScrapeState.Running(0, 1, 0, 0, null)),
        )
        assertNull(scrapeBlockedReason(canScrape = true, ScrapeState.Idle))
    }
}
