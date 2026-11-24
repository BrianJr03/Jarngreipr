package jr.brian.jarnfe.rss

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnRevealLazyItems
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.LocalJarnInputRouter
import jr.brian.jarnfe.input.LocalJarnScreen
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.JarnSessionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AUTO_REFRESH_TICK_MS = 60_000L
private const val RAIL_WIDTH_FRACTION = 0.34f
private const val FOCUS_ADD = "rail:add"
private const val FOCUS_PLAYER = "player"
private val ArticleThumbSize = 56.dp

private fun JarnRssFilter.focusKey(): String = when (this) {
    JarnRssFilter.All -> "rail:all"
    JarnRssFilter.Audio -> "rail:audio"
    JarnRssFilter.History -> "rail:history"
    is JarnRssFilter.Feed -> "rail:feed:$url"
}

private fun JarnRssItem.focusKey() = "item:$key"

@Stable
private class RssBrowserState {
    var focusKey by mutableStateOf<String?>(null)
    var focusFirstArticle by mutableStateOf(false)
    val railList = LazyListState()
    val articleList = LazyListState()
    private val requesters = mutableMapOf<String, FocusRequester>()

    fun requester(key: String): FocusRequester = requesters.getOrPut(key) { FocusRequester() }

    suspend fun restore(railKeys: List<String>, articles: List<JarnRssItem>, hasPlayer: Boolean, fallback: String) {
        val articleKeys = articles.map { it.focusKey() }
        val saved = focusKey?.takeIf { it in railKeys || it in articleKeys || (it == FOCUS_PLAYER && hasPlayer) }
        val target = saved ?: fallback
        reveal(articleList, articleKeys.indexOf(target))
        reveal(railList, railKeys.indexOf(target))
        withFrameNanos { }
        runCatching { requester(target).requestFocus() }
    }

    private suspend fun reveal(list: LazyListState, index: Int) {
        if (index >= 0 && list.layoutInfo.visibleItemsInfo.none { it.index == index }) list.scrollToItem(index)
    }
}

/** The RSS Canvas page. [blocked] is true while a Canvas sheet sits on top of it. */
@Composable
internal fun JarnRssRoute(
    state: JarnSessionState,
    blocked: Boolean,
    sheetSlot: JarnRssSheetSlot,
    onPageOptions: () -> Unit,
) {
    val context = LocalContext.current
    val data = remember(context) { context.jarnRssDataOrNull() }
    if (data == null) {
        JarnEmptyState(
            title = stringResource(R.string.jarnfe_rss_unavailable_title),
            message = stringResource(R.string.jarnfe_rss_unavailable_message),
        )
        return
    }
    val feeds by remember(data) { data.feeds }.collectAsState(initial = null)
    val items by remember(data) { data.items }.collectAsState(initial = null)
    val history by remember(data) { data.historyIds }.collectAsState(initial = emptyList())
    val playback by remember(data) { data.playback }.collectAsState(initial = null)
    val browser = remember { RssBrowserState() }
    DisposableEffect(Unit) { onDispose { JarnRss.close() } }
    RssAutoRefresh(data, feeds.orEmpty())

    val loadedFeeds = feeds
    val loadedItems = items
    val reader = JarnRss.reader
    when {
        loadedFeeds == null || loadedItems == null -> JarnText(
            text = stringResource(R.string.jarnfe_rss_loading),
            style = JarnTheme.type.body,
            color = JarnTheme.colors.textMid,
        )
        reader != null && !state.deckDisplayAvailable -> JarnRssReaderPanel(data, reader)
        else -> RssBrowser(
            data, browser, loadedFeeds, loadedItems, history, playback, blocked, sheetSlot, onPageOptions,
        )
    }
}

@Composable
private fun RssAutoRefresh(data: JarnRssData, feeds: List<JarnRssFeed>) {
    val latest by rememberUpdatedState(feeds)
    LaunchedEffect(data) {
        while (true) {
            delay(AUTO_REFRESH_TICK_MS)
            val now = System.currentTimeMillis()
            latest.filter { it.isDue(now) }.forEach { feed -> launch { data.refreshFeed(feed.url) } }
        }
    }
}

