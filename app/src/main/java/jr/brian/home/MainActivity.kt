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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import jr.brian.home.ui.components.AppOverlay
import jr.brian.home.ui.screens.LauncherPagerScreen
import jr.brian.home.ui.screens.SettingsScreen
import jr.brian.home.ui.theme.LauncherTheme
import jr.brian.home.ui.theme.LocalWallpaperManager
import jr.brian.home.util.Routes
import jr.brian.home.viewmodels.HomeViewModel
import jr.brian.home.viewmodels.WidgetViewModel
import androidx.compose.ui.graphics.Color as GraphicsColor

class MainActivity : ComponentActivity() {
    private val showWelcomeOverlay = mutableStateOf(false)

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

                MainContent(
                    showWelcomeOverlay = showWelcomeOverlay.value,
                    onDismissOverlay = {
                        showWelcomeOverlay.value = false
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showWelcomeOverlay.value = true
    }
}

@Composable
private fun MainContent(
    showWelcomeOverlay: Boolean,
    onDismissOverlay: () -> Unit
) {
    val homeViewModel: HomeViewModel = viewModel()
    val widgetViewModel: WidgetViewModel = viewModel()
    val context = LocalContext.current
    val navController = rememberNavController()
    val wallpaperManager = LocalWallpaperManager.current

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
                        }
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen()
                }
            }
        }

        if (showWelcomeOverlay) {
            AppOverlay(onDismissOverlay)
        }
    }
}