package jr.brian.jarnfe.themes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jr.brian.frontend.library.setup.EsdeSettings
import jr.brian.frontend.look.esde.EsdeImportOutcome
import jr.brian.frontend.look.esde.EsdeThemeSelection
import jr.brian.frontend.look.esde.selection
import jr.brian.frontend.look.esde.selectionFor
import jr.brian.frontend.look.themes.EsdeThemeFolder
import jr.brian.frontend.look.themes.EsdeThemeScan
import jr.brian.frontend.look.themes.EsdeThemeUpdate
import jr.brian.frontend.look.themes.ThemeImportProblem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Where "Import from ES-DE" is. Each step is one screenful in the panel. */
internal sealed interface EsdeImportStep {
    data object Scanning : EsdeImportStep
    data object NoAccess : EsdeImportStep
    data object NotFound : EsdeImportStep
    data class Choosing(val themes: List<EsdeThemeFolder>, val settings: EsdeSettings?) : EsdeImportStep
    data class Importing(val name: String) : EsdeImportStep
    data class Done(val outcome: EsdeImportOutcome.Imported) : EsdeImportStep
    data object Legacy : EsdeImportStep
    data class Failed(val problem: ThemeImportProblem) : EsdeImportStep
}

/**
 * Drives the import: find themes, read one off the main thread, then hand back the report. The
 * functions are passed in so the flow tests on the JVM without ES-DE or a device.
 */
internal class JarnEsdeThemeImportFlow(
    private val scan: () -> EsdeThemeScan,
    private val runImport: (source: File, selection: EsdeThemeSelection) -> EsdeImportOutcome,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    var step by mutableStateOf<EsdeImportStep>(EsdeImportStep.Scanning)
        private set

    suspend fun start() {
        step = EsdeImportStep.Scanning
        step = when (val found = withContext(io) { scan() }) {
            EsdeThemeScan.NoAccess -> EsdeImportStep.NoAccess
            is EsdeThemeScan.NotFound -> EsdeImportStep.NotFound
            is EsdeThemeScan.Found -> EsdeImportStep.Choosing(found.themes, found.settings)
        }
    }

    suspend fun import(theme: EsdeThemeFolder, settings: EsdeSettings?) =
        importFrom(theme.folder, theme.name, settings.selectionFor(theme))

    /** Repeats an earlier import with the variant, color scheme and aspect ratio it used. */
    suspend fun reimport(update: EsdeThemeUpdate) =
        importFrom(update.source, update.source.name, update.record.selection())

    private suspend fun importFrom(source: File, name: String, selection: EsdeThemeSelection) {
        step = EsdeImportStep.Importing(name)
        step = when (val outcome = withContext(io) { runImport(source, selection) }) {
            is EsdeImportOutcome.Imported -> EsdeImportStep.Done(outcome)
            EsdeImportOutcome.Legacy -> EsdeImportStep.Legacy
            is EsdeImportOutcome.Failed -> EsdeImportStep.Failed(outcome.problem)
        }
    }
}

/** A pending "Your ES-DE theme updated" offer for the active theme, found once per launch. */
internal object JarnEsdeThemeUpdatePending {
    var update by mutableStateOf<EsdeThemeUpdate?>(null)

    /** A restored backup's ES-DE layout this device doesn't have, offered once its folder can be read. */
    var restore by mutableStateOf<EsdeThemeUpdate?>(null)

    var checked = false
}
