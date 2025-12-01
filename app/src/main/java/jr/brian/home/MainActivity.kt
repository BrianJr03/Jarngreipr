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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.AppVisibilityManager
import jr.brian.home.data.GridSettingsManager
import jr.brian.home.data.HomeTabManager
import jr.brian.home.data.OnboardingManager
import jr.brian.home.data.PowerSettingsManager
import jr.brian.home.data.WidgetPageAppManager
import jr.brian.home.ui.screens.BlackScreen
import jr.brian.home.ui.screens.FAQScreen
import jr.brian.home.ui.screens.LauncherPagerScreen
import jr.brian.home.ui.screens.SettingsScreen
import jr.brian.home.ui.theme.LauncherTheme
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalOnboardingManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
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
    lateinit var appVisibilityManager: AppVisibilityManager

    @Inject
    lateinit var gridSettingsManager: GridSettingsManager

    @Inject
    lateinit var appDisplayPreferenceManager: AppDisplayPreferenceManager

    @Inject
    lateinit var powerSettingsManager: PowerSettingsManager

    @Inject
    lateinit var widgetPageAppManager: WidgetPageAppManager

    @Inject
    lateinit var homeTabManager: HomeTabManager

    @Inject
    lateinit var onboardingManager: OnboardingManager

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
                    LocalAppVisibilityManager provides appVisibilityManager,
                    LocalGridSettingsManager provides gridSettingsManager,
                    LocalAppDisplayPreferenceManager provides appDisplayPreferenceManager,
                    LocalPowerSettingsManager provides powerSettingsManager,
                    LocalWidgetPageAppManager provides widgetPageAppManager,
                    LocalHomeTabManager provides homeTabManager,
                    LocalOnboardingManager provides onboardingManager
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
    val appVisibilityManager = LocalAppVisibilityManager.current
    val homeTabManager = LocalHomeTabManager.current
    val hiddenApps by appVisibilityManager.hiddenApps.collectAsStateWithLifecycle()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()

    val prefs = remember {
        context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    }


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

    LaunchedEffect(hiddenApps) {
        homeViewModel.loadAllApps(context)
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

                    LauncherPagerScreen(
                        homeViewModel = homeViewModel,
                        widgetViewModel = widgetViewModel,
                        powerViewModel = powerViewModel,
                        onSettingsClick = {
                            navController.navigate(Routes.SETTINGS)
                        },
                        initialPage = currentHomeTabIndex
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        allAppsUnfiltered = homeUiState.allAppsUnfiltered,
                        onNavigateToFAQ = {
                            navController.navigate(Routes.FAQ)
                        }
                    )
                }

                composable(Routes.FAQ) {
                    FAQScreen()
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