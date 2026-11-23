package jr.brian.core.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme

private const val PLACEHOLDER_TITLE_MAX_LINES = 3

internal val LocalJarnArtCaptioned = staticCompositionLocalOf { false }

@Composable
fun JarnArtPlaceholder(title: String, modifier: Modifier = Modifier) {
    val showTitle = !LocalJarnArtCaptioned.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarnTheme.colors.raised)
            .padding(JarnTheme.space.sm),
        contentAlignment = Alignment.Center,
    ) {
        if (showTitle) {
            JarnText(
                text = title,
                style = JarnTheme.type.labelStrong,
                color = JarnTheme.colors.textHigh,
                maxLines = PLACEHOLDER_TITLE_MAX_LINES,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun JarnArtSlot(captioned: Boolean, art: @Composable BoxScope.() -> Unit) {
    CompositionLocalProvider(LocalJarnArtCaptioned provides captioned) {
        Box(Modifier.fillMaxSize().clearAndSetSemantics {}, content = art)
    }
}
