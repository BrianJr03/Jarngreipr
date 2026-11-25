package jr.brian.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import jr.brian.core.ui.R

/**
 * Command Panel's bundled fonts, keyed as in its look contract (`bundled:<key>`).
 * Licenses: res/raw/jarn_cp_fonts_ofl.txt.
 */
enum class JarnCpFontKey(val key: String) {
    Condensed("condensed"),
    CondensedDisplay("condensed_display"),
    Mono("mono"),
    Segment("segment"),
    Rounded("rounded"),
    Stencil("stencil"),
    Comic("comic"),
    ChromeWide("chrome_wide"),
    Serif("serif");

    companion object {
        private const val BUNDLED_PREFIX = "bundled:"

        fun fromKey(key: String?): JarnCpFontKey? {
            val bare = key?.removePrefix(BUNDLED_PREFIX) ?: return null
            return entries.firstOrNull { it.key == bare }
        }
    }
}

/** Size multipliers per role. Wide faces scale down so lines keep Barlow Condensed's width. */
@Immutable
data class JarnFontScale(
    val display: Float = 1f,
    val title: Float = 1f,
    val heading: Float = 1f,
    val body: Float = 1f,
    val label: Float = 1f,
) {
    companion object {
        val None = JarnFontScale()
    }
}

/**
 * A face Jarn can apply to its type roles.
 * [headingWeight] null keeps Jarn's own display/title/heading weights.
 * [textFace] faces are readable at paragraph size, so they may also take the body role.
 */
@Immutable
data class JarnFontFace(
    val family: FontFamily,
    val labelWeight: FontWeight? = null,
    val labelStrongWeight: FontWeight? = null,
    val headingWeight: FontWeight? = null,
    val style: FontStyle = FontStyle.Normal,
    val textFace: Boolean = false,
    val scale: JarnFontScale = JarnFontScale.None,
)

private val CondensedFamily = FontFamily(
    Font(R.font.jarn_barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.jarn_barlow_condensed_bold, FontWeight.Bold),
    Font(R.font.jarn_cp_condensed_extrabold, FontWeight.ExtraBold),
)

private val MonoFamily = FontFamily(
    Font(R.font.jarn_cp_mono_regular, FontWeight.Normal),
    Font(R.font.jarn_cp_mono_bold, FontWeight.Bold),
)

