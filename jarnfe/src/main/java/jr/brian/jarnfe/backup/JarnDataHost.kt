package jr.brian.jarnfe.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnSheet
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.jarnFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import kotlinx.coroutines.launch

private val ReadPaneStep = 96.dp

/** Sits outside the panes so its sheets can use the whole window. */
@Composable
internal fun JarnDataHost() {
    DataFocusReturn()
    val context = LocalContext.current
    val services = remember(context) { context.jarnDataServicesOrNull() } ?: return
    val sheet = JarnDataFlow.sheet ?: return
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            if (!JarnDataFlow.isBusy) hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(
                JarnAction.Back,
                if (JarnDataFlow.isBusy) null else R.string.jarnfe_hint_back,
                onPress = JarnDataFlow::dismiss,
            )
        },
    ) {
        key(sheet::class) { DataSheetContent(sheet, services) }
    }
}

@Composable
private fun DataFocusReturn() {
    val isOpen = JarnDataFlow.sheet != null
    var wasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(isOpen) {
        if (wasOpen && !isOpen) {
            withFrameNanos { }
            JarnDataFlow.returnFocus?.let { runCatching { it.requestFocus() } }
        }
        wasOpen = isOpen
    }
}

@Composable
private fun DataSheetContent(sheet: JarnDataSheet, services: JarnDataServices) {
    when (sheet) {
        is JarnDataSheet.Working -> WorkingSheet(sheet)
        is JarnDataSheet.Message -> MessageSheet(sheet)
        is JarnDataSheet.RestorePreview -> JarnRestorePreviewSheet(
            preview = sheet.candidate.preview,
            onCancel = JarnDataFlow::dismiss,
            onRestore = { JarnDataFlow.confirmRestore(services.backup) },
        )
        else -> JarnUpdateSheetContent(sheet, services.updates)
    }
}

@Composable
private fun WorkingSheet(sheet: JarnDataSheet.Working) {
    JarnDataSheetFrame(stringResource(sheet.messageRes), initialFocus = null) {
        JarnText(
            text = stringResource(R.string.jarnfe_data_working_hint),
            style = JarnTheme.type.body,
            color = JarnTheme.colors.textMid,
        )
    }
}

@Composable
private fun MessageSheet(sheet: JarnDataSheet.Message) {
    val close = remember { FocusRequester() }
    JarnDataSheetFrame(stringResource(sheet.titleRes), initialFocus = close) {
        JarnReadPane(Modifier.weight(1f, fill = false)) {
            JarnText(stringResource(sheet.messageRes), JarnTheme.type.body, JarnTheme.colors.textMid)
        }
        JarnDataActions {
            JarnButton(
                label = stringResource(R.string.jarnfe_data_close),
                onClick = JarnDataFlow::dismiss,
                modifier = Modifier.weight(1f).focusRequester(close),
            )
        }
    }
}

@Composable
internal fun JarnDataSheetFrame(
    title: String,
    initialFocus: FocusRequester?,
    content: @Composable ColumnScope.() -> Unit,
) {
    JarnSheet(
        visible = true,
        onDismiss = JarnDataFlow::dismiss,
        title = title,
        initialFocus = initialFocus,
        content = content,
    )
}

@Composable
internal fun JarnDataActions(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        content = content,
    )
}

/**
 * Long text a controller can read. It takes focus only when it overflows, and D-pad presses scroll
 * it until an end is reached, then move focus on as usual.
 */
@Composable
internal fun JarnReadPane(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val source = remember { MutableInteractionSource() }
    val stepPx = with(LocalDensity.current) { ReadPaneStep.toPx() }
    val canScroll = scroll.maxValue > 0
    Column(
        modifier = modifier
            .fillMaxWidth()
            .jarnFocus(JarnTheme.radius.smShape, source)
            .focusProperties { canFocus = canScroll }
            .onKeyEvent { event ->
                val delta = when (event.key) {
                    Key.DirectionDown -> stepPx.takeIf { scroll.canScrollForward }
                    Key.DirectionUp -> (-stepPx).takeIf { scroll.canScrollBackward }
                    else -> null
                } ?: return@onKeyEvent false
                if (event.type == KeyEventType.KeyDown) scope.launch { scroll.scrollBy(delta) }
                true
            }
            .focusable(canScroll, source)
            .verticalScroll(scroll)
            .padding(JarnTheme.space.focusEdge),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        content = content,
    )
}

@Composable
internal fun JarnDataProgressBar(fraction: Float?) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(JarnTheme.colors.raised, JarnTheme.radius.smShape),
    ) {
        if (fraction != null) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(JarnTheme.colors.accent, JarnTheme.radius.smShape),
            )
        }
    }
}
