package jr.brian.jarnfe.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.core.net.toUri
import jr.brian.core.ui.components.JarnButton
import jr.brian.core.ui.components.JarnButtonVariant
import jr.brian.core.ui.components.JarnDialog
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.core.ui.theme.rememberThemedStyle
import jr.brian.frontend.library.setup.LibrarySetupState
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.LocalJarnScreen
import jr.brian.jarnfe.launch.JarnGameActions
import jr.brian.jarnfe.launch.JarnGameFileStatus
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.LibrarySection
import jr.brian.jarnfe.session.StageRoute
import java.util.Calendar
import java.util.TimeZone

private const val MISSING_ART_ALPHA = 0.4f
private const val STAT_SEPARATOR = "  ·  "
private const val RELATIVE_DAY_LIMIT = 7
private const val MISSING_CHECK_SPAN = ALSO_RECENT_COUNT * 2
private const val DAY_MS = 86_400_000L

private fun startOfDay(ms: Long, zone: TimeZone): Long = Calendar.getInstance(zone).apply {
    timeInMillis = ms
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Whole calendar days between [thenMs] and [nowMs] in [zone]. Zero is today, never negative. */
internal fun nowDaysAgo(thenMs: Long, nowMs: Long, zone: TimeZone = TimeZone.getDefault()): Int {
    if (thenMs >= nowMs) return 0
    val gap = startOfDay(nowMs, zone) - startOfDay(thenMs, zone)
    return ((gap + DAY_MS / 2) / DAY_MS).toInt()
}

internal enum class ResumeAction { Play, ShowMissing }

/** A missing file never launches. Unknown still tries, so the launcher can explain what it finds. */
internal fun resumeAction(status: JarnGameFileStatus?): ResumeAction =
    if (status == JarnGameFileStatus.Missing) ResumeAction.ShowMissing else ResumeAction.Play

/** The missing-file notice for Continue playing, shown on the screen where A was pressed. */
internal object JarnResumeNotice {
    var game by mutableStateOf<JarnLibraryGame?>(null)
        private set
    var screen by mutableStateOf<JarnScreen?>(null)
        private set

    fun show(missing: JarnLibraryGame, on: JarnScreen) {
        game = missing
        screen = on
    }

    fun dismiss() {
        game = null
        screen = null
    }
}

internal fun resumeGame(game: JarnLibraryGame, status: JarnGameFileStatus?, screen: JarnScreen) {
    when (resumeAction(status)) {
        ResumeAction.Play -> JarnLibrary.play(game)
        ResumeAction.ShowMissing -> JarnResumeNotice.show(game, screen)
    }
}

/** Null while the check runs or when launching is not connected. */
@Composable
internal fun rememberGameFileStatus(game: JarnLibraryGame): JarnGameFileStatus? {
    val check = JarnLibrary.checkFile
    val recent = JarnLibrary.state.recent
    val status by produceState<JarnGameFileStatus?>(null, game.id, check, recent) {
        value = check?.let { runCatching { it(game) }.getOrNull() }
    }
    return status
}

@Composable
private fun rememberMissingRecentIds(): Set<String> {
    val check = JarnLibrary.checkFile
    val recent = JarnLibrary.state.recent
    val missing by produceState(emptySet<String>(), check, recent) {
        val candidates = recent.items.drop(1).take(MISSING_CHECK_SPAN)
        value = if (check == null) {
            emptySet()
        } else {
            candidates.filter { runCatching { check(it) }.getOrNull() == JarnGameFileStatus.Missing }
                .mapTo(HashSet()) { it.id }
        }
    }
    return missing
}

internal fun missingArtAlpha(status: JarnGameFileStatus?): Float =
    if (status == JarnGameFileStatus.Missing) MISSING_ART_ALPHA else 1f

@Composable
internal fun NowResumeEyebrow() {
    NowPill(stringResource(R.string.jarnfe_library_continue_playing), strong = true, themed = true)
}

@Composable
internal fun nowLastPlayedText(epochMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val days = remember(epochMs, nowMs / DAY_MS) { nowDaysAgo(epochMs, nowMs) }
    return when {
        days == 0 -> stringResource(R.string.jarnfe_resume_today)
        days == 1 -> stringResource(R.string.jarnfe_resume_yesterday)
        days < RELATIVE_DAY_LIMIT -> pluralStringResource(R.plurals.jarnfe_resume_days_ago, days, days)
        else -> nowDateText(epochMs)
    }
}

@Composable
private fun resumeStats(metadata: JarnGameMetadata): List<String> = listOfNotNull(
    metadata.lastPlayedAtMs?.takeIf { it > 0 }?.let {
        stringResource(R.string.jarnfe_library_last_played, nowLastPlayedText(it))
    },
    metadata.playtimeMinutes?.takeIf { it > 0 }?.let { nowPlaytimeText(it) },
    metadata.playCount?.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.jarnfe_resume_plays, it, it) },
)

