package jr.brian.core.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnScrim
import jr.brian.core.ui.components.JarnScrimEdge
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme

private const val HERO_TEXT_FRACTION = 0.55f
private const val HERO_BODY_MAX_LINES = 3

@Composable
fun JarnHeroPanel(
    eyebrow: String?,
    title: String,
    body: String?,
    modifier: Modifier = Modifier,
    art: @Composable BoxScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .clipToBounds()
            .background(JarnTheme.colors.raised),
    ) {
        JarnArtSlot(captioned = true, art = art)
        JarnScrim(JarnScrimEdge.Left, Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .align(AbsoluteAlignment.BottomLeft)
                .fillMaxWidth(HERO_TEXT_FRACTION)
                .padding(JarnTheme.space.stageMargin),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            eyebrow?.let { JarnHeroEyebrow(it) }
            JarnBalancedTitle(
                title = title,
                style = JarnTheme.type.display,
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
