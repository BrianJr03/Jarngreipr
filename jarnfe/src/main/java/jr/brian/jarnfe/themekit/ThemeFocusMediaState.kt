package jr.brian.jarnfe.themekit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import jr.brian.frontend.library.art.GameArt
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.jarnfe.library.jarnLibraryDataOrNull
import jr.brian.jarnfe.library.rememberJarnArtCanLoad
import jr.brian.jarnfe.library.rememberSettled
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** Pictures and video wait this long after focus moves, so scrolling never decodes art. */
internal const val THEME_MEDIA_DEBOUNCE_MILLIS = 200L

/** [focus] once it has rested for [THEME_MEDIA_DEBOUNCE_MILLIS]. New data for the same item passes at once. */
@Composable
internal fun rememberSettledThemeFocus(focus: FocusedItem): FocusedItem =
    rememberSettled(focus, THEME_MEDIA_DEBOUNCE_MILLIS, keyOf = { it.focusKey() })

/**
 * The settled game's media by kind. Read once per view and shared by every layer. Empty while a
 * game is up, for a system, or before the library answers. Kinds a preview hides
 * ([LocalThemeHiddenMedia]) are left out.
 */
@Composable
internal fun rememberThemeFocusMedia(settled: FocusedItem): ThemeFocusMedia {
    val context = LocalContext.current
    val library = remember(context) { context.jarnLibraryDataOrNull() }
    val canLoad = rememberJarnArtCanLoad()
    val gameId = settled.game?.id
    val media by produceState(ThemeFocusMedia(), library, gameId, canLoad) {
        value = ThemeFocusMedia()
        if (library == null || gameId == null || !canLoad) return@produceState
        library.themeArt(gameId).collect { art ->
            value = ThemeFocusMedia(gameId, art.slots.mapValues { it.value.path })
        }
    }
    val hidden = LocalThemeHiddenMedia.current
    return remember(media, hidden) { media.without(hidden) }
}

private fun JarnLibraryData.themeArt(gameKey: String): Flow<GameArt> = flow {
    val game = findGame(gameKey)
    if (game == null) emit(GameArt()) else emitAll(art(game))
}.catch { emit(GameArt()) }
