package jr.brian.jarnfe.keyboard

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R

private const val WIDE_KEY_WEIGHT = 2f

internal enum class JarnKeyboardSize(val keyHeight: Dp) { Compact(36.dp), Roomy(44.dp) }

/** Takes focus for [firstKey] unless the user is still on a tab row. */
@Composable
internal fun JarnKeyboardAutoFocus(firstKey: FocusRequester) {
    val tabsFocus = LocalJarnTabsFocus.current
    LaunchedEffect(Unit) { if (!tabsFocus.hasFocus) firstKey.requestFocus() }
}

/** Waits a frame so it lands after the window's own fallback focus, then takes it. */
@Composable
internal fun JarnKeyboardTakeFocus(firstKey: FocusRequester) {
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { firstKey.requestFocus() }
    }
}

/** The key grid every Jarn FE keyboard shares. Callers own the text and the button bindings. */
@Composable
internal fun JarnKeyboardGrid(
    rows: List<List<JarnKeyboardKey.Letter>>,
    controls: List<JarnKeyboardKey>,
    firstKey: FocusRequester,
    onKey: (JarnKeyboardKey) -> Unit,
    modifier: Modifier = Modifier,
    size: JarnKeyboardSize = JarnKeyboardSize.Compact,
    letterLabel: (Char) -> String = Char::toString,
) {
    var focused by remember { mutableStateOf<JarnKeyPosition?>(null) }
    val requesters = remember(firstKey) {
        hashMapOf(JarnKeyPosition(0, 0) to firstKey)
    }
    val focusedBeforeRowsChanged = remember(rows) { focused }
    LaunchedEffect(rows) {
        val current = focusedBeforeRowsChanged ?: return@LaunchedEffect
        val clamped = current.clampedTo(rows)
        if (clamped != current) {
            focused = clamped
            withFrameNanos { }
            runCatching { requesters[clamped]?.requestFocus() }
        }
    }
    val textStyle = if (size == JarnKeyboardSize.Roomy) JarnTheme.baseType.labelStrong else JarnTheme.baseType.label
    Column(modifier, verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs)) {
        rows.forEachIndexed { rowIndex, row ->
            KeyboardRow {
                row.forEachIndexed { keyIndex, key ->
                    val position = JarnKeyPosition(rowIndex, keyIndex)
                    KeyboardKeyCap(
                        text = letterLabel(key.char),
                        height = size.keyHeight,
                        textStyle = textStyle,
                        onClick = { onKey(key) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(requesters.getOrPut(position) { FocusRequester() })
                            .onFocusChanged { if (it.isFocused) focused = position },
                    )
                }
            }
        }
        KeyboardRow {
            controls.forEach { key ->
                KeyboardKeyCap(
                    text = stringResource(key.labelRes()),
                    height = size.keyHeight,
                    textStyle = textStyle,
                    onClick = { onKey(key) },
                    modifier = Modifier
                        .weight(if (key == JarnKeyboardKey.Space) WIDE_KEY_WEIGHT else 1f)
                        .onFocusChanged { if (it.isFocused) focused = null },
                )
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
    JarnKeyboardKey.Clear -> R.string.jarnfe_keyboard_clear
    JarnKeyboardKey.Done -> R.string.jarnfe_keyboard_done
    is JarnKeyboardKey.Letter -> R.string.jarnfe_keyboard_type
}

@Composable
private fun KeyboardRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        content = content,
    )
}

@Composable
private fun KeyboardKeyCap(
    text: String,
    height: Dp,
    textStyle: TextStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val shape = JarnTheme.radius.smShape
    Box(
        modifier = modifier
            .jarnInteractive(shape, source, onClick = onClick)
            .heightIn(min = height)
            .background(JarnTheme.colors.raised, shape),
        contentAlignment = Alignment.Center,
    ) {
        JarnText(text, textStyle, JarnTheme.colors.textHigh, maxLines = 1)
    }
}
