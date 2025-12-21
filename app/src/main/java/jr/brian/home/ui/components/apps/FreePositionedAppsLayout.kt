package jr.brian.home.ui.components.apps

import android.content.Context
import android.hardware.display.DisplayManager
import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.data.AppPositionManager
import jr.brian.home.model.AlignmentGuide
import jr.brian.home.model.AlignmentState
import jr.brian.home.model.AppInfo
import jr.brian.home.model.AppPosition
import jr.brian.home.model.DistanceMeasurement
import jr.brian.home.model.GuideType
import jr.brian.home.ui.components.dialog.AppOptionsDialog
import jr.brian.home.ui.components.dialog.CustomIconDialog
import jr.brian.home.ui.theme.AlignmentGuideColor
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
import jr.brian.home.util.openAppInfo
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun FreePositionedAppsLayout(
    apps: List<AppInfo>,
    appPositionManager: AppPositionManager,
    keyboardVisible: Boolean,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
    pageIndex: Int = 0,
    isDragLocked: Boolean = false
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val widgetPageAppManager = LocalWidgetPageAppManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val appVisibilityManager = LocalAppVisibilityManager.current
    val customIconManager = LocalCustomIconManager.current

    val longPressToastMsg = stringResource(R.string.app_drawer_long_press_app_msg)

    var containerSize by remember(pageIndex) { mutableStateOf(IntSize.Zero) }
    var focusedIndex by remember(pageIndex) { mutableIntStateOf(0) }
    val focusRequesters = remember(pageIndex, apps.size) {
        List(apps.size) { FocusRequester() }
    }
    // Each page should have its own scroll state
    val scrollState = remember(pageIndex) { ScrollState(0) }

    // Dialog state
    var showOptionsDialog by remember(pageIndex) { mutableStateOf(false) }
    var showCustomIconDialog by remember(pageIndex) { mutableStateOf(false) }
    var selectedApp by remember(pageIndex) { mutableStateOf<AppInfo?>(null) }

    val hasExternalDisplay = remember {
        val displayManager =
            context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.displays.size > 1
    }

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

                FreePositionedAppItem(
                    app = app,
                    keyboardVisible = keyboardVisible,
                    focusRequester = focusRequesters[index],
                    offsetX = initialX,
                    offsetY = initialY,
                    iconSize = currentIconSize,
                    isFocusable = false,
                    customIconManager = customIconManager,
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
                        } else {
                            Toast.makeText(
                                context,
                                longPressToastMsg,
                                Toast.LENGTH_SHORT
                            ).show()
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
            },
            onCustomIconClick = {
                showOptionsDialog = false
                showCustomIconDialog = true
            }
        )
    }

    if (showCustomIconDialog && selectedApp != null) {
        CustomIconDialog(
            packageName = selectedApp!!.packageName,
            appLabel = selectedApp!!.label,
            customIconManager = customIconManager,
            onDismiss = {
                showCustomIconDialog = false
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
