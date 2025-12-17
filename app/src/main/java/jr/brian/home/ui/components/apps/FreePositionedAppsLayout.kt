package jr.brian.home.ui.components.apps

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.AppPositionManager
import jr.brian.home.model.AlignmentGuide
import jr.brian.home.model.AlignmentState
import jr.brian.home.model.AppFolder
import jr.brian.home.model.AppInfo
import jr.brian.home.model.AppPosition
import jr.brian.home.model.DistanceMeasurement
import jr.brian.home.model.GuideType
import jr.brian.home.ui.components.dialog.AppOptionsDialog
import jr.brian.home.ui.components.folder.FolderContentDialog
import jr.brian.home.ui.components.folder.FolderIcon
import jr.brian.home.ui.components.folder.FolderOptionsDialog
import jr.brian.home.ui.theme.AlignmentGuideColor
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalFolderManager
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
import jr.brian.home.util.openAppInfo
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

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
    val folderManager = LocalFolderManager.current

    var containerSize by remember(pageIndex) { mutableStateOf(IntSize.Zero) }
    var focusedIndex by remember(pageIndex) { mutableIntStateOf(0) }
    val focusRequesters = remember(pageIndex, apps.size) {
        List(apps.size) { FocusRequester() }
    }
    val scrollState = remember(pageIndex) { ScrollState(0) }

    var showOptionsDialog by remember(pageIndex) { mutableStateOf(false) }
    var selectedApp by remember(pageIndex) { mutableStateOf<AppInfo?>(null) }
    var showFolderOptionsDialog by remember(pageIndex) { mutableStateOf(false) }
    var selectedFolderId by remember(pageIndex) { mutableStateOf<String?>(null) }
    
    val openFolderId by folderManager.openFolderId.collectAsStateWithLifecycle()
    val folders = folderManager.getFolders(pageIndex)
    
    val appsNotInFolders = apps.filter { app ->
        folderManager.isAppInAnyFolder(pageIndex, app.packageName) == null
    }

    val hasExternalDisplay = remember {
        val displayManager =
            context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.displays.size > 1
    }

    val positions = appPositionManager.getPositions(pageIndex)

    var draggingAppIndex by remember(pageIndex) { mutableIntStateOf(-1) }
    var draggingAppPackage by remember(pageIndex) { mutableStateOf<String?>(null) }
    var draggingFolderId by remember(pageIndex) { mutableStateOf<String?>(null) }
    var alignmentState by remember(pageIndex) { mutableStateOf(AlignmentState()) }
    var dragOverFolderId by remember(pageIndex) { mutableStateOf<String?>(null) }
    
    val snapThreshold = with(density) { 12.dp.toPx() }
    val borderPadding = with(density) { 4.dp.toPx() }
    val overlapThreshold = with(density) { 32.dp.toPx() }

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
        var calculatedMaxY = 0f
        appsNotInFolders.forEachIndexed { index, app ->
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
        
        folders.values.forEach { folder ->
            val iconSizePx = with(density) { folder.iconSize.dp.toPx() }
            val bottom = folder.y + iconSizePx + with(density) { 40.dp.toPx() }
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

                    val capSize = 8f
                    if (measurement.isHorizontal) {
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

                        val textBounds = android.graphics.Rect()
                        paint.getTextBounds(distanceText, 0, distanceText.length, textBounds)
                        val padding = 8f

                        val bgPaint = android.graphics.Paint().apply {
                            color = Color(0xCC000000).toArgb()
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

            folders.values.forEach { folder ->
                FolderIcon(
                    folder = folder,
                    apps = apps,
                    keyboardVisible = keyboardVisible,
                    offsetX = folder.x,
                    offsetY = folder.y,
                    onOffsetChanged = { x, y ->
                        folderManager.updateFolderPosition(pageIndex, folder.id, x, y)
                    },
                    onClick = {
                        folderManager.openFolder(folder.id)
                    },
                    onLongClick = {
                        if (isDragLocked) {
                            selectedFolderId = folder.id
                            showFolderOptionsDialog = true
                        }
                    },
                    isDraggingEnabled = !isDragLocked,
                    onDragStart = {
                        draggingFolderId = folder.id
                    },
                    onDragEnd = {
                        draggingFolderId = null
                    }
                )
            }

            appsNotInFolders.forEachIndexed { index, app ->
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
                    onOffsetChanged = { x, y ->
                        val currentIconSize =
                            appPositionManager.getPosition(pageIndex, app.packageName)?.iconSize
                                ?: 64f
                        val iconSizePx = with(density) { currentIconSize.dp.toPx() }

                        var potentialOverlapApp: String? = null
                        var potentialOverlapFolder: String? = null
                        
                        appsNotInFolders.forEachIndexed { otherIndex, otherApp ->
                            if (otherIndex != index) {
                                val otherPos = positions[otherApp.packageName]
                                val otherX = otherPos?.x ?: with(density) {
                                    val columns = 4
                                    val itemWidth = 80.dp.toPx()
                                    val spacing = 32.dp.toPx()
                                    val column = otherIndex % columns
                                    val startPadding = max(8.dp.toPx(), borderPadding)
                                    (startPadding + column * (itemWidth + spacing))
                                }
                                val otherY = otherPos?.y ?: with(density) {
                                    val columns = 4
                                    val itemHeight = 100.dp.toPx()
                                    val spacing = 24.dp.toPx()
                                    val row = otherIndex / columns
                                    val topPadding = max(8.dp.toPx(), borderPadding)
                                    (topPadding + row * (itemHeight + spacing))
                                }
                                val otherIconSize = otherPos?.iconSize ?: 64f
                                val otherIconSizePx = with(density) { otherIconSize.dp.toPx() }

                                val distance = calculateDistance(
                                    x, y, iconSizePx,
                                    otherX, otherY, otherIconSizePx
                                )

                                if (distance < overlapThreshold) {
                                    potentialOverlapApp = otherApp.packageName
                                }
                            }
                        }
                        
                        folders.values.forEach { folder ->
                            val folderIconSizePx = with(density) { folder.iconSize.dp.toPx() }
                            val distance = calculateDistance(
                                x, y, iconSizePx,
                                folder.x, folder.y, folderIconSizePx
                            )
                            
                            if (distance < overlapThreshold) {
                                potentialOverlapFolder = folder.id
                            }
                        }
                        
                        dragOverFolderId = potentialOverlapFolder

                        val alignment = calculateAlignmentGuides(
                            draggingIndex = index,
                            dragX = x,
                            dragY = y,
                            iconSize = currentIconSize,
                            containerSize = containerSize,
                            snapThreshold = snapThreshold,
                            apps = appsNotInFolders,
                            positions = positions,
                            density = density,
                            borderPadding = borderPadding,
                            folders = folders.values.toList()
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
                        draggingAppPackage = app.packageName
                    },
                    onDragEnd = {
                        if (draggingAppPackage != null) {
                            val draggedApp = draggingAppPackage!!
                            val draggedPos = positions[draggedApp]
                            
                            if (draggedPos != null) {
                                var createdFolder = false
                                
                                if (dragOverFolderId != null) {
                                    folderManager.addAppToFolder(pageIndex, dragOverFolderId!!, draggedApp)
                                    appPositionManager.removePosition(pageIndex, draggedApp)
                                    createdFolder = true
                                } else {
                                    appsNotInFolders.forEach { otherApp ->
                                        if (otherApp.packageName != draggedApp) {
                                            val otherPos = positions[otherApp.packageName]
                                            if (otherPos != null) {
                                                val distance = calculateDistance(
                                                    draggedPos.x, draggedPos.y,
                                                    with(density) { draggedPos.iconSize.dp.toPx() },
                                                    otherPos.x, otherPos.y,
                                                    with(density) { otherPos.iconSize.dp.toPx() }
                                                )
                                                
                                                if (distance < overlapThreshold && !createdFolder) {
                                                    val folderId = folderManager.createFolder(
                                                        pageIndex = pageIndex,
                                                        name = context.getString(R.string.folder_default_name),
                                                        apps = listOf(otherApp.packageName, draggedApp),
                                                        x = min(draggedPos.x, otherPos.x),
                                                        y = min(draggedPos.y, otherPos.y),
                                                        iconSize = max(draggedPos.iconSize, otherPos.iconSize)
                                                    )
                                                    
                                                    appPositionManager.removePosition(pageIndex, draggedApp)
                                                    appPositionManager.removePosition(pageIndex, otherApp.packageName)
                                                    createdFolder = true
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        draggingAppIndex = -1
                        draggingAppPackage = null
                        dragOverFolderId = null
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
    
    if (showFolderOptionsDialog && selectedFolderId != null) {
        val folder = folderManager.getFolder(pageIndex, selectedFolderId!!)
        if (folder != null) {
            FolderOptionsDialog(
                onDismiss = {
                    showFolderOptionsDialog = false
                    selectedFolderId = null
                },
                onEditName = {
                    folderManager.openFolder(folder.id)
                },
                onDeleteFolder = {
                    folder.apps.forEach { packageName ->
                        val defaultPos = with(density) {
                            val columns = 4
                            val itemWidth = 80.dp.toPx()
                            val spacing = 32.dp.toPx()
                            val itemHeight = 100.dp.toPx()
                            val vSpacing = 24.dp.toPx()
                            val index = apps.indexOfFirst { it.packageName == packageName }
                            val column = index % columns
                            val row = index / columns
                            val startPadding = max(8.dp.toPx(), borderPadding)
                            val topPadding = max(8.dp.toPx(), borderPadding)
                            AppPosition(
                                packageName = packageName,
                                x = startPadding + column * (itemWidth + spacing),
                                y = topPadding + row * (itemHeight + vSpacing),
                                iconSize = 64f
                            )
                        }
                        appPositionManager.savePosition(pageIndex, defaultPos)
                    }
                    folderManager.deleteFolder(pageIndex, folder.id)
                },
                onResizeIcon = {
                    val currentSize = folder.iconSize
                    val newSize = when {
                        currentSize < 64f -> 64f
                        currentSize < 80f -> 80f
                        currentSize < 96f -> 96f
                        else -> 48f
                    }
                    folderManager.updateFolderSize(pageIndex, folder.id, newSize)
                }
            )
        }
    }
    
    if (openFolderId != null) {
        val folder = folderManager.getFolder(pageIndex, openFolderId!!)
        if (folder != null) {
            FolderContentDialog(
                folder = folder,
                apps = apps,
                onDismiss = {
                    folderManager.closeFolder()
                },
                onAppClick = { app ->
                    folderManager.closeFolder()
                    onAppClick(app)
                },
                onAppRemove = { packageName ->
                    folderManager.removeAppFromFolder(pageIndex, folder.id, packageName)
                    val defaultPos = with(density) {
                        val columns = 4
                        val itemWidth = 80.dp.toPx()
                        val spacing = 32.dp.toPx()
                        val itemHeight = 100.dp.toPx()
                        val vSpacing = 24.dp.toPx()
                        val index = apps.indexOfFirst { it.packageName == packageName }
                        val column = index % columns
                        val row = index / columns
                        val startPadding = max(8.dp.toPx(), borderPadding)
                        val topPadding = max(8.dp.toPx(), borderPadding)
                        AppPosition(
                            packageName = packageName,
                            x = startPadding + column * (itemWidth + spacing),
                            y = topPadding + row * (itemHeight + vSpacing),
                            iconSize = 64f
                        )
                    }
                    appPositionManager.savePosition(pageIndex, defaultPos)
                },
                onFolderNameChange = { newName ->
                    folderManager.updateFolderName(pageIndex, folder.id, newName)
                },
                onDeleteFolder = {
                    folder.apps.forEach { packageName ->
                        val defaultPos = with(density) {
                            val columns = 4
                            val itemWidth = 80.dp.toPx()
                            val spacing = 32.dp.toPx()
                            val itemHeight = 100.dp.toPx()
                            val vSpacing = 24.dp.toPx()
                            val index = apps.indexOfFirst { it.packageName == packageName }
                            val column = index % columns
                            val row = index / columns
                            val startPadding = max(8.dp.toPx(), borderPadding)
                            val topPadding = max(8.dp.toPx(), borderPadding)
                            AppPosition(
                                packageName = packageName,
                                x = startPadding + column * (itemWidth + spacing),
                                y = topPadding + row * (itemHeight + vSpacing),
                                iconSize = 64f
                            )
                        }
                        appPositionManager.savePosition(pageIndex, defaultPos)
                    }
                    folderManager.deleteFolder(pageIndex, folder.id)
                }
            )
        }
    }
}

private fun calculateDistance(
    x1: Float,
    y1: Float,
    size1: Float,
    x2: Float,
    y2: Float,
    size2: Float
): Float {
    val centerX1 = x1 + size1 / 2
    val centerY1 = y1 + size1 / 2
    val centerX2 = x2 + size2 / 2
    val centerY2 = y2 + size2 / 2

    val dx = centerX1 - centerX2
    val dy = centerY1 - centerY2

    return sqrt(dx * dx + dy * dy)
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
    borderPadding: Float,
    folders: List<AppFolder> = emptyList()
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

    val distanceThreshold = with(density) { 200.dp.toPx() }

    if (abs(draggingCenterX - screenCenterX) < snapThreshold) {
        guides.add(AlignmentGuide(GuideType.VERTICAL, screenCenterX))
        snappedX = screenCenterX - iconSizePx / 2
    }

    if (abs(draggingCenterY - screenCenterY) < snapThreshold) {
        guides.add(AlignmentGuide(GuideType.HORIZONTAL, screenCenterY))
        snappedY = screenCenterY - iconSizePx / 2
    }

    apps.forEachIndexed { index, otherApp ->
        if (index != draggingIndex) {
            val otherPosition = positions[otherApp.packageName]

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

            if (abs(draggingCenterX - otherCenterX) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.VERTICAL, otherCenterX))
                if (snappedX == null) snappedX = otherCenterX - iconSizePx / 2
            } else if (abs(dragX - otherX) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.VERTICAL, otherX))
                if (snappedX == null) snappedX = otherX
            } else if (abs(draggingRight - otherRight) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.VERTICAL, otherRight))
                if (snappedX == null) snappedX = otherRight - iconSizePx
            } else if (abs(dragX - otherRight) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.VERTICAL, otherRight))
                if (snappedX == null) snappedX = otherRight
            } else if (abs(draggingRight - otherX) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.VERTICAL, otherX))
                if (snappedX == null) snappedX = otherX - iconSizePx
            }

            if (abs(draggingCenterY - otherCenterY) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherCenterY))
                if (snappedY == null) snappedY = otherCenterY - iconSizePx / 2
            } else if (abs(dragY - otherY) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherY))
                if (snappedY == null) snappedY = otherY
            } else if (abs(draggingBottom - otherBottom) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherBottom))
                if (snappedY == null) snappedY = otherBottom - iconSizePx
            } else if (abs(dragY - otherBottom) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherBottom))
                if (snappedY == null) snappedY = otherBottom
            } else if (abs(draggingBottom - otherY) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherY))
                if (snappedY == null) snappedY = otherY - iconSizePx
            }

            val horizontalDistance = if (draggingRight < otherX) {
                otherX - draggingRight
            } else if (dragX > otherRight) {
                dragX - otherRight
            } else {
                null
            }

            val verticalDistance = if (draggingBottom < otherY) {
                otherY - draggingBottom
            } else if (dragY > otherBottom) {
                dragY - otherBottom
            } else {
                null
            }

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
    
    folders.forEach { folder ->
        val folderIconSizePx = with(density) { folder.iconSize.dp.toPx() }
        val folderCenterX = folder.x + folderIconSizePx / 2
        val folderCenterY = folder.y + folderIconSizePx / 2
        val folderRight = folder.x + folderIconSizePx
        val folderBottom = folder.y + folderIconSizePx
        
        if (abs(draggingCenterX - folderCenterX) < snapThreshold) {
            guides.add(AlignmentGuide(GuideType.VERTICAL, folderCenterX))
            if (snappedX == null) snappedX = folderCenterX - iconSizePx / 2
        } else if (abs(dragX - folder.x) < snapThreshold) {
            guides.add(AlignmentGuide(GuideType.VERTICAL, folder.x))
            if (snappedX == null) snappedX = folder.x
        }
        
        if (abs(draggingCenterY - folderCenterY) < snapThreshold) {
            guides.add(AlignmentGuide(GuideType.HORIZONTAL, folderCenterY))
            if (snappedY == null) snappedY = folderCenterY - iconSizePx / 2
        } else if (abs(dragY - folder.y) < snapThreshold) {
            guides.add(AlignmentGuide(GuideType.HORIZONTAL, folder.y))
            if (snappedY == null) snappedY = folder.y
        }
    }
    
    return AlignmentState(
        guides = guides.distinctBy { it.position to it.type },
        snappedX = snappedX,
        snappedY = snappedY,
        distances = distances
    )
}
