package jr.brian.jarnfe.firstrun

import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnChip
import jr.brian.core.ui.components.JarnKey
import jr.brian.core.ui.components.JarnKeyCap
import jr.brian.core.ui.components.JarnScrim
import jr.brian.core.ui.components.JarnScrimEdge
import jr.brian.core.ui.components.JarnSurface
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.JarnScreenHintBar
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.LibraryArt
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.shell.JarnNoticeHost
import jr.brian.jarnfe.splash.JarnSplash
import kotlinx.coroutines.delay

private const val PRACTICE_PAUSE_MS = 650L

/** Longer than the Deck launch timeout, so a slow start or quick lid close keeps the saved step. */
private const val DECK_RETURN_GRACE_MS = 6_000L
private const val ART_WALL_TILES = 6
private val SideBySideMinWidth = 600.dp

@Composable
internal fun JarnFirstRunScreen(
    screen: JarnScreen,
    session: JarnSessionState,
    controller: JarnFirstRunController,
) {
    val run = controller.state ?: return
    val twoScreens = session.settingsOnDeck
    val actions = remember(controller) { JarnFirstRunActions(controller, JarnSession) }
    val hasGuide = screen == JarnScreen.Deck || !twoScreens
    if (screen == JarnScreen.Stage) FirstRunStageEffects(run, twoScreens, controller, actions)
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = { firstRunBindings(run, actions, confirmHere = !hasGuide) },
    ) {
        Box(Modifier.fillMaxSize().background(JarnTheme.colors.bg)) {
            Column(Modifier.fillMaxSize()) {
                FirstRunBody(screen, run, twoScreens, actions, Modifier.weight(1f).fillMaxWidth())
                JarnScreenHintBar()
            }
            if (screen == JarnScreen.Stage) JarnNoticeHost()
        }
    }
}

@Composable
private fun FirstRunStageEffects(
    run: JarnFirstRunState,
    twoScreens: Boolean,
    controller: JarnFirstRunController,
    actions: JarnFirstRunActions,
) {
    LaunchedEffect(Unit) { JarnSplash.finish() }
    LaunchedEffect(JarnLibrary.source) { JarnLibrary.observe() }
    LaunchedEffect(twoScreens) {
        if (!twoScreens) {
            delay(DECK_RETURN_GRACE_MS)
            controller.settle(twoScreens = false)
        }
    }
    LaunchedEffect(run.step, twoScreens) {
        if (twoScreens) JarnSession.update { activate(JarnScreen.Deck) }
    }
    LaunchedEffect(run.step, run.practiced) {
        if (run.practiced) {
            delay(PRACTICE_PAUSE_MS)
            actions.advanceFrom(run.step)
        }
    }
}

private fun JarnBindingsBuilder.firstRunBindings(
    run: JarnFirstRunState,
    actions: JarnFirstRunActions,
    confirmHere: Boolean,
) {
    val confirmLabel = primaryLabelRes(run)
    if (confirmHere) {
        on(JarnAction.Confirm, confirmLabel, onPress = actions::primary)
    } else {
        hint(JarnAction.Confirm, confirmLabel)
    }
    val backPractices = run.step == JarnFirstRunStep.PressB && !run.confirmingSkip && !run.practiced
    on(JarnAction.Back, R.string.jarnfe_hint_back) {
        if (backPractices) actions.perform(JarnFirstRunGesture.Back) else actions.back()
    }
    on(JarnAction.Search, R.string.jarnfe_first_run_skip, onPress = actions::skip)
    if (!run.confirmingSkip) gestureBindings(run.step, actions)
    if (run.confirmingSkip || run.step.gesture != JarnFirstRunGesture.SwitchScreen) {
        val label = R.string.jarnfe_hint_switch_screen.takeIf { confirmHere || run.step == JarnFirstRunStep.SwitchCue }
        on(JarnAction.SwitchScreen, label, onPress = actions::switchScreen)
    }
}

private fun JarnBindingsBuilder.gestureBindings(step: JarnFirstRunStep, actions: JarnFirstRunActions) {
    when (step.gesture) {
        JarnFirstRunGesture.Context -> on(JarnAction.Context, R.string.jarnfe_hint_context) {
            actions.perform(JarnFirstRunGesture.Context)
        }
        JarnFirstRunGesture.SwitchScreen -> on(JarnAction.SwitchScreen, R.string.jarnfe_hint_switch_screen) {
            actions.perform(JarnFirstRunGesture.SwitchScreen)
        }
        JarnFirstRunGesture.Tabs -> {
            on(JarnAction.TabPrevious) { actions.perform(JarnFirstRunGesture.Tabs, tabDelta = -1) }
            on(JarnAction.TabNext) { actions.perform(JarnFirstRunGesture.Tabs, tabDelta = 1) }
        }
        JarnFirstRunGesture.OtherTabs -> {
            on(JarnAction.BottomTabPrevious) { actions.perform(JarnFirstRunGesture.OtherTabs, tabDelta = -1) }
            on(JarnAction.BottomTabNext) { actions.perform(JarnFirstRunGesture.OtherTabs, tabDelta = 1) }
        }
        JarnFirstRunGesture.Back, null -> Unit
    }
}

