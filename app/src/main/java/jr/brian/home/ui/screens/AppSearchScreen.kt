package jr.brian.home.ui.screens

import android.content.Context
import android.hardware.display.DisplayManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.AppInfo
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.components.OnScreenKeyboard
import jr.brian.home.ui.components.dialog.SearchAppOptionsDialog
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.util.launchApp
import jr.brian.home.util.openAppInfo

@Composable
fun AppSearchScreen(
    allApps: List<AppInfo>
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) {
            allApps
        } else {
            allApps.filter { app ->
                app.label.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Surface(
        color = OledBackgroundColor,
        modifier = Modifier.fillMaxSize()
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
                    .weight(1f)
                    .fillMaxSize()
                    .background(OledBackgroundColor)
            ) {
                val keyboardFocusRequesters =
                    remember { SnapshotStateMap<Int, FocusRequester>() }
                var focusedKeyIndex by remember { mutableIntStateOf(0) }

                OnScreenKeyboard(
                    searchQuery = searchQuery,
                    onQueryChange = { searchQuery = it },
                    keyboardFocusRequesters = keyboardFocusRequesters,
                    onFocusChanged = { focusedKeyIndex = it },
                    onNavigateRight = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
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

    val hasExternalDisplay = remember {
        val displayManager =
            context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.displays.size > 1
    }

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
                onAppLongClick = {
                    selectedApp = app
                }
            )
        }
    }

    if (selectedApp != null) {
        SearchAppOptionsDialog(
            app = selectedApp!!,
            currentDisplayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                selectedApp!!.packageName
            ),
            onDismiss = { selectedApp = null },
            onAppInfoClick = {
                openAppInfo(context, selectedApp!!.packageName)
            },
            onDisplayPreferenceChange = { preference ->
                appDisplayPreferenceManager.setAppDisplayPreference(
                    selectedApp!!.packageName,
                    preference
                )
            },
            hasExternalDisplay = hasExternalDisplay
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(
    app: AppInfo,
    onAppClick: () -> Unit,
    onAppLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient(isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onAppClick,
                onLongClick = onAppLongClick
            )
            .focusable()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = app.icon),
            contentDescription = stringResource(R.string.app_icon_description, app.label),
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    }
}
