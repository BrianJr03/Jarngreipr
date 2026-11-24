package jr.brian.jarnfe.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnScreen

class JarnSessionStore(private val onInputScreenChanged: (JarnScreen) -> Unit = {}) {
    var state by mutableStateOf(JarnSessionState())
        private set

    var deckHostCreated = false

    private var initialized = false

    fun update(transform: JarnSessionState.() -> JarnSessionState) {
        initialized = true
        state = state.transform()
        onInputScreenChanged(state.inputScreen)
    }

    /** Only a new process restores, so a live session is never rolled back by a recreated activity. */
    fun restoreIfFresh(encoded: String?, rememberedTab: DeckTab? = null) {
        if (initialized) return
        initialized = true
        state = JarnSessionState.decode(encoded) ?: rememberedTab?.let { state.copy(deckTab = it) } ?: state
        onInputScreenChanged(state.inputScreen)
    }
}

val JarnInput = JarnInputRouter()

val JarnSession = JarnSessionStore(JarnInput::activate)
