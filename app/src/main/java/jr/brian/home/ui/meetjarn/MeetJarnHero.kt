package jr.brian.home.ui.meetjarn

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import jr.brian.home.R

private val HeroFrame = Color(0xFF05070A)
private val LookBg = Color(0xFF0E1419)
private val LookText = Color(0xFFEAF3F8)
private val LookAccent = Color(0xFF5CC8F5)
private val LookFocus = Color(0xFF9BE0FF)
private const val STAGE_ASPECT = 1920f / 1080f
private const val DECK_ASPECT = 1240f / 1080f
private const val DECK_WIDTH = 0.68f
private const val WALL_ALPHA = 0.34f
private const val WALL_TILT = -7.45f
private const val WALL_COLUMNS = 7
private const val SHELF_NEIGHBOR_SCALE = 0.62f
private const val SHELF_NEIGHBOR_ALPHA = 0.55f

/** Talanah's bundled pictures carry their own device shell, so they are never clipped or backed. */
private const val BUNDLED_DUAL_ASPECT = 960f / 1118f
private const val BUNDLED_SINGLE_ASPECT = 960f / 558f

/**
 * Jarn on the player's screens, made from their own covers when the library has enough art, or
 * the bundled picture otherwise. It is a still picture: nothing in it moves or takes focus.
 */
@Composable
fun MeetJarnHero(content: MeetJarnContent, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.meet_jarn_hero_description)
    val heroModifier = modifier.semantics { contentDescription = description }
    val art = content.heroArt
    if (art == null) {
        MeetJarnBundledHero(content.twoScreens, heroModifier)
    } else {
        MeetJarnCoversHero(art, content.twoScreens, heroModifier)
    }
}

@Composable
private fun MeetJarnBundledHero(twoScreens: Boolean, modifier: Modifier) {
    val (image, aspect) = if (twoScreens) {
        R.drawable.jarn_preview_hero_dual to BUNDLED_DUAL_ASPECT
    } else {
        R.drawable.jarn_preview_hero_single to BUNDLED_SINGLE_ASPECT
    }
    Image(
        painter = painterResource(image),
        contentDescription = null,
        modifier = modifier.fillMaxWidth().aspectRatio(aspect),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun MeetJarnCoversHero(art: MeetJarnHeroArt, twoScreens: Boolean, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HeroFrame, RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeroScreen(Modifier.fillMaxWidth().aspectRatio(STAGE_ASPECT)) { HeroStage(art, withNowPanel = !twoScreens) }
        if (twoScreens) {
            HeroScreen(Modifier.fillMaxWidth(DECK_WIDTH).aspectRatio(DECK_ASPECT)) { HeroNow(art, Modifier.fillMaxSize()) }
        }
    }
}

@Composable
private fun HeroScreen(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(4.dp)).background(LookBg)) { content() }
}

/** Sizes text and gaps as fractions of the screen's width, so the mock scales like a picture. */
@Composable
private fun Dp.toSp(): TextUnit = with(LocalDensity.current) { this@toSp.toSp() }

@Composable
private fun HeroStage(art: MeetJarnHeroArt, withNowPanel: Boolean) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val unit = maxWidth / 100
        HeroCoverWall(art.wall, maxWidth)
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(listOf(Color.Transparent, LookBg.copy(alpha = 0.75f))),
            ),
        )
        HeroContinueBand(
            art,
            unit,
            Modifier
                .align(Alignment.TopCenter)
                .padding(start = unit * 3, end = unit * 3, top = maxHeight * 0.13f)
                .fillMaxWidth()
                .fillMaxHeight(0.26f),
        )
        HeroShelf(
            art,
            unit,
            Modifier.align(Alignment.TopCenter).padding(top = maxHeight * 0.5f).fillMaxWidth().fillMaxHeight(0.42f),
        )
        if (withNowPanel) {
            HeroNow(
                art,
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.44f)
                    .clip(RoundedCornerShape(topStart = unit * 2, topEnd = unit * 2))
                    .background(LookBg.copy(alpha = 0.94f)),
            )
        }
    }
}

