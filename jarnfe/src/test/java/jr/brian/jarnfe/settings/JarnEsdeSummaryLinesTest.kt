package jr.brian.jarnfe.settings

import jr.brian.frontend.library.migrate.EsdeImportSummary
import jr.brian.jarnfe.R
import org.junit.Assert.assertEquals
import org.junit.Test

class JarnEsdeSummaryLinesTest {

    @Test
    fun `no emulator lines when nothing was brought over`() {
        assertEquals(emptyList<Pair<Int, Int>>(), esdeEmulatorLines(EsdeImportSummary(gamesUpdated = 4)))
    }

    @Test
    fun `each emulator count above zero gets its line`() {
        val lines = esdeEmulatorLines(
            EsdeImportSummary(emulatorSystemsSet = 1, emulatorGamesSet = 7, emulatorKeptExisting = 2, emulatorSkipped = 3),
        )

        assertEquals(
            listOf(
                R.plurals.jarnfe_esde_import_emulator_systems to 1,
                R.plurals.jarnfe_esde_import_emulator_games to 7,
                R.plurals.jarnfe_esde_import_emulator_skipped to 3,
            ),
            lines,
        )
    }

    @Test
    fun `skipped shows only when above zero`() {
        val lines = esdeEmulatorLines(EsdeImportSummary(emulatorGamesSet = 2))

        assertEquals(listOf(R.plurals.jarnfe_esde_import_emulator_games to 2), lines)
    }
}
