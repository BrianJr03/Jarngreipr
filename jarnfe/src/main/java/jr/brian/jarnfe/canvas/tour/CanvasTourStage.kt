package jr.brian.jarnfe.canvas.tour

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.session.DeckTab
import jr.brian.jarnfe.session.JarnSessionState

private val LessonMaxWidth = 860.dp

/** With two screens, the lesson reads on Stage while the Deck stays free for the practice board. */
@Composable
internal fun CanvasTourStagePreview(state: JarnSessionState) {
    if (!state.deckDisplayAvailable || state.deckTab != DeckTab.Canvas) return
    val run = JarnCanvasTour.controller?.state
    val lastShown = remember { arrayOfNulls<CanvasTourState>(1) }
    if (run != null) lastShown[0] = run
    val motion = JarnTheme.motion
    AnimatedVisibility(
        visible = run != null,
        enter = fadeIn(motion.panel.spec()),
        exit = fadeOut(motion.panel.spec()),
    ) {
        lastShown[0]?.let { CanvasTourStageLesson(it) }
    }
}

@Composable
private fun CanvasTourStageLesson(run: CanvasTourState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarnTheme.colors.bg)
            .padding(JarnTheme.space.stageMargin),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xl),
    ) {
        CanvasTourProgress(run)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Crossfade(run.step, animationSpec = JarnTheme.motion.panel.spec(), label = "canvasTourLesson") { step ->
                CanvasTourStageCopy(step)
            }
        }
        CanvasTourStageFooter(run)
    }
}

@Composable
private fun CanvasTourStageCopy(step: CanvasTourStep) {
    val lesson = lessonFor(step)
    Column(
        modifier = Modifier.widthIn(max = LessonMaxWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.lg),
    ) {
        JarnText(
            text = stringResource(lesson.titleRes),
            style = JarnTheme.type.title,
            color = JarnTheme.colors.textHigh,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
        JarnText(
            text = stringResource(lesson.bodyRes),
            style = JarnTheme.type.body,
            color = JarnTheme.colors.textMid,
            textAlign = TextAlign.Center,
        )
        if (lesson.keys.isNotEmpty()) CanvasTourKeys(lesson.keys)
    }
}

/** Always takes its height, so the lesson doesn't shift when the praise or tile size appears. */
@Composable
private fun CanvasTourStageFooter(run: CanvasTourState) {
    val holding = run.mode == CanvasTourMode.Holding
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        JarnText(
            text = stringResource(R.string.jarnfe_first_run_nice),
            style = JarnTheme.type.heading,
            color = JarnTheme.colors.success,
            maxLines = 1,
            modifier = Modifier.alpha(if (run.practiced) 1f else 0f),
        )
        if (holding && !run.practiced) {
            JarnText(
                text = stringResource(R.string.jarnfe_canvas_stage_size, run.game.rect.colSpan, run.game.rect.rowSpan),
                style = JarnTheme.type.heading,
                color = JarnTheme.colors.textHigh,
                maxLines = 1,
            )
        }
    }
}
