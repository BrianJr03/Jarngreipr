package jr.brian.jarnfe.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.transition.LocalJarnMarkHandoff
import jr.brian.jarnfe.transition.jarnMarkHandoff
import jr.brian.jarnfe.transition.jarnMarkSlot

/**
 * The header label on either screen, in a slot that also holds the [gap] after it, so the rest
 * of the row slides over while the label is away. Pass the splash modifier on Stage, where the
 * splash docks it.
 */
@Composable
internal fun JarnWordmark(modifier: Modifier = Modifier, gap: Dp = JarnTheme.space.lg) {
    val handoff = LocalJarnMarkHandoff.current
    val density = LocalDensity.current
    val travelPx = with(density) { JarnTheme.motion.markTravel.toPx() }
    val gapPx = with(density) { gap.roundToPx() }
    JarnText(
        text = stringResource(R.string.jarnfe_wordmark),
        style = JarnTheme.type.heading,
        color = JarnTheme.colors.textHigh,
        modifier = Modifier
            .jarnMarkSlot(handoff, gapPx)
            .then(modifier)
            .jarnMarkHandoff(handoff, travelPx),
        maxLines = 1,
    )
}
