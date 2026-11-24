package jr.brian.jarnfe.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnRevealScrollTop
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.launch.JarnGameActions
import jr.brian.jarnfe.look.rememberJarnDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

private const val MINUTES_PER_HOUR = 60L
private val UnboundedPanelHeight = 1000.dp
private val NowNameSpacing = 1.4.sp

/** [onRandom] and [onLastPlayed] are null while their button is hidden. Neither launches a game. */
@Immutable
internal class NowSystemActions(
    val onRandom: (() -> Unit)?,
    val onLastPlayed: (() -> Unit)?,
    val onPlay: ((JarnLibraryGame) -> Unit)?,
    val onDetails: ((JarnLibraryGame) -> Unit)?,
) {
    val buttons: List<NowFocusTarget>
        get() = listOfNotNull(
            NowFocusTarget.Random.takeIf { onRandom != null },
            NowFocusTarget.LastPlayed.takeIf { onLastPlayed != null },
        )
}

/** Random needs games to pick from. Last Played needs a game played in this system. */
internal fun nowShortcuts(
    gameCount: Int,
    lastPlayed: JarnLibraryGame?,
    randomVisible: Boolean,
    lastPlayedVisible: Boolean,
): List<NowFocusTarget> = listOfNotNull(
    NowFocusTarget.Random.takeIf { randomVisible && gameCount > 0 },
    NowFocusTarget.LastPlayed.takeIf { lastPlayedVisible && lastPlayed != null },
)

internal enum class NowShelfLayout { Hidden, Empty, Single, Shelf }

internal fun nowShelfLayout(gameCount: Int, shelfSize: Int): NowShelfLayout = when {
    gameCount == 0 -> NowShelfLayout.Empty
    shelfSize == 0 -> NowShelfLayout.Hidden
    gameCount == 1 && shelfSize == 1 -> NowShelfLayout.Single
    else -> NowShelfLayout.Shelf
}

@Composable
internal fun NowSystemView(system: JarnLibrarySystem, now: JarnSystemNow?) {
    val store = JarnNowShelf
    NowRevealWhen(now?.isLoaded == true)
    LaunchedEffect(JarnGameActions.gameId, JarnGameActions.page) { store.followLaunchedCover() }
    val canPlay = JarnLibrary.canPlay
    val hasDetails = JarnGameActions.controller != null
    val display = rememberJarnDisplay()
    val lastPlayed = libraryLastPlayed(now?.shelf.orEmpty())
    val shown = nowShortcuts(system.gameCount, lastPlayed, display.randomVisible, display.lastPlayedVisible)
    val scope = rememberCoroutineScope()
    val actions = remember(system.id, canPlay, hasDetails, shown, lastPlayed?.id) {
        NowSystemActions(
            onRandom = spinAction(scope, store, system.id).takeIf { NowFocusTarget.Random in shown },
            onLastPlayed = lastPlayed?.let { jumpAction(store, it) }.takeIf { NowFocusTarget.LastPlayed in shown },
            onPlay = if (canPlay) store::play else null,
            onDetails = if (hasDetails) store::openDetails else null,
        )
    }
    NowSystemContent(
        system = system,
        shelf = now?.shelf.orEmpty(),
        isLoaded = now?.isLoaded == true,
        actions = actions,
        pendingCoverId = store.pendingCoverId,
        onCoverRestored = store::coverRestored,
    )
}

private fun spinAction(scope: CoroutineScope, store: JarnNowShelfStore, systemId: String): () -> Unit = {
    scope.launch { store.spin(systemId) }
}

private fun jumpAction(store: JarnNowShelfStore, game: JarnLibraryGame): () -> Unit = {
    store.jumpTo(game.systemId, game.id, spin = false)
}

@Composable
internal fun NowSystemContent(
    system: JarnLibrarySystem,
    shelf: List<JarnShelfGame>,
    isLoaded: Boolean,
    actions: NowSystemActions,
    pendingCoverId: String?,
    onCoverRestored: () -> Unit,
) {
    val focus = remember { NowSystemFocus() }
    val shelfIds = remember(shelf) { shelf.map { it.game.id } }
    val buttons = actions.buttons
    SideEffect {
        focus.shelfIds = shelfIds
        focus.actions = buttons
    }
    PublishNowShortcuts(actions, focus)
    val entry = focus.coverEntryFor(shelfIds)
    NowEntryFocus(focus, entry, pendingCoverId != null)
    NowCoverRestore(focus, entry, isLoaded, pendingCoverId, onCoverRestored)
    JarnInputLayer(
        enabled = focus.focused != null,
        bindings = { nowSystemBindings(focus.focused, actions) },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().onFocusChanged { focus.holdsFocus = it.hasFocus }) {
            val bounded = constraints.hasBoundedHeight
            val coverHeight = nowCoverHeight(if (bounded) maxHeight else UnboundedPanelHeight)
            val scroll = rememberScrollState()
            JarnRevealScrollTop(scroll, topZone = if (bounded) maxHeight else NowTopZone) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        .heightIn(min = if (bounded) maxHeight else 0.dp)
                        .padding(top = JarnTheme.space.md, bottom = JarnTheme.space.focusEdge),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    NowNamePlate(system)
                    NowLowerArea(system, shelf, isLoaded, coverHeight, focus, actions)
                }
            }
        }
    }
}

