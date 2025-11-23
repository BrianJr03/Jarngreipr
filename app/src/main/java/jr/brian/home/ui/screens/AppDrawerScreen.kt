package jr.brian.home.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import jr.brian.home.R
import jr.brian.home.model.AppInfo
import jr.brian.home.ui.components.AppGridItem
import jr.brian.home.ui.components.OnScreenKeyboard
import jr.brian.home.ui.components.StyledDialogButton
import jr.brian.home.ui.components.WallpaperDisplay
import jr.brian.home.ui.theme.AppCardDark
import jr.brian.home.ui.theme.LocalGridSettingsManager
import jr.brian.home.ui.theme.LocalWallpaperManager
import kotlinx.coroutines.launch

private const val PREFS_NAME = "gaming_launcher_prefs"
private const val KEY_KEYBOARD_VISIBLE = "keyboard_visible"

@Composable
fun AppDrawerScreen(
    apps: List<AppInfo>,
    isLoading: Boolean = false,
    onSettingsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val gridSettingsManager = LocalGridSettingsManager.current
    var searchQuery by remember { mutableStateOf("") }

    val prefs = remember {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }
    var keyboardVisible by remember {
        mutableStateOf(prefs.getBoolean(KEY_KEYBOARD_VISIBLE, true))
    }

    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showAppInfoDialog by remember { mutableStateOf(false) }

    val keyboardFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    val appFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    var savedKeyboardIndex by remember { mutableIntStateOf(0) }
    var savedAppIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(keyboardVisible) {
        prefs.edit {
            putBoolean(KEY_KEYBOARD_VISIBLE, keyboardVisible)
        }
    }

    BackHandler {
        keyboardVisible = !keyboardVisible
    }

    if (showAppInfoDialog && selectedApp != null) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.app_info_title),
                    color = Color.White,
                )
            },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.app_info_message,
                        selectedApp!!.label
                    ),
                    color = Color.White.copy(alpha = 0.8f),
                )
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StyledDialogButton(
                        text = stringResource(id = R.string.dialog_cancel),
                        onClick = { showAppInfoDialog = false },
                    )
                    StyledDialogButton(
                        text = stringResource(id = R.string.dialog_app_info),
                        onClick = {
                            selectedApp?.let { app ->
                                openAppInfo(context, app.packageName)
                            }
                            showAppInfoDialog = false
                        },
                        isPrimary = true,
                    )
                }
            },
            dismissButton = {},
            containerColor = AppCardDark,
            shape = RoundedCornerShape(16.dp),
        )
    }

    val wallpaperManager = LocalWallpaperManager.current

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
    ) {
        WallpaperDisplay(
            wallpaperUri = wallpaperManager.getWallpaperUri(),
            wallpaperType = wallpaperManager.getWallpaperType(),
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            AppSelectionContent(
                apps = apps,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                columns = gridSettingsManager.columnCount,
                keyboardFocusRequesters = keyboardFocusRequesters,
                appFocusRequesters = appFocusRequesters,
                savedKeyboardIndex = savedKeyboardIndex,
                savedAppIndex = savedAppIndex,
                onKeyboardFocusChanged = { savedKeyboardIndex = it },
                onAppFocusChanged = { savedAppIndex = it },
                onAppClick = { app ->
                    launchApp(
                        context = context,
                        packageName = app.packageName
                    )
                },
                onAppLongClick = { app ->
                    selectedApp = app
                    showAppInfoDialog = true
                },
                keyboardVisible = keyboardVisible,
                onSettingsClick = onSettingsClick,
            )
        }
    }
}

@Composable
private fun AppSelectionContent(
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

private fun launchApp(
    context: Context,
    packageName: String
) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun openAppInfo(
    context: Context,
    packageName: String
) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:$packageName".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}