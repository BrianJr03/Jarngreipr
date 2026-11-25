package jr.brian.core.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnScrim
import jr.brian.core.ui.components.JarnScrimEdge
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnFocusStyle
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme

private const val HERO_TEXT_FRACTION = 0.55f
private const val HERO_BODY_MAX_LINES = 3

/**
 * A wide card with art behind a bottom-left caption. The caption keeps to the left
 * [HERO_TEXT_FRACTION] of the card unless [artEndWidth] says how much of the right edge the art
 * really covers, in which case it runs up to the art, never narrower than the default.
 */
@Composable
fun JarnHeroPanel(
    eyebrow: String?,
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    artEndWidth: Dp? = null,
    titleStyle: TextStyle = JarnTheme.type.display,
    art: @Composable BoxScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val source = remember { MutableInteractionSource() }
    val shape = JarnTheme.tileShape
    BoxWithConstraints(
        modifier = modifier
            .then(onClick?.let { Modifier.jarnInteractive(shape, source, focusStyle = JarnFocusStyle.ScaleBorder, onClick = it) } ?: Modifier)
            .clip(shape)
            .background(JarnTheme.colors.raised),
    ) {
        val textWidth = heroTextWidth(maxWidth, artEndWidth)
        JarnArtSlot(captioned = true, art = art)
        JarnScrim(JarnScrimEdge.Left, Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .align(AbsoluteAlignment.BottomLeft)
                .width(textWidth)
                .padding(JarnTheme.space.stageMargin),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            eyebrow?.let { JarnHeroEyebrow(it) }
            JarnBalancedTitle(
                title = title,
                style = titleStyle,
                color = JarnTheme.colors.textHigh,
                modifier = Modifier.weight(1f, fill = false),
            )
            body?.let {
                JarnText(
                    text = it,
                    style = JarnTheme.type.body,
                    color = JarnTheme.colors.textMid,
                    maxLines = HERO_BODY_MAX_LINES,
                )
            }
            footer()
        }
    }
}

internal fun heroTextWidth(panelWidth: Dp, artEndWidth: Dp?): Dp {
    val default = panelWidth * HERO_TEXT_FRACTION
    val beforeArt = artEndWidth?.let { panelWidth - it } ?: return default
    return maxOf(default, beforeArt)
}

@Composable
private fun JarnHeroEyebrow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
    ) {
        Box(
            Modifier
                .size(width = JarnTheme.space.lg, height = JarnTheme.space.activeBarHeight)
                .background(JarnTheme.colors.accent, RoundedCornerShape(2.dp)),
        )
        JarnText(text, JarnTheme.type.captionStrong, JarnTheme.colors.textHigh, maxLines = 1)
    }
}
