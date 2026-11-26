package jr.brian.jarnfe.library

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnCoverGlow

private const val SHELF_WASH_ALPHA = 0.22f
private val ShelfWashBlur = 48.dp

/** The centered item's art, for a glow drawn behind the whole screen. */
internal class LibraryGlowSource(val owner: Any, val art: @Composable BoxScope.() -> Unit)

/**
 * Where a shelf hands its glow, so it can light the full screen, header included, rather than
 * stop at the shelf's top. A shelf leaving clears only its own glow, so one sliding in keeps its.
 */
@Stable
internal class LibraryShelfGlow {
    var source by mutableStateOf<LibraryGlowSource?>(null)
        private set

    val isShowing: Boolean get() = source != null

    fun show(next: LibraryGlowSource) {
        source = next
    }

    fun clear(owner: Any) {
        if (source?.owner === owner) source = null
    }
}

/** No holder with the glow off, so shelves hand nothing over and nothing is drawn or sampled. */
internal fun libraryShelfGlowFor(choice: JarnCoverGlow): LibraryShelfGlow? = when (choice) {
    JarnCoverGlow.Off -> null
    JarnCoverGlow.Full -> LibraryShelfGlow()
}

internal val LocalLibraryShelfGlow = staticCompositionLocalOf<LibraryShelfGlow?> { null }

/** Draws the shelf's glow across [modifier], crossfading between items. */
@Composable
internal fun LibraryShelfGlowLayer(glow: LibraryShelfGlow, modifier: Modifier = Modifier) {
    Crossfade(
        targetState = glow.source,
        modifier = modifier,
        animationSpec = JarnTheme.motion.art.spec(),
        label = "shelfGlow",
    ) { shown -> if (shown != null) LibraryShelfWashArt(shown.art) }
}

/** Hands the centered art to the screen's glow. With the cover glow off there is none, and no wash. */
@Composable
internal fun <T> LibraryShelfWash(item: T, content: LibraryShelfContent<T>) {
    val glow = LocalLibraryShelfGlow.current ?: return
    val owner = remember { Any() }
    val key = content.itemKey(item)
    DisposableEffect(glow, key) {
        glow.show(LibraryGlowSource(owner) { content.art(this, item) })
        onDispose { }
    }
    DisposableEffect(glow) { onDispose { glow.clear(owner) } }
}

@Composable
private fun LibraryShelfWashArt(art: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .alpha(SHELF_WASH_ALPHA)
            .blur(ShelfWashBlur),
        content = art,
    )
}
