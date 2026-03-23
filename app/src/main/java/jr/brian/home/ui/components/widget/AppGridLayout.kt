package jr.brian.home.ui.components.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.Folder
import jr.brian.home.ui.components.apps.AppGridItem
import kotlinx.coroutines.launch

private val ICON_SIZE_DP = 64.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridLayout(
    columns: Int,
    maxAppsPerPage: Int,
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    appFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    onFocusChanged: (Int) -> Unit = {},
    onNavigateLeft: () -> Unit = {},
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit = {},
    onAppDoubleClick: (AppInfo) -> Unit = {},
    folders: List<Folder> = emptyList(),
    allApps: List<AppInfo> = emptyList(),
    onFolderClick: (Folder) -> Unit = {},
    isHeaderVisible: Boolean = true,
    horizontalSpacing: Dp = 32.dp,
    verticalSpacing: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = 8.dp,
        vertical = if (isHeaderVisible) 8.dp else 20.dp,
    ),
    equalizeMargins: Boolean = false,
    isHomeScreen: Boolean = false,
    gridState: LazyGridState = rememberLazyGridState()
) {

    val displayedApps = remember(apps, maxAppsPerPage) {
        apps.take(maxAppsPerPage)
    }

    LaunchedEffect(Unit) {
        appFocusRequesters[0]?.requestFocus()
    }

    val coroutineScope = rememberCoroutineScope()

    val gridContent: LazyGridScope.() -> Unit = {
        // Render apps first
        items(displayedApps.size) { index ->
            val app = displayedApps[index]
            val itemFocusRequester =
                remember(index) {
                    FocusRequester().also { appFocusRequesters[index] = it }
                }

            AppGridItem(
                app = app,
                focusRequester = itemFocusRequester,
                onClick = { onAppClick(app) },
                onLongClick = { onAppLongClick(app) },
                onDoubleClick = { onAppDoubleClick(app) },
                onFocusChanged = { onFocusChanged(index) },
                isHomeScreen = isHomeScreen,
                onNavigateUp = {
                    val prevIndex = index - columns
                    if (prevIndex >= 0) {
                        coroutineScope.launch {
                            gridState.animateScrollToItem(prevIndex)
                        }
                        appFocusRequesters[prevIndex]?.requestFocus()
                    }
                },
                onNavigateDown = {
                    val nextIndex = index + columns
                    if (nextIndex < displayedApps.size) {
                        coroutineScope.launch {
                            gridState.animateScrollToItem(nextIndex)
                        }
                        appFocusRequesters[nextIndex]?.requestFocus()
                    }
                },
                onNavigateLeft = {
                    if (index % columns == 0) {
                        onNavigateLeft()
                    } else {
                        val prevIndex = index - 1
                        if (prevIndex >= 0) {
                            appFocusRequesters[prevIndex]?.requestFocus()
                        }
                    }
                },
                onNavigateRight = {
                    val nextIndex = index + 1
                    if (nextIndex < displayedApps.size && nextIndex / columns == index / columns) {
                        appFocusRequesters[nextIndex]?.requestFocus()
                    }
                },
            )
        }

        items(folders.size) { index ->
            val folder = folders[index]
            val folderApps = allApps.filter { it.packageName in folder.appPackageNames }
            FolderGridItem(
                folder = folder,
                apps = folderApps,
                onClick = { onFolderClick(folder) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (equalizeMargins) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val topPadding = contentPadding.calculateTopPadding()
            val bottomPadding = contentPadding.calculateBottomPadding()
            // Compute spacing so cells are exactly ICON_SIZE_DP wide, making
            // the visual side margin equal to topPadding regardless of column count.
            val equalizedSpacing = if (columns > 1) {
                ((maxWidth - 2 * topPadding - ICON_SIZE_DP * columns) / (columns - 1))
                    .coerceAtLeast(4.dp)
            } else {
                0.dp
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = topPadding,
                    end = topPadding,
                    top = topPadding,
                    bottom = bottomPadding,
                ),
                horizontalArrangement = Arrangement.spacedBy(equalizedSpacing),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                content = gridContent,
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = gridContent,
        )
    }
}