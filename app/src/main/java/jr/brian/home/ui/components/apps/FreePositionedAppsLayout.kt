package jr.brian.home.ui.components.apps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import android.content.Context
import android.hardware.display.DisplayManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import jr.brian.home.data.AppPositionManager
import jr.brian.home.model.AlignmentGuide
import jr.brian.home.model.AlignmentState
import jr.brian.home.model.AppInfo
import jr.brian.home.model.AppPosition
import jr.brian.home.model.DistanceMeasurement
import jr.brian.home.model.GuideType
import jr.brian.home.model.WidgetInfo
import jr.brian.home.ui.theme.AlignmentGuideColor
import jr.brian.home.viewmodels.WidgetViewModel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import jr.brian.home.ui.components.dialog.AppOptionsDialog
import jr.brian.home.ui.theme.AlignmentGuideColor
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
import jr.brian.home.util.openAppInfo
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// Data class to represent item bounds for collision detection
private data class ItemBounds(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val isWidget: Boolean = false
)

// Check if two rectangles overlap
private fun checkCollision(
    x1: Float, y1: Float, width1: Float, height1: Float,
    x2: Float, y2: Float, width2: Float, height2: Float,
    padding: Float = 8f
): Boolean {
    return x1 < x2 + width2 + padding &&
            x1 + width1 + padding > x2 &&
            y1 < y2 + height2 + padding &&
            y1 + height1 + padding > y2
}

