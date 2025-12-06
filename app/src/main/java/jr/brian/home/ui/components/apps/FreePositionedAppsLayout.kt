package jr.brian.home.ui.components.apps

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import jr.brian.home.data.AppPositionManager
import jr.brian.home.model.AlignmentGuide
import jr.brian.home.model.AlignmentState
import jr.brian.home.model.AppInfo
import jr.brian.home.model.AppPosition
import jr.brian.home.model.GuideType
import jr.brian.home.ui.theme.AlignmentGuideColor
import kotlin.math.abs
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

    val appSizes = remember(apps.size) {
        mutableMapOf<Int, Float>()
    }

    val positions = appPositionManager.getPositions(pageIndex)

    var draggingAppIndex by remember { mutableIntStateOf(-1) }
    var alignmentState by remember { mutableStateOf(AlignmentState()) }
    val snapThreshold = with(density) { 12.dp.toPx() } // Distance to trigger snapping

    var maxY by remember { mutableStateOf(0f) }
    var maxX by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        if (focusRequesters.isNotEmpty()) {
            focusRequesters[0].requestFocus()
        }
    }

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
            // Draw alignment guides
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                alignmentState.guides.forEach { guide ->
                    when (guide.type) {
                        GuideType.VERTICAL -> {
                            drawLine(
                                color = AlignmentGuideColor,
                                start = Offset(guide.position, 0f),
                                end = Offset(guide.position, size.height),
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(10f, 10f),
                                    0f
                                )
                            )
                        }

                        GuideType.HORIZONTAL -> {
                            drawLine(
                                color = AlignmentGuideColor,
                                start = Offset(0f, guide.position),
                                end = Offset(size.width, guide.position),
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(10f, 10f),
                                    0f
                                )
                            )
                        }
                    }
                }
            }

            apps.forEachIndexed { index, app ->
                val position = positions[app.packageName]
                val defaultX = with(density) {
                    val columns = 4
                    val itemWidth = 80.dp.toPx()
                    val spacing = 32.dp.toPx()
                    val column = index % columns
                    val startPadding = 8.dp.toPx()
                    (startPadding + column * (itemWidth + spacing))
                }
                val defaultY = with(density) {
                    val columns = 4
                    val itemHeight = 100.dp.toPx()
                    val spacing = 24.dp.toPx()
                    val row = index / columns
                    val topPadding = 8.dp.toPx()
                    (topPadding + row * (itemHeight + spacing))
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
                appSizes[index] = currentIconSize

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

                        // Calculate alignment guides and potential snap positions
                        val alignment = calculateAlignmentGuides(
                            draggingIndex = index,
                            dragX = x,
                            dragY = y,
                            iconSize = currentIconSize,
                            containerSize = containerSize,
                            snapThreshold = snapThreshold,
                            apps = apps,
                            appPositions = appPositions,
                            appSizes = appSizes,
                            density = density
                        )
                        alignmentState = alignment

                        // Use snapped positions if available, otherwise use dragged position
                        val finalX = alignment.snappedX ?: x
                        val finalY = alignment.snappedY ?: y

                        // Constrain x and y to keep the entire item fully on screen
                        val constrainedX = finalX.coerceIn(
                            minimumValue = 0f,
                            maximumValue = (containerSize.width - iconSizePx - startPaddingPx).coerceAtLeast(
                                0f
                            )
                        )
                        val constrainedY = finalY.coerceIn(
                            minimumValue = 0f,
                            maximumValue = (containerSize.height - fullItemHeight - bottomPaddingPx).coerceAtLeast(
                                0f
                            )
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
                    onDragStart = {
                        draggingAppIndex = index
                    },
                    onDragEnd = {
                        draggingAppIndex = -1
                        alignmentState = AlignmentState()
                    },
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) },
                    onFocusChanged = {
                        focusedIndex = index
                    },
                    isDraggingEnabled = !isDragLocked
                )
            }
        }
    }
}

private fun calculateAlignmentGuides(
    dragX: Float,
    dragY: Float,
    iconSize: Float,
    density: Density,
    draggingIndex: Int,
    apps: List<AppInfo>,
    snapThreshold: Float,
    containerSize: IntSize,
    appSizes: Map<Int, Float>,
    appPositions: Map<Int, Pair<Float, Float>>
): AlignmentState {
    if (draggingIndex < 0 || containerSize.width == 0) {
        return AlignmentState()
    }

    val guides = mutableListOf<AlignmentGuide>()
    var snappedX: Float? = null
    var snappedY: Float? = null

    val iconSizePx = with(density) { iconSize.dp.toPx() }
    val draggingCenterX = dragX + iconSizePx / 2
    val draggingCenterY = dragY + iconSizePx / 2
    val draggingRight = dragX + iconSizePx
    val draggingBottom = dragY + iconSizePx

    val startPaddingPx = with(density) { 16.dp.toPx() }
    val screenCenterX = (containerSize.width - startPaddingPx) / 2
    val screenCenterY = containerSize.height / 2f

    // Check alignment with screen center
    if (abs(draggingCenterX - screenCenterX) < snapThreshold) {
        guides.add(AlignmentGuide(GuideType.VERTICAL, screenCenterX))
        snappedX = screenCenterX - iconSizePx / 2
    }

    if (abs(draggingCenterY - screenCenterY) < snapThreshold) {
        guides.add(AlignmentGuide(GuideType.HORIZONTAL, screenCenterY))
        snappedY = screenCenterY - iconSizePx / 2
    }

    // Check alignment with other apps
    apps.forEachIndexed { index, _ ->
        if (index != draggingIndex) {
            val otherPos = appPositions[index]
            val otherIconSize = appSizes[index] ?: 64f
            if (otherPos != null) {
                val (otherX, otherY) = otherPos
                val otherIconSizePx = with(density) { otherIconSize.dp.toPx() }
                val otherCenterX = otherX + otherIconSizePx / 2
                val otherCenterY = otherY + otherIconSizePx / 2
                val otherRight = otherX + otherIconSizePx
                val otherBottom = otherY + otherIconSizePx

                // Vertical alignment checks (center, left, right)
                if (abs(draggingCenterX - otherCenterX) < snapThreshold) {
                    guides.add(AlignmentGuide(GuideType.VERTICAL, otherCenterX))
                    if (snappedX == null) snappedX = otherCenterX - iconSizePx / 2
                } else if (abs(dragX - otherX) < snapThreshold) {
                    guides.add(AlignmentGuide(GuideType.VERTICAL, otherX))
                    if (snappedX == null) snappedX = otherX
                } else if (abs(draggingRight - otherRight) < snapThreshold) {
                    guides.add(AlignmentGuide(GuideType.VERTICAL, otherRight))
                    if (snappedX == null) snappedX = otherRight - iconSizePx
                }

                // Horizontal alignment checks (center, top, bottom)
                if (abs(draggingCenterY - otherCenterY) < snapThreshold) {
                    guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherCenterY))
                    if (snappedY == null) snappedY = otherCenterY - iconSizePx / 2
                } else if (abs(dragY - otherY) < snapThreshold) {
                    guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherY))
                    if (snappedY == null) snappedY = otherY
                } else if (abs(draggingBottom - otherBottom) < snapThreshold) {
                    guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherBottom))
                    if (snappedY == null) snappedY = otherBottom - iconSizePx
                }
            }
        }
    }

    return AlignmentState(
        guides = guides.distinctBy { it.position to it.type },
        snappedX = snappedX,
        snappedY = snappedY
    )
}
