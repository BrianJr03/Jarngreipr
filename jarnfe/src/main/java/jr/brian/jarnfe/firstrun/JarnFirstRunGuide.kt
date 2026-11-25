package jr.brian.jarnfe.firstrun

import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnStepper
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.settings.JarnEsdeImport

@Composable
internal fun FirstRunGuide(
    run: JarnFirstRunState,
    twoScreens: Boolean,
    actions: JarnFirstRunActions,
    modifier: Modifier = Modifier,
) {
    val primaryFocus = remember { FocusRequester() }
    val leadFocus = remember { FirstRunLeadFocus() }
    val startFocus = if (run.confirmingSkip) {
        FirstRunStartFocus.Primary
    } else {
        firstRunStartFocus(run.step, leadFocus.hasLead)
    }
    LaunchedEffect(run.step, run.confirmingSkip, startFocus) {
        withFrameNanos { }
        val target = if (startFocus == FirstRunStartFocus.LeadRow) leadFocus.requester else primaryFocus
        runCatching { target.requestFocus() }
    }
    CompositionLocalProvider(LocalFirstRunLeadFocus provides leadFocus) {
        Column(
            modifier = modifier.padding(JarnTheme.space.deckMargin),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        ) {
            FirstRunProgress(run, twoScreens)
            FirstRunGuideContent(run, twoScreens, actions, Modifier.weight(1f).fillMaxWidth())
            FirstRunButtons(run, actions, primaryFocus)
            if (run.step == JarnFirstRunStep.Esde && !run.confirmingSkip) FirstRunEsdeNotNow(actions)
        }
    }
}

@Composable
private fun FirstRunProgress(run: JarnFirstRunState, twoScreens: Boolean) {
    val position = run.position(twoScreens)
    val total = run.total(twoScreens)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnStepper(position, total)
        JarnText(
            text = stringResource(R.string.jarnfe_first_run_counter, position + 1, total),
            style = JarnTheme.type.caption,
            color = JarnTheme.colors.textMid,
            maxLines = 1,
        )
    }
}

@Composable
private fun FirstRunGuideContent(
    run: JarnFirstRunState,
    twoScreens: Boolean,
    actions: JarnFirstRunActions,
    modifier: Modifier,
) {
    val scroll = rememberScrollState()
    Box(modifier) {
        JarnRevealScrollTop(scroll) {
            Crossfade(
                targetState = run.step to run.confirmingSkip,
                animationSpec = JarnTheme.motion.panel.spec(),
                label = "firstRunGuide",
            ) { (step, confirmingSkip) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusGroup()
                        .verticalScroll(scroll)
                        .padding(vertical = JarnTheme.space.xs),
                    verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
                ) {
                    if (confirmingSkip) {
                        FirstRunHeading(R.string.jarnfe_first_run_skip_title, R.string.jarnfe_first_run_skip_message)
                    } else {
                        FirstRunStepContent(step, run, twoScreens, actions)
                    }
                }
            }
        }
    }
}

@Composable
internal fun FirstRunHeading(@StringRes titleRes: Int, @StringRes bodyRes: Int) {
    JarnText(
        text = stringResource(titleRes),
        style = JarnTheme.type.heading,
        color = JarnTheme.colors.textHigh,
        maxLines = 2,
    )
    JarnText(
        text = stringResource(bodyRes),
        style = JarnTheme.type.body,
        color = JarnTheme.colors.textMid,
    )
}

@Composable
private fun FirstRunButtons(
    run: JarnFirstRunState,
    actions: JarnFirstRunActions,
    primaryFocus: FocusRequester,
) {
    Row(
        modifier = Modifier.fillMaxWidth().focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
    ) {
        if (!run.confirmingSkip) {
            FirstRunSecondaryButton(R.string.jarnfe_hint_back, actions::back)
        }
        if (run.step != JarnFirstRunStep.Done) {
            FirstRunSecondaryButton(R.string.jarnfe_first_run_skip, actions::skip)
        }
        if (run.offersLessons) {
            FirstRunSecondaryButton(R.string.jarnfe_ready_show_buttons, actions::showLessons)
        }
        if (run.step == JarnFirstRunStep.Esde && !run.confirmingSkip) {
            FirstRunEsdePrimaryButton(actions, primaryFocus, Modifier.weight(1f))
        } else {
            JarnButton(
                label = stringResource(primaryLabelRes(run)),
                onClick = actions::primary,
                modifier = Modifier.weight(1f).focusRequester(primaryFocus),
            )
        }
    }
}

@Composable
private fun FirstRunSecondaryButton(@StringRes labelRes: Int, onClick: () -> Unit) {
    JarnButton(
        label = stringResource(labelRes),
        onClick = onClick,
        variant = JarnButtonVariant.Secondary,
    )
}

@StringRes
internal fun primaryLabelRes(run: JarnFirstRunState): Int = when {
    run.confirmingSkip -> R.string.jarnfe_first_run_keep_going
    run.step == JarnFirstRunStep.Welcome -> R.string.jarnfe_first_run_start
    run.step == JarnFirstRunStep.Done -> R.string.jarnfe_first_run_finish
    run.step == JarnFirstRunStep.Esde -> esdePrimaryLabelRes(JarnEsdeImport.importer?.state)
    run.step.gesture != null && !run.practiced -> R.string.jarnfe_first_run_do_it
    else -> R.string.jarnfe_first_run_next
}
