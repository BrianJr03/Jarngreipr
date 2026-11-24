package jr.brian.jarnfe.utilities

import android.content.ClipData
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import jr.brian.core.ui.components.JarnChip
import jr.brian.core.ui.components.JarnDialog
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.jarnFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.tools.ToolsInitialFocus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val ReaderLineStep = 72.dp
private val ReaderPageStep = 320.dp

/**
 * True only while this window is resumed and holds input focus. A game on the other screen takes
 * focus, so live readings stop for as long as it is being played.
 */
@Composable
internal fun rememberUtilityLive(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by lifecycle.currentStateFlow.collectAsState()
    return LocalWindowInfo.current.isWindowFocused && state.isAtLeast(Lifecycle.State.RESUMED)
}

@Composable
internal fun UtilityLayer(
    onBack: () -> Unit,
    bindings: JarnBindingsBuilder.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onBack)
            on(JarnAction.SwitchScreen, R.string.jarnfe_hint_switch_screen) {
                JarnSession.update { switchActiveScreen() }
            }
            bindings()
        },
        content = content,
    )
}

@Composable
internal fun UtilityTitle(text: String, modifier: Modifier = Modifier) {
    JarnText(
        text = text,
        style = JarnTheme.type.label,
        color = JarnTheme.colors.textHigh,
        modifier = modifier,
    )
}

@Composable
internal fun UtilityNote(text: String, modifier: Modifier = Modifier) {
    JarnText(
        text = text,
        style = JarnTheme.type.caption,
        color = JarnTheme.colors.textMid,
        modifier = modifier,
    )
}

/** Stays composed while hidden, because the dialog hands focus back as it closes. */
@Composable
internal fun UtilityConfirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    restoreFocusTo: FocusRequester? = null,
) {
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        enabled = visible,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onDismiss)
        },
    ) {
        JarnDialog(
            visible = visible,
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            dismissLabel = stringResource(R.string.jarnfe_utilities_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
            restoreFocusTo = restoreFocusTo,
        )
    }
}

/** Long text a controller can read: D-pad scrolls it, then moves on once an edge is reached. */
@Composable
internal fun UtilityReader(
    title: String,
    body: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
    snippets: List<JarnFaqSnippet> = emptyList(),
    bodyFocus: FocusRequester = remember { FocusRequester() },
    bindings: JarnBindingsBuilder.() -> Unit = {},
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val pagePx = with(LocalDensity.current) { ReaderPageStep.toPx() }
    ToolsInitialFocus(bodyFocus)
    UtilityLayer(
        onBack = onBack,
        bindings = {
            on(JarnAction.TabPrevious, R.string.jarnfe_utilities_page_up, repeats = true) {
                scope.launch { scroll.scrollBy(-pagePx) }
            }
            on(JarnAction.TabNext, R.string.jarnfe_utilities_page_down, repeats = true) {
                scope.launch { scroll.scrollBy(pagePx) }
            }
            bindings()
        },
    ) {
        Column(
            modifier = modifier.fillMaxSize().padding(JarnTheme.space.lg),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            UtilityTitle(title)
            ReaderBody(body, snippets, monospace, scroll, bodyFocus, Modifier.weight(1f).fillMaxWidth())
            if (snippets.isNotEmpty()) SnippetCopyRow(snippets)
        }
    }
}

@Composable
private fun ReaderBody(
    body: String,
    snippets: List<JarnFaqSnippet>,
    monospace: Boolean,
    scroll: ScrollState,
    focus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val linePx = with(LocalDensity.current) { ReaderLineStep.toPx() }
    val shape = JarnTheme.radius.smShape
    Box(
        modifier = modifier
            .focusRequester(focus)
            .jarnFocus(shape, source)
            .onKeyEvent { event -> event.scrollReader(scroll, scope, linePx) }
            .focusable(interactionSource = source),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(scroll).padding(JarnTheme.space.xs),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        ) {
            ReaderText(body, monospace)
            snippets.forEach { snippet ->
                Column(verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs)) {
                    UtilityNote(snippet.label)
                    ReaderText(snippet.text, monospace = true)
                }
            }
        }
    }
}

private fun androidx.compose.ui.input.key.KeyEvent.scrollReader(
    scroll: ScrollState,
    scope: CoroutineScope,
    linePx: Float,
): Boolean {
    if (type != KeyEventType.KeyDown) return false
    val delta = when {
        key == Key.DirectionDown && scroll.canScrollForward -> linePx
        key == Key.DirectionUp && scroll.canScrollBackward -> -linePx
        else -> return false
    }
    scope.launch { scroll.scrollBy(delta) }
    return true
}

@Composable
private fun ReaderText(text: String, monospace: Boolean) {
    val base = if (monospace) JarnTheme.type.caption else JarnTheme.type.body
    JarnText(
        text = text,
        style = if (monospace) base.copy(fontFamily = FontFamily.Monospace) else base,
        color = JarnTheme.colors.textHigh,
    )
}

@Composable
private fun SnippetCopyRow(snippets: List<JarnFaqSnippet>) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf<JarnFaqSnippet?>(null) }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        snippets.forEach { snippet ->
            val isCopied = copied == snippet
            JarnChip(
                label = if (isCopied) {
                    stringResource(R.string.jarnfe_utilities_copied)
                } else {
                    stringResource(R.string.jarnfe_utilities_copy, snippet.label)
                },
                selected = isCopied,
                onClick = {
                    copied = snippet
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(snippet.label, snippet.text)))
                    }
                },
            )
        }
    }
}
