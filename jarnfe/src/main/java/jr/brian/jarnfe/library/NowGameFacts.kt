package jr.brian.jarnfe.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import java.text.DateFormat
import java.util.Date

private const val MINUTES_PER_HOUR = 60L
private const val FACT_SEPARATOR = ", "
private const val STAT_SEPARATOR = "  ·  "
private val ReleaseYear = Regex("(?<!\\d)(19|20)\\d{2}")
private val PlayerCount = Regex("\\d+(\\s*[-+]\\s*\\d*)?")

internal enum class NowPlayersKind { One, Count, Text }

/** The short facts on the poster. A missing or blank value is null and isn't shown. */
@Immutable
internal data class NowGameFacts(
    val genre: String? = null,
    val year: String? = null,
    val players: String? = null,
    val playtimeMinutes: Long? = null,
    val lastPlayedAtMs: Long? = null,
    val rating: String? = null,
) {
    val isEmpty: Boolean get() = this == NowGameFacts()
}

internal fun JarnGameMetadata.nowFacts() = NowGameFacts(
    genre = genre.cleaned(),
    year = releaseYear(releaseDate),
    players = players.cleaned()?.takeUnless { it == "0" },
    playtimeMinutes = playtimeMinutes?.takeIf { it > 0 },
    lastPlayedAtMs = lastPlayedAtMs?.takeIf { it > 0 },
    rating = rating.cleaned(),
)

/** Handles ES-DE's `19950101T000000` as well as dates written by hand. */
internal fun releaseYear(releaseDate: String?): String? =
    releaseDate?.let { ReleaseYear.find(it)?.value }

internal fun nowPlayersKind(players: String): NowPlayersKind = when {
    players == "1" -> NowPlayersKind.One
    PlayerCount.matches(players) -> NowPlayersKind.Count
    else -> NowPlayersKind.Text
}

private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

@Composable
internal fun NowFactsPlate(facts: NowGameFacts, modifier: Modifier = Modifier) {
    if (facts.isEmpty) return
    val colors = JarnTheme.colors
    val summary = listOfNotNull(facts.genre, facts.year, facts.players?.let { playersText(it) })
    val stats = listOfNotNull(
        facts.playtimeMinutes?.let { nowPlaytimeText(it) },
        facts.lastPlayedAtMs?.let { stringResource(R.string.jarnfe_library_last_played, nowDateText(it)) },
        facts.rating?.let { stringResource(R.string.jarnfe_now_rating, it) },
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.artScrim, JarnTheme.radius.smShape)
            .padding(horizontal = JarnTheme.space.md, vertical = JarnTheme.space.sm),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        if (summary.isNotEmpty()) {
            JarnText(summary.joinToString(FACT_SEPARATOR), JarnTheme.baseType.label, colors.textHigh, maxLines = 1)
        }
        if (stats.isNotEmpty()) {
            JarnText(stats.joinToString(STAT_SEPARATOR), JarnTheme.type.caption, colors.textMid, maxLines = 1)
        }
    }
}

@Composable
private fun playersText(players: String): String = when (nowPlayersKind(players)) {
    NowPlayersKind.One -> stringResource(R.string.jarnfe_now_players_one)
    NowPlayersKind.Count -> stringResource(R.string.jarnfe_now_players_count, players)
    NowPlayersKind.Text -> players
}

@Composable
internal fun nowPlaytimeText(minutes: Long): String = if (minutes >= MINUTES_PER_HOUR) {
    stringResource(R.string.jarnfe_library_playtime_hours, minutes / MINUTES_PER_HOUR)
} else {
    stringResource(R.string.jarnfe_library_playtime_minutes, minutes)
}

@Composable
internal fun nowDateText(epochMs: Long): String =
    remember(epochMs) { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMs)) }

@Composable
internal fun nowMetadataEntries(metadata: JarnGameMetadata): List<Pair<String, String>> {
    val text = metadata.textFields().map { stringResource(it.labelRes) to it.value }
    val playtime = metadata.playtimeMinutes?.takeIf { it > 0 }?.let {
        stringResource(R.string.jarnfe_library_meta_playtime) to nowPlaytimeText(it)
    }
    val lastPlayed = metadata.lastPlayedAtMs?.takeIf { it > 0 }?.let {
        stringResource(R.string.jarnfe_library_meta_last_played) to nowDateText(it)
    }
    return text + listOfNotNull(playtime, lastPlayed)
}
