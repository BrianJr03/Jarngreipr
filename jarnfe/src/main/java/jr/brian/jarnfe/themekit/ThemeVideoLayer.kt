package jr.brian.jarnfe.themekit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import jr.brian.core.model.theme.ThemeCorner
import jr.brian.core.model.theme.ThemeFit
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.LibrarySelection
import jr.brian.jarnfe.library.NowSceneVideo
import jr.brian.jarnfe.library.nowVideoPreview
import jr.brian.jarnfe.look.rememberJarnDisplay
import kotlinx.coroutines.delay

/**
 * The settled game's preview over its fallback picture. It plays through Now's player, so the
 * Video previews setting, sound setting, launch hold, failed-file memory, power gates and Select
 * to hide the UI all apply. Sound needs both the layer's [ThemeLayer.Video.audio] and the setting.
 */
@Composable
internal fun ThemeVideoLayer(layer: ThemeLayer.Video, decor: ThemeDecorFocus, decodeCap: ThemePxSize?) {
    val fallbacks = resolveThemePictures(layer.pictures, decor.settled, decor.media, decor.themeDir)
    val game = rememberDelayedGame(decor.settled.game, layer.delayMs.toLong())
    val display = rememberJarnDisplay()
    val preview = remember(display, layer.audio) {
        display.nowVideoPreview().let { it.copy(muted = it.muted || !layer.audio, crop = true) }
    }
    Box(Modifier.fillMaxSize().clip(layer.corner.shape())) {
        if (fallbacks.isNotEmpty()) ThemePictureLayer(fallbacks, ThemeFit.Crop, ThemeCorner.None, decodeCap)
        if (game != null) NowSceneVideo(LibrarySelection.Game(game), preview)
    }
}

/** [game] once it has held for [delayMs], on top of the player's own start delay. */
@Composable
private fun rememberDelayedGame(game: JarnLibraryGame?, delayMs: Long): JarnLibraryGame? {
    val delayed by produceState<JarnLibraryGame?>(null, game?.id, delayMs) {
        value = null
        if (game == null) return@produceState
        if (delayMs > 0) delay(delayMs)
        value = game
    }
    return delayed?.takeIf { it.id == game?.id }
}
