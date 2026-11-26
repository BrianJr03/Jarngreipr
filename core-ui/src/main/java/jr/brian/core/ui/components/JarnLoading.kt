package jr.brian.core.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import kotlinx.coroutines.delay

/** Loads faster than this never show the spinner, so a quick load doesn't flash. */
const val JARN_LOADING_DELAY_MILLIS = 200L

private val LoadingSpinnerSize = 40.dp
private val LoadingSpinnerStroke = 3.dp

private enum class JarnLoadPhase { Waiting, Loading, Loaded }

@Composable
fun JarnLoadingState(label: String, modifier: Modifier = Modifier) {
    val colors = JarnTheme.colors
    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .padding(JarnTheme.space.xl)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
    ) {
        JarnSpinner(colors.accent, size = LoadingSpinnerSize, strokeWidth = LoadingSpinnerStroke)
        JarnText(label, JarnTheme.type.caption, colors.textMid, maxLines = 2, textAlign = TextAlign.Center)
    }
}

/**
 * Shows nothing for [delayMillis] while [value] is null, then [JarnLoadingState], and fades
 * into [content] once [value] arrives. A value present on the first frame shows straight away.
 */
@Composable
fun <T : Any> JarnLoadingGate(
    value: T?,
    loadingLabel: String,
    modifier: Modifier = Modifier,
    delayMillis: Long = JARN_LOADING_DELAY_MILLIS,
    content: @Composable (T) -> Unit,
) {
    val delayElapsed = rememberJarnDelayElapsed(delayMillis)
    val phase = when {
        value != null -> JarnLoadPhase.Loaded
        delayElapsed -> JarnLoadPhase.Loading
        else -> JarnLoadPhase.Waiting
    }
    Crossfade(
        targetState = phase,
        modifier = modifier,
        animationSpec = JarnTheme.motion.small.spec(),
        label = "jarnLoadingGate",
    ) { shown ->
        when (shown) {
            JarnLoadPhase.Waiting -> Unit
            JarnLoadPhase.Loading -> JarnCentredLoadingState(loadingLabel)
            JarnLoadPhase.Loaded -> value?.let { content(it) }
        }
    }
}

/** Fills the area so the spinner stays centred while it crossfades with full-size content. */
@Composable
private fun JarnCentredLoadingState(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        JarnLoadingState(label)
    }
}

@Composable
private fun rememberJarnDelayElapsed(delayMillis: Long): Boolean {
    val elapsed by produceState(initialValue = delayMillis <= 0L, delayMillis) {
        delay(delayMillis)
        value = true
    }
    return elapsed
}