private val SerifFamily = FontFamily(
    Font(R.font.jarn_cp_serif_regular, FontWeight.Normal),
    Font(R.font.jarn_cp_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

private val RoundedFamily = FontFamily(Font(R.font.jarn_cp_rounded_bold, FontWeight.ExtraBold))
private val StencilFamily = FontFamily(Font(R.font.jarn_cp_stencil, FontWeight.Normal))
private val ComicFamily = FontFamily(Font(R.font.jarn_cp_comic, FontWeight.Normal))
private val ChromeWideFamily = FontFamily(Font(R.font.jarn_cp_chrome_wide, FontWeight.Normal))

private val MonoScale = JarnFontScale(display = 0.62f, title = 0.72f, heading = 0.78f, body = 0.88f, label = 0.9f)
private val SerifScale = JarnFontScale(display = 0.82f, title = 0.86f, heading = 0.9f, body = 0.96f, label = 1f)
private val RoundedScale = JarnFontScale(display = 0.8f, title = 0.84f, heading = 0.88f)
private val ChromeWideScale = JarnFontScale(display = 0.56f, title = 0.62f, heading = 0.66f, label = 0.66f)
private val StencilScale = JarnFontScale(display = 0.78f, title = 0.82f, heading = 0.86f, label = 0.9f)

private fun single(family: FontFamily, weight: FontWeight, scale: JarnFontScale) = JarnFontFace(
    family = family,
    labelWeight = weight,
    labelStrongWeight = weight,
    headingWeight = weight,
    scale = scale,
)

private fun condensed(weight: FontWeight, strong: FontWeight, heading: FontWeight?) = JarnFontFace(
    family = CondensedFamily,
    labelWeight = weight,
    labelStrongWeight = strong,
    headingWeight = heading,
)

private fun mono() = JarnFontFace(
    family = MonoFamily,
    labelWeight = FontWeight.Normal,
    labelStrongWeight = FontWeight.Bold,
    headingWeight = FontWeight.Bold,
    textFace = true,
    scale = MonoScale,
)

private fun serif() = JarnFontFace(
    family = SerifFamily,
    labelWeight = FontWeight.Normal,
    labelStrongWeight = FontWeight.Normal,
    headingWeight = FontWeight.Normal,
    textFace = true,
    scale = SerifScale,
)

/** Null for unknown keys and for DSEG7, which only has digits and is never used for text. */
fun jarnCpFontFace(key: JarnCpFontKey?): JarnFontFace? = when (key) {
    null, JarnCpFontKey.Segment -> null
    JarnCpFontKey.Condensed -> condensed(FontWeight.SemiBold, FontWeight.Bold, null)
    JarnCpFontKey.CondensedDisplay -> condensed(FontWeight.ExtraBold, FontWeight.ExtraBold, FontWeight.ExtraBold)
    JarnCpFontKey.Mono -> mono()
    JarnCpFontKey.Rounded -> single(RoundedFamily, FontWeight.ExtraBold, RoundedScale)
    JarnCpFontKey.Stencil -> single(StencilFamily, FontWeight.Normal, StencilScale)
    JarnCpFontKey.Comic -> single(ComicFamily, FontWeight.Normal, JarnFontScale.None)
    JarnCpFontKey.ChromeWide -> single(ChromeWideFamily, FontWeight.Normal, ChromeWideScale)
    JarnCpFontKey.Serif -> serif()
}

fun jarnCpFontFace(key: String?): JarnFontFace? = jarnCpFontFace(JarnCpFontKey.fromKey(key))

/** An imported font file: no known weights or scale, so Jarn's role weights and sizes stay. */
fun jarnImportedFontFace(family: FontFamily): JarnFontFace = JarnFontFace(family = family)

/**
 * Builds Jarn's type with Command Panel fonts.
 * A bundled key wins; an imported family is used only when its key does not resolve.
 * Display, title and heading take the display face. Labels take the label face.
 * Body switches only for text faces. Captions always stay on [base].
 */
fun jarnTypeWithFonts(
    displayKey: String?,
    labelKey: String?,
    importedDisplay: FontFamily? = null,
    importedLabel: FontFamily? = null,
    base: JarnType = JarnType(),
): JarnType {
    val display = jarnCpFontFace(displayKey) ?: importedDisplay?.let(::jarnImportedFontFace)
    val label = jarnCpFontFace(labelKey) ?: importedLabel?.let(::jarnImportedFontFace)
    return jarnTypeWithFaces(display, label, base)
}

fun jarnTypeWithFaces(display: JarnFontFace?, label: JarnFontFace?, base: JarnType = JarnType()): JarnType {
    val bodyFace = label?.takeIf { it.textFace } ?: display?.takeIf { it.textFace }
    return base.copy(
        display = base.display.withFace(display, display?.headingWeight, display?.scale?.display),
        title = base.title.withFace(display, display?.headingWeight, display?.scale?.title),
        heading = base.heading.withFace(display, display?.headingWeight, display?.scale?.heading),
        body = base.body.withFace(bodyFace, null, bodyFace?.scale?.body),
        label = base.label.withFace(label, label?.labelWeight, label?.scale?.label),
        labelStrong = base.labelStrong.withFace(label, label?.labelStrongWeight, label?.scale?.label),
    )
}

private fun TextStyle.withFace(face: JarnFontFace?, weight: FontWeight?, scale: Float?): TextStyle {
    if (face == null) return this
    val factor = scale ?: 1f
    return copy(
        fontFamily = face.family,
        fontWeight = weight ?: fontWeight,
        fontStyle = face.style,
        fontSize = fontSize * factor,
        lineHeight = lineHeight * factor,
    )
}
