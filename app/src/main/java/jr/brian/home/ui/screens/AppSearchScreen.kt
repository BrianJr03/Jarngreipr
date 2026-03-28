package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.ui.extensions.combinedClickWithHaptic
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.components.QwertyKeyboard
import jr.brian.home.ui.components.VerticalKeyboard
import jr.brian.home.ui.components.apps.AppIconImage
import jr.brian.home.ui.components.dialog.RenameAppDialog
import jr.brian.home.ui.components.dialog.SearchAppOptionsDialog
import jr.brian.home.ui.components.onboarding.SearchOnboardingOverlay
import jr.brian.home.ui.components.settings.AppName
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalCustomIconManager
import jr.brian.home.ui.theme.managers.LocalIconShapeManager
import jr.brian.home.ui.theme.managers.LocalCustomAppNameManager
import jr.brian.home.ui.theme.managers.LocalSearchLayoutManager
import jr.brian.home.ui.util.rememberHasExternalDisplay
import jr.brian.home.util.launchApp
import jr.brian.home.util.launchAppOnOppositeDisplay
import jr.brian.home.util.openAppInfo

@Composable
fun AppSearchScreen(
    allApps: List<AppInfo>,
    onDismiss: () -> Unit = {},
    onNavigateToRomSearch: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchLayoutManager = LocalSearchLayoutManager.current
    val isHorizontalLayout = searchLayoutManager.isHorizontalLayout
    val hasCompletedOnboarding = searchLayoutManager.hasCompletedOnboarding
    var showOnboarding by remember { mutableStateOf(!hasCompletedOnboarding) }
    val customAppNameManager = LocalCustomAppNameManager.current
    val customNames by customAppNameManager.customNames.collectAsStateWithLifecycle()

    BackHandler(onBack = onDismiss)

    val filteredApps = remember(allApps, searchQuery, customNames) {
        if (searchQuery.isBlank()) {
            allApps
        } else {
            allApps.filter { app ->
                val customName = customNames[app.packageName]
                app.label.contains(searchQuery, ignoreCase = true) ||
                    customName?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = OledBackgroundColor,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isHorizontalLayout) {
                HorizontalSearchLayout(
                    searchQuery = searchQuery,
                    filteredApps = filteredApps,
                    onQueryChange = { searchQuery = it },
                    onFlipLayout = { searchLayoutManager.toggleLayout() },
                    onNavigateToRomSearch = onNavigateToRomSearch
                )
            } else {
                VerticalSearchLayout(
                    searchQuery = searchQuery,
                    filteredApps = filteredApps,
                    onQueryChange = { searchQuery = it },
                    onFlipLayout = { searchLayoutManager.toggleLayout() },
                    onNavigateToRomSearch = onNavigateToRomSearch
                )
            }
        }

        if (showOnboarding) {
            SearchOnboardingOverlay(
                onComplete = {
                    showOnboarding = false
                    searchLayoutManager.markOnboardingComplete()
                }
            )
        }
    }
}

