package jr.brian.home.esde.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.esde.ui.components.AppPickerOverlay
import jr.brian.home.esde.ui.components.InfoCard
import jr.brian.home.esde.ui.components.KeyboardToggleButton
import jr.brian.home.esde.ui.components.PromptCard
import jr.brian.home.esde.ui.components.RomsPathCard
import jr.brian.home.esde.ui.components.SystemAppCard
import jr.brian.home.esde.util.getPathFromUri
import jr.brian.home.esde.viewmodel.ESDEViewModel
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.QwertyKeyboard
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.theme.OledBackgroundColor
import kotlinx.coroutines.delay

@Composable
fun SystemAppsScreen(
    allApps: List<AppInfo>,
    onNavigateBack: () -> Unit
) {
    val viewModel: ESDEViewModel = hiltViewModel()
    val prefs = LocalESDEPreferencesManager.current
    val prefsState by prefs.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var showKeyboard by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var systemBeingConfigured by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.scanComplete.collect {
            delay(1500)
            isRefreshing = false
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val path = getPathFromUri(it)
            if (path != null) {
                prefs.addRomsPath(path)
                viewModel.scanRomsFolders()
            }
        }
    }

    BackHandler(onBack = onNavigateBack)

    Scaffold(
        containerColor = OledBackgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            Column {
                ScreenHeader(onBackClick = onNavigateBack)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.esde_system_apps_title),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp)
                ) {
                    InfoCard(
                        title = stringResource(R.string.esde_system_apps_info_title),
                        content = stringResource(R.string.esde_system_apps_info_content)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    RomsPathCard(
                        paths = prefsState.romsPaths,
                        isRefreshing = isRefreshing,
                        onAddPath = { folderPickerLauncher.launch(null) },
                        onRemovePath = { path -> prefs.removeRomsPath(path) },
                        onRefresh = {
                            if (prefsState.romsPaths.isNotEmpty()) {
                                isRefreshing = true
                                viewModel.scanRomsFolders()
                            }
                        }
                    )

                    if (prefsState.systemAppMap.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        KeyboardToggleButton(
                            isKeyboardVisible = showKeyboard,
                            onToggle = { showKeyboard = !showKeyboard }
                        )

                        AnimatedVisibility(
                            visible = showKeyboard,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))

                                val keyboardFocusRequesters = remember { SnapshotStateMap<Int, FocusRequester>() }
                                var focusedKeyIndex by remember { mutableIntStateOf(0) }

                                QwertyKeyboard(
                                    searchQuery = searchQuery,
                                    showFlipLayoutButton = false,
                                    onQueryChange = { searchQuery = it },
                                    keyboardFocusRequesters = keyboardFocusRequesters,
                                    onFocusChanged = { focusedKeyIndex = it },
                                    onFlipLayout = {},
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (prefsState.systemAppMap.isEmpty() && prefsState.romsPaths.isEmpty()) {
                        PromptCard(
                            message = stringResource(R.string.esde_system_apps_prompt_message)
                        )
                    } else {
                        val filteredEntries = prefsState.systemAppMap.entries.filter { entry ->
                            if (searchQuery.isBlank()) true
                            else entry.key.contains(searchQuery, ignoreCase = true)
                        }

                        if (filteredEntries.isNotEmpty()) {
                            filteredEntries.forEach { entry ->
                                SystemAppCard(
                                    systemFolderName = entry.key,
                                    packageName = entry.value,
                                    allApps = allApps,
                                    autoLaunchEnabled = prefsState.systemAutoLaunchSet.contains(entry.key),
                                    onAutoLaunchToggle = {
                                        prefs.toggleSystemAutoLaunch(entry.key)
                                    },
                                    onChangeClick = {
                                        systemBeingConfigured = entry.key
                                    },
                                    onRemoveClick = {
                                        viewModel.removeSystemEntry(entry.key)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        } else if (searchQuery.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.esde_system_apps_no_systems_match, searchQuery),
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            systemBeingConfigured?.let { systemName ->
                AppPickerOverlay(
                    allApps = allApps,
                    onAppSelected = { app ->
                        viewModel.setSystemApp(systemName, app.packageName)
                        systemBeingConfigured = null
                    },
                    onDismiss = {
                        systemBeingConfigured = null
                    }
                )
            }
        }
    }
}
