package jr.brian.home.esde.launch

import jr.brian.jarnfe.launch.JarnEmulatorChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemEmulatorPlanTest {
    private val keys = listOf("snes", "sfc")
    private val saved = GameEmulatorMaps(
        emulators = mapOf("snes//a.sfc" to "old.emu", "gba//b.gba" to "gba.emu"),
        commands = mapOf("sfc//c.sfc" to "old command"),
        cores = mapOf("snes//d.sfc" to "old.so"),
    )
    private val rows = listOf(SystemGameRow("snes//a.sfc", null), SystemGameRow("snes//e.sfc", null))

    @Test
    fun `counts each game with its own choice across aliases`() {
        assertEquals(3, countSystemConflicts(saved, keys, JarnEmulatorChoice("new.emu")))
    }

    @Test
    fun `replaces only this system's game choices`() {
        val plan = planSystemEmulator(saved, keys, JarnEmulatorChoice("new.emu"), rows)
        assertEquals(mapOf("gba//b.gba" to "gba.emu"), plan.emulators)
        assertTrue(plan.commands.isEmpty())
        assertTrue(plan.cores.isEmpty())
    }

    @Test
    fun `core reaches every game and drops the command that would ignore it`() {
        val choice = JarnEmulatorChoice("com.retroarch.aarch64", "ra command", "snes9x.so")
        val plan = planSystemEmulator(saved, keys, choice, rows)
        assertEquals(mapOf("snes//a.sfc" to "snes9x.so", "snes//e.sfc" to "snes9x.so"), plan.cores)
        assertTrue(plan.commands.isEmpty())
    }

    @Test
    fun `command reaches every game when no core is involved`() {
        val plan = planSystemEmulator(saved, keys, JarnEmulatorChoice("new.emu", "cmd"), rows)
        assertEquals(mapOf("snes//a.sfc" to "cmd", "snes//e.sfc" to "cmd"), plan.commands)
    }

    @Test
    fun `game imported with RetroArch is pinned so it never asks for a core`() {
        val imported = listOf(SystemGameRow("snes//r.sfc", "com.retroarch"))
        val plan = planSystemEmulator(GameEmulatorMaps(), keys, JarnEmulatorChoice("new.emu"), imported)
        assertEquals(mapOf("snes//r.sfc" to "new.emu"), plan.emulators)
    }

    @Test
    fun `applying the same choice again finds nothing to replace`() {
        val choice = JarnEmulatorChoice("new.emu", "cmd")
        val plan = planSystemEmulator(saved, keys, choice, rows)
        assertEquals(0, countSystemConflicts(plan, keys, choice))
    }
}
