package jr.brian.jarnfe.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R

/** Tints the header row in place, so entering Arrange never changes the grid's size. */
internal fun Modifier.canvasArrangeBackdrop(arranging: Boolean): Modifier =
    if (arranging) composed { background(JarnTheme.colors.raised) } else this

@Composable
internal fun RowScope.CanvasArrangeBanner(step: CanvasArrangeStep) {
    val colors = JarnTheme.colors
    JarnText(
        text = stringResource(R.string.jarnfe_canvas_arrange_title),
        style = JarnTheme.type.labelStrong,
        color = colors.accent,
        maxLines = 1,
    )
    JarnText(
        text = stringResource(step.messageRes),
        style = JarnTheme.type.caption,
        color = colors.textMid,
        maxLines = 1,
        modifier = Modifier.weight(1f, fill = false),
    )
}

private val CanvasArrangeStep.messageRes: Int
    get() = when (this) {
        CanvasArrangeStep.Choose -> R.string.jarnfe_canvas_arrange_choose
        CanvasArrangeStep.HoldingWithController -> R.string.jarnfe_canvas_arrange_holding
        CanvasArrangeStep.HoldingWithTouch -> R.string.jarnfe_canvas_arrange_holding_touch
    }
