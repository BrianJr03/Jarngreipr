package jr.brian.home.ui.components.apps

import android.content.Context
import android.widget.Toast
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
import jr.brian.home.model.alignment.AlignmentState
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import jr.brian.home.ui.theme.managers.LocalFolderManager
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.ui.util.rememberHasExternalDisplay
import jr.brian.home.util.launchApp
import jr.brian.home.util.launchAppOnOppositeDisplay
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

    val appOptionsDialogState = rememberDialogState<AppInfo>()
    val customIconDialogState = rememberDialogState<AppInfo>()
    val renameDialogState = rememberDialogState<AppInfo>()
    val folderDialogState = rememberDialogState<Folder>()

    val hasExternalDisplay = rememberHasExternalDisplay()

    val positions = appPositionManager.getPositions(pageIndex)

    var draggingAppIndex by remember(pageIndex) { mutableIntStateOf(-1) }
    var alignmentState by remember(pageIndex) { mutableStateOf(AlignmentState()) }
    val snapThreshold = with(density) { 12.dp.toPx() } // Distance to trigger snapping
    val borderPadding = with(density) { 4.dp.toPx() } // 4dp border constraint

    val dragDropHandler = remember(density, snapThreshold, borderPadding) {
        DragDropHandler(density, snapThreshold, borderPadding)
    }
    val positionCalculator = remember(density, borderPadding) {
        PositionCalculator(density, borderPadding)
    }

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
            positionCalculator = positionCalculator
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { contentHeight.toDp() })
        ) {
            // Alignment guides and distance measurements overlay
            AlignmentOverlay(alignmentState = alignmentState)

            // Apps can exist both in folders and outside, so show all apps
            val filteredApps = apps

            folders.forEachIndexed { _, folder ->
                val folderApps = allApps.filter { it.packageName in folder.appPackageNames }
                val position = folder.position

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
                        val dragResult = dragDropHandler.processDragForFolder(
                            dragX = x,
                            dragY = y,
                            iconSize = position.iconSize,
                            containerSize = containerSize,
                            contentHeight = contentHeight,
                            apps = filteredApps,
                            positions = positions,
                            folders = folders,
                            draggingFolderId = folder.id
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
                    isDraggingEnabled = !isDragLocked
                )
            }

            // Pre-calculate all existing positioned items (apps with saved positions + folders)
            val existingPositionedItems = positionCalculator.getExistingPositionedItems(
                apps = filteredApps,
                positions = positions,
                folders = folders
            )

            filteredApps.forEachIndexed { index, app ->
                val position = positions[app.packageName]
                val currentIconSize = position?.iconSize ?: 64f
                val iconSizePx = with(density) { currentIconSize.dp.toPx() }
                
                val (initialX, initialY) = if (position != null) {
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

                        val dragResult = dragDropHandler.processDragForApp(
                            dragX = x,
                            dragY = y,
                            draggingIndex = index,
                            iconSize = currentIconSize,
                            containerSize = containerSize,
                            contentHeight = contentHeight,
                            apps = filteredApps,
                            positions = positions,
                            folders = folders,
                            excludePackageName = app.packageName
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
                            currentPreference = appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
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
                    isDraggingEnabled = !isDragLocked
                )
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
}
