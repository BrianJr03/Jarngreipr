package jr.brian.home.ui.screens.rss

internal object RssListKeys {
    const val NOW_PLAYING_HEADER = "now_playing_header"
    const val NOW_PLAYING_PINNED = "now_playing_pinned"
    const val MIXED_HEADER = "mixed_header"
    fun mixedItem(feedUrl: String, id: String) = "mixed_${feedUrl}_${id}"
    fun feedHeader(feedUrl: String) = "header_${feedUrl}"
    fun feedItem(feedUrl: String, id: String) = "${feedUrl}_${id}"
    fun feedSpacer(feedUrl: String) = "spacer_${feedUrl}"
}
