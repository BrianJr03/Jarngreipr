package jr.brian.jarnfe.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R

internal const val NOW_COLLECTION_TAG = "now_collection"
private const val MOSAIC_WIDTH_FRACTION = 0.6f
private const val MOSAIC_ASPECT = 16f / 10f

/** Total play time across a collection's listed games, in minutes. */
internal fun collectionPlaytimeMinutes(games: List<JarnLibraryGame>): Long =
    games.sumOf { it.metadata.playtimeMinutes ?: 0L }

/** A focused collection tile on the Deck: its mosaic, then its name, game count and play time. */
@Composable
internal fun NowCollectionView(
    collection: JarnCollection,
    source: JarnCollectionSource = JarnLibrary.source.collections,
) {
    val games = rememberCollectionGames(source, collection.id)
    NowRevealWhen(games != null)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = JarnTheme.space.deckMargin)
            .testTag(NOW_COLLECTION_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md, Alignment.CenterVertically),
    ) {
        NowCollectionMosaic(games?.take(COLLECTION_MOSAIC_SIZE))
        NowCollectionPlate(collection, collectionPlaytimeMinutes(games.orEmpty()))
    }
}

@Composable
private fun NowCollectionMosaic(covers: List<JarnLibraryGame>?) {
    Box(
        Modifier
            .fillMaxWidth(MOSAIC_WIDTH_FRACTION)
            .aspectRatio(MOSAIC_ASPECT)
            .clip(JarnTheme.tileShape),
    ) { LibraryCollectionArt(covers) }
}

@Composable
private fun NowCollectionPlate(collection: JarnCollection, playtimeMinutes: Long) {
    Column(
        modifier = Modifier
            .nowPlate()
            .padding(horizontal = JarnTheme.space.md, vertical = JarnTheme.space.sm),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        JarnText(collection.name, JarnTheme.type.heading, JarnTheme.colors.textHigh, maxLines = 2)
        NowCollectionStats(collection.gameCount, playtimeMinutes)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NowCollectionStats(gameCount: Int, playtimeMinutes: Long) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.lg),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        NowStat(gameCount.toString(), pluralStringResource(R.plurals.jarnfe_now_stat_games, gameCount))
        if (playtimeMinutes > 0) {
            NowStat(nowPlaytimeValue(playtimeMinutes), stringResource(R.string.jarnfe_now_stat_played))
        }
    }
}
