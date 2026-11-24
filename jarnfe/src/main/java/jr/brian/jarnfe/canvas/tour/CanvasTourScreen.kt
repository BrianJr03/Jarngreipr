package jr.brian.jarnfe.canvas.tour

import androidx.annotation.StringRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnKey
import jr.brian.core.ui.components.JarnKeyCap
import jr.brian.core.ui.components.JarnStepper
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.session.JarnSession
import kotlinx.coroutines.delay

private const val PRACTICE_PAUSE_MS = 900L
private val SideBySideMinWidth = 560.dp

@Composable
internal fun CanvasTourScreen(tour: CanvasTourController) {
    val run = tour.state ?: return
    LaunchedEffect(run.step, run.practiced) {
        if (run.practiced) {
            delay(PRACTICE_PAUSE_MS)
            tour.advanceFrom(run.step)
        }
    }
    val twoScreens = JarnSession.state.deckDisplayAvailable
    JarnInputLayer(mode = JarnLayerMode.Capture, bindings = { canvasTourBindings(run, tour) }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(JarnTheme.colors.bg)
                .padding(JarnTheme.space.deckMargin),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        ) {
            if (twoScreens) {
                CanvasTourDeckBoard(run, Modifier.weight(1f).fillMaxWidth())
            } else {
                CanvasTourProgress(run)
                CanvasTourBody(run, Modifier.weight(1f).fillMaxWidth())
            }
            CanvasTourButtons(run, tour)
        }
    }
}

@Composable
private fun CanvasTourDeckBoard(run: CanvasTourState, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
        JarnText(
            text = stringResource(lessonFor(run.step).titleRes),
            style = JarnTheme.type.heading,
            color = JarnTheme.colors.textHigh,
            maxLines = 1,
        )
        CanvasTourBoard(run, Modifier.weight(1f).fillMaxWidth())
    }
}

private fun JarnBindingsBuilder.canvasTourBindings(run: CanvasTourState, tour: CanvasTourController) {
    on(JarnAction.Confirm, primaryLabelRes(run), onPress = tour::primary)
    on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = tour::back)
    on(JarnAction.Context) { tour.press(CanvasTourInput.Context) }
    on(JarnAction.Menu) { tour.press(CanvasTourInput.Menu) }
    on(JarnAction.Search) { tour.press(CanvasTourInput.Undo) }
    on(JarnAction.TabPrevious) { tour.press(CanvasTourInput.Shrink) }
    on(JarnAction.TabNext) { tour.press(CanvasTourInput.Grow) }
    on(JarnAction.OppositeTabPrevious, onPress = tour::finish)
    on(JarnAction.OppositeTabNext, onPress = tour::doItForMe)
    on(JarnAction.SwitchScreen) { JarnSession.update { switchActiveScreen() } }
    direction(JarnAction.MoveUp, CanvasTourInput.Up, tour)
    direction(JarnAction.MoveDown, CanvasTourInput.Down, tour)
    direction(JarnAction.MoveLeft, CanvasTourInput.Left, tour)
    direction(JarnAction.MoveRight, CanvasTourInput.Right, tour)
}

private fun JarnBindingsBuilder.direction(
    action: JarnAction,
    input: CanvasTourInput,
    tour: CanvasTourController,
) {
    on(action, onRepeat = { tour.press(input, repeat = true) }) { tour.press(input) }
}

@StringRes
private fun primaryLabelRes(run: CanvasTourState): Int? = when {
    run.step == CanvasTourStep.Intro -> R.string.jarnfe_canvas_tour_start
    run.step == CanvasTourStep.Done -> R.string.jarnfe_canvas_tour_finish
    else -> null
}

@Composable
internal fun CanvasTourProgress(run: CanvasTourState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnStepper(run.position, run.total)
        JarnText(
            text = stringResource(R.string.jarnfe_first_run_counter, run.position + 1, run.total),
            style = JarnTheme.type.caption,
            color = JarnTheme.colors.textMid,
            maxLines = 1,
        )
    }
}

