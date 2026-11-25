package jr.brian.jarnfe.canvas

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.jarnfe.R
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.launch

private val PhotoMimeTypes = arrayOf("image/*", "image/gif", "image/webp")

/** Opens the system picker for one photo tile. Cancelling leaves the tile exactly as it was. */
@Composable
internal fun rememberCanvasPhotoPicker(data: JarnCanvasData): (Int, CanvasItem.PhotoContainer) -> Unit {
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<Pair<Int, CanvasItem.PhotoContainer>?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val (page, item) = target ?: return@rememberLauncherForActivityResult
        target = null
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val stored = data.importPhoto(uri.toString())
            if (stored == null) {
                JarnNotices.show(R.string.jarnfe_canvas_notice_photo_failed)
            } else {
                data.updateTile(page, item.copy(imageUri = stored))
            }
        }
    }
    return { page, item ->
        target = page to item
        try {
            picker.launch(PhotoMimeTypes)
        } catch (_: ActivityNotFoundException) {
            target = null
            JarnNotices.show(R.string.jarnfe_canvas_notice_picker_failed)
        }
    }
}

@Composable
internal fun CanvasMusicSheet(data: JarnCanvasData, music: JarnCanvasMusic?, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val first = remember { FocusRequester() }
    val buffering = music?.isBuffering == true
    val playLabel = when {
        buffering -> R.string.jarnfe_canvas_music_buffering
        music?.isPlaying == true -> R.string.jarnfe_canvas_music_pause
        else -> R.string.jarnfe_canvas_music_play
    }
    CanvasModalSheet(stringResource(R.string.jarnfe_canvas_music_title), first, onClose) {
        JarnText(
            text = music?.title ?: stringResource(R.string.jarnfe_canvas_music_idle),
            style = JarnTheme.baseType.labelStrong,
            color = JarnTheme.colors.textHigh,
            maxLines = 2,
        )
        music?.artist?.let { JarnText(it, JarnTheme.type.caption, JarnTheme.colors.textMid, maxLines = 1) }
        JarnNavigateRow(
            label = stringResource(playLabel),
            onClick = {
                if (music != null) {
                    data.toggleMusic()
                } else {
                    scope.launch {
                        if (!data.startMusic()) JarnNotices.show(R.string.jarnfe_canvas_notice_no_audio)
                    }
                }
            },
            enabled = !buffering,
            focusableWhenDisabled = true,
            modifier = Modifier.fillMaxWidth().focusRequester(first),
        )
        if (music != null) {
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_canvas_music_previous),
                onClick = { data.skipMusic(forward = false) },
                modifier = Modifier.fillMaxWidth(),
            )
            JarnNavigateRow(
                label = stringResource(R.string.jarnfe_canvas_music_next),
                onClick = { data.skipMusic(forward = true) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun CanvasRssSheet(data: JarnCanvasData, onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val flow = remember(data) { data.rssItems }
    val items by flow.collectAsState(initial = null)
    val first = remember { FocusRequester() }
    val rows = items.orEmpty()
    CanvasModalSheet(
        title = stringResource(R.string.jarnfe_canvas_rss_title),
        initialFocus = first.takeIf { rows.isNotEmpty() },
        onClose = onClose,
    ) {
        if (rows.isEmpty()) {
            JarnText(
                text = stringResource(
                    if (items == null) R.string.jarnfe_canvas_pick_loading else R.string.jarnfe_canvas_rss_empty,
                ),
                style = JarnTheme.type.body,
                color = JarnTheme.colors.textMid,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = CanvasSheetListMaxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = JarnTheme.space.focusEdge),
                verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            ) {
                rows.forEachIndexed { index, item ->
                    JarnNavigateRow(
                        label = item.title,
                        supporting = if (item.hasAudio) stringResource(R.string.jarnfe_canvas_rss_audio) else null,
                        onClick = {
                            val host = activity ?: return@JarnNavigateRow
                            scope.launch {
                                if (!data.openRssItem(host, item)) {
                                    JarnNotices.show(R.string.jarnfe_canvas_notice_rss_failed)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().then(
                            if (index == 0) Modifier.focusRequester(first) else Modifier,
                        ),
                    )
                }
            }
        }
    }
}
