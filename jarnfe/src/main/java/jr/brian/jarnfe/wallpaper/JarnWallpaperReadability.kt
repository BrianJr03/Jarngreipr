package jr.brian.jarnfe.wallpaper

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.core.ui.theme.jarnBlend
import jr.brian.core.ui.theme.jarnContrastRatio

internal const val JARN_MIN_TEXT_CONTRAST = 4.5

/** What a dimmed control sits on. Provided by a screen that draws over a wallpaper. */
internal val LocalJarnDisabledBacking = staticCompositionLocalOf { Color.Transparent }

/** The brightest a scrimmed image can get: pure white under the scrim. */
internal fun JarnColors.brightestScrimmedBackground(): Color = jarnBlend(artScrim, Color.White)

/** [color] when it stays readable on any scrimmed image, otherwise the high-emphasis text color. */
internal fun JarnColors.readableOverWallpaper(color: Color): Color =
    if (jarnContrastRatio(color, brightestScrimmedBackground()) >= JARN_MIN_TEXT_CONTRAST) color else textHigh

/** Keeps a dimmed row reading as it does on the theme background, whatever is behind it. */
@Composable
internal fun Modifier.jarnDisabledRowBacking(enabled: Boolean): Modifier =
    if (enabled) this else background(LocalJarnDisabledBacking.current, JarnTheme.radius.smShape)
