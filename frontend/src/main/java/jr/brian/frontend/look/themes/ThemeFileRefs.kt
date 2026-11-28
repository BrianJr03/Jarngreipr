package jr.brian.frontend.look.themes

import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeSystemArt
import jr.brian.core.model.theme.ThemeTextStyle
import jr.brian.core.model.theme.ThemeViewPatch
import jr.brian.core.model.theme.ViewSpec

/** A theme with every file it names checked, and how many references were dropped to get there. */
internal data class CheckedTheme(val spec: JarnThemeSpec, val dropped: Int)

/**
 * Every font and picture path must name a file that passed its check inside the theme folder.
 * A bad reference drops alone: a picture layer falls to its next good fallback (and goes when it has
 * none), a primary or video keeps Jarn's default art, a text style keeps its role's font, and a
 * system's art keeps only the files that passed. Narrow layouts follow the same rules.
 */
internal fun checkFileRefs(spec: JarnThemeSpec, assets: Map<String, ThemeAssetKind>): CheckedTheme {
    var dropped = 0
    fun ok(path: String, vararg kinds: ThemeAssetKind) = (isSafeThemePath(path) && assets[path] in kinds).also {
        if (!it) dropped++
    }
    fun ThemePicture?.checked(): ThemePicture? =
        if (this is ThemePicture.File && !ok(path, ThemeAssetKind.Image)) null else this
    fun List<ThemePicture>.checked(): List<ThemePicture> = mapNotNull { it.checked() }
    fun ThemeTextStyle?.checked(): ThemeTextStyle? =
        this?.let { style -> style.font?.let { if (ok(it, ThemeAssetKind.Font)) style else style.copy(font = null) } ?: style }

    fun ThemePrimary.checked(): ThemePrimary = when (this) {
        is ThemePrimary.Shelf -> copy(art = art.checked())
        is ThemePrimary.Grid -> copy(art = art.checked())
        is ThemePrimary.List -> copy(art = art.checked())
    }
    fun ThemeLayer.checked(): ThemeLayer? = when (this) {
        is ThemeLayer.Picture -> {
            val pictures = (listOf(source) + fallbacks).checked()
            pictures.firstOrNull()?.let { copy(source = it, fallbacks = pictures.drop(1)) }
        }
        is ThemeLayer.Video -> copy(fallback = fallback.checked(), fallbacks = fallbacks.checked())
        else -> this
    }
    /** A narrow layer that loses every picture hides on narrow screens, rather than showing the wide one. */
    fun ThemeViewPatch.checked(): ThemeViewPatch {
        val kept = layers.map { it to it.checked() }
        val gone = kept.filter { it.second == null }.map { it.first.id }
        return copy(layers = kept.mapNotNull { it.second }, hide = (hide + gone).distinct())
    }
    fun ViewSpec.checked() = copy(
        primary = primary.checked(),
        layers = layers.mapNotNull { it.checked() },
        narrow = narrow?.checked(),
    )

    val type = spec.tokens.type
    val checkedType = type.copy(
        display = type.display.checked(),
        title = type.title.checked(),
        heading = type.heading.checked(),
        body = type.body.checked(),
        label = type.label.checked(),
        labelStrong = type.labelStrong.checked(),
        caption = type.caption.checked(),
        captionStrong = type.captionStrong.checked(),
    )
    val checkedArt = spec.systemArt
        .mapValues { (_, art) ->
            ThemeSystemArt(
                logo = art.logo?.takeIf { ok(it, ThemeAssetKind.Image) },
                background = art.background?.takeIf { ok(it, ThemeAssetKind.Image) },
            )
        }
        .filterValues { it.logo != null || it.background != null }
    val checked = spec.copy(
        tokens = spec.tokens.copy(type = checkedType),
        views = spec.views.copy(systems = spec.views.systems?.checked(), games = spec.views.games?.checked()),
        systemArt = checkedArt,
    )
    return CheckedTheme(checked, dropped)
}
