package jr.brian.jarnfe.settings

import android.content.Context
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jr.brian.frontend.library.migrate.EsdeImportProgress
import jr.brian.frontend.library.migrate.EsdeImportSummary
import jr.brian.frontend.library.setup.EsdeDetection
import jr.brian.frontend.library.setup.LibrarySetup
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.library.jarnLibrarySetupOrNull
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Where "Import from ES-DE" is. */
internal sealed interface JarnEsdeImportState {
    data object Idle : JarnEsdeImportState

    /** [progress] is null until the first gamelist is reached. */
    data class Running(val progress: EsdeImportProgress? = null) : JarnEsdeImportState
    data class Done(val summary: EsdeImportSummary) : JarnEsdeImportState

    /** All-files access is missing, so ES-DE's folder can't be read. Nothing was imported. */
    data object NeedsAccess : JarnEsdeImportState
    data object Failed : JarnEsdeImportState
}

/** What the import needs from the library. */
internal interface JarnEsdeImportBackend {
    val progress: StateFlow<EsdeImportProgress?>
    val lastImport: StateFlow<EsdeImportSummary?>
    fun hasFileAccess(): Boolean
    suspend fun detect(): EsdeDetection
    suspend fun import(): EsdeImportSummary
}

internal class LibrarySetupEsdeImportBackend(private val setup: LibrarySetup) : JarnEsdeImportBackend {
    override val progress get() = setup.esdeImportProgress
    override val lastImport get() = setup.lastEsdeImport
    override fun hasFileAccess(): Boolean = Environment.isExternalStorageManager()
    override suspend fun detect(): EsdeDetection = setup.detectEsde()
    override suspend fun import(): EsdeImportSummary = setup.importFromEsde()
}

/**
 * Runs "Import from ES-DE" for first run and Settings. One run at a time. The run
 * belongs to [scope], so moving on or pressing B never cancels it. State is Compose
 * state, so any screen reading it follows along.
 *
 * @param onFinished called with each finished import, and whether a screen that
 *   shows the summary was watching.
 */
internal class JarnEsdeImporter(
    private val backend: JarnEsdeImportBackend,
    private val scope: CoroutineScope,
    private val onFinished: (summary: EsdeImportSummary, seen: Boolean) -> Unit = { _, _ -> },
) {
    var state by mutableStateOf<JarnEsdeImportState>(JarnEsdeImportState.Idle)
        private set

    /** The screen showing the consent dialog, or null when it's closed. */
    var consentOn by mutableStateOf<JarnScreen?>(null)
        private set

    /** Null until [refreshDetection] has run once. */
    var detection by mutableStateOf<EsdeDetection?>(null)
        private set

    val lastImport: StateFlow<EsdeImportSummary?> get() = backend.lastImport

    /** Counts returns from Android's file access screen, so the row or button that asked takes focus back. */
    var accessReturns by mutableIntStateOf(0)
        private set

    private var awaitingAccess = false
    private var viewers = 0
    private var noticeViewers = 0

    val isRunning: Boolean get() = state is JarnEsdeImportState.Running

    /** "Bring them over", "Re-import" or "Try again": imports, or asks for file access first. */
    fun request(screen: JarnScreen) {
        if (isRunning) return
        if (backend.hasFileAccess()) start() else consentOn = screen
    }

    fun cancelConsent() {
        consentOn = null
    }

    /** Continue in the dialog. [openFileAccess] opens Android's file access screen; false if it couldn't. */
    fun confirmConsent(openFileAccess: () -> Boolean) {
        consentOn = null
        when {
            backend.hasFileAccess() -> start()
            openFileAccess() -> awaitingAccess = true
            else -> state = JarnEsdeImportState.NeedsAccess
        }
    }

    /** Back from the file access screen: imports when access was granted, otherwise says it's missing. */
    fun onResumed() {
        if (!awaitingAccess) return
        awaitingAccess = false
        accessReturns++
        if (backend.hasFileAccess()) start() else state = JarnEsdeImportState.NeedsAccess
    }

    /** Clears a finished, failed or waiting state. A running import keeps going. */
    fun dismiss() {
        if (!isRunning) state = JarnEsdeImportState.Idle
    }

    suspend fun refreshDetection(): EsdeDetection =
        runCatching { backend.detect() }.getOrElse { EsdeDetection(found = false, hasFileAccess = false) }
            .also { detection = it }

    /**
     * A screen showing the import's progress and summary. [wantsNotice] screens get a
     * notice on finishing anyway; without any screen watching, a notice always shows.
     * Returns the call that ends the watch.
     */
    fun watch(wantsNotice: Boolean = false): () -> Unit {
        viewers++
        if (wantsNotice) noticeViewers++
        var ended = false
        return {
            if (!ended) {
                ended = true
                viewers--
                if (wantsNotice) noticeViewers--
            }
        }
    }

    private fun start() {
        state = JarnEsdeImportState.Running()
        scope.launch {
            val progress = launch {
                backend.progress.collect { step ->
                    if (isRunning && step != null) state = JarnEsdeImportState.Running(step)
                }
            }
            state = try {
                val summary = backend.import()
                if (summary.accessMissing) {
                    JarnEsdeImportState.NeedsAccess
                } else {
                    onFinished(summary, viewers > 0 && noticeViewers == 0)
                    JarnEsdeImportState.Done(summary)
                }
            } catch (e: CancellationException) {
                state = JarnEsdeImportState.Idle
                throw e
            } catch (_: Exception) {
                JarnEsdeImportState.Failed
            } finally {
                progress.cancel()
            }
        }
    }
}

/** The app-wide importer, so a run outlives the screen that started it. */
internal object JarnEsdeImport {
    var importer by mutableStateOf<JarnEsdeImporter?>(null)
        private set

    fun connect(context: Context): JarnEsdeImporter? {
        importer?.let { return it }
        val setup = context.jarnLibrarySetupOrNull() ?: return null
        return JarnEsdeImporter(
            backend = LibrarySetupEsdeImportBackend(setup),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            onFinished = ::noticeIfUnseen,
        ).also { importer = it }
    }

    private fun noticeIfUnseen(summary: EsdeImportSummary, seen: Boolean) {
        if (!seen) JarnNotices.showCount(R.plurals.jarnfe_esde_import_games, summary.gamesUpdated)
    }
}
