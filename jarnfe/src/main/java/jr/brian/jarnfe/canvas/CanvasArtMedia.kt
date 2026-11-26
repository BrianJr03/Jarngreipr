package jr.brian.jarnfe.canvas

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import jr.brian.core.model.GameImageType
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.library.art.GameArt
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.frontend.library.media.MediaKind
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.EsdeContentScale
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.LibrarySelection
import jr.brian.jarnfe.library.jarnLibraryDataOrNull
import jr.brian.jarnfe.session.JarnSessionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

private const val ART_LOAD_DELAY_MS = 120L
private const val SVG_SUFFIX = ".svg"

private val StillArtFallback = listOf(
    MediaKind.Cover,
    MediaKind.Miximage,
    MediaKind.Screenshot,
    MediaKind.Titlescreen,
    MediaKind.Fanart,
    MediaKind.Marquee,
    MediaKind.PhysicalMedia,
)

/** The image types a Frontend art tile can show. Classic can save exactly these. */
internal val CanvasArtMediaTypes: List<GameImageType> = GameImageType.randomizableTypes()

/**
 * The art Stage is showing, which Frontend art tiles mirror. [media] is empty for a system.
 * [isPending] while a game's media is still being read: no path is known yet, so tiles keep what
 * they show instead of falling back to some other kind of art.
 */
internal data class CanvasStageArt(
    val title: String,
    val path: String?,
    val logoPath: String? = null,
    val media: Map<MediaKind, String> = emptyMap(),
    val isPending: Boolean = false,
) {
    fun pathFor(type: GameImageType): String? =
        if (isPending) null else canvasArtPath(type, media, path, logoPath)
}

/** A finished media lookup, tagged with the game it belongs to. */
internal data class CanvasArtLookup(val gameKey: String, val media: Map<MediaKind, String>)

/** Uses [lookup] only when it is this game's, so another game's media never shows for it. */
internal fun canvasGameStageArt(
    gameKey: String,
    title: String,
    defaultPath: String?,
    lookup: CanvasArtLookup?,
    lookupRuns: Boolean,
): CanvasStageArt {
    val own = lookup?.takeIf { it.gameKey == gameKey }
    return CanvasStageArt(
        title = title,
        path = defaultPath,
        media = own?.media.orEmpty(),
        isPending = lookupRuns && own == null,
    )
}

/** A choice shown on the tile but not saved yet. */
internal data class CanvasArtPreview(val itemId: String, val type: GameImageType)

internal fun GameImageType.toMediaKind(): MediaKind? = when (this) {
    GameImageType.Screenshots -> MediaKind.Screenshot
    GameImageType.Fanart -> MediaKind.Fanart
    GameImageType.TitleScreens -> MediaKind.Titlescreen
    GameImageType.Covers -> MediaKind.Cover
    GameImageType.PhysicalMedia -> MediaKind.PhysicalMedia
    GameImageType.Marquee -> MediaKind.Marquee
    GameImageType.MixImages -> MediaKind.Miximage
    GameImageType.None,
    GameImageType.All,
    GameImageType.Description -> null
}

/** The chosen art, then the selection's own art, then any still image it has. */
internal fun canvasArtPath(
    type: GameImageType,
    media: Map<MediaKind, String>,
    defaultPath: String?,
    logoPath: String?,
): String? {
    val kind = type.toMediaKind()
    val chosen = kind?.let(media::get)
    val preferred = if (kind == MediaKind.Marquee) {
        listOf(chosen, logoPath, defaultPath)
    } else {
        listOf(chosen, defaultPath, logoPath)
    }
    return preferred.firstOrNull { !it.isNullOrBlank() }
        ?: StillArtFallback.firstNotNullOfOrNull { fallback -> media[fallback]?.takeIf(String::isNotBlank) }
}

