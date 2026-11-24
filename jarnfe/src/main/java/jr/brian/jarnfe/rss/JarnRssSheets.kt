package jr.brian.jarnfe.rss

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnDialog
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnSheet
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.canvas.CanvasModalSheet
import jr.brian.jarnfe.canvas.findActivity
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.keyboard.JarnTextKeyboard
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val RssSheetListMaxHeight = 280.dp

internal sealed interface RssSheet {
    data object Menu : RssSheet
    data object Add : RssSheet
    data object Settings : RssSheet
    data class Feed(val url: String) : RssSheet
    data class Interval(val url: String) : RssSheet
    data class ConfirmRemove(val url: String) : RssSheet
    data class Item(val key: String) : RssSheet
}

/** What the sheets need from the list behind them. */
internal class RssSheetActions(
    val data: JarnRssData,
    val scope: CoroutineScope,
    val feeds: List<JarnRssFeed>,
    val articles: List<JarnRssItem>,
    val onSheet: (RssSheet?) -> Unit,
    val onPageOptions: () -> Unit,
    val onRead: (JarnRssItem) -> Unit,
    val onWatch: (JarnRssItem) -> Unit,
) {
    val close: () -> Unit = { onSheet(null) }

    fun refreshAll() {
        close()
        scope.launch { noticeForRefresh(data.refreshAll()) }
    }

    fun refresh(url: String) {
        close()
        scope.launch { noticeForRefresh(data.refreshFeed(url)) }
    }

    fun add(raw: String) {
        val url = jarnRssFeedUrl(raw)
        if (url == null) {
            JarnNotices.show(R.string.jarnfe_rss_notice_invalid_url)
            return
        }
        close()
        scope.launch {
            val result = runCatching { data.addFeed(url) }.getOrDefault(JarnRssAddResult.Failed)
            JarnNotices.show(result.noticeRes())
        }
    }

    fun remove(url: String) {
        close()
        scope.launch {
            data.removeFeed(url)
            JarnNotices.show(R.string.jarnfe_rss_notice_removed)
        }
    }

    fun move(url: String, delta: Int) {
        val order = jarnRssMoved(feeds.map { it.url }, url, delta)
        scope.launch { data.reorderFeeds(order) }
    }

    fun listen(item: JarnRssItem) {
        close()
        scope.launch {
            if (!data.playAudio(item, articles)) JarnNotices.show(R.string.jarnfe_rss_notice_play_failed)
        }
    }

    private fun noticeForRefresh(ok: Boolean) = JarnNotices.show(
        if (ok) R.string.jarnfe_rss_notice_refreshed else R.string.jarnfe_rss_notice_refresh_failed,
    )
}

@StringRes
private fun JarnRssAddResult.noticeRes(): Int = when (this) {
    JarnRssAddResult.Added -> R.string.jarnfe_rss_notice_added
    JarnRssAddResult.AddedNotLoaded -> R.string.jarnfe_rss_notice_not_loaded
    JarnRssAddResult.Duplicate -> R.string.jarnfe_rss_notice_duplicate
    JarnRssAddResult.Failed -> R.string.jarnfe_rss_notice_add_failed
}

@Composable
internal fun RssSheetHost(sheet: RssSheet?, actions: RssSheetActions) {
    when (sheet) {
        null -> Unit
        RssSheet.Menu -> RssMenuSheet(actions)
        RssSheet.Add -> RssAddFeedSheet(actions)
        RssSheet.Settings -> RssSettingsSheet(actions)
        is RssSheet.Feed -> actions.feeds.firstOrNull { it.url == sheet.url }
            ?.let { RssFeedSheet(it, actions) } ?: actions.close()
        is RssSheet.Interval -> actions.feeds.firstOrNull { it.url == sheet.url }
            ?.let { RssIntervalSheet(it, actions) } ?: actions.close()
        is RssSheet.ConfirmRemove -> actions.feeds.firstOrNull { it.url == sheet.url }
            ?.let { RssRemoveDialog(it, actions) } ?: actions.close()
        is RssSheet.Item -> actions.articles.firstOrNull { it.key == sheet.key }
            ?.let { RssItemSheet(it, actions) } ?: actions.close()
    }
}

@Composable
private fun RssSheetColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = RssSheetListMaxHeight)
            .verticalScroll(rememberScrollState())
            .padding(vertical = JarnTheme.space.focusEdge),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
    ) { content() }
}

