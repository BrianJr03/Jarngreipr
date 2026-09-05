package jr.brian.home.esde.ui.frontend.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Monotonic counter, incremented once per Confirm press inside the row list. */
val LocalRowActivation: ProvidableCompositionLocal<Int> = staticCompositionLocalOf { 0 }

/**
 * Runs [onActivate] when Confirm is pressed while this row holds the cursor.
 * Unfocused rows still record the tick they have seen, so a row that gains the
 * cursor later does not immediately fire on a press meant for its neighbour.
 */
@Composable
fun ActivateOnConfirm(focused: Boolean, onActivate: () -> Unit) {
    val tick = LocalRowActivation.current
    var lastSeen by remember { mutableIntStateOf(tick) }
    LaunchedEffect(tick) {
        if (tick != lastSeen) {
            lastSeen = tick
            if (focused) onActivate()
        }
    }
}

/** Running signed count of horizontal steps aimed at a row that wants them. */
val LocalRowStep: ProvidableCompositionLocal<Int> = staticCompositionLocalOf { 0 }

/** Lets the focused row declare that it consumes Left/Right. */
val LocalHorizontalRowRegistration: ProvidableCompositionLocal<(Boolean) -> Unit> =
    staticCompositionLocalOf { { } }

@Composable
fun RegisterForHorizontalSteps(focused: Boolean) {
    val register = LocalHorizontalRowRegistration.current
    DisposableEffect(focused, register) {
        if (focused) register(true)
        onDispose { if (focused) register(false) }
    }
}

/**
 * Runs [onStep] with the *delta* since this row last looked, not a single step,
 * so a row cannot lose presses to a recomposition between them.
 */
@Composable
fun StepOnHorizontal(focused: Boolean, onStep: (Int) -> Unit) {
    val steps = LocalRowStep.current
    var lastSeen by remember { mutableIntStateOf(steps) }
    LaunchedEffect(steps) {
        val delta = steps - lastSeen
        lastSeen = steps
        if (delta != 0 && focused) onStep(delta)
    }
}
