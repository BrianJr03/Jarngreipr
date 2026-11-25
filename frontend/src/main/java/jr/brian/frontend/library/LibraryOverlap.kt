package jr.brian.frontend.library

import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.GameMerge
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SourceGameCount
import jr.brian.frontend.library.setup.locationContains
import jr.brian.frontend.library.setup.sourceLocation

/**
 * Innermost folders first, so a system's own folder is scanned before the
 * library root that holds it, whatever order the sources were added in.
 */
internal fun innermostFirst(sources: List<SourceEntity>): List<SourceEntity> =
    sources.sortedWith(
        compareByDescending<SourceEntity> { locationDepth(sourceLocation(it.treeUri)) }
            .thenBy { it.id },
    )

/** Systems that a source nested inside [source] already holds, so [source] skips them. */
internal fun systemsCoveredInside(
    source: SourceEntity,
    sources: List<SourceEntity>,
    counts: List<SourceGameCount>,
): Set<String> {
    val location = sourceLocation(source.treeUri) ?: return emptySet()
    val insideIds = sources.filter { other ->
        val otherLocation = sourceLocation(other.treeUri)
        other.id != source.id && otherLocation != null && otherLocation != location &&
            locationContains(location, otherLocation)
    }.mapTo(HashSet()) { it.id }
    return counts.filter { it.sourceId in insideIds && it.gameCount > 0 }
        .mapTo(HashSet()) { it.systemId }
}

/**
 * Groups copies of one game found through folders that overlap. The copy from
 * the innermost folder is kept and takes on every copy's user data.
 */
internal fun planOverlapMerges(
    duplicates: List<GameEntity>,
    sources: List<SourceEntity>,
): List<GameMerge> {
    val locations = sources.associate { it.id to sourceLocation(it.treeUri) }
    return duplicates.groupBy { it.systemId to it.relativePath }.values.flatMap { copies ->
        val ordered = copies.sortedWith(
            compareByDescending<GameEntity> { locationDepth(locations[it.sourceId]) }.thenBy { it.id },
        )
        val clusters = mutableListOf<MutableList<GameEntity>>()
        for (game in ordered) {
            val location = locations[game.sourceId]
            val cluster = location?.let { loc ->
                clusters.firstOrNull { cluster ->
                    locations[cluster.first().sourceId]?.let { overlaps(it, loc) } == true
                }
            }
            if (cluster != null) cluster += game else clusters += mutableListOf(game)
        }
        clusters.filter { it.size > 1 }.map { cluster ->
            val drops = cluster.drop(1)
            GameMerge(keep = mergeUserData(cluster.first(), drops), dropIds = drops.map { it.id })
        }
    }
}

/** Sums play stats, keeps any favorite, and hides only when every copy was hidden. */
internal fun mergeUserData(keep: GameEntity, drops: List<GameEntity>): GameEntity {
    val all = listOf(keep) + drops
    fun <T> first(pick: (GameEntity) -> T?): T? = all.firstNotNullOfOrNull(pick)
    return keep.copy(
        absolutePath = first { it.absolutePath },
        description = first { it.description },
        rating = first { it.rating },
        releaseDate = first { it.releaseDate },
        developer = first { it.developer },
        publisher = first { it.publisher },
        genre = first { it.genre },
        players = first { it.players },
        isFavorite = all.any { it.isFavorite },
        isHidden = all.all { it.isHidden },
        playCount = all.sumOf { it.playCount },
        playTimeMinutes = all.sumOf { it.playTimeMinutes },
        lastPlayed = all.mapNotNull { it.lastPlayed }.maxOrNull(),
        crc32 = first { it.crc32 },
        md5 = first { it.md5 },
        fileSizeBytes = first { it.fileSizeBytes },
        emulatorPackage = first { it.emulatorPackage },
        launchCommand = first { it.launchCommand },
        emulatorCore = first { it.emulatorCore },
    )
}

/**
 * A scanned source left with no games because every system it held has its
 * own folder source inside it, e.g. the parent saved by a single-system pick.
 */
internal fun findRedundantParents(
    sources: List<SourceEntity>,
    counts: List<SourceGameCount>,
): List<SourceEntity> {
    val withGames = counts.filter { it.gameCount > 0 }.mapTo(HashSet()) { it.sourceId }
    return sources.filter { parent ->
        val location = sourceLocation(parent.treeUri) ?: return@filter false
        parent.lastScannedAt > 0L && parent.id !in withGames &&
            sources.any { child ->
                child.id != parent.id && child.id in withGames &&
                    sourceLocation(child.treeUri)?.substringBeforeLast('/') == location
            }
    }
}

private fun overlaps(a: String, b: String) = locationContains(a, b) || locationContains(b, a)

private fun locationDepth(location: String?): Int = location?.count { it == '/' } ?: -1
