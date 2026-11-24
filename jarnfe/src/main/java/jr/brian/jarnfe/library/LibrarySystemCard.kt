package jr.brian.jarnfe.library

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import jr.brian.core.ui.focus.JarnFocusStyle
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.library.JARN_SYSTEM_TILE_ASPECT
import jr.brian.core.ui.theme.JarnTheme

/**
 * A grid system tile with "Show system names" off: the same shape, size and focus as the named
 * tile, with no name and count caption, so the logo takes the whole card.
 */
@Composable
internal fun LibrarySystemCard(system: JarnLibrarySystem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val shape = JarnTheme.tileShape
    Box(
        modifier = modifier
            .aspectRatio(JARN_SYSTEM_TILE_ASPECT)
            .semantics { contentDescription = system.title }
            .jarnInteractive(shape, source, focusStyle = JarnFocusStyle.FloatBorder, onClick = onClick)
            .clip(shape)
            .background(JarnTheme.colors.raised),
    ) { LibrarySystemArt(system, logoAlignment = Alignment.Center) }
}
