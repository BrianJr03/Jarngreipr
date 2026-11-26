package jr.brian.jarnfe.shell

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnCornerToastCard
import jr.brian.core.ui.components.JarnToastTone
import jr.brian.core.ui.theme.JarnMotionToken
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.isOnTopScreen
import kotlinx.coroutines.delay

internal const val JARN_CORNER_TOAST_TAG = "jarn_corner_toast"

private val ToastTopInset = 72.dp

sealed interface JarnToastIcon {
    data class Res(@param:DrawableRes val id: Int) : JarnToastIcon
    data class Vector(val image: ImageVector) : JarnToastIcon
}

/** Where a toast sits. Only the top screen's top-right corner exists for now. */
enum class JarnToastPlacement { TopEnd }

/**
 * One corner toast. Text is string resources from the caller. A toast with the same [key] as the
 * shown or a queued one replaces it (and restarts the timer when shown), so repeats never stack.
 */
@Immutable
data class JarnCornerToast(
    val icon: JarnToastIcon,
    @param:StringRes val title: Int,
    @param:StringRes val subtitle: Int? = null,
    val tone: JarnToastTone = JarnToastTone.Accent,
    val durationMillis: Long = DEFAULT_DURATION_MS,
    val key: String? = null,
    val placement: JarnToastPlacement = JarnToastPlacement.TopEnd,
) {
    companion object {
        const val DEFAULT_DURATION_MS = 2_500L
    }
}

/** A shown toast. [sequence] changes on every show or replacement, which restarts its timer. */
@Immutable
internal data class JarnShownToast(val toast: JarnCornerToast, val sequence: Long)

/**
 * Short confirmations for any feature: call [show] from anywhere on the main thread. One shows at a
 * time, the rest wait in order, and past [MAX_QUEUED] waiting the oldest is dropped.
 * [JarnCornerToastHost] draws them and never takes focus or input.
 */
object JarnCornerToasts {
    const val MAX_QUEUED = 3

    internal var current by mutableStateOf<JarnShownToast?>(null)
        private set

    private val queue = ArrayDeque<JarnCornerToast>()
    private var sequence = 0L

    fun show(toast: JarnCornerToast) {
        val shown = current
        when {
            shown == null -> current = next(toast)
            toast.key != null && shown.toast.key == toast.key -> current = next(toast)
            else -> enqueue(toast)
        }
    }

    internal val waiting: List<JarnCornerToast> get() = queue.toList()

    internal fun dismiss(shown: JarnShownToast) {
        if (current != shown) return
        current = queue.removeFirstOrNull()?.let(::next)
    }

    internal fun clear() {
        queue.clear()
        current = null
    }

    private fun enqueue(toast: JarnCornerToast) {
        val same = toast.key?.let { key -> queue.indexOfFirst { it.key == key } } ?: -1
        if (same >= 0) {
            queue[same] = toast
            return
        }
        queue.addLast(toast)
        while (queue.size > MAX_QUEUED) queue.removeFirst()
    }

    private fun next(toast: JarnCornerToast): JarnShownToast {
        sequence += 1
        return JarnShownToast(toast, sequence)
    }
}

/** Place inside each window's root Box. It draws on the top screen only, whichever role is there. */
@Composable
internal fun BoxScope.JarnCornerToastHost(onTopScreen: Boolean = isOnTopScreen()) {
    if (!onTopScreen) return
    val shown = JarnCornerToasts.current
    LaunchedEffect(shown) {
        shown ?: return@LaunchedEffect
        delay(shown.toast.durationMillis)
        JarnCornerToasts.dismiss(shown)
    }
    val token = JarnTheme.motion.panel
    val reduced = JarnTheme.motion.reduced
    AnimatedContent(
        targetState = shown,
        transitionSpec = { cornerToastTransform(token, reduced) },
        contentKey = { it?.toast?.key ?: it?.sequence },
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = ToastTopInset, end = JarnTheme.space.stageMargin),
        label = "JarnCornerToast",
    ) { toast ->
        toast?.let { CornerToast(it.toast) }
    }
}

@Composable
private fun CornerToast(toast: JarnCornerToast) {
    JarnCornerToastCard(
        icon = toast.icon.painter(),
        title = stringResource(toast.title),
        subtitle = toast.subtitle?.let { stringResource(it) },
        tone = toast.tone,
        modifier = Modifier.testTag(JARN_CORNER_TOAST_TAG),
    )
}

@Composable
private fun JarnToastIcon.painter() = when (this) {
    is JarnToastIcon.Res -> painterResource(id)
    is JarnToastIcon.Vector -> rememberVectorPainter(image)
}

private fun cornerToastTransform(token: JarnMotionToken, reduced: Boolean): ContentTransform {
    val spec = tween<Float>(token.durationMillis, easing = token.easing)
    if (reduced) return fadeIn(spec) togetherWith fadeOut(spec)
    val slide = tween<IntOffset>(token.durationMillis, easing = token.easing)
    return (fadeIn(spec) + slideInHorizontally(slide) { it }) togetherWith
        (fadeOut(spec) + slideOutHorizontally(slide) { it })
}
