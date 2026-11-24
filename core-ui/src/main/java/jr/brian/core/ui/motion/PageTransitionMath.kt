package jr.brian.core.ui.motion

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue

/**
 * One page's layer values during a page change. Classic's pager and Jarn's Canvas both use this,
 * so a preset looks the same in each. Only the fields a preset sets differ from the defaults.
 */
data class PageLayer(
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val alpha: Float = 1f,
)

fun PageLayer.applyTo(scope: GraphicsLayerScope) {
    scope.translationX = translationX
    scope.translationY = translationY
    scope.scaleX = scaleX
    scope.scaleY = scaleY
    scope.alpha = alpha
}

private const val STACK_LIFT_DP = 24f

/**
 * The math behind the gentle page transitions. [offset] is the page's distance from the settled
 * page in pager terms, from -1 to 1, and translations are added to where a pager already puts it.
 */
object PageTransitionMath {
    fun slide(offset: Float, width: Float): PageLayer = PageLayer(
        translationX = width * offset,
        alpha = lerp(0.6f, 1f, 1f - offset.absoluteValue.coerceIn(0f, 1f)),
    )

    fun fade(offset: Float): PageLayer =
        PageLayer(alpha = lerp(0f, 1f, 1f - offset.absoluteValue.coerceIn(0f, 1f)))

    fun depth(offset: Float, width: Float): PageLayer {
        val absOffset = offset.absoluteValue.coerceIn(0f, 1f)
        return if (offset <= 0f) {
            val scale = lerp(1f, 0.75f, absOffset)
            PageLayer(scaleX = scale, scaleY = scale, alpha = lerp(1f, 0f, absOffset))
        } else {
            val scale = lerp(0.75f, 1f, 1f - absOffset)
            PageLayer(
                translationX = width * offset,
                scaleX = scale,
                scaleY = scale,
                alpha = lerp(0f, 1f, 1f - absOffset),
            )
        }
    }

    fun stack(offset: Float, width: Float, density: Float): PageLayer {
        val absOffset = offset.absoluteValue.coerceIn(0f, 1f)
        return if (offset <= 0f) {
            PageLayer(translationX = width * offset, alpha = lerp(1f, 0f, absOffset))
        } else {
            val scale = lerp(0.85f, 1f, 1f - absOffset)
            PageLayer(
                translationY = lerp(0f, STACK_LIFT_DP * density, absOffset),
                scaleX = scale,
                scaleY = scale,
                alpha = lerp(0.4f, 0.85f, 1f - absOffset),
            )
        }
    }

    fun glideUp(offset: Float, height: Float): PageLayer {
        val absOffset = offset.absoluteValue.coerceIn(0f, 1f)
        return if (offset <= 0f) {
            PageLayer(translationY = -height * absOffset, alpha = lerp(1f, 0f, absOffset))
        } else {
            PageLayer(translationY = height * absOffset, alpha = lerp(0f, 1f, 1f - absOffset))
        }
    }

    fun carousel(offset: Float, width: Float): PageLayer {
        val absOffset = offset.absoluteValue.coerceIn(0f, 1f)
        val scale = lerp(0.7f, 1f, 1f - absOffset)
        return PageLayer(
            translationX = width * offset * 0.75f,
            scaleX = scale,
            scaleY = scale,
            alpha = lerp(0.4f, 1f, 1f - absOffset),
        )
    }
}
