package jr.brian.jarnfe.search

import jr.brian.jarnfe.library.JarnLibrarySystem
import java.text.Normalizer
import java.util.Locale

private const val COMMAND_PREFIX = '@'
private val CombiningMarks = Regex("\\p{Mn}+")
private val Whitespace = Regex("\\s+")
private val WordBreak = Regex("[^\\p{L}\\p{N}]+")

private const val RANK_EXACT = 0
private const val RANK_PREFIX = 1
private const val RANK_WORD = 2
private const val RANK_CONTAINS = 3

/** Plain [terms] match titles. Each `@name` in [systemFilters] narrows results to matching systems. */
internal data class JarnSearchQuery(
    val terms: List<String> = emptyList(),
    val systemFilters: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = terms.isEmpty() && systemFilters.isEmpty()
}

internal fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(CombiningMarks, "")
        .lowercase(Locale.ROOT)
        .trim()

internal fun parseJarnSearchQuery(raw: String): JarnSearchQuery {
    val tokens = normalizeForSearch(raw).split(Whitespace).filter { it.isNotEmpty() }
    val (commands, terms) = tokens.partition { it.first() == COMMAND_PREFIX }
    return JarnSearchQuery(
        terms = terms,
        systemFilters = commands.map { it.drop(1) }.filter { it.isNotEmpty() },
    )
}

/** Lower is better. Null when any term is missing from [title]. */
internal fun rankTitle(title: String, terms: List<String>): Int? {
    val normalized = normalizeForSearch(title)
    if (terms.any { it !in normalized }) return null
    val phrase = terms.joinToString(" ")
    val words = normalized.split(WordBreak)
    return when {
        normalized == phrase -> RANK_EXACT
        normalized.startsWith(phrase) -> RANK_PREFIX
        terms.all { term -> words.any { it.startsWith(term) } } -> RANK_WORD
        else -> RANK_CONTAINS
    }
}

/** Best matches first, then by title. An empty term list keeps every item. */
internal fun <T> rankByTitle(items: List<T>, terms: List<String>, title: (T) -> String): List<T> =
    items.mapNotNull { item -> rankTitle(title(item), terms)?.let { item to it } }
        .sortedWith(compareBy({ it.second }, { normalizeForSearch(title(it.first)) }))
        .map { it.first }

/** The system ids a query may return, or null when every system is allowed. */
internal fun allowedSystemIds(
    query: JarnSearchQuery,
    systems: List<JarnLibrarySystem>,
    scopeSystemId: String?,
): Set<String>? {
    val filtered = if (query.systemFilters.isEmpty()) {
        null
    } else {
        systems.filter { system ->
            val id = normalizeForSearch(system.id)
            val title = normalizeForSearch(system.title)
            query.systemFilters.any { it in id || it in title }
        }.mapTo(HashSet()) { it.id }
    }
    return when {
        scopeSystemId == null -> filtered
        filtered == null -> setOf(scopeSystemId)
        else -> filtered.intersect(setOf(scopeSystemId))
    }
}

internal fun searchSystems(
    systems: List<JarnLibrarySystem>,
    query: JarnSearchQuery,
    allowed: Set<String>?,
): List<JarnLibrarySystem> {
    if (query.isEmpty) return emptyList()
    val candidates = if (allowed == null) systems else systems.filter { it.id in allowed }
    return rankByTitle(candidates, query.terms) { it.title }
}

/** [systemId] and [title] read each game, so this stays independent of the game model. */
internal fun <T> searchGames(
    games: List<T>,
    query: JarnSearchQuery,
    allowed: Set<String>?,
    limit: Int,
    systemId: (T) -> String,
    title: (T) -> String,
): List<T> {
    if (query.isEmpty) return emptyList()
    val candidates = if (allowed == null) games else games.filter { systemId(it) in allowed }
    return rankByTitle(candidates, query.terms, title).take(limit)
}

/** Apps have no system, so a scoped or `@system` search finds games and systems only. */
internal fun <T> searchApps(
    apps: List<T>,
    query: JarnSearchQuery,
    allowed: Set<String>?,
    limit: Int,
    label: (T) -> String,
): List<T> {
    if (query.terms.isEmpty() || allowed != null) return emptyList()
    return rankByTitle(apps, query.terms, label).take(limit)
}
