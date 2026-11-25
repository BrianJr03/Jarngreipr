package jr.brian.jarnfe.deck

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.jarnClickable
import jr.brian.core.ui.focus.jarnFocus
import jr.brian.core.ui.library.JarnArtPlaceholder
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.LibraryHeldArt
import jr.brian.jarnfe.library.NowGameArt
import jr.brian.jarnfe.library.NowPill
import jr.brian.jarnfe.library.nowCoverAspect
import jr.brian.jarnfe.library.nowLastPlayedText
import jr.brian.jarnfe.library.nowPlaytimeText
import jr.brian.jarnfe.library.resumeStatsStyle
import jr.brian.jarnfe.shell.JarnDeviceStatus
import jr.brian.jarnfe.shell.isBatteryLow

internal val NowPlayingCoverHeight = 280.dp
private val NowPlayingBackdropBlur = 24.dp
private val StatusIconSize = 20.dp
private const val STAT_SEPARATOR = "  ·  "

/** How readable one part of the page is: awake, freshly dimmed, or dimmed for a long time. */
internal enum class NowPlayingTone { Awake, Dimmed, LongDimmed }

@Composable
private fun NowPlayingTone.readoutColor(awake: Color): Color = when (this) {
    NowPlayingTone.Awake -> awake
    NowPlayingTone.Dimmed -> JarnTheme.colors.textMid
    NowPlayingTone.LongDimmed -> JarnTheme.colors.textLow
}

/** The game's fanart, blurred under a scrim. With none, the still wallpaper behind shows through. */
@Composable
internal fun BoxScope.NowPlayingBackdrop(fanartPath: String?) {
    val path = fanartPath ?: return
    val paths = remember(path) { listOf(path) }
    Box(Modifier.matchParentSize().blur(NowPlayingBackdropBlur)) {
        LibraryHeldArt(paths, ContentScale.Crop, canLoad = true)
    }
    Box(Modifier.matchParentSize().background(JarnTheme.colors.artScrim))
}

/** [dim] shades the cover itself, so at full dim it reads at 30% whatever sits behind it. */
@Composable
internal fun NowPlayingCover(game: JarnLibraryGame, art: NowGameArt, dim: Float, modifier: Modifier = Modifier) {
    val shape = JarnTheme.tileShape
    Box(
        modifier
            .height(NowPlayingCoverHeight)
            .aspectRatio(nowCoverAspect(null))
            .clearAndSetSemantics {}
            .clip(shape)
            .background(JarnTheme.colors.raised, shape)
            .drawWithContent {
                drawContent()
                val shade = nowPlayingCoverShade(dim)
                if (shade > 0f) drawRect(Color.Black, alpha = shade)
            },
    ) {
        val cover = art.coverPath
        when {
            !art.isLoaded -> Unit
            cover == null -> JarnArtPlaceholder(game.title, Modifier.matchParentSize())
            else -> {
                val paths = remember(cover) { listOf(cover) }
                LibraryHeldArt(paths, ContentScale.Crop, canLoad = true) {
                    JarnArtPlaceholder(game.title, Modifier.matchParentSize())
                }
            }
        }
    }
}

@Composable
internal fun NowPlayingEyebrow() {
    NowPill(stringResource(R.string.jarnfe_now_playing_title), strong = true, themed = true)
}

@Composable
internal fun NowPlayingTitle(title: String, systemTitle: String) {
    val colors = JarnTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs)) {
        JarnText(title, JarnTheme.type.title, colors.textHigh, maxLines = 2)
        JarnText(systemTitle, JarnTheme.type.caption, colors.textMid, maxLines = 1)
    }
}

/** The hero: hours and minutes this session, in tabular figures so the digits swap in place. */
@Composable
internal fun NowPlayingSessionTime(text: String, tone: NowPlayingTone) {
    val description = stringResource(R.string.jarnfe_now_playing_session, text)
    JarnText(
        text = text,
        style = JarnTheme.type.display.copy(fontFeatureSettings = "tnum"),
        color = tone.readoutColor(JarnTheme.colors.accent),
        modifier = Modifier.semantics { contentDescription = description },
        maxLines = 1,
    )
}

