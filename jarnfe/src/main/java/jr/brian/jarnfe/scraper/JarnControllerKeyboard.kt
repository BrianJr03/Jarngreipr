package jr.brian.jarnfe.scraper

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode

private const val MASK_CHAR = '•'
private const val VISIBLE_ENTRY_CHARS = 40
private val KeyMinHeight = 36.dp

/** On-screen keyboard for pads. A types, X deletes, Y shifts, Start finishes, B cancels. */
@Composable
internal fun JarnControllerKeyboard(
    label: String,
    masked: Boolean,
    onDone: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf(JarnKeyboardState()) }
    val firstKey = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstKey.requestFocus() }
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_keyboard_type)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onCancel)
            on(JarnAction.Context, R.string.jarnfe_keyboard_delete) {
                state = state.press(JarnKeyboardKey.Delete)
            }
            on(JarnAction.Search, R.string.jarnfe_keyboard_shift) {
                state = state.press(JarnKeyboardKey.Shift)
            }
            on(JarnAction.Menu, R.string.jarnfe_keyboard_done) { onDone(state.text) }
        },
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs)) {
            KeyboardEntry(label, state.text, masked)
            jarnKeyboardRows(state.page).forEachIndexed { rowIndex, row ->
                KeyboardRow {
                    row.forEachIndexed { keyIndex, key ->
                        val focus = if (rowIndex == 0 && keyIndex == 0) {
                            Modifier.focusRequester(firstKey)
                        } else {
                            Modifier
                        }
                        KeyboardKeyCap(
                            text = key.char.toString(),
                            onClick = { state = state.press(key) },
                            modifier = Modifier.weight(1f).then(focus),
                        )
                    }
                }
            }
            KeyboardRow {
                JarnKeyboardControls.forEach { key ->
                    KeyboardKeyCap(
                        text = stringResource(key.labelRes()),
                        onClick = {
                            if (key == JarnKeyboardKey.Done) onDone(state.text) else state = state.press(key)
                        },
                        modifier = Modifier.weight(if (key == JarnKeyboardKey.Space) 2f else 1f),
                    )
                }
            }
        }
    }
}

@StringRes
private fun JarnKeyboardKey.labelRes(): Int = when (this) {
    JarnKeyboardKey.Shift -> R.string.jarnfe_keyboard_shift
    JarnKeyboardKey.Symbols -> R.string.jarnfe_keyboard_symbols
    JarnKeyboardKey.Space -> R.string.jarnfe_keyboard_space
    JarnKeyboardKey.Delete -> R.string.jarnfe_keyboard_delete
    JarnKeyboardKey.Done -> R.string.jarnfe_keyboard_done
    is JarnKeyboardKey.Letter -> R.string.jarnfe_keyboard_type
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
            style = JarnTheme.type.label,
            color = JarnTheme.colors.textHigh,
            maxLines = 1,
        )
    }
}

@Composable
private fun KeyboardRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        content = content,
    )
}

@Composable
private fun KeyboardKeyCap(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val shape = JarnTheme.radius.smShape
    Box(
        modifier = modifier
            .jarnInteractive(shape, source, onClick = onClick)
            .heightIn(min = KeyMinHeight)
            .background(JarnTheme.colors.raised, shape),
        contentAlignment = Alignment.Center,
    ) {
        JarnText(text, JarnTheme.type.label, JarnTheme.colors.textHigh, maxLines = 1)
    }
}
