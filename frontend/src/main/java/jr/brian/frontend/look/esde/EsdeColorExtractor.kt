package jr.brian.frontend.look.esde

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnLook
import jr.brian.core.ui.theme.JarnPaletteMatch
import jr.brian.core.ui.theme.adaptedTo
import jr.brian.core.ui.theme.jarnRelativeLuminance
import jr.brian.core.ui.theme.readableJarnColors
import jr.brian.core.ui.theme.resolvedColors

private const val HEX_RGB = 6
private const val HEX_RGBA = 8
private const val MIN_OPAQUE_ALPHA = 0x80
private const val SURFACE_STEP = 0.06f
private const val RAISED_STEP = 0.12f
private const val LINE_STEP = 0.25f
private const val TEXT_MID_STEP = 0.22f
private const val TEXT_LOW_STEP = 0.35f
private const val LIGHT_BACKGROUND = 0.4
private const val FULL_SCREEN = "1 1"
private const val LABEL_PREFIX = "md_lbl_"
private val LIST_TYPES = setOf("textlist", "grid", "carousel")
private val TEXT_TYPES = setOf("text", "datetime")

/** Colors an ES-DE theme states for each Jarn role, before repair. Null when the theme says nothing usable. */
data class EsdeRawPalette(
    val bg: Color? = null,
    val surface: Color? = null,
    val textHigh: Color? = null,
    val textMid: Color? = null,
    val textLow: Color? = null,
    val accent: Color? = null,
) {
    /** Dark text on a light background: Jarn has no light mode, so the full palette can't be used. */
    val isLight: Boolean
        get() {
            val background = bg ?: return false
            val text = textHigh
            val luminance = jarnRelativeLuminance(background)
            return luminance > LIGHT_BACKGROUND || (text != null && jarnRelativeLuminance(text) < luminance)
        }
}

/** The colors a fallback lands on, and how much of the theme they kept. */
data class EsdeColorPick(val colors: JarnColors, val tier: JarnPaletteMatch)

/**
 * Reads role colors from an ES-DE theme's elements in the import plan's order (§3.2), then runs
 * Jarn's shared repair. ES-DE has no palette block, so colors are guessed from where themes
 * usually put them. Danger and success always keep the saved look's hues.
 */
object EsdeColorExtractor {

    fun extract(model: EsdeThemeModel): EsdeRawPalette {
        val elements = model.allElements()
        return EsdeRawPalette(
            bg = elements.firstColor({ it.key.type == "image" && it.key.name == "background" }, "color")
                ?: elements.firstColor({ it.key.type == "image" && it.props["size"] == FULL_SCREEN }, "color"),
            surface = elements.firstColor({ it.key.type in LIST_TYPES }, "backgroundColor"),
            textHigh = elements.firstColor({ it.key.type == "textlist" }, "primaryColor")
                ?: elements.mostCommonColor { it.view == EsdeView.Gamelist && it.key.type in TEXT_TYPES && !it.isLabel },
            textMid = elements.firstColor({ it.key.type == "textlist" }, "secondaryColor")
                ?: elements.firstColor({ it.isLabel }, "color"),
            accent = elements.firstColor({ it.key.type == "textlist" }, "selectedColor")
                ?: elements.firstColor({ it.key.type == "textlist" }, "selectorColor")
                ?: elements.firstColor({ it.key.type == "carousel" || it.key.type == "grid" }, "selectorColor")
                ?: elements.firstColor({ it.key.type == "grid" }, "imageSelectedColor")
                ?: elements.firstColor({ it.key.type == "helpsystem" }, "iconColor"),
        )
    }

    /**
     * The full palette when it is dark and passes repair, else the theme's accent on [savedLook],
     * else [savedLook] as it is.
     */
    fun pick(raw: EsdeRawPalette, savedLook: JarnLook): EsdeColorPick {
        val adapted = savedLook.adaptedTo(fullColors(raw, savedLook), raw.accent)
        return EsdeColorPick(adapted.look.resolvedColors, adapted.match)
    }

    /** Null for a light theme or one whose text fails Jarn's contrast rules. */
    fun fullColors(raw: EsdeRawPalette, savedLook: JarnLook): JarnColors? {
        val bg = raw.bg ?: return null
        val textHigh = raw.textHigh ?: return null
        if (raw.isLight) return null
        val saved = savedLook.resolvedColors
        val surface = raw.surface ?: lerp(bg, textHigh, SURFACE_STEP)
        return readableJarnColors(
            bg = bg,
            surface = surface,
            raised = lerp(surface, textHigh, RAISED_STEP - SURFACE_STEP),
            line = lerp(surface, textHigh, LINE_STEP),
            textHigh = textHigh,
            textMid = raw.textMid ?: lerp(textHigh, bg, TEXT_MID_STEP),
            textLow = raw.textLow ?: lerp(textHigh, bg, TEXT_LOW_STEP),
            accent = raw.accent ?: saved.accent,
            danger = saved.danger,
            success = saved.success,
        )
    }
}

/** `RRGGBB` or `RRGGBBAA`, as ES-DE writes colors. Mostly transparent colors can't carry a role, so they are null. */
internal fun parseEsdeColor(value: String?): Color? {
    val hex = value?.trim()?.removePrefix("#") ?: return null
    if (hex.length != HEX_RGB && hex.length != HEX_RGBA) return null
    val bits = hex.toLongOrNull(16) ?: return null
    val alpha = if (hex.length == HEX_RGBA) (bits and 0xFF).toInt() else 0xFF
    if (alpha < MIN_OPAQUE_ALPHA) return null
    val rgb = if (hex.length == HEX_RGBA) bits ushr 8 else bits
    return Color(0xFF000000 or rgb)
}

/** One element of one view, wherever it came from. */
internal data class EsdeElement(val view: EsdeView, val key: EsdeElementKey, val props: Map<String, String>) {
    val isLabel: Boolean get() = key.name.startsWith(LABEL_PREFIX)
}

/** Every element of every system and view, in model order: system view first, then gamelist. */
internal fun EsdeThemeModel.allElements(): List<EsdeElement> = systems.values.flatMap { system ->
    EsdeView.entries.flatMap { view ->
        system.views[view]?.elements.orEmpty().map { (key, props) -> EsdeElement(view, key, props) }
    }
}

private fun List<EsdeElement>.firstColor(match: (EsdeElement) -> Boolean, property: String): Color? =
    asSequence().filter(match).mapNotNull { parseEsdeColor(it.props[property]) }.firstOrNull()

private fun List<EsdeElement>.mostCommonColor(match: (EsdeElement) -> Boolean): Color? =
    filter(match).mapNotNull { parseEsdeColor(it.props["color"]) }
        .groupingBy { it }.eachCount()
        .maxByOrNull { it.value }?.key