@Composable
private fun HeroCoverWall(covers: List<String>, width: Dp) {
    val tile = width / WALL_COLUMNS
    Box(
        Modifier
            .fillMaxSize()
            .alpha(WALL_ALPHA)
            .rotate(WALL_TILT),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(tile * 0.1f)) {
            covers.chunked(WALL_COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(tile * 0.1f)) {
                    row.forEach { cover -> HeroCover(cover, Modifier.requiredSize(tile, tile * 4 / 3)) }
                }
            }
        }
    }
}

@Composable
private fun HeroCover(path: String, modifier: Modifier) {
    AsyncImage(
        model = path,
        contentDescription = null,
        modifier = modifier.clip(RoundedCornerShape(4.dp)),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun HeroContinueBand(art: MeetJarnHeroArt, unit: Dp, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(unit * 1.5f))) {
        HeroCover(art.continueCover, Modifier.fillMaxSize().alpha(0.5f))
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(LookBg.copy(alpha = 0.9f), Color.Transparent)),
            ),
        )
        HeroEyebrowAndTitle(
            art.continueTitle,
            unit,
            Modifier.align(Alignment.CenterStart).padding(start = unit * 4).fillMaxWidth(0.7f),
        )
    }
}

@Composable
private fun HeroEyebrowAndTitle(title: String, unit: Dp, modifier: Modifier) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.meet_jarn_continue_playing),
            color = LookAccent,
            fontSize = (unit * 1.4f).toSp(),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            text = title,
            color = LookText,
            fontSize = (unit * 3f).toSp(),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Five system cards with the middle one focused, as Jarn's carousel shows them. */
@Composable
private fun HeroShelf(art: MeetJarnHeroArt, unit: Dp, modifier: Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(unit * 2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val middle = art.systemLogos.size / 2
        art.systemLogos.forEachIndexed { index, logo ->
            val centered = index == middle
            val width = unit * 28 * if (centered) 1f else SHELF_NEIGHBOR_SCALE
            HeroSystemCard(logo, centered, unit, Modifier.requiredSize(width, width * 10 / 16))
        }
    }
}

@Composable
private fun HeroSystemCard(logo: String?, centered: Boolean, unit: Dp, modifier: Modifier) {
    val shape = RoundedCornerShape(unit)
    val focused = if (centered) {
        Modifier.background(LookFocus, RoundedCornerShape(unit * 1.4f)).padding(unit * 0.4f)
    } else {
        Modifier.alpha(SHELF_NEIGHBOR_ALPHA)
    }
    Box(modifier.then(focused)) {
        Box(Modifier.fillMaxSize().clip(shape).background(Color(0xFF1C2630)), contentAlignment = Alignment.Center) {
            logo?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.fillMaxSize(0.62f)) }
        }
    }
}

@Composable
private fun HeroNow(art: MeetJarnHeroArt, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val unit = maxWidth / 100
        HeroCover(art.continueCover, Modifier.fillMaxSize().alpha(0.5f))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, LookBg))))
        Row(
            Modifier.fillMaxSize().padding(unit * 6),
            horizontalArrangement = Arrangement.spacedBy(unit * 6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeroCover(art.continueCover, Modifier.fillMaxWidth(0.4f).aspectRatio(3f / 4f))
            Column(verticalArrangement = Arrangement.spacedBy(unit * 2)) {
                HeroEyebrowAndTitle(art.continueTitle, unit * 2, Modifier)
                Text(
                    text = stringResource(R.string.meet_jarn_resume),
                    color = LookBg,
                    fontSize = (unit * 4f).toSp(),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(LookAccent, RoundedCornerShape(unit * 2))
                        .padding(horizontal = unit * 4, vertical = unit),
                )
            }
        }
    }
}
