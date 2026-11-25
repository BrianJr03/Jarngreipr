package jr.brian.jarnfe.look.commandpanel

import android.app.Application
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

private val GOOD = "a real font".toByteArray()
private val OTHER = "another font".toByteArray()
private val BROKEN = "BAD not a font".toByteArray()

private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

private val FakeCheck = FontFileCheck { !it.readText().startsWith("BAD") }
private val FakeLoader = FontFamilyLoader { FontFamily.Monospace }

private class FakeFontsPort(var next: CommandPanelFontsRead) : CommandPanelFontsPort {
    val files = mutableMapOf<String, ByteArray>()
    var opens = 0
    var isWatching = false

    override fun read() = next

    override fun openFont(id: String): InputStream? {
        opens += 1
        return files[id]?.let(::ByteArrayInputStream)
    }

    override fun setObserving(observing: Boolean) {
        isWatching = observing
    }

    override fun listen(onChange: () -> Unit) = Unit

    override fun unlisten() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CommandPanelFontCacheTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun cache() = CommandPanelFontCache(temp.root, FakeCheck, FakeLoader)
    private fun copies(): List<String> = File(temp.root, "cp_fonts").list().orEmpty().sorted()

    @Test
    fun `a miss copies the file once under its digest`() {
        var opens = 0
        val cache = cache()
        val open = { opens += 1; ByteArrayInputStream(GOOD) }
        assertNotNull(cache.family(sha(GOOD), open))
        assertNotNull(cache.family(sha(GOOD), open))
        assertEquals(1, opens)
        assertEquals(listOf(sha(GOOD) + ".ttf"), copies())
    }

    @Test
    fun `a verified copy on disk is a hit without Command Panel`() {
        cache().family(sha(GOOD)) { ByteArrayInputStream(GOOD) }
        assertNotNull(cache().family(sha(GOOD), open = null))
    }

    @Test
    fun `nothing cached and no fetch is a miss`() {
        assertNull(cache().family(sha(GOOD), open = null))
        assertNull(cache().family(sha(GOOD)) { null })
    }

    @Test
    fun `a stale copy whose bytes changed is replaced`() {
        cache().family(sha(GOOD)) { ByteArrayInputStream(GOOD) }
        File(temp.root, "cp_fonts/${sha(GOOD)}.ttf").writeBytes(OTHER)
        assertNull(cache().family(sha(GOOD), open = null))
        assertTrue(copies().isEmpty())
        assertNotNull(cache().family(sha(GOOD)) { ByteArrayInputStream(GOOD) })
    }

    @Test
    fun `a corrupt or mismatched file is never kept`() {
        assertNull(cache().family(sha(BROKEN)) { ByteArrayInputStream(BROKEN) })
        assertNull(cache().family(sha(GOOD)) { ByteArrayInputStream(OTHER) })
        assertNull(cache().family(sha(GOOD)) { error("provider blew up") })
        assertTrue(copies().isEmpty())
    }

    @Test
    fun `retain drops copies nothing uses`() {
        val cache = cache()
        cache.family(sha(GOOD)) { ByteArrayInputStream(GOOD) }
        cache.family(sha(OTHER)) { ByteArrayInputStream(OTHER) }
        File(temp.root, "cp_fonts/leftover.part").writeText("x")
        cache.retain(setOf(sha(OTHER)))
        assertEquals(listOf(sha(OTHER) + ".ttf"), copies())
        cache.clear()
        assertTrue(copies().isEmpty())
    }

    private fun importedRow(bytes: ByteArray, id: String = "aa-1") = CommandPanelFonts(
        "custom:mine", CommandPanelFont.Bundled("mono"), CommandPanelFont.Imported(id, sha(bytes)), "Mine",
    )

    @Test
    fun `the controller copies, applies and caches an imported font`() = runTest {
        val port = FakeFontsPort(CommandPanelFontsRead.Fresh(importedRow(GOOD))).apply { files["aa-1"] = GOOD }
        val rows = CommandPanelFontsRowCache(temp.root)
        val controller = CommandPanelFontsController(port, rows, cache(), StandardTestDispatcher(testScheduler))
        controller.start()
        advanceUntilIdle()
        assertEquals(FontFamily.Monospace, controller.match.value?.type?.label?.fontFamily)
        assertEquals(importedRow(GOOD), rows.read())
        assertTrue(port.isWatching)
        controller.stop()
        advanceUntilIdle()

        val relaunched = CommandPanelFontsController(port, rows, cache(), StandardTestDispatcher(testScheduler))
        relaunched.seedFromCache()
        assertEquals(FontFamily.Monospace, relaunched.match.value?.type?.label?.fontFamily)
        assertEquals(1, port.opens)
    }

    @Test
    fun `switching to a look without the font deletes its copy`() = runTest {
        val port = FakeFontsPort(CommandPanelFontsRead.Fresh(importedRow(GOOD))).apply { files["aa-1"] = GOOD }
        val controller = CommandPanelFontsController(
            port, CommandPanelFontsRowCache(temp.root), cache(), StandardTestDispatcher(testScheduler),
        )
        controller.start()
        advanceUntilIdle()
        port.next = CommandPanelFontsRead.Fresh(CommandPanelFonts("TERMINAL", CommandPanelFont.Bundled("mono"), null))
        controller.refresh()
        advanceUntilIdle()
        assertTrue(copies().isEmpty())
        controller.stop()
        advanceUntilIdle()
    }

    @Test
    fun `an untrusted or removed Command Panel wipes every copy`() = runTest {
        val port = FakeFontsPort(CommandPanelFontsRead.Fresh(importedRow(GOOD))).apply { files["aa-1"] = GOOD }
        val rows = CommandPanelFontsRowCache(temp.root)
        val controller = CommandPanelFontsController(port, rows, cache(), StandardTestDispatcher(testScheduler))
        controller.start()
        advanceUntilIdle()
        port.next = CommandPanelFontsRead.Untrusted
        controller.refresh()
        advanceUntilIdle()
        assertNull(controller.match.value)
        assertNull(rows.read())
        assertTrue(copies().isEmpty())
        assertFalse(port.isWatching)
        controller.stop()
        advanceUntilIdle()
    }

    @Test
    fun `turning matching off forgets the copies`() = runTest {
        val port = FakeFontsPort(CommandPanelFontsRead.Fresh(importedRow(GOOD))).apply { files["aa-1"] = GOOD }
        val rows = CommandPanelFontsRowCache(temp.root)
        val controller = CommandPanelFontsController(port, rows, cache(), StandardTestDispatcher(testScheduler))
        controller.start()
        advanceUntilIdle()
        controller.stop()
        controller.forget()
        advanceUntilIdle()
        assertNull(rows.read())
        assertTrue(copies().isEmpty())
    }

    @Test
    fun `the offer samples only verified copies`() {
        val fresh = CommandPanelFontsRead.Fresh(importedRow(GOOD))
        val empty = commandPanelFontOffer(fresh, true, cache())
        assertEquals(FontMatchAvailability.Available("Mine"), empty.availability)
        assertTrue(empty.sample?.label?.fontFamily != FontFamily.Monospace)
        cache().family(sha(GOOD)) { ByteArrayInputStream(GOOD) }
        val cached = commandPanelFontOffer(fresh, true, cache())
        assertEquals(FontFamily.Monospace, cached.sample?.label?.fontFamily)
        assertNull(commandPanelFontOffer(CommandPanelFontsRead.Rejected, true, cache()).sample)
    }
}
