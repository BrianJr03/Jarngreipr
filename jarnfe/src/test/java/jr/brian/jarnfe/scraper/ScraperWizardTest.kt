package jr.brian.jarnfe.scraper

import jr.brian.frontend.scrape.ScraperConnectionResult
import jr.brian.frontend.scrape.ScraperMediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScraperWizardTest {
    private val fresh = JarnScraperStatus(
        screenScraperAvailable = true,
        screenScraperSaved = false,
        screenScraperEnabled = false,
        screenScraperKinds = ScreenScraperArtKinds.toSet(),
        steamGridDbSaved = false,
        steamGridDbEnabled = false,
        steamGridDbKinds = SteamGridDbArtKinds.toSet(),
    )

    @Test
    fun `account step needs every field for the chosen source`() {
        val account = ScraperWizard.from(fresh).next()
        assertEquals(ScraperStep.Account, account.step)
        assertFalse(account.canContinue)
        val filled = account.edit(ScraperField.UserId, "user").edit(ScraperField.Password, "pass")
        assertTrue(filled.canContinue)
    }

    @Test
    fun `saved account can continue without retyping and blank keeps it`() {
        val saved = fresh.copy(screenScraperSaved = true, screenScraperEnabled = true)
        val account = ScraperWizard.from(saved).next().edit(ScraperField.Password, "   ")
        assertTrue(account.canContinue)
        assertNull(account.draft.password)
    }

    @Test
    fun `editing a credential clears only that source's test result`() {
        val wizard = ScraperWizard.from(fresh).toggleSource(JarnScraperSource.SteamGridDb).copy(
            results = mapOf(
                JarnScraperSource.ScreenScraper to ScraperConnectionResult.Ok,
                JarnScraperSource.SteamGridDb to ScraperConnectionResult.Ok,
            ),
        )
        val edited = wizard.edit(ScraperField.ApiKey, "new")
        assertEquals(setOf(JarnScraperSource.ScreenScraper), edited.results.keys)
    }

    @Test
    fun `test step blocks continue until passing but can be skipped`() {
        val test = ScraperWizard.from(fresh).copy(step = ScraperStep.Test)
        assertEquals(ScraperStep.Test, test.next().step)
        assertEquals(ScraperStep.Art, test.skipTest().step)
        val passed = test.copy(results = mapOf(JarnScraperSource.ScreenScraper to ScraperConnectionResult.Ok))
        assertEquals(ScraperStep.Art, passed.next().step)
    }

    @Test
    fun `each chosen source needs an art type`() {
        var art = ScraperWizard.from(fresh).copy(step = ScraperStep.Art)
        ScreenScraperArtKinds.forEach { art = art.toggleKind(JarnScraperSource.ScreenScraper, it) }
        assertFalse(art.canContinue)
        assertTrue(art.toggleKind(JarnScraperSource.ScreenScraper, ScraperMediaKind.Covers).canContinue)
    }

    @Test
    fun `only real user changes count as unsaved`() {
        val wizard = ScraperWizard.from(fresh)
        assertFalse(wizard.isDirty)
        assertTrue(wizard.toggleSource(JarnScraperSource.SteamGridDb).isDirty)
        assertTrue(wizard.edit(ScraperField.UserId, "user").isDirty)
    }

    @Test
    fun `unavailable screenscraper cannot be chosen`() {
        val status = fresh.copy(screenScraperAvailable = false)
        val wizard = ScraperWizard.from(status)
        assertFalse(wizard.useScreenScraper)
        assertTrue(wizard.useSteamGridDb)
        assertFalse(wizard.toggleSource(JarnScraperSource.ScreenScraper).useScreenScraper)
    }

    @Test
    fun `secrets never appear in text forms`() {
        val secret = "hunter2-secret"
        val wizard = ScraperWizard.from(fresh)
            .edit(ScraperField.Password, secret)
            .edit(ScraperField.ApiKey, secret)
        assertFalse(wizard.toString().contains(secret))
        assertFalse(wizard.draft.toString().contains(secret))
        assertFalse(wizard.toPlan().toString().contains(secret))
        assertFalse(JarnKeyboardState(secret).toString().contains(secret))
    }

    @Test
    fun `keyboard reaches every printable character`() {
        val reachable = JarnKeyboardPage.entries
            .flatMap(::jarnKeyboardRows)
            .flatten()
            .map { it.char }
            .toSet() + ' '
        (32..126).map(Int::toChar).forEach { assertTrue("Missing '$it'", it in reachable) }
    }

    @Test
    fun `keyboard edits text and caps its length`() {
        val typed = JarnKeyboardState()
            .press(JarnKeyboardKey.Letter('a'))
            .press(JarnKeyboardKey.Shift)
            .press(JarnKeyboardKey.Letter('B'))
            .press(JarnKeyboardKey.Space)
            .press(JarnKeyboardKey.Delete)
        assertEquals("aB", typed.text)
        assertEquals(JarnKeyboardPage.Upper, typed.page)
        val full = JarnKeyboardState("x".repeat(JarnKeyboardState.MAX_LENGTH))
        assertEquals(full.text, full.press(JarnKeyboardKey.Letter('y')).text)
    }
}
