package jr.brian.jarnfe.utilities

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.tools.ToolsInitialFocus

internal enum class JarnUtilityPage(
    @param:StringRes val titleRes: Int,
    @param:StringRes val supportingRes: Int,
) {
    Monitor(R.string.jarnfe_utilities_monitor, R.string.jarnfe_utilities_monitor_supporting),
    CrashLogs(R.string.jarnfe_utilities_crashes, R.string.jarnfe_utilities_crashes_supporting),
    RecentApps(R.string.jarnfe_utilities_recents, R.string.jarnfe_utilities_recents_supporting),
    Faq(R.string.jarnfe_utilities_faq, R.string.jarnfe_utilities_faq_supporting),
    Volume(R.string.jarnfe_utilities_volume, R.string.jarnfe_utilities_volume_supporting),
    Floaty(R.string.jarnfe_utilities_floaty, R.string.jarnfe_utilities_floaty_supporting),
}

/** The entry point for the utility screens, ready to host from Settings. */
@Composable
internal fun JarnUtilitiesPanel(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val utilities = remember(context) { context.jarnUtilitiesOrNull() }
    var pageName by rememberSaveable { mutableStateOf<String?>(null) }
    var lastPageName by rememberSaveable { mutableStateOf(JarnUtilityPage.entries.first().name) }
    val page = pageName?.let(JarnUtilityPage::valueOf)
    val closePage = { pageName = null }
    when {
        utilities == null -> UtilitiesUnavailable(onClose, modifier)
        page == null -> UtilitiesMenu(
            focused = JarnUtilityPage.valueOf(lastPageName),
            onOpen = {
                lastPageName = it.name
                pageName = it.name
            },
            onClose = onClose,
            modifier = modifier,
        )
        page == JarnUtilityPage.Monitor -> JarnMonitorPanel(utilities, closePage, modifier)
        page == JarnUtilityPage.CrashLogs -> JarnCrashLogsPanel(utilities, closePage, modifier)
        page == JarnUtilityPage.RecentApps -> JarnRecentAppsPanel(utilities, closePage, modifier)
        page == JarnUtilityPage.Faq -> JarnFaqPanel(utilities, closePage, modifier)
        page == JarnUtilityPage.Volume -> JarnVolumePanel(utilities, closePage, modifier)
        else -> JarnFloatyPanel(utilities, closePage, modifier)
    }
}

@Composable
private fun UtilitiesMenu(
    focused: JarnUtilityPage,
    onOpen: (JarnUtilityPage) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val target = remember { FocusRequester() }
    val scroll = rememberScrollState()
    ToolsInitialFocus(target)
    UtilityLayer(onBack = onClose, bindings = { hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm) }) {
        JarnRevealScrollTop(scroll) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(JarnTheme.space.lg),
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
            ) {
                UtilityTitle(stringResource(R.string.jarnfe_utilities_title))
                JarnUtilityPage.entries.forEach { entry ->
                    JarnNavigateRow(
                        label = stringResource(entry.titleRes),
                        supporting = stringResource(entry.supportingRes),
                        onClick = { onOpen(entry) },
                        modifier = if (entry == focused) Modifier.focusRequester(target) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun UtilitiesUnavailable(onClose: () -> Unit, modifier: Modifier = Modifier) {
    UtilityLayer(onBack = onClose) {
        UtilityNote(
            text = stringResource(R.string.jarnfe_utilities_unavailable),
            modifier = modifier.padding(JarnTheme.space.lg),
        )
    }
}