@StringRes
internal fun canvasArtMediaLabel(type: GameImageType): Int? = when (type) {
    GameImageType.Screenshots -> R.string.jarnfe_game_art_screenshot
    GameImageType.Fanart -> R.string.jarnfe_game_art_fanart
    GameImageType.TitleScreens -> R.string.jarnfe_game_art_titlescreen
    GameImageType.Covers -> R.string.jarnfe_game_art_cover
    GameImageType.PhysicalMedia -> R.string.jarnfe_game_art_physical
    GameImageType.Marquee -> R.string.jarnfe_game_art_marquee
    GameImageType.MixImages -> R.string.jarnfe_game_art_mixed
    GameImageType.None,
    GameImageType.All,
    GameImageType.Description -> null
}

/** Switching to Marquee shows it whole. Any other choice, or keeping Marquee, leaves Image fit alone. */
internal fun CanvasItem.EsdeArtItem.withMediaType(type: GameImageType): CanvasItem.EsdeArtItem {
    val switchedToMarquee = type == GameImageType.Marquee && resolvedImageType != GameImageType.Marquee
    return copy(imageType = type, contentScale = if (switchedToMarquee) EsdeContentScale.FIT else contentScale)
}

internal fun CanvasItem.withArtPreview(preview: CanvasArtPreview?): CanvasItem =
    if (this is CanvasItem.EsdeArtItem && preview?.itemId == id) withMediaType(preview.type) else this

/** True once the saved tile matches the preview, or the tile is gone. */
internal fun CanvasArtPreview.isSettled(items: List<CanvasItem>): Boolean {
    val tile = items.firstOrNull { it.id == itemId } as? CanvasItem.EsdeArtItem ?: return true
    return tile.resolvedImageType == type
}

private fun JarnLibraryData.artFor(gameKey: String): Flow<GameArt> = flow {
    emitAll(findGame(gameKey)?.let(::art) ?: flowOf(GameArt()))
}.catch { emit(GameArt()) }

/**
 * One lookup for the selected game, shared by every art tile on the page. It waits briefly so
 * scrolling through a list doesn't read art for each game passed.
 */
@Composable
internal fun rememberCanvasStageArt(state: JarnSessionState, page: JarnCanvasPage): CanvasStageArt? {
    val selection = JarnLibrary.state.selection(state.stageRoute, state.librarySection)
    val context = LocalContext.current
    val library = remember(context) { context.jarnLibraryDataOrNull() }
    val hasArtTile = page.layout.items.any { it is CanvasItem.EsdeArtItem }
    val gameKey = (selection as? LibrarySelection.Game)?.game?.id?.takeIf { hasArtTile }
    var lookup by remember { mutableStateOf<CanvasArtLookup?>(null) }
    LaunchedEffect(library, gameKey) {
        if (library == null || gameKey == null) return@LaunchedEffect
        delay(ART_LOAD_DELAY_MS)
        library.artFor(gameKey).collect { art ->
            lookup = CanvasArtLookup(gameKey, art.slots.mapValues { it.value.path })
        }
    }
    return when (selection) {
        is LibrarySelection.Game -> canvasGameStageArt(
            gameKey = selection.game.id,
            title = selection.game.title,
            defaultPath = selection.game.artPath,
            lookup = lookup,
            lookupRuns = library != null && gameKey != null,
        )
        is LibrarySelection.System ->
            CanvasStageArt(selection.system.title, selection.system.artPath, selection.system.logoPath)
        is LibrarySelection.Collection, LibrarySelection.None -> null
    }
}

/**
 * System logos can be SVGs, which need their own decoder. [previousPath] stays on screen while
 * [path] loads, and the swap is a short crossfade, or instant with reduced motion.
 */
@Composable
internal fun rememberCanvasArtModel(path: String?, previousPath: String? = null): Any? {
    val context = LocalContext.current
    val fadeMillis = JarnTheme.motion.small.durationMillis
    return remember(context, path, previousPath, fadeMillis) {
        path?.let {
            ImageRequest.Builder(context)
                .data(it)
                .placeholderMemoryCacheKey(previousPath?.takeIf { previous -> previous != it })
                .crossfade(fadeMillis)
                .apply { if (it.endsWith(SVG_SUFFIX, ignoreCase = true)) decoderFactory(SvgDecoder.Factory()) }
                .build()
        }
    }
}
