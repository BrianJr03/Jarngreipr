package jr.brian.jarnfe.settings

import jr.brian.frontend.library.migrate.EsdeImportProgress
import jr.brian.frontend.library.migrate.EsdeImportSummary
import jr.brian.frontend.library.setup.EsdeDetection
import jr.brian.jarnfe.input.JarnScreen
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JarnEsdeImporterTest {
    private val scope = TestScope(StandardTestDispatcher())
    private val backend = FakeBackend()
    private val finished = mutableListOf<Pair<EsdeImportSummary, Boolean>>()
    private val importer = JarnEsdeImporter(backend, scope) { summary, seen -> finished += summary to seen }

    @Test
    fun `with access a request imports straight away`() {
        importer.request(JarnScreen.Deck)
        assertEquals(JarnEsdeImportState.Running(), importer.state)

        backend.finish(SUMMARY)
        scope.advanceUntilIdle()

        assertEquals(JarnEsdeImportState.Done(SUMMARY), importer.state)
        assertNull(importer.consentOn)
        assertEquals(1, backend.imports)
    }

    @Test
    fun `progress shows while it runs`() {
        importer.request(JarnScreen.Deck)
        scope.runCurrent()

        backend.progress.value = EsdeImportProgress(2, 5, "Game Boy Advance")
        scope.runCurrent()

        assertEquals(JarnEsdeImportState.Running(EsdeImportProgress(2, 5, "Game Boy Advance")), importer.state)
    }

    @Test
    fun `without access it asks on the screen that asked and imports nothing`() {
        backend.access = false

        importer.request(JarnScreen.Stage)

        assertEquals(JarnScreen.Stage, importer.consentOn)
        assertEquals(JarnEsdeImportState.Idle, importer.state)
        assertEquals(0, backend.imports)
    }

    @Test
    fun `cancel closes the dialog and imports nothing`() {
        backend.access = false
        importer.request(JarnScreen.Deck)

        importer.cancelConsent()

        assertNull(importer.consentOn)
        assertEquals(0, backend.imports)
    }

    @Test
    fun `access granted on return imports`() {
        backend.access = false
        importer.request(JarnScreen.Deck)
        importer.confirmConsent { true }
        assertNull(importer.consentOn)

        backend.access = true
        importer.onResumed()

        assertTrue(importer.isRunning)
    }

    @Test
    fun `no access on return says so and imports nothing`() {
        backend.access = false
        importer.request(JarnScreen.Deck)
        importer.confirmConsent { true }

        importer.onResumed()

        assertEquals(JarnEsdeImportState.NeedsAccess, importer.state)
        assertEquals(0, backend.imports)
    }

    @Test
    fun `a resume without a pending request does nothing`() {
        importer.onResumed()

        assertEquals(JarnEsdeImportState.Idle, importer.state)
        assertEquals(0, importer.accessReturns)
    }

    @Test
    fun `each return from the access screen is counted for refocus`() {
        backend.access = false
        importer.request(JarnScreen.Deck)
        importer.confirmConsent { true }

        importer.onResumed()
        importer.onResumed()

        assertEquals(1, importer.accessReturns)
    }

    @Test
    fun `a second request while running is ignored`() {
        importer.request(JarnScreen.Deck)
        scope.runCurrent()

        importer.request(JarnScreen.Deck)
        scope.runCurrent()

        assertEquals(1, backend.imports)
    }

    @Test
    fun `a failing import says so`() {
        importer.request(JarnScreen.Deck)
        backend.fail()
        scope.advanceUntilIdle()

        assertEquals(JarnEsdeImportState.Failed, importer.state)
        assertTrue(finished.isEmpty())
    }

    @Test
    fun `access lost mid way ends in needs access, not done`() {
        importer.request(JarnScreen.Deck)
        backend.finish(EsdeImportSummary(accessMissing = true))
        scope.advanceUntilIdle()

        assertEquals(JarnEsdeImportState.NeedsAccess, importer.state)
        assertTrue(finished.isEmpty())
    }

    @Test
    fun `finishing unwatched asks for a notice`() {
        importer.request(JarnScreen.Deck)
        backend.finish(SUMMARY)
        scope.advanceUntilIdle()

        assertEquals(listOf(SUMMARY to false), finished)
    }

    @Test
    fun `finishing on a watching screen needs no notice unless it wants one`() {
        val end = importer.watch()
        importer.request(JarnScreen.Deck)
        backend.finish(SUMMARY)
        scope.advanceUntilIdle()
        assertEquals(listOf(SUMMARY to true), finished)
        end()

        importer.watch(wantsNotice = true)
        importer.dismiss()
        backend.reset()
        importer.request(JarnScreen.Deck)
        backend.finish(SUMMARY)
        scope.advanceUntilIdle()
        assertEquals(SUMMARY to false, finished.last())
    }

    @Test
    fun `dismiss clears a finished state but never a running one`() {
        importer.request(JarnScreen.Deck)
        importer.dismiss()
        assertTrue(importer.isRunning)

        backend.finish(SUMMARY)
        scope.advanceUntilIdle()
        importer.dismiss()
        assertEquals(JarnEsdeImportState.Idle, importer.state)
    }

    @Test
    fun `detection is kept for the screens`() {
        scope.launch { importer.refreshDetection() }
        scope.advanceUntilIdle()

        assertEquals(DETECTION, importer.detection)
    }

    private class FakeBackend : JarnEsdeImportBackend {
        var access = true
        var imports = 0
        private var result = CompletableDeferred<EsdeImportSummary>()
        override val progress = MutableStateFlow<EsdeImportProgress?>(null)
        override val lastImport = MutableStateFlow<EsdeImportSummary?>(null)
        override fun hasFileAccess() = access
        override suspend fun detect() = DETECTION
        override suspend fun import(): EsdeImportSummary {
            imports++
            return result.await()
        }

        fun finish(summary: EsdeImportSummary) = result.complete(summary)
        fun fail() = result.completeExceptionally(IllegalStateException("broken"))
        fun reset() {
            result = CompletableDeferred()
        }
    }

    private companion object {
        val SUMMARY = EsdeImportSummary(gamelistsFound = 3, systemsMatched = 3, gamesUpdated = 40, favoritesAdded = 6)
        val DETECTION = EsdeDetection(found = true, hasFileAccess = true, systems = 3, games = 120, favorites = 8)
    }
}
