package jr.brian.core.model.bus

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the Phase 1 bus adapter — read-after-write, independent streams,
 * and the presence flags.
 *
 * The adapter is intentionally thin (just the StateFlow / SharedFlow
 * plumbing), so these tests pin the contract rather than the behaviour.
 * As :app wires producers onto the bus in later phases, these tests
 * stay valid because they target the interface.
 */
class FrontendBusAdapterTest {

    @Test
    fun `focus state round-trips through publishFocus`() = runBlocking {
        val bus: FrontendBus = FrontendBusAdapter()
        assertNull(bus.focus.value)

        val focus = FrontendFocus.Game(
            id = "n64:zelda",
            title = "Ocarina of Time",
            systemId = "n64",
            art = ResolvedArt(cover = "/media/n64/covers/zelda.png"),
        )
        bus.publishFocus(focus)

        assertEquals(focus, bus.focus.value)
    }

    @Test
    fun `events replay the latest emission to a late subscriber`() = runBlocking {
        val bus: FrontendBus = FrontendBusAdapter()
        bus.publishEvent(FrontendEvent.GameLaunched("n64:zelda"))

        // Subscribing after the emission — the adapter's replay cache
        // should still deliver the event. This is the "frontend went
        // through finish() while a game was launching" case: a late
        // consumer still has to see the lifecycle marker.
        val replayed = bus.events.first()
        assertEquals(FrontendEvent.GameLaunched("n64:zelda"), replayed)
    }

    @Test
    fun `commands replay the latest emission to a late subscriber`() = runBlocking {
        val bus: FrontendBus = FrontendBusAdapter()
        bus.sendCommand(FrontendCommand.Search("mario"))

        val replayed = bus.commands.first()
        assertEquals(FrontendCommand.Search("mario"), replayed)
    }

    @Test
    fun `presence flags are independently writable and start false`() = runBlocking {
        val bus: FrontendBus = FrontendBusAdapter()
        assertFalse(bus.launcherPresent.value)
        assertFalse(bus.frontendPresent.value)

        bus.setLauncherPresent(true)
        assertTrue(bus.launcherPresent.value)
        assertFalse(bus.frontendPresent.value)

        bus.setFrontendPresent(true)
        assertTrue(bus.frontendPresent.value)
    }

    @Test
    fun `focus and command streams are independent`() = runBlocking {
        val bus: FrontendBus = FrontendBusAdapter()
        bus.publishFocus(
            FrontendFocus.System(id = "n64", name = "Nintendo 64", art = ResolvedArt.EMPTY)
        )
        bus.sendCommand(FrontendCommand.SwapScreens)

        // The focus flow carries only the focus; the command flow carries
        // only the command. No cross-contamination.
        assertTrue(bus.focus.value is FrontendFocus.System)
        assertEquals(FrontendCommand.SwapScreens, bus.commands.first())
    }
}
