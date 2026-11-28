package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnLoadingState
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.library.setup.EsdeSettings
import jr.brian.frontend.look.themes.EsdeThemeFolder
import jr.brian.frontend.look.themes.ThemeImportResult
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.shell.JarnNotices
import jr.brian.jarnfe.themekit.themeImportMessage
import jr.brian.jarnfe.themes.EsdeImportStep
import jr.brian.jarnfe.themes.JarnEsdeThemeUpdatePending
import jr.brian.jarnfe.themes.jarnThemeImportOrNull

/**
 * One step of Import from ES-DE, in place under its row. Only A (pick) and B (close) act here, so
 * X still switches screens and Y still opens Search. Focus lands on each step's first choice.
 */
@Composable
internal fun JarnEsdeImportSteps(
    step: EsdeImportStep,
    onPick: (EsdeThemeFolder, EsdeSettings?) -> Unit,
    onRescan: () -> Unit,
    onAllowAccess: () -> Unit,
    onUse: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(step) {
        withFrameNanos { }
        runCatching { firstFocus.requestFocus() }
    }
    JarnInputLayer(
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onClose)
        },
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
            val focus = Modifier.fillMaxWidth().focusRequester(firstFocus)
            when (step) {
                EsdeImportStep.Scanning -> JarnLoadingState(stringResource(R.string.jarnfe_esde_import_scanning))
                is EsdeImportStep.Importing ->
                    JarnLoadingState(stringResource(R.string.jarnfe_esde_import_importing, step.name))
                EsdeImportStep.NoAccess -> NoAccessStep(focus, onAllowAccess, onRescan)
                EsdeImportStep.NotFound -> MessageStep(R.string.jarnfe_esde_import_none, focus, onRescan, howTo = true)
                EsdeImportStep.Legacy -> MessageStep(R.string.jarnfe_esde_import_legacy, focus, onRescan)
                is EsdeImportStep.Failed -> MessageStep(
                    themeImportMessage(ThemeImportResult.Rejected(step.problem)) ?: R.string.jarnfe_theme_import_damaged,
                    focus,
                    onRescan,
                )
                is EsdeImportStep.Choosing -> ThemeList(step, focus, onPick)
                is EsdeImportStep.Done -> DoneStep(step, focus, onUse)
            }
        }
    }
}

@Composable
private fun ThemeList(step: EsdeImportStep.Choosing, focus: Modifier, onPick: (EsdeThemeFolder, EsdeSettings?) -> Unit) {
    step.themes.forEachIndexed { index, theme ->
        JarnValueRow(
            label = theme.name,
            value = if (theme.isCurrent) stringResource(R.string.jarnfe_esde_import_in_use) else "",
            onClick = { onPick(theme, step.settings) },
            modifier = if (index == 0) focus else Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DoneStep(step: EsdeImportStep.Done, focus: Modifier, onUse: (String) -> Unit) {
    EsdeImportReport(step.outcome)
    JarnButton(
        label = stringResource(R.string.jarnfe_esde_import_use),
        onClick = { onUse(step.outcome.themeId) },
        modifier = focus,
    )
}

@Composable
private fun NoAccessStep(focus: Modifier, onAllowAccess: () -> Unit, onRescan: () -> Unit) {
    StepText(R.string.jarnfe_esde_theme_no_access)
    StepText(R.string.jarnfe_esde_theme_howto)
    JarnNavigateRow(stringResource(R.string.jarnfe_esde_import_allow_access), onAllowAccess, focus)
    JarnNavigateRow(stringResource(R.string.jarnfe_esde_import_check_again), onRescan, Modifier.fillMaxWidth())
}

@Composable
private fun MessageStep(@StringRes messageRes: Int, focus: Modifier, onRescan: () -> Unit, howTo: Boolean = false) {
    StepText(messageRes)
    if (howTo) StepText(R.string.jarnfe_esde_theme_howto)
    JarnNavigateRow(stringResource(R.string.jarnfe_esde_import_check_again), onRescan, focus)
}

@Composable
private fun StepText(@StringRes res: Int) {
    JarnText(stringResource(res), JarnTheme.type.body, JarnTheme.colors.textMid)
}

/**
 * On launch, checks once whether a restored backup's ES-DE layout can be brought back, or else
 * whether the active theme's ES-DE folder changed since it was imported, and says where to
 * re-import. A missing folder or SD card says nothing.
 */
@Composable
internal fun JarnEsdeThemeUpdateCheck() {
    val context = LocalContext.current
    LaunchedEffect(context) {
        if (JarnEsdeThemeUpdatePending.checked) return@LaunchedEffect
        JarnEsdeThemeUpdatePending.checked = true
        val id = context.jarnLookStoreOrNull()?.activeThemeId ?: return@LaunchedEffect
        val updates = context.jarnThemeImportOrNull()?.jarnEsdeThemeUpdates ?: return@LaunchedEffect
        runCatching { updates.restoreOfferFor(id) }.getOrNull()?.let { offer ->
            JarnEsdeThemeUpdatePending.restore = offer
            JarnNotices.show(R.string.jarnfe_esde_theme_restore_notice)
            return@LaunchedEffect
        }
        runCatching { updates.updateFor(id) }.getOrNull()?.let { update ->
            JarnEsdeThemeUpdatePending.update = update
            JarnNotices.show(R.string.jarnfe_esde_theme_updated_notice)
        }
    }
}
