package jr.brian.home.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import jr.brian.home.R
import jr.brian.home.model.ComicPopMessage
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.components.QwertyKeyboard
import jr.brian.home.ui.components.UpdateAvailableDialog
import jr.brian.home.ui.components.WhatsNewDialog
import jr.brian.home.ui.components.dialog.NotificationAccessDialog
import jr.brian.home.ui.components.dialog.openAppSettings
import jr.brian.home.ui.components.dialog.openNotificationAccessSettings
import jr.brian.home.ui.components.dialog.setNotificationAccessDeclined
import jr.brian.home.ui.components.konfetti.KonfettiPreset
import jr.brian.home.ui.components.konfetti.KonfettiPresets
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.components.settings.sections.AppearanceSection
import jr.brian.home.ui.components.settings.sections.ESDEDisplaySection
import jr.brian.home.ui.components.settings.sections.ExtrasSection
import jr.brian.home.ui.components.settings.sections.LayoutSection
import jr.brian.home.ui.components.settings.sections.MusicSection
import jr.brian.home.ui.components.settings.sections.RssSection
import jr.brian.home.ui.components.settings.sections.SupportSection
import jr.brian.home.ui.components.settings.sections.SystemSection
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalAppUpdateManager
import jr.brian.home.ui.theme.managers.LocalFloatyModeManager
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.ui.screens.SettingsConstants.SECTION_APPEARANCE
import jr.brian.home.ui.screens.SettingsConstants.SECTION_ESDE
import jr.brian.home.ui.screens.SettingsConstants.SECTION_EXTRAS
import jr.brian.home.ui.screens.SettingsConstants.SECTION_LAYOUT
import jr.brian.home.ui.screens.SettingsConstants.SECTION_MUSIC
import jr.brian.home.ui.screens.SettingsConstants.SECTION_RSS
import jr.brian.home.ui.screens.SettingsConstants.SECTION_SUPPORT
import jr.brian.home.ui.screens.SettingsConstants.SECTION_SYSTEM
import jr.brian.home.util.DeviceModel
import jr.brian.home.util.PatchNotesUtil
import jr.brian.home.util.UpdateChecker
import jr.brian.home.util.UpdateInfo
import jr.brian.home.util.sectionKeywords
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import kotlin.math.roundToInt
import kotlin.random.Random

