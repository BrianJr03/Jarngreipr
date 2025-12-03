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
    onAppLongClick: (AppInfo) -> Unit = {},
    modifier: Modifier = Modifier
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

    // Observe positions to trigger recomposition when they change
    val positions = appPositionManager.positions

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
        val contentHeight = with(density) {
            max(containerSize.height.toFloat(), maxY + 16.dp.toPx())
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

                val initialX = position?.x ?: defaultX
                val initialY = position?.y ?: defaultY

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
                    onOffsetChanged = { x, y ->
                        // Get current icon size from the position manager at the time of drag
                        val currentIconSize =
                            appPositionManager.getPosition(app.packageName)?.iconSize ?: 64f

                        // Calculate bounds with icon size and padding
                        val iconSizePx = with(density) { currentIconSize.dp.toPx() }
                        val startPaddingPx = with(density) { 16.dp.toPx() }

                        // Constrain x and y to keep icon on screen
                        val constrainedX = x.coerceIn(
                            minimumValue = 0f,
                            maximumValue = (containerSize.width - iconSizePx - startPaddingPx).coerceAtLeast(
                                0f
                            )
                        )
                        val constrainedY = y.coerceIn(
                            minimumValue = 0f,
                            maximumValue = (containerSize.height - iconSizePx).coerceAtLeast(0f)
                        )

                        appPositions[index] = constrainedX to constrainedY
                        if (constrainedY > maxY) maxY = constrainedY
                        if (constrainedX > maxX) maxX = constrainedX

                        appPositionManager.savePosition(
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
                    isDraggingEnabled = true
                )
            }
        }
    }
}

private enum class Direction {
    UP, DOWN, LEFT, RIGHT
}
