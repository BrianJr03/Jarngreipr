package jr.brian.jarnfe.themekit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import jr.brian.core.model.theme.ThemeAlign
import jr.brian.core.model.theme.ThemeColorRole
import jr.brian.core.model.theme.ThemeCorner
import jr.brian.core.model.theme.ThemeFit
import jr.brian.core.model.theme.ThemeTextRole
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnTheme

/** Roles read the repaired palette, so a layer recolors with the theme and keeps Jarn's contrast. */
internal fun ThemeColorRole.color(colors: JarnColors): Color = when (this) {
    ThemeColorRole.Bg -> colors.bg
    ThemeColorRole.Surface -> colors.surface
    ThemeColorRole.Raised -> colors.raised
    ThemeColorRole.Line -> colors.line
    ThemeColorRole.TextHigh -> colors.textHigh
    ThemeColorRole.TextMid -> colors.textMid
    ThemeColorRole.TextLow -> colors.textLow
    ThemeColorRole.Accent -> colors.accent
    ThemeColorRole.Danger -> colors.danger
    ThemeColorRole.Success -> colors.success
}

/**
 * Text color on the readable plate. Tonal roles are backgrounds, not text, so they fall back to
 * high text rather than vanish into the plate.
 */
internal fun ThemeColorRole.textColor(colors: JarnColors): Color = when (this) {
    ThemeColorRole.Bg, ThemeColorRole.Surface, ThemeColorRole.Raised, ThemeColorRole.Line -> colors.textHigh
    else -> color(colors)
}

@Composable
internal fun ThemeTextRole.style(): TextStyle {
    val type = JarnTheme.type
    return when (this) {
        ThemeTextRole.Display -> type.display
        ThemeTextRole.Title -> type.title
        ThemeTextRole.Heading -> type.heading
        ThemeTextRole.Body -> type.body
        ThemeTextRole.Label -> type.label
        ThemeTextRole.LabelStrong -> type.labelStrong
        ThemeTextRole.Caption -> type.caption
        ThemeTextRole.CaptionStrong -> type.captionStrong
    }
}

@Composable
internal fun ThemeCorner.shape(): Shape {
    val radius = JarnTheme.radius
    return when (this) {
        ThemeCorner.None -> RectangleShape
        ThemeCorner.Sm -> radius.smShape
        ThemeCorner.Md -> radius.mdShape
        ThemeCorner.Lg -> radius.lgShape
        ThemeCorner.Xl -> radius.xlShape
    }
}

internal val ThemeAlign.textAlign: TextAlign
    get() = when (this) {
        ThemeAlign.Start -> TextAlign.Start
        ThemeAlign.Center -> TextAlign.Center
        ThemeAlign.End -> TextAlign.End
    }

/** Where the plate sits in the layer's rect: on the aligned edge, centered vertically. */
internal val ThemeAlign.plateAlignment: Alignment
    get() = when (this) {
        ThemeAlign.Start -> Alignment.CenterStart
        ThemeAlign.Center -> Alignment.Center
        ThemeAlign.End -> Alignment.CenterEnd
    }

internal val ThemeFit.contentScale: ContentScale
    get() = when (this) {
        ThemeFit.Fit -> ContentScale.Fit
        ThemeFit.Crop -> ContentScale.Crop
        ThemeFit.Fill -> ContentScale.FillBounds
    }
