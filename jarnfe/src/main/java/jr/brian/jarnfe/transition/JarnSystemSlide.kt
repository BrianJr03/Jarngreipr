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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import jr.brian.core.ui.theme.JarnRouteStep
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.library.JarnLibrary
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Steps a game list to a neighboring system with a short slide: the next system comes in from
 * the right, the previous one from the left. [content] gets the system it draws, which for the
 * outgoing list is the one being left, so render from that id rather than the live open system.
 */
@Composable
internal fun JarnSystemSlide(
    systemId: String?,
    modifier: Modifier = Modifier,
    orderOf: (String?) -> Int = ::librarySystemOrder,
    content: @Composable (systemId: String?) -> Unit,
) {
    val step = JarnTheme.motion.systemStep
    AnimatedContent(
        targetState = systemId,
        modifier = modifier,
        transitionSpec = { slideTransform(step, jarnStepDirection(orderOf(initialState), orderOf(targetState))) },
        contentAlignment = Alignment.Center,
        label = "JarnSystemSlide",
    ) { shown -> content(shown) }
}

/** +1 for a step to a later system, -1 for an earlier one, 0 when there is no neighbor to slide from. */
internal fun jarnStepDirection(fromIndex: Int, toIndex: Int): Int =
    if (fromIndex < 0 || toIndex < 0) 0 else (toIndex - fromIndex).sign

private fun librarySystemOrder(systemId: String?): Int =
    JarnLibrary.state.systems.items.indexOfFirst { it.id == systemId }

private fun AnimatedContentTransitionScope<String?>.slideTransform(step: JarnRouteStep, direction: Int): ContentTransform {
    if (direction == 0) return (EnterTransition.None togetherWith ExitTransition.None).using(null)
    val shift = step.offsetFraction * direction
    var enter = fadeIn(step.enter.spec())
    var exit = fadeOut(step.exit.spec())
    if (shift != 0f) {
        enter += slideInHorizontally(step.enter.spec<IntOffset>()) { width -> (width * shift).roundToInt() }
        exit += slideOutHorizontally(step.exit.spec<IntOffset>()) { width -> -(width * shift).roundToInt() }
    }
    return (enter togetherWith exit).using(SizeTransform(clip = false) { _, _ -> snap() })
}
