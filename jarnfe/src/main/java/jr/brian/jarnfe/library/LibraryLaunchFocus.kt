package jr.brian.jarnfe.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * On a cold start the shell's first focus lands on the tab row, because the Library is still
 * loading and has nothing else to focus. The first Library content to focus itself may then take
 * focus from the tab row once, unless the user has pressed or tapped something since the app
 * started.
 */
internal object LibraryLaunchFocus {
    var isPending by mutableStateOf(true)
        private set

    /** The user moved first, so their focus is theirs to keep. */
    fun release() {
        isPending = false
    }

    /** Runs [request] and ends the launch entry once focus has landed in the content. */
    fun settle(request: () -> Boolean): Boolean = request().also { landed -> if (landed) release() }
}

/** Content focuses itself unless the user sits on the tab row by choice. */
internal fun libraryContentTakesFocus(forcing: Boolean, tabsHaveFocus: Boolean, launchEntry: Boolean): Boolean =
    forcing || !tabsHaveFocus || launchEntry
