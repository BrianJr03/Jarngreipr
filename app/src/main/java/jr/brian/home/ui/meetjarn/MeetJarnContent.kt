package jr.brian.home.ui.meetjarn

import android.content.Context
import android.hardware.display.DisplayManager
import androidx.annotation.StringRes
import jr.brian.core.model.ArtSource
import jr.brian.frontend.library.art.VisibleList
import jr.brian.home.R
import jr.brian.jarnfe.library.jarnArtDetectorOrNull
import jr.brian.jarnfe.library.jarnLibraryDataOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** What the first line promises, picked before the screen opens so it never changes while shown. */
enum class MeetJarnLibrary(@param:StringRes val lineRes: Int) {
    EsDeArt(R.string.meet_jarn_library_esde),
    Games(R.string.meet_jarn_library_games),
    NoGames(R.string.meet_jarn_library_none),
}

fun meetJarnLibrary(gameCount: Int, esDeArt: Boolean): MeetJarnLibrary = when {
    gameCount <= 0 -> MeetJarnLibrary.NoGames
    esDeArt -> MeetJarnLibrary.EsDeArt
    else -> MeetJarnLibrary.Games
}

/**
 * The player's own art for the hero: covers for the wall, the last game played for Continue
 * playing and Now, and system logos for the shelf.
 */
data class MeetJarnHeroArt(
    val wall: List<String>,
    val continueTitle: String,
    val continueCover: String,
    val systemLogos: List<String?>,
    val systemName: String?,
)

/** Everything the screen needs, ready before it opens. A null [heroArt] means the bundled picture. */
data class MeetJarnContent(
    val library: MeetJarnLibrary = MeetJarnLibrary.Games,
    val heroArt: MeetJarnHeroArt? = null,
    val twoScreens: Boolean = true,
)

/** Fewer covers than this would leave the wall patchy, so the bundled picture reads better. */
internal const val MEET_JARN_MIN_COVERS = 6
internal const val MEET_JARN_WALL_COVERS = 42
internal const val MEET_JARN_SHELF_SYSTEMS = 5
private const val MEET_JARN_GAMES_TO_SCAN = 120
private const val MEET_JARN_LOAD_LIMIT_MS = 800L

internal fun meetJarnHeroArt(
    covers: List<String>,
    continueTitle: String?,
    continueCover: String?,
    systemLogos: List<String?>,
    systemName: String?,
): MeetJarnHeroArt? {
    if (covers.size < MEET_JARN_MIN_COVERS) return null
    return MeetJarnHeroArt(
        wall = covers.take(MEET_JARN_WALL_COVERS),
        continueTitle = continueTitle ?: return null,
        continueCover = continueCover ?: covers.first(),
        systemLogos = systemLogos.take(MEET_JARN_SHELF_SYSTEMS),
        systemName = systemName,
    )
}

/**
 * Reads the shared library for the screen. A slow library never holds the screen back: past the
 * time limit it opens with the bundled picture and the general games line.
 */
suspend fun loadMeetJarnContent(context: Context): MeetJarnContent {
    val twoScreens = hasSecondScreen(context)
    val loaded = withTimeoutOrNull(MEET_JARN_LOAD_LIMIT_MS) {
        withContext(Dispatchers.IO) { readLibrary(context) }
    }
    return (loaded ?: MeetJarnContent()).copy(twoScreens = twoScreens)
}

private suspend fun readLibrary(context: Context): MeetJarnContent {
    val data = context.jarnLibraryDataOrNull() ?: return MeetJarnContent()
    val games = data.allGames.first().items
    val esDeArt = context.jarnArtDetectorOrNull()?.detected?.value == ArtSource.EsDe
    val library = meetJarnLibrary(games.size, esDeArt)
    if (games.isEmpty()) return MeetJarnContent(library)
    val withArt = data.withArt(flowOf(VisibleList(games.take(MEET_JARN_GAMES_TO_SCAN)))).first().items
    val recent = data.withArt(data.recent(limit = 1)).first().items.firstOrNull()
    val systems = data.systemsWithArt.first().items.take(MEET_JARN_SHELF_SYSTEMS)
    val heroArt = meetJarnHeroArt(
        covers = withArt.mapNotNull { it.artPath },
        continueTitle = recent?.game?.title ?: withArt.firstOrNull { it.artPath != null }?.game?.title,
        continueCover = recent?.artPath,
        systemLogos = systems.map { it.art.logoPath },
        systemName = systems.firstOrNull()?.system?.displayName,
    )
    return MeetJarnContent(library, heroArt)
}

private fun hasSecondScreen(context: Context): Boolean {
    val displays = context.getSystemService(DisplayManager::class.java)?.displays ?: return false
    return displays.size > 1
}
