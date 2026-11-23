package jr.brian.core.ui.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.LocalJarnColors
import jr.brian.core.ui.theme.LocalJarnMotion
import jr.brian.core.ui.theme.LocalJarnScreenActivation
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

internal val JarnFocusRingWidth = 3.dp
internal val JarnFocusGapWidth = 2.dp
internal val JarnFocusOutset = JarnFocusRingWidth + JarnFocusGapWidth
internal val JarnFocusGrowthCap = 6.dp
private const val JarnFocusLandingSpread = 1.4f

internal fun jarnFocusScale(widthPx: Float, heightPx: Float, capPx: Float, targetScale: Float): Float {
    val longest = max(widthPx, heightPx)
    if (longest <= 0f || targetScale <= 1f) return 1f
    return min(targetScale, 1f + 2f * capPx / longest)
}

internal fun jarnCombinedScale(focusScale: Float, focusProgress: Float, pressScale: Float, pressProgress: Float): Float {
    val focused = 1f + (focusScale - 1f) * focusProgress
    val pressed = 1f + (pressScale - 1f) * pressProgress
    return focused * pressed
}

fun Modifier.jarnFocus(
    shape: Shape,
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier = this then JarnFocusElement(shape, interactionSource, enabled)

private data class JarnFocusElement(
    val shape: Shape,
    val interactionSource: InteractionSource,
    val enabled: Boolean,
) : ModifierNodeElement<JarnFocusNode>() {
    override fun create() = JarnFocusNode(shape, interactionSource, enabled)

    override fun update(node: JarnFocusNode) = node.update(shape, interactionSource, enabled)

    override fun InspectorInfo.inspectableProperties() {
        name = "jarnFocus"
        properties["shape"] = shape
        properties["enabled"] = enabled
    }
}

private class JarnFocusNode(
    private var shape: Shape,
    private var interactionSource: InteractionSource,
    private var enabled: Boolean,
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val focusProgress = Animatable(0f)
    private val pressProgress = Animatable(0f)
    private var isFocused by mutableStateOf(false)
    private var activePresses = 0
    private var interactionJob: Job? = null

    private var cachedPath: Path? = null
    private var cachedSize = Size.Unspecified
    private var cachedDirection: LayoutDirection? = null
    private var cachedDensity = 0f

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val scale = currentScale(size.width, size.height, this)
        scaleX = scale
        scaleY = scale
    }

    override val shouldAutoInvalidate: Boolean = false

    fun update(shape: Shape, interactionSource: InteractionSource, enabled: Boolean) {
        val shapeChanged = this.shape != shape
        this.shape = shape
        this.enabled = enabled
        if (shapeChanged) {
            cachedPath = null
            invalidateDraw()
        }
        if (this.interactionSource != interactionSource) {
            this.interactionSource = interactionSource
            if (isAttached) observeInteractions()
        }
        if (!enabled) settlePress()
    }

    override fun onAttach() {
        observeInteractions()
    }

    override fun onDetach() {
        interactionJob = null
        isFocused = false
        activePresses = 0
    }

    override fun onReset() {
        coroutineScope.launch {
            focusProgress.snapTo(0f)
            pressProgress.snapTo(0f)
        }
    }

    private fun observeInteractions() {
        interactionJob?.cancel()
        interactionJob = coroutineScope.launch {
            interactionSource.interactions.collect(::onInteraction)
        }
    }

    private fun onInteraction(interaction: Interaction) {
        when (interaction) {
            is FocusInteraction.Focus -> animateFocus(true)
            is FocusInteraction.Unfocus -> animateFocus(false)
            is PressInteraction.Press -> if (enabled) {
                activePresses++
                animatePress()
            }
            is PressInteraction.Release, is PressInteraction.Cancel -> if (activePresses > 0) {
                activePresses--
                animatePress()
            }
        }
    }

    private fun animateFocus(focused: Boolean) {
        isFocused = focused
        val motion = currentValueOf(LocalJarnMotion)
        val token = if (focused) motion.focusIn else motion.focusOut
        coroutineScope.launch {
            focusProgress.animateTo(if (focused) 1f else 0f, token.spec())
        }
    }

    private fun animatePress() {
        val token = currentValueOf(LocalJarnMotion).press
        val target = if (activePresses > 0) 1f else 0f
        coroutineScope.launch { pressProgress.animateTo(target, token.spec()) }
    }

    private fun settlePress() {
        if (activePresses == 0) return
        activePresses = 0
        animatePress()
    }

    private fun currentScale(widthPx: Float, heightPx: Float, density: Density): Float {
        val activation = currentValueOf(LocalJarnScreenActivation).value
        if (activation <= 0f) return 1f
        val motion = currentValueOf(LocalJarnMotion)
        val capPx = with(density) { JarnFocusGrowthCap.toPx() }
        val focusScale = jarnFocusScale(widthPx, heightPx, capPx, motion.focusScale)
        return jarnCombinedScale(focusScale, focusProgress.value * activation, motion.pressScale, pressProgress.value)
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, zIndex = if (isFocused) 1f else 0f, layerBlock = layerBlock)
        }
    }

    override fun ContentDrawScope.draw() {
        val progress = focusProgress.value
        if (isFocused || progress > 0f) drawIndicator(progress)
        drawContent()
    }

    /** The ring exists only on the active screen, and lands inward as that screen takes control. */
    private fun DrawScope.drawIndicator(progress: Float) {
        val activation = currentValueOf(LocalJarnScreenActivation).value
        if (activation <= 0f) return
        val colors = currentValueOf(LocalJarnColors)
        val scale = currentScale(size.width, size.height, this)
        val spread = 1f + (1f - activation) * JarnFocusLandingSpread
        drawRing(outlinePath(), scale, colors.focus, colors.focusGap, progress * activation, spread)
    }

    private fun DrawScope.drawRing(path: Path, scale: Float, ring: Color, gap: Color, alpha: Float, spread: Float) {
        val ringStroke = 2f * JarnFocusOutset.toPx() * spread / scale
        val gapStroke = 2f * JarnFocusGapWidth.toPx() * spread / scale
        withTransform({
            scale(scale, scale, center)
            clipPath(path, ClipOp.Difference)
        }) {
            drawPath(path, ring, alpha, Stroke(width = ringStroke, join = StrokeJoin.Round))
            drawPath(path, gap, alpha, Stroke(width = gapStroke, join = StrokeJoin.Round))
        }
    }

    private fun DrawScope.outlinePath(): Path {
        cachedPath?.let { path ->
            if (cachedSize == size && cachedDirection == layoutDirection && cachedDensity == density) return path
        }
        val path = Path().apply {
            when (val outline = shape.createOutline(size, layoutDirection, this@outlinePath)) {
                is Outline.Rectangle -> addRect(outline.rect)
                is Outline.Rounded -> addRoundRect(outline.roundRect)
                is Outline.Generic -> addPath(outline.path)
            }
        }
        cachedPath = path
        cachedSize = size
        cachedDirection = layoutDirection
        cachedDensity = density
        return path
    }
}
