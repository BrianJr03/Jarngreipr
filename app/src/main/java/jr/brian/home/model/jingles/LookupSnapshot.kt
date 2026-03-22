package jr.brian.home.model.jingles

import jr.brian.home.esde.model.JingleSource
import jr.brian.home.model.JingleEntry

data class LookupSnapshot(
    val sources: List<JingleSource> = emptyList(),
    val regexEntries: List<Triple<Regex, JingleSource, JingleEntry>> = emptyList(),
    val exactMap: Map<String, Pair<JingleSource, JingleEntry>> = emptyMap(),
    val lowerEntries: List<Triple<String, JingleSource, JingleEntry>> = emptyList(),
    val branchMap: Map<String, String> = emptyMap(),
    /** Keyed by lowercase platform name (e.g. "n3ds"). First match wins. */
    val systemJingles: Map<String, Pair<JingleSource, JingleEntry>> = emptyMap(),
    /** Played for any game when no other tier matches. */
    val globalDefault: Pair<JingleSource, JingleEntry>? = null,
)