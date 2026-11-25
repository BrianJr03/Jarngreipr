package jr.brian.jarnfe.firstrun

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.JarnLibrary

private const val ROW_STAGGER_MS = 80
private const val ROW_FADE_MS = 160
private const val REDUCED_FADE_MS = 120
private val CheckSize = 20.dp

/** The fade for the row at [index]: staggered normally, one plain fade together with reduced motion. */
internal data class ReadyRowFade(val delayMillis: Int, val durationMillis: Int)

internal fun readyRowFade(index: Int, reduced: Boolean): ReadyRowFade =
    if (reduced) ReadyRowFade(0, REDUCED_FADE_MS) else ReadyRowFade(index * ROW_STAGGER_MS, ROW_FADE_MS)

internal enum class FirstRunStartFocus { Primary, LeadRow }

/** Ready starts on Next, or on the games folder row when there are no games yet. */
internal fun firstRunStartFocus(step: JarnFirstRunStep, hasLeadRow: Boolean): FirstRunStartFocus =
    if (step == JarnFirstRunStep.Ready && hasLeadRow) FirstRunStartFocus.LeadRow else FirstRunStartFocus.Primary

/** Lets the Ready card hand its leading row to the guide's first focus. */
internal class FirstRunLeadFocus {
    val requester = FocusRequester()
    var hasLead by mutableStateOf(false)
}

internal val LocalFirstRunLeadFocus = staticCompositionLocalOf<FirstRunLeadFocus?> { null }

@Composable
internal fun FirstRunReadyStep(twoScreens: Boolean, actions: JarnFirstRunActions) {
    val hasGames = JarnLibrary.state.systems.items.any { it.gameCount > 0 }
    val card = remember(hasGames, actions) { JarnReadyCard(hasGames, actions::skipToLook) }
    val rows = rememberReadyRows(card)
    val lead = readyLeadRow(rows)
    val leadFocus = LocalFirstRunLeadFocus.current
    SideEffect { leadFocus?.hasLead = lead != null }
    FirstRunHeading(
        R.string.jarnfe_ready_title,
        if (twoScreens) R.string.jarnfe_ready_screens else R.string.jarnfe_ready_screens_single,
    )
    Column(verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
        rows.forEachIndexed { index, row ->
            key(row.key) {
                FirstRunReadyRowItem(
                    row = row,
                    fade = readyRowFade(index, JarnTheme.motion.reduced),
                    focus = leadFocus?.requester?.takeIf { row === lead } ?: row.focus,
                )
            }
        }
    }
    FirstRunReadyEsdeSummary()
    FirstRunSwitchBackNote()
}

@Composable
internal fun FirstRunSwitchBackNote() {
    JarnText(stringResource(R.string.jarnfe_ready_switch_back), JarnTheme.type.caption, JarnTheme.colors.textMid)
}

@Composable
private fun FirstRunReadyRowItem(row: JarnReadyRow, fade: ReadyRowFade, focus: FocusRequester?) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(fade.durationMillis, fade.delayMillis, LinearEasing))
    }
    Column(
        modifier = Modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha.value },
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (row.isDone || !row.needsTap) FirstRunReadyCheck(row.isDone)
            JarnText(row.text, JarnTheme.type.labelStrong, JarnTheme.colors.textHigh, modifier = Modifier.weight(1f))
            if (row.needsTap) {
                JarnButton(
                    label = row.actionLabel.orEmpty(),
                    onClick = { row.onAction?.invoke() },
                    variant = JarnButtonVariant.Secondary,
                    modifier = focus?.let { Modifier.focusRequester(it) } ?: Modifier,
                )
            }
        }
        if (row.linkLabel != null && row.onLink != null && !row.isDone) {
            JarnButton(
                label = row.linkLabel,
                onClick = row.onLink,
                variant = JarnButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun FirstRunReadyCheck(done: Boolean) {
    val colors = JarnTheme.colors
    Image(
        painter = painterResource(R.drawable.ic_jarn_check),
        contentDescription = stringResource(R.string.jarnfe_ready_done).takeIf { done },
        modifier = Modifier.size(CheckSize),
        colorFilter = ColorFilter.tint(if (done) colors.success else colors.textMid),
    )
}