private fun JarnBindingsBuilder.nowSystemBindings(focused: NowFocusTarget?, actions: NowSystemActions) {
    when (focused) {
        NowFocusTarget.Random -> hint(JarnAction.Confirm, R.string.jarnfe_now_random)
        NowFocusTarget.LastPlayed -> hint(JarnAction.Confirm, R.string.jarnfe_now_last_played)
        is NowFocusTarget.Cover -> {
            if (actions.onPlay != null) hint(JarnAction.Confirm, R.string.jarnfe_hint_play)
            actions.onDetails?.let { open ->
                on(JarnAction.Context, R.string.jarnfe_now_hint_details) { open(focused.game) }
            }
        }
        null -> Unit
    }
}

@Composable
private fun NowNamePlate(system: JarnLibrarySystem) {
    Box(Modifier.padding(horizontal = JarnTheme.space.deckMargin)) {
        Column(
            modifier = Modifier
                .nowPlate()
                .padding(horizontal = JarnTheme.space.md, vertical = JarnTheme.space.sm),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        ) {
            NowSystemMark(system)
            NowSystemStats(system)
        }
    }
}

@Composable
private fun NowSystemMark(system: JarnLibrarySystem) {
    var logoFailed by remember(system.logoPath) { mutableStateOf(false) }
    val logo = system.logoPath?.takeUnless { it.isBlank() || logoFailed }
    if (logo == null) {
        JarnText(
            text = system.title.uppercase(Locale.getDefault()),
            style = JarnTheme.type.heading.copy(letterSpacing = NowNameSpacing),
            color = JarnTheme.colors.textHigh,
            maxLines = 1,
        )
    } else {
        NowSystemLogo(logo, system.title, onFailed = { logoFailed = true })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NowSystemStats(system: JarnLibrarySystem) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.lg),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        NowStat(
            system.gameCount.toString(),
            pluralStringResource(R.plurals.jarnfe_now_stat_games, system.gameCount),
        )
        if (system.favoriteCount > 0) {
            NowStat(
                system.favoriteCount.toString(),
                pluralStringResource(R.plurals.jarnfe_now_stat_favorites, system.favoriteCount),
            )
        }
        if (system.playtimeMinutes > 0) {
            NowStat(nowPlaytimeValue(system.playtimeMinutes), stringResource(R.string.jarnfe_now_stat_played))
        }
        system.lastPlayedAtMs?.let { lastPlayed ->
            JarnText(
                text = stringResource(R.string.jarnfe_library_last_played, formatDate(lastPlayed)),
                style = JarnTheme.type.caption,
                color = JarnTheme.colors.textMid,
                modifier = Modifier.align(Alignment.CenterVertically),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun nowPlaytimeValue(minutes: Long): String = if (minutes >= MINUTES_PER_HOUR) {
    stringResource(R.string.jarnfe_now_hours, minutes / MINUTES_PER_HOUR)
} else {
    stringResource(R.string.jarnfe_now_minutes, minutes)
}

@Composable
private fun NowStat(value: String, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnText(value, JarnTheme.type.labelStrong, JarnTheme.colors.textHigh, maxLines = 1)
        JarnText(label, JarnTheme.type.caption, JarnTheme.colors.textMid, maxLines = 1)
    }
}

@Composable
private fun NowLowerArea(
    system: JarnLibrarySystem,
    shelf: List<JarnShelfGame>,
    isLoaded: Boolean,
    coverHeight: Dp,
    focus: NowSystemFocus,
    actions: NowSystemActions,
) {
    val layout = nowShelfLayout(system.gameCount, shelf.size)
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = JarnTheme.space.md),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
    ) {
        if (layout == NowShelfLayout.Empty && isLoaded) NowEmptyPlate()
        when (layout) {
            NowShelfLayout.Hidden, NowShelfLayout.Empty -> Unit
            NowShelfLayout.Single -> NowSoloShelf(shelf.first().game, coverHeight, focus, actions)
            NowShelfLayout.Shelf -> {
                NowShelfHeader(shelf, (focus.focused as? NowFocusTarget.Cover)?.game?.id, system.gameCount)
                NowCoverRow(
                    shelf = shelf,
                    coverHeight = coverHeight,
                    coverModifier = { game -> Modifier.nowCoverFocus(game, focus) },
                    onCover = { game -> actions.onPlay?.invoke(game) },
                )
            }
        }
    }
}

@Composable
private fun NowEmptyPlate() {
    val colors = JarnTheme.colors
    Column(
        modifier = Modifier
            .padding(horizontal = JarnTheme.space.deckMargin)
            .fillMaxWidth()
            .nowPlate()
            .padding(JarnTheme.space.md),
    ) {
        JarnText(stringResource(R.string.jarnfe_library_no_games_title), JarnTheme.type.heading, colors.textHigh, maxLines = 2)
        JarnText(stringResource(R.string.jarnfe_now_empty_message), JarnTheme.type.label, colors.textMid, maxLines = 3)
    }
}

@Composable
private fun NowSoloShelf(
    game: JarnLibraryGame,
    coverHeight: Dp,
    focus: NowSystemFocus,
    actions: NowSystemActions,
) {
    Box(Modifier.padding(horizontal = JarnTheme.space.deckMargin)) {
        NowPill(stringResource(R.string.jarnfe_now_group_only), strong = true)
    }
    NowSoloCard(
        game = game,
        coverHeight = coverHeight,
        modifier = Modifier.nowCoverFocus(game, focus),
        onClick = { actions.onPlay?.invoke(game) },
    )
}
