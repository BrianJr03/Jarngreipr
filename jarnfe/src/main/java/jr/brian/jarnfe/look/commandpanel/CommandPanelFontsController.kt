package jr.brian.jarnfe.look.commandpanel

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import jr.brian.core.ui.theme.JarnType
import jr.brian.core.ui.theme.jarnTypeWithFonts
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What Jarn draws with while matching fonts: the row and the type built from it. */
@Immutable
internal data class CommandPanelFontMatch(val fonts: CommandPanelFonts, val type: JarnType)

/** Resolves each role's imported family; null when it has none or its file can't be had. */
internal fun buildCommandPanelFontMatch(
    fonts: CommandPanelFonts,
    family: (CommandPanelFont.Imported) -> FontFamily?,
): CommandPanelFontMatch {
    fun bundledKey(font: CommandPanelFont?) = (font as? CommandPanelFont.Bundled)?.key
    fun imported(font: CommandPanelFont?) = (font as? CommandPanelFont.Imported)?.let(family)
    val type = jarnTypeWithFonts(
        displayKey = bundledKey(fonts.display),
        labelKey = bundledKey(fonts.label),
        importedDisplay = imported(fonts.display),
        importedLabel = imported(fonts.label),
    )
    return CommandPanelFontMatch(fonts, type)
}

/**
 * Shared by Stage and Deck, like [CommandPanelThemeController]. [start], [stop] and [refresh] run on the main thread;
 * reads and font copies use a one-at-a-time lane.
 */
internal class CommandPanelFontsController(
    private val port: CommandPanelFontsPort,
    private val rows: CommandPanelFontsRowCache,
    private val fonts: CommandPanelFontCache,
    lane: CoroutineDispatcher,
) : CommandPanelLifecycleTarget {
    private val mutableMatch = MutableStateFlow<CommandPanelFontMatch?>(null)
    val match: StateFlow<CommandPanelFontMatch?> = mutableMatch.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + lane)
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private var worker: Job? = null
    private var isSeeded = false

    @Volatile
    private var stored: CommandPanelFonts? = null

    @Volatile
    private var holders = 0

    /** Synchronous, from verified copies only, so the first frame already has the last good fonts. Never fetches. */
    fun seedFromCache() {
        if (isSeeded) return
        isSeeded = true
        val cached = rows.read() ?: return
        stored = cached
        if (mutableMatch.value == null) {
            mutableMatch.value = buildCommandPanelFontMatch(cached) { fonts.family(it.sha256, open = null) }
        }
    }

    override fun start() {
        holders += 1
        if (holders == 1) {
            port.listen(::refresh)
            worker = scope.launch {
                for (request in requests) {
                    val read = runCatching { port.read() }.getOrDefault(CommandPanelFontsRead.Unavailable)
                    ensureActive()
                    apply(read)
                }
            }
        }
        refresh()
    }

    override fun refresh() {
        if (holders > 0) requests.trySend(Unit)
    }

    override fun stop() {
        if (holders == 0) return
        holders -= 1
        if (holders > 0) return
        port.unlisten()
        worker?.cancel()
        worker = null
        scope.launch { if (holders == 0) port.setObserving(false) }
    }

    /** Deletes every copy and the cached row. For when matching is off, so nothing outlives the setting. */
    fun forget() {
        scope.launch { if (holders == 0) forgetFonts() }
    }

    private fun apply(read: CommandPanelFontsRead) {
        when (read) {
            is CommandPanelFontsRead.Fresh -> {
                port.setObserving(true)
                mutableMatch.value = buildCommandPanelFontMatch(read.fonts) { font ->
                    fonts.family(font.sha256) { port.openFont(font.id) }
                }
                fonts.retain(read.fonts.imported.map { it.sha256 }.toSet())
                if (stored != read.fonts) {
                    rows.write(read.fonts)
                    stored = read.fonts
                }
            }
            CommandPanelFontsRead.Locked,
            CommandPanelFontsRead.Unavailable,
            -> Unit
            CommandPanelFontsRead.Stopped -> port.setObserving(true)
            CommandPanelFontsRead.Untrusted -> {
                port.setObserving(false)
                forgetFonts()
            }
            CommandPanelFontsRead.Unsupported,
            CommandPanelFontsRead.Rejected,
            -> {
                port.setObserving(true)
                forgetFonts()
            }
        }
    }

    private fun forgetFonts() {
        mutableMatch.value = null
        rows.clear()
        fonts.clear()
        stored = null
    }
}
