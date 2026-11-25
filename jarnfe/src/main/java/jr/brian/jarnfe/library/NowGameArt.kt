package jr.brian.jarnfe.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import jr.brian.frontend.library.art.GameArt
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.frontend.library.media.MediaKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** [isLoaded] tells a game with no art apart from one whose art is still being read. */
@Immutable
internal data class NowGameArt(
    val marqueePath: String? = null,
    val coverPath: String? = null,
    val mediaPath: String? = null,
    val isLoaded: Boolean = false,
)

/** Art the user chose as the full-screen image isn't repeated in the foreground. */
internal fun nowGameArt(slots: Map<MediaKind, String>, wideArtPath: String?): NowGameArt {
    fun path(kind: MediaKind) = slots[kind]?.takeIf { it.isNotBlank() }
    return NowGameArt(
        marqueePath = path(MediaKind.Marquee)?.takeUnless { it == wideArtPath },
        coverPath = path(MediaKind.Cover),
        mediaPath = path(MediaKind.PhysicalMedia)?.takeUnless { it == wideArtPath },
        isLoaded = true,
    )
}

/**
 * Pass a settled game. The result never holds another game's art. [canLoad] is true for a page
 * meant to show beside a running game, which the usual gate would keep empty.
 */
@Composable
internal fun rememberNowGameArt(game: JarnLibraryGame, canLoad: Boolean = rememberJarnArtCanLoad()): NowGameArt {
    val context = LocalContext.current
    val library = remember(context) { context.jarnLibraryDataOrNull() }
    val wideArt = rememberGameWideArt(game, canLoad)
    val loaded by produceState<Pair<String, Map<MediaKind, String>>?>(null, library, game.id, canLoad) {
        when {
            library == null -> value = game.id to emptyMap()
            canLoad -> library.nowArt(game.id).collect { art ->
                value = game.id to art.slots.mapValues { it.value.path }
            }
        }
    }
    val slots = loaded?.takeIf { it.first == game.id }?.second
    return remember(slots, wideArt) { slots?.let { nowGameArt(it, wideArt) } ?: NowGameArt() }
}

private fun JarnLibraryData.nowArt(gameKey: String): Flow<GameArt> = flow {
    val game = findGame(gameKey)
    if (game == null) emit(GameArt()) else emitAll(art(game))
}.catch { emit(GameArt()) }
