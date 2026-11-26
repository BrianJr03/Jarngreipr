package jr.brian.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme

/** Tints a toast's icon: the theme's quiet text, its accent, or its danger colour. */
enum class JarnToastTone { Neutral, Accent, Warning }

private val ToastIconSize = 28.dp
private val ToastElevation = 8.dp

/**
 * The look of a corner toast: a raised rounded card with an icon, a title and an optional subtitle.
 * TalkBack reads it politely when it appears. It holds no state; see the host that queues toasts.
 */
@Composable
fun JarnCornerToastCard(
    icon: Painter,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tone: JarnToastTone = JarnToastTone.Accent,
) {
    val shape = JarnTheme.radius.lgShape
    JarnSurface(
        modifier = modifier
            .shadow(ToastElevation, shape)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = JarnTheme.space.lg, vertical = JarnTheme.space.md),
            horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(tone.color()),
                modifier = Modifier.size(ToastIconSize),
            )
            ToastText(title, subtitle)
        }
    }
}

@Composable
private fun ToastText(title: String, subtitle: String?) {
    Column {
        JarnText(title, JarnTheme.type.labelStrong, JarnTheme.colors.textHigh, maxLines = 1)
        subtitle?.let { JarnText(it, JarnTheme.type.caption, JarnTheme.colors.textMid, maxLines = 1) }
    }
}

@Composable
private fun JarnToastTone.color(): Color = when (this) {
    JarnToastTone.Neutral -> JarnTheme.colors.textMid
    JarnToastTone.Accent -> JarnTheme.colors.accent
    JarnToastTone.Warning -> JarnTheme.colors.danger
}
