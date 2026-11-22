package jr.brian.core.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.R

const val JARN_ART_SCRIM_ALPHA = 0.78f
const val JARN_CAPTION_SCRIM_ALPHA = 0.82f

val JarnFocusGapColor = Color(0xFF0A0908)
val JarnCaptionScrimColor = Color(0xFF080706).copy(alpha = JARN_CAPTION_SCRIM_ALPHA)
val JarnCaptionTextColor = Color(0xFFE5DED8)

@Immutable
data class JarnColors(
    val bg: Color,
    val surface: Color,
    val raised: Color,
    val line: Color,
    val textHigh: Color,
    val textMid: Color,
    val textLow: Color,
    val accent: Color,
    val onAccent: Color,
    val focus: Color,
    val focusGap: Color,
    val danger: Color,
    val onDanger: Color,
    val success: Color,
    val artScrim: Color,
)

enum class JarnCardStyle { Tonal, Outlined }

@Immutable
data class JarnLook(
    val id: String,
    val colors: JarnColors,
    val tileShape: Shape? = null,
    val cardStyle: JarnCardStyle? = null,
    val focusColor: Color? = null,
    @param:StringRes @get:StringRes val nameRes: Int? = null,
)

val JarnLook.resolvedColors: JarnColors
    get() = focusColor?.let { colors.copy(focus = it) } ?: colors

@Composable
fun JarnLook.displayName(): String = nameRes?.let { stringResource(it) } ?: id

private fun jarnColors(
    bg: Long,
    surface: Long,
    raised: Long,
    line: Long,
    textHigh: Long,
    textMid: Long,
    textLow: Long,
    accent: Long,
    focus: Long,
    danger: Long,
    success: Long,
): JarnColors = JarnColors(
    bg = Color(bg),
    surface = Color(surface),
    raised = Color(raised),
    line = Color(line),
    textHigh = Color(textHigh),
    textMid = Color(textMid),
    textLow = Color(textLow),
    accent = Color(accent),
    onAccent = jarnOnColor(Color(accent)),
    focus = Color(focus),
    focusGap = JarnFocusGapColor,
    danger = Color(danger),
    onDanger = jarnOnColor(Color(danger)),
    success = Color(success),
    artScrim = Color(bg).copy(alpha = JARN_ART_SCRIM_ALPHA),
)

object JarnLooks {
    val Forge = JarnLook(
        id = "forge",
        nameRes = R.string.jarn_look_forge,
        colors = jarnColors(
            bg = 0xFF141211, surface = 0xFF1F1B19, raised = 0xFF2B2522, line = 0xFF4A403A,
            textHigh = 0xFFF6EFE8, textMid = 0xFFC9BDB2, textLow = 0xFFA39689,
            accent = 0xFFFF8B3D, focus = 0xFFFFB070, danger = 0xFFFF7A6B, success = 0xFF7FD99A,
        ),
    )

    val Fjord = JarnLook(
        id = "fjord",
        nameRes = R.string.jarn_look_fjord,
        colors = jarnColors(
            bg = 0xFF0E1419, surface = 0xFF172028, raised = 0xFF212D37, line = 0xFF3C4C59,
            textHigh = 0xFFEAF3F8, textMid = 0xFFB7C7D2, textLow = 0xFF8FA2B0,
            accent = 0xFF5CC8F5, focus = 0xFF9BE0FF, danger = 0xFFFF8177, success = 0xFF71DCA6,
        ),
    )

    val Grove = JarnLook(
        id = "grove",
        nameRes = R.string.jarn_look_grove,
        colors = jarnColors(
            bg = 0xFF10150F, surface = 0xFF1A2118, raised = 0xFF252E22, line = 0xFF424E3D,
            textHigh = 0xFFEFF4E8, textMid = 0xFFC1CCB7, textLow = 0xFF99A68E,
            accent = 0xFFA5D65C, focus = 0xFFCDEE8F, danger = 0xFFFF8570, success = 0xFF86DE8E,
        ),
    )

    val Night = JarnLook(
        id = "night",
        nameRes = R.string.jarn_look_night,
        colors = jarnColors(
            bg = 0xFF000000, surface = 0xFF121212, raised = 0xFF1E1E1E, line = 0xFF424242,
            textHigh = 0xFFFFFFFF, textMid = 0xFFC8C8C8, textLow = 0xFFA0A0A0,
            accent = 0xFFF2E6D0, focus = 0xFFFFFFFF, danger = 0xFFFF7D73, success = 0xFF7EE0A0,
        ),
    )

    val Default: JarnLook = Fjord
    val All: List<JarnLook> = listOf(Fjord, Forge, Grove, Night)

    fun fromId(id: String?): JarnLook = All.firstOrNull { it.id == id } ?: Default
}
