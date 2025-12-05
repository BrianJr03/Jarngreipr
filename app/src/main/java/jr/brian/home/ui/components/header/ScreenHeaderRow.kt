package jr.brian.home.ui.components.header

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.components.IconBox
import jr.brian.home.ui.components.dialog.HomeTabSelectionDialog
import jr.brian.home.ui.components.onboarding.HeaderOnboardingOverlay
import jr.brian.home.ui.components.onboarding.OnboardingStep
import jr.brian.home.ui.extensions.blockHorizontalNavigation
import jr.brian.home.ui.extensions.handleFullNavigation
import jr.brian.home.data.PageType
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalOnboardingManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.viewmodels.PowerViewModel

@Composable
fun ScreenHeaderRow(
    totalPages: Int,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingIconContentDescription: String? = null,
    onLeadingIconClick: () -> Unit = {},
    trailingIcon: ImageVector? = null,
    trailingIconContentDescription: String? = null,
    onTrailingIconClick: () -> Unit = {},
    leadingIconFocusRequester: FocusRequester? = null,
    trailingIconFocusRequester: FocusRequester? = null,
    onNavigateToGrid: () -> Unit = {},
    onNavigateFromGrid: () -> Unit = {},
    powerViewModel: PowerViewModel? = null,
    showPowerButton: Boolean = false,
    enableOnboarding: Boolean = true,
    keyboardCoordinates: LayoutCoordinates? = null,
    keyboardContent: @Composable (() -> Unit)? = null,
    onFolderClick: () -> Unit = {},
    onDeletePage: (Int) -> Unit = {},
) {
    val powerSettingsManager = jr.brian.home.ui.theme.managers.LocalPowerSettingsManager.current
    val showFolder by powerSettingsManager.quickDeleteVisible.collectAsStateWithLifecycle()
    var isLeadingFocused by remember { mutableStateOf(false) }
    var isFolderFocused by remember { mutableStateOf(false) }
    var isPowerFocused by remember { mutableStateOf(false) }
    var isTrailingFocused by remember { mutableStateOf(false) }
    val folderIconFocusRequester = remember { FocusRequester() }
    val powerIconFocusRequester = remember { FocusRequester() }
    var showHomeTabDialog by remember { mutableStateOf(false) }
    val homeTabManager = LocalHomeTabManager.current
    val currentHomeTabIndex by homeTabManager.homeTabIndex.collectAsStateWithLifecycle()
    val pageCountManager = LocalPageCountManager.current
    val pageTypeManager = LocalPageTypeManager.current
    val pageTypes by pageTypeManager.pageTypes.collectAsStateWithLifecycle()

    val onboardingManager = LocalOnboardingManager.current
    val hasCompletedOnboarding by onboardingManager.hasCompletedOnboarding.collectAsStateWithLifecycle()
    var showOnboarding by remember { mutableStateOf(false) }
    var currentOnboardingStep by remember { mutableIntStateOf(0) }

    var leadingIconCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pageIndicatorsCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var trailingIconCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val settingsTitle = stringResource(R.string.onboarding_settings_title)
    val settingsDescription = stringResource(R.string.onboarding_settings_description)
    val pageIndicatorsTitle = stringResource(R.string.onboarding_page_indicators_title)
    val pageIndicatorsDescription = stringResource(R.string.onboarding_page_indicators_description)
    val optionsTitle = stringResource(R.string.onboarding_options_title)
    val optionsDescription = stringResource(R.string.onboarding_options_description)
    val keyboardTitle = stringResource(R.string.onboarding_keyboard_title)
    val keyboardDescription = stringResource(R.string.onboarding_keyboard_description)

    LaunchedEffect(hasCompletedOnboarding, enableOnboarding) {
        if (!hasCompletedOnboarding && leadingIcon != null && trailingIcon != null && enableOnboarding) {
            showOnboarding = true
        }
    }

    val onboardingSteps = remember(
        settingsTitle,
        settingsDescription,
        pageIndicatorsTitle,
        pageIndicatorsDescription,
        optionsTitle,
        optionsDescription,
        keyboardTitle,
        keyboardDescription,
        keyboardContent
    ) {
        buildList {
            add(
                OnboardingStep(
                    targetId = "leading_icon",
                    title = settingsTitle,
                    description = settingsDescription
                )
            )
            add(
                OnboardingStep(
                    targetId = "page_indicators",
                    title = pageIndicatorsTitle,
                    description = pageIndicatorsDescription
                )
            )
            add(
                OnboardingStep(
                    targetId = "trailing_icon",
                    title = optionsTitle,
                    description = optionsDescription
                )
            )
            if (keyboardContent != null) {
                add(
                    OnboardingStep(
                        targetId = "keyboard",
                        title = keyboardTitle,
                        description = keyboardDescription
                    )
                )
            }
        }
    }

    if (showHomeTabDialog) {
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
            pageTypes = pageTypes
        )
    }

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .blockHorizontalNavigation()
        ) {
            if (leadingIcon != null) {
                IconBox(
                    isFocused = isLeadingFocused,
                    modifier = Modifier
                        .handleFullNavigation(
                            onNavigateRight = {
                                if (showFolder) {
                                    folderIconFocusRequester.requestFocus()
                                } else {
                                    onNavigateToGrid()
                                }
                            },
                            onNavigateDown = onNavigateToGrid,
                            onEnterPress = onLeadingIconClick
                        )
                        .onGloballyPositioned { coordinates ->
                            leadingIconCoordinates = coordinates
                        },
                    focusRequester = leadingIconFocusRequester,
                    onFocusChanged = { isLeadingFocused = it },
                    onClick = onLeadingIconClick
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = leadingIconContentDescription,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(animatedRotation(isLeadingFocused))
                    )
                }
            }

            if (showFolder) {
                Spacer(modifier = Modifier.size(16.dp))

                IconBox(
                    isFocused = isFolderFocused,
                    modifier = Modifier.handleFullNavigation(
                        onNavigateLeft = {
                            leadingIconFocusRequester?.requestFocus()
                        },
                        onNavigateRight = onNavigateFromGrid,
                        onNavigateDown = onNavigateToGrid,
                        onEnterPress = onFolderClick
                    ),
                    focusRequester = folderIconFocusRequester,
                    onFocusChanged = { isFolderFocused = it },
                    onClick = onFolderClick
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = stringResource(R.string.header_folder_options),
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(animatedRotation(isFolderFocused))
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .clickable { showHomeTabDialog = true }
                    .onGloballyPositioned { coordinates ->
                        pageIndicatorsCoordinates = coordinates
                    }
            ) {
                PageIndicators(
                    homeTabIndex = currentHomeTabIndex,
                    totalPages = totalPages,
                    pagerState = pagerState,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (showPowerButton && powerViewModel != null) {
                IconBox(
                    isFocused = isPowerFocused,
                    modifier = Modifier.handleFullNavigation(
                        onNavigateLeft = {
                        if (showFolder) {
                                folderIconFocusRequester.requestFocus()
                            } else {
                                onNavigateFromGrid()
                            }
                        },
                        onNavigateRight = {
                            trailingIconFocusRequester?.requestFocus()
                        },
                        onNavigateDown = onNavigateToGrid,
                        onEnterPress = {
                            powerViewModel.togglePower()
                        }
                    ),
                    focusRequester = powerIconFocusRequester,
                    onFocusChanged = { isPowerFocused = it },
                    onClick = {
                        powerViewModel.togglePower()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = stringResource(R.string.header_power_button),
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(animatedRotation(isPowerFocused))
                    )
                }

                Spacer(modifier = Modifier.size(16.dp))
            }

            if (trailingIcon != null) {
                IconBox(
                    isFocused = isTrailingFocused,
                    modifier = Modifier
                        .handleFullNavigation(
                            onNavigateLeft = {
                                if (showPowerButton) {
                                    powerIconFocusRequester.requestFocus()
                                } else {
                                    onNavigateFromGrid()
                                }
                            },
                            onNavigateDown = onNavigateToGrid,
                            onEnterPress = onTrailingIconClick
                        )
                        .onGloballyPositioned { coordinates ->
                            trailingIconCoordinates = coordinates
                        },
                    focusRequester = trailingIconFocusRequester,
                    onFocusChanged = { isTrailingFocused = it },
                    onClick = onTrailingIconClick
                ) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = trailingIconContentDescription,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(animatedRotation(isTrailingFocused))
                    )
                }
            }
        }

        if (showOnboarding && leadingIcon != null && trailingIcon != null) {
            HeaderOnboardingOverlay(
                steps = onboardingSteps,
                currentStep = currentOnboardingStep,
                onNext = {
                    currentOnboardingStep++
                },
                onComplete = {
                    showOnboarding = false
                    onboardingManager.markOnboardingComplete()
                },
                leadingIconCoordinates = leadingIconCoordinates,
                pageIndicatorsCoordinates = pageIndicatorsCoordinates,
                trailingIconCoordinates = trailingIconCoordinates,
                leadingIconContent = {
                    IconBox(
                        isFocused = false,
                        onClick = {}
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = leadingIconContentDescription,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                pageIndicatorsContent = {
                    PageIndicators(
                        homeTabIndex = currentHomeTabIndex,
                        totalPages = totalPages,
                        pagerState = pagerState,
                    )
                },
                trailingIconContent = {
                    IconBox(
                        isFocused = false,
                        onClick = {}
                    ) {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = trailingIconContentDescription,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                keyboardCoordinates = keyboardCoordinates,
                keyboardContent = keyboardContent
            )
        }
    }
}
