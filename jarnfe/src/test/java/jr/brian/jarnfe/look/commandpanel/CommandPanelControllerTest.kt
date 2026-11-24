package jr.brian.jarnfe.look.commandpanel

import jr.brian.core.model.CommandPanelPalette
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val SAMPLE = requireNotNull(commandPanelPaletteOf(1, 1, List(14) { (0xFF102030.toInt() + it).toLong() }))
private val OTHER = SAMPLE.copy(accent = 0xFF00FF00.toInt())

private class FakePort(var next: CommandPanelRead = CommandPanelRead.Fresh(SAMPLE)) : CommandPanelThemePort {
    var reads = 0
    var isWatching = false
    var listener: (() -> Unit)? = null
    var throwOnRead = false

    override fun read(): CommandPanelRead {
        reads += 1
        if (throwOnRead) error("provider blew up")
        return next
    }

    override fun setObserving(observing: Boolean) {
        isWatching = observing
    }

    override fun listen(onChange: () -> Unit) {
        listener = onChange
    }

    override fun unlisten() {
        listener = null
    }
}

private class FakeStore(var value: CommandPanelPalette? = null) : CommandPanelPaletteStore {
    var writes = 0

    override fun read() = value

    override fun write(palette: CommandPanelPalette) {
        writes += 1
        value = palette
    }

    override fun clear() {
        value = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CommandPanelControllerTest {
    private fun TestScope.controller(port: FakePort, store: FakeStore = FakeStore()) =
        CommandPanelThemeController(port, store, StandardTestDispatcher(testScheduler))

    private fun TestScope.finish(controller: CommandPanelThemeController) {
        controller.stop()
        advanceUntilIdle()
    }

    @Test
    fun `nothing happens until started`() = runTest {
        val port = FakePort()
        controller(port).refresh()
        advanceUntilIdle()
        assertEquals(0, port.reads)
        assertNull(port.listener)
        assertFalse(port.isWatching)
    }

    @Test
    fun `fresh read publishes, caches and observes`() = runTest {
        val port = FakePort()
        val store = FakeStore()
        val controller = controller(port, store)
        controller.start()
        advanceUntilIdle()
        assertEquals(SAMPLE, controller.palette.value)
        assertEquals(SAMPLE, store.value)
        assertTrue(port.isWatching)
        finish(controller)
    }

    @Test
    fun `identical palette is written once`() = runTest {
        val store = FakeStore()
        val controller = controller(FakePort(), store)
        controller.start()
        advanceUntilIdle()
        controller.refresh()
        advanceUntilIdle()
        assertEquals(1, store.writes)
        finish(controller)
    }

    @Test
    fun `cache seeds the first value`() = runTest {
        val controller = controller(FakePort(), FakeStore(OTHER))
        controller.seedFromCache()
        assertEquals(OTHER, controller.palette.value)
    }

    @Test
    fun `change callback re-reads`() = runTest {
        val port = FakePort()
        val controller = controller(port)
        controller.start()
        advanceUntilIdle()
        port.next = CommandPanelRead.Fresh(OTHER)
        requireNotNull(port.listener).invoke()
        advanceUntilIdle()
        assertEquals(OTHER, controller.palette.value)
        finish(controller)
    }

    @Test
    fun `untrusted provider clears palette, cache and observer`() = runTest {
        val port = FakePort()
        val store = FakeStore()
        val controller = controller(port, store)
        controller.start()
        advanceUntilIdle()
        port.next = CommandPanelRead.Untrusted
        controller.refresh()
        advanceUntilIdle()
        assertNull(controller.palette.value)
        assertNull(store.value)
        assertFalse(port.isWatching)
        finish(controller)
    }

    @Test
    fun `rejected row clears palette and cache`() = runTest {
        val port = FakePort()
        val store = FakeStore()
        val controller = controller(port, store)
        controller.start()
        advanceUntilIdle()
        port.next = CommandPanelRead.Rejected
        controller.refresh()
        advanceUntilIdle()
        assertNull(controller.palette.value)
        assertNull(store.value)
        finish(controller)
    }

    @Test
    fun `transient failure keeps the current colors and the cache`() = runTest {
        val port = FakePort()
        val store = FakeStore()
        val controller = controller(port, store)
        controller.start()
        advanceUntilIdle()
        port.throwOnRead = true
        controller.refresh()
        advanceUntilIdle()
        assertEquals(SAMPLE, controller.palette.value)
        assertEquals(SAMPLE, store.value)
        finish(controller)
    }

    @Test
    fun `failed read never starts observing`() = runTest {
        val port = FakePort(CommandPanelRead.Unavailable)
        val controller = controller(port)
        controller.start()
        advanceUntilIdle()
        assertFalse(port.isWatching)
        finish(controller)
    }

    @Test
    fun `stopped and locked keep the last palette`() = runTest {
        val port = FakePort()
        val store = FakeStore()
        val controller = controller(port, store)
        controller.start()
        advanceUntilIdle()
        listOf(CommandPanelRead.Stopped, CommandPanelRead.Locked).forEach {
            port.next = it
            controller.refresh()
            advanceUntilIdle()
            assertEquals(SAMPLE, controller.palette.value)
            assertNotNull(store.value)
        }
        finish(controller)
    }

    @Test
    fun `second holder keeps it running until both stop`() = runTest {
        val port = FakePort()
        val controller = controller(port)
        controller.start()
        controller.start()
        advanceUntilIdle()
        controller.stop()
        advanceUntilIdle()
        assertNotNull(port.listener)
        assertTrue(port.isWatching)
        controller.stop()
        advanceUntilIdle()
        assertNull(port.listener)
        assertFalse(port.isWatching)
    }

    @Test
    fun `no reads after stop`() = runTest {
        val port = FakePort()
        val controller = controller(port)
        controller.start()
        advanceUntilIdle()
        finish(controller)
        val reads = port.reads
        controller.refresh()
        advanceUntilIdle()
        assertEquals(reads, port.reads)
    }
}
