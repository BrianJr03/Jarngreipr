package jr.brian.jarnfe.rss

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.audio.JarnAudio
import jr.brian.jarnfe.canvas.findActivity
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val VIDEO_POLL_MS = 250L
private val ReaderScrollStep = 160.dp
private val ReaderImageMaxHeight = 220.dp

/** Shows the open article on the Stage while a real Deck window keeps the list. */
@Composable
internal fun JarnRssStageHost(state: JarnSessionState) {
    if (!state.deckDisplayAvailable) return
    val reader = JarnRss.reader ?: return
    val context = LocalContext.current
    val data = remember(context) { context.jarnRssDataOrNull() } ?: return
    Box(
        Modifier
            .fillMaxSize()
            .background(JarnTheme.colors.bg),
    ) {
        JarnRssReaderPanel(data, reader)
    }
}

@Composable
internal fun JarnRssReaderPanel(data: JarnRssData, reader: JarnRssReader) {
    LaunchedEffect(reader.item.key) { data.recordHistory(reader.item.id) }
    val exit = {
        JarnRss.close()
        JarnSession.update { activate(JarnScreen.Deck) }
    }
    if (reader.watchingVideo) {
        key(reader.item.videoUrl) {
            RssVideoPanel(data, reader.item.videoUrl) { JarnRss.watchVideo(false) }
        }
    } else {
        RssArticlePanel(data, reader, exit)
    }
}

@Composable
private fun RssArticlePanel(data: JarnRssData, reader: JarnRssReader, onExit: () -> Unit) {
    val item = reader.item
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val scroll = key(item.key) { rememberScrollState() }
    val stepPx = with(LocalDensity.current) { ReaderScrollStep.toPx() }
    val playbackFlow = remember(data) { data.playback }
    val playback by playbackFlow.collectAsState(initial = null)
    val isCurrent = playback?.let {
        !it.isSystemMedia && it.itemId == item.id && it.feedUrl == item.feedUrl
    } == true
    val listen: () -> Unit = {
        if (isCurrent) {
            data.togglePlayPause()
        } else {
            scope.launch {
                if (!data.playAudio(item, reader.queue)) JarnNotices.show(R.string.jarnfe_rss_notice_play_failed)
            }
        }
    }
    val openBrowser: () -> Unit = {
        val opened = activity?.let { data.openLink(it, item.link) } == true
        if (!opened) JarnNotices.show(R.string.jarnfe_rss_notice_link_failed)
    }
    JarnInputLayer(
        mode = JarnLayerMode.Capture,
        bindings = {
            on(JarnAction.MoveUp, R.string.jarnfe_rss_hint_scroll) {
                scope.launch { scroll.animateScrollBy(-stepPx) }
            }
            on(JarnAction.MoveDown) { scope.launch { scroll.animateScrollBy(stepPx) } }
            if (isCurrent) seekBindings(data::seekBy)
            articlePrimaryBinding(item, isCurrent, playback?.isPlaying == true, listen)
            if (item.hasVideo && item.hasAudio) {
                on(JarnAction.Search, R.string.jarnfe_rss_hint_listen, onPress = listen)
            }
            if (item.link.isNotEmpty()) {
                on(JarnAction.Context, R.string.jarnfe_rss_hint_browser, onPress = openBrowser)
            }
            on(JarnAction.TabPrevious, R.string.jarnfe_rss_hint_previous) { JarnRss.step(-1) }
            on(JarnAction.TabNext, R.string.jarnfe_rss_hint_next) { JarnRss.step(1) }
            switchScreenBinding()
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onExit)
        },
    ) {
        RssArticleBody(data, item, isCurrent, scroll)
    }
}

private fun JarnBindingsBuilder.articlePrimaryBinding(
    item: JarnRssItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    listen: () -> Unit,
) {
    when {
        item.hasVideo -> on(JarnAction.Confirm, R.string.jarnfe_rss_hint_watch) { JarnRss.watchVideo(true) }
        item.hasAudio -> on(
            action = JarnAction.Confirm,
            label = if (isCurrent && isPlaying) R.string.jarnfe_rss_hint_pause else R.string.jarnfe_rss_hint_play,
            onPress = listen,
        )
        else -> on(JarnAction.Confirm) {}
    }
}

private fun JarnBindingsBuilder.seekBindings(seekBy: (Long) -> Unit) {
    on(JarnAction.MoveLeft, R.string.jarnfe_rss_hint_seek) { seekBy(-JARN_RSS_SEEK_MS) }
    on(JarnAction.MoveRight) { seekBy(JARN_RSS_SEEK_MS) }
}

private fun JarnBindingsBuilder.switchScreenBinding() {
    on(JarnAction.SwitchScreen, R.string.jarnfe_hint_switch_screen) {
        JarnSession.update { switchActiveScreen() }
    }
}

@Composable
private fun RssArticleBody(
    data: JarnRssData,
    item: JarnRssItem,
    isCurrent: Boolean,
    scroll: ScrollState,
) {
    val body by produceState("", item.key) {
        value = withContext(Dispatchers.Default) { jarnRssPlainText(item.description) }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = JarnTheme.space.stageMargin, vertical = JarnTheme.space.md),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
    ) {
        JarnText(item.title, JarnTheme.baseType.title, JarnTheme.colors.textHigh)
        if (item.dateLabel.isNotEmpty()) {
            JarnText(item.dateLabel, JarnTheme.type.caption, JarnTheme.colors.textMid, maxLines = 1)
        }
        if (isCurrent) JarnRssProgressLine(data)
        if (item.imageUrl.isNotEmpty()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = ReaderImageMaxHeight)
                    .clip(JarnTheme.radius.mdShape),
            )
        }
        JarnText(body, JarnTheme.baseType.body, JarnTheme.colors.textHigh)
    }
}

@Composable
private fun RssVideoPanel(data: JarnRssData, url: String, onExit: () -> Unit) {
    val context = LocalContext.current
    val video = remember(data, url) { data.createVideo(context, url) }
    DisposableEffect(video) {
        JarnAudio.setVideoOpen(true)
        onDispose {
            video.release()
            JarnAudio.setVideoOpen(false)
        }
    }
    var isPlaying by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(video) {
        while (true) {
            isPlaying = video.isPlaying
            failed = video.failed
            positionMs = video.positionMs
            durationMs = video.durationMs
            delay(VIDEO_POLL_MS)
        }
    }
    JarnInputLayer(
        mode = JarnLayerMode.Capture,
        bindings = {
            if (!failed) {
                on(
                    action = JarnAction.Confirm,
                    label = if (isPlaying) R.string.jarnfe_rss_hint_pause else R.string.jarnfe_rss_hint_play,
                    onPress = video::togglePlaying,
                )
                seekBindings(video::seekBy)
            }
            switchScreenBinding()
            on(JarnAction.Back, R.string.jarnfe_hint_back, onPress = onExit)
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (failed) {
                    JarnText(
                        text = stringResource(R.string.jarnfe_rss_video_failed),
                        style = JarnTheme.type.body,
                        color = JarnTheme.colors.textHigh,
                    )
                } else {
                    AndroidView(factory = { video.view }, modifier = Modifier.fillMaxSize())
                }
            }
            Row(Modifier.padding(JarnTheme.space.sm)) {
                JarnRssProgressBar(positionMs, durationMs)
            }
        }
    }
}
