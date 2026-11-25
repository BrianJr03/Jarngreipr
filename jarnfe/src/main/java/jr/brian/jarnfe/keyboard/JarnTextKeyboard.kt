package jr.brian.jarnfe.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode

private const val MASK_CHAR = '•'
private const val VISIBLE_ENTRY_CHARS = 40

/**
 * Full text entry for pads. A types, Select deletes (hold to repeat), Y shifts, L1 toggles symbols,
 * R1 adds a space, Start finishes, B cancels.
 */
@Composable
internal fun JarnTextKeyboard(
    label: String,
    masked: Boolean,
    onDone: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    initialText: String = "",
    maxLength: Int = JarnKeyboardState.MAX_LENGTH,
) {
    var state by remember { mutableStateOf(JarnKeyboardState(initialText, maxLength = maxLength)) }
    val firstKey = remember { FocusRequester() }
    JarnKeyboardAutoFocus(firstKey)
    val press: (JarnKeyboardKey) -> Unit = { key ->
        if (key == JarnKeyboardKey.Done) onDone(state.text) else state = state.press(key)
    }
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = { textKeyboardBindings(press, onCancel) },
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs)) {
            KeyboardEntry(label, state.text, masked)
            JarnKeyboardGrid(
                rows = remember(state.page) { jarnKeyboardRows(state.page) },
                controls = JarnKeyboardControls,
                firstKey = firstKey,
                onKey = press,
            )
        }
    }
}

private fun JarnBindingsBuilder.textKeyboardBindings(
    press: (JarnKeyboardKey) -> Unit,
    onCancel: () -> Unit,
) {
    hint(JarnAction.Confirm, R.string.jarnfe_keyboard_type)
    on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onCancel)
    on(JarnAction.Context, R.string.jarnfe_keyboard_delete, repeats = true) { press(JarnKeyboardKey.Delete) }
    on(JarnAction.Search, R.string.jarnfe_keyboard_shift) { press(JarnKeyboardKey.Shift) }
    on(JarnAction.TabPrevious, R.string.jarnfe_keyboard_symbols) { press(JarnKeyboardKey.Symbols) }
    on(JarnAction.TabNext, R.string.jarnfe_keyboard_space, repeats = true) { press(JarnKeyboardKey.Space) }
    on(JarnAction.Menu, R.string.jarnfe_keyboard_done) { press(JarnKeyboardKey.Done) }
}

@Composable
private fun KeyboardEntry(label: String, text: String, masked: Boolean) {
    val shown = if (masked) MASK_CHAR.toString().repeat(text.length) else text
    Column(
        Modifier
            .fillMaxWidth()
            .background(JarnTheme.colors.raised, JarnTheme.radius.smShape)
            .padding(horizontal = JarnTheme.space.md, vertical = JarnTheme.space.xs),
    ) {
        JarnText(label, JarnTheme.type.caption, JarnTheme.colors.textMid, maxLines = 1)
        JarnText(
            text = shown.takeLast(VISIBLE_ENTRY_CHARS).ifEmpty { " " },
            style = JarnTheme.baseType.label,
            color = JarnTheme.colors.textHigh,
            maxLines = 1,
        )
    }
}
