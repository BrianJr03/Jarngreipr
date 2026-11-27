package jr.brian.jarnfe.themekit

import jr.brian.core.model.theme.ThemeRect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A pixel rectangle on the current screen. [width] and [height] are never negative. */
internal data class ThemePxRect(val left: Int, val top: Int, val width: Int, val height: Int) {
    val right: Int get() = left + width
    val bottom: Int get() = top + height
    val area: Long get() = width.toLong() * height

    fun intersect(other: ThemePxRect): ThemePxRect? {
        val l = max(left, other.left)
        val t = max(top, other.top)
        val r = min(right, other.right)
        val b = min(bottom, other.bottom)
        return if (r > l && b > t) ThemePxRect(l, t, r - l, b - t) else null
    }

    fun contains(other: ThemePxRect): Boolean =
        other.left >= left && other.top >= top && other.right <= right && other.bottom <= bottom

    /** Shifts this rect inside [area], shrinking it first on any axis where it doesn't fit. */
    fun clampInto(area: ThemePxRect): ThemePxRect {
        val w = min(width, area.width)
        val h = min(height, area.height)
        return ThemePxRect(left.coerceIn(area.left, area.right - w), top.coerceIn(area.top, area.bottom - h), w, h)
    }

    /** Like [clampInto] but never resizes: null when this rect is bigger than [area] on either axis. */
    fun moveInto(area: ThemePxRect): ThemePxRect? =
        if (width > area.width || height > area.height) null else clampInto(area)
}

internal data class ThemePxSize(val width: Int, val height: Int)

/** Pixel insets the theme shouldn't draw under: the hint bar and any visible system bars. */
internal data class ThemeInsets(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0)

/** The screen a view is laid out on. Each screen passes its own size, so each keeps its own aspect. */
internal data class ThemeScreen(val width: Int, val height: Int, val insets: ThemeInsets = ThemeInsets()) {
    val bounds: ThemePxRect get() = ThemePxRect(0, 0, width, height)

    val safeArea: ThemePxRect
        get() {
            val l = insets.left.coerceIn(0, width)
            val t = insets.top.coerceIn(0, height)
            val r = (width - insets.right).coerceIn(l, width)
            val b = (height - insets.bottom).coerceIn(t, height)
            return ThemePxRect(l, t, r - l, b - t)
        }
}

/**
 * Maps a rect in screen fractions to pixels on a [screenWidth] x [screenHeight] screen.
 * Edges are rounded separately so rects that touch in fractions touch in pixels too.
 * Null when the rect has a non-finite value or no area.
 */
internal fun placeThemeRect(rect: ThemeRect, screenWidth: Int, screenHeight: Int): ThemePxRect? {
    val values = floatArrayOf(rect.x, rect.y, rect.width, rect.height, rect.originX, rect.originY)
    if (values.any { !it.isFinite() } || rect.width <= 0f || rect.height <= 0f) return null
    val startX = rect.x - rect.originX * rect.width
    val startY = rect.y - rect.originY * rect.height
    val left = (startX * screenWidth).roundToInt()
    val top = (startY * screenHeight).roundToInt()
    val right = ((startX + rect.width) * screenWidth).roundToInt()
    val bottom = ((startY + rect.height) * screenHeight).roundToInt()
    if (right <= left || bottom <= top) return null
    return ThemePxRect(left, top, right - left, bottom - top)
}
