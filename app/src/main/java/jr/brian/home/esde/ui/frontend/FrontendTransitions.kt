package jr.brian.home.esde.ui.frontend

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import jr.brian.home.esde.model.FRONTEND_TRANSITION_MS_MAX
import jr.brian.home.esde.model.FRONTEND_TRANSITION_MS_MIN
import jr.brian.home.esde.model.FrontendTransition

/**
 * Direction-aware transition for the Systems <-> Games route swap.
 *
 * [forward] is true when navigating Systems -> Games (drilling deeper). On the
 * way back every directional preset must reverse — a slide that goes the same
 * way both ways reads as broken.
 *
 * [durationMs] is clamped in-range here so a hand-edited pref cannot produce a
 * zero-length or multi-second animation.
 *
 * `togetherWith` alone returns a ContentTransform with the default SizeTransform.
 * We can't call the scope-only `using` infix here (it's only exposed inside
 * AnimatedContentTransitionScope), and ContentTransform's `sizeTransform`
 * setter is internal — so build ContentTransform via its public constructor
 * when we need SizeTransform(clip = false).
 */
fun FrontendTransition.transform(forward: Boolean, durationMs: Int): ContentTransform {
    val duration = durationMs.coerceIn(FRONTEND_TRANSITION_MS_MIN, FRONTEND_TRANSITION_MS_MAX)
    val floatSpec = tween<Float>(duration, easing = FrontendTokens.Motion.Easing)
    val offsetSpec = tween<androidx.compose.ui.unit.IntOffset>(
        durationMillis = duration,
        easing = FrontendTokens.Motion.Easing
    )
    val noClip = SizeTransform(clip = false)

    return when (this) {
        FrontendTransition.None -> ContentTransform(
            targetContentEnter = EnterTransition.None,
            initialContentExit = ExitTransition.None,
            sizeTransform = noClip
        )

        FrontendTransition.Fade -> ContentTransform(
            targetContentEnter = fadeIn(floatSpec),
            initialContentExit = fadeOut(floatSpec),
            sizeTransform = noClip
        )

        FrontendTransition.Slide -> ContentTransform(
            targetContentEnter = slideInHorizontally(offsetSpec) { w -> if (forward) w else -w } +
                    fadeIn(floatSpec),
            initialContentExit = slideOutHorizontally(offsetSpec) { w -> if (forward) -w else w } +
                    fadeOut(floatSpec),
            sizeTransform = noClip
        )

        FrontendTransition.Zoom -> ContentTransform(
            targetContentEnter = scaleIn(floatSpec, initialScale = if (forward) 0.92f else 1.08f) +
                    fadeIn(floatSpec),
            initialContentExit = scaleOut(floatSpec, targetScale = if (forward) 1.08f else 0.92f) +
                    fadeOut(floatSpec),
            sizeTransform = noClip
        )

        FrontendTransition.SlideUp -> ContentTransform(
            targetContentEnter = slideInVertically(offsetSpec) { h -> if (forward) h else -h } +
                    fadeIn(floatSpec),
            initialContentExit = slideOutVertically(offsetSpec) { h -> if (forward) -h else h } +
                    fadeOut(floatSpec),
            sizeTransform = noClip
        )
    }
}
