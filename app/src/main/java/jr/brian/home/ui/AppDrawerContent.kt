package jr.brian.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import jr.brian.home.model.AppInfo
import kotlinx.coroutines.launch

@Composable
fun AppSelectionContent(
    apps: List<AppInfo>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    keyboardFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    appFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    savedKeyboardIndex: Int,
    savedAppIndex: Int,
    onKeyboardFocusChanged: (Int) -> Unit,
    onAppFocusChanged: (Int) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit = {},
    keyboardVisible: Boolean = true,
    onSettingsClick: () -> Unit = {},
) {
    val filteredApps =
        remember(apps, searchQuery) {
            if (searchQuery.isBlank()) {
                apps.sortedBy { it.label.uppercase() }
            } else {
                apps
                    .filter { it.label.contains(searchQuery, ignoreCase = true) }
                    .sortedBy { it.label.uppercase() }
            }
        }

    Row(modifier = modifier.fillMaxSize()) {
        if (keyboardVisible) {
            OnScreenKeyboard(
                searchQuery = searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier =
                    Modifier
                        .weight(0.5f),
                keyboardFocusRequesters = keyboardFocusRequesters,
                onFocusChanged = onKeyboardFocusChanged,
                onNavigateRight = {
                    appFocusRequesters[savedAppIndex]?.requestFocus()
                },
                onSettingsClick = onSettingsClick,
            )
        }

        AppGrid(
            apps = filteredApps,
            columns = columns,
            modifier = Modifier.weight(if (keyboardVisible) 0.5f else 1f),
            appFocusRequesters = appFocusRequesters,
            onFocusChanged = onAppFocusChanged,
            onNavigateLeft = {
                if (keyboardVisible) {
                    keyboardFocusRequesters[savedKeyboardIndex]?.requestFocus()
                }
            },
            onAppClick = onAppClick,
            onAppLongClick = onAppLongClick,
            keyboardVisible = keyboardVisible,
        )
    }
}

@Composable
private fun AppGrid(
    columns: Int,
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    appFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    onFocusChanged: (Int) -> Unit = {},
    onNavigateLeft: () -> Unit = {},
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit = {},
    keyboardVisible: Boolean = true,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        appFocusRequesters[0]?.requestFocus()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = if (keyboardVisible) 16.dp else 32.dp,
            vertical = if (keyboardVisible) 0.dp else 16.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(if (keyboardVisible) 16.dp else 32.dp),
        verticalArrangement = Arrangement.spacedBy(if (keyboardVisible) 16.dp else 24.dp),
    ) {
        items(apps.size) { index ->
            val app = apps[index]
            val itemFocusRequester =
                remember(index) {
                    FocusRequester().also { appFocusRequesters[index] = it }
                }

            AppGridItem(
                app = app,
                keyboardVisible = keyboardVisible,
                focusRequester = itemFocusRequester,
                onClick = { onAppClick(app) },
                onLongClick = { onAppLongClick(app) },
                onFocusChanged = { onFocusChanged(index) },
                onNavigateUp = {
                    val prevIndex = index - columns
                    if (prevIndex >= 0) {
                        appFocusRequesters[prevIndex]?.requestFocus()
                        scope.launch {
                            gridState.animateScrollToItem(prevIndex)
                        }
                    }
                },
                onNavigateDown = {
                    val nextIndex = index + columns
                    if (nextIndex < apps.size) {
                        appFocusRequesters[nextIndex]?.requestFocus()
                        scope.launch {
                            gridState.animateScrollToItem(nextIndex)
                        }
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
                    if (nextIndex < apps.size && nextIndex / columns == index / columns) {
                        appFocusRequesters[nextIndex]?.requestFocus()
                    }
                },
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}