@Composable
private fun FirstRunBody(
    screen: JarnScreen,
    run: JarnFirstRunState,
    twoScreens: Boolean,
    actions: JarnFirstRunActions,
    modifier: Modifier,
) {
    when {
        screen == JarnScreen.Deck -> FirstRunGuide(run, twoScreens = true, actions, modifier)
        twoScreens -> FirstRunStageShowcase(run, modifier)
        else -> FirstRunSingleScreen(run, actions, modifier)
    }
}

@Composable
private fun FirstRunSingleScreen(run: JarnFirstRunState, actions: JarnFirstRunActions, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        if (maxWidth < SideBySideMinWidth) {
            FirstRunGuide(run, twoScreens = false, actions, Modifier.fillMaxSize())
        } else {
            Row(Modifier.fillMaxSize()) {
                FirstRunStageShowcase(run, Modifier.weight(1f).fillMaxHeight())
                FirstRunGuide(run, twoScreens = false, actions, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun FirstRunStageShowcase(run: JarnFirstRunState, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        FirstRunArtWall()
        JarnScrim(JarnScrimEdge.Full, Modifier.fillMaxSize())
        Crossfade(run.step, animationSpec = JarnTheme.motion.panel.spec(), label = "firstRunStage") { step ->
            JarnSurface(Modifier.padding(JarnTheme.space.lg)) {
                FirstRunStageCaption(step, run, JarnSession.state.practiceTabsStepOn(JarnScreen.Stage))
            }
        }
    }
}

/** Decoration only, so systems without art are left out rather than showing their names. */
@Composable
private fun FirstRunArtWall() {
    val systems = JarnLibrary.state.systems.items
        .filter { !it.artPath.isNullOrBlank() }
        .take(ART_WALL_TILES)
    if (systems.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        systems.forEach { system ->
            Box(Modifier.weight(1f).fillMaxHeight().clip(JarnTheme.radius.smShape)) {
                LibraryArt(system.artPath, title = "")
            }
        }
    }
}

@Composable
private fun FirstRunStageCaption(step: JarnFirstRunStep, run: JarnFirstRunState, practiceStep: JarnFirstRunStep?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(JarnTheme.space.stageMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
    ) {
        JarnText(
            text = stringResource(stageTitleRes(step, practiceStep)),
            style = JarnTheme.type.title,
            color = JarnTheme.colors.textHigh,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        JarnText(
            text = stringResource(R.string.jarnfe_first_run_stage_cue),
            style = JarnTheme.type.body,
            color = JarnTheme.colors.textMid,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        if (step == practiceStep) FirstRunPracticeTabs(run.practiceTab)
        if (step == JarnFirstRunStep.SwitchCue) FirstRunCueSample()
    }
}

@StringRes
private fun stageTitleRes(step: JarnFirstRunStep, practiceStep: JarnFirstRunStep?): Int = when (step) {
    practiceStep -> R.string.jarnfe_first_run_stage_other_tabs
    JarnFirstRunStep.Welcome -> R.string.jarnfe_first_run_stage_welcome
    JarnFirstRunStep.Library -> R.string.jarnfe_first_run_library_title
    JarnFirstRunStep.Media -> R.string.jarnfe_first_run_media_title
    JarnFirstRunStep.Look -> R.string.jarnfe_first_run_look_title
    JarnFirstRunStep.SwitchCue -> R.string.jarnfe_first_run_cue_title
    JarnFirstRunStep.PressSelect -> R.string.jarnfe_first_run_stage_select
    JarnFirstRunStep.Done -> R.string.jarnfe_first_run_done_title
    else -> R.string.jarnfe_first_run_stage_practice
}

@Composable
internal fun FirstRunPracticeTabs(selected: Int) {
    val labels = listOf(
        R.string.jarnfe_section_systems,
        R.string.jarnfe_section_favorites,
        R.string.jarnfe_section_recent,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
        labels.forEachIndexed { index, label ->
            JarnChip(stringResource(label), selected = index == selected)
        }
    }
}

@Composable
internal fun FirstRunKeys(keys: List<JarnKey>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        keys.forEach { JarnKeyCap(it) }
    }
}
