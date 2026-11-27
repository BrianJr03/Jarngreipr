package jr.brian.jarnfe.library

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnFocusStyle
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.library.JARN_GAME_TILE_ASPECT
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.LocalJarnScreen
import jr.brian.jarnfe.launch.JarnGameFileStatus

/** The Continue playing strip's own height; with its top gap it stays inside [LIBRARY_HEADER_BAND]. */
internal val LibraryContinueStripHeight = 56.dp

/**
 * The top share of a spec-driven systems view that browse keeps for its header. Templates keep
 * their primary and text below it, so the Continue playing strip never covers them.
 */
internal const val LIBRARY_HEADER_BAND = 0.20f

/**
 * True when the browse header (the Continue playing card) belongs inside the list, as in the
 * built-in Grid and Carousel. Spec-driven themes set it false and get the header as its own strip.
 */
internal val LocalLibraryHeaderInPrimary = compositionLocalOf { true }

/**
 * Continue playing as one compact, focusable line for spec-driven themes: the cover, the eyebrow
 * and the title, with the same resume, missing-file and focus hand-back behavior as the card.
 */
@Composable
internal fun LibraryContinueStrip(library: JarnLibraryStore, game: JarnLibraryGame, modifier: Modifier = Modifier) {
    val status = rememberGameFileStatus(game)
    val screen = LocalJarnScreen.current
    val missing = status == JarnGameFileStatus.Missing
    val focus = remember { FocusRequester() }
    val source = remember { MutableInteractionSource() }
    val shape = JarnTheme.radius.mdShape
    LaunchedEffect(JarnLibrary.heroFocusPending) {
        if (!JarnLibrary.heroFocusPending) return@LaunchedEffect
        withFrameNanos { }
        runCatching { focus.requestFocus() }
        JarnLibrary.consumeHeroFocus()
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(LibraryContinueStripHeight)
            .focusRequester(focus)
            .onFocusChanged { library.heroFocused = it.isFocused }
            .jarnInteractive(shape, source, enabled = library.canPlay, focusStyle = JarnFocusStyle.Ring) {
                resumeGame(game, status, screen)
            }
            .clip(shape)
            .background(JarnTheme.colors.raised)
            .padding(end = JarnTheme.space.md),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.fillMaxHeight().aspectRatio(JARN_GAME_TILE_ASPECT).alpha(missingArtAlpha(status))) {
            LibraryCoverArt(game)
        }
        ContinueStripText(
            title = heroTitle(game.title),
            detail = if (missing) stringResource(R.string.jarnfe_resume_file_missing) else playSummary(game.metadata),
        )
    }
}

@Composable
private fun ContinueStripText(title: String, detail: String?) {
    val colors = JarnTheme.colors
    Column(verticalArrangement = Arrangement.Center) {
        JarnText(stringResource(R.string.jarnfe_library_continue_playing), JarnTheme.type.caption, colors.textMid, maxLines = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md), verticalAlignment = Alignment.CenterVertically) {
            JarnText(title, JarnTheme.type.labelStrong, colors.textHigh, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
            detail?.let { JarnText(it, JarnTheme.type.caption, colors.textMid, maxLines = 1) }
        }
    }
}
