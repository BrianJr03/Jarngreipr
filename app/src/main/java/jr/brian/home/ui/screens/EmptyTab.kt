package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.ui.components.dialog.DrawerOptionsDialog
import jr.brian.home.ui.components.dialog.HomeTabSelectionDialog
import jr.brian.home.ui.components.header.ScreenHeaderRow
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.viewmodels.PowerViewModel

@Composable
fun EmptyTab(
    modifier: Modifier = Modifier,
    powerViewModel: PowerViewModel = hiltViewModel(),
    totalPages: Int = 1,
    onShowBottomSheet: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDeletePage: (Int) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    pagerState: PagerState? = null,
    pageIndicatorBorderColor: Color = ThemePrimaryColor,
) {

    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()
    var showDrawerOptionsDialog by remember { mutableStateOf(false) }
    var showHomeTabDialog by remember { mutableStateOf(false) }
    val appFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }

    BackHandler(enabled = isPoweredOff) {}

    val powerSettingsManager = LocalPowerSettingsManager.current
    val isPowerButtonVisible by powerSettingsManager.powerButtonVisible.collectAsStateWithLifecycle()
    val isHeaderVisible by powerSettingsManager.headerVisible.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        if (pagerState != null) {
            val settingsIconFocusRequester = remember { FocusRequester() }
            val menuIconFocusRequester = remember { FocusRequester() }

            AnimatedVisibility(
                visible = isHeaderVisible,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                ScreenHeaderRow(
                    totalPages = totalPages,
                    pagerState = pagerState,
                    leadingIcon = Icons.Default.Settings,
                    leadingIconContentDescription = stringResource(R.string.keyboard_label_settings),
                    onLeadingIconClick = onSettingsClick,
                    leadingIconFocusRequester = settingsIconFocusRequester,
                    trailingIcon = Icons.Default.Menu,
                    trailingIconContentDescription = null,
                    onTrailingIconClick = {},
                    trailingIconFocusRequester = menuIconFocusRequester,
                    onNavigateToGrid = {
                        appFocusRequesters[0]?.requestFocus()
                    },
                    onNavigateFromGrid = {
                        menuIconFocusRequester.requestFocus()
                    },
                    powerViewModel = powerViewModel,
                    showPowerButton = isPowerButtonVisible,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    onFolderClick = onShowBottomSheet,
                    onDeletePage = onDeletePage,
                    pageIndicatorBorderColor = pageIndicatorBorderColor,
                    onNavigateToSearch = onNavigateToSearch
                )
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            powerViewModel.togglePower()
                        },
                        onLongPress = {
                            showDrawerOptionsDialog = true
                        }
                    )
                }
        )
    }

    if (showDrawerOptionsDialog) {
        DrawerOptionsDialog(
            onDismiss = { showDrawerOptionsDialog = false },
            onPowerClick = {
                powerViewModel.togglePower()
            },
            onTabsClick = {
                showHomeTabDialog = true
            },
            onMenuClick = {
            },
            onSettingsClick = onSettingsClick,
            onQuickDeleteClick = onShowBottomSheet,
            onCreateFolderClick = null,
        )
    }

    if (showHomeTabDialog) {
        val homeTabManager = LocalHomeTabManager.current
        val currentHomeTabIndex by homeTabManager.homeTabIndex.collectAsStateWithLifecycle()
        val pageCountManager = LocalPageCountManager.current
        val pageTypeManager = LocalPageTypeManager.current
        val pageTypes by pageTypeManager.pageTypes.collectAsStateWithLifecycle()

        HomeTabSelectionDialog(
            currentTabIndex = currentHomeTabIndex,
            totalPages = totalPages,
            onTabSelected = { index ->
                homeTabManager.setHomeTabIndex(index)
            },
            onDismiss = { showHomeTabDialog = false },
            onDeletePage = { pageIndex ->
                onDeletePage(pageIndex)
            },
            onAddPage = { pageType ->
                pageTypeManager.addPage(pageType)
                pageCountManager.addPage()
            },
            pageTypes = pageTypes,
            onNavigateToSearch = onNavigateToSearch
        )
    }
}
