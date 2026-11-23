package jr.brian.jarnfe.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R

private val DeckLogoSize = 144.dp
private val SuffixDropDistance = 72.dp
private val TileRiseDistance = 24.dp
private const val TILE_START_SCALE = 0.96f

/** The startup timeline in milliseconds. Every value is pure so the sequence can be tested. */
internal object JarnSplashTimeline {
    const val DROP_START = 650f
    const val DOCK_START = 1100f
    const val REVEAL_START = 1650f
    const val END = 2450f

    private const val DROP_MS = 400f
    private const val DOCK_MS = 500f
    private const val FADE_MS = 200f
    private const val TILE_MS = 300f
    private const val TILE_STAGGER_MS = 45f
    private const val MAX_STAGGERED_TILE = 11

    private fun eased(raw: Float) = FastOutSlowInEasing.transform(raw.coerceIn(0f, 1f))

    fun drop(elapsed: Float) = eased((elapsed - DROP_START) / DROP_MS)

    fun dock(elapsed: Float) = eased((elapsed - DOCK_START) / DOCK_MS)

    fun fade(elapsed: Float) = eased((elapsed - REVEAL_START) / FADE_MS)

    fun tile(elapsed: Float, order: Int): Float {
        val delay = order.coerceIn(0, MAX_STAGGERED_TILE) * TILE_STAGGER_MS
        return eased((elapsed - REVEAL_START - delay) / TILE_MS)
    }
}

/** Plays once per app start, shared by the Stage and Deck windows. */
internal object JarnSplash {
    var elapsedMs by mutableFloatStateOf(0f)
        private set
    var markBounds by mutableStateOf<Rect?>(null)

    val isPlaying by derivedStateOf { elapsedMs < JarnSplashTimeline.END }
    val isIntro by derivedStateOf { elapsedMs < JarnSplashTimeline.REVEAL_START }

    fun finish() {
        elapsedMs = JarnSplashTimeline.END
    }

    suspend fun play(reducedMotion: Boolean) {
        if (reducedMotion) finish()
        if (!isPlaying) return
        val startedAt = withFrameMillis { it } - elapsedMs.toLong()
        while (isPlaying) {
            withFrameMillis { now ->
                if (isPlaying) elapsedMs = (now - startedAt).toFloat().coerceAtMost(JarnSplashTimeline.END)
            }
        }
    }
}

internal fun Modifier.jarnSplashFade(): Modifier = graphicsLayer {
    alpha = JarnSplashTimeline.fade(JarnSplash.elapsedMs)
}

internal fun Modifier.jarnSplashTile(order: Int): Modifier = graphicsLayer {
    val progress = JarnSplashTimeline.tile(JarnSplash.elapsedMs, order)
    val scale = lerp(TILE_START_SCALE, 1f, progress)
    alpha = progress
    scaleX = scale
    scaleY = scale
    translationY = (1f - progress) * TileRiseDistance.toPx()
}

/** The header wordmark, hidden until the animated one has landed on it. */
internal fun Modifier.jarnSplashMark(): Modifier = this
    .onGloballyPositioned { JarnSplash.markBounds = it.boundsInRoot() }
    .graphicsLayer { alpha = if (JarnSplash.isIntro) 0f else 1f }

@Composable
internal fun JarnStageSplash() {
    if (!JarnSplash.isIntro) return
    val colors = JarnTheme.colors
    val display = JarnTheme.type.display.copy(color = colors.textHigh)
    val dockScale = JarnTheme.type.heading.fontSize.value / display.fontSize.value
    val word = stringResource(R.string.jarnfe_wordmark)
    val suffix = stringResource(R.string.jarnfe_wordmark_suffix)
    val measurer = rememberTextMeasurer()
    val wordLayout = remember(measurer, word, display) { measurer.measure(word, display) }
    val suffixLayout = remember(measurer, suffix, display) { measurer.measure(suffix, display) }
    Canvas(Modifier.fillMaxSize().background(Color.Black)) {
        val elapsed = JarnSplash.elapsedMs
        val startX = (size.width - wordLayout.size.width - suffixLayout.size.width) / 2f
        val startY = (size.height - wordLayout.size.height) / 2f
        val dock = JarnSplashTimeline.dock(elapsed)
        val target = JarnSplash.markBounds
        val x = lerp(startX, target?.left ?: startX, dock)
        val y = lerp(startY, target?.top ?: startY, dock)
        val scale = lerp(1f, dockScale, dock)
        withTransform({
            translate(x, y)
            scale(scale, scale, Offset.Zero)
        }) { drawText(wordLayout) }
        val drop = JarnSplashTimeline.drop(elapsed)
        if (drop < 1f) {
            drawText(
                textLayoutResult = suffixLayout,
                topLeft = Offset(startX + wordLayout.size.width, startY + drop * SuffixDropDistance.toPx()),
                alpha = 1f - drop,
            )
        }
    }
}

@Composable
internal fun JarnDeckSplash() {
    if (!JarnSplash.isPlaying) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - JarnSplashTimeline.fade(JarnSplash.elapsedMs) }
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_jarn_logo),
            contentDescription = null,
            modifier = Modifier.size(DeckLogoSize),
        )
    }
}
