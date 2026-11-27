package jr.brian.frontend.look.esde

import jr.brian.core.model.MediaKind
import jr.brian.core.model.theme.ThemeAlign
import jr.brian.core.model.theme.ThemeRect

private const val DEFAULT_ASPECT = 16f / 9f
private const val MIN_EXTENT = 0.01f

/** One element as every player system sees it. Layout comes from the first system that has it. */
internal class EsdeElementAcrossSystems(
    val key: EsdeElementKey,
    val bySystem: List<Pair<EsdeSystemRef, Map<String, String>>>,
    allSystemCount: Int,
) {
    val props: Map<String, String> = bySystem.first().second
    val onEverySystem: Boolean = bySystem.size == allSystemCount
    val systemNames: List<String> get() = bySystem.map { it.first.name }

    fun varies(property: String): Boolean = bySystem.map { it.second[property] }.distinct().size > 1

    fun float(property: String): Float? = props.float(property)
    fun pair(property: String): Pair<Float, Float>? = props.pair(property)
    fun bool(property: String): Boolean? = props[property]?.trim()?.lowercase()?.toBooleanStrictOrNull()
}

/** Every element key of [view] across [systems], in first-seen order. */
internal fun EsdeThemeModel.elementsIn(view: EsdeView): List<EsdeElementAcrossSystems> {
    val themes = systems.values.toList()
    val keys = LinkedHashSet<EsdeElementKey>()
    themes.forEach { theme -> theme.views[view]?.elements?.keys?.let(keys::addAll) }
    return keys.map { key ->
        val present = themes.mapNotNull { theme -> theme.views[view]?.elements?.get(key)?.let { theme.system to it } }
        EsdeElementAcrossSystems(key, present, themes.size)
    }
}

internal fun Map<String, String>.float(property: String): Float? = this[property]?.trim()?.toFloatOrNull()

/** ES-DE writes pairs as `"x y"`. */
internal fun Map<String, String>.pair(property: String): Pair<Float, Float>? {
    val parts = this[property]?.trim()?.split(Regex("\\s+")) ?: return null
    if (parts.size != 2) return null
    val first = parts[0].toFloatOrNull() ?: return null
    val second = parts[1].toFloatOrNull() ?: return null
    return first to second
}

/** `16:9` as a width over height ratio; ES-DE's vertical forms (`9:16`) work the same way. */
internal fun aspectOf(ratio: String?): Float {
    val parts = ratio?.split(':')?.mapNotNull { it.trim().toFloatOrNull() } ?: return DEFAULT_ASPECT
    if (parts.size != 2 || parts[1] <= 0f) return DEFAULT_ASPECT
    return parts[0] / parts[1]
}

/**
 * pos / size / origin as a [ThemeRect]. A zero or missing side is guessed so the box keeps
 * [fallbackSize]'s other side; [guessed] reports whether that happened.
 */
internal data class EsdeRect(val rect: ThemeRect, val guessed: Boolean)

internal fun EsdeElementAcrossSystems.rect(fallbackSize: Pair<Float, Float>): EsdeRect {
    val (x, y) = pair("pos") ?: (0f to 0f)
    val (originX, originY) = pair("origin") ?: (0f to 0f)
    val size = pair("size") ?: pair("maxSize") ?: pair("cropSize")
    val width = size?.first?.takeIf { it > 0f }
    val height = size?.second?.takeIf { it > 0f }
    return EsdeRect(
        rect = ThemeRect(
            x = x,
            y = y,
            width = (width ?: fallbackSize.first).coerceAtLeast(MIN_EXTENT),
            height = (height ?: fallbackSize.second).coerceAtLeast(MIN_EXTENT),
            originX = originX,
            originY = originY,
        ),
        guessed = width == null || height == null,
    )
}

internal fun ThemeRect.top(): Float = y - originY * height

internal fun alignOf(value: String?): ThemeAlign = when (value?.trim()?.lowercase()) {
    "center" -> ThemeAlign.Center
    "right" -> ThemeAlign.End
    else -> ThemeAlign.Start
}

/** ES-DE's image types in their fallback order; null for kinds Jarn doesn't track. */
internal fun mediaKindsOf(imageType: String?): List<MediaKind?> =
    imageType.orEmpty().split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() && it != "none" }.map {
        when (it) {
            "cover" -> MediaKind.Cover
            "marquee" -> MediaKind.Marquee
            "fanart" -> MediaKind.Fanart
            "screenshot" -> MediaKind.Screenshot
            "titlescreen" -> MediaKind.Titlescreen
            "miximage" -> MediaKind.Miximage
            "physicalmedia" -> MediaKind.PhysicalMedia
            else -> null
        }
    }

/** Theme picture paths drop the parser's `./` prefix. */
internal fun themeFilePath(value: String): String = value.removePrefix("./")
