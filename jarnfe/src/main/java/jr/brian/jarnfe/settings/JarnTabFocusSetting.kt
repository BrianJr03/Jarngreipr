package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.core.ui.focus.JarnTabSwitchFocus
import jr.brian.frontend.look.JarnTabFocus
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.jarnLookStoreOrNull

/** What the strips follow: which switches move focus into the new tab. */
internal fun JarnTabFocus.switchFocus(): JarnTabSwitchFocus = when (this) {
    JarnTabFocus.Shoulders -> JarnTabSwitchFocus(onShoulder = true, onDpad = false)
    JarnTabFocus.Dpad -> JarnTabSwitchFocus(onShoulder = false, onDpad = true)
    JarnTabFocus.Both -> JarnTabSwitchFocus(onShoulder = true, onDpad = true)
    JarnTabFocus.Neither -> JarnTabSwitchFocus(onShoulder = false, onDpad = false)
}

internal fun JarnTabFocus.next(): JarnTabFocus = JarnTabFocus.entries[(ordinal + 1) % JarnTabFocus.entries.size]

@get:StringRes
private val JarnTabFocus.labelRes: Int
    get() = when (this) {
        JarnTabFocus.Shoulders -> R.string.jarnfe_tab_focus_shoulders
        JarnTabFocus.Dpad -> R.string.jarnfe_tab_focus_dpad
        JarnTabFocus.Both -> R.string.jarnfe_tab_focus_both
        JarnTabFocus.Neither -> R.string.jarnfe_tab_focus_neither
    }

@get:StringRes
private val JarnTabFocus.hintRes: Int
    get() = when (this) {
        JarnTabFocus.Shoulders -> R.string.jarnfe_tab_focus_shoulders_hint
        JarnTabFocus.Dpad -> R.string.jarnfe_tab_focus_dpad_hint
        JarnTabFocus.Both -> R.string.jarnfe_tab_focus_both_hint
        JarnTabFocus.Neither -> R.string.jarnfe_tab_focus_neither_hint
    }

@Composable
internal fun JarnTabFocusGroup() {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() } ?: return
    val display by store.displayState.collectAsState()
    SettingsGroup(R.string.jarnfe_tab_focus_group) {
        JarnTabFocusRow(display.tabFocus) { store.updateDisplay { it.copy(tabFocus = it.tabFocus.next()) } }
    }
}

/** A cycles through the four choices, as other Jarn value rows do. */
@Composable
internal fun JarnTabFocusRow(choice: JarnTabFocus, onClick: () -> Unit) {
    JarnValueRow(
        label = stringResource(R.string.jarnfe_tab_focus_title),
        value = stringResource(choice.labelRes),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(choice.hintRes),
    )
}
