package jr.brian.frontend.library.art

import jr.brian.frontend.library.db.GamePlayRow

internal const val SHELF_MAX_GAMES = 12
internal const val SHELF_GROUP_GAMES = 5
internal const val SHELF_BROWSE_GAMES = 10

enum class SystemShelfGroup { Recent, MostPlayed, InSystem }

data class SystemShelfGame(val game: LibraryGameWithArt, val group: SystemShelfGroup)

/** [sceneArtPath] is the first shelf game's fanart or screenshot, for the system's backdrop. */
data class SystemShelf(
    val games: List<SystemShelfGame> = emptyList(),
    val sceneArtPath: String? = null,
)

data class SystemActivity(
    val lastPlayedAt: Long? = null,
    val playTimeMinutes: Int = 0,
    val favoriteCount: Int = 0,
)

/** Recent then most played. A system with neither shows its first games A to Z. */
internal fun pickShelf(
    recent: List<LibraryGame>,
    mostPlayed: List<LibraryGame>,
    browse: List<LibraryGame>,
): List<Pair<LibraryGame, SystemShelfGroup>> {
    val latest = recent.take(SHELF_GROUP_GAMES)
    val latestIds = latest.mapTo(HashSet()) { it.id }
    val most = mostPlayed.filterNot { it.id in latestIds }.take(SHELF_GROUP_GAMES)
    if (latest.isEmpty() && most.isEmpty()) {
        return browse.take(SHELF_BROWSE_GAMES).map { it to SystemShelfGroup.InSystem }
    }
    return (latest.map { it to SystemShelfGroup.Recent } + most.map { it to SystemShelfGroup.MostPlayed })
        .take(SHELF_MAX_GAMES)
}

/** [launches] are (system, launch time) pairs already checked against the hide rules. */
internal fun summarizeActivity(
    rows: List<GamePlayRow>,
    launches: List<Pair<String, Long>>,
): Map<String, SystemActivity> {
    val latestLaunch = launches.groupBy({ it.first }, { it.second }).mapValues { it.value.max() }
    val rowsBySystem = rows.groupBy { it.systemId }
    return (rowsBySystem.keys + latestLaunch.keys).associateWith { systemId ->
        val systemRows = rowsBySystem[systemId].orEmpty()
        SystemActivity(
            lastPlayedAt = listOfNotNull(
                systemRows.mapNotNull { it.lastPlayed }.maxOrNull(),
                latestLaunch[systemId],
            ).maxOrNull(),
            playTimeMinutes = systemRows.sumOf { it.playTimeMinutes },
            favoriteCount = systemRows.count { it.isFavorite },
        )
    }
}
