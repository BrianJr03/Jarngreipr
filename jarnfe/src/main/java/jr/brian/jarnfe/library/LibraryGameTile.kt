package jr.brian.jarnfe.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import jr.brian.core.ui.library.JARN_GAME_TILE_ASPECT
import jr.brian.core.ui.library.JarnGameTile

/**
 * A game's cover with no card behind it. Once the cover loads, the tile takes the cover's own
 * shape inside its [slotAspectRatio] slot, so the focus border wraps the art itself. [artAlpha]
 * dims only the art, for shelf neighbors.
 */
@Composable
internal fun LibraryGameTile(
    game: JarnLibraryGame,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    slotAspectRatio: Float = JARN_GAME_TILE_ASPECT,
    artAlpha: () -> Float = { 1f },
) {
    var artAspect by remember(game.artPath) { mutableStateOf<Float?>(null) }
    JarnGameTile(
        title = game.title,
        onClick = onClick,
        modifier = modifier,
        aspectRatio = slotAspectRatio,
        artAspectRatio = artAspect,
        focusBorder = true,
    ) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = artAlpha() }) {
            LibraryCoverArt(game, onAspectRatio = { artAspect = it })
        }
    }
}