@Composable
private fun RssBrowser(
    data: JarnRssData,
    browser: RssBrowserState,
    feeds: List<JarnRssFeed>,
    items: List<JarnRssItem>,
    history: List<String>,
    playback: JarnRssPlayback?,
    blocked: Boolean,
    sheetSlot: JarnRssSheetSlot,
    onPageOptions: () -> Unit,
) {
    var sheet by remember { mutableStateOf<RssSheet?>(null) }
    val scope = rememberCoroutineScope()
    val filter = jarnRssValidFilter(JarnRss.filter, feeds)
    val articles = remember(items, filter, history) { jarnRssVisibleItems(items, filter, history) }
    val feedTitles = remember(feeds) { feeds.associate { it.url to it.title } }
    val railKeys = remember(feeds) {
        listOf(JarnRssFilter.All, JarnRssFilter.Audio, JarnRssFilter.History).map { it.focusKey() } +
            feeds.map { JarnRssFilter.Feed(it.url).focusKey() } + FOCUS_ADD
    }
    val openReader: (JarnRssItem, Boolean) -> Unit = { item, watch ->
        JarnRss.open(item, articles)
        if (watch) JarnRss.watchVideo(true)
        JarnSession.update { activate(JarnScreen.Stage) }
    }
    val actions = RssSheetActions(
        data = data,
        scope = scope,
        feeds = feeds,
        articles = articles,
        onSheet = { sheet = it },
        onPageOptions = onPageOptions,
        onRead = { openReader(it, false) },
        onWatch = { openReader(it, true) },
    )
    RssBrowserInput(browser, feeds, articles, playback != null, blocked || sheet != null) { sheet = it }
    RssBrowserFocus(browser, railKeys, articles, playback != null, filter, blocked || sheet != null)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = JarnTheme.space.deckMargin),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
    ) {
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        ) {
            RssFeedRail(
                browser = browser,
                feeds = feeds,
                filter = filter,
                onSelect = { selected ->
                    JarnRss.filter = selected
                    browser.focusFirstArticle = true
                },
                onAdd = { sheet = RssSheet.Add },
                modifier = Modifier.fillMaxWidth(RAIL_WIDTH_FRACTION).fillMaxHeight(),
            )
            RssArticlePane(
                browser = browser,
                articles = articles,
                feedTitles = feedTitles,
                hasFeeds = feeds.isNotEmpty(),
                onOpen = { openReader(it, false) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        playback?.let {
            JarnRssPlayerBar(
                data = data,
                playback = it,
                playFocus = browser.requester(FOCUS_PLAYER),
                onFocused = { browser.focusKey = FOCUS_PLAYER },
            )
        }
    }
    val latestActions by rememberUpdatedState(actions)
    DisposableEffect(sheetSlot, sheet) {
        val open = sheet
        sheetSlot.content = open?.let { current -> { RssSheetHost(current, latestActions) } }
        onDispose { sheetSlot.content = null }
    }
}

@Composable
private fun RssBrowserInput(
    browser: RssBrowserState,
    feeds: List<JarnRssFeed>,
    articles: List<JarnRssItem>,
    hasPlayer: Boolean,
    blocked: Boolean,
    onSheet: (RssSheet) -> Unit,
) {
    val focusKey = browser.focusKey
    val focusedFeed = feeds.firstOrNull { JarnRssFilter.Feed(it.url).focusKey() == focusKey }
    val focusedItem = articles.firstOrNull { it.focusKey() == focusKey }
    JarnInputLayer(
        enabled = !blocked,
        bindings = {
            when {
                focusedItem != null -> {
                    hint(JarnAction.Confirm, R.string.jarnfe_rss_hint_read)
                    on(JarnAction.Context, R.string.jarnfe_hint_context) {
                        onSheet(RssSheet.Item(focusedItem.key))
                    }
                }
                focusedFeed != null -> {
                    hint(JarnAction.Confirm, R.string.jarnfe_rss_hint_select)
                    on(JarnAction.Context, R.string.jarnfe_hint_context) {
                        onSheet(RssSheet.Feed(focusedFeed.url))
                    }
                }
                focusKey != FOCUS_PLAYER -> hint(JarnAction.Confirm, R.string.jarnfe_rss_hint_select)
            }
            on(JarnAction.Menu, R.string.jarnfe_rss_hint_menu) { onSheet(RssSheet.Menu) }
            if (hasPlayer) {
                on(JarnAction.Search, R.string.jarnfe_rss_hint_player) {
                    runCatching { browser.requester(FOCUS_PLAYER).requestFocus() }
                }
            }
        },
    )
}

@Composable
private fun RssBrowserFocus(
    browser: RssBrowserState,
    railKeys: List<String>,
    articles: List<JarnRssItem>,
    hasPlayer: Boolean,
    filter: JarnRssFilter,
    blocked: Boolean,
) {
    val router = LocalJarnInputRouter.current
    val isActive = router.activeScreen == LocalJarnScreen.current
    val tabsFocus = LocalJarnTabsFocus.current
    val ready = isActive && !blocked
    val latestArticles by rememberUpdatedState(articles)
    val focusGone = browser.focusKey?.let { key ->
        key !in railKeys && articles.none { it.focusKey() == key } && !(key == FOCUS_PLAYER && hasPlayer)
    } == true
    LaunchedEffect(ready, focusGone) {
        if (!ready) return@LaunchedEffect
        withFrameNanos { }
        if (!tabsFocus.hasFocus) browser.restore(railKeys, latestArticles, hasPlayer, filter.focusKey())
    }
    LaunchedEffect(browser.focusFirstArticle, filter, ready) {
        if (!browser.focusFirstArticle || !ready) return@LaunchedEffect
        browser.focusFirstArticle = false
        val first = latestArticles.firstOrNull() ?: return@LaunchedEffect
        browser.articleList.scrollToItem(0)
        withFrameNanos { }
        runCatching { browser.requester(first.focusKey()).requestFocus() }
    }
}

@Composable
private fun RssFeedRail(
    browser: RssBrowserState,
    feeds: List<JarnRssFeed>,
    filter: JarnRssFilter,
    onSelect: (JarnRssFilter) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fixed = listOf(
        JarnRssFilter.All to stringResource(R.string.jarnfe_rss_filter_all),
        JarnRssFilter.Audio to stringResource(R.string.jarnfe_rss_filter_audio),
        JarnRssFilter.History to stringResource(R.string.jarnfe_rss_filter_history),
    )
    JarnRevealLazyItems(browser.railList) {
        LazyColumn(
            state = browser.railList,
            modifier = modifier,
            contentPadding = PaddingValues(vertical = JarnTheme.space.focusEdge),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        ) {
            items(fixed, key = { it.first.focusKey() }) { (entry, label) ->
                RssRailRow(browser, entry.focusKey(), label, entry == filter) { onSelect(entry) }
            }
            items(feeds, key = { it.url }) { feed ->
                val entry = JarnRssFilter.Feed(feed.url)
                RssRailRow(browser, entry.focusKey(), feed.title, entry == filter) { onSelect(entry) }
            }
            item(key = FOCUS_ADD) {
                RssRailRow(browser, FOCUS_ADD, stringResource(R.string.jarnfe_rss_menu_add), false, onAdd)
            }
        }
    }
}

@Composable
private fun RssRailRow(
    browser: RssBrowserState,
    focusKey: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = JarnTheme.colors
    val shape = JarnTheme.radius.smShape
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(browser.requester(focusKey))
            .onFocusChanged { if (it.isFocused) browser.focusKey = focusKey }
            .jarnInteractive(shape, source, onClick = onClick)
            .heightIn(min = 40.dp)
            .background(if (selected) colors.raised else colors.surface, shape)
            .padding(horizontal = JarnTheme.space.md, vertical = JarnTheme.space.sm),
        contentAlignment = Alignment.CenterStart,
    ) {
        JarnText(
            text = label,
            style = if (selected) JarnTheme.type.labelStrong else JarnTheme.type.label,
            color = if (selected) colors.accent else colors.textHigh,
            maxLines = 1,
        )
    }
}

