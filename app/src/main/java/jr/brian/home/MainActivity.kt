package jr.brian.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.data.ManagerContainer
import jr.brian.home.ui.screens.AppSearchScreen
import jr.brian.home.ui.screens.BlackScreen
import jr.brian.home.ui.screens.CustomThemeScreen
import jr.brian.home.ui.screens.FAQScreen
import jr.brian.home.ui.screens.LauncherPagerScreen
import jr.brian.home.ui.screens.QuickDeleteScreen
import jr.brian.home.ui.screens.SettingsScreen
import jr.brian.home.ui.theme.LauncherTheme
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppPositionManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalIconPackManager
import jr.brian.home.ui.theme.managers.LocalOnboardingManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.theme.managers.LocalThemeManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
import jr.brian.home.util.Routes
import jr.brian.home.viewmodels.HomeViewModel
import jr.brian.home.viewmodels.PowerViewModel
import jr.brian.home.viewmodels.WidgetViewModel
import javax.inject.Inject
import androidx.compose.ui.graphics.Color as GraphicsColor

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var managers: ManagerContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            LauncherTheme {
                val wallpaperManager = LocalWallpaperManager.current

                LaunchedEffect(wallpaperManager.currentWallpaper) {
                    if (wallpaperManager.isTransparent()) {
                        window.setBackgroundDrawable(null)
                        window.decorView.setBackgroundColor(Color.TRANSPARENT)
                    } else {
                        window.setBackgroundDrawableResource(android.R.color.transparent)
                    }
                }

                CompositionLocalProvider(
                    LocalAppVisibilityManager provides managers.appVisibilityManager,
                    LocalGridSettingsManager provides managers.gridSettingsManager,
                    LocalAppDisplayPreferenceManager provides managers.appDisplayPreferenceManager,
                    LocalPowerSettingsManager provides managers.powerSettingsManager,
                    LocalWidgetPageAppManager provides managers.widgetPageAppManager,
                    LocalHomeTabManager provides managers.homeTabManager,
                    LocalOnboardingManager provides managers.onboardingManager,
                    LocalAppPositionManager provides managers.appPositionManager,
                    LocalPageCountManager provides managers.pageCountManager,
                    LocalPageTypeManager provides managers.pageTypeManager,
                    LocalIconPackManager provides managers.iconPackManager
                ) {
                    MainContent()
                }
            }
        }
    }
}

@Composable
private fun MainContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val widgetViewModel: WidgetViewModel = viewModel()
    val powerViewModel: PowerViewModel = viewModel()
    val wallpaperManager = LocalWallpaperManager.current
    val homeTabManager = LocalHomeTabManager.current
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                powerViewModel.powerOn()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.loadAllApps(context)
        widgetViewModel.initializeWidgetHost(context)
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_PACKAGE_ADDED,
                    Intent.ACTION_PACKAGE_REMOVED,
                    Intent.ACTION_PACKAGE_CHANGED -> {
                        homeViewModel.loadAllApps(context!!)
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }

        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (wallpaperManager.isTransparent()) {
                GraphicsColor.Transparent
            } else {
                MaterialTheme.colorScheme.background
            }
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.LAUNCHER
            ) {
                composable(Routes.LAUNCHER) {
                    val currentHomeTabIndex by homeTabManager.homeTabIndex.collectAsStateWithLifecycle()
                    var showBottomSheet by remember { mutableStateOf(false) }

                    LauncherPagerScreen(
                        homeViewModel = homeViewModel,
                        widgetViewModel = widgetViewModel,
                        powerViewModel = powerViewModel,
                        onSettingsClick = {
                            navController.navigate(Routes.SETTINGS)
                        },
                        initialPage = currentHomeTabIndex,
                        onShowBottomSheet = {
                            showBottomSheet = true
                        },
                        onNavigateToSearch = {
                            navController.navigate(Routes.APP_SEARCH)
                        }
                    )

                    AnimatedVisibility(
                        visible = showBottomSheet,
                        enter = slideInVertically(
                            initialOffsetY = { it }
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { it }
                        ) + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            QuickDeleteScreen(
                                onDismiss = {
                                    showBottomSheet = false
                                }
                            )
                        }
                    }
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        allAppsUnfiltered = homeUiState.allAppsUnfiltered,
                        onNavigateToFAQ = {
                            navController.navigate(Routes.FAQ)
                        },
                        onNavigateToCustomTheme = {
                            navController.navigate(Routes.CUSTOM_THEME)
                        },
                        onIconPackChanged = {
                            homeViewModel.loadAllApps(context)
                        }
                    )
                }

                composable(Routes.FAQ) {
                    FAQScreen()
                }

                composable(Routes.CUSTOM_THEME) {
                    val themeManager = LocalThemeManager.current
                    CustomThemeScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onThemeCreated = { customTheme ->
                            themeManager.addCustomTheme(customTheme)
                            themeManager.setTheme(customTheme)
                            navController.popBackStack()
                        }
                    )
                }

                composable(Routes.APP_SEARCH) {
                    AppSearchScreen(
                        allApps = homeUiState.allAppsUnfiltered
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isPoweredOff,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            BlackScreen(
                onPowerOn = {
                    powerViewModel.powerOn()
                }
            )
        }
    }
}