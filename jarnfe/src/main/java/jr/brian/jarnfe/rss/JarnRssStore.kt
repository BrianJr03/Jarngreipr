package jr.brian.jarnfe.rss

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Immutable
internal data class JarnRssReader(
    val item: JarnRssItem,
    val queue: List<JarnRssItem>,
    val watchingVideo: Boolean = false,
) {
    fun stepped(delta: Int): JarnRssReader {
        val index = queue.indexOfFirst { it.key == item.key }
        val next = queue.getOrNull(index + delta)
        return if (index < 0 || next == null) this else copy(item = next, watchingVideo = false)
    }
}

/** Lets RSS sheets draw over the whole Canvas area, so the keyboard is never clipped by the page. */
@Stable
internal class JarnRssSheetSlot {
    var content by mutableStateOf<(@Composable () -> Unit)?>(null)

    @Composable
    fun Host() {
        content?.invoke()
    }
}

@Immutable
internal data class JarnRssScroll(val index: Int = 0, val offset: Int = 0)

/** What the RSS page last showed, so coming back to it skips the loading state and keeps its place. */
internal class JarnRssMemory {
    var feeds: List<JarnRssFeed>? = null
    var items: List<JarnRssItem>? = null
    var history: List<String> = emptyList()
    var focusKey: String? = null
    var rail = JarnRssScroll()
    var articles = JarnRssScroll()
}

/** Shared by both windows, so the Deck list can open an article on the Stage. */
internal object JarnRss {
    var filter by mutableStateOf<JarnRssFilter>(JarnRssFilter.All)
    val memory = JarnRssMemory()
    var reader by mutableStateOf<JarnRssReader?>(null)
        private set

    fun open(item: JarnRssItem, queue: List<JarnRssItem>) {
        reader = JarnRssReader(item, queue)
    }

    fun step(delta: Int) {
        reader = reader?.stepped(delta)
    }

    fun watchVideo(watching: Boolean) {
        reader = reader?.copy(watchingVideo = watching)
    }

    fun close() {
        reader = null
    }
}
