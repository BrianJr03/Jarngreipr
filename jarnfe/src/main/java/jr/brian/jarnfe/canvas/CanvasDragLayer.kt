package jr.brian.jarnfe.canvas

import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import kotlinx.coroutines.flow.collectLatest

private const val GHOST_FILL_ALPHA = 0.14f
private val GhostStrokeWidth = 2.dp
private val GhostDashLength = 6.dp
internal val CanvasLiftElevation = 10.dp

@Composable
internal fun rememberCanvasSlotGlide(originX: Int, originY: Int, glides: Boolean): CanvasSlotGlide {
    val glide = remember { CanvasSlotGlide() }
    val token = JarnTheme.motion.small
    SideEffect { glide.place(originX, originY, glides) }
    LaunchedEffect(glide, token) {
        snapshotFlow { glide.generation }.collectLatest {
            if (glide.isGliding) {
                animate(0f, 1f, animationSpec = token.spec()) { value, _ -> glide.step(value) }
            }
        }
    }
    return glide
}

/** Everything here is read in the layer block, so a finger move only redraws this one layer. */
internal fun Modifier.canvasSlotLayer(
    glide: CanvasSlotGlide,
    visual: CanvasDragVisual?,
    tileId: String?,
    originX: Int,
    originY: Int,
    grabLift: State<Float>,
    reduced: Boolean,
    tileShape: Shape,
): Modifier = graphicsLayer {
    val floating = visual != null && tileId != null && visual.tileId == tileId
    val raised = if (floating) visual.lift else grabLift.value
    translationX = if (floating) visual.x - originX else glide.dx
    translationY = if (floating) visual.y - originY else glide.dy
    val scale = canvasLiftScale(raised, reduced)
    scaleX = scale
    scaleY = scale
    shadowElevation = CanvasLiftElevation.toPx() * raised
    shape = tileShape
    clip = false
}

/** Eases a released tile into its cell, or places it at once under reduced motion. */
@Composable
internal fun CanvasDragSettle(visual: CanvasDragVisual, heldId: String?, target: IntOffset?) {
    val tile = visual.tileId ?: return
    if (tile == heldId) return
    val motion = JarnTheme.motion
    if (motion.reduced || target == null) {
        SideEffect { visual.finish() }
        return
    }
    LaunchedEffect(tile, target) {
        visual.beginSettle()
        animate(0f, 1f, animationSpec = motion.small.spec()) { value, _ ->
            visual.settleStep(value, target.x.toFloat(), target.y.toFloat())
        }
        visual.finish()
    }
}

@Composable
internal fun CanvasDragGhost() {
    val accent = JarnTheme.colors.accent
    val shape = JarnTheme.tileShape
    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val dash = GhostDashLength.toPx()
                val stroke = Stroke(
                    width = GhostStrokeWidth.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                )
                val outline = shape.createOutline(size, layoutDirection, this)
                onDrawBehind {
                    drawOutline(outline, accent.copy(alpha = GHOST_FILL_ALPHA))
                    drawOutline(outline, accent, style = stroke)
                }
            },
    )
}
