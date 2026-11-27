package jr.brian.frontend.look.esde

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import jr.brian.core.model.theme.ThemeColor
import jr.brian.core.model.theme.ThemeColors
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnLook
import jr.brian.core.ui.theme.JarnPaletteMatch
import jr.brian.frontend.library.systems.SystemCatalog
import java.io.File

/** Something the fallback did that the import report tells the player about. jarnfe words each one. */
sealed interface EsdeFallbackNote {
    /** Which colors the theme kept. [darkVersion] names the theme's dark color scheme when that was used instead. */
    data class Colors(val tier: JarnPaletteMatch, val darkVersion: String? = null) : EsdeFallbackNote

    /** A light theme with no usable dark scheme: only its accent carries over. */
    data object LightThemeAccentOnly : EsdeFallbackNote

    /** An ES-DE system folder Jarn doesn't know, so its art is left out. */
    data class UnknownSystem(val esdeName: String) : EsdeFallbackNote

    data class LogoNotCopied(val systemId: String) : EsdeFallbackNote

    data class BackgroundNotCopied(val systemId: String) : EsdeFallbackNote

    /** SVG logos aren't kept in Jarn themes; Jarn's own logo for the system shows instead. */
    data class SvgNotCopied(val systemId: String) : EsdeFallbackNote

    /** Not a TrueType or OpenType font, too large, or missing Latin letters. */
    data class FontRefused(val path: String) : EsdeFallbackNote

    /** The copy reached [ESDE_ASSET_CAP_BYTES]; the rest of the art is left out. */
    data object AssetsCapped : EsdeFallbackNote
}

/**
 * The parts of an ES-DE theme that don't translate to layout, carried over on their own. The
 * import step combines this with the translator's layout into the final Jarn theme: [colors]
 * become its tokens, [fonts] its type fonts, and [systemArt] per-system art. [assets] lists every
 * file written to the theme folder, relative to it.
 */
data class EsdeFallbackResult(
    val colors: ThemeColors,
    val tier: JarnPaletteMatch,
    val darkVersion: String?,
    val systemArt: Map<String, EsdeSystemArtFiles>,
    val fonts: EsdeFontFiles,
    val assets: List<String>,
    val notes: List<EsdeFallbackNote>,
)

object EsdeThemeFallback {

    /**
     * Builds the fallback for [model], read from [themeDir], with [savedLook] as the floor.
     * [reparse] reads the theme again with another color scheme, so a light theme can use its
     * dark scheme; without it a light theme keeps only its accent. Assets go to [outDir], the
     * theme's install folder. Blocking file IO; call off the main thread.
     */
    fun build(
        model: EsdeThemeModel,
        themeDir: File,
        savedLook: JarnLook,
        catalog: SystemCatalog,
        outDir: File,
        reparse: ((colorScheme: String) -> EsdeThemeModel)? = null,
        copier: EsdeAssetCopier = EsdeAssetCopier(themeDir, outDir),
    ): EsdeFallbackResult {
        val notes = mutableListOf<EsdeFallbackNote>()
        val colors = pickColors(model, savedLook, reparse, notes)

        val (candidates, unknown) = EsdeArtFallback.systemArt(model, catalog)
        notes += unknown.map(EsdeFallbackNote::UnknownSystem)
        val art = copier.copyArt(candidates)
        val fonts = copier.copyFonts(EsdeArtFallback.fonts(model))
        notes += copier.takeNotes()

        return EsdeFallbackResult(
            colors = colors.colors.toThemeColors(),
            tier = colors.tier,
            darkVersion = colors.darkVersion,
            systemArt = art,
            fonts = fonts,
            assets = copier.copied.toList(),
            notes = notes.distinct(),
        )
    }

    private data class Picked(val colors: JarnColors, val tier: JarnPaletteMatch, val darkVersion: String?)

    private fun pickColors(
        model: EsdeThemeModel,
        savedLook: JarnLook,
        reparse: ((String) -> EsdeThemeModel)?,
        notes: MutableList<EsdeFallbackNote>,
    ): Picked {
        val raw = EsdeColorExtractor.extract(model)
        if (raw.isLight) darkScheme(model, savedLook, reparse)?.let { (label, colors) ->
            notes += EsdeFallbackNote.Colors(JarnPaletteMatch.FullPalette, label)
            return Picked(colors, JarnPaletteMatch.FullPalette, label)
        }
        val pick = EsdeColorExtractor.pick(raw, savedLook)
        notes += EsdeFallbackNote.Colors(pick.tier)
        if (raw.isLight && pick.tier == JarnPaletteMatch.AccentOnSavedLook) notes += EsdeFallbackNote.LightThemeAccentOnly
        return Picked(pick.colors, pick.tier, null)
    }

    /** The first other selectable color scheme whose full palette is dark and passes repair, with its label. */
    private fun darkScheme(
        model: EsdeThemeModel,
        savedLook: JarnLook,
        reparse: ((String) -> EsdeThemeModel)?,
    ): Pair<String, JarnColors>? {
        if (reparse == null) return null
        return model.capabilities.colorSchemes.asSequence()
            .filter { it.selectable && it.name != model.selection.colorScheme }
            .mapNotNull { scheme ->
                EsdeColorExtractor.fullColors(EsdeColorExtractor.extract(reparse(scheme.name)), savedLook)
                    ?.let { (scheme.label ?: scheme.name) to it }
            }
            .firstOrNull()
    }
}

/** Jarn colors as theme tokens; the importer writes them, and Jarn repairs them again on load. */
fun JarnColors.toThemeColors() = ThemeColors(
    bg = bg.token(),
    surface = surface.token(),
    raised = raised.token(),
    textHigh = textHigh.token(),
    textMid = textMid.token(),
    textLow = textLow.token(),
    accent = accent.token(),
    danger = danger.token(),
    success = success.token(),
    line = line.token(),
    focus = focus.token(),
)

private fun Color.token() = ThemeColor(toArgb())