@Composable
private fun VerticalSearchLayout(
    searchQuery: String,
    filteredApps: List<AppInfo>,
    onQueryChange: (String) -> Unit,
    onFlipLayout: () -> Unit,
    onNavigateToRomSearch: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            AppGrid(
                apps = filteredApps,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .weight(.8f)
                .fillMaxSize()
                .background(OledBackgroundColor)
        ) {
            val keyboardFocusRequesters =
                remember { SnapshotStateMap<Int, FocusRequester>() }
            var focusedKeyIndex by remember { mutableIntStateOf(0) }

            VerticalKeyboard(
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                keyboardFocusRequesters = keyboardFocusRequesters,
                onFocusChanged = { focusedKeyIndex = it },
                onNavigateRight = {},
                onFlipLayout = onFlipLayout,
                onReopenResults = onNavigateToRomSearch,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun HorizontalSearchLayout(
    searchQuery: String,
    filteredApps: List<AppInfo>,
    onQueryChange: (String) -> Unit,
    onFlipLayout: () -> Unit,
    onNavigateToRomSearch: () -> Unit = {},
) {
    var showSpecialCharRow by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            HorizontalAppGrid(
                apps = filteredApps,
                showAppNames = !showSpecialCharRow,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(OledBackgroundColor)
        ) {
            val keyboardFocusRequesters =
                remember { SnapshotStateMap<Int, FocusRequester>() }
            var focusedKeyIndex by remember { mutableIntStateOf(0) }

            QwertyKeyboard(
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                keyboardFocusRequesters = keyboardFocusRequesters,
                onFocusChanged = { focusedKeyIndex = it },
                onFlipLayout = onFlipLayout,
                showSpecialCharRow = showSpecialCharRow,
                onReopenResults = onNavigateToRomSearch,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGrid(
    apps: List<AppInfo>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var appForRename by remember { mutableStateOf<AppInfo?>(null) }

    val hasExternalDisplay = rememberHasExternalDisplay()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .background(OledBackgroundColor)
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            AppGridItem(
                app = app,
                onAppClick = {
                    val displayPreference = if (hasExternalDisplay) {
                        appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                    } else {
                        DisplayPreference.CURRENT_DISPLAY
                    }
                    launchApp(
                        context = context,
                        packageName = app.packageName,
                        displayPreference = displayPreference
                    )
                },
                onAppDoubleClick = {
                    launchAppOnOppositeDisplay(
                        context = context,
                        packageName = app.packageName,
                        currentPreference = appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                    )
                },
                onAppLongClick = {
                    selectedApp = app
                }
            )
        }
    }

    selectedApp?.let { app ->
        SearchAppOptionsDialog(
            app = app,
            currentDisplayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                app.packageName
            ),
            onDismiss = { selectedApp = null },
            onAppInfoClick = {
                openAppInfo(context, app.packageName)
            },
            onDisplayPreferenceChange = { preference ->
                appDisplayPreferenceManager.setAppDisplayPreference(
                    app.packageName,
                    preference
                )
            },
            onRenameClick = {
                appForRename = app
                selectedApp = null
            },
            hasExternalDisplay = hasExternalDisplay
        )
    }

    appForRename?.let { app ->
        RenameAppDialog(
            packageName = app.packageName,
            appLabel = app.label,
            onDismiss = { appForRename = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontalAppGrid(
    apps: List<AppInfo>,
    showAppNames: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var appForRename by remember { mutableStateOf<AppInfo?>(null) }

    val hasExternalDisplay = rememberHasExternalDisplay()

    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = modifier
            .background(OledBackgroundColor)
            .padding(horizontal = 8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            HorizontalAppGridItem(
                app = app,
                showAppName = showAppNames,
                onAppClick = {
                    val displayPreference = if (hasExternalDisplay) {
                        appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                    } else {
                        DisplayPreference.CURRENT_DISPLAY
                    }
                    launchApp(
                        context = context,
                        packageName = app.packageName,
                        displayPreference = displayPreference
                    )
                },
                onAppDoubleClick = {
                    launchAppOnOppositeDisplay(
                        context = context,
                        packageName = app.packageName,
                        currentPreference = appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                    )
                },
                onAppLongClick = {
                    selectedApp = app
                }
            )
        }
    }

    selectedApp?.let { app ->
        SearchAppOptionsDialog(
            app = app,
            currentDisplayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                app.packageName
            ),
            onDismiss = { selectedApp = null },
            onAppInfoClick = {
                openAppInfo(context, app.packageName)
            },
            onDisplayPreferenceChange = { preference ->
                appDisplayPreferenceManager.setAppDisplayPreference(
                    app.packageName,
                    preference
                )
            },
            onRenameClick = {
                appForRename = app
                selectedApp = null
            },
            hasExternalDisplay = hasExternalDisplay
        )
    }

    appForRename?.let { app ->
        RenameAppDialog(
            packageName = app.packageName,
            appLabel = app.label,
            onDismiss = { appForRename = null }
        )
    }
}

@Composable
private fun HorizontalAppGridItem(
    app: AppInfo,
    modifier: Modifier = Modifier,
    showAppName: Boolean = true,
    onAppClick: () -> Unit,
    onAppDoubleClick: () -> Unit = {},
    onAppLongClick: () -> Unit
) {
    val customIconManager = LocalCustomIconManager.current
    val iconShapeManager = LocalIconShapeManager.current
    val iconShape = iconShapeManager.iconShape.toComposeShape()
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .width(80.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused),
                shape = RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .combinedClickWithHaptic(
                haptic = haptic,
                onClick = onAppClick,
                onDoubleClick = onAppDoubleClick,
                onLongClick = onAppLongClick
            )
            .focusable()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(iconShape),
            contentAlignment = Alignment.Center
        ) {
            AppIconImage(
                defaultIcon = app.icon,
                packageName = app.packageName,
                contentDescription = stringResource(R.string.app_icon_description, app.label),
                customIconManager = customIconManager,
                modifier = Modifier.matchParentSize()
            )
        }

        Spacer(Modifier.height(2.dp))
        AnimatedVisibility(showAppName) {
            app.AppName()
        }
    }
}

@Composable
private fun AppGridItem(
    app: AppInfo,
    modifier: Modifier = Modifier,
    onAppClick: () -> Unit,
    onAppDoubleClick: () -> Unit = {},
    onAppLongClick: () -> Unit
) {
    val customIconManager = LocalCustomIconManager.current
    val iconShapeManager = LocalIconShapeManager.current
    val iconShape = iconShapeManager.iconShape.toComposeShape()
    var isFocused by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .combinedClickWithHaptic(
                haptic = haptic,
                onClick = onAppClick,
                onDoubleClick = onAppDoubleClick,
                onLongClick = onAppLongClick
            )
            .focusable()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(iconShape),
            contentAlignment = Alignment.Center
        ) {
            AppIconImage(
                defaultIcon = app.icon,
                packageName = app.packageName,
                contentDescription = stringResource(R.string.app_icon_description, app.label),
                customIconManager = customIconManager,
                modifier = Modifier.matchParentSize()
            )
        }

        Spacer(Modifier.height(4.dp))
        app.AppName()
    }
}