@Composable
private fun CanvasTourBody(run: CanvasTourState, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        if (maxWidth < SideBySideMinWidth) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
            ) {
                CanvasTourLesson(run, Modifier.fillMaxWidth().weight(1f))
                CanvasTourBoard(run, Modifier.fillMaxWidth().weight(1f))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.lg),
            ) {
                CanvasTourLesson(run, Modifier.weight(1f).fillMaxHeight())
                CanvasTourBoard(run, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun CanvasTourLesson(run: CanvasTourState, modifier: Modifier) {
    val lesson = lessonFor(run.step)
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
    ) {
        JarnText(
            text = stringResource(lesson.titleRes),
            style = JarnTheme.type.heading,
            color = JarnTheme.colors.textHigh,
            maxLines = 2,
        )
        JarnText(stringResource(lesson.bodyRes), JarnTheme.type.body, JarnTheme.colors.textMid)
        if (lesson.keys.isNotEmpty()) CanvasTourKeys(lesson.keys)
    }
}

@Composable
internal fun CanvasTourKeys(keys: List<JarnKey>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        keys.forEach { JarnKeyCap(it) }
    }
}

@Composable
private fun CanvasTourButtons(run: CanvasTourState, tour: CanvasTourController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (run.step != CanvasTourStep.Done) {
            val skipLabel = if (run.step == CanvasTourStep.Intro) {
                R.string.jarnfe_canvas_tour_not_now
            } else {
                R.string.jarnfe_canvas_tour_skip
            }
            CanvasTourButton(JarnKey.L2, skipLabel, JarnButtonVariant.Secondary, tour::finish)
        }
        Box(Modifier.weight(1f))
        when {
            run.step.isPractice && !run.practiced ->
                CanvasTourButton(JarnKey.R2, R.string.jarnfe_first_run_do_it, JarnButtonVariant.Primary, tour::doItForMe)
            !run.step.isPractice -> CanvasTourButton(
                key = JarnKey.A,
                labelRes = primaryLabelRes(run) ?: R.string.jarnfe_first_run_next,
                variant = JarnButtonVariant.Primary,
                onClick = tour::primary,
            )
        }
    }
}

/** Never takes focus, because the tour owns every button press for its practice board. */
@Composable
private fun CanvasTourButton(
    key: JarnKey,
    @StringRes labelRes: Int,
    variant: JarnButtonVariant,
    onClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnKeyCap(key)
        JarnButton(
            label = stringResource(labelRes),
            onClick = onClick,
            variant = variant,
            modifier = Modifier.focusProperties { canFocus = false },
        )
    }
}

internal class CanvasTourLessonCopy(
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
    val keys: List<JarnKey> = emptyList(),
)

internal fun lessonFor(step: CanvasTourStep): CanvasTourLessonCopy = when (step) {
    CanvasTourStep.Intro -> CanvasTourLessonCopy(
        R.string.jarnfe_canvas_tour_intro_title,
        R.string.jarnfe_canvas_tour_intro_body,
    )
    CanvasTourStep.Launch -> CanvasTourLessonCopy(
        R.string.jarnfe_canvas_tour_launch_title,
        R.string.jarnfe_canvas_tour_launch_body,
        listOf(JarnKey.A),
    )
    CanvasTourStep.TileOptions -> CanvasTourLessonCopy(
        R.string.jarnfe_canvas_tour_options_title,
        R.string.jarnfe_canvas_tour_options_body,
        listOf(JarnKey.X),
    )
    CanvasTourStep.OneShotMove -> CanvasTourLessonCopy(
        R.string.jarnfe_canvas_tour_move_title,
        R.string.jarnfe_canvas_tour_move_body,
        listOf(JarnKey.A, JarnKey.Dpad, JarnKey.A),
    )
    CanvasTourStep.Arrange -> CanvasTourLessonCopy(
        R.string.jarnfe_canvas_tour_arrange_title,
        R.string.jarnfe_canvas_tour_arrange_body,
        listOf(JarnKey.Start, JarnKey.A),
    )
    CanvasTourStep.Resize -> CanvasTourLessonCopy(
        R.string.jarnfe_canvas_tour_resize_title,
        R.string.jarnfe_canvas_tour_resize_body,
        listOf(JarnKey.A, JarnKey.L1, JarnKey.R1),
    )
    CanvasTourStep.PageEdge -> CanvasTourLessonCopy(
        R.string.jarnfe_canvas_tour_page_title,
        R.string.jarnfe_canvas_tour_page_body,
        listOf(JarnKey.Dpad),
    )
    CanvasTourStep.Undo -> CanvasTourLessonCopy(
        R.string.jarnfe_canvas_tour_undo_title,
        R.string.jarnfe_canvas_tour_undo_body,
        listOf(JarnKey.A, JarnKey.Y),
    )
    CanvasTourStep.Done -> CanvasTourLessonCopy(
        R.string.jarnfe_canvas_tour_done_title,
        R.string.jarnfe_canvas_tour_done_body,
    )
}
