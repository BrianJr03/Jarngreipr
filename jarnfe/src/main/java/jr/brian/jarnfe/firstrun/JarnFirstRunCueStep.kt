package jr.brian.jarnfe.firstrun

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnChoiceRow
import jr.brian.core.ui.components.JarnSwitchMarkLine
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnFocusStyle
import jr.brian.core.ui.focus.jarnFocus
import jr.brian.core.ui.theme.JarnSwitchCue
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.JarnSwitchCuePreview
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.settings.cueHintRes
import jr.brian.jarnfe.settings.cueLabelRes
import jr.brian.jarnfe.shell.JarnWordmark
import jr.brian.jarnfe.transition.LocalJarnMarkHandoff

internal fun nextPreviewCue(current: JarnSwitchCue?, cue: JarnSwitchCue, isFocused: Boolean): JarnSwitchCue? = when {
    isFocused -> cue
    current == cue -> null
    else -> current
}

@Composable
internal fun FirstRunSwitchCueStep(actions: JarnFirstRunActions) {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() }
    FirstRunHeading(R.string.jarnfe_first_run_cue_title, R.string.jarnfe_first_run_cue_body)
    if (store == null) return
    val display by store.displayState.collectAsState()
    DisposableEffect(Unit) { onDispose { JarnSwitchCuePreview.cue = null } }
    Column(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
    ) {
        JarnSwitchCue.entries.forEach { cue ->
            FirstRunCueOption(cue, chosen = cue == display.switchCue) {
                store.updateDisplay { it.copy(switchCue = cue) }
            }
        }
    }
    JarnButton(
        label = stringResource(R.string.jarnfe_hint_switch_screen),
        onClick = actions::switchScreen,
        variant = JarnButtonVariant.Secondary,
    )
    FirstRunCueSample(takesFocus = false)
}

@Composable
private fun FirstRunCueOption(cue: JarnSwitchCue, chosen: Boolean, onChoose: () -> Unit) {
    JarnChoiceRow(
        label = stringResource(cue.cueLabelRes),
        chosen = chosen,
        onClick = onChoose,
        modifier = Modifier.onFocusChanged { state ->
            JarnSwitchCuePreview.cue = nextPreviewCue(JarnSwitchCuePreview.cue, cue, state.isFocused)
        },
        supporting = stringResource(cue.cueHintRes),
    )
}

/** Gives each screen something real to pulse, mark and label, since the tour hides the normal headers. */
@Composable
internal fun FirstRunCueSample(takesFocus: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    val requester = remember { FocusRequester() }
    if (takesFocus) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            runCatching { requester.requestFocus() }
        }
    }
    val shape = JarnTheme.radius.smShape
    val focus = if (takesFocus) {
        Modifier
            .focusRequester(requester)
            .jarnFocus(shape, source, style = JarnFocusStyle.Scale)
            .focusable(interactionSource = source)
    } else {
        Modifier
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (LocalJarnMarkHandoff.current.follows) JarnWordmark()
        Box(focus.background(JarnTheme.colors.raised, shape)) {
            JarnText(
                text = stringResource(R.string.jarnfe_section_systems),
                modifier = Modifier.padding(horizontal = JarnTheme.space.md, vertical = JarnTheme.space.xs),
                style = JarnTheme.type.labelStrong,
                color = JarnTheme.colors.textHigh,
                maxLines = 1,
            )
            JarnSwitchMarkLine()
        }
    }
}
