package jr.brian.jarnfe.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.library.JarnGameTile
import jr.brian.core.ui.theme.JarnTheme
import kotlinx.coroutines.delay

private const val SHELF_WASH_ALPHA = 0.22f
private val ShelfWashBlur = 48.dp
private const val SHELF_BADGE_SURFACE_ALPHA = 0.82f

/**
 * A card so logos never float bare, with the grid's scale, float and focus border. The card itself
 * never changes for focus or dimming, only the art on it does.
 */
@Composable
internal fun LibraryShelfCard(slot: LibraryShelfSlotCard, art: @Composable BoxScope.() -> Unit) {
    JarnGameTile(
        title = slot.description,
        onClick = slot.onClick,
        modifier = slot.modifier.fillMaxWidth(),
        aspectRatio = slot.aspectRatio,
        focusBorder = true,
    ) {
        Box(Modifier.fillMaxSize().background(JarnTheme.colors.raised)) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = slot.artAlpha() }, content = art)
        }
    }
}

/** A soft wash of the centered art behind the row. */
@Composable
internal fun <T> LibraryShelfWash(item: T, content: LibraryShelfContent<T>) {
    Crossfade(
        targetState = item,
        animationSpec = JarnTheme.motion.art.spec(),
        label = "shelfWash",
    ) { shown ->
        Box(
            Modifier
                .fillMaxSize()
                .alpha(SHELF_WASH_ALPHA)
                .blur(ShelfWashBlur),
        ) { content.art(this, shown) }
    }
}

@Composable
internal fun <T> BoxScope.LibraryShelfCaption(item: T, content: LibraryShelfContent<T>, metrics: ShelfMetrics) {
    val colors = JarnTheme.colors
    val space = JarnTheme.space
    Column(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = metrics.centerY + metrics.cardHeight / 2 + space.lg)
            .fillMaxWidth()
            .padding(horizontal = JarnTheme.space.stageMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space.xs),
    ) {
        JarnText(
            text = content.headline(item),
            style = JarnTheme.baseType.heading,
            color = colors.textHigh,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        content.caption(item)?.let { meta ->
            JarnText(meta, JarnTheme.baseType.label, colors.textMid, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

/** Shows the centered item's letter while a direction is held, and fades out once it settles. */
@Composable
internal fun BoxScope.LibraryShelfBadge(title: String, tick: Int, fast: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(tick, fast) {
        if (fast && tick > 0) {
            visible = true
        } else {
            delay(SHELF_BADGE_HOLD_MS)
            visible = false
        }
    }
    val motion = JarnTheme.motion
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(motion.small.spec()),
        exit = fadeOut(motion.small.spec()),
        modifier = Modifier.align(Alignment.Center),
    ) {
        JarnText(
            text = shelfBadgeLetter(title),
            style = JarnTheme.baseType.display,
            color = JarnTheme.colors.textHigh,
            modifier = Modifier
                .background(
                    JarnTheme.colors.bg.copy(alpha = SHELF_BADGE_SURFACE_ALPHA),
                    JarnTheme.radius.smShape,
                )
                .padding(horizontal = JarnTheme.space.lg, vertical = JarnTheme.space.sm),
            maxLines = 1,
        )
    }
}
