package jr.brian.jarnfe.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.session.JarnSession

private enum class JarnToolPage { Menu, Trackpad, BackShortcuts }

/** The entry point for the input tools, ready to host from Settings. */
@Composable
internal fun JarnInputToolsPanel(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tools = remember(context) { context.jarnInputToolsOrNull() }
    var page by remember { mutableStateOf(JarnToolPage.Menu) }
    when {
        tools == null -> ToolsUnavailable(onClose, modifier)
        page == JarnToolPage.Trackpad -> JarnTrackpadPanel(tools, { page = JarnToolPage.Menu }, modifier)
        page == JarnToolPage.BackShortcuts -> JarnBackShortcutsPanel(tools, { page = JarnToolPage.Menu }, modifier)
        else -> ToolsMenu(onOpen = { page = it }, onClose = onClose, modifier = modifier)
    }
}

@Composable
private fun ToolsMenu(
    onOpen: (JarnToolPage) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val first = remember { FocusRequester() }
    ToolsInitialFocus(first)
    ToolsLayer(onClose) {
        Column(
            modifier = modifier.fillMaxSize().padding(JarnTheme.space.lg),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        ) {
            JarnText(
                text = stringResource(R.string.jarnfe_tools_title),
                style = JarnTheme.type.label,
                color = JarnTheme.colors.textHigh,
            )
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_tools_trackpad),
                supporting = stringResource(R.string.jarnfe_tools_trackpad_supporting),
                onClick = { onOpen(JarnToolPage.Trackpad) },
                modifier = Modifier.focusRequester(first),
            )
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_tools_back_shortcuts),
                supporting = stringResource(R.string.jarnfe_tools_back_supporting),
                onClick = { onOpen(JarnToolPage.BackShortcuts) },
            )
        }
    }
}

@Composable
private fun ToolsUnavailable(onClose: () -> Unit, modifier: Modifier = Modifier) {
    ToolsLayer(onClose) {
        JarnText(
            text = stringResource(R.string.jarnfe_tools_unavailable),
            style = JarnTheme.type.body,
            color = JarnTheme.colors.textMid,
            modifier = modifier.padding(JarnTheme.space.lg),
        )
    }
}

@Composable
private fun ToolsLayer(onClose: () -> Unit, content: @Composable () -> Unit) {
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onClose)
            on(JarnAction.SwitchScreen, R.string.jarnfe_hint_switch_screen) {
                JarnSession.update { switchActiveScreen() }
            }
        },
        content = content,
    )
}
