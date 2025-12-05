package jr.brian.home.ui.components.apps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import jr.brian.home.data.AppPositionManager
import jr.brian.home.model.AppInfo
import jr.brian.home.model.AppPosition
import kotlin.math.max

@Composable
fun FreePositionedAppsLayout(
    apps: List<AppInfo>,
    appPositionManager: AppPositionManager,
    keyboardVisible: Boolean,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
    pageIndex: Int = 0,
    isDragLocked: Boolean = false,
    onAppLongClick: (AppInfo) -> Unit = {}
) {
    val density = LocalDensity.current
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var focusedIndex by remember { mutableIntStateOf(0) }
    val focusRequesters = remember(apps.size) {
        List(apps.size) { FocusRequester() }
    }
    val scrollState = rememberScrollState()

    val appPositions = remember(apps.size) {
        mutableMapOf<Int, Pair<Float, Float>>()
    }

    val positions = appPositionManager.getPositions(pageIndex)

    LaunchedEffect(Unit) {
        if (focusRequesters.isNotEmpty()) {
            focusRequesters[0].requestFocus()
        }
    }

    fun findNearestApp(
        currentIndex: Int,
        direction: Direction
    ): Int? {
        val currentPos = appPositions[currentIndex] ?: return null
        val (currentX, currentY) = currentPos

        val candidates = apps.indices.filter { it != currentIndex }.mapNotNull { index ->
            appPositions[index]?.let { pos ->
                val (x, y) = pos
                when (direction) {
                    Direction.UP -> if (y < currentY) index to (currentY - y) else null
                    Direction.DOWN -> if (y > currentY) index to (y - currentY) else null
                    Direction.LEFT -> if (x < currentX) index to (currentX - x) else null
                    Direction.RIGHT -> if (x > currentX) index to (x - currentX) else null
                }
            }
        }

        return candidates.minByOrNull { it.second }?.first
    }

    var maxY by remember { mutableStateOf(0f) }
    var maxX by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp)
            .verticalScroll(scrollState)
            .onSizeChanged {
                containerSize = it
            }
    ) {
        // Since isFocusable is false, there's no spacer or divider below the icon
        val extraItemHeight = 0f

        // Add sufficient padding at the bottom to ensure all apps are fully visible
        val contentHeight = with(density) {
            val maxIconSize = apps.mapNotNull { app ->
                positions[app.packageName]?.iconSize
            }.maxOrNull() ?: 64f
            val maxIconSizePx = maxIconSize.dp.toPx()
            val bottomPadding = 8.dp.toPx() // Extra padding for safety
            max(
                containerSize.height.toFloat(),
                maxY + maxIconSizePx + extraItemHeight + bottomPadding
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { contentHeight.toDp() })
        ) {
            apps.forEachIndexed { index, app ->
                val position = positions[app.packageName]
                val defaultX = with(density) {
                    val columns = 4
                    val itemWidth = 80.dp.toPx()
                    val spacing = 32.dp.toPx()
                    val column = index % columns
                    val startPadding = 8.dp.toPx()
                    (startPadding + column * (itemWidth + spacing)).toFloat()
                }
                val defaultY = with(density) {
                    val columns = 4
                    val itemHeight = 100.dp.toPx()
                    val spacing = 24.dp.toPx()
                    val row = index / columns
                    val topPadding = 8.dp.toPx()
                    (topPadding + row * (itemHeight + spacing)).toFloat()
                }

                // Get icon size for this app
                val currentIconSize = position?.iconSize ?: 64f
                val iconSizePx = with(density) { currentIconSize.dp.toPx() }
                val fullItemHeight = iconSizePx + extraItemHeight
                val startPaddingPx = with(density) { 16.dp.toPx() }
                val bottomPaddingPx = with(density) { 8.dp.toPx() }

                // Constrain initial positions to ensure they're fully on screen
                val initialX = (position?.x ?: defaultX).coerceIn(
                    minimumValue = 0f,
                    maximumValue = (containerSize.width - iconSizePx - startPaddingPx).coerceAtLeast(
                        0f
                    )
                )
                val initialY = (position?.y ?: defaultY).coerceIn(
                    minimumValue = 0f,
                    maximumValue = if (containerSize.height > 0) {
                        (containerSize.height - fullItemHeight - bottomPaddingPx).coerceAtLeast(0f)
                    } else {
                        position?.y ?: defaultY
                    }
                )

                appPositions[index] = initialX to initialY

                if (initialY > maxY) maxY = initialY
                if (initialX > maxX) maxX = initialX

                FreePositionedAppItem(
                    app = app,
                    keyboardVisible = keyboardVisible,
                    focusRequester = focusRequesters[index],
                    offsetX = initialX,
                    offsetY = initialY,
                    iconSize = position?.iconSize ?: 64f,
                    isFocusable = false,
                    onOffsetChanged = { x, y ->
                        val currentIconSize =
                            appPositionManager.getPosition(pageIndex, app.packageName)?.iconSize
                                ?: 64f

                        // Calculate the full item height (just the icon since focus is disabled)
                        val iconSizePx = with(density) { currentIconSize.dp.toPx() }
                        val fullItemHeight = iconSizePx + extraItemHeight
                        val startPaddingPx = with(density) { 16.dp.toPx() }
                        val bottomPaddingPx =
                            with(density) { 8.dp.toPx() } // Ensure space at bottom

                        // Constrain x and y to keep the entire item fully on screen
                        val constrainedX = x.coerceIn(
                            minimumValue = 0f,
                            maximumValue = (containerSize.width - iconSizePx - startPaddingPx).coerceAtLeast(
                                0f
                            )
                        )
                        val constrainedY = y.coerceIn(
                            minimumValue = 0f,
                            maximumValue = (containerSize.height - fullItemHeight - bottomPaddingPx).coerceAtLeast(0f)
                        )

                        appPositions[index] = constrainedX to constrainedY
                        if (constrainedY > maxY) maxY = constrainedY
                        if (constrainedX > maxX) maxX = constrainedX

                        appPositionManager.savePosition(
                            pageIndex,
                            AppPosition(
                                packageName = app.packageName,
                                x = constrainedX,
                                y = constrainedY,
                                iconSize = currentIconSize
                            )
                        )
                    },
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) },
                    onNavigateUp = {
                        findNearestApp(focusedIndex, Direction.UP)?.let { targetIndex ->
                            focusedIndex = targetIndex
                            focusRequesters[targetIndex].requestFocus()
                        }
                    },
                    onNavigateDown = {
                        findNearestApp(focusedIndex, Direction.DOWN)?.let { targetIndex ->
                            focusedIndex = targetIndex
                            focusRequesters[targetIndex].requestFocus()
                        }
                    },
                    onNavigateLeft = {
                        findNearestApp(focusedIndex, Direction.LEFT)?.let { targetIndex ->
                            focusedIndex = targetIndex
                            focusRequesters[targetIndex].requestFocus()
                        }
                    },
                    onNavigateRight = {
                        findNearestApp(focusedIndex, Direction.RIGHT)?.let { targetIndex ->
                            focusedIndex = targetIndex
                            focusRequesters[targetIndex].requestFocus()
                        }
                    },
                    onFocusChanged = {
                        focusedIndex = index
                    },
                    isDraggingEnabled = !isDragLocked
                )
            }
        }
    }
}

private enum class Direction {
    UP, DOWN, LEFT, RIGHT
}
