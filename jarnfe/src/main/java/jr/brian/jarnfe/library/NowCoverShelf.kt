package jr.brian.jarnfe.library

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnEdgeScroll
import jr.brian.core.ui.focus.JarnFocusStyle
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.library.JarnGameTile
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.wallpaper.JarnWallpaperScreen
import jr.brian.jarnfe.wallpaper.rememberJarnWallpaperScreen

private const val PLATE_ALPHA = 0.86f
private const val SOLO_COVER_RATIO = 3f / 4f
private val RoomyCoverHeight = 104.dp
private val CompactCoverHeight = 80.dp
private val RoomyCoverMinPanel = 340.dp

/** Roomy when the Deck has the height, compact when the dock row takes some of it. */
internal fun nowCoverHeight(panelHeight: Dp): Dp =
    if (panelHeight >= RoomyCoverMinPanel) RoomyCoverHeight else CompactCoverHeight

/** True for the first cover of a later group, which gets a little extra space before it. */
internal fun nowStartsGroup(shelf: List<JarnShelfGame>, index: Int): Boolean =
    index > 0 && shelf[index - 1].group != shelf[index].group

@Composable
internal fun Modifier.nowPlate(): Modifier =
    background(JarnTheme.colors.bg.copy(alpha = PLATE_ALPHA), JarnTheme.radius.smShape)

@Composable
internal fun NowShelfHeader(shelf: List<JarnShelfGame>, focusedId: String?, totalGames: Int) {
    val focused = shelf.firstOrNull { it.game.id == focusedId }
    val group = (focused ?: shelf.first()).group
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = JarnTheme.space.deckMargin),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NowPill(stringResource(group.labelRes()), strong = true)
        Box(Modifier.weight(1f, fill = false).padding(start = JarnTheme.space.sm)) {
            NowPill(shelfCaption(focused, shelf.size, totalGames), strong = false)
        }
    }
}

@Composable
private fun shelfCaption(focused: JarnShelfGame?, shelfSize: Int, totalGames: Int): String {
    val game = focused?.game ?: return stringResource(R.string.jarnfe_now_shelf_count, shelfSize, totalGames)
    val lastPlayed = game.metadata.lastPlayedAtMs ?: return game.title
    return stringResource(R.string.jarnfe_now_cover_caption, game.title, formatDate(lastPlayed))
}

private fun JarnShelfGroup.labelRes(): Int = when (this) {
    JarnShelfGroup.Recent -> R.string.jarnfe_now_group_recent
    JarnShelfGroup.MostPlayed -> R.string.jarnfe_now_group_most_played
    JarnShelfGroup.InSystem -> R.string.jarnfe_now_group_in_system
}

@Composable
internal fun NowPill(text: String, strong: Boolean) {
    val colors = JarnTheme.colors
    JarnText(
        text = text,
        style = if (strong) JarnTheme.type.labelStrong else JarnTheme.type.label,
        color = if (strong) colors.textHigh else colors.textMid,
        modifier = Modifier
            .nowPlate()
            .padding(horizontal = JarnTheme.space.sm, vertical = JarnTheme.space.xs),
        maxLines = 1,
    )
}

/** Now on the larger top screen, after a swap, shows its covers 20% bigger. */
internal const val NOW_TOP_SCREEN_COVER_SCALE = 1.2f

internal fun nowCoverScale(screen: JarnWallpaperScreen?, hasTwoScreens: Boolean): Float =
    if (hasTwoScreens && screen == JarnWallpaperScreen.Top) NOW_TOP_SCREEN_COVER_SCALE else 1f

/** Read from the window's own display, so a swap is what moves Now to the bigger size. */
@Composable
internal fun rememberNowCoverScale(): Float =
    nowCoverScale(rememberJarnWallpaperScreen(), JarnSession.state.deckDisplayAvailable)

/**
 * One row of game covers. Each keeps its own shape at [coverHeight], and they share one bottom
 * edge, with art that leaves room drawn against that edge rather than floating in the middle.
 */
@Composable
internal fun NowCoverRow(
    shelf: List<JarnShelfGame>,
    coverHeight: Dp,
    coverModifier: (JarnLibraryGame) -> Modifier,
    onCover: (JarnLibraryGame) -> Unit,
    aspectOf: (JarnLibraryGame, IntSize?) -> Float = { _, bounds -> nowCoverAspect(bounds) },
    scale: Float = rememberNowCoverScale(),
) {
    val height = coverHeight * scale
    JarnEdgeScroll(margin = JarnTheme.space.deckMargin) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = JarnTheme.space.deckMargin),
            horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
            verticalAlignment = Alignment.Bottom,
        ) {
            shelf.forEachIndexed { index, item ->
                key(item.game.id) {
                    val groupGap = if (nowStartsGroup(shelf, index)) JarnTheme.space.sm else 0.dp
                    val coverBounds = rememberNowArtBounds(item.game.artPath)
                    NowRevealWhen(item.game.artPath == null || coverBounds != null)
                    JarnGameTile(
                        title = item.game.title,
                        onClick = { onCover(item.game) },
                        modifier = Modifier
                            .padding(start = groupGap)
                            .then(coverModifier(item.game))
                            .height(height),
                        aspectRatio = aspectOf(item.game, coverBounds),
                    ) { LibraryCoverArt(item.game, alignment = Alignment.BottomCenter) }
                }
            }
        }
    }
}

@Composable
internal fun NowSoloCard(
    game: JarnLibraryGame,
    coverHeight: Dp,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = JarnTheme.colors
    val shape = JarnTheme.radius.smShape
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .padding(horizontal = JarnTheme.space.deckMargin)
            .then(modifier)
            .fillMaxWidth()
            .height(coverHeight)
            .jarnInteractive(shape, source, focusStyle = JarnFocusStyle.Float, onClick = onClick)
            .clip(shape)
            .background(colors.bg.copy(alpha = PLATE_ALPHA)),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.fillMaxHeight().aspectRatio(SOLO_COVER_RATIO)) {
            LibraryCoverArt(game)
        }
        Column(Modifier.weight(1f).padding(end = JarnTheme.space.md)) {
            JarnText(game.title, JarnTheme.baseType.heading, colors.textHigh, maxLines = 2)
            JarnText(soloCaption(game), JarnTheme.type.caption, colors.textMid, maxLines = 1)
        }
    }
}

@Composable
private fun soloCaption(game: JarnLibraryGame): String =
    game.metadata.lastPlayedAtMs?.let { stringResource(R.string.jarnfe_library_last_played, formatDate(it)) }
        ?: stringResource(R.string.jarnfe_now_not_played)
