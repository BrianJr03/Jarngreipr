package jr.brian.frontend.look.themes

import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeTextStyle
import jr.brian.core.model.theme.ViewSpec

/** A theme with every file it names checked, and how many references were dropped to get there. */
internal data class CheckedTheme(val spec: JarnThemeSpec, val dropped: Int)

/**
 * Every font and picture path must name a file that passed its check inside the theme folder.
 * A bad reference drops alone: a picture layer goes, a primary or video keeps Jarn's default art,
 * and a text style keeps its role's font.
 */
internal fun checkFileRefs(spec: JarnThemeSpec, assets: Map<String, ThemeAssetKind>): CheckedTheme {
    var dropped = 0
    fun ok(path: String, kind: ThemeAssetKind) = (isSafeThemePath(path) && assets[path] == kind).also {
        if (!it) dropped++
    }
    fun ThemePicture?.checked(): ThemePicture? =
        if (this is ThemePicture.File && !ok(path, ThemeAssetKind.Image)) null else this
    fun ThemeTextStyle?.checked(): ThemeTextStyle? =
        this?.let { style -> style.font?.let { if (ok(it, ThemeAssetKind.Font)) style else style.copy(font = null) } ?: style }

    fun ThemePrimary.checked(): ThemePrimary = when (this) {
        is ThemePrimary.Shelf -> copy(art = art.checked())
        is ThemePrimary.Grid -> copy(art = art.checked())
        is ThemePrimary.List -> copy(art = art.checked())
    }
    fun ThemeLayer.checked(): ThemeLayer? = when (this) {
        is ThemeLayer.Picture -> takeIf { source.checked() != null }?.copy(fallbacks = fallbacks.mapNotNull { it.checked() })
        is ThemeLayer.Video -> copy(fallback = fallback.checked(), fallbacks = fallbacks.mapNotNull { it.checked() })
        else -> this
    }
    fun ViewSpec.checked() = copy(primary = primary.checked(), layers = layers.mapNotNull { it.checked() })

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
    val checked = spec.copy(
        tokens = spec.tokens.copy(type = checkedType),
        views = spec.views.copy(systems = spec.views.systems?.checked(), games = spec.views.games?.checked()),
    )
    return CheckedTheme(checked, dropped)
}
