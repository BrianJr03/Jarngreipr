package jr.brian.jarnfe.themekit

import jr.brian.core.model.theme.ThemeFit
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ViewSpec
import jr.brian.core.model.theme.forArea
import kotlin.math.max
import kotlin.math.min

/** A layer with its pixel bounds. [decodeCap] is the largest size its picture may decode at, when it has one. */
internal data class PlacedThemeLayer(
    val layer: ThemeLayer,
    val bounds: ThemePxRect,
    val decodeCap: ThemePxSize? = null,
)

/**
 * A view ready to draw: [below] draws first, then the primary, then [above], each already in z order.
 * [issues] lists what the guardrails changed, for fit reports.
 */
internal data class PlacedThemeView(
    val primary: ThemePxRect,
    val below: List<PlacedThemeLayer>,
    val above: List<PlacedThemeLayer>,
    val issues: List<ThemeLayoutIssue>,
)

/** The primary is at least this fraction of the safe area on each axis. */
internal const val MIN_PRIMARY_FRACTION = 0.2f

/** Pictures decode at most at this multiple of their on-screen size. */
internal const val DECODE_CAP_MULTIPLIER = 2

/** A layer above the primary is moved below it when it covers at least this share of the primary... */
internal const val COVER_AREA_FRACTION = 0.5f

/** ...and is at least this opaque. */
internal const val COVER_OPACITY = 0.5f

/**
 * Places [spec] on [screen], with its narrow patch when the screen is narrow (see [forArea]), and
 * enforces the theme guardrails:
 * - The primary is grown to [MIN_PRIMARY_FRACTION] and clamped into the safe area, so it can't be hidden or off-screen.
 * - Layers above the primary that would mostly cover it are moved below it.
 * - Only the first video plays; later ones show their fallback picture or are dropped.
 * - Text, date and rating layers are moved out of the safe zones when they fit, and flagged when they don't.
 *   Other layers may run full bleed under the bars and are only flagged.
 * - Pictures decode at most at [DECODE_CAP_MULTIPLIER] times their on-screen size.
 * Layers with z of 0 or less draw below the primary, ties keeping their order in the theme.
 */
internal fun resolveThemeView(spec: ViewSpec, screen: ThemeScreen): PlacedThemeView {
    val view = spec.forArea(screen.width, screen.height)
    val issues = mutableListOf<ThemeLayoutIssue>()
    val primary = placePrimary(view, screen, issues)
    val below = mutableListOf<PlacedThemeLayer>()
    val above = mutableListOf<PlacedThemeLayer>()
    var playingVideoId: String? = null

    for (original in view.layers) {
        val layer = limitVideos(original, playingVideoId, issues) ?: continue
        if (layer is ThemeLayer.Video) playingVideoId = layer.id
        val bounds = placeLayer(layer, screen, issues) ?: continue
        val placed = PlacedThemeLayer(layer, bounds, decodeCapFor(layer, bounds, screen))
        when {
            layer.z <= 0 -> below += placed
            coversPrimary(layer, bounds, primary) -> {
                issues += ThemeLayoutIssue.CoveredPrimary(layer.id)
                below += placed
            }
            else -> above += placed
        }
    }
    return PlacedThemeView(primary, below.sortedBy { it.layer.z }, above.sortedBy { it.layer.z }, issues)
}

private fun placePrimary(view: ViewSpec, screen: ThemeScreen, issues: MutableList<ThemeLayoutIssue>): ThemePxRect {
    val safe = screen.safeArea
    val placed = placeThemeRect(view.primary.box, screen.width, screen.height)
    if (placed == null) {
        issues += ThemeLayoutIssue.PrimaryInvalid
        return safe
    }
    val grown = growToMinimum(placed, safe)
    if (grown != placed) issues += ThemeLayoutIssue.PrimaryTooSmall
    val clamped = grown.clampInto(safe)
    if (clamped != grown) issues += ThemeLayoutIssue.PrimaryClamped
    return clamped
}

private fun growToMinimum(rect: ThemePxRect, safe: ThemePxRect): ThemePxRect {
    val minWidth = (safe.width * MIN_PRIMARY_FRACTION).toInt()
    val minHeight = (safe.height * MIN_PRIMARY_FRACTION).toInt()
    if (rect.width >= minWidth && rect.height >= minHeight) return rect
    val width = max(rect.width, minWidth)
    val height = max(rect.height, minHeight)
    return ThemePxRect(rect.left - (width - rect.width) / 2, rect.top - (height - rect.height) / 2, width, height)
}

private fun limitVideos(layer: ThemeLayer, playingId: String?, issues: MutableList<ThemeLayoutIssue>): ThemeLayer? {
    if (layer !is ThemeLayer.Video || playingId == null) return layer
    val pictures = layer.pictures
    issues += ThemeLayoutIssue.ExtraVideo(layer.id, playingId, fallbackShown = pictures.isNotEmpty())
    return pictures.firstOrNull()?.let {
        ThemeLayer.Picture(
            id = layer.id,
            rect = layer.rect,
            z = layer.z,
            opacity = layer.opacity,
            visibleWhen = layer.visibleWhen,
            source = it,
            fit = ThemeFit.Crop,
            corner = layer.corner,
            fallbacks = pictures.drop(1),
        )
    }
}

private fun placeLayer(layer: ThemeLayer, screen: ThemeScreen, issues: MutableList<ThemeLayoutIssue>): ThemePxRect? {
    val bounds = placeThemeRect(layer.rect, screen.width, screen.height)
    if (bounds == null) {
        issues += ThemeLayoutIssue.InvalidRect(layer.id)
        return null
    }
    val visible = bounds.intersect(screen.bounds)
    if (visible == null) {
        issues += ThemeLayoutIssue.OffScreen(layer.id)
        return null
    }
    val safe = screen.safeArea
    if (safe.contains(visible)) return bounds
    if (!layer.isTextLike()) {
        issues += ThemeLayoutIssue.InSafeZone(layer.id)
        return bounds
    }
    val moved = bounds.moveInto(safe)
    issues += if (moved != null) ThemeLayoutIssue.TextMoved(layer.id) else ThemeLayoutIssue.TextInSafeZone(layer.id)
    return moved ?: bounds
}

private fun ThemeLayer.isTextLike(): Boolean =
    this is ThemeLayer.Text || this is ThemeLayer.Date || this is ThemeLayer.Rating

private fun coversPrimary(layer: ThemeLayer, bounds: ThemePxRect, primary: ThemePxRect): Boolean {
    if (layer.isTextLike() || layer.opacity < COVER_OPACITY || primary.area == 0L) return false
    val overlap = bounds.intersect(primary)?.area ?: return false
    return overlap >= primary.area * COVER_AREA_FRACTION
}

private fun decodeCapFor(layer: ThemeLayer, bounds: ThemePxRect, screen: ThemeScreen): ThemePxSize? {
    val hasPicture = layer is ThemeLayer.Picture || (layer is ThemeLayer.Video && layer.pictures.isNotEmpty())
    if (!hasPicture) return null
    return ThemePxSize(
        width = min(bounds.width, screen.width) * DECODE_CAP_MULTIPLIER,
        height = min(bounds.height, screen.height) * DECODE_CAP_MULTIPLIER,
    )
}

/** What a video shows before it plays or when it has none: [ThemeLayer.Video.fallback], then its fallbacks. */
internal val ThemeLayer.Video.pictures: List<ThemePicture> get() = listOfNotNull(fallback) + fallbacks
