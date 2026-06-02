package jr.brian.home.ui.components.appsandwidgets

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.Folder
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.home.model.widget.WidgetInfo
import jr.brian.home.ui.components.apps.AppVisibilityDialog
import jr.brian.home.ui.components.widget.AppItem
import jr.brian.home.ui.components.widget.FolderGridItem
import jr.brian.home.ui.components.widget.WidgetItem
import jr.brian.home.viewmodels.WidgetViewModel
import kotlinx.coroutines.launch

fun LazyGridScope.renderAppItems(
    apps: List<AppInfo>,
    items: Any,
    editModeEnabled: Boolean,
    pageIndex: Int
) {
    if (apps.isEmpty() || editModeEnabled) return

    @Suppress("UNCHECKED_CAST")
    (items as List<AppInfo>).forEach { app ->
        item(key = "app_${app.packageName}") {
            AppItem(
                app = app,
                pageIndex = pageIndex
            )
        }
    }
}

fun LazyGridScope.renderWidgetItems(
    widgets: List<WidgetInfo>,
    items: Any,
    columns: Int,
    pageIndex: Int,
    viewModel: WidgetViewModel,
    onNavigateToResize: (WidgetInfo, Int) -> Unit,
    swapModeEnabled: Boolean,
    swapSourceWidgetId: Int?,
    onSwapComplete: () -> Unit,
    onSwapModeEnabled: (Int) -> Unit,
    editModeEnabled: Boolean
) {
    if (widgets.isEmpty()) return

    @Suppress("UNCHECKED_CAST")
    (items as List<WidgetInfo>).forEachIndexed { index, widget ->
        item(
            key = "widget_${widget.widgetId}_${pageIndex}_$index",
            span = { GridItemSpan(widget.width.coerceIn(1, columns)) }
        ) {
            WidgetItem(
                widgetInfo = widget,
                viewModel = viewModel,
                pageIndex = pageIndex,
                onNavigateToResize = onNavigateToResize,
                swapModeEnabled = swapModeEnabled,
                isSwapSource = swapSourceWidgetId == widget.widgetId,
                onSwapSelect = { selectedWidgetId ->
                    if (swapSourceWidgetId != null && swapSourceWidgetId != selectedWidgetId) {
                        viewModel.swapWidgets(
                            swapSourceWidgetId,
                            selectedWidgetId,
                            pageIndex
                        )
                        onSwapComplete()
                    }
                },
                onEnableSwapMode = {
                    onSwapModeEnabled(widget.widgetId)
                },
                editModeEnabled = editModeEnabled
            )
        }
    }
}

fun LazyGridScope.renderRomItems(
    pinnedRoms: List<PinnedRomInfo>,
    onRomClick: (PinnedRomInfo) -> Unit,
    onRomLongClick: (PinnedRomInfo) -> Unit
) {
    if (pinnedRoms.isEmpty()) return

    pinnedRoms.forEach { rom ->
        item(key = "rom_${rom.key}") {
            RomGridItem(
                rom = rom,
                onClick = { onRomClick(rom) },
                onLongClick = { onRomLongClick(rom) }
            )
        }
    }
}

fun LazyGridScope.renderFolderItems(
    folders: List<Folder>,
    allApps: List<AppInfo>,
    editModeEnabled: Boolean,
    onClick: (Folder) -> Unit
) {
    if (folders.isEmpty() || editModeEnabled) return

    folders.forEach { folder ->
        val folderApps = allApps.filter { it.packageName in folder.appPackageNames }
        item(key = "folder_${folder.id}") {
            FolderGridItem(
                folder = folder,
                apps = folderApps,
                onClick = { onClick(folder) }
            )
        }
    }
}

@Composable
fun AppVisibilityDialogForWidgetTab(
    apps: List<AppInfo>,
    visibleApps: Set<String>,
    pageIndex: Int,
    onDismiss: () -> Unit,
    widgetPageAppManager: jr.brian.home.data.WidgetPageAppManager
) {
    val scope = rememberCoroutineScope()

    AppVisibilityDialog(
        apps = apps,
        onDismiss = onDismiss,
        pageIndex = pageIndex,
        isWidgetTabMode = true,
        visibleAppsOverride = visibleApps,
        onToggleAppOverride = { packageName ->
            scope.launch {
                if (packageName in visibleApps) {
                    widgetPageAppManager.removeVisibleApp(pageIndex, packageName)
                } else {
                    widgetPageAppManager.addVisibleApp(pageIndex, packageName)
                }
            }
        },
        onShowAllOverride = {
            scope.launch {
                apps.forEach { app ->
                    if (app.packageName !in visibleApps) {
                        widgetPageAppManager.addVisibleApp(pageIndex, app.packageName)
                    }
                }
            }
        },
        onHideAllOverride = {
            scope.launch {
                apps.forEach { app ->
                    if (app.packageName in visibleApps) {
                        widgetPageAppManager.removeVisibleApp(pageIndex, app.packageName)
                    }
                }
            }
        }
    )
}
