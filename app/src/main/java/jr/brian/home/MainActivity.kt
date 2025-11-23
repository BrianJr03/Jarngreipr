package jr.brian.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import jr.brian.home.data.AppVisibilityManager
import jr.brian.home.ui.components.AppOverlay
import jr.brian.home.ui.screens.LauncherPagerScreen
import jr.brian.home.ui.screens.SettingsScreen
import jr.brian.home.ui.theme.LauncherTheme
import jr.brian.home.ui.theme.LocalAppVisibilityManager
import jr.brian.home.ui.theme.LocalWallpaperManager
import jr.brian.home.util.Routes
import jr.brian.home.viewmodels.HomeViewModel
import jr.brian.home.viewmodels.WidgetViewModel
import javax.inject.Inject
import androidx.compose.ui.graphics.Color as GraphicsColor

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appVisibilityManager: AppVisibilityManager

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
                    LocalAppVisibilityManager provides appVisibilityManager
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
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val widgetViewModel: WidgetViewModel = viewModel()
    val wallpaperManager = LocalWallpaperManager.current
    val appVisibilityManager = LocalAppVisibilityManager.current
    val hiddenApps by appVisibilityManager.hiddenApps.collectAsStateWithLifecycle()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    val displayManager =
        remember { context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    val hasExternalDisplays = remember {
        displayManager.displays.size > 1
    }

    var showWelcomeOverlay by remember { mutableStateOf(hasExternalDisplays) }

    LaunchedEffect(hiddenApps) {
        homeViewModel.loadAllApps(context)
    }

    BackHandler(enabled = showWelcomeOverlay) {}

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
                    LauncherPagerScreen(
                        homeViewModel = homeViewModel,
                        widgetViewModel = widgetViewModel,
                        onSettingsClick = {
                            navController.navigate(Routes.SETTINGS)
                        },
                        isOverlayShown = showWelcomeOverlay
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        allApps = homeUiState.allAppsUnfiltered
                    )
                }
            }
        }

        if (showWelcomeOverlay) {
            AppOverlay(
                onDismissOverlay = {
                    showWelcomeOverlay = false
                }
            )
        }
    }
}