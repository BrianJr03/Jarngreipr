package jr.brian.core.model.bus

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default in-memory [FrontendBus] implementation. Buffers events and
 * commands with a small replay cache so a late-starting consumer still
 * receives the most recent emission (bounded at [REPLAY_SIZE] to keep
 * memory flat).
 *
 * This is intentionally the only implementation shipped in Phase 1. The
 * bridging to `RomSearchStateHolder` happens in later phases as :app
 * swaps producers over one at a time; the interface stays stable.
 */
@Singleton
class FrontendBusAdapter @Inject constructor() : FrontendBus {

    private val _focus = MutableStateFlow<FrontendFocus?>(null)
    override val focus: StateFlow<FrontendFocus?> = _focus.asStateFlow()

    private val _events = MutableSharedFlow<FrontendEvent>(
        replay = REPLAY_SIZE,
        extraBufferCapacity = EXTRA_BUFFER_CAPACITY,
    )
    override val events: SharedFlow<FrontendEvent> = _events.asSharedFlow()

    private val _commands = MutableSharedFlow<FrontendCommand>(
        replay = REPLAY_SIZE,
        extraBufferCapacity = EXTRA_BUFFER_CAPACITY,
    )
    override val commands: SharedFlow<FrontendCommand> = _commands.asSharedFlow()

    private val _launcherPresent = MutableStateFlow(false)
    override val launcherPresent: StateFlow<Boolean> = _launcherPresent.asStateFlow()

    private val _frontendPresent = MutableStateFlow(false)
    override val frontendPresent: StateFlow<Boolean> = _frontendPresent.asStateFlow()

    override suspend fun publishFocus(focus: FrontendFocus?) {
        _focus.value = focus
    }

    override suspend fun publishEvent(event: FrontendEvent) {
        _events.emit(event)
    }

    override suspend fun sendCommand(command: FrontendCommand) {
        _commands.emit(command)
    }

    override fun setLauncherPresent(present: Boolean) {
        _launcherPresent.value = present
    }

    override fun setFrontendPresent(present: Boolean) {
        _frontendPresent.value = present
    }

    private companion object {
        // Keep enough replay for a cold-start consumer to catch the last
        // command / event but not so much that the buffer leaks memory as
        // the launcher runs for days.
        const val REPLAY_SIZE = 1
        const val EXTRA_BUFFER_CAPACITY = 16
    }
}
