package jr.brian.home.esde.ui.frontend.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.esde.model.ESDEPrefsState
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.esde.viewmodels.RomSearchViewModel
import jr.brian.home.ui.theme.OledBackgroundColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FrontendSettingsScreen(
    onDismiss: () -> Unit,
    onOpenSystemFilter: () -> Unit = {},
    onOpenAddSystems: () -> Unit = {}
) {
    val prefsManager = LocalESDEPreferencesManager.current
    val prefsState by prefsManager.state.collectAsStateWithLifecycle()
    val cursor = rememberFrontendSettingsState()
    val romSearchViewModel: RomSearchViewModel = hiltViewModel()
    val imageLoader = LocalESDEImageLoader.current
    val refreshRunning by romSearchViewModel.isLoading.collectAsStateWithLifecycle()
    var lastRefreshResult by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val refreshScope = rememberCoroutineScope()
    val onRefresh: () -> Unit = remember(romSearchViewModel, imageLoader) {
        {
            refreshLibrary(
                scope = refreshScope,
                imageLoader = imageLoader,
                viewModel = romSearchViewModel,
                onDone = { games, systems -> lastRefreshResult = games to systems }
            )
        }
    }

    val rowCount = rowCountFor(cursor.selectedCategory)
    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { rootFocus.requestFocus() } }

    val registerHorizontal = remember(cursor) {
        { claims: Boolean -> cursor.registerHorizontal(claims) }
    }

    CompositionLocalProvider(
        LocalRowActivation provides cursor.activationTick,
        LocalRowStep provides cursor.horizontalStep,
        LocalHorizontalRowRegistration provides registerHorizontal
    ) {
        Surface(
            color = OledBackgroundColor,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(rootFocus)
                .focusTarget()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    cursor.handleKey(
                        keyCode = event.nativeKeyEvent.keyCode,
                        rowCount = rowCount,
                        onClose = onDismiss
                    )
                }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                CategoryRail(
                    entries = FrontendSettingsCategory.entries,
                    selected = cursor.selectedCategory,
                    railHasFocus = cursor.focusOnRail
                )
                RowPaneContainer(
                    category = cursor.selectedCategory,
                    prefsState = prefsState,
                    prefsManager = prefsManager,
                    focusedRow = if (cursor.focusOnRail) -1 else cursor.focusedRow,
                    onOpenSystemFilter = {
                        onDismiss()
                        onOpenSystemFilter()
                    },
                    onOpenAddSystems = {
                        onDismiss()
                        onOpenAddSystems()
                    },
                    refreshRunning = refreshRunning,
                    lastRefreshResult = lastRefreshResult,
                    onRefresh = onRefresh
                )
            }
        }
    }
}

private fun rowCountFor(category: FrontendSettingsCategory): Int = when (category) {
    FrontendSettingsCategory.LAYOUT -> 6
    FrontendSettingsCategory.MEDIA -> 5
    FrontendSettingsCategory.FEEL -> 5
    FrontendSettingsCategory.SYSTEMS -> 2
    FrontendSettingsCategory.SCRAPING -> 4
}

private fun refreshLibrary(
    scope: CoroutineScope,
    imageLoader: ImageLoader,
    viewModel: RomSearchViewModel,
    onDone: (games: Int, systems: Int) -> Unit
) {
    if (viewModel.isLoading.value) return
    scope.launch {
        // Clear Coil caches so scraped art at paths Coil has already tried (and possibly
        // cached as a miss) is re-fetched from disk.
        imageLoader.memoryCache?.clear()
        withContext(Dispatchers.IO) { imageLoader.diskCache?.clear() }
        viewModel.refreshGames(onComplete = onDone)
    }
}

@Composable
private fun RowPaneContainer(
    category: FrontendSettingsCategory,
    prefsState: ESDEPrefsState,
    prefsManager: ESDEPreferencesManager,
    focusedRow: Int,
    onOpenSystemFilter: () -> Unit,
    onOpenAddSystems: () -> Unit,
    refreshRunning: Boolean,
    lastRefreshResult: Pair<Int, Int>?,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp, end = 16.dp, bottom = 16.dp)
    ) {
        PaneHeader(category = category)
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(end = 4.dp)),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FrontendSettingsRows(
                category = category,
                prefsState = prefsState,
                prefsManager = prefsManager,
                focusedRow = focusedRow,
                onOpenSystemFilter = onOpenSystemFilter,
                onOpenAddSystems = onOpenAddSystems,
                refreshRunning = refreshRunning,
                lastRefreshResult = lastRefreshResult,
                onRefresh = onRefresh
            )
        }
    }
}

@Composable
private fun PaneHeader(category: FrontendSettingsCategory) {
    Column {
        Text(
            text = stringResource(category.titleRes),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(category.summaryRes),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp
        )
    }
}
