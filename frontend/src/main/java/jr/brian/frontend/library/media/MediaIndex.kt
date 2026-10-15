package jr.brian.frontend.library.media

import android.util.Log
import java.io.File

/**
 * Resolves one game's media paths across every configured media root
 * in a single pass. Each media directory is `listFiles()`-ed exactly
 * once per index build — the cost drops from "roots × system aliases ×
 * 5 extensions × 7 folders" per game (today's probe shape in
 * `findFirstMedia`) to "N × 7 listFiles total, lookup by basename".
 * That's the perf fix for plan problem #3.
 *
 * ### Build pattern
 *
 * The index is built per scan: `MediaIndex(roots, layouts, systems)`
 * walks every (root × layout × system × kind) tuple exactly once and
 * stashes the resulting `basename → absolute path` maps. Game-time
 * lookup is then a HashMap probe keyed by the game's basename.
 *
 * The index does NOT retain file handles — all probes happen eagerly
 * at construction. That means a freshly-scraped file is only seen if
 * the index is rebuilt. Repository rebuilds the index at the start of
 * every scan, which is where `mediaDirMtimes` on [SystemStamp] catches
 * drift to invalidate per-system caches.
 */
class MediaIndex private constructor(
    private val byKind: Map<ResolveKey, Map<String, String>>,
) {

    /**
     * Resolved paths for one game. All fields may be null; the index
     * never fabricates a path. A null everywhere means the index saw
     * no art for this basename in any (root × layout × system × kind)
     * tuple it was built with — which is the right signal to the
     * scraper that this game needs attention.
     */
    data class Resolved(
        val cover: String? = null,
        val marquee: String? = null,
        val fanart: String? = null,
        val screenshot: String? = null,
        val titlescreen: String? = null,
        val miximage: String? = null,
        val physicalMedia: String? = null,
        val video: String? = null,
    ) {
        companion object {
            val EMPTY = Resolved()
        }
    }

    /**
     * Look up every known media file for [systemFolder] / [basename].
     * Case-insensitive — ES-DE sometimes capitalises filenames
     * (`Super Mario 64.png` vs `super mario 64.png`) and the two
     * shouldn't miss each other.
     */
    fun resolve(systemFolder: String, basename: String): Resolved {
        if (basename.isEmpty()) return Resolved.EMPTY
        val lower = basename.lowercase()
        fun lookup(kind: MediaKind): String? =
            byKind[ResolveKey(systemFolder.lowercase(), kind)]?.get(lower)
        return Resolved(
            cover = lookup(MediaKind.Cover),
            marquee = lookup(MediaKind.Marquee),
            fanart = lookup(MediaKind.Fanart),
            screenshot = lookup(MediaKind.Screenshot),
            titlescreen = lookup(MediaKind.Titlescreen),
            miximage = lookup(MediaKind.Miximage),
            physicalMedia = lookup(MediaKind.PhysicalMedia),
            video = lookup(MediaKind.Video),
        )
    }

    private data class ResolveKey(val systemFolder: String, val kind: MediaKind)

    companion object {
        private const val TAG = "MediaIndex"

        /**
         * Build an index over every (root × layout × system × kind)
         * tuple. [systems] is the set of system folder names the index
         * should cover — pass every folder you plan to resolve games
         * from, including the system id itself and any folder aliases
         * the catalog knows about, so a scanner that found roms in a
         * `famicom/` folder can look up media stored under `nes/`.
         */
        fun build(
            mediaRoots: List<File>,
            layouts: List<MediaLayout>,
            systems: Set<String>,
        ): MediaIndex {
            val out = HashMap<ResolveKey, HashMap<String, String>>()
            for (root in mediaRoots) {
                if (!root.exists() || !root.isDirectory) continue
                for (layout in layouts) {
                    for (sys in systems) {
                        val sysLower = sys.lowercase()
                        for (kind in MediaKind.entries) {
                            val dir = layout.mediaDir(root, sys, kind) ?: continue
                            val files = runCatching { dir.listFiles() }
                                .onFailure { Log.w(TAG, "listFiles failed for $dir", it) }
                                .getOrNull()
                                .orEmpty()
                                .filter { it.isFile }
                            if (files.isEmpty()) continue
                            val bucket = out.getOrPut(ResolveKey(sysLower, kind)) { HashMap() }
                            for (file in files) {
                                val base = file.nameWithoutExtension.lowercase()
                                if (base.isEmpty()) continue
                                // First-write-wins across layouts and roots: a user who
                                // scraped locally AND imported ES-DE art prefers the
                                // first configured root. The repository passes layouts
                                // in a fixed order so this is predictable.
                                bucket.putIfAbsent(base, file.absolutePath)
                            }
                        }
                    }
                }
            }
            return MediaIndex(out)
        }

        /** Empty index — resolve() always returns Resolved.EMPTY. */
        fun empty(): MediaIndex = MediaIndex(emptyMap())
    }
}
