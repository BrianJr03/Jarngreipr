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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.data.AppPositionManager
import jr.brian.home.model.GuideType
import jr.brian.home.model.alignment.AlignmentState
import jr.brian.home.model.alignment.calculateAlignmentGuides
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import jr.brian.home.model.getAllItemRects
import jr.brian.home.ui.components.dialog.AppOptionsDialog
import jr.brian.home.ui.components.dialog.CustomIconDialog
import jr.brian.home.ui.components.dialog.FolderContentsDialog
import jr.brian.home.ui.theme.AlignmentGuideColor
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import jr.brian.home.ui.theme.managers.LocalFolderManager
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
import jr.brian.home.util.findNonOverlappingPosition
import jr.brian.home.util.launchApp
import jr.brian.home.util.openAppInfo
import kotlinx.coroutines.launch
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
    allApps: List<AppInfo> = apps
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val widgetPageAppManager = LocalWidgetPageAppManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val appVisibilityManager = LocalAppVisibilityManager.current
    val customIconManager = LocalCustomIconManager.current
    val folderManager = LocalFolderManager.current

    val longPressToastMsg = stringResource(R.string.app_drawer_long_press_app_msg)
    val folders by folderManager.getFolders(pageIndex).collectAsStateWithLifecycle(initialValue = emptyList())

    var containerSize by remember(pageIndex) { mutableStateOf(IntSize.Zero) }
    var focusedIndex by remember(pageIndex) { mutableIntStateOf(0) }
    val focusRequesters = remember(pageIndex, apps.size) {
        List(apps.size) { FocusRequester() }
    }
    val scrollState = remember(pageIndex) { ScrollState(0) }

    var showOptionsDialog by remember(pageIndex) { mutableStateOf(false) }
    var showCustomIconDialog by remember(pageIndex) { mutableStateOf(false) }
    var selectedApp by remember(pageIndex) { mutableStateOf<AppInfo?>(null) }
    var showFolderDialog by remember(pageIndex) { mutableStateOf(false) }
    var selectedFolder by remember(pageIndex) { mutableStateOf<Folder?>(null) }

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
        
        folders.forEach { folder ->
            val position = folder.position
            val iconSizePx = with(density) { position.iconSize.dp.toPx() }
            val bottom = position.y + iconSizePx
            if (bottom > calculatedMaxY) calculatedMaxY = bottom
        }
        
        // Apps can exist both in folders and outside, so don't filter them out
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

            // Apps can exist both in folders and outside, so show all apps
            val filteredApps = apps

            folders.forEachIndexed { _, folder ->
                val folderApps = allApps.filter { it.packageName in folder.appPackageNames }
                val position = folder.position
                
                val currentBottom = position.y + with(density) { position.iconSize.dp.toPx() }
                val currentRight = position.x + with(density) { position.iconSize.dp.toPx() }
                if (currentBottom > maxY) maxY = currentBottom
                if (currentRight > maxX) maxX = currentRight

                FolderItem(
                    apps = folderApps,
                    folderName = folder.name,
                    keyboardVisible = keyboardVisible,
                    focusRequester = FocusRequester(),
                    offsetX = position.x,
                    offsetY = position.y,
                    iconSize = position.iconSize,
                    isFocusable = false,
                    customIconManager = customIconManager,
                    onOffsetChanged = { x, y ->
                        val iconSizePx = with(density) { position.iconSize.dp.toPx() }

                        val alignment = calculateAlignmentGuides(
                            draggingIndex = -1,
                            dragX = x,
                            dragY = y,
                            iconSize = position.iconSize,
                            containerSize = containerSize,
                            snapThreshold = snapThreshold,
                            apps = filteredApps,
                            positions = positions,
                            density = density,
                            borderPadding = borderPadding,
                            folders = folders,
                            draggingFolderId = folder.id
                        )
                        alignmentState = alignment

                        val snappedX = alignment.snappedX ?: x
                        val snappedY = alignment.snappedY ?: y

                        val maxX = containerSize.width.toFloat() - iconSizePx - borderPadding
                        val maxY = contentHeight - iconSizePx - borderPadding

                        val constrainedX = snappedX.coerceIn(borderPadding, maxX)
                        val constrainedY = snappedY.coerceIn(borderPadding, maxY)

                        // Check for collisions and adjust position
                        val allItems = getAllItemRects(filteredApps, positions, folders, density)
                        val (finalX, finalY) = findNonOverlappingPosition(
                            targetX = constrainedX,
                            targetY = constrainedY,
                            targetSize = iconSizePx,
                            excludeId = folder.id,
                            allItems = allItems,
                            containerWidth = containerSize.width.toFloat(),
                            containerHeight = contentHeight,
                            borderPadding = borderPadding
                        )

                        scope.launch {
                            folderManager.updateFolderPosition(
                                pageIndex,
                                folder.id,
                                AppPosition(
                                    packageName = folder.id,
                                    x = finalX,
                                    y = finalY,
                                    iconSize = position.iconSize
                                )
                            )
                        }
                    },
                    onDragStart = {
                        draggingAppIndex = -1
                    },
                    onDragEnd = {
                        draggingAppIndex = -1
                        alignmentState = AlignmentState()
                    },
                    onClick = {
                        selectedFolder = folder
                        showFolderDialog = true
                    },
                    onLongClick = {},
                    isDraggingEnabled = !isDragLocked
                )
            }

            // Pre-calculate all existing positioned items (apps with saved positions + folders)
            val existingPositionedItems = getAllItemRects(
                apps = filteredApps.filter { positions[it.packageName] != null },
                positions = positions,
                folders = folders,
                density = density
            )

            filteredApps.forEachIndexed { index, app ->
                val position = positions[app.packageName]
                val currentIconSize = position?.iconSize ?: 64f
                val iconSizePx = with(density) { currentIconSize.dp.toPx() }
                
                val (initialX, initialY) = if (position != null) {
                    Pair(position.x, position.y)
                } else {
                    // Calculate default position in grid
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
                    
                    // Check if default position overlaps with ANY existing positioned item
                    val nonOverlapping = findNonOverlappingPosition(
                        targetX = defaultX,
                        targetY = defaultY,
                        targetSize = iconSizePx,
                        excludeId = app.packageName,
                        allItems = existingPositionedItems,
                        containerWidth = containerSize.width.toFloat().takeIf { it > 0 } ?: 1080f,
                        containerHeight = contentHeight.takeIf { it > 0 } ?: 1920f,
                        borderPadding = borderPadding
                    )
                    
                    // Save the position so subsequent new apps also avoid this spot
                    if (nonOverlapping.first != defaultX || nonOverlapping.second != defaultY) {
                        appPositionManager.savePosition(
                            pageIndex,
                            AppPosition(
                                packageName = app.packageName,
                                x = nonOverlapping.first,
                                y = nonOverlapping.second,
                                iconSize = currentIconSize
                            )
                        )
                    }
                    
                    nonOverlapping
                }

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
                            apps = filteredApps,
                            positions = positions,
                            density = density,
                            borderPadding = borderPadding,
                            folders = folders
                        )
                        alignmentState = alignment

                        val snappedX = alignment.snappedX ?: x
                        val snappedY = alignment.snappedY ?: y

                        val maxX = containerSize.width.toFloat() - iconSizePx - borderPadding
                        val maxY = contentHeight - iconSizePx - borderPadding

                        val constrainedX = snappedX.coerceIn(borderPadding, maxX)
                        val constrainedY = snappedY.coerceIn(borderPadding, maxY)

                        // Check for collisions and adjust position
                        val allItems = getAllItemRects(filteredApps, positions, folders, density)
                        val (finalX, finalY) = findNonOverlappingPosition(
                            targetX = constrainedX,
                            targetY = constrainedY,
                            targetSize = iconSizePx,
                            excludeId = app.packageName,
                            allItems = allItems,
                            containerWidth = containerSize.width.toFloat(),
                            containerHeight = contentHeight,
                            borderPadding = borderPadding
                        )

                        appPositionManager.savePosition(
                            pageIndex,
                            AppPosition(
                                packageName = app.packageName,
                                x = finalX,
                                y = finalY,
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
                    onDoubleClick = {
                        // Launch on opposite display from current preference
                        val currentPreference = appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                        val oppositePreference = if (currentPreference == DisplayPreference.PRIMARY_DISPLAY) {
                            DisplayPreference.CURRENT_DISPLAY
                        } else {
                            DisplayPreference.PRIMARY_DISPLAY
                        }
                        launchApp(context, app.packageName, oppositePreference)
                    },
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
            onDismiss = {
                showCustomIconDialog = false
                selectedApp = null
            }
        )
    }

    if (showFolderDialog && selectedFolder != null) {
        val folderApps = allApps.filter { it.packageName in selectedFolder!!.appPackageNames }
        FolderContentsDialog(
            folderName = selectedFolder!!.name,
            apps = folderApps,
            folderId = selectedFolder!!.id,
            pageIndex = pageIndex,
            allApps = allApps,
            onDismiss = {
                showFolderDialog = false
                selectedFolder = null
            }
        )
    }
}
