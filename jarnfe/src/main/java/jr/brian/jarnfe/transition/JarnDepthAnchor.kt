package jr.brian.jarnfe.transition

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement

/**
 * Where each system item last sat on screen, so the depth push can scale around the one that was
 * picked. Entries outlive their item on purpose: going back needs the spot it will return to.
 * Only touched on the main thread.
 */
internal object JarnDepthAnchors {
    private val bounds = HashMap<String, Rect>()

    fun record(id: String, rect: Rect) {
        bounds[id] = rect
    }

    fun boundsOf(id: String?): Rect? = id?.let(bounds::get)
}

/** Marks a system item as the point a depth push into its games grows from. */
internal fun Modifier.jarnDepthAnchor(id: String): Modifier = this then JarnDepthAnchorElement(id)

private data class JarnDepthAnchorElement(val id: String) : ModifierNodeElement<JarnDepthAnchorNode>() {
    override fun create() = JarnDepthAnchorNode(id)

    override fun update(node: JarnDepthAnchorNode) {
        node.id = id
    }
}

private class JarnDepthAnchorNode(var id: String) : Modifier.Node(), GlobalPositionAwareModifierNode {
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        JarnDepthAnchors.record(id, coordinates.boundsInRoot())
    }
}

/** The anchor's center as a fraction of [container], or the center when there is none. */
internal fun jarnDepthOrigin(anchor: Rect?, container: Rect?): TransformOrigin {
    if (anchor == null || container == null || container.width <= 0f || container.height <= 0f) {
        return TransformOrigin.Center
    }
    val center = anchor.center
    return TransformOrigin(
        pivotFractionX = ((center.x - container.left) / container.width).coerceIn(0f, 1f),
        pivotFractionY = ((center.y - container.top) / container.height).coerceIn(0f, 1f),
    )
}
