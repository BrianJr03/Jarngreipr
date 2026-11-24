package jr.brian.jarnfe.canvas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.LocalJarnInputRouter
import jr.brian.jarnfe.input.LocalJarnScreen

private val EmptySpaceMaxWidth = 360.dp

/** Replaces the grid on a page with no tiles. Left and right still change page on a controller. */
@Composable
internal fun CanvasEmptySpace(
    page: JarnCanvasPage,
    sheetOpen: Boolean,
    onAdd: () -> Unit,
    onOptions: () -> Unit,
) {
    val first = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    val pages = JarnCanvas.snapshot?.pages.orEmpty()
    val previous = canvasPageInDirection(pages, page.index, -1)
    val next = canvasPageInDirection(pages, page.index, 1)
    JarnInputLayer(
        enabled = !sheetOpen && hasFocus,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            previous?.let { on(JarnAction.MoveLeft, repeats = false) { JarnCanvas.selectedPage = it.index } }
            next?.let { on(JarnAction.MoveRight, repeats = false) { JarnCanvas.selectedPage = it.index } }
        },
    )
    CanvasEmptySpaceFocus(first, page.index, sheetOpen)
    Column(
        modifier = Modifier
            .onFocusChanged { hasFocus = it.hasFocus }
            .widthIn(max = EmptySpaceMaxWidth)
            .verticalScroll(rememberScrollState())
            .padding(JarnTheme.space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
    ) {
        CanvasEmptySpaceText()
        JarnButton(
            label = stringResource(R.string.jarnfe_canvas_option_add),
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().focusRequester(first),
        )
        JarnButton(
            label = stringResource(R.string.jarnfe_canvas_options_title),
            onClick = onOptions,
            modifier = Modifier.fillMaxWidth(),
            variant = JarnButtonVariant.Secondary,
        )
    }
}

@Composable
private fun CanvasEmptySpaceText() {
    JarnText(
        text = stringResource(R.string.jarnfe_canvas_empty_title),
        style = JarnTheme.type.title,
        color = JarnTheme.colors.textHigh,
        maxLines = 2,
        textAlign = TextAlign.Center,
    )
    JarnText(
        text = stringResource(R.string.jarnfe_canvas_empty_message),
        style = JarnTheme.type.body,
        color = JarnTheme.colors.textMid,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CanvasEmptySpaceFocus(first: FocusRequester, pageIndex: Int, sheetOpen: Boolean) {
    val router = LocalJarnInputRouter.current
    val isActive = router.activeScreen == LocalJarnScreen.current
    val tabsFocus = LocalJarnTabsFocus.current
    LaunchedEffect(pageIndex, sheetOpen, isActive) {
        if (sheetOpen || !isActive) return@LaunchedEffect
        withFrameNanos { }
        if (!tabsFocus.hasFocus) runCatching { first.requestFocus() }
    }
}
