package jr.brian.home.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jr.brian.home.ui.theme.ThemePrimaryColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A scrollable column a gamepad can read: it takes focus, D-pad up/down scrolls it, and
 * focus only moves on once the content is scrolled to that edge.
 */
@Composable
fun GamepadScrollColumn(
    modifier: Modifier = Modifier,
    focusOnOpen: Boolean = false,
    step: Dp = 120.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val stepPx = with(LocalDensity.current) { step.toPx() }
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)

    if (focusOnOpen) {
        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    }

    Column(
        modifier = modifier
            .border(1.dp, if (isFocused) ThemePrimaryColor.copy(alpha = 0.6f) else Color.Transparent, shape)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { scrollOnDpad(it, scrollState, stepPx, scope) }
            .focusable()
            .verticalScroll(scrollState)
            .padding(horizontal = 4.dp),
        content = content,
    )
}

private fun scrollOnDpad(
    event: androidx.compose.ui.input.key.KeyEvent,
    scrollState: ScrollState,
    stepPx: Float,
    scope: CoroutineScope,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val delta = when (event.nativeKeyEvent.keyCode) {
        KeyEvent.KEYCODE_DPAD_DOWN -> if (scrollState.canScrollForward) stepPx else return false
        KeyEvent.KEYCODE_DPAD_UP -> if (scrollState.canScrollBackward) -stepPx else return false
        else -> return false
    }
    scope.launch { scrollState.animateScrollBy(delta) }
    return true
}
