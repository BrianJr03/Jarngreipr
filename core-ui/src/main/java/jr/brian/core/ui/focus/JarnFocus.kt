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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.LocalJarnColors
import jr.brian.core.ui.theme.LocalJarnMotion
import jr.brian.core.ui.theme.LocalJarnScreenActivation
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal val JarnFocusRingWidth = 3.dp
internal val JarnFocusGapWidth = 2.dp
private const val JarnFocusLandingSpread = 1.4f

/** Never above 1, so a focused or pressed item stays inside its own bounds. */
internal fun jarnPressScale(pressScale: Float, pressProgress: Float): Float =
    (1f + (pressScale - 1f) * pressProgress).coerceAtMost(1f)

enum class JarnFocusStyle { Ring, Scale }

private const val JarnFocusScale = 1.08f

/** Under the 20dp focus edge, so a scaled item always clears the edges bring-into-view leaves. */
internal val JarnFocusMaxGrowth = 10.dp

/** Large items grow by less, so no side ever moves past [maxGrowthPx]. */
internal fun jarnFocusScale(
    style: JarnFocusStyle,
    focusProgress: Float,
    activation: Float,
    maxSidePx: Int,
    maxGrowthPx: Float,
): Float {
    if (style == JarnFocusStyle.Ring || maxSidePx <= 0) return 1f
    val target = minOf(JarnFocusScale, 1f + 2f * maxGrowthPx / maxSidePx)
    return 1f + (target - 1f) * focusProgress * activation
}

fun Modifier.jarnFocus(
    shape: Shape,
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    style: JarnFocusStyle = JarnFocusStyle.Ring,
): Modifier = this then JarnFocusElement(shape, interactionSource, enabled, style)

private data class JarnFocusElement(
    val shape: Shape,
    val interactionSource: InteractionSource,
    val enabled: Boolean,
    val style: JarnFocusStyle,
) : ModifierNodeElement<JarnFocusNode>() {
    override fun create() = JarnFocusNode(shape, interactionSource, enabled, style)

    override fun update(node: JarnFocusNode) = node.update(shape, interactionSource, enabled, style)

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
    style: JarnFocusStyle,
) : Modifier.Node(), LayoutModifierNode, DrawModifierNode, CompositionLocalConsumerModifierNode {

    private val focusProgress = Animatable(0f)
    private val pressProgress = Animatable(0f)
    private var isFocused by mutableStateOf(false)
    private var style by mutableStateOf(style)
    private var activePresses = 0
    private var interactionJob: Job? = null

    private var maxSidePx = 0
    private var cachedPath: Path? = null
    private var cachedSize = Size.Unspecified
    private var cachedDirection: LayoutDirection? = null
    private var cachedDensity = 0f

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val activation = currentValueOf(LocalJarnScreenActivation).value
        val growth = jarnFocusScale(style, focusProgress.value, activation, maxSidePx, JarnFocusMaxGrowth.toPx())
        val scale = currentScale() * growth
        scaleX = scale
        scaleY = scale
    }

    override val shouldAutoInvalidate: Boolean = false

    fun update(shape: Shape, interactionSource: InteractionSource, enabled: Boolean, style: JarnFocusStyle) {
        this.style = style
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

    private fun currentScale(): Float =
        jarnPressScale(currentValueOf(LocalJarnMotion).pressScale, pressProgress.value)

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        maxSidePx = maxOf(placeable.width, placeable.height)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, zIndex = if (isFocused) 1f else 0f, layerBlock = layerBlock)
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val progress = focusProgress.value
        if (style == JarnFocusStyle.Ring && (isFocused || progress > 0f)) drawIndicator(progress)
    }

    /** The ring exists only on the active screen, and settles as that screen takes control. */
    private fun DrawScope.drawIndicator(progress: Float) {
        val activation = currentValueOf(LocalJarnScreenActivation).value
        if (activation <= 0f) return
        val colors = currentValueOf(LocalJarnColors)
        val spread = 1f + (1f - activation) * JarnFocusLandingSpread
        drawRing(outlinePath(), currentScale(), colors.focus, colors.focusGap, progress * activation, spread)
    }

    /** Clipped to the item's shape, so the ring can't reach a neighbor or be cut off by one. */
    private fun DrawScope.drawRing(path: Path, scale: Float, ring: Color, gap: Color, alpha: Float, spread: Float) {
        val ringStroke = 2f * JarnFocusRingWidth.toPx() * spread / scale
        val gapStroke = ringStroke + 2f * JarnFocusGapWidth.toPx() * spread / scale
        withTransform({
            scale(scale, scale, center)
            clipPath(path, ClipOp.Intersect)
        }) {
            drawPath(path, gap, alpha, Stroke(width = gapStroke, join = StrokeJoin.Round))
            drawPath(path, ring, alpha, Stroke(width = ringStroke, join = StrokeJoin.Round))
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
