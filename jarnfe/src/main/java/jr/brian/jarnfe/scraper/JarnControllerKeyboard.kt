package jr.brian.jarnfe.scraper

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import jr.brian.jarnfe.keyboard.JarnKeyboardState
import jr.brian.jarnfe.keyboard.JarnTextKeyboard

/** Kept at this path for existing callers. New code can use [JarnTextKeyboard] directly. */
@Composable
internal fun JarnControllerKeyboard(
    label: String,
    masked: Boolean,
    onDone: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    initialText: String = "",
    maxLength: Int = JarnKeyboardState.MAX_LENGTH,
) {
    JarnTextKeyboard(label, masked, onDone, onCancel, modifier, initialText, maxLength)
}
