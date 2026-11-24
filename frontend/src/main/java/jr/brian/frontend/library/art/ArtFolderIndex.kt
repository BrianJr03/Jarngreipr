package jr.brian.frontend.library.art

import android.util.Log
import jr.brian.frontend.library.media.MediaKind
import jr.brian.frontend.library.media.MediaLayout
import jr.brian.frontend.library.media.RetroHraiLayout
import jr.brian.frontend.library.media.StemMatch
import java.io.File

/**
 * Read-only index of one art source. Separate from `MediaIndex` so Classic's
 * first-write-wins resolution stays untouched. Ranking is deterministic and
 * does not depend on `listFiles` order.
 */
internal class ArtFolderIndex private constructor(
    private val files: Map<Key, String>,
    val fileCount: Int,
) {
    fun find(systemFolders: List<String>, basenames: List<String>, kind: MediaKind): String? {
        for (folder in systemFolders) {
            for (name in basenames) {
                files[Key(folder.lowercase(), kind, name.lowercase())]?.let { return it }
            }
        }
        return null
    }

    private data class Key(val system: String, val kind: MediaKind, val name: String)

    private data class Rank(
        val root: Int,
        val layout: Int,
        val tier: Int,
        val ordinal: Int,
        val fileName: String,
    ) : Comparable<Rank> {
        override fun compareTo(other: Rank): Int = compareValuesBy(
            this, other, Rank::root, Rank::layout, Rank::tier, Rank::ordinal, Rank::fileName,
        )
    }

    companion object {
        private const val TAG = "ArtFolderIndex"

        fun build(
            roots: List<File>,
            layouts: List<MediaLayout>,
            systems: Set<String>,
        ): ArtFolderIndex {
            val best = HashMap<Key, Pair<Rank, String>>()
            val seen = HashSet<String>()
            roots.forEachIndexed { rootIndex, root ->
                if (!root.isDirectory) return@forEachIndexed
                layouts.forEachIndexed { layoutIndex, layout ->
                    for (system in systems) {
                        for (kind in MediaKind.entries) {
                            val dir = layout.mediaDir(root, system, kind) ?: continue
                            for (file in listFiles(dir)) {
                                seen += file.absolutePath
                                for (match in matchesFor(layout, kind, file)) {
                                    val key = Key(system.lowercase(), kind, match.key)
                                    val rank = Rank(
                                        rootIndex, layoutIndex, match.tier, match.ordinal, file.name,
                                    )
                                    val current = best[key]
                                    if (current == null || rank < current.first) {
                                        best[key] = rank to file.absolutePath
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return ArtFolderIndex(best.mapValues { it.value.second }, seen.size)
        }

        private fun listFiles(dir: File): List<File> =
            runCatching { dir.listFiles() }
                .onFailure { Log.w(TAG, "listFiles failed for $dir", it) }
                .getOrNull()
                .orEmpty()
                .filter { it.isFile && it.nameWithoutExtension.isNotEmpty() }

        private fun matchesFor(layout: MediaLayout, kind: MediaKind, file: File): List<StemMatch> =
            if (layout is RetroHraiLayout) {
                RetroHraiLayout.matches(kind, file.nameWithoutExtension)
            } else {
                listOf(StemMatch(file.nameWithoutExtension.lowercase(), 0, 0))
            }
    }
}
