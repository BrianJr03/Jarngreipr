package jr.brian.home.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import jr.brian.home.model.BackButtonShortcut
import jr.brian.home.ui.animations.SlideInVertically
import jr.brian.home.ui.screens.AppSearchScreen
import jr.brian.home.ui.screens.BackButtonShortcutScreen
import jr.brian.home.ui.screens.CrashLogsScreen
import jr.brian.home.ui.screens.CustomThemeScreen
import jr.brian.home.ui.screens.FAQScreen
import jr.brian.home.ui.screens.LauncherPagerScreen
import jr.brian.home.ui.screens.MonitorScreen
import jr.brian.home.ui.screens.QuickDeleteScreen
import jr.brian.home.ui.screens.SettingsScreen
import jr.brian.home.ui.screens.WidgetPickerScreen
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.theme.managers.LocalThemeManager
import jr.brian.home.util.Routes
import jr.brian.home.util.launchApp
import jr.brian.home.viewmodels.MainViewModel
import jr.brian.home.viewmodels.PowerViewModel
import jr.brian.home.viewmodels.WidgetViewModel

fun NavGraphBuilder.launcherScreen(
    context: Context,
    navController: NavHostController,
    mainViewModel: MainViewModel,
    widgetViewModel: WidgetViewModel,
    powerViewModel: PowerViewModel
) {
    composable(Routes.LAUNCHER) {
        val homeTabManager = LocalHomeTabManager.current
        val powerSettingsManager = LocalPowerSettingsManager.current
        val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
        val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

        var showSettingsSheet by remember { mutableStateOf(false) }
        var showAppSearchSheet by remember { mutableStateOf(false) }
        var showCustomThemeSheet by remember { mutableStateOf(false) }
        var showQuickDeleteSheet by remember { mutableStateOf(false) }
        var showMonitorSheet by remember { mutableStateOf(false) }
        var showBackButtonShortcutSheet by remember { mutableStateOf(false) }

        val currentHomeTabIndex by homeTabManager.homeTabIndex.collectAsStateWithLifecycle()
        val backButtonShortcut by powerSettingsManager.backButtonShortcut.collectAsStateWithLifecycle()
        val isBackButtonShortcutEnabled by powerSettingsManager.backButtonShortcutEnabled.collectAsStateWithLifecycle()
        val backButtonShortcutAppPackage by powerSettingsManager.backButtonShortcutAppPackage.collectAsStateWithLifecycle()

        LauncherPagerScreen(
            mainViewModel = mainViewModel,
            widgetViewModel = widgetViewModel,
            powerViewModel = powerViewModel,
            navController = navController,
            initialPage = currentHomeTabIndex,
            onSettingsClick = {
                navController.navigate(Routes.SETTINGS)
            },
            onShowBottomSheet = {
                showQuickDeleteSheet = true
            },
            onNavigateToSearch = {
                navController.navigate(Routes.APP_SEARCH)
            },
            onBackButtonShortcut = {
                if (isBackButtonShortcutEnabled) {
                    when (backButtonShortcut) {
                        BackButtonShortcut.NONE -> showBackButtonShortcutSheet = true
                        BackButtonShortcut.SETTINGS -> showSettingsSheet = true
                        BackButtonShortcut.APP_SEARCH -> showAppSearchSheet = true
                        BackButtonShortcut.POWERED_OFF -> powerViewModel.togglePower()
                        BackButtonShortcut.QUICK_DELETE -> showQuickDeleteSheet = true
                        BackButtonShortcut.CUSTOM_THEME -> showCustomThemeSheet = true
                        BackButtonShortcut.MONITOR -> showMonitorSheet = true
                        BackButtonShortcut.APP -> {
                            backButtonShortcutAppPackage?.let { packageName ->
                                launchApp(
                                    context = context,
                                    packageName = packageName,
                                    displayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                                        packageName
                                    )
                                )
                            }
                        }
                    }
                }
            }
        )

        SlideInVertically(showQuickDeleteSheet) {
            Box(modifier = Modifier.fillMaxSize()) {
                QuickDeleteScreen(
                    onDismiss = {
                        showQuickDeleteSheet = false
                    }
                )
            }
        }

        SlideInVertically(showSettingsSheet) {
            Box(modifier = Modifier.fillMaxSize()) {
                SettingsScreen(
                    allAppsUnfiltered = uiState.allAppsUnfiltered,
                    onNavigateToFAQ = {
                        showSettingsSheet = false
                        navController.navigate(Routes.FAQ)
                    },
                    onNavigateToCustomTheme = {
                        showSettingsSheet = false
                        navController.navigate(Routes.CUSTOM_THEME)
                    },
                    onIconPackChanged = {
                        mainViewModel.loadAllApps(context)
                    },
                    onNavigateToBackButtonShortcut = {
                        showSettingsSheet = false
                        navController.navigate(Routes.BACK_BUTTON_SHORTCUT)
                    },
                    onNavigateToMonitor = {
                        showSettingsSheet = false
                        navController.navigate(Routes.MONITOR)
                    },
                    onNavigateToCrashLogs = {
                        showSettingsSheet = false
                        navController.navigate(Routes.CRASH_LOGS)
                    },
                    onDismiss = {
                        showSettingsSheet = false
                    }
                )
            }
        }

        SlideInVertically(showAppSearchSheet) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppSearchScreen(
                    allApps = uiState.allAppsUnfiltered,
                    onDismiss = {
                        showAppSearchSheet = false
                    }
                )
            }
        }

        SlideInVertically(showCustomThemeSheet) {
            Box(modifier = Modifier.fillMaxSize()) {
                val themeManager = LocalThemeManager.current
                CustomThemeScreen(
                    onNavigateBack = {
                        showCustomThemeSheet = false
                    },
                    onThemeCreated = { customTheme ->
                        themeManager.addCustomTheme(customTheme)
                        themeManager.setTheme(customTheme)
                        showCustomThemeSheet = false
                    }
                )
            }
        }

        SlideInVertically(showMonitorSheet) {
            Box(modifier = Modifier.fillMaxSize()) {
                MonitorScreen(
                    onDismiss = {
                        showMonitorSheet = false
                    }
                )
            }
        }

        SlideInVertically(showBackButtonShortcutSheet) {
            Box(modifier = Modifier.fillMaxSize()) {
                BackButtonShortcutScreen(
                    allApps = uiState.allAppsUnfiltered,
                    onDismiss = {
                        showBackButtonShortcutSheet = false
                    }
                )
            }
        }
    }
}

