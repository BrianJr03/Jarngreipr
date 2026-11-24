package jr.brian.jarnfe.look.commandpanel

import jr.brian.core.model.CommandPanelPalette
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

/** Shared by Stage and Deck. [start], [stop] and [refresh] run on the main thread; reads use a one-at-a-time lane. */
internal class CommandPanelThemeController(
    private val port: CommandPanelThemePort,
    private val store: CommandPanelPaletteStore,
    lane: CoroutineDispatcher,
) {
    private val mutablePalette = MutableStateFlow<CommandPanelPalette?>(null)
    val palette: StateFlow<CommandPanelPalette?> = mutablePalette.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + lane)
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private var worker: Job? = null
    private var isSeeded = false

    @Volatile
    private var stored: CommandPanelPalette? = null

    @Volatile
    private var holders = 0

    /** A tiny synchronous read, so the first frame already has the last good colors. */
    fun seedFromCache() {
        if (isSeeded) return
        isSeeded = true
        stored = store.read()
        if (mutablePalette.value == null) mutablePalette.value = stored
    }

    fun start() {
        holders += 1
        if (holders == 1) {
            port.listen(::refresh)
            worker = scope.launch {
                for (request in requests) {
                    val read = runCatching { port.read() }.getOrDefault(CommandPanelRead.Unavailable)
                    ensureActive()
                    apply(read)
                }
            }
        }
        refresh()
    }

    fun refresh() {
        if (holders > 0) requests.trySend(Unit)
    }

    fun stop() {
        if (holders == 0) return
        holders -= 1
        if (holders > 0) return
        port.unlisten()
        worker?.cancel()
        worker = null
        scope.launch { if (holders == 0) port.setObserving(false) }
    }

    private fun apply(read: CommandPanelRead) {
        when (read) {
            is CommandPanelRead.Fresh -> {
                port.setObserving(true)
                mutablePalette.value = read.palette
                if (stored != read.palette) {
                    store.write(read.palette)
                    stored = read.palette
                }
            }
            CommandPanelRead.Locked -> Unit
            CommandPanelRead.Stopped -> port.setObserving(true)
            CommandPanelRead.Untrusted -> {
                port.setObserving(false)
                forgetPalette()
            }
            CommandPanelRead.Rejected -> {
                port.setObserving(true)
                forgetPalette()
            }
            CommandPanelRead.Unavailable -> Unit
        }
    }

    private fun forgetPalette() {
        mutablePalette.value = null
        store.clear()
        stored = null
    }
}
