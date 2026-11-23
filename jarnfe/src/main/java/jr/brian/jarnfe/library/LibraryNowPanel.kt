package jr.brian.jarnfe.library

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import jr.brian.core.ui.focus.JarnRevealScrollTop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.library.JarnMetadataPlate
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.core.ui.theme.LocalJarnScreenActive
import jr.brian.jarnfe.R
import jr.brian.jarnfe.launch.GameActionsDeck
import jr.brian.jarnfe.launch.JarnGameActions
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.systems.JarnSystems
import java.text.DateFormat
import java.util.Date

private const val MINUTES_PER_HOUR = 60L
private const val NOW_RECENT_LIMIT = 3
private const val DESCRIPTION_MAX_LINES = 5
private val NowArtHeight = 132.dp
private val NowTopZone = 240.dp

@Composable
internal fun LibraryNowPanel(selection: LibrarySelection) {
    val settled = rememberSettledSelection(selection)
    NowFocusKeeper(settled) {
        when (settled) {
            LibrarySelection.None -> JarnEmptyState(
                title = stringResource(R.string.jarnfe_empty_now_title),
                message = stringResource(R.string.jarnfe_empty_now_message),
            )
            is LibrarySelection.System -> NowColumn { SystemSummary(settled.system) }
            is LibrarySelection.Game -> NowColumn { GameNow(settled.game) }
        }
    }
}

/**
 * The Stage can change the selection while the controller is on Now. If that removes the focused
 * row, focus lands back inside the panel instead of disappearing from the active screen.
 */
@Composable
private fun NowFocusKeeper(selection: LibrarySelection, content: @Composable () -> Unit) {
    val requester = remember { FocusRequester() }
    val holdsFocus = remember { booleanArrayOf(false) }
    val selectionKey = selection.focusKey()
    val hadFocus = remember(selectionKey) { holdsFocus[0] }
    val isActive = LocalJarnScreenActive.current
    LaunchedEffect(selectionKey) {
        if (!hadFocus || !isActive) return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        if (!holdsFocus[0]) runCatching { requester.requestFocus() }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { holdsFocus[0] = it.hasFocus }
            .focusRequester(requester)
            .focusGroup(),
        contentAlignment = Alignment.Center,
    ) { content() }
}

internal fun LibrarySelection.focusKey(): String? = when (this) {
    LibrarySelection.None -> null
    is LibrarySelection.System -> "system:${system.id}"
    is LibrarySelection.Game -> "game:${game.id}"
}

@Composable
private fun NowColumn(content: @Composable () -> Unit) {
    val scroll = rememberScrollState()
    JarnRevealScrollTop(scroll, topZone = NowTopZone) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = JarnTheme.space.deckMargin, vertical = JarnTheme.space.focusEdge),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        ) { content() }
    }
}

@Composable
private fun SystemSummary(system: JarnLibrarySystem) {
    val colors = JarnTheme.colors
    val shelf = rememberSystemNow(system.id).shelf
    val recentHere = remember(shelf) {
        shelf.filter { it.group == JarnShelfGroup.Recent }.take(NOW_RECENT_LIMIT).map { it.game }
    }
    JarnText(system.title, JarnTheme.type.title, colors.textHigh, maxLines = 2)
    JarnText(systemSubtitle(system), JarnTheme.type.body, colors.textMid, maxLines = 2)
    ManageSystemButton(system.id)
    if (recentHere.isNotEmpty()) RecentGames(recentHere)
}

@Composable
private fun ManageSystemButton(systemId: String) {
    if (JarnSystems.manager == null) return
    JarnButton(
        label = stringResource(R.string.jarnfe_systems_manage_one),
        onClick = {
            JarnSystems.openAt(systemId)
            JarnSession.update { openSystems() }
        },
        modifier = Modifier.fillMaxWidth(),
        variant = JarnButtonVariant.Secondary,
    )
}

@Composable
private fun systemSubtitle(system: JarnLibrarySystem): String {
    val count = gameCountText(system.gameCount)
    val lastPlayed = system.lastPlayedAtMs ?: return count
    return "$count, ${stringResource(R.string.jarnfe_library_last_played, formatDate(lastPlayed))}"
}

@Composable
private fun RecentGames(games: List<JarnLibraryGame>) {
    val colors = JarnTheme.colors
    JarnText(stringResource(R.string.jarnfe_library_recent_games), JarnTheme.type.labelStrong, colors.textMid, maxLines = 1)
    games.forEach { game ->
        JarnText(game.title, JarnTheme.type.body, colors.textHigh, maxLines = 1)
    }
}

@Composable
private fun GameNow(game: JarnLibraryGame) {
    val controller = JarnGameActions.controller
    if (controller == null) {
        GameDetails(game) {}
    } else {
        GameActionsDeck(game, controller) { actions -> GameDetails(game, actions) }
    }
}

@Composable
private fun GameDetails(game: JarnLibraryGame, actions: @Composable () -> Unit) {
    val colors = JarnTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(NowArtHeight)
            .clip(JarnTheme.radius.mdShape),
    ) { LibraryHeldDeckArt(game) }
    JarnText(game.title, JarnTheme.type.heading, colors.textHigh, maxLines = 2)
    actions()
    game.metadata.description?.trim()?.takeIf { it.isNotEmpty() }?.let {
        JarnText(it, JarnTheme.type.body, colors.textMid, maxLines = DESCRIPTION_MAX_LINES)
    }
    val entries = metadataEntries(game.metadata)
    if (entries.isNotEmpty()) JarnMetadataPlate(entries)
}

@Composable
private fun metadataEntries(metadata: JarnGameMetadata): List<Pair<String, String>> {
    val text = metadata.textFields().map { stringResource(it.labelRes) to it.value }
    val playtime = metadata.playtimeMinutes?.takeIf { it > 0 }?.let {
        stringResource(R.string.jarnfe_library_meta_playtime) to playtimeText(it)
    }
    val lastPlayed = metadata.lastPlayedAtMs?.let {
        stringResource(R.string.jarnfe_library_meta_last_played) to formatDate(it)
    }
    return text + listOfNotNull(playtime, lastPlayed)
}

@Composable
internal fun playSummary(metadata: JarnGameMetadata): String? {
    val playtime = metadata.playtimeMinutes?.takeIf { it > 0 }?.let { playtimeText(it) }
    val lastPlayed = metadata.lastPlayedAtMs?.let {
        stringResource(R.string.jarnfe_library_last_played, formatDate(it))
    }
    return listOfNotNull(playtime, lastPlayed).joinToString(". ").ifEmpty { null }
}

@Composable
private fun playtimeText(minutes: Long): String = if (minutes >= MINUTES_PER_HOUR) {
    stringResource(R.string.jarnfe_library_playtime_hours, minutes / MINUTES_PER_HOUR)
} else {
    stringResource(R.string.jarnfe_library_playtime_minutes, minutes)
}

@Composable
private fun formatDate(epochMs: Long): String =
    remember(epochMs) { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMs)) }