fun NavGraphBuilder.settingsScreen(
    navController: NavHostController,
    context: Context,
    mainViewModel: MainViewModel
) {
    composable(Routes.SETTINGS) {
        var showScreen by remember { mutableStateOf(true) }
        val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

        SlideInVertically(showScreen) {
            SettingsScreen(
                allAppsUnfiltered = uiState.allAppsUnfiltered,
                onNavigateToFAQ = {
                    showScreen = false
                    navController.navigate(Routes.FAQ)
                },
                onNavigateToCustomTheme = {
                    showScreen = false
                    navController.navigate(Routes.CUSTOM_THEME)
                },
                onIconPackChanged = {
                    mainViewModel.loadAllApps(context)
                },
                onNavigateToBackButtonShortcut = {
                    showScreen = false
                    navController.navigate(Routes.BACK_BUTTON_SHORTCUT)
                },
                onNavigateToMonitor = {
                    showScreen = false
                    navController.navigate(Routes.MONITOR)
                },
                onNavigateToCrashLogs = {
                    showScreen = false
                    navController.navigate(Routes.CRASH_LOGS)
                },
                onDismiss = {
                    showScreen = false
                    navController.popBackStack()
                }
            )
        }
    }
}

fun NavGraphBuilder.faqScreen(
    navController: NavHostController
) {
    composable(Routes.FAQ) {
        var showScreen by remember { mutableStateOf(true) }

        SlideInVertically(showScreen) {
            FAQScreen(
                onDismiss = {
                    showScreen = false
                    navController.popBackStack()
                }
            )
        }
    }
}

fun NavGraphBuilder.customThemeScreen(
    navController: NavHostController
) {
    composable(Routes.CUSTOM_THEME) {
        val themeManager = LocalThemeManager.current
        var showScreen by remember { mutableStateOf(true) }

        SlideInVertically(showScreen) {
            CustomThemeScreen(
                onNavigateBack = {
                    showScreen = false
                    navController.popBackStack()
                },
                onThemeCreated = { customTheme ->
                    themeManager.addCustomTheme(customTheme)
                    themeManager.setTheme(customTheme)
                    showScreen = false
                    navController.popBackStack()
                }
            )
        }
    }
}

fun NavGraphBuilder.appSearchScreen(
    navController: NavHostController,
    mainViewModel: MainViewModel
) {
    composable(Routes.APP_SEARCH) {
        var showScreen by remember { mutableStateOf(true) }
        val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

        SlideInVertically(showScreen) {
            AppSearchScreen(
                allApps = uiState.allAppsUnfiltered,
                onDismiss = {
                    showScreen = false
                    navController.popBackStack()
                }
            )
        }
    }
}

fun NavGraphBuilder.backButtonShortcutScreen(
    navController: NavHostController,
    mainViewModel: MainViewModel
) {
    composable(Routes.BACK_BUTTON_SHORTCUT) {
        var showScreen by remember { mutableStateOf(true) }
        val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

        SlideInVertically(showScreen) {
            Box(modifier = Modifier.fillMaxSize()) {
                BackButtonShortcutScreen(
                    allApps = uiState.allAppsUnfiltered,
                    onDismiss = {
                        showScreen = false
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

fun NavGraphBuilder.monitorScreen(
    navController: NavHostController
) {
    composable(Routes.MONITOR) {
        var showScreen by remember { mutableStateOf(true) }

        SlideInVertically(showScreen) {
            MonitorScreen(
                onDismiss = {
                    showScreen = false
                    navController.popBackStack()
                }
            )
        }
    }
}

fun NavGraphBuilder.crashLogsScreen(
    navController: NavHostController
) {
    composable(Routes.CRASH_LOGS) {
        var showScreen by remember { mutableStateOf(true) }

        SlideInVertically(showScreen) {
            CrashLogsScreen(
                onDismiss = {
                    showScreen = false
                    navController.popBackStack()
                }
            )
        }
    }
}

fun NavGraphBuilder.widgetPickerScreen(
    navController: NavHostController,
    widgetViewModel: WidgetViewModel
) {
    composable(
        route = Routes.WIDGET_PICKER,
        arguments = listOf(
            navArgument("pageIndex") {
                type = NavType.IntType
            }
        )
    ) { backStackEntry ->
        val pageIndex = backStackEntry.arguments?.getInt("pageIndex") ?: 0
        var showScreen by remember { mutableStateOf(true) }

        SlideInVertically(showScreen) {
            WidgetPickerScreen(
                pageIndex = pageIndex,
                onNavigateBack = {
                    showScreen = false
                    navController.popBackStack()
                },
                onWidgetAdded = {
                    // Widget added successfully
                },
                widgetViewModel = widgetViewModel
            )
        }
    }
}
