package jr.brian.jarnfe.utilities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnSliderRow
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.focus.JarnRevealLazyItems
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.tools.ToolsInitialFocus

private const val FLOATY_MAX_APPS = 100

/** Edits the Floaty settings Classic already stores and backs up. The effects render in Classic. */
@Composable
internal fun JarnFloatyPanel(utilities: JarnUtilities, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val settings by remember(utilities) { utilities.floaty }.collectAsState(null)
    var confirmReset by remember { mutableStateOf(false) }
    var resetPending by remember { mutableStateOf(false) }
    val resetRow = remember { FocusRequester() }
    Box(modifier.fillMaxSize()) {
        UtilityLayer(onBack = onClose, bindings = { hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm) }) {
            val current = settings
            when {
                current == null -> Unit
                !current.unlocked -> FloatyLocked()
                else -> FloatySettingsList(current, utilities, resetRow) { confirmReset = true }
            }
        }
        UtilityConfirmDialog(
            visible = confirmReset,
            title = stringResource(R.string.jarnfe_utilities_floaty_reset_title),
            message = stringResource(R.string.jarnfe_utilities_floaty_reset_message),
            confirmLabel = stringResource(R.string.jarnfe_utilities_floaty_reset),
            onConfirm = {
                confirmReset = false
                resetPending = true
                utilities.resetFloaty()
            },
            onDismiss = { confirmReset = false },
            restoreFocusTo = resetRow.takeIf { settings?.unlocked == true && !resetPending },
        )
    }
}

@Composable
private fun FloatyLocked() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        JarnEmptyState(
            title = stringResource(R.string.jarnfe_utilities_floaty_locked_title),
            message = stringResource(R.string.jarnfe_utilities_floaty_locked_message),
        )
    }
}

@Composable
private fun FloatySettingsList(
    settings: JarnFloatySettings,
    utilities: JarnUtilities,
    resetRow: FocusRequester,
    onReset: () -> Unit,
) {
    val first = remember { FocusRequester() }
    val list = rememberLazyListState()
    ToolsInitialFocus(first)
    val strings = rememberFloatyStrings()
    JarnRevealLazyItems(list) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(JarnTheme.space.lg),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        ) {
            item(key = "title") { FloatyHeading(strings.title, strings.classicNote) }
            floatyToggle("active", strings.active, settings.active, utilities::setFloatyActive, Modifier.focusRequester(first))
            floatyToggle("konfetti", strings.konfetti, settings.sectionKonfetti, utilities::setFloatySectionKonfetti)
            floatyToggle("poweredOff", strings.poweredOff, settings.poweredOffEffect, utilities::setFloatyPoweredOffEffect)
            floatyToggle("drawer", strings.drawer, settings.drawerEffect, utilities::setFloatyDrawerEffect)
            if (settings.drawerEffect) {
                item(key = "count") {
                    JarnSliderRow(
                        label = strings.drawerCount,
                        value = settings.drawerAppCount.toFloat(),
                        valueText = if (settings.drawerAppCount == 0) strings.all else settings.drawerAppCount.toString(),
                        onValueChange = { utilities.setFloatyDrawerAppCount(Math.round(it)) },
                        valueRange = 0f..FLOATY_MAX_APPS.toFloat(),
                        steps = FLOATY_MAX_APPS,
                    )
                }
            }
            floatyToggle("bubble", strings.bubble, settings.bubblePop, utilities::setFloatyBubblePop)
            if (settings.active && settings.tabCount > 0) floatyTabs(settings, strings, utilities)
            item(key = "reset") {
                JarnNavigateRow(
                    label = strings.reset,
                    supporting = strings.resetSupporting,
                    onClick = onReset,
                    modifier = Modifier.focusRequester(resetRow),
                )
            }
        }
    }
}

private fun LazyListScope.floatyToggle(
    key: String,
    copy: FloatyRowCopy,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    item(key = key) {
        JarnToggleRow(
            label = copy.label,
            supporting = copy.supporting,
            checked = checked,
            onCheckedChange = onChange,
            modifier = modifier,
        )
    }
}

private fun LazyListScope.floatyTabs(
    settings: JarnFloatySettings,
    strings: FloatyStrings,
    utilities: JarnUtilities,
) {
    item(key = "tabsTitle") { FloatyHeading(strings.tabs, strings.tabsSupporting) }
    items(settings.tabCount, key = { "tab$it" }) { index ->
        JarnToggleRow(
            label = stringResource(R.string.jarnfe_utilities_floaty_tab, index + 1),
            checked = settings.isTabEnabled(index),
            onCheckedChange = { utilities.setFloatyTab(index, it) },
        )
    }
}

@Composable
private fun FloatyHeading(title: String, note: String) {
    Column(verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs)) {
        UtilityTitle(title)
        UtilityNote(note)
    }
}

private data class FloatyRowCopy(val label: String, val supporting: String)

private data class FloatyStrings(
    val title: String,
    val classicNote: String,
    val active: FloatyRowCopy,
    val konfetti: FloatyRowCopy,
    val poweredOff: FloatyRowCopy,
    val drawer: FloatyRowCopy,
    val bubble: FloatyRowCopy,
    val drawerCount: String,
    val all: String,
    val tabs: String,
    val tabsSupporting: String,
    val reset: String,
    val resetSupporting: String,
)

@Composable
private fun rememberFloatyStrings() = FloatyStrings(
    title = stringResource(R.string.jarnfe_utilities_floaty),
    classicNote = stringResource(R.string.jarnfe_utilities_floaty_classic_note),
    active = FloatyRowCopy(
        stringResource(R.string.jarnfe_utilities_floaty_active),
        stringResource(R.string.jarnfe_utilities_floaty_active_supporting),
    ),
    konfetti = FloatyRowCopy(
        stringResource(R.string.jarnfe_utilities_floaty_konfetti),
        stringResource(R.string.jarnfe_utilities_floaty_konfetti_supporting),
    ),
    poweredOff = FloatyRowCopy(
        stringResource(R.string.jarnfe_utilities_floaty_powered_off),
        stringResource(R.string.jarnfe_utilities_floaty_powered_off_supporting),
    ),
    drawer = FloatyRowCopy(
        stringResource(R.string.jarnfe_utilities_floaty_drawer),
        stringResource(R.string.jarnfe_utilities_floaty_drawer_supporting),
    ),
    bubble = FloatyRowCopy(
        stringResource(R.string.jarnfe_utilities_floaty_bubble),
        stringResource(R.string.jarnfe_utilities_floaty_bubble_supporting),
    ),
    drawerCount = stringResource(R.string.jarnfe_utilities_floaty_count),
    all = stringResource(R.string.jarnfe_utilities_floaty_count_all),
    tabs = stringResource(R.string.jarnfe_utilities_floaty_tabs),
    tabsSupporting = stringResource(R.string.jarnfe_utilities_floaty_tabs_supporting),
    reset = stringResource(R.string.jarnfe_utilities_floaty_reset),
    resetSupporting = stringResource(R.string.jarnfe_utilities_floaty_reset_supporting),
)
