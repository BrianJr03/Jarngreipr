package jr.brian.jarnfe.transition

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import jr.brian.core.ui.theme.JarnRouteLeg
import jr.brian.core.ui.theme.JarnTheme

/**
 * The header beside a [JarnDepthPush]: crossfades when [target] changes level and swaps
 * instantly otherwise. Takes the same [levelOf] as the push it sits over.
 */
@Composable
internal fun <T> JarnHeaderSwap(
    target: T,
    levelOf: (T) -> Int?,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val leg = JarnTheme.motion.headerSwap
    AnimatedContent(
        targetState = target,
        modifier = modifier,
        transitionSpec = { headerTransform(leg, levelOf(initialState), levelOf(targetState)) },
        contentAlignment = Alignment.CenterStart,
        label = "JarnHeaderSwap",
    ) { shown -> content(shown) }
}

private fun <T> AnimatedContentTransitionScope<T>.headerTransform(
    leg: JarnRouteLeg,
    from: Int?,
    to: Int?,
): ContentTransform {
    if (from == null || to == null || from == to) {
        return (EnterTransition.None togetherWith ExitTransition.None).using(null)
    }
    return (fadeIn(leg.spec()) togetherWith fadeOut(leg.spec())).using(SizeTransform(clip = false) { _, _ -> snap() })
}