@Composable
private fun RssSheetRow(
    @StringRes labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    JarnNavigateRow(
        label = stringResource(labelRes),
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        focusableWhenDisabled = true,
    )
}

@Composable
private fun RssMenuSheet(actions: RssSheetActions) {
    val first = remember { FocusRequester() }
    CanvasModalSheet(
        title = stringResource(R.string.jarnfe_rss_menu_title),
        initialFocus = first,
        onClose = actions.close,
    ) {
        RssSheetColumn {
            RssSheetRow(
                R.string.jarnfe_rss_menu_add,
                { actions.onSheet(RssSheet.Add) },
                Modifier.focusRequester(first),
            )
            RssSheetRow(R.string.jarnfe_rss_menu_refresh_all, actions::refreshAll)
            RssSheetRow(R.string.jarnfe_rss_menu_settings, { actions.onSheet(RssSheet.Settings) })
            RssSheetRow(R.string.jarnfe_rss_menu_pages, {
                actions.close()
                actions.onPageOptions()
            })
        }
    }
}

@Composable
private fun RssAddFeedSheet(actions: RssSheetActions) {
    JarnSheet(visible = true, onDismiss = actions.close) {
        JarnTextKeyboard(
            label = stringResource(R.string.jarnfe_rss_add_label),
            masked = false,
            onDone = actions::add,
            onCancel = actions.close,
            modifier = Modifier.fillMaxWidth(),
            maxLength = JARN_RSS_MAX_URL_LENGTH,
        )
    }
}

@Composable
private fun RssFeedSheet(feed: JarnRssFeed, actions: RssSheetActions) {
    val first = remember { FocusRequester() }
    val index = actions.feeds.indexOfFirst { it.url == feed.url }
    CanvasModalSheet(title = feed.title, initialFocus = first, onClose = actions.close) {
        RssSheetColumn {
            RssSheetRow(
                R.string.jarnfe_rss_feed_refresh,
                { actions.refresh(feed.url) },
                Modifier.focusRequester(first),
            )
            JarnValueRow(
                label = stringResource(R.string.jarnfe_rss_feed_interval),
                value = rssIntervalLabel(feed.refreshMinutes),
                onClick = { actions.onSheet(RssSheet.Interval(feed.url)) },
                modifier = Modifier.fillMaxWidth(),
            )
            RssSheetRow(
                R.string.jarnfe_rss_feed_move_up,
                { actions.move(feed.url, -1) },
                enabled = index > 0,
            )
            RssSheetRow(
                R.string.jarnfe_rss_feed_move_down,
                { actions.move(feed.url, 1) },
                enabled = index < actions.feeds.lastIndex,
            )
            RssSheetRow(
                R.string.jarnfe_rss_feed_remove,
                { actions.onSheet(RssSheet.ConfirmRemove(feed.url)) },
            )
        }
    }
}

@Composable
private fun rssIntervalLabel(minutes: Int): String = when {
    minutes <= 0 -> stringResource(R.string.jarnfe_rss_interval_manual)
    minutes == 60 -> stringResource(R.string.jarnfe_rss_interval_hour)
    minutes % 60 == 0 -> stringResource(R.string.jarnfe_rss_interval_hours, minutes / 60)
    else -> stringResource(R.string.jarnfe_rss_interval_minutes, minutes)
}

@Composable
private fun RssIntervalSheet(feed: JarnRssFeed, actions: RssSheetActions) {
    val current = remember { FocusRequester() }
    val options = remember(feed.refreshMinutes) {
        (JarnRssRefreshMinutes + feed.refreshMinutes).distinct().sorted()
    }
    CanvasModalSheet(
        title = stringResource(R.string.jarnfe_rss_feed_interval),
        initialFocus = current,
        onClose = { actions.onSheet(RssSheet.Feed(feed.url)) },
    ) {
        RssSheetColumn {
            options.forEach { minutes ->
                val selected = minutes == feed.refreshMinutes
                JarnNavigateRow(
                    label = rssIntervalLabel(minutes),
                    supporting = if (selected) stringResource(R.string.jarnfe_rss_interval_current) else null,
                    onClick = {
                        actions.scope.launch { actions.data.setRefreshInterval(feed.url, minutes) }
                        actions.onSheet(RssSheet.Feed(feed.url))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (selected) Modifier.focusRequester(current) else Modifier),
                )
            }
        }
    }
}