/** Play stats and the emulator line, or "File not found" in place of the stats. */
@Composable
internal fun NowResumeFacts(game: JarnLibraryGame, status: JarnGameFileStatus?) {
    val colors = JarnTheme.colors
    val stats = if (status == JarnGameFileStatus.Missing) {
        listOf(stringResource(R.string.jarnfe_resume_file_missing))
    } else {
        resumeStats(game.metadata)
    }
    val emulator = JarnGameActions.setup?.takeIf { JarnGameActions.gameId == game.id }?.currentEmulatorLabel
    if (stats.isEmpty() && emulator == null) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.artScrim, JarnTheme.radius.smShape)
            .padding(horizontal = JarnTheme.space.md, vertical = JarnTheme.space.sm),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        if (stats.isNotEmpty()) {
            val text = stats.joinToString(STAT_SEPARATOR)
            JarnText(text, rememberThemedStyle(text, resumeStatsStyle(), JarnTheme.baseType.label), colors.textHigh, maxLines = 1)
        }
        emulator?.let {
            JarnText(stringResource(R.string.jarnfe_resume_opens_in, it), JarnTheme.type.caption, colors.textMid, maxLines = 1)
        }
    }
}

@Composable
internal fun NowResumeButton(
    game: JarnLibraryGame,
    status: JarnGameFileStatus?,
    focus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val screen = LocalJarnScreen.current
    JarnButton(
        label = stringResource(R.string.jarnfe_hint_resume),
        onClick = { resumeGame(game, status, screen) },
        icon = painterResource(R.drawable.ic_jarn_play),
        modifier = modifier.focusRequester(focus),
    )
}

/** The next recent games, newest first. Hidden when there is nothing behind the Continue card. */
@Composable
internal fun NowAlsoRecent(coverHeight: Dp) {
    val missing = rememberMissingRecentIds()
    val games = JarnLibrary.state.alsoRecent(missing)
    if (games.isEmpty()) return
    val shelf = remember(games) { games.map { JarnShelfGame(it, JarnShelfGroup.Recent) } }
    Column(verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = JarnTheme.space.deckMargin),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NowPill(stringResource(R.string.jarnfe_resume_also_recent), strong = true)
            JarnButton(
                label = stringResource(R.string.jarnfe_resume_see_all),
                onClick = ::openRecentOnStage,
                variant = JarnButtonVariant.Secondary,
            )
        }
        NowCoverRow(shelf, coverHeight, coverModifier = { Modifier }, onCover = JarnLibrary::play)
    }
}

private fun openRecentOnStage() {
    JarnSession.update {
        openRoute(StageRoute.Library).copy(librarySection = LibrarySection.Recent).activate(JarnScreen.Stage)
    }
}

/** Offers the one fix that applies: reconnecting a folder that lost access, or a library refresh. */
@Composable
internal fun ResumeMissingDialog() {
    val game = JarnResumeNotice.game
    val visible = game != null && JarnResumeNotice.screen == LocalJarnScreen.current
    val setup = LocalContext.current.jarnLibrarySetupOrNull()
    val setupState = setup?.state?.collectAsState()?.value
    val reconnect = setupState as? LibrarySetupState.NeedsReconnect
    JarnDialog(
        visible = visible,
        title = stringResource(R.string.jarnfe_resume_missing_title),
        message = stringResource(R.string.jarnfe_resume_missing_message, heroTitle(game?.title.orEmpty())),
        confirmLabel = stringResource(
            if (reconnect != null) R.string.jarnfe_resume_reconnect else R.string.jarnfe_settings_refresh,
        ),
        dismissLabel = stringResource(R.string.jarnfe_resume_cancel),
        onConfirm = {
            JarnResumeNotice.dismiss()
            if (reconnect != null) {
                JarnFolderPicker.open?.invoke(reconnect.nextTreeUri.takeIf { it.startsWith("content://") }?.toUri())
            } else {
                JarnLibrary.retry()
            }
        },
        onDismiss = JarnResumeNotice::dismiss,
    )
}

/** The look's label font when it has one, otherwise its display font, at the label size. */
@Composable
internal fun resumeStatsStyle(): TextStyle {
    val base = JarnTheme.baseType.label
    val label = JarnTheme.type.label
    if (label.fontFamily != base.fontFamily) return label
    val display = JarnTheme.type.display
    if (display.fontFamily == JarnTheme.baseType.display.fontFamily) return base
    return base.copy(fontFamily = display.fontFamily, letterSpacing = JarnTheme.type.heading.letterSpacing)
}
