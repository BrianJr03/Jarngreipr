package jr.brian.core.ui.theme

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont

private const val SHARED_PUNCTUATION = ".,:;!?'\"-()&/+#%*"
private const val GLYPH_CACHE_SIZE = 512

private val glyphCache = LruCache<Pair<Typeface, String>, Boolean>(GLYPH_CACHE_SIZE)

private fun Int.isShared(): Boolean =
    Character.isWhitespace(this) || Character.isSpaceChar(this) || SHARED_PUNCTUATION.indexOf(toChar()) >= 0

/** True when [hasGlyph] covers every code point of [text] that is not whitespace or shared punctuation. */
internal fun jarnCoversText(text: String, hasGlyph: (Int) -> Boolean): Boolean =
    text.codePoints().allMatch { it.isShared() || hasGlyph(it) }

/** [themed] when its face draws all of [text], else [base], so a line never mixes fonts. */
internal fun jarnStyleFor(text: String, themed: TextStyle, base: TextStyle, hasGlyph: (Int) -> Boolean): TextStyle =
    if (jarnCoversText(text, hasGlyph)) themed else base

private val coverageCache = LruCache<Int, (Int) -> Boolean>(16)

private fun Context.resourceCoverage(resId: Int): (Int) -> Boolean = coverageCache.get(resId)
    ?: runCatching { resources.openRawResource(resId).use { jarnCmapCoverage(it.readBytes()) } }
        .getOrElse { { false } }
        .also { coverageCache.put(resId, it) }

/** The face's own glyphs for bundled fonts; Paint.hasGlyph (which also counts fallbacks) otherwise. */
private fun glyphCheck(context: Context, family: FontFamily, weight: FontWeight, typeface: Typeface): (Int) -> Boolean {
    val fonts = (family as? FontListFontFamily)?.fonts.orEmpty().filterIsInstance<ResourceFont>()
    val font = fonts.minByOrNull { kotlin.math.abs(it.weight.weight - weight.weight) }
    if (font != null) return context.resourceCoverage(font.resId)
    val paint = Paint().also { it.typeface = typeface }
    return { paint.hasGlyph(String(Character.toChars(it))) }
}

/** The themed style for [text], or the whole of [base] when the theme font is missing any of its glyphs. */
@Composable
fun rememberThemedStyle(text: String, themed: TextStyle, base: TextStyle): TextStyle {
    val family = themed.fontFamily
    if (family == null || family == base.fontFamily || family == JarnBarlowCondensed) return themed
    val context = LocalContext.current
    val weight = themed.fontWeight ?: FontWeight.Normal
    val typeface = LocalFontFamilyResolver.current.resolve(
        family,
        weight,
        themed.fontStyle ?: FontStyle.Normal,
        FontSynthesis.All,
    ).value as? Typeface ?: return themed
    return remember(typeface, text, themed, base) {
        val covered = glyphCache.get(typeface to text)
            ?: jarnCoversText(text, glyphCheck(context, family, weight, typeface)).also { glyphCache.put(typeface to text, it) }
        if (covered) themed else base
    }
}