/** Last played and total play time, as on Continue playing. */
@Composable
internal fun NowPlayingStats(game: JarnLibraryGame) {
    val metadata = game.metadata
    val stats = listOfNotNull(
        metadata.lastPlayedAtMs?.takeIf { it > 0 }?.let {
            stringResource(R.string.jarnfe_library_last_played, nowLastPlayedText(it))
        },
        metadata.playtimeMinutes?.takeIf { it > 0 }?.let { nowPlaytimeText(it) },
    )
    if (stats.isEmpty()) return
    JarnText(stats.joinToString(STAT_SEPARATOR), resumeStatsStyle(), JarnTheme.colors.textMid, maxLines = 1)
}

@Composable
internal fun NowPlayingStatus(status: JarnDeviceStatus, tone: NowPlayingTone) {
    val colors = JarnTheme.colors
    val style = JarnTheme.type.heading.copy(fontFeatureSettings = "tnum")
    Row(
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnText(status.time, style, tone.readoutColor(colors.textHigh), maxLines = 1)
        if (tone == NowPlayingTone.Awake) NowPlayingBattery(status)
    }
}

@Composable
private fun NowPlayingBattery(status: JarnDeviceStatus) {
    val percent = status.batteryPercent ?: return
    val colors = JarnTheme.colors
    val color = if (status.isBatteryLow) colors.danger else colors.textHigh
    Row(
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            status.isCharging -> StatusIcon(R.drawable.ic_jarn_bolt, R.string.jarnfe_now_playing_charging, colors.success)
            status.isBatteryLow -> StatusIcon(R.drawable.ic_jarn_battery_low, R.string.jarnfe_now_playing_battery_low, colors.danger)
        }
        JarnText(
            text = stringResource(R.string.jarnfe_battery_percent, percent),
            style = JarnTheme.type.heading.copy(fontFeatureSettings = "tnum"),
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun StatusIcon(icon: Int, description: Int, tint: Color) {
    Image(
        painter = painterResource(icon),
        contentDescription = stringResource(description),
        colorFilter = ColorFilter.tint(tint),
        modifier = Modifier.size(StatusIconSize),
    )
}

/** The Deck header's way back to a page the player left: tap it or press A on it. */
@Composable
internal fun NowPlayingPill(title: String, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .jarnClickable(source) { JarnNowPlaying.reopen() }
            .jarnFocus(JarnTheme.radius.smShape, source),
    ) {
        NowPill(stringResource(R.string.jarnfe_now_playing_pill, title), strong = true, themed = true)
    }
}

/** Parts that only matter awake fade with the dim, so the scrim reads as one 75% layer. */
internal fun Modifier.nowPlayingFade(dim: Float): Modifier = graphicsLayer { alpha = 1f - dim }

/** The cover's wake lift. Its dim is a shade drawn on the cover, see [nowPlayingCoverShade]. */
internal fun Modifier.nowPlayingCoverLift(lift: Float): Modifier = graphicsLayer {
    scaleX = lift
    scaleY = lift
}

internal const val NOW_PLAYING_COVER_DIM_LOSS = 0.7f

/**
 * Black drawn over the cover at [dim]. A see-through cover would mix with the backdrop and read
 * far brighter than 30% on the dark page, so the cover is shaded in place instead.
 */
internal fun nowPlayingCoverShade(dim: Float): Float = dim.coerceIn(0f, 1f) * NOW_PLAYING_COVER_DIM_LOSS

@Composable
internal fun nowPlayingDimLabel(seconds: Int?): String = when {
    seconds == null -> stringResource(R.string.jarnfe_dim_never)
    seconds < 60 -> pluralStringResource(R.plurals.jarnfe_dim_seconds, seconds, seconds)
    else -> pluralStringResource(R.plurals.jarnfe_dim_minutes, seconds / 60, seconds / 60)
}

internal const val NOW_PLAYING_DIM_ALPHA = 0.75f
