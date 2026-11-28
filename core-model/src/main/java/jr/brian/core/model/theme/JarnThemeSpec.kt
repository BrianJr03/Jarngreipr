package jr.brian.core.model.theme

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A Jarn theme as stored on disk. Data only: the frontend turns it into colors, type,
 * shapes and layouts, and repairs any colors that miss Jarn's contrast rules.
 *
 * [schema] is always written. Additive changes (a new optional field, a new layer type)
 * keep the schema; anything an older build would misread bumps [CURRENT_SCHEMA].
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class JarnThemeSpec(
    @EncodeDefault val schema: Int = CURRENT_SCHEMA,
    val meta: ThemeMeta,
    val tokens: ThemeTokens,
    val views: ThemeViews = ThemeViews(),
    /** Per-system art the theme brings, by Jarn system id. See [ThemeSystemArt]. */
    val systemArt: Map<String, ThemeSystemArt> = emptyMap(),
) {
    companion object {
        const val CURRENT_SCHEMA = 1
    }
}

/**
 * One system's art from the theme, as file paths relative to the theme folder. When a theme has
 * an entry for a system, its cards and backdrops use only this art, never mixed with another
 * source; a missing [background] shows the system's name over a plain card.
 */
@Serializable
data class ThemeSystemArt(
    val logo: String? = null,
    val background: String? = null,
)

@Serializable
data class ThemeMeta(
    val name: String,
    val author: String? = null,
    val version: String? = null,
    val license: String? = null,
    val source: ThemeSource = ThemeSource.Jarn,
)

/** Where a theme came from. Imports keep their source so sharing can honour the original license. */
@Serializable
enum class ThemeSource {
    @SerialName("jarn") Jarn,
    @SerialName("es-de") EsDe,
}

@Serializable
data class ThemeTokens(
    val colors: ThemeColors,
    val type: ThemeTypeScale = ThemeTypeScale(),
    val radius: ThemeRadius = ThemeRadius(),
)

/**
 * The base colors a theme sets. On-colors, focus gap and the art scrim are always derived.
 * [line] falls back to a tone between surface and text, and [focus] to a lift of the accent.
 */
@Serializable
data class ThemeColors(
    val bg: ThemeColor,
    val surface: ThemeColor,
    val raised: ThemeColor,
    val textHigh: ThemeColor,
    val textMid: ThemeColor,
    val textLow: ThemeColor,
    val accent: ThemeColor,
    val danger: ThemeColor,
    val success: ThemeColor,
    val line: ThemeColor? = null,
    val focus: ThemeColor? = null,
)

/** Mirrors the frontend's type roles. A null role keeps Jarn's own style for it. */
@Serializable
data class ThemeTypeScale(
    val display: ThemeTextStyle? = null,
    val title: ThemeTextStyle? = null,
    val heading: ThemeTextStyle? = null,
    val body: ThemeTextStyle? = null,
    val label: ThemeTextStyle? = null,
    val labelStrong: ThemeTextStyle? = null,
    val caption: ThemeTextStyle? = null,
    val captionStrong: ThemeTextStyle? = null,
)

/** Sizes are in sp. [font] is a font file path relative to the theme folder; null keeps the role's family. */
@Serializable
data class ThemeTextStyle(
    val size: Float,
    val lineHeight: Float? = null,
    val weight: Int = DEFAULT_WEIGHT,
    val font: String? = null,
) {
    companion object {
        const val DEFAULT_WEIGHT = 400
    }
}

/** Corner radii in dp. The defaults match Jarn's own scale. */
@Serializable
data class ThemeRadius(
    val sm: Float = 8f,
    val md: Float = 12f,
    val lg: Float = 16f,
    val xl: Float = 24f,
)

/** A step on the theme's radius scale, so layers round with the rest of the theme. */
@Serializable
enum class ThemeCorner {
    @SerialName("none") None,
    @SerialName("sm") Sm,
    @SerialName("md") Md,
    @SerialName("lg") Lg,
    @SerialName("xl") Xl,
}

/** A theme color role, so layers recolor with the palette instead of carrying their own hex. */
@Serializable
enum class ThemeColorRole {
    @SerialName("bg") Bg,
    @SerialName("surface") Surface,
    @SerialName("raised") Raised,
    @SerialName("line") Line,
    @SerialName("textHigh") TextHigh,
    @SerialName("textMid") TextMid,
    @SerialName("textLow") TextLow,
    @SerialName("accent") Accent,
    @SerialName("danger") Danger,
    @SerialName("success") Success,
}
