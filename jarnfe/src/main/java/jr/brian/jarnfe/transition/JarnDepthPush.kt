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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import jr.brian.core.ui.theme.JarnMotion
import jr.brian.core.ui.theme.JarnRouteChange
import jr.brian.core.ui.theme.JarnRouteLeg
import jr.brian.core.ui.theme.JarnTheme

/**
 * Moves between levels of [target] with a depth push: going deeper sinks the old level and
 * brings the new one in from just in front, around [anchor]. [levelOf] gives each target's depth,
 * or null for one that changes without a transition. A change of mind mid-way reverses from where
 * it is, and the incoming content is live from the first frame, so input never waits.
 */
@Composable
internal fun <T> JarnDepthPush(
    target: T,
    levelOf: (T) -> Int?,
    anchor: () -> Rect?,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable (T) -> Unit,
) {
    val motion = JarnTheme.motion
    val container = remember { arrayOfNulls<Rect>(1) }
    AnimatedContent(
        targetState = target,
        modifier = modifier.onGloballyPositioned { container[0] = it.boundsInRoot() },
        transitionSpec = {
            depthTransform(motion, levelOf(initialState), levelOf(targetState)) {
                jarnDepthOrigin(anchor(), container[0])
            }
        },
        contentAlignment = contentAlignment,
        label = "JarnDepthPush",
    ) { shown -> content(shown) }
}

private fun <T> AnimatedContentTransitionScope<T>.depthTransform(
    motion: JarnMotion,
    from: Int?,
    to: Int?,
    origin: () -> TransformOrigin,
): ContentTransform {
    val change = jarnDepthChangeBetween(motion, from, to)
        ?: return (EnterTransition.None togetherWith ExitTransition.None).using(null)
    val pivot = origin()
    return (change.enter.enter(pivot) togetherWith change.exit.exit(pivot))
        .using(SizeTransform(clip = false) { _, _ -> snap() })
}

/** The change for moving from level [from] to [to], or null when there is nothing to animate. */
internal fun jarnDepthChangeBetween(motion: JarnMotion, from: Int?, to: Int?): JarnRouteChange? = when {
    from == null || to == null || from == to -> null
    to > from -> motion.depthOpen
    else -> motion.depthBack
}

private fun JarnRouteLeg.enter(pivot: TransformOrigin): EnterTransition {
    val fade = fadeIn(spec())
    return if (scales) fade + scaleIn(spec(), initialScale = fromScale, transformOrigin = pivot) else fade
}

private fun JarnRouteLeg.exit(pivot: TransformOrigin): ExitTransition {
    val fade = fadeOut(spec())
    return if (scales) fade + scaleOut(spec(), targetScale = toScale, transformOrigin = pivot) else fade
}