private data class SectionInfo(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

@Composable
fun SettingsScreen(
    allAppsUnfiltered: List<AppInfo> = emptyList(),
    onNavigateToFAQ: () -> Unit,
    onNavigateToCustomTheme: () -> Unit,
    onNavigateToThemeShare: () -> Unit,
    onIconPackChanged: () -> Unit,
    onNavigateToBackButtonShortcut: () -> Unit = {},
    onNavigateToMonitor: () -> Unit = {},
    onNavigateToControlPad: () -> Unit = {},
    onNavigateToCrashLogs: () -> Unit = {},
    onNavigateToVolumeControls: () -> Unit = {},
    onNavigateToDockSettings: () -> Unit = {},
    onNavigateToEsdeSettings: () -> Unit = {},
    onRunSetupWizard: () -> Unit = {},
    onNavigateToMarqueePressShortcut: () -> Unit = {},
    onNavigateToSystemApps: () -> Unit = {},
    onNavigateToKonfettiEditor: () -> Unit = {},
    onNavigateToJingles: () -> Unit = {},
    onNavigateToRomSearch: () -> Unit = {},
    onNavigateToRssSettings: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appUpdateManager = LocalAppUpdateManager.current

    val updateAvailable = stringResource(R.string.update_not_available)

    val updateDialogState = rememberDialogState<UpdateInfo>()
    val notificationAccessDialogState = rememberDialogState<Unit>()
    val whatsNewDialogState = rememberDialogState<Unit>()
    var isCheckingForUpdates by remember { mutableStateOf(false) }
    var comicPopMessage by remember { mutableStateOf<ComicPopMessage?>(null) }
    var comicPopContainer by remember { mutableStateOf(IntSize.Zero) }

    val currentVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }
    LaunchedEffect(comicPopMessage) {
        if (comicPopMessage != null) {
            delay(650)
            comicPopMessage = null
        }
    }

    Scaffold(
        containerColor = OledBackgroundColor,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
                .onSizeChanged { comicPopContainer = it },
        ) {
            Column {
                ScreenHeader(
                    showVersion = true,
                    onVersionTapCountdown = { remaining ->
                        comicPopMessage = createVersionTapComicPopMessage(
                            remaining = remaining,
                            container = comicPopContainer
                        )
                    },
                    onBackClick = onDismiss
                )
                SettingsContent(
                    allAppsUnfiltered = allAppsUnfiltered,
                    onNavigateToFAQ = onNavigateToFAQ,
                    onNavigateToCustomTheme = onNavigateToCustomTheme,
                    onNavigateToThemeShare = onNavigateToThemeShare,
                    onIconPackChanged = onIconPackChanged,
                    onNavigateToBackButtonShortcut = onNavigateToBackButtonShortcut,
                    onNavigateToMonitor = onNavigateToMonitor,
                    onNavigateToControlPad = onNavigateToControlPad,
                    onNavigateToCrashLogs = onNavigateToCrashLogs,
                    onNavigateToVolumeControls = onNavigateToVolumeControls,
                    onNavigateToDockSettings = onNavigateToDockSettings,
                    onNavigateToEsdeSettings = onNavigateToEsdeSettings,
                    onRunSetupWizard = onRunSetupWizard,
                    onNavigateToMarqueePressShortcut = onNavigateToMarqueePressShortcut,
                    isCheckingForUpdates = isCheckingForUpdates,
                    onNavigateToSystemApps = onNavigateToSystemApps,
                    onNavigateToKonfettiEditor = onNavigateToKonfettiEditor,
                    onNavigateToJingles = onNavigateToJingles,
                    onNotificationBadgeClick = { notificationAccessDialogState.show(Unit) },
                    onWhatsNewClick = { whatsNewDialogState.show(Unit) },
                    onNavigateToRomSearch = onNavigateToRomSearch,
                    onNavigateToRssSettings = onNavigateToRssSettings,
                    onCheckForUpdates = {
                        if (!isCheckingForUpdates) {
                            isCheckingForUpdates = true
                            scope.launch {
                                appUpdateManager.clearSkippedVersion(context)
                                appUpdateManager.clearDownloadedVersion(context)

                                val update = UpdateChecker.checkForUpdate(currentVersionName)
                                isCheckingForUpdates = false

                                if (update.isUpdateAvailable) {
                                    updateDialogState.show(update)
                                } else {
                                    Toast.makeText(
                                        context,
                                        updateAvailable,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    onDismiss = onDismiss
                )
            }

            updateDialogState.item?.let { updateInfo ->
                if (updateDialogState.isVisible) {
                    UpdateAvailableDialog(
                        updateInfo = updateInfo,
                        currentVersion = currentVersionName,
                        onDismiss = updateDialogState::dismiss,
                        onRemindLater = updateDialogState::dismiss,
                        onSkipVersion = {
                            appUpdateManager.skipVersion(context, updateInfo.latestVersion)
                            updateDialogState.dismiss()
                        },
                        onDownloadComplete = {
                            appUpdateManager.markVersionDownloaded(
                                context,
                                updateInfo.latestVersion
                            )
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = comicPopMessage != null,
                enter = scaleIn(initialScale = 0.35f) + fadeIn(),
                exit = scaleOut(targetScale = 1.8f) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                comicPopMessage?.let { pop ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = pop.text,
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .offset { IntOffset(pop.x, pop.y) }
                                .rotate(pop.rotation)
                                .background(
                                    color = ThemePrimaryColor,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 3.dp,
                                    color = ThemeSecondaryColor,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (notificationAccessDialogState.isVisible) {
                NotificationAccessDialog(
                    onDismiss = notificationAccessDialogState::dismiss,
                    onGrantAccess = {
                        notificationAccessDialogState.dismiss()
                        openNotificationAccessSettings(context)
                    },
                    onOpenAppSettings = {
                        openAppSettings(context)
                    },
                    onNeverAskAgain = {
                        setNotificationAccessDeclined(context)
                        notificationAccessDialogState.dismiss()
                    }
                )
            }

            if (whatsNewDialogState.isVisible) {
                var patchNotes by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    patchNotes = PatchNotesUtil.fetchPatchNotesWithFallback(
                        context = context,
                        currentVersionName = currentVersionName,
                    )
                }
                patchNotes?.let { notes ->
                    WhatsNewDialog(
                        versionName = currentVersionName,
                        patchNotes = notes,
                        onDismiss = whatsNewDialogState::dismiss
                    )
                }
            }
        }
    }
}

private fun createVersionTapComicPopMessage(
    remaining: Int,
    container: IntSize
): ComicPopMessage {
    val width = container.width.takeIf { it > 0 } ?: 1080
    val height = container.height.takeIf { it > 0 } ?: 1920
    val x = Random.nextInt((width * 0.08f).toInt(), (width * 0.78f).toInt().coerceAtLeast(1))
    val y = Random.nextInt((height * 0.12f).toInt(), (height * 0.72f).toInt().coerceAtLeast(1))
    return ComicPopMessage(
        text = if (remaining == 0) "float \uD83D\uDC10" else "$remaining",
        x = x,
        y = y,
        rotation = Random.nextInt(-24, 25).toFloat()
    )
}

@Composable
private fun SettingsContent(
    allAppsUnfiltered: List<AppInfo> = emptyList(),
    onNavigateToFAQ: () -> Unit,
    onNavigateToCustomTheme: () -> Unit,
    onNavigateToThemeShare: () -> Unit,
    onIconPackChanged: () -> Unit,
    onNavigateToBackButtonShortcut: () -> Unit = {},
    onNavigateToMonitor: () -> Unit = {},
    onNavigateToControlPad: () -> Unit = {},
    onNavigateToCrashLogs: () -> Unit = {},
    onNavigateToVolumeControls: () -> Unit = {},
    onNavigateToDockSettings: () -> Unit = {},
    onNavigateToEsdeSettings: () -> Unit = {},
    onRunSetupWizard: () -> Unit = {},
    onNavigateToMarqueePressShortcut: () -> Unit = {},
    isCheckingForUpdates: Boolean = false,
    onNotificationBadgeClick: () -> Unit = {},
    onWhatsNewClick: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onNavigateToSystemApps: () -> Unit = {},
    onNavigateToKonfettiEditor: () -> Unit = {},
    onNavigateToJingles: () -> Unit = {},
    onNavigateToRomSearch: () -> Unit = {},
    onNavigateToRssSettings: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val floatyModeManager = LocalFloatyModeManager.current
    val explodeParties = KonfettiPresets.getParties(KonfettiPreset.EXPLODE)
    val isThorDevice = remember { Build.MODEL == DeviceModel.THOR }

    var expandedSection by remember { mutableStateOf<String?>(null) }
    var headerKonfettiKey by remember { mutableIntStateOf(0) }
    var headerKonfettiParties by remember { mutableStateOf<List<Party>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isKeyboardVisible by remember { mutableStateOf(false) }
    val keyboardFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var containerHeight by remember { mutableStateOf(0) }
    val thumbHeight = 80.dp
    val thumbHeightPx = with(density) { thumbHeight.toPx() }

    fun sectionMatchesQuery(sectionKey: String): Boolean {
        if (searchQuery.isBlank()) return true
        val q = searchQuery.lowercase().trim()
        return sectionKeywords[sectionKey]?.any { fuzzyMatches(q, it) } == true
    }

    val hasSearchResults = searchQuery.isBlank() ||
            listOf(
                SECTION_APPEARANCE, SECTION_ESDE, SECTION_LAYOUT, SECTION_SUPPORT,
                SECTION_SYSTEM, SECTION_EXTRAS, SECTION_MUSIC, SECTION_RSS
            ).any { sectionMatchesQuery(it) }

    val scrollProgress by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems == 0) 0f
            else {
                val firstVisible = listState.firstVisibleItemIndex
                val firstVisibleOffset = listState.firstVisibleItemScrollOffset
                val avgItemSize = listState.layoutInfo.visibleItemsInfo.map { it.size }.average()
                    .takeIf { it > 0 } ?: 1.0
                ((firstVisible + firstVisibleOffset / avgItemSize) / (totalItems - 1).coerceAtLeast(
                    1
                )).toFloat().coerceIn(0f, 1f)
            }
        }
    }

    fun selectSection(section: String) {
        expandedSection = section
        if (floatyModeManager.isFloatyModeActive && floatyModeManager.isSectionTapKonfettiEnabled) {
            headerKonfettiParties = null
            headerKonfettiKey++
            headerKonfettiParties = explodeParties
        }
    }

    BackHandler {
        when {
            isKeyboardVisible -> isKeyboardVisible = false
            searchQuery.isNotEmpty() -> searchQuery = ""
            expandedSection != null -> expandedSection = null
            else -> onDismiss()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsSearchBar(
            query = searchQuery,
            isKeyboardVisible = isKeyboardVisible,
            onQueryChange = { searchQuery = it },
            onToggleKeyboard = { isKeyboardVisible = !isKeyboardVisible },
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
        )

        AnimatedVisibility(
            visible = isKeyboardVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            QwertyKeyboard(
                searchQuery = searchQuery,
                showQueryText = false,
                showFlipLayoutButton = false,
                showVolControl = false,
                showSettings = false,
                showController = false,
                keyboardFocusRequesters = keyboardFocusRequesters,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .onSizeChanged { containerHeight = it.height }
        ) {
            if (searchQuery.isNotBlank()) {
                // Search results: show all matching sections fully expanded
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 4.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (sectionMatchesQuery(SECTION_APPEARANCE)) item(key = SECTION_APPEARANCE) {
                        AppearanceSection(
                            isExpanded = true,
                            onToggle = {},
                            onNavigateToCustomTheme = onNavigateToCustomTheme,
                            onNavigateToThemeShare = onNavigateToThemeShare,
                            onIconPackChanged = onIconPackChanged,
                            onNavigateToEsdeSettings = onNavigateToEsdeSettings
                        )
                    }
                    if (sectionMatchesQuery(SECTION_ESDE)) item(key = SECTION_ESDE) {
                        ESDEDisplaySection(
                            isExpanded = true,
                            onToggle = {},
                            onRunSetupWizard = onRunSetupWizard,
                            onNavigateToMarqueePressShortcut = onNavigateToMarqueePressShortcut,
                            onNavigateToSystemApps = onNavigateToSystemApps,
                            onNavigateToKonfettiEditor = onNavigateToKonfettiEditor,
                            onNavigateToJingles = onNavigateToJingles,
                            onNavigateToRomSearch = onNavigateToRomSearch,
                            onSectionHeaderTap = {}
                        )
                    }
                    if (sectionMatchesQuery(SECTION_EXTRAS)) item(key = SECTION_EXTRAS) {
                        ExtrasSection(
                            isExpanded = true,
                            onToggle = {},
                            onWhatsNewClick = onWhatsNewClick
                        )
                    }
                    if (sectionMatchesQuery(SECTION_LAYOUT)) item(key = SECTION_LAYOUT) {
                        LayoutSection(
                            isExpanded = true,
                            onToggle = {},
                            isThorDevice = isThorDevice,
                            allAppsUnfiltered = allAppsUnfiltered,
                            onNavigateToBackButtonShortcut = onNavigateToBackButtonShortcut,
                            onNavigateToDockSettings = onNavigateToDockSettings
                        )
                    }
                    if (sectionMatchesQuery(SECTION_MUSIC)) item(key = SECTION_MUSIC) {
                        MusicSection(isExpanded = true, onToggle = {})
                    }
                    if (sectionMatchesQuery(SECTION_RSS)) item(key = SECTION_RSS) {
                        RssSection(
                            isExpanded = true,
                            onToggle = {},
                            onNavigateToRssSettings = onNavigateToRssSettings
                        )
                    }
                    if (sectionMatchesQuery(SECTION_SUPPORT)) item(key = SECTION_SUPPORT) {
                        SupportSection(isExpanded = true, onToggle = {}, onNavigateToFAQ = onNavigateToFAQ)
                    }
                    if (sectionMatchesQuery(SECTION_SYSTEM)) item(key = SECTION_SYSTEM) {
                        SystemSection(
                            isExpanded = true,
                            onToggle = {},
                            isCheckingForUpdates = isCheckingForUpdates,
                            onCheckForUpdates = onCheckForUpdates,
                            onNavigateToCrashLogs = onNavigateToCrashLogs,
                            onNavigateToControlPad = onNavigateToControlPad,
                            onNavigateToMonitor = onNavigateToMonitor,
                            onNavigateToVolumeControls = onNavigateToVolumeControls,
                            onNotificationBadgeClick = onNotificationBadgeClick
                        )
                    }
                    if (!hasSearchResults) item(key = "no_results") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No settings match \"$searchQuery\"",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                // Grid or section detail, animated between the two
                AnimatedContent(
                    targetState = expandedSection,
                    transitionSpec = {
                        val goingDeeper = targetState != null
                        if (goingDeeper) {
                            (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f)) togetherWith
                                    (fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 1.02f))
                        } else {
                            (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 1.02f)) togetherWith
                                    (fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.96f))
                        }
                    },
                    label = "section_nav"
                ) { currentSection ->
                    if (currentSection == null) {
                        SettingsSectionGrid(onSectionSelected = ::selectSection)
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 32.dp, vertical = 4.dp),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item {
                                when (currentSection) {
                                    SECTION_APPEARANCE -> AppearanceSection(
                                        isExpanded = true,
                                        onToggle = { expandedSection = null },
                                        onNavigateToCustomTheme = onNavigateToCustomTheme,
                                        onNavigateToThemeShare = onNavigateToThemeShare,
                                        onIconPackChanged = onIconPackChanged,
                                        onNavigateToEsdeSettings = onNavigateToEsdeSettings
                                    )
                                    SECTION_ESDE -> ESDEDisplaySection(
                                        isExpanded = true,
                                        onToggle = { expandedSection = null },
                                        onRunSetupWizard = onRunSetupWizard,
                                        onNavigateToMarqueePressShortcut = onNavigateToMarqueePressShortcut,
                                        onNavigateToSystemApps = onNavigateToSystemApps,
                                        onNavigateToKonfettiEditor = onNavigateToKonfettiEditor,
                                        onNavigateToJingles = onNavigateToJingles,
                                        onNavigateToRomSearch = onNavigateToRomSearch,
                                        onSectionHeaderTap = {}
                                    )
                                    SECTION_EXTRAS -> ExtrasSection(
                                        isExpanded = true,
                                        onToggle = { expandedSection = null },
                                        onWhatsNewClick = onWhatsNewClick
                                    )
                                    SECTION_LAYOUT -> LayoutSection(
                                        isExpanded = true,
                                        onToggle = { expandedSection = null },
                                        isThorDevice = isThorDevice,
                                        allAppsUnfiltered = allAppsUnfiltered,
                                        onNavigateToBackButtonShortcut = onNavigateToBackButtonShortcut,
                                        onNavigateToDockSettings = onNavigateToDockSettings
                                    )
                                    SECTION_MUSIC -> MusicSection(
                                        isExpanded = true,
                                        onToggle = { expandedSection = null }
                                    )
                                    SECTION_RSS -> RssSection(
                                        isExpanded = true,
                                        onToggle = { expandedSection = null },
                                        onNavigateToRssSettings = onNavigateToRssSettings
                                    )
                                    SECTION_SUPPORT -> SupportSection(
                                        isExpanded = true,
                                        onToggle = { expandedSection = null },
                                        onNavigateToFAQ = onNavigateToFAQ
                                    )
                                    SECTION_SYSTEM -> SystemSection(
                                        isExpanded = true,
                                        onToggle = { expandedSection = null },
                                        isCheckingForUpdates = isCheckingForUpdates,
                                        onCheckForUpdates = onCheckForUpdates,
                                        onNavigateToCrashLogs = onNavigateToCrashLogs,
                                        onNavigateToControlPad = onNavigateToControlPad,
                                        onNavigateToMonitor = onNavigateToMonitor,
                                        onNavigateToVolumeControls = onNavigateToVolumeControls,
                                        onNotificationBadgeClick = onNotificationBadgeClick
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Scrollbar — only in section detail or search mode
            if (expandedSection != null || searchQuery.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(6.dp)
                        .padding(vertical = 24.dp, horizontal = 2.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, _ ->
                                change.consume()
                                val trackHeight =
                                    containerHeight - thumbHeightPx - 48.dp.toPx()
                                val newProgress =
                                    ((change.position.y - thumbHeightPx / 2) / trackHeight).coerceIn(
                                        0f, 1f
                                    )
                                val totalItems = listState.layoutInfo.totalItemsCount
                                val targetIndex =
                                    (newProgress * (totalItems - 1)).roundToInt()
                                scope.launch { listState.scrollToItem(targetIndex) }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight(0.15f)
                            .offset {
                                IntOffset(
                                    0,
                                    ((containerHeight - thumbHeightPx - 48.dp.toPx()) * scrollProgress).roundToInt()
                                )
                            }
                            .clip(RoundedCornerShape(2.dp))
                            .background(ThemePrimaryColor)
                    )
                }
            }

            headerKonfettiParties?.let { parties ->
                key(headerKonfettiKey) {
                    KonfettiView(
                        modifier = Modifier.fillMaxSize(),
                        parties = parties
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionGrid(
    onSectionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = remember {
        listOf(
            SectionInfo(SECTION_APPEARANCE, "Appearance", "Theme & Icons", Icons.Default.Palette),
            SectionInfo(SECTION_LAYOUT, "Layout", "Grid & Dock", Icons.Default.GridView),
            SectionInfo(SECTION_MUSIC, "Music", "Background Audio", Icons.Default.MusicNote),
            SectionInfo(SECTION_RSS, "RSS", "News Feeds", Icons.Default.RssFeed),
            SectionInfo(SECTION_ESDE, "ES-DE", "Emulation Display", Icons.Default.SportsEsports),
            SectionInfo(SECTION_EXTRAS, "Extras", "Extras! Extras!", Icons.Default.AutoAwesome),
            SectionInfo(SECTION_SYSTEM, "System", "Updates & Tools", Icons.Default.Tune),
            SectionInfo(SECTION_SUPPORT, "Support", "FAQ & Links", Icons.Default.Help),
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            SupportLinksRow()
        }
        items(sections, key = { it.key }) { section ->
            SectionTile(section = section, onClick = { onSectionSelected(section.key) })
        }
    }
}

@Composable
private fun SectionTile(
    section: SectionInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = subtleCardGradient(isFocused),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = borderBrush(
                    isFocused = true,
                    colors = if (isFocused) {
                        listOf(
                            ThemePrimaryColor.copy(alpha = 0.8f),
                            ThemeSecondaryColor.copy(alpha = 0.6f),
                        )
                    } else {
                        listOf(
                            ThemePrimaryColor.copy(alpha = 0.4f),
                            ThemeSecondaryColor.copy(alpha = 0.3f),
                        )
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .focusable()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = null,
            tint = ThemePrimaryColor,
            modifier = Modifier.size(26.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = section.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = section.subtitle,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun SupportLinksRow() {
    val context = LocalContext.current
    val coffeeUrl = stringResource(R.string.settings_buy_me_coffee_url)
    val kofiUrl = stringResource(R.string.settings_kofi_url)
    val discordUrl = stringResource(R.string.settings_discord_url)

    val assetManager = context.assets
    val coffeeBitmap = remember {
        BitmapFactory.decodeStream(assetManager.open("icons/icons8-buy-me-a-coffee-96.png"))
            ?.asImageBitmap()
    }
    val kofiBitmap = remember {
        BitmapFactory.decodeStream(assetManager.open("icons/icons8-ko-fi-96.png"))
            ?.asImageBitmap()
    }
    val discordBitmap = remember {
        BitmapFactory.decodeStream(assetManager.open("icons/icons8-discord-96.png"))
            ?.asImageBitmap()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(
            Triple(coffeeBitmap, stringResource(R.string.settings_buy_me_coffee_title), coffeeUrl),
            Triple(kofiBitmap, stringResource(R.string.settings_kofi_title), kofiUrl),
            Triple(discordBitmap, stringResource(R.string.settings_discord_title), discordUrl),
        ).forEach { (bitmap, title, url) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        brush = subtleCardGradient(isFocused = false),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = title,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun fuzzyMatches(query: String, target: String): Boolean {
    var qi = 0
    for (c in target) {
        if (qi < query.length && c == query[qi]) qi++
    }
    return qi == query.length
}

@Composable
private fun SettingsSearchBar(
    query: String,
    isKeyboardVisible: Boolean,
    onQueryChange: (String) -> Unit,
    onToggleKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onToggleKeyboard() }
            .fillMaxWidth()
            .background(
                brush = subtleCardGradient(isKeyboardVisible || query.isNotEmpty()),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isKeyboardVisible || query.isNotEmpty()) 2.dp else 1.dp,
                brush = borderBrush(
                    isFocused = true,
                    colors = if (isKeyboardVisible || query.isNotEmpty()) {
                        listOf(
                            ThemePrimaryColor.copy(alpha = 0.8f),
                            ThemeSecondaryColor.copy(alpha = 0.6f),
                        )
                    } else {
                        listOf(
                            ThemePrimaryColor.copy(alpha = 0.4f),
                            ThemeSecondaryColor.copy(alpha = 0.3f),
                        )
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = if (query.isNotEmpty()) ThemePrimaryColor else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = query.ifEmpty { stringResource(R.string.settings_search_settings) },
            color = if (query.isEmpty()) Color.White.copy(alpha = 0.4f) else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear search",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        IconButton(
            onClick = onToggleKeyboard,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Keyboard,
                contentDescription = if (isKeyboardVisible) "Hide keyboard" else "Show keyboard",
                tint = if (isKeyboardVisible) ThemePrimaryColor else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
