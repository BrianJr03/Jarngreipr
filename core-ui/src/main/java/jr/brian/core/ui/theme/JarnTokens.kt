package jr.brian.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.core.ui.R

@Immutable
data class JarnSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val stageMargin: Dp = 24.dp,
    val deckMargin: Dp = 16.dp,
    val focusEdge: Dp = 20.dp,
    val hintBarHeight: Dp = 40.dp,
    val activeBarHeight: Dp = 3.dp,
)

@Immutable
data class JarnRadius(
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
) {
    val smShape = RoundedCornerShape(sm)
    val mdShape = RoundedCornerShape(md)
    val lgShape = RoundedCornerShape(lg)
    val xlShape = RoundedCornerShape(xl)
    val sheetShape = RoundedCornerShape(topStart = lg, topEnd = lg)
}

val JarnBarlowCondensed = FontFamily(
    Font(R.font.jarn_barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.jarn_barlow_condensed_bold, FontWeight.Bold),
)

private val NoFontPadding = PlatformTextStyle(includeFontPadding = false)

private fun barlow(weight: FontWeight, size: Int, line: Int) = TextStyle(
    fontFamily = JarnBarlowCondensed,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    platformStyle = NoFontPadding,
)

private fun sans(weight: FontWeight, size: Int, line: Int) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    platformStyle = NoFontPadding,
)

@Immutable
data class JarnType(
    val display: TextStyle = barlow(FontWeight.Bold, 56, 58),
    val title: TextStyle = barlow(FontWeight.SemiBold, 32, 34),
    val heading: TextStyle = barlow(FontWeight.SemiBold, 24, 26),
    val body: TextStyle = sans(FontWeight.Normal, 16, 22),
    val label: TextStyle = sans(FontWeight.Medium, 14, 18),
    val labelStrong: TextStyle = sans(FontWeight.SemiBold, 14, 18),
    val caption: TextStyle = sans(FontWeight.Normal, 12, 16),
    val captionStrong: TextStyle = sans(FontWeight.SemiBold, 12, 16),
)
