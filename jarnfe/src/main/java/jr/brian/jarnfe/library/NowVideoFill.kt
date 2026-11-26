package jr.brian.jarnfe.library

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import jr.brian.core.ui.theme.JarnMotion
import jr.brian.core.ui.theme.JarnTheme
import kotlin.math.roundToInt

/** Where the scene sits in its window, read after placement so growing never feeds back into it. */
@Stable
private class NowSceneSpot {
    var origin by mutableStateOf(Offset.Zero)
    var window by mutableStateOf(IntSize.Zero)

    fun update(coordinates: LayoutCoordinates) {
        origin = coordinates.positionInRoot()
        window = coordinates.findRootCoordinates().size
    }
}

/**
 * The art and preview layer. With the UI hidden it grows from its own slot to the whole window,
 * so the preview fills the screen. It only changes size and offset, so the player inside is never
 * recreated, and at rest it lays out exactly as a plain full-size box.
 */
@Composable
internal fun BoxScope.NowVideoFill(content: @Composable BoxScope.() -> Unit) {
    val spot = remember { NowSceneSpot() }
    val grow by rememberNowFillProgress(NowVideoVisibility.uiHidden)
    Box(Modifier.matchParentSize().onPlaced(spot::update)) {
        Box(
            modifier = Modifier.nowFillWindow(spot) { grow }.clipToBounds(),
            content = content,
        )
    }
}

@Composable
private fun rememberNowFillProgress(filled: Boolean): State<Float> {
    val motion = JarnTheme.motion
    return animateFloatAsState(
        targetValue = if (filled) 1f else 0f,
        animationSpec = nowFillSpec(motion),
        label = "NowVideoFill",
    )
}

/** Reduced motion jumps straight to the new size, since the grow is movement, not a fade. */
private fun nowFillSpec(motion: JarnMotion): AnimationSpec<Float> =
    if (motion.reduced) snap() else motion.screen.spec()

private fun Modifier.nowFillWindow(spot: NowSceneSpot, progress: () -> Float): Modifier =
    layout { measurable, constraints ->
        val slotWidth = constraints.maxWidth
        val slotHeight = constraints.maxHeight
        val grow = progress().takeIf { spot.window != IntSize.Zero } ?: 0f
        val width = lerp(slotWidth.toFloat(), spot.window.width.toFloat(), grow).roundToInt()
        val height = lerp(slotHeight.toFloat(), spot.window.height.toFloat(), grow).roundToInt()
        val placeable = measurable.measure(Constraints.fixed(width, height))
        layout(slotWidth, slotHeight) {
            placeable.place((-spot.origin.x * grow).roundToInt(), (-spot.origin.y * grow).roundToInt())
        }
    }
