package jr.brian.home.ui.components.apps

import android.widget.Toast
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.AppPositionManager
import jr.brian.home.model.alignment.AlignmentState
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import jr.brian.home.model.ItemRect
import jr.brian.home.model.floaty.BubbleBurst
import jr.brian.home.model.floaty.FloatingAppInit
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.home.ui.components.konfetti.KonfettiPreset
import jr.brian.home.ui.components.konfetti.KonfettiPresets
import jr.brian.home.ui.components.dialog.PinnedRomOptionsDialog
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import jr.brian.home.ui.theme.managers.LocalFloatyModeManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalFolderManager
import jr.brian.home.ui.theme.managers.LocalPinnedRomManager
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.ui.util.rememberHasExternalDisplay
import jr.brian.home.util.launchAppOnOppositeDisplay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Position
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FreePositionedAppsLayout(
    apps: List<AppInfo>,
    appPositionManager: AppPositionManager,
    keyboardVisible: Boolean,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
    pageIndex: Int = 0,
    isDragLocked: Boolean = false,
    forceFloatyMode: Boolean = false,
    allApps: List<AppInfo> = apps,
    bubblePopEnabled: Boolean = false,
    floatyObstacleSpec: FloatyObstacleSpec? = null,
    onBubblePop: (AppInfo) -> Unit = {},
    pinnedRoms: List<PinnedRomInfo> = emptyList(),
    onRomClick: (PinnedRomInfo) -> Unit = {},
    onRomRemove: (PinnedRomInfo) -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val widgetPageAppManager = LocalWidgetPageAppManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val appVisibilityManager = LocalAppVisibilityManager.current
    val customIconManager = LocalCustomIconManager.current
    val folderManager = LocalFolderManager.current
    val floatyModeManager = LocalFloatyModeManager.current
    val gridSettingsManager = LocalGridSettingsManager.current

    val snapEnabled = gridSettingsManager.iconSnapEnabled
    val longPressToastMsg = stringResource(R.string.app_drawer_long_press_app_msg)
    val folders by folderManager.getFolders(pageIndex).collectAsStateWithLifecycle(initialValue = emptyList())

    var containerSize by remember(pageIndex) { mutableStateOf(IntSize.Zero) }
    var focusedIndex by remember(pageIndex) { mutableIntStateOf(0) }
    val focusRequesters = remember(pageIndex, apps.size) {
        List(apps.size) { FocusRequester() }
    }
    val scrollState = remember(pageIndex) { ScrollState(0) }

    val pinnedRomManager = LocalPinnedRomManager.current
    var romForOptions by remember { mutableStateOf<PinnedRomInfo?>(null) }

    val appOptionsDialogState = rememberDialogState<AppInfo>()
    val customIconDialogState = rememberDialogState<AppInfo>()
    val renameDialogState = rememberDialogState<AppInfo>()
    val folderDialogState = rememberDialogState<Folder>()

    val hasExternalDisplay = rememberHasExternalDisplay()

    val positions = appPositionManager.getPositions(pageIndex)

    var draggingAppIndex by remember(pageIndex) { mutableIntStateOf(-1) }
    var draggingRomKey by remember(pageIndex) { mutableStateOf<String?>(null) }
    var alignmentState by remember(pageIndex) { mutableStateOf(AlignmentState()) }
    val snapThreshold = with(density) { 12.dp.toPx() } // Distance to trigger snapping
    val borderPadding = with(density) { 4.dp.toPx() } // 4dp border constraint

    val dragDropHandler = remember(density, snapThreshold, borderPadding) {
        DragDropHandler(density, snapThreshold, borderPadding)
    }
    val positionCalculator = remember(density, borderPadding) {
        PositionCalculator(density, borderPadding)
    }

    var currentContentHeight by remember(pageIndex) { mutableFloatStateOf(0f) }
    var bubbleBurstKey by remember { mutableIntStateOf(0) }
    var bubbleBursts by remember { mutableStateOf<List<BubbleBurst>>(emptyList()) }
    val explodeTemplate = KonfettiPresets.getParties(KonfettiPreset.EXPLODE)
    val (isFloaty, floatingEngine) = rememberInitializedFloatyEngine(
        forceFloatyMode = forceFloatyMode,
        isFloatyModeActive = floatyModeManager.isFloatyEnabledOnTab(pageIndex),
        pageIndex = pageIndex,
        apps = apps,
        folders = folders,
        positions = positions,
        containerSize = containerSize,
        currentContentHeight = currentContentHeight,
        density = density,
        floatyObstacleSpec = floatyObstacleSpec
    )

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
        val contentHeight = ContentHeightCalculator.calculateContentHeight(
            apps = apps,
            folders = folders,
            positions = positions,
            containerSize = containerSize,
            density = density,
            positionCalculator = positionCalculator,
            pinnedRoms = pinnedRoms
        ).also { currentContentHeight = it }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { contentHeight.toDp() })
        ) {
            if (!isFloaty) {
                AlignmentOverlay(alignmentState = alignmentState)
            }

            folders.forEachIndexed { _, folder ->
                val folderApps = allApps.filter { it.packageName in folder.appPackageNames }
                val position = folder.position
                val floatingState = if (isFloaty) floatingEngine.positions[folder.id] else null
                val displayX = floatingState?.x ?: position.x
                val displayY = floatingState?.y ?: position.y

                FolderItem(
                    apps = folderApps,
                    folderName = folder.name,
                    keyboardVisible = keyboardVisible,
                    focusRequester = FocusRequester(),
                    offsetX = displayX,
                    offsetY = displayY,
                    iconSize = position.iconSize,
                    isFocusable = false,
                    customIconManager = customIconManager,
                    backgroundColorArgb = folder.backgroundColorArgb,
                    backgroundImagePath = folder.backgroundImagePath,
                    onOffsetChanged = { x, y ->
                        if (isFloaty) return@FolderItem

                        val dragResult = dragDropHandler.processDragForFolder(
                            dragX = x,
                            dragY = y,
                            iconSize = position.iconSize,
                            containerSize = containerSize,
                            contentHeight = contentHeight,
                            apps = apps,
                            positions = positions,
                            folders = folders,
                            draggingFolderId = folder.id,
                            snapEnabled = snapEnabled
                        )

                        alignmentState = dragResult.alignmentState

                        scope.launch {
                            folderManager.updateFolderPosition(
                                pageIndex,
                                folder.id,
                                AppPosition(
                                    packageName = folder.id,
                                    x = dragResult.finalX,
                                    y = dragResult.finalY,
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
                        folderDialogState.show(folder)
                    },
                    onLongClick = {},
                    isDraggingEnabled = !isDragLocked && !isFloaty
                )
            }

            // Pre-calculate all existing positioned items (apps with saved positions + folders + ROMs)
            val existingPositionedItems = buildList {
                addAll(positionCalculator.getExistingPositionedItems(
                    apps = apps,
                    positions = positions,
                    folders = folders
                ))
                pinnedRoms.forEach { rom ->
                    val pos = positions[rom.key] ?: return@forEach
                    val iconSizePx = with(density) { pos.iconSize.dp.toPx() }
                    add(ItemRect(rom.key, pos.x, pos.y, iconSizePx, iconSizePx))
                }
            }

            apps.forEachIndexed { index, app ->
                val position = positions[app.packageName]
                val currentIconSize = position?.iconSize ?: 64f
                val iconSizePx = with(density) { currentIconSize.dp.toPx() }

                val (savedX, savedY) = if (position != null) {
                    Pair(position.x, position.y)
                } else {
                    // Calculate default position in grid, avoiding existing items
                    val nonOverlapping = positionCalculator.calculateDefaultPosition(
                        index = index,
                        iconSizePx = iconSizePx,
                        existingItems = existingPositionedItems,
                        containerWidth = containerSize.width.toFloat(),
                        contentHeight = contentHeight
                    )

                    // Save the position so subsequent new apps also avoid this spot
                    appPositionManager.savePosition(
                        pageIndex,
                        AppPosition(
                            packageName = app.packageName,
                            x = nonOverlapping.first,
                            y = nonOverlapping.second,
                            iconSize = currentIconSize
                        )
                    )

                    nonOverlapping
                }

                val floatingState = if (isFloaty) {
                    floatingEngine.positions[app.packageName]
                } else null

                val displayX = floatingState?.x ?: savedX
                val displayY = floatingState?.y ?: savedY

                key(app.packageName) {
                    FreePositionedAppItem(
                        app = app,
                        keyboardVisible = keyboardVisible,
                        focusRequester = focusRequesters[index],
                        offsetX = displayX,
                        offsetY = displayY,
                        iconSize = currentIconSize,
                        isFocusable = false,
                        customIconManager = customIconManager,
                        onOffsetChanged = { x, y ->
                            if (isFloaty) return@FreePositionedAppItem // ignore drag in floaty mode

                        val currentIconSize =
                            appPositionManager.getPosition(pageIndex, app.packageName)?.iconSize
                                ?: 64f

                        val dragResult = dragDropHandler.processDragForApp(
                            dragX = x,
                            dragY = y,
                            draggingIndex = index,
                            iconSize = currentIconSize,
                            containerSize = containerSize,
                            contentHeight = contentHeight,
                            apps = apps,
                            positions = positions,
                            folders = folders,
                            excludePackageName = app.packageName,
                            snapEnabled = snapEnabled
                        )

                        alignmentState = dragResult.alignmentState

                            appPositionManager.savePosition(
                                pageIndex,
                                AppPosition(
                                    packageName = app.packageName,
                                    x = dragResult.finalX,
                                    y = dragResult.finalY,
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
                            launchAppOnOppositeDisplay(
                                context = context,
                                packageName = app.packageName,
                                currentPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                                    app.packageName
                                )
                            )
                        },
                        onLongClick = {
                            if (isDragLocked) {
                                appOptionsDialogState.show(app)
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
                        isDraggingEnabled = !isDragLocked && !isFloaty,
                        bubblePopEnabled = bubblePopEnabled && isFloaty,
                        onBubblePopTap = { tapX, tapY ->
                            if (containerSize.width <= 0 || contentHeight <= 0f) return@FreePositionedAppItem
                            val relX = (tapX / containerSize.width.toFloat()).coerceIn(0f, 1f).toDouble()
                            val relY = (tapY / contentHeight).coerceIn(0f, 1f).toDouble()
                            val parties = explodeTemplate.map { party ->
                                party.copy(position = Position.Relative(relX, relY))
                            }
                            val burstId = bubbleBurstKey + 1
                            bubbleBurstKey = burstId
                            bubbleBursts = bubbleBursts + BubbleBurst(
                                id = burstId,
                                parties = parties
                            )
                            scope.launch {
                                delay(700)
                                bubbleBursts = bubbleBursts.filterNot { it.id == burstId }
                            }
                        },
                        onBubblePopComplete = { onBubblePop(app) }
                    )
                }
            }
            pinnedRoms.forEach { rom ->
                val position = positions[rom.key]
                val romIconSize = position?.iconSize ?: ROM_DEFAULT_ICON_SIZE
                val (savedX, savedY) = if (position != null) {
                    Pair(position.x, position.y)
                } else {
                    val iconSizePx = with(density) { romIconSize.dp.toPx() }
                    val nonOverlapping = positionCalculator.calculateDefaultPosition(
                        index = apps.size + pinnedRoms.indexOf(rom),
                        iconSizePx = iconSizePx,
                        existingItems = existingPositionedItems,
                        containerWidth = containerSize.width.toFloat(),
                        contentHeight = contentHeight
                    )
                    appPositionManager.savePosition(
                        pageIndex,
                        AppPosition(
                            packageName = rom.key,
                            x = nonOverlapping.first,
                            y = nonOverlapping.second,
                            iconSize = romIconSize
                        )
                    )
                    nonOverlapping
                }

                key(rom.key) {
                    FreePositionedRomItem(
                        rom = rom,
                        offsetX = savedX,
                        offsetY = savedY,
                        iconSize = romIconSize,
                        isDraggingEnabled = !isDragLocked,
                        onOffsetChanged = { x, y ->
                            val currentRomIconSize =
                                appPositionManager.getPosition(pageIndex, rom.key)?.iconSize
                                    ?: ROM_DEFAULT_ICON_SIZE
                            val dragResult = dragDropHandler.processDragForRom(
                                dragX = x,
                                dragY = y,
                                draggingRomKey = rom.key,
                                iconSize = currentRomIconSize,
                                containerSize = containerSize,
                                contentHeight = contentHeight,
                                apps = apps,
                                positions = positions,
                                folders = folders,
                                pinnedRoms = pinnedRoms,
                                snapEnabled = snapEnabled
                            )
                            alignmentState = dragResult.alignmentState
                            appPositionManager.savePosition(
                                pageIndex,
                                AppPosition(
                                    packageName = rom.key,
                                    x = dragResult.finalX,
                                    y = dragResult.finalY,
                                    iconSize = currentRomIconSize
                                )
                            )
                        },
                        onDragStart = {
                            draggingAppIndex = -1
                            draggingRomKey = rom.key
                        },
                        onDragEnd = {
                            draggingRomKey = null
                            alignmentState = AlignmentState()
                        },
                        onClick = { onRomClick(rom) },
                        onLongClick = { romForOptions = rom }
                    )
                }
            }

            bubbleBursts.forEach { burst ->
                key(burst.id) {
                    KonfettiView(
                        modifier = Modifier.fillMaxSize(),
                        parties = burst.parties
                    )
                }
            }
        }
    }

    FreePositionedDialogsManager(
        appOptionsDialogState = appOptionsDialogState,
        customIconDialogState = customIconDialogState,
        renameDialogState = renameDialogState,
        folderDialogState = folderDialogState,
        pageIndex = pageIndex,
        allApps = allApps,
        context = context,
        hasExternalDisplay = hasExternalDisplay,
        appPositionManager = appPositionManager,
        appDisplayPreferenceManager = appDisplayPreferenceManager,
        scope = scope,
        onHideApp = { packageName ->
            appVisibilityManager.hideApp(pageIndex, packageName)
            widgetPageAppManager.removeVisibleApp(pageIndex, packageName)
        }
    )

    romForOptions?.let { rom ->
        val currentSize = positions[rom.key]?.iconSize ?: ROM_DEFAULT_ICON_SIZE
        PinnedRomOptionsDialog(
            rom = rom,
            onDismiss = { romForOptions = null },
            currentIconSize = currentSize,
            onIconSizeChange = { newSize ->
                val pos = positions[rom.key]
                appPositionManager.savePosition(
                    pageIndex,
                    AppPosition(
                        packageName = rom.key,
                        x = pos?.x ?: 0f,
                        y = pos?.y ?: 0f,
                        iconSize = newSize
                    )
                )
            },
            hasExternalDisplay = hasExternalDisplay,
            currentDisplayPreference = appDisplayPreferenceManager.getAppDisplayPreference(rom.key),
            onDisplayPreferenceChange = { pref ->
                appDisplayPreferenceManager.setAppDisplayPreference(rom.key, pref)
            },
            onMediaTypeSelected = { type ->
                pinnedRomManager.updatePinnedRom(pageIndex, rom.copy(displayMediaType = type))
                romForOptions = null
            },
            onRemove = {
                onRomRemove(rom)
                romForOptions = null
            }
        )
    }
}

@Composable
private fun rememberInitializedFloatyEngine(
    forceFloatyMode: Boolean,
    isFloatyModeActive: Boolean,
    pageIndex: Int,
    apps: List<AppInfo>,
    folders: List<Folder>,
    positions: Map<String, AppPosition>,
    containerSize: IntSize,
    currentContentHeight: Float,
    density: Density,
    floatyObstacleSpec: FloatyObstacleSpec?
): Pair<Boolean, FloatingAppsEngine> {
    val isFloaty = forceFloatyMode || isFloatyModeActive
    val floatingEngine = remember(pageIndex) { FloatingAppsEngine() }

    LaunchedEffect(isFloaty, apps.size, folders.size, containerSize, currentContentHeight, pageIndex) {
        if (!isFloaty || containerSize == IntSize.Zero || currentContentHeight <= 0f) return@LaunchedEffect
        val appInits = apps.mapIndexed { index, app ->
            val pos = positions[app.packageName]
            val iconSize = pos?.iconSize ?: 64f
            val iconSizePx = with(density) { iconSize.dp.toPx() }
            val fallbackSpacing = with(density) { 16.dp.toPx() }
            val fallbackHorizontalPadding = with(density) { 20.dp.toPx() }
            val containerWidth = containerSize.width.toFloat()
            val safeHeight = currentContentHeight
            val columns = (
                (containerWidth - (fallbackHorizontalPadding * 2f) + fallbackSpacing) /
                    (iconSizePx + fallbackSpacing)
                ).toInt().coerceAtLeast(1)
            val column = index % columns
            val row = index / columns
            val fallbackX = (
                fallbackHorizontalPadding + (column * (iconSizePx + fallbackSpacing))
                ).coerceIn(0f, (containerWidth - iconSizePx).coerceAtLeast(0f))
            val fallbackY = (
                fallbackSpacing + (row * (iconSizePx + fallbackSpacing))
                ).coerceIn(0f, (safeHeight - iconSizePx).coerceAtLeast(0f))
            FloatingAppInit(
                id = app.packageName,
                x = pos?.x ?: fallbackX,
                y = pos?.y ?: fallbackY,
                width = iconSizePx
            )
        }
        val folderInits = folders.map { folder ->
            val iconSizePx = with(density) { folder.position.iconSize.dp.toPx() }
            FloatingAppInit(
                id = folder.id,
                x = folder.position.x,
                y = folder.position.y,
                width = iconSizePx
            )
        }
        floatingEngine.initialize(
            apps = appInits + folderInits,
            width = containerSize.width.toFloat(),
            height = currentContentHeight // full scrollable content area
        )
    }
    LaunchedEffect(isFloaty, containerSize, currentContentHeight, floatyObstacleSpec) {
        if (!isFloaty || containerSize == IntSize.Zero || currentContentHeight <= 0f) {
            floatingEngine.setObstacles(emptyList())
            return@LaunchedEffect
        }
        val obstacle = floatyObstacleSpec?.toObstacle(
            containerWidthPx = containerSize.width.toFloat(),
            density = density
        )
        floatingEngine.setObstacles(if (obstacle != null) listOf(obstacle) else emptyList())
    }
    LaunchedEffect(isFloaty, pageIndex) {
        if (!isFloaty) return@LaunchedEffect
        var lastNanos = 0L
        while (isActive) {
            withInfiniteAnimationFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = (frameNanos - lastNanos) / 1_000_000_000f // seconds
                    floatingEngine.tick(dt.coerceAtMost(0.05f)) // cap to avoid jumps
                }
                lastNanos = frameNanos
            }
        }
    }
    return isFloaty to floatingEngine
}
data class FloatyObstacleSpec(
    val width: Dp,
    val height: Dp,
    val topPadding: Dp,
    val endPadding: Dp,
    val fullWidth: Boolean = false,
    val horizontalPadding: Dp = 0.dp
) {
    fun toObstacle(containerWidthPx: Float, density: Density): FloatingObstacle {
        val widthPx = if (fullWidth) {
            val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }
            (containerWidthPx - (horizontalPaddingPx * 2f)).coerceAtLeast(0f)
        } else {
            with(density) { width.toPx() }
        }
        val heightPx = with(density) { height.toPx() }
        val topPx = with(density) { topPadding.toPx() }
        val x = if (fullWidth) {
            with(density) { horizontalPadding.toPx() }
        } else {
            val endPx = with(density) { endPadding.toPx() }
            (containerWidthPx - widthPx - endPx).coerceAtLeast(0f)
        }
        return FloatingObstacle(
            x = x,
            y = topPx,
            width = widthPx,
            height = heightPx
        )
    }
}