@Composable
private fun RssArticlePane(
    browser: RssBrowserState,
    articles: List<JarnRssItem>,
    feedTitles: Map<String, String>,
    hasFeeds: Boolean,
    onOpen: (JarnRssItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (articles.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            JarnEmptyState(
                title = stringResource(
                    if (hasFeeds) R.string.jarnfe_rss_no_articles_title else R.string.jarnfe_rss_no_feeds_title,
                ),
                message = stringResource(
                    if (hasFeeds) R.string.jarnfe_rss_no_articles_message else R.string.jarnfe_rss_no_feeds_message,
                ),
            )
        }
        return
    }
    JarnRevealLazyItems(browser.articleList) {
        LazyColumn(
            state = browser.articleList,
            modifier = modifier,
            contentPadding = PaddingValues(vertical = JarnTheme.space.focusEdge),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            items(articles, key = { it.key }) { item ->
                RssArticleRow(browser, item, feedTitles[item.feedUrl]) { onOpen(item) }
            }
        }
    }
}

@Composable
private fun RssArticleRow(
    browser: RssBrowserState,
    item: JarnRssItem,
    feedTitle: String?,
    onClick: () -> Unit,
) {
    val colors = JarnTheme.colors
    val shape = JarnTheme.radius.mdShape
    val source = remember { MutableInteractionSource() }
    val focusKey = item.focusKey()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(browser.requester(focusKey))
            .onFocusChanged { if (it.isFocused) browser.focusKey = focusKey }
            .jarnInteractive(shape, source, onClick = onClick)
            .background(colors.surface, shape)
            .padding(JarnTheme.space.sm),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.imageUrl.isNotEmpty()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ArticleThumbSize)
                    .clip(JarnTheme.radius.smShape),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs)) {
            JarnText(item.title, JarnTheme.type.labelStrong, colors.textHigh, maxLines = 2)
            JarnText(rssArticleMeta(item, feedTitle), JarnTheme.type.caption, colors.textMid, maxLines = 1)
        }
    }
}

@Composable
private fun rssArticleMeta(item: JarnRssItem, feedTitle: String?): String {
    val badge = when {
        item.hasVideo -> stringResource(R.string.jarnfe_rss_badge_video)
        item.hasAudio -> stringResource(R.string.jarnfe_rss_badge_audio)
        else -> null
    }
    return listOfNotNull(feedTitle, item.dateLabel.takeIf { it.isNotEmpty() }, badge)
        .joinToString(stringResource(R.string.jarnfe_rss_meta_separator))
}