// Find valid position that doesn't overlap with existing items
private fun findNonOverlappingPosition(
    targetX: Float,
    targetY: Float,
    itemWidth: Float,
    itemHeight: Float,
    itemId: String,
    allBounds: List<ItemBounds>,
    containerWidth: Int,
    containerHeight: Int,
    maxAttempts: Int = 50
): Pair<Float, Float>? {
    // Check if current position is valid
    val hasCollision = allBounds.any { bounds ->
        bounds.id != itemId && checkCollision(
            targetX, targetY, itemWidth, itemHeight,
            bounds.x, bounds.y, bounds.width, bounds.height
        )
    }

    if (!hasCollision) {
        return targetX to targetY
    }

    // Try to find a nearby valid position
    val searchRadius = 20f
    for (attempt in 1..maxAttempts) {
        val radius = searchRadius * attempt
        val angles = listOf(0f, 90f, 180f, 270f, 45f, 135f, 225f, 315f)

        for (angle in angles) {
            val radians = Math.toRadians(angle.toDouble())
            val testX = (targetX + radius * kotlin.math.cos(radians).toFloat())
                .coerceIn(0f, (containerWidth - itemWidth).coerceAtLeast(0f))
            val testY = (targetY + radius * kotlin.math.sin(radians).toFloat())
                .coerceIn(0f, (containerHeight - itemHeight).coerceAtLeast(0f))

            val testHasCollision = allBounds.any { bounds ->
                bounds.id != itemId && checkCollision(
                    testX, testY, itemWidth, itemHeight,
                    bounds.x, bounds.y, bounds.width, bounds.height
                )
            }

            if (!testHasCollision) {
                return testX to testY
            }
        }
    }

    return null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FreePositionedAppsLayout(
    apps: List<AppInfo>,
    appPositionManager: AppPositionManager,
    keyboardVisible: Boolean,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
    pageIndex: Int = 0,
    isDragLocked: Boolean = false,
    onAppLongClick: (AppInfo) -> Unit = {},
    widgets: List<WidgetInfo> = emptyList(),
    widgetViewModel: WidgetViewModel? = null,
    onNavigateToWidgetResize: (WidgetInfo, Int) -> Unit = { _, _ -> },
    widgetEditModeEnabled: Boolean = false
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val widgetPageAppManager = LocalWidgetPageAppManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val appVisibilityManager = LocalAppVisibilityManager.current

    var containerSize by remember(pageIndex) { mutableStateOf(IntSize.Zero) }
    var focusedIndex by remember(pageIndex) { mutableIntStateOf(0) }
    val focusRequesters = remember(pageIndex, apps.size) {
        List(apps.size) { FocusRequester() }
    }
    // Each page should have its own scroll state
    val scrollState = remember(pageIndex) { ScrollState(0) }

    // Dialog state
    var showOptionsDialog by remember(pageIndex) { mutableStateOf(false) }
    var selectedApp by remember(pageIndex) { mutableStateOf<AppInfo?>(null) }

    val hasExternalDisplay = remember {
        val displayManager =
            context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.displays.size > 1
    }

    // Track all item bounds for collision detection
    val allItemBounds = remember { mutableStateOf<List<ItemBounds>>(emptyList()) }

    // Track bounce animations for each item
    val bounceAnimations =
        remember { mutableMapOf<String, Pair<Animatable<Float, *>, Animatable<Float, *>>>() }

    val positions = appPositionManager.getPositions(pageIndex)

    var draggingWidgetId by remember { mutableIntStateOf(-1) }
    // Get positions directly without remember to allow reactivity to position changes
    // The SnapshotStateMap in AppPositionManager will trigger recomposition when needed
    val positions = appPositionManager.getPositions(pageIndex)

    var draggingAppIndex by remember(pageIndex) { mutableIntStateOf(-1) }
    var alignmentState by remember(pageIndex) { mutableStateOf(AlignmentState()) }
    val snapThreshold = with(density) { 12.dp.toPx() } // Distance to trigger snapping
    val borderPadding = with(density) { 4.dp.toPx() } // 4dp border constraint

    // Calculate maxY and maxX on each composition based on current positions
    // This ensures they're always accurate and reset when positions are cleared
    var maxY = 0f
    var maxX = 0f

    LaunchedEffect(Unit) {
        if (focusRequesters.isNotEmpty()) {
            focusRequesters[0].requestFocus()
        }
    }

    // Update item bounds whenever positions change
    LaunchedEffect(appPositions.size, widgets.size, positions) {
        val bounds = mutableListOf<ItemBounds>()

        // Add app bounds
        apps.forEachIndexed { index, app ->
            val pos = appPositions[index]
            val iconSize = appSizes[index] ?: 64f
            val iconSizePx = with(density) { iconSize.dp.toPx() }
            if (pos != null) {
                bounds.add(
                    ItemBounds(
                        id = app.packageName,
                        x = pos.first,
                        y = pos.second,
                        width = iconSizePx,
                        height = iconSizePx,
                        isWidget = false
                    )
                )
            }
        }

        // Add widget bounds
        widgets.forEach { widget ->
            val widgetPosition = positions["widget_${widget.widgetId}"]
            if (widgetPosition != null) {
                val widgetWidthPx = with(density) {
                    val cellWidth = 80.dp
                    (widget.width * cellWidth.value).dp.toPx().coerceAtLeast(200.dp.toPx())
                }
                val widgetHeightPx = with(density) {
                    val cellHeight = 80.dp
                    (widget.height * cellHeight.value).dp.toPx().coerceAtLeast(80.dp.toPx())
                }
                bounds.add(
                    ItemBounds(
                        id = "widget_${widget.widgetId}",
                        x = widgetPosition.x,
                        y = widgetPosition.y,
                        width = widgetWidthPx,
                        height = widgetHeightPx,
                        isWidget = true
                    )
                )
            }
        }

        allItemBounds.value = bounds
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .onSizeChanged {
                containerSize = it
            }
    ) {
        // Pre-calculate maxY to determine content height
        // This is done before rendering so scroll area is correct
        var calculatedMaxY = 0f
        apps.forEachIndexed { index, app ->
            val position = positions[app.packageName]
            val defaultY = with(density) {
                val columns = 4
                val itemHeight = 100.dp.toPx()
                val spacing = 24.dp.toPx()
                val row = index / columns
                val topPadding = max(8.dp.toPx(), borderPadding)
                (topPadding + row * (itemHeight + spacing))
            }
            val iconSize = position?.iconSize ?: 64f
            val iconSizePx = with(density) { iconSize.dp.toPx() }
            val y = position?.y ?: defaultY
            val bottom = y + iconSizePx
            if (bottom > calculatedMaxY) calculatedMaxY = bottom
        }

        val contentHeight = with(density) {
            val bottomPadding = 8.dp.toPx()
            max(
                containerSize.height.toFloat(),
                calculatedMaxY + bottomPadding
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { contentHeight.toDp() })
        ) {
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

                alignmentState.distances.forEach { measurement ->
                    val distanceColor = Color(0xFFFF9800)
                    val textColor = Color.White

                    drawLine(
                        color = distanceColor,
                        start = Offset(measurement.startX, measurement.startY),
                        end = Offset(measurement.endX, measurement.endY),
                        strokeWidth = 2f
                    )

                    // Draw end caps
                    val capSize = 8f
                    if (measurement.isHorizontal) {
                        // Vertical caps for horizontal measurements
                        drawLine(
                            color = distanceColor,
                            start = Offset(measurement.startX, measurement.startY - capSize),
                            end = Offset(measurement.startX, measurement.startY + capSize),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = distanceColor,
                            start = Offset(measurement.endX, measurement.endY - capSize),
                            end = Offset(measurement.endX, measurement.endY + capSize),
                            strokeWidth = 2f
                        )
                    } else {
                        // Horizontal caps for vertical measurements
                        drawLine(
                            color = distanceColor,
                            start = Offset(measurement.startX - capSize, measurement.startY),
                            end = Offset(measurement.startX + capSize, measurement.startY),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = distanceColor,
                            start = Offset(measurement.endX - capSize, measurement.endY),
                            end = Offset(measurement.endX + capSize, measurement.endY),
                            strokeWidth = 2f
                        )
                    }

                    // Draw distance text
                    val distanceText = "${measurement.distance.toInt()}dp"
                    val textX = (measurement.startX + measurement.endX) / 2
                    val textY = (measurement.startY + measurement.endY) / 2

                    drawIntoCanvas { canvas ->
                        val paint = Paint().asFrameworkPaint().apply {
                            color = textColor.toArgb()
                            textSize = 32f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                            setShadowLayer(4f, 0f, 0f, Color.Black.toArgb())
                        }

                        // Draw background rectangle for better readability
                        val textBounds = android.graphics.Rect()
                        paint.getTextBounds(distanceText, 0, distanceText.length, textBounds)
                        val padding = 8f

                        val bgPaint = android.graphics.Paint().apply {
                            color = Color(0xCC000000).toArgb() // Semi-transparent black
                            style = android.graphics.Paint.Style.FILL
                        }

                        canvas.nativeCanvas.drawRoundRect(
                            textX - textBounds.width() / 2 - padding,
                            textY + textBounds.top - padding,
                            textX + textBounds.width() / 2 + padding,
                            textY + textBounds.bottom + padding,
                            8f,
                            8f,
                            bgPaint
                        )

                        canvas.nativeCanvas.drawText(
                            distanceText,
                            textX,
                            textY,
                            paint
                        )
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
                    val startPadding = max(8.dp.toPx(), borderPadding)
                    (startPadding + column * (itemWidth + spacing))
                }
                val defaultY = with(density) {
                    val columns = 4
                    val itemHeight = 100.dp.toPx()
                    val spacing = 24.dp.toPx()
                    val row = index / columns
                    val topPadding = max(8.dp.toPx(), borderPadding)
                    (topPadding + row * (itemHeight + spacing))
                }

                val currentIconSize = position?.iconSize ?: 64f
                val iconSizePx = with(density) { currentIconSize.dp.toPx() }

                val initialX = position?.x ?: defaultX
                val initialY = position?.y ?: defaultY

                val currentBottom = initialY + iconSizePx
                val currentRight = initialX + iconSizePx
                if (currentBottom > maxY) maxY = currentBottom
                if (currentRight > maxX) maxX = currentRight

                // Get or create bounce animation for this app
                val appId = app.packageName
                val bounceAnim = bounceAnimations.getOrPut(appId) {
                    Animatable(0f) to Animatable(0f)
                }

                FreePositionedAppItem(
                    app = app,
                    keyboardVisible = keyboardVisible,
                    focusRequester = focusRequesters[index],
                    offsetX = initialX + bounceAnim.first.value,
                    offsetY = initialY + bounceAnim.second.value,
                    iconSize = currentIconSize,
                    isFocusable = false,
                    onOffsetChanged = { x, y ->
                        val currentIconSize =
                            appPositionManager.getPosition(pageIndex, app.packageName)?.iconSize
                                ?: 64f
                        val iconSizePx = with(density) { currentIconSize.dp.toPx() }

                        val alignment = calculateAlignmentGuides(
                            draggingIndex = index,
                            dragX = x,
                            dragY = y,
                            iconSize = currentIconSize,
                            containerSize = containerSize,
                            snapThreshold = snapThreshold,
                            apps = apps,
                            positions = positions,
                            density = density,
                            borderPadding = borderPadding
                        )
                        alignmentState = alignment

                        val finalX = alignment.snappedX ?: x
                        val finalY = alignment.snappedY ?: y

                        val maxX = containerSize.width.toFloat() - iconSizePx - borderPadding
                        val maxY = contentHeight - iconSizePx - borderPadding

                        val constrainedX = finalX.coerceIn(borderPadding, maxX)
                        val constrainedY = finalY.coerceIn(borderPadding, maxY)

                        appPositionManager.savePosition(
                            pageIndex,
                            AppPosition(
                                packageName = app.packageName,
                                x = constrainedX,
                                y = constrainedY,
                                iconSize = currentIconSize
                            )
                        )

                        if (validPosition != null) {
                            constrainedX = validPosition.first
                            constrainedY = validPosition.second

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
                        } else {
                            // Collision detected, trigger bounce animation
                            val (xAnim, yAnim) = bounceAnim
                            scope.launch {
                                val oldPos = appPositions[index] ?: (initialX to initialY)
                                val bounceDistance = 20f
                                val directionX =
                                    if (constrainedX > oldPos.first) -bounceDistance else bounceDistance
                                val directionY =
                                    if (constrainedY > oldPos.second) -bounceDistance else bounceDistance

                                // Animate bounce
                                launch {
                                    xAnim.animateTo(
                                        directionX,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                    xAnim.animateTo(
                                        0f, animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                                launch {
                                    yAnim.animateTo(
                                        directionY,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                    yAnim.animateTo(
                                        0f, animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        }
                    },
                    onDragStart = {
                        draggingAppIndex = index
                    },
                    onDragEnd = {
                        draggingAppIndex = -1
                        alignmentState = AlignmentState()
                    },
                    onClick = { onAppClick(app) },
                    onLongClick = {
                        if (isDragLocked) {
                            selectedApp = app
                            showOptionsDialog = true
                        }
                    },
                    onFocusChanged = {
                        focusedIndex = index
                    },
                    isDraggingEnabled = !isDragLocked
                )
            }

            // Render widgets if provided
            if (widgets.isNotEmpty() && widgetViewModel != null) {
                widgets.forEachIndexed { index, widget ->
                    val widgetPosition = positions["widget_${widget.widgetId}"]
                    val defaultWidgetX = with(density) { 16.dp.toPx() }
                    val defaultWidgetY = with(density) {
                        val appsHeight = maxY + 100.dp.toPx() + 32.dp.toPx()
                        appsHeight + (index * 180.dp.toPx())
                    }

                    val widgetWidthPx = with(density) {
                        val cellWidth = 80.dp
                        (widget.width * cellWidth.value).dp.toPx().coerceAtLeast(200.dp.toPx())
                    }
                    val widgetHeightPx = with(density) {
                        val cellHeight = 80.dp
                        (widget.height * cellHeight.value).dp.toPx().coerceAtLeast(80.dp.toPx())
                    }

                    val initialWidgetX = (widgetPosition?.x ?: defaultWidgetX).coerceIn(
                        minimumValue = 0f,
                        maximumValue = (containerSize.width - widgetWidthPx).coerceAtLeast(0f)
                    )
                    val initialWidgetY = (widgetPosition?.y ?: defaultWidgetY).coerceIn(
                        minimumValue = 0f,
                        maximumValue = (containerSize.height - widgetHeightPx).coerceAtLeast(0f)
                    )

                    if (initialWidgetY > maxY) maxY = initialWidgetY

                    // Get or create bounce animation for this widget
                    val widgetId = "widget_${widget.widgetId}"
                    val widgetBounceAnim = bounceAnimations.getOrPut(widgetId) {
                        Animatable(0f) to Animatable(0f)
                    }

                    FreePositionedWidgetItem(
                        widget = widget,
                        viewModel = widgetViewModel,
                        pageIndex = pageIndex,
                        offsetX = initialWidgetX + widgetBounceAnim.first.value,
                        offsetY = initialWidgetY + widgetBounceAnim.second.value,
                        editModeEnabled = widgetEditModeEnabled,
                        onOffsetChanged = { x, y ->
                            var constrainedX = x.coerceIn(
                                minimumValue = 0f,
                                maximumValue = (containerSize.width - widgetWidthPx).coerceAtLeast(
                                    0f
                                )
                            )
                            var constrainedY = y.coerceIn(
                                minimumValue = 0f,
                                maximumValue = (containerSize.height - widgetHeightPx).coerceAtLeast(
                                    0f
                                )
                            )

                            // Check for collisions with other items
                            val currentBounds = allItemBounds.value
                            val validPosition = findNonOverlappingPosition(
                                targetX = constrainedX,
                                targetY = constrainedY,
                                itemWidth = widgetWidthPx,
                                itemHeight = widgetHeightPx,
                                itemId = widgetId,
                                allBounds = currentBounds,
                                containerWidth = containerSize.width,
                                containerHeight = containerSize.height
                            )

                            if (validPosition != null) {
                                constrainedX = validPosition.first
                                constrainedY = validPosition.second

                                if (constrainedY > maxY) maxY = constrainedY

                                appPositionManager.savePosition(
                                    pageIndex,
                                    AppPosition(
                                        packageName = widgetId,
                                        x = constrainedX,
                                        y = constrainedY,
                                        iconSize = 64f
                                    )
                                )
                            } else {
                                // Collision detected, trigger bounce animation
                                val (xAnim, yAnim) = widgetBounceAnim
                                scope.launch {
                                    val oldX = widgetPosition?.x ?: defaultWidgetX
                                    val oldY = widgetPosition?.y ?: defaultWidgetY
                                    val bounceDistance = 30f
                                    val directionX =
                                        if (constrainedX > oldX) -bounceDistance else bounceDistance
                                    val directionY =
                                        if (constrainedY > oldY) -bounceDistance else bounceDistance

                                    // Animate bounce
                                    launch {
                                        xAnim.animateTo(
                                            directionX,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                        xAnim.animateTo(
                                            0f, animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                    launch {
                                        yAnim.animateTo(
                                            directionY,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                        yAnim.animateTo(
                                            0f, animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        isDraggingEnabled = !isDragLocked,
                        onNavigateToResize = onNavigateToWidgetResize
                    )
                }
            }
        }
    }

    if (showOptionsDialog && selectedApp != null) {
        val app = selectedApp!!
        val currentPosition = appPositionManager.getPosition(pageIndex, app.packageName)
        val currentIconSize = currentPosition?.iconSize ?: 64f

        AppOptionsDialog(
            app = app,
            currentDisplayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                app.packageName
            ),
            onDismiss = {
                showOptionsDialog = false
                selectedApp = null
            },
            onAppInfoClick = {
                openAppInfo(context, app.packageName)
            },
            onDisplayPreferenceChange = { preference ->
                appDisplayPreferenceManager.setAppDisplayPreference(
                    app.packageName,
                    preference
                )
            },
            hasExternalDisplay = hasExternalDisplay,
            currentIconSize = currentIconSize,
            onIconSizeChange = { newSize ->
                appPositionManager.savePosition(
                    pageIndex,
                    AppPosition(
                        packageName = app.packageName,
                        x = currentPosition?.x ?: 0f,
                        y = currentPosition?.y ?: 0f,
                        iconSize = newSize
                    )
                )
            },
            showResizeOption = true,
            onHideApp = {
                scope.launch {
                    appVisibilityManager.hideApp(pageIndex, app.packageName)
                    widgetPageAppManager.removeVisibleApp(pageIndex, app.packageName)
                }
                showOptionsDialog = false
                selectedApp = null
            }
        )
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
    positions: Map<String, AppPosition>,
    borderPadding: Float
): AlignmentState {
    if (draggingIndex < 0 || containerSize.width == 0) {
        return AlignmentState()
    }

    val guides = mutableListOf<AlignmentGuide>()
    val distances = mutableListOf<DistanceMeasurement>()
    var snappedX: Float? = null
    var snappedY: Float? = null

    val iconSizePx = with(density) { iconSize.dp.toPx() }
    val draggingCenterX = dragX + iconSizePx / 2
    val draggingCenterY = dragY + iconSizePx / 2
    val draggingRight = dragX + iconSizePx
    val draggingBottom = dragY + iconSizePx

    val screenCenterX = containerSize.width / 2f
    val screenCenterY = containerSize.height / 2f

    // Distance threshold for showing measurements (in pixels)
    val distanceThreshold = with(density) { 200.dp.toPx() }

    // Check alignment with screen center
    if (abs(draggingCenterX - screenCenterX) < snapThreshold) {
        guides.add(AlignmentGuide(GuideType.VERTICAL, screenCenterX))
        snappedX = screenCenterX - iconSizePx / 2
    }

    if (abs(draggingCenterY - screenCenterY) < snapThreshold) {
        guides.add(AlignmentGuide(GuideType.HORIZONTAL, screenCenterY))
        snappedY = screenCenterY - iconSizePx / 2
    }

    // Check alignment with other apps and calculate distances
    apps.forEachIndexed { index, otherApp ->
        if (index != draggingIndex) {
            val otherPosition = positions[otherApp.packageName]

            // Calculate default position for apps without saved positions
            val otherX = otherPosition?.x ?: with(density) {
                val columns = 4
                val itemWidth = 80.dp.toPx()
                val spacing = 32.dp.toPx()
                val column = index % columns
                val startPadding = max(8.dp.toPx(), borderPadding)
                (startPadding + column * (itemWidth + spacing))
            }
            val otherY = otherPosition?.y ?: with(density) {
                val columns = 4
                val itemHeight = 100.dp.toPx()
                val spacing = 24.dp.toPx()
                val row = index / columns
                val topPadding = max(8.dp.toPx(), borderPadding)
                (topPadding + row * (itemHeight + spacing))
            }
            val otherIconSize = otherPosition?.iconSize ?: 64f
            val otherIconSizePx = with(density) { otherIconSize.dp.toPx() }
            val otherCenterX = otherX + otherIconSizePx / 2
            val otherCenterY = otherY + otherIconSizePx / 2
            val otherRight = otherX + otherIconSizePx
            val otherBottom = otherY + otherIconSizePx

            // Vertical alignment checks (center, left, right)
            // Center-to-center alignment
            if (abs(draggingCenterX - otherCenterX) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.VERTICAL, otherCenterX))
                // Position dragging app so its center aligns with other app's center
                if (snappedX == null) snappedX = otherCenterX - iconSizePx / 2
            }
            // Left edge alignment
            else if (abs(dragX - otherX) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.VERTICAL, otherX))
                if (snappedX == null) snappedX = otherX
            }
            // Right edge alignment - both right edges should align
            else if (abs(draggingRight - otherRight) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.VERTICAL, otherRight))
                // Position dragging app so its right edge aligns with other app's right edge
                if (snappedX == null) snappedX = otherRight - iconSizePx
            }
            // Left edge to right edge (for spacing next to each other)
            else if (abs(dragX - otherRight) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.VERTICAL, otherRight))
                if (snappedX == null) snappedX = otherRight
            }
            // Right edge to left edge (for spacing next to each other)
            else if (abs(draggingRight - otherX) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.VERTICAL, otherX))
                if (snappedX == null) snappedX = otherX - iconSizePx
            }

            // Horizontal alignment checks (center, top, bottom)
            // Center-to-center alignment
            if (abs(draggingCenterY - otherCenterY) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherCenterY))
                // Position dragging app so its center aligns with other app's center
                if (snappedY == null) snappedY = otherCenterY - iconSizePx / 2
            }
            // Top edge alignment
            else if (abs(dragY - otherY) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherY))
                if (snappedY == null) snappedY = otherY
            }
            // Bottom edge alignment - both bottom edges should align
            else if (abs(draggingBottom - otherBottom) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherBottom))
                // Position dragging app so its bottom edge aligns with other app's bottom edge
                if (snappedY == null) snappedY = otherBottom - iconSizePx
            }
            // Top edge to bottom edge (for spacing above/below each other)
            else if (abs(dragY - otherBottom) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherBottom))
                if (snappedY == null) snappedY = otherBottom
            }
            // Bottom edge to top edge (for spacing above/below each other)
            else if (abs(draggingBottom - otherY) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherY))
                if (snappedY == null) snappedY = otherY - iconSizePx
            }

            // Calculate distances to nearby apps
            // Horizontal distance (left/right)
            val horizontalDistance = if (draggingRight < otherX) {
                // Dragging app is to the left
                otherX - draggingRight
            } else if (dragX > otherRight) {
                // Dragging app is to the right
                dragX - otherRight
            } else {
                null // Apps overlap horizontally
            }

            // Vertical distance (top/bottom)
            val verticalDistance = if (draggingBottom < otherY) {
                // Dragging app is above
                otherY - draggingBottom
            } else if (dragY > otherBottom) {
                // Dragging app is below
                dragY - otherBottom
            } else {
                null // Apps overlap vertically
            }

            // Add horizontal distance measurement if within threshold and apps are roughly aligned vertically
            if (horizontalDistance != null && horizontalDistance > 0 && horizontalDistance < distanceThreshold) {
                val verticalOverlap = min(draggingBottom, otherBottom) - max(dragY, otherY)
                if (verticalOverlap > 0) {
                    val measurementY = max(dragY, otherY) + verticalOverlap / 2
                    val startX = if (draggingRight < otherX) draggingRight else dragX
                    val endX = if (draggingRight < otherX) otherX else otherRight

                    distances.add(
                        DistanceMeasurement(
                            startX = startX,
                            startY = measurementY,
                            endX = endX,
                            endY = measurementY,
                            distance = with(density) { horizontalDistance.toDp().value },
                            isHorizontal = true
                        )
                    )
                }
            }

            // Add vertical distance measurement if within threshold and apps are roughly aligned horizontally
            if (verticalDistance != null && verticalDistance > 0 && verticalDistance < distanceThreshold) {
                val horizontalOverlap = min(draggingRight, otherRight) - max(dragX, otherX)
                if (horizontalOverlap > 0) {
                    val measurementX = max(dragX, otherX) + horizontalOverlap / 2
                    val startY = if (draggingBottom < otherY) draggingBottom else dragY
                    val endY = if (draggingBottom < otherY) otherY else otherBottom

                    distances.add(
                        DistanceMeasurement(
                            startX = measurementX,
                            startY = startY,
                            endX = measurementX,
                            endY = endY,
                            distance = with(density) { verticalDistance.toDp().value },
                            isHorizontal = false
                        )
                    )
                }
            }
        }
    }
    return AlignmentState(
        guides = guides.distinctBy { it.position to it.type },
        snappedX = snappedX,
        snappedY = snappedY,
        distances = distances
    )
}
