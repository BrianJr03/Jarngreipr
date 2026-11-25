package jr.brian.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnBaseTypeScope
import jr.brian.core.ui.theme.JarnTheme

@Composable
private fun JarnModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    restoreFocusTo: FocusRequester?,
    initialFocus: FocusRequester?,
    alignment: Alignment,
    shape: Shape,
    panelModifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val transition = remember { MutableTransitionState(false) }
    transition.targetState = visible
    var wasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            wasOpen = true
        } else if (wasOpen) {
            wasOpen = false
            // What opened the modal can be gone by now; then focus simply stays where it is.
            runCatching { restoreFocusTo?.requestFocus() }
        }
    }
    if (!visible && !transition.currentState && transition.isIdle) return

    val motion = JarnTheme.motion
    val offsetPx = with(LocalDensity.current) { motion.depthOffset.roundToPx() }
    val latestDismiss by rememberUpdatedState(onDismiss)
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = transition,
            enter = fadeIn(motion.panel.spec()),
            exit = fadeOut(motion.panel.spec()),
        ) {
            JarnModalScrim { latestDismiss() }
        }
        AnimatedVisibility(
            visibleState = transition,
            modifier = Modifier.align(alignment),
            enter = motion.panelEnter(offsetPx),
            exit = motion.panelExit(offsetPx),
        ) {
            JarnBaseTypeScope {
                JarnModalPanel(initialFocus, shape, panelModifier, { latestDismiss() }, content)
            }
        }
    }
}

@Composable
private fun JarnModalScrim(onDismiss: () -> Unit) {
    val latestDismiss by rememberUpdatedState(onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(JarnTheme.colors.artScrim)
            .pointerInput(Unit) { detectTapGestures { latestDismiss() } },
    )
}

@Composable
private fun JarnModalPanel(
    initialFocus: FocusRequester?,
    shape: Shape,
    modifier: Modifier,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val containerFocus = remember { FocusRequester() }
    var backArmed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { (initialFocus ?: containerFocus).requestFocus() }
    Column(
        modifier = modifier
            .semantics { dialog() }
            .onKeyEvent { event ->
                if (event.key != Key.Back && event.key != Key.ButtonB) return@onKeyEvent false
                when (event.type) {
                    KeyEventType.KeyDown -> backArmed = true
                    KeyEventType.KeyUp -> if (backArmed) {
                        backArmed = false
                        onDismiss()
                    }
                }
                true
            }
            .focusRequester(containerFocus)
            .focusProperties { onExit = { cancelFocusChange() } }
            .focusGroup()
            .background(JarnTheme.colors.surface, shape)
            .padding(JarnTheme.space.lg),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        content = content,
    )
}

@Composable
fun JarnSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    restoreFocusTo: FocusRequester? = null,
    initialFocus: FocusRequester? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    JarnModal(
        visible = visible,
        onDismiss = onDismiss,
        restoreFocusTo = restoreFocusTo,
        initialFocus = initialFocus,
        alignment = Alignment.BottomCenter,
        shape = JarnTheme.radius.sheetShape,
        panelModifier = modifier.fillMaxWidth(),
    ) {
        title?.let { JarnText(it, JarnTheme.type.heading, JarnTheme.colors.textHigh, maxLines = 2) }
        content()
    }
}

@Composable
fun JarnDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    restoreFocusTo: FocusRequester? = null,
) {
    val confirmFocus = remember { FocusRequester() }
    val dismissFocus = remember { FocusRequester() }
    JarnModal(
        visible = visible,
        onDismiss = onDismiss,
        restoreFocusTo = restoreFocusTo,
        initialFocus = if (destructive) dismissFocus else confirmFocus,
        alignment = Alignment.Center,
        shape = JarnTheme.radius.xlShape,
        panelModifier = modifier
            .padding(JarnTheme.space.xl)
            .widthIn(max = 480.dp),
    ) {
        JarnText(title, JarnTheme.type.heading, JarnTheme.colors.textHigh, maxLines = 2)
        JarnText(message, JarnTheme.type.body, JarnTheme.colors.textMid)
        Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md)) {
            JarnButton(
                label = dismissLabel,
                onClick = onDismiss,
                modifier = Modifier.weight(1f).focusRequester(dismissFocus),
                variant = JarnButtonVariant.Secondary,
            )
            JarnButton(
                label = confirmLabel,
                onClick = onConfirm,
                modifier = Modifier.weight(1f).focusRequester(confirmFocus),
                variant = if (destructive) JarnButtonVariant.Danger else JarnButtonVariant.Primary,
            )
        }
    }
}
