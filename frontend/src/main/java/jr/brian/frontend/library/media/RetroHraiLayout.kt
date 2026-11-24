package jr.brian.frontend.library.media

import java.io.File

/**
 * RetroHrai's `media/<system>/<folder>/` convention, read only by Jarn FE.
 * Classic never receives this layout, so its art is unaffected.
 *
 * RetroHrai has no title screen, mix image, physical media, or video
 * folders, and calls marquees `logos`. Files carry a per-kind suffix:
 * `<rom>_logo_0.png`, `<rom>_fanart_0.jpg`, `<rom>_ss_2.png`, and a user's
 * own pick as `<rom>_usr_cover_0.png`. Covers are usually a bare `<rom>.png`.
 */
class RetroHraiLayout : MediaLayout {
    override fun mediaDir(root: File, systemFolder: String, kind: MediaKind): File? =
        folderFor(kind)?.let { File(File(root, systemFolder), it) }

    companion object {
        private const val TIER_USER = 0
        private const val TIER_EXACT = 1
        private const val TIER_NUMBERED = 2

        /** RetroHrai folders whose name is not a catalog id or alias. */
        val EXTRA_SYSTEM_FOLDERS: Map<String, List<String>> =
            mapOf("xbox360" to listOf("custom_x360"))

        fun folderFor(kind: MediaKind): String? = when (kind) {
            MediaKind.Cover -> "covers"
            MediaKind.Marquee -> "logos"
            MediaKind.Fanart -> "fanart"
            MediaKind.Screenshot -> "screenshots"
            MediaKind.Titlescreen,
            MediaKind.Miximage,
            MediaKind.PhysicalMedia,
            MediaKind.Video -> null
        }

        /**
         * Every lookup key [stem] answers to. The literal stem always counts,
         * so a ROM really named `Game_ss_1` still finds its own file.
         */
        internal fun matches(kind: MediaKind, stem: String): List<StemMatch> {
            val lower = stem.lowercase()
            val exact = StemMatch(lower, TIER_EXACT, 0)
            val token = suffixToken(kind) ?: return listOf(exact)
            val found = Regex("^(.+?)_(usr_)?${token}_(\\d+)$").find(lower)
                ?: return listOf(exact)
            val (base, user, number) = found.destructured
            val ordinal = number.toIntOrNull() ?: Int.MAX_VALUE
            val tier = if (user.isNotEmpty()) TIER_USER else TIER_NUMBERED
            return listOf(exact, StemMatch(base, tier, ordinal))
        }

        private fun suffixToken(kind: MediaKind): String? = when (kind) {
            MediaKind.Cover -> "cover"
            MediaKind.Marquee -> "logo"
            MediaKind.Fanart -> "fanart"
            MediaKind.Screenshot -> "ss"
            else -> null
        }
    }
}

/** A lookup key for one file. Lower [tier], then lower [ordinal], wins. */
internal data class StemMatch(val key: String, val tier: Int, val ordinal: Int)
