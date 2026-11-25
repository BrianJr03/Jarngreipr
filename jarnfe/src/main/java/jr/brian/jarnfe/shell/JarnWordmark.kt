package jr.brian.jarnfe.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.transition.LocalJarnMarkHandoff
import jr.brian.jarnfe.transition.jarnMarkHandoff

/** The header label on either screen. Pass the splash modifier on Stage, where the splash docks it. */
@Composable
internal fun JarnWordmark(modifier: Modifier = Modifier) {
    val travelPx = with(LocalDensity.current) { JarnTheme.motion.markTravel.toPx() }
    JarnText(
        text = stringResource(R.string.jarnfe_wordmark),
        style = JarnTheme.type.heading,
        color = JarnTheme.colors.textHigh,
        modifier = modifier.jarnMarkHandoff(LocalJarnMarkHandoff.current, travelPx),
        maxLines = 1,
    )
}