@Composable
private fun RssRemoveDialog(feed: JarnRssFeed, actions: RssSheetActions) {
    val dismiss = { actions.onSheet(RssSheet.Feed(feed.url)) }
    RssConfirmDialog(
        title = stringResource(R.string.jarnfe_rss_remove_title),
        message = stringResource(R.string.jarnfe_rss_remove_message, feed.title),
        confirmLabel = stringResource(R.string.jarnfe_rss_remove_confirm),
        onConfirm = { actions.remove(feed.url) },
        onDismiss = dismiss,
    )
}

@Composable
private fun RssConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        bindings = {
            hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm)
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onDismiss)
        },
    ) {
        JarnDialog(
            visible = true,
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            dismissLabel = stringResource(R.string.jarnfe_rss_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            destructive = true,
        )
    }
}

@Composable
private fun RssItemSheet(item: JarnRssItem, actions: RssSheetActions) {
    val first = remember { FocusRequester() }
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    CanvasModalSheet(title = item.title, initialFocus = first, onClose = actions.close) {
        RssSheetColumn {
            RssSheetRow(
                R.string.jarnfe_rss_item_read,
                {
                    actions.close()
                    actions.onRead(item)
                },
                Modifier.focusRequester(first),
            )
            if (item.hasAudio) {
                RssSheetRow(R.string.jarnfe_rss_item_listen, { actions.listen(item) })
            }
            if (item.hasVideo) {
                RssSheetRow(R.string.jarnfe_rss_item_watch, {
                    actions.close()
                    actions.onWatch(item)
                })
            }
            if (item.link.isNotEmpty()) {
                RssSheetRow(R.string.jarnfe_rss_item_browser, {
                    actions.close()
                    val opened = activity?.let { actions.data.openLink(it, item.link) } == true
                    if (!opened) JarnNotices.show(R.string.jarnfe_rss_notice_link_failed)
                })
            }
        }
    }
}

@Composable
private fun RssSettingsSheet(actions: RssSheetActions) {
    val first = remember { FocusRequester() }
    var confirmClear by remember { mutableStateOf(false) }
    CanvasModalSheet(
        title = stringResource(R.string.jarnfe_rss_menu_settings),
        initialFocus = first,
        onClose = { actions.onSheet(RssSheet.Menu) },
    ) {
        RssSheetColumn { JarnRssSettingsPanel(actions.data, { confirmClear = true }, first) }
    }
    if (confirmClear) {
        JarnRssClearPositionsDialog(actions.data) { confirmClear = false }
    }
}

/** Rows only, so the Settings screen can host them. Show the clear dialog outside any scroller. */
@Composable
internal fun JarnRssSettingsPanel(
    data: JarnRssData,
    onClearPositions: () -> Unit,
    firstFocus: FocusRequester? = null,
    clearFocus: FocusRequester? = null,
) {
    val flow = remember(data) { data.dateStyle }
    val style by flow.collectAsState(initial = JarnRssDateStyle())
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_rss_settings_day_first),
        checked = style.dayFirst,
        onCheckedChange = { data.setDateStyle(style.copy(dayFirst = it)) },
        modifier = Modifier
            .fillMaxWidth()
            .then(firstFocus?.let { Modifier.focusRequester(it) } ?: Modifier),
        supporting = stringResource(R.string.jarnfe_rss_settings_day_first_hint),
    )
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_rss_settings_24_hour),
        checked = style.use24Hour,
        onCheckedChange = { data.setDateStyle(style.copy(use24Hour = it)) },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_rss_settings_24_hour_hint),
    )
    RssSheetRow(
        labelRes = R.string.jarnfe_rss_settings_clear,
        onClick = onClearPositions,
        modifier = clearFocus?.let { Modifier.focusRequester(it) } ?: Modifier,
    )
}

@Composable
internal fun JarnRssClearPositionsDialog(data: JarnRssData, onDone: () -> Unit) {
    RssConfirmDialog(
        title = stringResource(R.string.jarnfe_rss_settings_clear_title),
        message = stringResource(R.string.jarnfe_rss_settings_clear_message),
        confirmLabel = stringResource(R.string.jarnfe_rss_settings_clear_confirm),
        onConfirm = {
            onDone()
            data.clearPlayPositions()
            JarnNotices.show(R.string.jarnfe_rss_notice_cleared)
        },
        onDismiss = onDone,
    )
}
