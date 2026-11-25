package jr.brian.frontend.library.migrate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EsdeImportSummaryTest {

    @Test
    fun `emulator counts come from the emulator import`() {
        val result = EsdeAltEmulatorResult(
            systemsSet = 2,
            gamesSet = 5,
            keptExisting = 3,
            gamesNotInLibrary = 9,
            skipped = listOf(
                EsdeAltEmulatorSkip("gba", "mGBA (Standalone)", EsdeAltEmulatorSkipReason.NotInstalled),
                EsdeAltEmulatorSkip("psx", "Mystery", EsdeAltEmulatorSkipReason.UnknownLabel),
            ),
        )

        val summary = EsdeImportSummary(gamesUpdated = 40, favoritesAdded = 6).withEmulators(result)

        assertEquals(2, summary.emulatorSystemsSet)
        assertEquals(5, summary.emulatorGamesSet)
        assertEquals(3, summary.emulatorKeptExisting)
        assertEquals(2, summary.emulatorSkipped)
        assertEquals(40, summary.gamesUpdated)
        assertEquals(6, summary.favoritesAdded)
    }

    @Test
    fun `no emulator import leaves the counts at zero`() {
        val summary = EsdeImportSummary(gamesUpdated = 1)

        assertEquals(0, summary.emulatorSystemsSet + summary.emulatorGamesSet + summary.emulatorSkipped)
        assertEquals(summary, summary.withEmulators(EsdeAltEmulatorResult()))
    }

    @Test
    fun `the gamelist line has no emulator part`() {
        val line = EsdeImportSummary(gamelistsFound = 3).withEmulators(EsdeAltEmulatorResult(gamesSet = 4)).logLine()

        assertTrue(line.startsWith("Gamelist import: 3 gamelists, 0 skipped as unchanged"))
        assertFalse(line.contains("emulator"))
    }

    @Test
    fun `the emulator line has the emulator counts`() {
        val result = EsdeAltEmulatorResult(
            systemsSet = 1,
            gamesSet = 4,
            keptExisting = 2,
            skipped = listOf(EsdeAltEmulatorSkip("psx", "Mystery", EsdeAltEmulatorSkipReason.UnknownLabel)),
        )

        assertEquals(
            "ES-DE emulator import: 1 systems, 4 games, 2 kept, 1 skipped",
            EsdeImportSummary().withEmulators(result).emulatorLogLine(),
        )
    }
}
