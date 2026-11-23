package jr.brian.jarnfe.rss

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnChip
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R

@Composable
internal fun JarnRssPlayerBar(
    data: JarnRssData,
    playback: JarnRssPlayback,
    playFocus: FocusRequester,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(JarnTheme.colors.surface, JarnTheme.radius.mdShape)
            .padding(JarnTheme.space.sm),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        JarnRssPlayerTitle(playback)
        JarnRssProgressLine(data)
        JarnRssTransport(data, playback, playFocus, onFocused)
    }
}

@Composable
private fun JarnRssPlayerTitle(playback: JarnRssPlayback) {
    val detail = when {
        playback.isBuffering -> stringResource(R.string.jarnfe_rss_player_buffering)
        playback.isSystemMedia -> stringResource(R.string.jarnfe_rss_player_other_app)
        else -> playback.artist
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnText(
            text = playback.title,
            style = JarnTheme.type.labelStrong,
            color = JarnTheme.colors.textHigh,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        detail?.let { JarnText(it, JarnTheme.type.caption, JarnTheme.colors.textMid, maxLines = 1) }
        JarnText(
            text = stringResource(R.string.jarnfe_rss_player_volume, (playback.volume * 100).toInt()),
            style = JarnTheme.type.caption,
            color = JarnTheme.colors.textMid,
            maxLines = 1,
        )
    }
}

@Composable
internal fun JarnRssProgressLine(data: JarnRssData, modifier: Modifier = Modifier) {
    val flow = remember(data) { data.progress }
    val progress by flow.collectAsState(initial = JarnRssProgress())
    JarnRssProgressBar(progress.positionMs, progress.durationMs, modifier)
}

@Composable
internal fun JarnRssProgressBar(positionMs: Long, durationMs: Long, modifier: Modifier = Modifier) {
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(4.dp)
                .clip(JarnTheme.radius.smShape)
                .background(JarnTheme.colors.raised),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(JarnTheme.colors.accent),
            )
        }
        JarnText(
            text = stringResource(
                R.string.jarnfe_rss_player_progress,
                jarnRssTime(positionMs),
                jarnRssTime(durationMs),
            ),
            style = JarnTheme.type.caption,
            color = JarnTheme.colors.textMid,
            maxLines = 1,
        )
    }
}

@Composable
private fun JarnRssTransport(
    data: JarnRssData,
    playback: JarnRssPlayback,
    playFocus: FocusRequester,
    onFocused: () -> Unit,
) {
    val track = Modifier.onFocusChanged { if (it.isFocused) onFocused() }
    val playLabel = if (playback.isPlaying) R.string.jarnfe_rss_hint_pause else R.string.jarnfe_rss_hint_play
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
    ) {
        JarnChip(stringResource(R.string.jarnfe_rss_player_previous), track, onClick = { data.skip(forward = false) })
        JarnChip(stringResource(R.string.jarnfe_rss_player_back), track, onClick = { data.seekBy(-JARN_RSS_SEEK_MS) })
        JarnChip(
            label = stringResource(playLabel),
            modifier = Modifier.focusRequester(playFocus).then(track),
            onClick = data::togglePlayPause,
        )
        JarnChip(stringResource(R.string.jarnfe_rss_player_forward), track, onClick = { data.seekBy(JARN_RSS_SEEK_MS) })
        JarnChip(stringResource(R.string.jarnfe_rss_player_next), track, onClick = { data.skip(forward = true) })
        JarnChip(stringResource(R.string.jarnfe_rss_player_quieter), track, onClick = { data.stepVolume(-JARN_RSS_VOLUME_STEP) })
        JarnChip(stringResource(R.string.jarnfe_rss_player_louder), track, onClick = { data.stepVolume(JARN_RSS_VOLUME_STEP) })
    }
}
