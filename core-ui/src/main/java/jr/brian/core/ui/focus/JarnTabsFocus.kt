package jr.brian.core.ui.focus

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Tracks whether a tab row holds focus. Tabs switch as they are focused, so content appearing
 * under them checks this before taking focus and leaves the user on the tab row.
 */
@Stable
class JarnTabsFocus {
    private var owner by mutableStateOf<Any?>(null)

    val hasFocus: Boolean get() = owner != null

    internal fun update(group: Any, focused: Boolean) {
        if (focused) {
            owner = group
        } else if (owner === group) {
            owner = null
        }
    }
}

val LocalJarnTabsFocus = staticCompositionLocalOf { JarnTabsFocus() }
