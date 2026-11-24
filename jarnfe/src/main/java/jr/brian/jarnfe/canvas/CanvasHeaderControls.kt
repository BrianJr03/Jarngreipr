package jr.brian.jarnfe.canvas

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.wallpaper.readableOverWallpaper

private const val INACTIVE_DOT_ALPHA = 0.45f
private val DotSize = 8.dp
private val ActiveDotWidth = 20.dp
private val HomeIconSize = 14.dp
private val ActiveHomeIconSize = 20.dp
private val OptionsButtonSize = 44.dp
private val OptionsIconSize = 24.dp

/** What the Deck's tab row shows for Canvas. Only a page or mode change produces a new value. */
internal data class CanvasHeaderStatus(
    val pageIndex: Int,
    val pageCount: Int,
    val homePage: Int,
    val arrangeStep: CanvasArrangeStep?,
    val moving: Boolean,
    val onOptions: () -> Unit,
) {
    val isEditing: Boolean get() = arrangeStep != null || moving
}

internal object CanvasHeaderBoard {
    var status by mutableStateOf<CanvasHeaderStatus?>(null)
}

@Composable
internal fun PublishCanvasHeader(
    page: JarnCanvasPage?,
    pageCount: Int,
    homePage: Int,
    arrangeStep: CanvasArrangeStep?,
    moving: Boolean,
    onOptions: () -> Unit,
) {
    val latestOptions by rememberUpdatedState(onOptions)
    val openOptions = remember { { latestOptions() } }
    SideEffect {
        CanvasHeaderBoard.status = page?.let {
            CanvasHeaderStatus(it.index, pageCount, homePage, arrangeStep, moving, openOptions)
        }
    }
    DisposableEffect(Unit) {
        onDispose { CanvasHeaderBoard.status = null }
    }
}

/** Sits opposite the tabs. Start opens the same options from a controller. */
@Composable
internal fun CanvasHeaderControls(modifier: Modifier = Modifier) {
    val status = CanvasHeaderBoard.status ?: return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CanvasPagerIndicator(status)
        CanvasOptionsIconButton(status.onOptions)
    }
}

/** Covers the tabs while editing, when they can't be used, so the grid never changes size. */
@Composable
internal fun CanvasHeaderEditBanner(modifier: Modifier = Modifier) {
    val status = CanvasHeaderBoard.status?.takeIf { it.isEditing } ?: return
    val colors = JarnTheme.colors
    Row(
        modifier = modifier
            .background(colors.raised)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            }
            .padding(horizontal = JarnTheme.space.deckMargin),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val step = status.arrangeStep
        if (step != null) {
            CanvasArrangeBanner(step)
        } else {
            JarnText(
                text = stringResource(R.string.jarnfe_canvas_moving),
                style = JarnTheme.type.labelStrong,
                color = colors.readableOverWallpaper(colors.accent),
                maxLines = 1,
            )
        }
        Box(Modifier.weight(1f))
        CanvasPagerIndicator(status)
    }
}

@Composable
private fun CanvasPagerIndicator(status: CanvasHeaderStatus) {
    if (status.pageCount <= 1) return
    val pageText = stringResource(R.string.jarnfe_canvas_page_of, status.pageIndex + 1, status.pageCount)
    val homeText = stringResource(R.string.jarnfe_canvas_home)
    val description = if (status.pageIndex == status.homePage) "$pageText, $homeText" else pageText
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(status.pageCount) { index ->
            val current = index == status.pageIndex
            if (index == status.homePage) CanvasHomeDot(current) else CanvasPageDot(current)
        }
    }
}

@Composable
private fun CanvasPageDot(current: Boolean) {
    val colors = JarnTheme.colors
    val width by animateDpAsState(if (current) ActiveDotWidth else DotSize, JarnTheme.motion.small.spec())
    val color = if (current) colors.readableOverWallpaper(colors.accent) else colors.textHigh.copy(alpha = INACTIVE_DOT_ALPHA)
    Box(Modifier.size(width, DotSize).background(color, CircleShape))
}

@Composable
private fun CanvasHomeDot(current: Boolean) {
    val colors = JarnTheme.colors
    val iconSize by animateDpAsState(if (current) ActiveHomeIconSize else HomeIconSize, JarnTheme.motion.small.spec())
    val tint = if (current) colors.readableOverWallpaper(colors.accent) else colors.textHigh.copy(alpha = INACTIVE_DOT_ALPHA)
    Image(
        painter = painterResource(R.drawable.ic_jarn_home),
        contentDescription = null,
        modifier = Modifier.size(iconSize),
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
private fun CanvasOptionsIconButton(onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(OptionsButtonSize)
            .focusProperties { canFocus = false }
            .jarnInteractive(CircleShape, source, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_jarn_settings),
            contentDescription = stringResource(R.string.jarnfe_canvas_options_title),
            modifier = Modifier.size(OptionsIconSize),
            colorFilter = ColorFilter.tint(JarnTheme.colors.textHigh),
        )
    }
}
