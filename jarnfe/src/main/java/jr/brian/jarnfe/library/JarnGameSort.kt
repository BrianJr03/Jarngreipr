package jr.brian.jarnfe.library

import androidx.annotation.StringRes
import jr.brian.jarnfe.R
import java.util.Locale

/** How a system's games are ordered. Saved by [name], so renaming an entry breaks saved choices. */
enum class JarnGameSort(@param:StringRes val labelRes: Int) {
    Name(R.string.jarnfe_sort_name),
    LastPlayed(R.string.jarnfe_sort_last_played),
    MostPlayed(R.string.jarnfe_sort_most_played),
    ReleaseYear(R.string.jarnfe_sort_release_year);

    fun next(): JarnGameSort = entries[(ordinal + 1) % entries.size]

    companion object {
        val Default = Name

        fun fromId(id: String?): JarnGameSort = entries.firstOrNull { it.name == id } ?: Default
    }
}

private val LeadingArticles = listOf("the ", "a ")

/**
 * What the Name sort orders by, and what letter jump must group by so the two always agree:
 * lowercase, trimmed, with one leading "The" or "A" dropped. "Theme Hospital" keeps its "theme".
 */
fun nameSortKey(title: String): String {
    val lower = title.trim().lowercase(Locale.ROOT)
    val article = LeadingArticles.firstOrNull { lower.startsWith(it) && lower.length > it.length }
    return article?.let { lower.removePrefix(it).trim() } ?: lower
}

/** The same year Now shows for the game, so the order matches what the player reads. */
private fun JarnLibraryGame.year(): Int? = releaseYear(metadata.releaseDate)?.toIntOrNull()

private val ByName: Comparator<JarnLibraryGame> =
    compareBy<JarnLibraryGame> { nameSortKey(it.title) }.thenBy { it.title }.thenBy { it.id }

/** Games with no play data, or no year, go after the rest, in name order. */
internal fun JarnGameSort.comparator(): Comparator<JarnLibraryGame> = when (this) {
    JarnGameSort.Name -> ByName
    JarnGameSort.LastPlayed ->
        compareBy<JarnLibraryGame> { it.metadata.lastPlayedAtMs == null }
            .thenByDescending { it.metadata.lastPlayedAtMs ?: 0L }
            .then(ByName)
    JarnGameSort.MostPlayed ->
        compareByDescending<JarnLibraryGame> { it.metadata.playCount ?: 0 }.then(ByName)
    JarnGameSort.ReleaseYear ->
        compareBy<JarnLibraryGame> { it.year() == null }
            .thenBy { it.year() ?: 0 }
            .then(ByName)
}

internal fun List<JarnLibraryGame>.sortedFor(sort: JarnGameSort): List<JarnLibraryGame> =
    sortedWith(sort.comparator())
